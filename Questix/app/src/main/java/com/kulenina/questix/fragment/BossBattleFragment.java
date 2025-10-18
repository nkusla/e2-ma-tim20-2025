package com.kulenina.questix.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
// Removed LinearLayoutManager import - no longer needed

import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentBossBattleBinding;
import com.kulenina.questix.dialog.ActiveEquipmentDialog;
import com.kulenina.questix.model.*;
import com.kulenina.questix.service.*;
import com.kulenina.questix.repository.UserRepository;

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
    // Removed equipmentAdapter - now using dialog approach

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
        loadBattleData();
    }

    private void initializeServices() {
        bossBattleService = new BossBattleService();
        equipmentService = new EquipmentService();
        authService = new AuthService();
        userRepository = new UserRepository();
        levelProgressionService = new LevelProgressionService();
    }

    // Removed setupRecyclerView - now using button approach

    private void setupClickListeners() {
        binding.btnAttack.setOnClickListener(v -> performAttack());
        binding.btnContinue.setOnClickListener(v -> continueBattle());
        binding.btnViewEquipment.setOnClickListener(v -> showActiveEquipmentDialog());

        // TODO: Add shake sensor for alternative attack method
    }

    private void loadBattleData() {
        String userId = authService.getCurrentUser() != null ? authService.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load user data
        userRepository.read(userId)
            .addOnSuccessListener(user -> {
                currentUser = user;
                if (user != null) {
                    updateUserPowerDisplay();
                    loadBossBattle();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to load user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadBossBattle() {
        bossBattleService.getCurrentBossBattle()
            .addOnSuccessListener(bossBattle -> {
                currentBossBattle = bossBattle;
                updateBossDisplay();
                loadActiveEquipment(); // Still load equipment for dialog use
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to load boss battle: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadActiveEquipment() {
        String userId = authService.getCurrentUser() != null ? authService.getCurrentUser().getUid() : null;
        if (userId == null) return;

        equipmentService.getActiveEquipment(userId)
            .addOnSuccessListener(equipment -> {
                activeEquipment = equipment;
                // Equipment is now stored for dialog use, no need to update display
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to load equipment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void updateBossDisplay() {
        if (currentBossBattle == null) return;

        // Update boss level
        binding.tvBossLevel.setText("Level " + currentBossBattle.getBossLevel());

        // Update boss name
        binding.tvBossName.setText("Shadow Boss Lv." + currentBossBattle.getBossLevel());

        // Update boss HP
        binding.tvBossHp.setText(String.format(Locale.getDefault(), "%d/%d",
            currentBossBattle.getCurrentHp(), currentBossBattle.getMaxHp()));

        // Update boss HP progress bar
        int hpPercentage = (int) (currentBossBattle.getHpPercentage() * 100);
        binding.pbBossHp.setProgress(hpPercentage);

        // Update attacks remaining
        binding.tvAttacksRemaining.setText(String.format(Locale.getDefault(), "%d/5",
            currentBossBattle.getAttacksRemaining()));

        // Update hit chance
        int hitChance = (int) (currentBossBattle.getSuccessRate() * 100);
        binding.tvHitChance.setText(hitChance + "%");

        // Enable/disable attack button
        binding.btnAttack.setEnabled(!currentBossBattle.isBattleFinished());
    }

    private void updateUserPowerDisplay() {
        if (currentUser == null) return;

        int powerPoints = currentUser.powerPoints != null ? currentUser.powerPoints : 0;
        binding.tvUserPower.setText(powerPoints + " PP");

        // For the progress bar, we'll use a relative scale
        // Assuming max display of 200 PP for the progress bar
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
        // Show attack result
        if (battleResult.attackHit) {
            Toast.makeText(getContext(),
                String.format(Locale.getDefault(), "Hit! Dealt %d damage!", battleResult.damageDealt),
                Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Miss! Attack failed!", Toast.LENGTH_SHORT).show();
        }

        // Update boss HP display
        currentBossBattle.setCurrentHp(battleResult.bossCurrentHp);
        currentBossBattle.setAttacksRemaining(battleResult.attacksRemaining);
        updateBossDisplay();

        if (battleResult.battleFinished) {
            showBattleResult(battleResult);
            awardRewards(battleResult);
        } else {
            binding.btnAttack.setEnabled(true);
        }
    }

    private void showBattleResult(BossBattleService.BattleResult battleResult) {
        binding.cvBattleResult.setVisibility(View.VISIBLE);

        if (battleResult.bossDefeated) {
            binding.tvBattleOutcome.setText("Victory!");
            binding.tvBattleOutcome.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            binding.tvBattleOutcome.setText("Defeat!");
            binding.tvBattleOutcome.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        // Show coins reward
        binding.tvCoinsReward.setText("+ " + battleResult.coinsReward + " Coins");

        // Show equipment reward if any
        if (battleResult.equipmentDropped != null) {
            binding.tvEquipmentReward.setVisibility(View.VISIBLE);
            binding.tvEquipmentReward.setText("+ " + battleResult.equipmentDropped.getName());
        } else {
            binding.tvEquipmentReward.setVisibility(View.GONE);
        }
    }

    private void awardRewards(BossBattleService.BattleResult battleResult) {
        bossBattleService.awardBattleRewards(battleResult)
            .addOnSuccessListener(success -> {
                if (success) {
                    // Reload user data to reflect new coins
                    loadBattleData();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to award rewards: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void continueBattle() {
        // Navigate back or to next screen
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    // Removed onEquipmentClick - now using button approach

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
