package com.kulenina.questix.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kulenina.questix.R;
import com.kulenina.questix.model.Clothing;
import com.kulenina.questix.model.Equipment;

import java.util.ArrayList;
import java.util.List;

public class EquipmentDisplayAdapter extends RecyclerView.Adapter<EquipmentDisplayAdapter.EquipmentViewHolder> {
    private List<Equipment> equipmentList = new ArrayList<>();

    public void setEquipmentList(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList != null ? equipmentList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_equipment_display_only, parent, false);
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

    static class EquipmentViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewIcon;
        private TextView textViewName;
        private TextView textViewEffect;
        private TextView textViewType;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewEquipmentIcon);
            textViewName = itemView.findViewById(R.id.textViewEquipmentName);
            textViewEffect = itemView.findViewById(R.id.textViewEquipmentEffect);
            textViewType = itemView.findViewById(R.id.textViewEquipmentType);
        }

        public void bind(Equipment equipment) {
            textViewName.setText(equipment.getName());
            textViewEffect.setText(equipment.getEffectDescription());
            textViewType.setText(equipment.getType().name());

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
        }
    }
}
