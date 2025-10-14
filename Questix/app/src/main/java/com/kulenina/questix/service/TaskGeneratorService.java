package com.kulenina.questix.service;

import com.kulenina.questix.model.AppTask;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskGeneratorService {

    // Maksimalan broj dana u budućnosti za koje generišemo ponavljanja
    private static final int MAX_DAYS_IN_FUTURE = 90;
    private static final long MAX_TIME_IN_FUTURE_MILLIS =
            System.currentTimeMillis() + (long)MAX_DAYS_IN_FUTURE * 24 * 60 * 60 * 1000;

    /**
     * Generiše sve instance ponavljajućeg zadatka koje se dešavaju između dva vremenska okvira.
     * @param originalTask Originalni ponavljajući zadatak (iz baze).
     * @param fromTimestamp Vreme od kojeg počinje generisanje.
     * @param toTimestamp Vreme do kojeg se generisanje zaustavlja.
     * @return Lista generisanih instanci AppTask objekata.
     */
    public List<AppTask> generateRecurringInstances(
            AppTask originalTask,
            long fromTimestamp,
            long toTimestamp) {

        List<AppTask> instances = new ArrayList<>();

        if (!originalTask.isRecurring || originalTask.repetitionInterval == null) {
            return instances;
        }

        // 1. Definišemo granice generisanja
        // Krajnji datum je minimum od: definisanog endDate zadatka, traženog toTimestampa i MAX_DAYS_IN_FUTURE
        long effectiveEndTimestamp = Math.min(originalTask.endDate, toTimestamp);
        effectiveEndTimestamp = Math.min(effectiveEndTimestamp, MAX_TIME_IN_FUTURE_MILLIS);

        // 2. Pronalazimo prvu instancu
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(originalTask.executionTime);

        // Pomeramo kalendar napred dok ne dođemo do fromTimestamp (ili startDate)
        long currentExecutionTime = originalTask.executionTime;

        // U slučaju da je startDate zadatka u budućnosti, počinjemo od njega.
        if (originalTask.startDate > currentExecutionTime) {
            currentExecutionTime = originalTask.startDate;
        }

        // Postavljamo kalendar na vreme prve instance
        calendar.setTimeInMillis(currentExecutionTime);

        // Određujemo jedinicu za ponavljanje
        int calendarUnit = getCalendarUnit(originalTask.repetitionUnit);
        int interval = originalTask.repetitionInterval;

        // Iterativno generisanje
        while (currentExecutionTime <= effectiveEndTimestamp) {

            // 3. Generisanje instance
            if (currentExecutionTime >= fromTimestamp && currentExecutionTime <= effectiveEndTimestamp) {
                AppTask instance = cloneTaskInstance(originalTask, currentExecutionTime);
                instances.add(instance);
            }

            // 4. Pomeranje kalendara na sledeće ponavljanje
            calendar.add(calendarUnit, interval);
            currentExecutionTime = calendar.getTimeInMillis();
        }

        return instances;
    }

    // --- Pomoćne Metode ---

    /**
     * Kreira kopiju AppTask, ali sa novim ID-om i postavljenim executionTime-om.
     * Status originalnog zadatka se zadržava samo za prvu instancu.
     * Za buduće instance, status je uvek "active".
     * @param originalTask Originalni zadatak.
     * @param newExecutionTime Vreme novog ponavljanja.
     * @return Klonirana instanca.
     */
    private AppTask cloneTaskInstance(AppTask originalTask, long newExecutionTime) {
        // Kreiranje novog objekta (ručni klon, da se izbegnu problemi sa ID-om)
        AppTask instance = new AppTask(
                originalTask.userId, originalTask.categoryId, originalTask.colorHex,
                originalTask.name, originalTask.description, newExecutionTime,
                originalTask.isRecurring, originalTask.repetitionInterval, originalTask.repetitionUnit,
                originalTask.startDate, originalTask.endDate,
                originalTask.difficulty, originalTask.difficultyXp, originalTask.importance, originalTask.importanceXp
                // ID i status će se ponovo podesiti u konstruktoru, ali ćemo ga korigovati
        );

        // Za ponavljajuće zadatke, status se prenosi samo ako se poklapa executionTime originalnog zadatka.
        if (newExecutionTime == originalTask.executionTime) {
            instance.status = originalTask.status;
            instance.id = originalTask.id; // Zadržavamo ID za prvu instancu za rešavanje/izmenu
            instance.completedAt = originalTask.completedAt;
        } else {
            // Buduće instance su uvek aktivne i dobijaju privremeni ID za prikaz (ne za bazu)
            instance.status = AppTask.STATUS_ACTIVE;
            // Dajemo joj jedinstveni ID za prikaz: "ORIGINAL_ID_YYYYMMDDTHHMM"
            instance.id = originalTask.id + "_" + newExecutionTime;
        }

        instance.totalXpValue = originalTask.totalXpValue;
        return instance;
    }

    private int getCalendarUnit(String repetitionUnit) {
        if (AppTask.UNIT_DAY.equals(repetitionUnit)) {
            return Calendar.DAY_OF_YEAR;
        } else if (AppTask.UNIT_WEEK.equals(repetitionUnit)) {
            return Calendar.WEEK_OF_YEAR;
        }
        // Možete dodati i "Month" ako je potrebno
        throw new IllegalArgumentException("Invalid repetition unit: " + repetitionUnit);
    }
}