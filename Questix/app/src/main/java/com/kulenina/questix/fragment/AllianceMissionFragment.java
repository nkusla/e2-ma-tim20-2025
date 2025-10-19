package com.kulenina.questix.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentAllianceMissionBinding;
import com.kulenina.questix.model.Alliance;
import com.kulenina.questix.model.MissionProgress;
import com.kulenina.questix.service.AllianceMissionService;
import com.kulenina.questix.service.AllianceMissionService.AllianceMissionListener;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AllianceMissionFragment extends Fragment {

    private FragmentAllianceMissionBinding binding;
    private AllianceMissionService missionService;
    private String allianceId;
    private String currentUserId;
    private ListenerRegistration allianceListenerRegistration;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAllianceMissionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        missionService = new AllianceMissionService();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 1. Dobijanje allianceId iz argumenata
        if (getArguments() != null) {
            allianceId = getArguments().getString("allianceId");
        }

        if (allianceId == null) {
            Toast.makeText(getContext(), "Mission ID missing.", Toast.LENGTH_SHORT).show();
            // Povratak na prethodni fragment
            requireActivity().onBackPressed();
            return;
        }

        // 2. Provera i finalizacija misije (pre prikaza)
        missionService.checkAndFinalizeMission(allianceId)
                .addOnCompleteListener(task -> {
                    // Bez obzira na ishod, pokušavamo da prikažemo stanje
                    startRealtimeListener();
                    loadUserProgress();
                });
    }

    private void startRealtimeListener() {
        // Koristimo ListenerRegistration iz AllianceMissionService
        allianceListenerRegistration = missionService.getAllianceMissionDetailsListener(
                allianceId,
                new AllianceMissionListener() {
                    @Override
                    public void onAllianceUpdated(Alliance alliance) {
                        if (alliance == null || !alliance.isMissionActive()) {
                            // Misija je završena, ili savez ne postoji/nema misije
                            updateBossUI(null);
                            // Možda navigacija nazad
                            if (isAdded()) {
                                requireActivity().onBackPressed();
                            }
                        } else {
                            // Misija je aktivna, ažuriraj UI
                            updateBossUI(alliance);
                            startTimer(alliance.getMissionStartedAt());
                        }
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(getContext(), "Real-time error: " + message, Toast.LENGTH_SHORT).show();
                        updateBossUI(null);
                    }
                }
        );
    }

    private void loadUserProgress() {
        missionService.getUserProgress(allianceId, currentUserId)
                .addOnSuccessListener(this::updateUserProgressUI)
                .addOnFailureListener(e -> {
                    binding.textViewMemberContributions.setText(getString(R.string.mission_progress_load_error));
                    // Obezbedite da imate string resurs mission_progress_load_error = "Error loading your contribution."
                });
    }

    private void updateBossUI(@Nullable Alliance alliance) {
        if (alliance == null || !alliance.isMissionActive()) {
            binding.textViewMissionTitle.setText(R.string.mission_status_inactive);
            binding.textViewTimer.setText(R.string.mission_time_finished); // Obezbedite da imate string
            binding.progressBarBossHp.setProgress(0);
            binding.textViewBossHp.setText(getString(R.string.mission_boss_defeated));
            stopTimer();
            return;
        }

        // Prikaz HP-a bosa
        int currentHp = alliance.getBossCurrentHp();
        int maxHp = alliance.getBossMaxHp();
        int progressPercent = (maxHp > 0) ? (int) (((double) currentHp / maxHp) * 100) : 0;

        binding.textViewBossHp.setText(String.format(
                Locale.US,
                "%d / %d HP (%d%%)",
                currentHp,
                maxHp,
                progressPercent
        ));

        // Postavljanje Progres Bara
        binding.progressBarBossHp.setMax(maxHp);
        binding.progressBarBossHp.setProgress(currentHp);

        // Ažuriranje naslova
        binding.textViewMissionTitle.setText(R.string.mission_title_active); // Obezbedite string
    }

    private void updateUserProgressUI(@Nullable MissionProgress progress) {
        if (progress == null) {
            binding.textViewMemberContributions.setText(R.string.mission_no_progress); // Obezbedite string
            return;
        }

        // Prikaz totalnog doprinosa (po specifikaciji 7.3)
        binding.textViewMemberContributions.setText(String.format(
                Locale.US,
                "Total HP Contribution: %d",
                progress.totalHpContribution
        ));

        // Ovde bi trebalo dodati prikaz svih pojedinačnih kvota (purchasesCount, successfulHitsCount, itd.)
        // Za sada ostavljamo samo Total HP Contribution.
    }

    private void startTimer(long missionStartedAt) {
        stopTimer(); // Zaustavi prethodni tajmer

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long duration = TimeUnit.DAYS.toMillis(14); // 14 dana trajanje misije
                long missionEndTime = missionStartedAt + duration;
                long timeLeftMillis = missionEndTime - System.currentTimeMillis();

                if (timeLeftMillis <= 0) {
                    binding.textViewTimer.setText(R.string.mission_time_finished);
                    stopTimer();
                    // Pokreće checkAndFinalizeMission opet, ako nije već završena na back-endu
                    missionService.checkAndFinalizeMission(allianceId);
                    return;
                }

                long days = TimeUnit.MILLISECONDS.toDays(timeLeftMillis);
                long hours = TimeUnit.MILLISECONDS.toHours(timeLeftMillis) % 24;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftMillis) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftMillis) % 60;

                String timeString;
                if (days > 0) {
                    timeString = String.format(Locale.US, "Time Left: %dD %02d:%02d:%02d", days, hours, minutes, seconds);
                } else {
                    timeString = String.format(Locale.US, "Time Left: %02d:%02d:%02d", hours, minutes, seconds);
                }

                binding.textViewTimer.setText(timeString);

                // Pokreni ponovo za 1 sekundu
                timerHandler.postDelayed(this, 1000);
            }
        };

        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Važno: Uklanjanje Firebase listenera i stopiranje tajmera
        if (allianceListenerRegistration != null) {
            allianceListenerRegistration.remove();
        }
        stopTimer();
        binding = null;
    }
}