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
        //  this.equipmentService = new EquipmentService();
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
                    "missionActive", true,
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
                batch.set(missionProgressRepository.getDocumentReference(progressId), progress);
            }

            // 3. Izvršavanje svih operacija
            return batch.commit();
        });
    }

    /**
     * OPŠTA METODA ZA AŽURIRANJE PROGRESA I HP BOSA
     * @param allianceId ID saveza
     * @param userId ID korisnika koji je izvršio akciju
     * @param actionType Tip akcije (npr. "PURCHASE", "HIT", "LIGHT_TASK", "HEAVY_TASK", "MESSAGE")
     * @return Task<Boolean> - True ako je HP bosa umanjen
     */
    public Task<Boolean> updateMissionProgress(String allianceId, String userId, String actionType) {
        String progressId = allianceId + "_" + userId;

        return Tasks.whenAllSuccess(
                allianceRepository.read(allianceId),
                missionProgressRepository.read(progressId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            Alliance alliance = (Alliance) results.get(0);
            MissionProgress progress = (MissionProgress) results.get(1);

            if (alliance == null || !alliance.isMissionActive() || progress == null) {
                return Tasks.forResult(false);
            }

            int hpChange = 0;
            boolean progressUpdated = false;

            // Transakcija za ažuriranje HP i Progresa
            WriteBatch batch = db.batch();

            // 1. Provera kvota i obračun HP za korisnika
            switch (actionType) {
                case "PURCHASE":
                    if (progress.purchasesCount < 5) {
                        progress.purchasesCount++;
                        hpChange = HP_PURCHASE;
                        progressUpdated = true;
                    }
                    break;
                case "SUCCESSFUL_HIT":
                    if (progress.successfulHitsCount < 10) {
                        progress.successfulHitsCount++;
                        hpChange = HP_SUCCESSFUL_HIT;
                        progressUpdated = true;
                    }
                    break;
                case "LIGHT_TASK": // VL, L, N, V
                    // Specifikacija: L+N se računa 2 puta. Ako je L+N, caller šalje "LIGHT_TASK_DOUBLE"
                    // Ovde uvek samo povećavamo za 1, jer je po specifikaciji max 10 PUTA.
                    if (progress.lightTasksCount < 10) {
                        progress.lightTasksCount++;
                        hpChange = HP_LIGHT_TASK;
                        progressUpdated = true;
                    }
                    break;
                case "LIGHT_TASK_DOUBLE": // LAK + NORMALAN
                    // Dva puta se računa, max 10 puta ukupno
                    if (progress.lightTasksCount <= 8) {
                        progress.lightTasksCount += 2;
                        hpChange = HP_LIGHT_TASK * 2;
                        progressUpdated = true;
                    } else if (progress.lightTasksCount == 9) {
                        // Može da se poveća samo za 1
                        progress.lightTasksCount++;
                        hpChange = HP_LIGHT_TASK;
                        progressUpdated = true;
                    }
                    break;
                case "HEAVY_TASK": // T, ET, EV, S
                    if (progress.heavyTasksCount < 6) {
                        progress.heavyTasksCount++;
                        hpChange = HP_HEAVY_TASK;
                        progressUpdated = true;
                    }
                    break;
                case "MESSAGE":
                    String today = DATE_FORMAT.format(new Date());
                    if (!progress.messageDays.contains(today)) {
                        progress.messageDays.add(today);
                        hpChange = HP_MESSAGE_DAY;
                        progressUpdated = true;
                    }
                    break;
            }

            if (hpChange > 0) {
                // 2. Ažuriranje MissionProgress-a
                progress.totalHpContribution += hpChange;
                batch.update(missionProgressRepository.getDocumentReference(progressId),
                        "totalHpContribution", progress.totalHpContribution,
                        "purchasesCount", progress.purchasesCount,
                        "successfulHitsCount", progress.successfulHitsCount,
                        "lightTasksCount", progress.lightTasksCount,
                        "heavyTasksCount", progress.heavyTasksCount,
                        "messageDays", progress.messageDays // Message days je set, updateuje se ceo
                );

                // 3. Ažuriranje HP bosa saveza (atomski)
                int newHp = Math.max(0, alliance.getBossCurrentHp() - hpChange);
                batch.update(allianceRepository.getDocumentReference(allianceId),
                        "bossCurrentHp", newHp
                );

                return batch.commit().continueWith(commitTask -> true);
            }

            return Tasks.forResult(false);
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
                // Koristimo transakciju za HP ažuriranje bosa radi sigurnosti,
                // iako ovde koristimo samo batch za update MissionProgressa.
                // Idealno bi bilo ceo ovaj deo staviti u jednu transakciju.

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
                return awardMissionRewards(alliance, allProgresses).continueWithTask(awardTask -> {
                    // Resetovanje misije
                    return resetMissionState(allianceId, batch);
                });
            } else {
                // MISIJA NEUSPEŠNA
                return resetMissionState(allianceId, batch);
            }
        });
    }

    private Task<Void> resetMissionState(String allianceId, WriteBatch batch) {
        // Resetovanje stanja misije u Savezu
        batch.update(allianceRepository.getDocumentReference(allianceId),
                "missionActive", false,
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

                        // POZIV LOKALNE METODE registerEquipmentDrop
                        equipmentTasks.add(registerEquipmentDrop(potion));
                        equipmentTasks.add(registerEquipmentDrop(clothing));
                    }

                    // Prvo izvršavamo sve operacije na User i Alliance, pa onda opremu
                    return batch.commit().continueWithTask(commitTask -> Tasks.whenAll(equipmentTasks));
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

    public Task<MissionProgress> getUserProgress(String allianceId, String userId) {
        String progressId = allianceId + "_" + userId;
        return missionProgressRepository.read(progressId);
    }

    public Task<List<MissionProgress>> getAllianceProgresses(String allianceId) {
        return missionProgressRepository.getAllProgressesByAllianceId(allianceId);
    }

    public Task<Boolean> registerEquipmentDrop(Equipment droppedEquipment) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new RuntimeException("User not authenticated"));
        }

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
                                // Međutim, ako je to Permanent Potion, treba ga spojiti. Pretpostavimo da je Potion.combineWith već implementiran za to.
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
     * INTERFEJS za ListenerRegistration, trebaće nam u Fragmentu.
     * Dodat ovde jer je Servis jedino mesto koje zna kako da dohvati podatke.
     */
    public interface AllianceMissionListener {
        void onAllianceUpdated(Alliance alliance);
        void onError(String message);
    }
}
