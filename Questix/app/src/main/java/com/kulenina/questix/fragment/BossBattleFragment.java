package com.kulenina.questix.fragment;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentBossBattleBinding;
import com.kulenina.questix.dialog.ActiveEquipmentDialog;
import com.kulenina.questix.dialog.BattleResultDialog;
import com.kulenina.questix.model.*;
import com.kulenina.questix.service.*;
import com.kulenina.questix.repository.UserRepository;
import com.kulenina.questix.util.ShakeDetector;

import java.util.List;
import java.util.Locale;

public class BossBattleFragment extends Fragment {
    private FragmentBossBattleBinding binding;
    private BossBattleService bossBattleService;
    private EquipmentService equipmentService;
    private AuthService authService;
    private UserRepository userRepository;
    private LevelProgressionService levelProgressionService;

    private BossBattle currentBossBattle;
    private User currentUser;
    private List<Equipment> activeEquipment;

    // Shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_boss_battle, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeServices();
        setupClickListeners();
        setupShakeDetection();
        loadBattleData();
    }

    private void initializeServices() {
        bossBattleService = new BossBattleService();
        equipmentService = new EquipmentService();
        authService = new AuthService();
        userRepository = new UserRepository();
        levelProgressionService = new LevelProgressionService();
    }

    private void setupClickListeners() {
        binding.btnAttack.setOnClickListener(v -> performAttack());
        binding.btnViewEquipment.setOnClickListener(v -> showActiveEquipmentDialog());
    }

    private void setupShakeDetection() {
        sensorManager = (SensorManager) getActivity().getSystemService(getActivity().SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            shakeDetector = new ShakeDetector();
            shakeDetector.setOnShakeListener(count -> {
                // Trigger attack on shake
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        performAttack();
                    });
                }
            });
        }
    }

    private void showLoading() {
        binding.llLoading.setVisibility(View.VISIBLE);
        binding.llMainContent.setVisibility(View.GONE);
    }

    private void hideLoading() {
        binding.llLoading.setVisibility(View.GONE);
        binding.llMainContent.setVisibility(View.VISIBLE);
    }

    private void loadBattleData() {
        showLoading();

        String userId = authService.getCurrentUser() != null ? authService.getCurrentUser().getUid() : null;
        if (userId == null) {
            hideLoading();
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        userRepository.read(userId)
            .addOnSuccessListener(user -> {
                currentUser = user;
                if (user != null) {
                    updateUserPowerDisplay();
                    loadBossBattle();
                } else {
                    hideLoading();
                }
            })
            .addOnFailureListener(e -> {
                hideLoading();
                Toast.makeText(getContext(), "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadBossBattle() {
        bossBattleService.getCurrentBossBattle()
            .addOnSuccessListener(bossBattle -> {
                currentBossBattle = bossBattle;
                updateBossDisplay();
                loadActiveEquipment();
            })
            .addOnFailureListener(e -> {
                hideLoading();
                Toast.makeText(getContext(), "Failed to load boss battle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadActiveEquipment() {
        String userId = authService.getCurrentUser() != null ? authService.getCurrentUser().getUid() : null;
        if (userId == null) {
            hideLoading();
            return;
        }

        equipmentService.getActiveEquipment(userId)
            .addOnSuccessListener(equipment -> {
                activeEquipment = equipment;
                hideLoading();
            })
            .addOnFailureListener(e -> {
                hideLoading();
                Toast.makeText(getContext(), "Failed to load equipment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void updateBossDisplay() {
        if (currentBossBattle == null) return;

        binding.tvBossLevel.setText("Level " + currentBossBattle.getBossLevel());

        loadBossAnimation();

        binding.tvBossHp.setText(String.format(Locale.getDefault(), "%d/%d",
            currentBossBattle.getCurrentHp(), currentBossBattle.getMaxHp()));

        int hpPercentage = (int) (currentBossBattle.getHpPercentage() * 100);
        binding.pbBossHp.setProgress(hpPercentage);

        binding.tvAttacksRemaining.setText(String.format(Locale.getDefault(), "%d/5",
            currentBossBattle.getAttacksRemaining()));

        int hitChance = (int) (currentBossBattle.getSuccessRate() * 100);
        binding.tvHitChance.setText(hitChance + "%");

        binding.btnAttack.setEnabled(!currentBossBattle.isBattleFinished());
    }

    private void loadBossAnimation() {
        Glide.with(this)
            .asGif()
            .load(R.drawable.boss_idle)
            .into(binding.ivBoss);
    }

    private void showHitAnimation() {
        binding.ivHitAnimation.setBackground(new ColorDrawable(Color.argb(180, 255, 0, 0))); // Semi-transparent red
        binding.ivHitAnimation.setVisibility(View.VISIBLE);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.ivBoss, "scaleX", 1.0f, 0.9f, 1.1f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.ivBoss, "scaleY", 1.0f, 0.9f, 1.1f, 1.0f);
        ObjectAnimator shakeX = ObjectAnimator.ofFloat(binding.ivBoss, "translationX", 0f, -20f, 20f, -10f, 10f, 0f);

        ObjectAnimator flashAlpha = ObjectAnimator.ofFloat(binding.ivHitAnimation, "alpha", 0.8f, 0.0f, 0.8f, 0.0f);

        AnimatorSet hitAnimationSet = new AnimatorSet();
        hitAnimationSet.playTogether(scaleX, scaleY, shakeX, flashAlpha);
        hitAnimationSet.setDuration(600);

        hitAnimationSet.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            binding.ivHitAnimation.setVisibility(View.GONE);
        }, 600);
    }

    private void showMissAnimation() {
        binding.ivHitAnimation.setBackground(new ColorDrawable(Color.argb(120, 200, 200, 200)));
        binding.ivHitAnimation.setVisibility(View.VISIBLE);

        ObjectAnimator dodgeX = ObjectAnimator.ofFloat(binding.ivBoss, "translationX", 0f, 15f, -15f, 0f);
        ObjectAnimator dodgeRotation = ObjectAnimator.ofFloat(binding.ivBoss, "rotation", 0f, 3f, -3f, 0f);

        ObjectAnimator missFlash = ObjectAnimator.ofFloat(binding.ivHitAnimation, "alpha", 0.6f, 0.0f);

        AnimatorSet missAnimationSet = new AnimatorSet();
        missAnimationSet.playTogether(dodgeX, dodgeRotation, missFlash);
        missAnimationSet.setDuration(400);

        missAnimationSet.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            binding.ivHitAnimation.setVisibility(View.GONE);
            binding.ivBoss.setRotation(0f);
        }, 400);
    }

    private void updateUserPowerDisplay() {
        if (currentUser == null) return;

        int powerPoints = currentUser.powerPoints != null ? currentUser.powerPoints : 0;
        binding.tvUserPower.setText(powerPoints + " PP");

        int maxDisplayPP = 200;
        int progress = Math.min(100, (powerPoints * 100) / maxDisplayPP);
        binding.pbUserPower.setProgress(progress);
    }

    private void showActiveEquipmentDialog() {
        if (activeEquipment == null) {
            Toast.makeText(getContext(), "Equipment not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        ActiveEquipmentDialog dialog = ActiveEquipmentDialog.newInstance(activeEquipment);
        dialog.show(getChildFragmentManager(), "ActiveEquipmentDialog");
    }

    private void performAttack() {
        if (currentBossBattle == null || currentBossBattle.isBattleFinished()) {
            return;
        }

        binding.btnAttack.setEnabled(false);

        bossBattleService.performAttack(currentBossBattle)
            .addOnSuccessListener(battleResult -> {
                handleBattleResult(battleResult);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Attack failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                binding.btnAttack.setEnabled(true);
            });
    }

    private void handleBattleResult(BossBattleService.BattleResult battleResult) {
        Log.d("BossBattle", "Battle result - Hit: " + battleResult.attackHit +
              ", Finished: " + battleResult.battleFinished +
              ", Defeated: " + battleResult.bossDefeated);

        if (battleResult.attackHit) {
            showHitAnimation();
        } else {
            showMissAnimation();
        }

        currentBossBattle.setCurrentHp(battleResult.bossCurrentHp);
        currentBossBattle.setAttacksRemaining(battleResult.attacksRemaining);
        updateBossDisplay();

        if (battleResult.bossDefeated) {
            awardRewards(battleResult);
        }

        if(battleResult.battleFinished) {
            Log.d("BossBattle", "Showing battle result dialog");
            showBattleResult(battleResult);
        } else {
            binding.btnAttack.setEnabled(true);
        }
    }

    private void showBattleResult(BossBattleService.BattleResult battleResult) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                BattleResultDialog dialog = BattleResultDialog.newInstance(battleResult);
                dialog.setOnBattleResultListener(() -> {
                    continueBattle();
                });
                dialog.show(getChildFragmentManager(), "BattleResultDialog");
            }
        }, 500);
    }

    private void awardRewards(BossBattleService.BattleResult battleResult) {
        bossBattleService.awardBattleRewards(battleResult)
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to award rewards: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void continueBattle() {
        // Always get current boss battle (which resets the battle state)
        bossBattleService.getCurrentBossBattle()
            .addOnSuccessListener(bossBattle -> {
                currentBossBattle = bossBattle;
                updateBossDisplay();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to reload boss battle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register shake detector
        if (sensorManager != null && accelerometer != null && shakeDetector != null) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister shake detector to save battery
        if (sensorManager != null && shakeDetector != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up sensor resources
        if (sensorManager != null && shakeDetector != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
        binding = null;
    }
}
