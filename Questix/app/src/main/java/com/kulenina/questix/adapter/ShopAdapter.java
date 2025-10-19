package com.kulenina.questix.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kulenina.questix.R;
import com.kulenina.questix.model.Equipment;
import com.kulenina.questix.model.BossBattle;

import java.util.ArrayList;
import java.util.List;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {
    private List<Equipment> shopItems = new ArrayList<>();
    private int bossLevel = 1;
    private OnShopItemClickListener listener;

    public interface OnShopItemClickListener {
        void onShopItemClick(Equipment equipment);
    }

    public ShopAdapter(OnShopItemClickListener listener) {
        this.listener = listener;
    }

    public void setShopItems(List<Equipment> shopItems) {
        this.shopItems = shopItems;
        notifyDataSetChanged();
    }

    public void setBossLevel(int bossLevel) {
        this.bossLevel = bossLevel;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_shop_equipment, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        Equipment equipment = shopItems.get(position);
        int basePriceFromBossLevel = BossBattle.calculateCoinsReward(bossLevel);
        holder.bind(equipment, basePriceFromBossLevel);
    }

    @Override
    public int getItemCount() {
        return shopItems.size();
    }

    class ShopViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewIcon;
        private TextView textViewName;
        private TextView textViewEffect;
        private TextView textViewPrice;
        private TextView textViewType;
        private Button buttonPurchase;

        public ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewEquipmentIcon);
            textViewName = itemView.findViewById(R.id.textViewEquipmentName);
            textViewEffect = itemView.findViewById(R.id.textViewEquipmentEffect);
            textViewPrice = itemView.findViewById(R.id.textViewEquipmentPrice);
            textViewType = itemView.findViewById(R.id.textViewEquipmentType);
            buttonPurchase = itemView.findViewById(R.id.buttonPurchase);
        }

        public void bind(Equipment equipment, int basePrice) {
            textViewName.setText(equipment.getName());
            textViewEffect.setText(equipment.getEffectDescription());
            textViewType.setText(equipment.getType().name());

            int price = equipment.getPrice(basePrice);
            textViewPrice.setText(String.valueOf(price));

            switch (equipment.getType()) {
                case POTION:
                    imageViewIcon.setImageResource(R.drawable.ic_potion);
                    textViewType.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_blue_light));
                    break;
                case CLOTHING:
                    imageViewIcon.setImageResource(R.drawable.ic_clothing);
                    textViewType.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_green_light));
                    break;
            }

            buttonPurchase.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShopItemClick(equipment);
                }
            });
        }
    }
}
