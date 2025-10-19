package com.kulenina.questix.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kulenina.questix.R;
import com.kulenina.questix.model.Clothing;
import com.kulenina.questix.model.Equipment;
import com.kulenina.questix.model.Potion;
import com.kulenina.questix.model.Weapon;

import java.util.ArrayList;
import java.util.List;

public class EquipmentInventoryAdapter extends RecyclerView.Adapter<EquipmentInventoryAdapter.EquipmentViewHolder> {
    private List<Equipment> equipmentList = new ArrayList<>();
    private OnEquipmentActivationListener listener;

    public interface OnEquipmentActivationListener {
        void onEquipmentActivationChanged(Equipment equipment, boolean isActive);
    }

    public EquipmentInventoryAdapter(OnEquipmentActivationListener listener) {
        this.listener = listener;
    }

    public void setEquipmentList(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList;
        notifyDataSetChanged();
    }

    public List<Equipment> getEquipmentList() {
        return equipmentList;
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_equipment_inventory, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        holder.bind(equipment);
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    class EquipmentViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkBoxActivate;
        private ImageView imageViewIcon;
        private TextView textViewName;
        private TextView textViewEffect;
        private TextView textViewType;
        private TextView textViewExpired;
        private TextView textViewDuration;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBoxActivate = itemView.findViewById(R.id.checkBoxActivate);
            imageViewIcon = itemView.findViewById(R.id.imageViewEquipmentIcon);
            textViewName = itemView.findViewById(R.id.textViewEquipmentName);
            textViewEffect = itemView.findViewById(R.id.textViewEquipmentEffect);
            textViewType = itemView.findViewById(R.id.textViewEquipmentType);
            textViewExpired = itemView.findViewById(R.id.textViewExpired);
            textViewDuration = itemView.findViewById(R.id.textViewDuration);
        }

        public void bind(Equipment equipment) {
            textViewName.setText(equipment.getName());
            textViewEffect.setText(equipment.getEffectDescription());
            textViewType.setText(equipment.getType().name());

            checkBoxActivate.setOnCheckedChangeListener(null);
            checkBoxActivate.setChecked(equipment.isActive());

            if (equipment.isExpired()) {
                checkBoxActivate.setEnabled(false);
                checkBoxActivate.setChecked(false);
                textViewExpired.setVisibility(View.VISIBLE);
                textViewDuration.setVisibility(View.GONE);
                itemView.setAlpha(0.5f);
            } else {
                checkBoxActivate.setEnabled(true);
                textViewExpired.setVisibility(View.GONE);
                itemView.setAlpha(1.0f);

                if (equipment instanceof Clothing) {
                    Clothing clothing = (Clothing) equipment;
                    if (clothing.getRemainingBattles() > 0) {
                        textViewDuration.setText(" " + clothing.getRemainingBattles() + " battles remaining");
                        textViewDuration.setVisibility(View.VISIBLE);
                    } else {
                        textViewDuration.setVisibility(View.GONE);
                    }
                } else {
                    textViewDuration.setVisibility(View.GONE);
                }
            }

            switch (equipment.getType()) {
                case POTION:
                    imageViewIcon.setImageResource(R.drawable.ic_potion);
                    textViewType.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_blue_light));
                    break;
                case CLOTHING:
                    imageViewIcon.setImageResource(R.drawable.ic_clothing);
                    textViewType.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_green_light));
                    break;
                case WEAPON:
                    imageViewIcon.setImageResource(R.drawable.ic_weapon);
                    textViewType.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_orange_light));
                    break;
            }

            checkBoxActivate.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null && !equipment.isExpired()) {
                    listener.onEquipmentActivationChanged(equipment, isChecked);
                }
            });
        }
    }
}
