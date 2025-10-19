package com.kulenina.questix.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.kulenina.questix.R;
import com.kulenina.questix.model.Equipment;
import com.kulenina.questix.service.BossBattleService;

public class BattleResultDialog extends DialogFragment {
    private static final String ARG_BATTLE_RESULT = "battle_result";
    private static final String ARG_BOSS_DEFEATED = "boss_defeated";
    private static final String ARG_COINS_REWARD = "coins_reward";
    private static final String ARG_EQUIPMENT_NAME = "equipment_name";

    private boolean bossDefeated;
    private int coinsReward;
    private String equipmentName;
    private OnBattleResultListener listener;

    public interface OnBattleResultListener {
        void onContinueClicked();
    }

    public static BattleResultDialog newInstance(BossBattleService.BattleResult battleResult) {
        BattleResultDialog dialog = new BattleResultDialog();
        Bundle args = new Bundle();
        args.putBoolean(ARG_BOSS_DEFEATED, battleResult.bossDefeated);
        args.putInt(ARG_COINS_REWARD, battleResult.coinsReward);
        if (battleResult.equipmentDropped != null) {
            args.putString(ARG_EQUIPMENT_NAME, battleResult.equipmentDropped.getName());
        }
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnBattleResultListener(OnBattleResultListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bossDefeated = getArguments().getBoolean(ARG_BOSS_DEFEATED, false);
            coinsReward = getArguments().getInt(ARG_COINS_REWARD, 0);
            equipmentName = getArguments().getString(ARG_EQUIPMENT_NAME);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_battle_result, null);

        setupViews(view);

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create();

        return dialog;
    }

    private void setupViews(View view) {
        TextView tvBattleOutcome = view.findViewById(R.id.tv_battle_outcome);
        ImageView ivTreasureChest = view.findViewById(R.id.iv_treasure_chest);
        TextView tvCoinsReward = view.findViewById(R.id.tv_coins_reward);
        TextView tvEquipmentReward = view.findViewById(R.id.tv_equipment_reward);
        Button btnContinue = view.findViewById(R.id.btn_continue);

        // Set battle outcome
        if (bossDefeated) {
            tvBattleOutcome.setText("Victory!");
            tvBattleOutcome.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            ivTreasureChest.setImageResource(R.drawable.ic_coin);
            ivTreasureChest.setColorFilter(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            tvBattleOutcome.setText("Defeat!");
            tvBattleOutcome.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            ivTreasureChest.setImageResource(R.drawable.ic_defeat);
            ivTreasureChest.setColorFilter(getResources().getColor(android.R.color.holo_red_dark));
        }

        if(coinsReward > 0) {
            tvCoinsReward.setText("+ " + coinsReward + " Coins");
        }

        if (equipmentName != null && !equipmentName.isEmpty()) {
            tvEquipmentReward.setVisibility(View.VISIBLE);
            tvEquipmentReward.setText("+ " + equipmentName);
        } else {
            tvEquipmentReward.setVisibility(View.GONE);
        }

        // Always show "Next Battle" regardless of outcome
        btnContinue.setText("Next Battle");

        btnContinue.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContinueClicked();
            }
            dismiss();
        });
    }
}
