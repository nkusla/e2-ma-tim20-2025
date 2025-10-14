package com.kulenina.questix.repository;

import com.google.android.gms.tasks.Task;
import com.kulenina.questix.model.QuotaState;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class QuotaStateRepository extends Repository<QuotaState> {

    private static final String COLLECTION_NAME = "quota_states";

    // Imajte na umu: Ovaj Repository NEĆE KORISTITI getUserScopedCollectionReference()
    // jer QuotaState dokumenti moraju imati fiksnu putanju za transakcije.
    // Pretpostavljamo da je osnovna klasa Repository<> postavljena da koristi COLLECTION_NAME
    // bez prefiksa /users/{id}/

    public QuotaStateRepository() {
        super(COLLECTION_NAME, QuotaState.class);
    }

    /**
     * Ključna metoda: Atomsko ažuriranje kvote unutar Firestore transakcije.
     * Koristi Firestore transakciju da obezbedi da se kvota uveća za samo 1
     * i da ne pređe maksimum, čak i ako dva klijenta pristupe istovremeno.
     * * @param state QuotaState objekat
     * @return Task koji vraća ažurirani QuotaState ili null ako je kvota ispunjena.
     */
    public Task<QuotaState> runQuotaTransaction(QuotaState state, String quotaType, int maxCount) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        return db.runTransaction(transaction -> {
            // 1. Čitanje trenutnog stanja
            QuotaState current = transaction.get(getCollectionReference().document(state.getId()))
                    .toObject(QuotaState.class);

            if (current == null) {
                // Ako ne postoji, kreiramo novi sa trenutnom kvotom na 1
                current = state;
                transaction.set(getCollectionReference().document(state.getId()), current);
                return current;
            }

            // 2. Provera i povećanje kvote
            int currentCount = 0;
            switch (quotaType) {
                case "VE_N": currentCount = current.veryEasyNormalCount; break;
                case "E_I": currentCount = current.easyImportantCount; break;
                case "H_EI": currentCount = current.hardExtremelyImportantCount; break;
                case "EH": currentCount = current.extremelyHardCount; break;
                case "S": currentCount = current.specialCount; break;
            }

            if (currentCount >= maxCount) {
                // Kvota je ispunjena, vraćamo null što signalizira servisu da XP nije dodeljen
                return null;
            }

            // 3. Ažuriranje i upis
            switch (quotaType) {
                case "VE_N": current.veryEasyNormalCount++; break;
                case "E_I": current.easyImportantCount++; break;
                case "H_EI": current.hardExtremelyImportantCount++; break;
                case "EH": current.extremelyHardCount++; break;
                case "S": current.specialCount++; break;
            }

            transaction.set(getCollectionReference().document(current.getId()), current);
            return current;
        });
    }

    /**
     * Čitanje stanja kvote po ID-u.
     */
    public Task<QuotaState> read(String id) {
        return getCollectionReference().document(id).get()
                .continueWith(task -> toObject(task.getResult()));
    }
}