package com.kulenina.questix.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.kulenina.questix.model.Alliance;
import com.kulenina.questix.model.User;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AllianceRepository extends Repository<Alliance> {
    private final UserRepository userRepository;
    public AllianceRepository() {
        super("alliances", Alliance.class);
        this.userRepository = new UserRepository();
    }

    public Task<List<User>> getAllianceMembers(String allianceId) {
        // 1. Čitanje Alliance dokumenta radi dobijanja liste članova (memberIds)
        return read(allianceId).continueWithTask(task -> {
            Alliance alliance = task.getResult();
            if (alliance == null || alliance.memberIds == null || alliance.memberIds.isEmpty()) {
                return Tasks.forResult(Collections.emptyList());
            }

            // 2. Kreiranje liste Task-ova za čitanje svakog User-a pojedinačno
            List<Task<User>> userTasks = alliance.memberIds.stream()
                    // userRepository::read mora da vraća Task<User>
                    .map(userRepository::read)
                    .collect(Collectors.toList());

            // 3. Čekanje na sve Task-ove i prikupljanje rezultata
            return Tasks.whenAllSuccess(userTasks)
                    .continueWith(t -> {
                        // Sigurno kastovanje, jer Tasks.whenAllSuccess kombinuje rezultate u List<Object>
                        @SuppressWarnings("unchecked")
                        List<Object> results = (List<Object>) t.getResult();

                        // Konvertujemo nazad u List<User> i filtriramo null vrednosti (ako je user obrisan)
                        return results.stream()
                                .filter(Objects::nonNull)
                                .map(obj -> (User) obj)
                                .collect(Collectors.toList());
                    });
        });
    }
}