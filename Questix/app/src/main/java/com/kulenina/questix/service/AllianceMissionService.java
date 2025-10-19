package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // DODATO: Uvoz za ListenerRegistration
import com.google.firebase.firestore.WriteBatch;
import com.kulenina.questix.model.Alliance;
import com.kulenina.questix.model.BossBattle;
import com.kulenina.questix.model.Clothing;
import com.kulenina.questix.model.Equipment;
import com.kulenina.questix.model.MissionProgress;
import com.kulenina.questix.model.Potion;
import com.kulenina.questix.model.User;
import com.kulenina.questix.model.Weapon;
import com.kulenina.questix.repository.AllianceRepository;
import com.kulenina.questix.repository.EquipmentRepository;
import com.kulenina.questix.repository.MissionProgressRepository;
import com.kulenina.questix.repository.UserRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AllianceMissionService {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final AllianceRepository allianceRepository;
    private final MissionProgressRepository missionProgressRepository;
    private final UserRepository userRepository;
    //private final EquipmentService equipmentService;
    private final EquipmentRepository equipmentRepository;

    // Specifikacije iz tačke 7.3
    private static final int BASE_BOSS_HP_PER_MEMBER = 100;
    private static final long MISSION_DURATION_MILLIS = TimeUnit.DAYS.toMillis(14);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    // HP Vrednosti i Kvota (Primer 7)
    private static final int HP_PURCHASE = 2; // max 5
    private static final int HP_SUCCESSFUL_HIT = 2; // max 10
    private static final int HP_LIGHT_TASK = 1; // max 10
    private static final int HP_HEAVY_TASK = 4; // max 6
    private static final int HP_MESSAGE_DAY = 4; // max 14 (1 po danu)
    private static final int HP_PERFECT_TASK = 10; // 10 HP na kraju misije

    public AllianceMissionService() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.allianceRepository = new AllianceRepository();
        this.missionProgressRepository = new MissionProgressRepository();
        this.userRepository = new UserRepository();
        //  this.equipmentService = new EquipmentService();
        this.equipmentRepository = new EquipmentRepository();
    }

    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    /**
     * POKRETANJE MISIJE (Poziva se iz AllianceService-a, samo Vođa)
     */
    public Task<Void> startMission(String allianceId) {
        String leaderId = getCurrentUserId();

        return Tasks.whenAllSuccess(
                // 1. Čitanje dokumenta Saveza
                allianceRepository.read(allianceId),
                // 2. Čitanje svih članova saveza
                allianceRepository.getAllianceMembers(allianceId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            Alliance alliance = (Alliance) results.get(0);
            @SuppressWarnings("unchecked")
            List<User> members = (List<User>) results.get(1);

            if (alliance == null || !alliance.isLeader(leaderId)) {
                throw new RuntimeException("Alliance not found or user is not leader.");
            }
            if (alliance.isMissionActive()) {
                throw new RuntimeException("Mission is already active.");
            }

            // PROVERA (REŠENJE PROBLEMA): Misija zahteva minimalno 2 člana (vođa + 1)
            // Pretpostavljamo da getAllianceMembers vraća celu listu članova
            if (members == null || members.size() < 2) {
                throw new RuntimeException("Mission requires at least 2 members in the alliance.");
            }

            // Inicijalizacija misije
            int totalBossHp = members.size() * BASE_BOSS_HP_PER_MEMBER;
            long startTime = System.currentTimeMillis();

            // LOKALNO ažuriranje objekta (iako koristimo mapu za batch, dobra je praksa)
            alliance.setMissionActive(true);
            alliance.setBossCurrentHp(totalBossHp);
            alliance.setBossMaxHp(totalBossHp);
            alliance.setMissionStartedAt(startTime);

            WriteBatch batch = db.batch();

            // 1. Ažuriranje Alliance dokumenta (Batch update)
            batch.update(allianceRepository.getDocumentReference(allianceId),
                    "isMissionActive", true,
                    "bossCurrentHp", totalBossHp,
                    "bossMaxHp", totalBossHp,
                    "missionStartedAt", startTime,
                    "updatedAt", startTime // Dodavanje updatedAt
            );

            // 2. Kreiranje MissionProgress dokumenta za svakog člana
            for (User member : members) {
                // MissionProgress konstruktor automatski postavlja ID = allianceId_userId
                MissionProgress progress = new MissionProgress(allianceId, member.getId());

                String progressId = allianceId + "_" + member.getId();
                System.out.println("Creating MissionProgress document with ID: " + progressId + " for user: " + member.getId());
                batch.set(missionProgressRepository.getDocumentReference(progressId), progress);
            }

            // 3. Izvršavanje svih operacija
            System.out.println("Committing batch with " + members.size() + " MissionProgress documents");
            return batch.commit()
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Mission started successfully! MissionProgress documents should be created.");
                })
                .addOnFailureListener(e -> {
                    System.out.println("Failed to start mission: " + e.getMessage());
                });
        });
    }

    /**
     * OPŠTA METODA ZA AŽURIRANJE PROGRESA I HP BOSA
     * KORISTI FIREBASE TRANSACTION za atomsko smanjenje HP-a Bosa, rešavajući problem sinhronizacije.
     * @param allianceId ID saveza
     * @param userId ID korisnika koji je izvršio akciju
     * @param actionType Tip akcije (npr. "PURCHASE", "HIT", "LIGHT_TASK", "HEAVY_TASK", "MESSAGE")
     * @return Task<Boolean> - True ako je HP bosa umanjen
     */
    public Task<Boolean> updateMissionProgress(String allianceId, String userId, String actionType) {
        String progressId = allianceId + "_" + userId;
        DocumentReference allianceRef = allianceRepository.getDocumentReference(allianceId);
        DocumentReference progressRef = missionProgressRepository.getDocumentReference(progressId);

        System.out.println("DEBUG: updateMissionProgress() called for allianceId: " + allianceId + ", userId: " + userId + ", actionType: " + actionType);

        // Vraća Task<Boolean> koji je rezultat transakcije
        return db.runTransaction(transaction -> {
            // 1. Čitanje dokumenata unutar transakcije
            Alliance alliance = transaction.get(allianceRef).toObject(Alliance.class);
            MissionProgress progress = transaction.get(progressRef).toObject(MissionProgress.class);

            System.out.println("DEBUG: Transaction - alliance: " + (alliance != null ? "found" : "null") + 
                    ", missionActive: " + (alliance != null ? alliance.isMissionActive() : "N/A") +
                    ", progress: " + (progress != null ? "found" : "null"));

            // Provera: Misija mora biti aktivna i Progress mora postojati
            if (alliance == null || !alliance.isMissionActive() || progress == null || alliance.getBossCurrentHp() <= 0) {
                System.out.println("DEBUG: Transaction failed validation - alliance: " + (alliance != null) + 
                        ", missionActive: " + (alliance != null ? alliance.isMissionActive() : false) +
                        ", progress: " + (progress != null) +
                        ", bossHp: " + (alliance != null ? alliance.getBossCurrentHp() : "N/A"));
                return false; // Nema promene
            }

            int hpChange = 0;

            // 2. Provera kvota i obračun HP za korisnika
            switch (actionType) {
                case "PURCHASE":
                    if (progress.purchasesCount < 5) {
                        progress.purchasesCount++;
                        hpChange = HP_PURCHASE;
                    }
                    break;
                case "SUCCESSFUL_HIT":
                    if (progress.successfulHitsCount < 10) {
                        progress.successfulHitsCount++;
                        hpChange = HP_SUCCESSFUL_HIT;
                    }
                    break;
                case "LIGHT_TASK": // VL, L, N, V (računa se kao 1 put)
                    if (progress.lightTasksCount < 10) {
                        progress.lightTasksCount++;
                        hpChange = HP_LIGHT_TASK;
                    }
                    break;
                case "LIGHT_TASK_DOUBLE": // LAK + NORMALAN (računa se kao 2 puta)
                    if (progress.lightTasksCount <= 8) {
                        progress.lightTasksCount += 2;
                        hpChange = HP_LIGHT_TASK * 2;
                    } else if (progress.lightTasksCount == 9) {
                        // Može da se poveća samo za 1
                        progress.lightTasksCount++;
                        hpChange = HP_LIGHT_TASK;
                    }
                    break;
                case "HEAVY_TASK": // T, ET, EV, S
                    if (progress.heavyTasksCount < 6) {
                        progress.heavyTasksCount++;
                        hpChange = HP_HEAVY_TASK;
                    }
                    break;
                case "MESSAGE":
                    String today = DATE_FORMAT.format(new Date());
                    // Provera da li je kvota dana dostignuta (max 14 dana je limit misije)
                    if (progress.getMessageDays().size() < 14 && !progress.getMessageDays().contains(today)) {
                        // Pretpostavljamo da je messageDays List<String>
                        progress.getMessageDays().add(today);
                        hpChange = HP_MESSAGE_DAY;
                    }
                    break;
            }

            // 3. Ažuriranje dokumenata
            if (hpChange > 0) {
                System.out.println("DEBUG: Updating progress - hpChange: " + hpChange + 
                        ", current totalHpContribution: " + progress.totalHpContribution);
                
                // Ažuriraj Progress
                progress.totalHpContribution += hpChange;
                transaction.set(progressRef, progress); // Postavi ceo objekat sa ažuriranim brojačima

                // Ažuriraj Alliance HP (atomski unutar transakcije)
                int newHp = Math.max(0, alliance.getBossCurrentHp() - hpChange);
                alliance.setBossCurrentHp(newHp);
                transaction.update(allianceRef, "bossCurrentHp", newHp, "updatedAt", System.currentTimeMillis());

                System.out.println("DEBUG: Progress updated successfully - new totalHpContribution: " + progress.totalHpContribution + 
                        ", new bossHp: " + newHp);
                return true; // HP bosa umanjen
            }

            // Ako je kvota dostignuta ili nema promene
            System.out.println("DEBUG: No HP change - quota reached or invalid action");
            return false;
        });
    }

    /**
     * Poziva se kada se zadatak označi kao NEURAĐEN (sekcija 2.4 - checkAndMarkMissedTasks)
     */
    public Task<Void> setMissedTaskFlag(String userId) {
        // Pronađi aktivan savez korisnika
        return userRepository.read(userId)
                .continueWithTask(userTask -> {
                    User user = userTask.getResult();
                    if (user == null || !user.isInAlliance()) return Tasks.forResult(null);

                    String progressId = user.currentAllianceId + "_" + userId;

                    return missionProgressRepository.read(progressId)
                            .continueWithTask(progressTask -> {
                                MissionProgress progress = progressTask.getResult();
                                if (progress != null && !progress.hasMissedTask) {
                                    progress.hasMissedTask = true;
                                    // Postavlja flag da se HP_PERFECT_TASK ne dodeli na kraju misije
                                    return missionProgressRepository.update(progress);
                                }
                                return Tasks.forResult(null);
                            });
                });
    }

    /**
     * PROVERA ZAVRŠETKA MISIJE (Poziva se npr. pri ulasku u Alliance fragment)
     */
    public Task<Void> checkAndFinalizeMission(String allianceId) {
        return Tasks.whenAllSuccess(
                allianceRepository.read(allianceId),
                missionProgressRepository.getAllProgressesByAllianceId(allianceId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            Alliance alliance = (Alliance) results.get(0);
            @SuppressWarnings("unchecked")
            List<MissionProgress> allProgresses = (List<MissionProgress>) results.get(1);

            if (alliance == null || !alliance.isMissionActive()) {
                return Tasks.forResult(null);
            }

            long currentTime = System.currentTimeMillis();
            long missionEndTime = alliance.getMissionStartedAt() + MISSION_DURATION_MILLIS;

            // Provera: Da li je misija završena (vreme isteklo ILI bos poražen)
            if (alliance.getBossCurrentHp() > 0 && currentTime < missionEndTime) {
                return Tasks.forResult(null); // Misija je i dalje aktivna
            }

            WriteBatch batch = db.batch();

            // 1. Obračun bonusa za "Bez nerešenih zadataka" (samo ako vreme isteklo i bos živ)
            if (alliance.getBossCurrentHp() > 0 && currentTime >= missionEndTime) {
                // Pošto je ovo Batch, moramo ažurirati lokalni Alliance objekat pre ažuriranja batch-a
                boolean allianceUpdated = false;
                for (MissionProgress progress : allProgresses) {
                    if (!progress.hasMissedTask) {
                        int newHp = Math.max(0, alliance.getBossCurrentHp() - HP_PERFECT_TASK);

                        // Samo ako imamo još HP-a za oduzimanje
                        if (newHp < alliance.getBossCurrentHp()) {
                            alliance.setBossCurrentHp(newHp);
                            progress.totalHpContribution += HP_PERFECT_TASK;
                            batch.update(missionProgressRepository.getDocumentReference(progress.getId()),
                                    "totalHpContribution", progress.totalHpContribution
                            );
                            allianceUpdated = true;
                        }
                    }
                }
                // Ažuriranje HP-a saveza sa izmenjenom vrednošću
                if(allianceUpdated) {
                    batch.update(allianceRepository.getDocumentReference(allianceId),
                            "bossCurrentHp", alliance.getBossCurrentHp()
                    );
                }
            }


            // 2. Provera ishoda i dodela nagrada - ponovo proveravamo stanje HP-a nakon bonusa
            boolean bossDefeated = alliance.getBossCurrentHp() <= 0;

            if (bossDefeated) {
                // DODELA NAGRADA
                // Prvo izvršavamo sve batch operacije koje su se možda nagomilale (npr. bonus HP za Perfect Task)
                return batch.commit().continueWithTask(commitTask ->
                        // Zatim dodeljujemo nagrade, koje uključuju asinhrono ažuriranje opreme
                        awardMissionRewards(alliance, allProgresses)
                ).continueWithTask(awardTask -> {
                    // Resetovanje misije
                    // Moramo kreirati NOVI batch jer je prethodni commit-ovan
                    return resetMissionState(allianceId, db.batch());
                });
            } else {
                // MISIJA NEUSPEŠNA
                // Prvo izvršavamo sve batch operacije (npr. bonus HP za Perfect Task)
                return batch.commit().continueWithTask(commitTask ->
                        // Zatim resetujemo stanje
                        resetMissionState(allianceId, db.batch())
                );
            }
        });
    }

    private Task<Void> resetMissionState(String allianceId, WriteBatch batch) {
        // Resetovanje stanja misije u Savezu
        batch.update(allianceRepository.getDocumentReference(allianceId),
                "isMissionActive", false,
                "bossCurrentHp", 0,
                "bossMaxHp", 0,
                "missionStartedAt", 0
        );
        // Brisanje svih MissionProgress dokumenata
        return missionProgressRepository.getAllProgressesByAllianceId(allianceId)
                .continueWithTask(task -> {
                    for (MissionProgress progress : task.getResult()) {
                        batch.delete(missionProgressRepository.getDocumentReference(progress.getId()));
                    }
                    return batch.commit();
                });
    }

    private Task<Void> awardMissionRewards(Alliance alliance, List<MissionProgress> allProgresses) {
        // Pronađi najveći nivo među članovima saveza
        return allianceRepository.getAllianceMembers(alliance.getId())
                .continueWithTask(membersTask -> {
                    List<User> members = membersTask.getResult();
                    int maxLevel = members.stream()
                            .mapToInt(u -> u.level)
                            .max()
                            .orElse(1);

                    // Izračunaj nagradu za pobedu nad narednim regularnim bosom (za taj max level)
                    int nextLevel = maxLevel + 1;
                    int nextBossReward = BossBattle.calculateCoinsReward(nextLevel);

                    // Nagrada u novčićima za misiju je 50%
                    int missionCoinsReward = (int) (nextBossReward * 0.5);

                    WriteBatch batch = db.batch();
                    List<Task<?>> equipmentTasks = new ArrayList<>();

                    for (User member : members) {
                        // 1. Dodeljivanje novčića
                        member.coins = (member.coins != null ? member.coins : 0) + missionCoinsReward;

                        // 2. Dodeljivanje bedževa
                        member.badgesCount = (member.badgesCount != null ? member.badgesCount : 0) + 1;

                        batch.update(userRepository.getDocumentReference(member.getId()),
                                "coins", member.coins,
                                "badgesCount", member.badgesCount
                        );

                        // 3. Dodeljivanje opreme (1 napitak, 1 odeća)
                        // Napitak (proizvoljno, npr. PotionType.PERMANENT_5_PERCENT)
                        Potion potion = new Potion(member.getId(), Potion.PotionType.PERMANENT_POWER_5);
                        // Odeća (proizvoljno, npr. ClothingType.GLOVES)
                        Clothing clothing = new Clothing(member.getId(), Clothing.ClothingType.GLOVES);

                        // POZIV ASINHRONE METODE registerEquipmentDrop
                        equipmentTasks.add(registerEquipmentDrop(potion));
                        equipmentTasks.add(registerEquipmentDrop(clothing));
                    }

                    // Prvo izvršavamo sve operacije na User
                    return batch.commit().continueWithTask(commitTask -> {
                        // Nakon uspešnog commit-a, čekamo da se svi asinhroni zadaci za opremu završe
                        return Tasks.whenAll(equipmentTasks);
                    });
                });
    }

    // --- Metode za pregled napretka ---

    // DODATO: Metoda za real-time praćenje stanja alijanse.
    /**
     * Postavlja real-time listener na Alliance dokument radi praćenja HP Bosa i statusa misije.
     * @param allianceId ID alijanse.
     * @param listener Listener za obradu promena stanja.
     * @return ListenerRegistration za ručno uklanjanje listenera u onDestroyView.
     */
    public ListenerRegistration getAllianceMissionDetailsListener(String allianceId, AllianceMissionListener listener) {
        return allianceRepository.getDocumentReference(allianceId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        listener.onError(e.getMessage());
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        Alliance alliance = snapshot.toObject(Alliance.class);
                        if(alliance != null) {
                            alliance.setId(snapshot.getId());
                        }
                        listener.onAllianceUpdated(alliance);
                    } else {
                        listener.onAllianceUpdated(null);
                    }
                });
    }

    public Task<Alliance> getAlliance(String allianceId) {
        return allianceRepository.read(allianceId);
    }

    public Task<MissionProgress> getUserProgressSafe(String allianceId, String userId) {
        String progressId = allianceId + "_" + userId;
        System.out.println("DEBUG: getUserProgressSafe() called for progressId: " + progressId);
        
        return missionProgressRepository.read(progressId)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    MissionProgress progress = task.getResult();
                    if (progress == null) {
                        System.out.println("DEBUG: MissionProgress document not found for ID: " + progressId);
                        return null;
                    }
                    System.out.println("DEBUG: Successfully loaded progress for user " + userId);
                    return progress;
                } else {
                    System.out.println("DEBUG: Failed to read MissionProgress for " + progressId + ": " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    return null;
                }
            });
    }

    public Task<MissionProgress> getUserProgress(String allianceId, String userId) {
        String progressId = allianceId + "_" + userId;
        return missionProgressRepository.read(progressId)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    MissionProgress progress = task.getResult();
                    if (progress == null) {
                        throw new RuntimeException("MissionProgress document not found for ID: " + progressId + ". This usually means the mission progress wasn't created when the mission started.");
                    }
                    return progress;
                } else {
                    throw new RuntimeException("Failed to read MissionProgress: " + task.getException().getMessage());
                }
            });
    }

    public Task<List<MissionProgress>> getAllianceProgresses(String allianceId) {
        return missionProgressRepository.getAllProgressesByAllianceId(allianceId);
    }

    // DEBUG METHOD: Test creating and reading a MissionProgress document
    public Task<Void> testMissionProgressCreation(String allianceId, String userId) {
        String progressId = allianceId + "_" + userId;
        MissionProgress testProgress = new MissionProgress(allianceId, userId);
        
        System.out.println("TEST: Creating MissionProgress with ID: " + progressId);
        System.out.println("TEST: AllianceId: " + testProgress.allianceId + ", UserId: " + testProgress.userId);
        
        return missionProgressRepository.getDocumentReference(progressId)
            .set(testProgress)
            .continueWithTask(task -> {
                if (task.isSuccessful()) {
                    System.out.println("TEST: Document created successfully, now trying to read it back");
                    return missionProgressRepository.read(progressId);
                } else {
                    throw new RuntimeException("Failed to create test document: " + task.getException().getMessage());
                }
            })
            .continueWith(readTask -> {
                if (readTask.isSuccessful()) {
                    MissionProgress readProgress = readTask.getResult();
                    if (readProgress != null) {
                        System.out.println("TEST: Document read successfully! ID: " + readProgress.getId());
                    } else {
                        System.out.println("TEST: Document was created but returned null when reading");
                    }
                } else {
                    System.out.println("TEST: Failed to read document: " + readTask.getException().getMessage());
                }
                return null;
            });
    }

    public Task<Boolean> registerEquipmentDrop(Equipment droppedEquipment) {
        String userId = droppedEquipment.getUserId();

        if (droppedEquipment == null) {
            return Tasks.forResult(true); // Nema opreme za dodelu
        }

        // Logika za dobijanje trenutne opreme korisnika i proveru postojanja
        return equipmentRepository.readAll()
                .continueWithTask(task -> {
                    List<Equipment> allEquipment = task.getResult();
                    // Filtrirajte opremu za trenutnog korisnika
                    List<Equipment> userEquipment = allEquipment.stream()
                            .filter(equipment -> userId.equals(equipment.getUserId()))
                            .collect(Collectors.toList());

                    Equipment existingEquipment = null;

                    // Provera za Clothing
                    if (droppedEquipment instanceof Clothing) {
                        Clothing droppedClothing = (Clothing) droppedEquipment;
                        for (Equipment eq : userEquipment) {
                            if (eq instanceof Clothing) {
                                Clothing clothing = (Clothing) eq;
                                // Proverava da li je isti tip odeće i da nije istekla
                                if (clothing.getClothingType() == droppedClothing.getClothingType() && !clothing.isExpired()) {
                                    existingEquipment = clothing;
                                    break;
                                }
                            }
                        }
                        // Provera za Weapon (Ne radimo proveru za Weapon jer smo ovde hardkodovali samo Potion i Clothing)
                    } else if (droppedEquipment instanceof Potion) {
                        Potion droppedPotion = (Potion) droppedEquipment;
                        for (Equipment eq : userEquipment) {
                            if (eq instanceof Potion) {
                                Potion potion = (Potion) eq;
                                // Napitak je unique, tako da ga uvek dodajemo kao novu instancu.
                                // Međutim, ako je to Permanent Potion, treba ga spojiti. Pretpostavljamo da je Potion.combineWith već implementiran za to.
                                // Za potrebe ovog koda, pretpostavljamo da se Potioni spajaju (npr. povećava se kvantitet ili procenat).
                                if (potion.getPotionType() == droppedPotion.getPotionType()) {
                                    existingEquipment = potion;
                                    break;
                                }
                            }
                        }
                    }


                    // Ako postojeća oprema pronađena, spoji je (combineWith)
                    if (existingEquipment != null) {
                        if (existingEquipment instanceof Clothing && droppedEquipment instanceof Clothing) {
                            ((Clothing) existingEquipment).combineWith((Clothing) droppedEquipment);
                        } else if (existingEquipment instanceof Potion && droppedEquipment instanceof Potion) {
                            ((Potion) existingEquipment).combineWith((Potion) droppedEquipment);
                        }
                        // Obrada ostale opreme (ako je potrebno)

                        // Ažuriraj postojeću opremu
                        return equipmentRepository.update(existingEquipment)
                                .continueWith(updateTask -> true);
                    } else {
                        // Kreiraj novu opremu
                        return equipmentRepository.create(droppedEquipment)
                                .continueWith(createTask -> true);
                    }
                });
    }

    /**
     * Creates missing MissionProgress documents for all alliance members
     * This is a fix for missions that were started before proper progress tracking was implemented
     */
    public Task<Void> createMissingMissionProgress(String allianceId) {
        System.out.println("DEBUG: createMissingMissionProgress() called for allianceId: " + allianceId);
        
        return allianceRepository.getAllianceMembers(allianceId)
                .continueWithTask(membersTask -> {
                    System.out.println("DEBUG: getAllianceMembers task completed, success: " + membersTask.isSuccessful());
                    
                    if (!membersTask.isSuccessful()) {
                        System.out.println("DEBUG: Failed to get alliance members: " + membersTask.getException().getMessage());
                        return Tasks.forResult(null);
                    }
                    
                    List<User> members = membersTask.getResult();
                    System.out.println("DEBUG: Got members from task: " + (members != null ? members.size() : 0));
                    
                    if (members == null || members.isEmpty()) {
                        System.out.println("DEBUG: No members found for alliance: " + allianceId);
                        return Tasks.forResult(null);
                    }

                    System.out.println("DEBUG: Found " + members.size() + " members, checking for missing progress documents");
                    
                    // Log all member details
                    for (User member : members) {
                        System.out.println("DEBUG: Member - ID: " + member.getId() + ", Username: " + member.username);
                    }

                    // Check which members are missing progress documents
                    List<Task<MissionProgress>> checkTasks = new ArrayList<>();
                    for (User member : members) {
                        System.out.println("DEBUG: Creating check task for member: " + member.username);
                        checkTasks.add(getUserProgressSafe(allianceId, member.getId()));
                    }

                    System.out.println("DEBUG: Created " + checkTasks.size() + " check tasks, waiting for completion...");
                    
                    return Tasks.whenAllComplete(checkTasks)
                            .continueWithTask(checkTask -> {
                                System.out.println("DEBUG: All check tasks completed, success: " + checkTask.isSuccessful());
                                
                                if (!checkTask.isSuccessful()) {
                                    System.out.println("DEBUG: Check tasks failed: " + checkTask.getException().getMessage());
                                    return Tasks.forResult(null);
                                }
                                
                                List<User> membersNeedingProgress = new ArrayList<>();
                                
                                for (int i = 0; i < checkTasks.size(); i++) {
                                    Task<MissionProgress> task = checkTasks.get(i);
                                    User member = members.get(i);
                                    
                                    System.out.println("DEBUG: Checking task " + i + " for member " + member.username + 
                                            " - success: " + task.isSuccessful() + 
                                            ", result: " + (task.getResult() != null ? "found" : "null"));
                                    
                                    if (!task.isSuccessful() || task.getResult() == null) {
                                        membersNeedingProgress.add(member);
                                        System.out.println("DEBUG: Member " + member.username + " needs MissionProgress document");
                                    } else {
                                        System.out.println("DEBUG: Member " + member.username + " already has MissionProgress document");
                                    }
                                }

                                if (membersNeedingProgress.isEmpty()) {
                                    System.out.println("DEBUG: All members already have MissionProgress documents");
                                    return Tasks.forResult(null);
                                }

                                System.out.println("DEBUG: Creating MissionProgress documents for " + membersNeedingProgress.size() + " members");

                                // Create missing MissionProgress documents
                                WriteBatch batch = db.batch();
                                for (User member : membersNeedingProgress) {
                                    MissionProgress progress = new MissionProgress(allianceId, member.getId());
                                    String progressId = allianceId + "_" + member.getId();
                                    System.out.println("DEBUG: Creating MissionProgress document for " + member.username + " with ID: " + progressId);
                                    batch.set(missionProgressRepository.getDocumentReference(progressId), progress);
                                }

                                System.out.println("DEBUG: Committing batch with " + membersNeedingProgress.size() + " documents...");
                                
                                return batch.commit()
                                        .continueWith(commitTask -> {
                                            System.out.println("DEBUG: Batch commit completed, success: " + commitTask.isSuccessful());
                                            
                                            if (commitTask.isSuccessful()) {
                                                System.out.println("DEBUG: Successfully created " + membersNeedingProgress.size() + " MissionProgress documents");
                                            } else {
                                                System.out.println("DEBUG: Failed to create MissionProgress documents: " + 
                                                        (commitTask.getException() != null ? commitTask.getException().getMessage() : "Unknown error"));
                                                if (commitTask.getException() != null) {
                                                    commitTask.getException().printStackTrace();
                                                }
                                            }
                                            return null;
                                        });
                            });
                });
    }

    /**
     * Gets progress for all alliance members
     */
    public Task<List<MissionProgress>> getAllMembersProgress(String allianceId) {
        System.out.println("DEBUG: AllianceMissionService.getAllMembersProgress() called for allianceId: " + allianceId);
        
        return allianceRepository.getAllianceMembers(allianceId)
                .continueWithTask(membersTask -> {
                    List<User> members = membersTask.getResult();
                    System.out.println("DEBUG: AllianceMissionService got " + (members != null ? members.size() : 0) + " members from repository");
                    
                    if (members == null || members.isEmpty()) {
                        System.out.println("DEBUG: No members found, returning empty list");
                        return Tasks.forResult(new ArrayList<>());
                    }

                    // Create tasks to get progress for each member
                    List<Task<MissionProgress>> progressTasks = new ArrayList<>();
                    for (User member : members) {
                        System.out.println("DEBUG: Creating progress task for member: " + member.username + " (ID: " + member.getId() + ")");
                        progressTasks.add(getUserProgressSafe(allianceId, member.getId()));
                    }

                    System.out.println("DEBUG: Created " + progressTasks.size() + " progress tasks, waiting for completion...");
                    
                    // Wait for all progress tasks to complete
                    return Tasks.whenAllComplete(progressTasks)
                            .continueWith(allProgressTask -> {
                                List<MissionProgress> allProgress = new ArrayList<>();
                                
                                System.out.println("DEBUG: Got " + progressTasks.size() + " task results");
                                
                                for (int i = 0; i < progressTasks.size(); i++) {
                                    Task<MissionProgress> task = progressTasks.get(i);
                                    User member = members.get(i);
                                    
                                    if (task.isSuccessful() && task.getResult() != null) {
                                        MissionProgress progress = task.getResult();
                                        allProgress.add(progress);
                                        System.out.println("DEBUG: Added progress for user " + progress.userId + " with " + progress.totalHpContribution + " HP contribution");
                                    } else {
                                        System.out.println("DEBUG: Failed to get progress for user " + member.username + " (ID: " + member.getId() + "): " + 
                                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                                        
                                        // Create a default progress entry for this member
                                        MissionProgress defaultProgress = new MissionProgress(allianceId, member.getId());
                                        allProgress.add(defaultProgress);
                                        System.out.println("DEBUG: Created default progress for user " + member.username);
                                    }
                                }
                                
                                System.out.println("DEBUG: Returning " + allProgress.size() + " progress items");
                                return allProgress;
                            });
                });
    }

    /**
     * INTERFEJS za ListenerRegistration, trebaće nam u Fragmentu.
     * Dodat ovde jer je Servis jedino mesto koje zna kako da dohvati podatke.
     */
    public interface AllianceMissionListener {
        void onAllianceUpdated(Alliance alliance);
        void onError(String message);
    }
}