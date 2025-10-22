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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kulenina.questix.R;
import com.kulenina.questix.adapter.ShopAdapter;
import com.kulenina.questix.databinding.FragmentShopBinding;
import com.kulenina.questix.model.BossBattle;
import com.kulenina.questix.model.Equipment;
import com.kulenina.questix.model.User;
import com.kulenina.questix.service.EquipmentService;
import com.kulenina.questix.service.AuthService;
import com.kulenina.questix.repository.UserRepository;

import java.util.List;

public class ShopFragment extends Fragment implements ShopAdapter.OnShopItemClickListener {
    private FragmentShopBinding binding;
    private EquipmentService equipmentService;
    private AuthService authService;
    private UserRepository userRepository;
    private ShopAdapter shopAdapter;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_shop, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        equipmentService = new EquipmentService();
        authService = new AuthService();
        userRepository = new UserRepository();

        setupRecyclerView();
        loadUserData();
        loadShopItems();
    }

    private void setupRecyclerView() {
        shopAdapter = new ShopAdapter(this);
        binding.recyclerViewShop.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewShop.setAdapter(shopAdapter);
    }

    private void loadUserData() {
        if (!authService.isUserLoggedIn()) {
            return;
        }

        String userId = authService.getCurrentUser().getUid();
        userRepository.read(userId)
            .addOnSuccessListener(user -> {
                if (user != null) {
                    currentUser = user;
                    updateUserInfo();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            });
    }

    private void updateUserInfo() {
        if (currentUser != null && binding != null) {
            binding.textViewCoins.setText(String.valueOf(currentUser.coins));

            // Display boss level instead of user level
            int bossLevel = currentUser.bossLevel != null ? currentUser.bossLevel : 1;
            binding.textViewLevel.setText("Boss Level: " + bossLevel);

            // Update shop adapter with current boss level for dynamic pricing
            shopAdapter.setBossLevel(bossLevel);
        }
    }

    private void loadShopItems() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }

        equipmentService.getShopItems()
            .addOnSuccessListener(shopItems -> {
                if (binding != null) {
                    binding.progressBar.setVisibility(View.GONE);
                    shopAdapter.setShopItems(shopItems);
                }
            })
            .addOnFailureListener(e -> {
                if (binding != null) {
                    binding.progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(getContext(), "Failed to load shop items: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onShopItemClick(Equipment equipment) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "User data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        int basePrice = BossBattle.calculateCoinsReward(currentUser.bossLevel);
        int price = equipment.getPrice(basePrice);
        if (currentUser.coins < price) {
            Toast.makeText(getContext(), "Insufficient coins! You need " + price + " coins.",
                Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Purchase Equipment")
            .setMessage("Do you want to purchase " + equipment.getName() + " for " + price + " coins?\n\n" +
                       equipment.getEffectDescription())
            .setPositiveButton("Purchase", (dialog, which) -> purchaseItem(equipment))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void purchaseItem(Equipment equipment) {
        if (binding != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }

        Equipment.EquipmentType equipmentType = equipment.getType();
        String itemType = "";

        if (equipment instanceof com.kulenina.questix.model.Potion) {
            itemType = ((com.kulenina.questix.model.Potion) equipment).getPotionType().name();
        } else if (equipment instanceof com.kulenina.questix.model.Clothing) {
            itemType = ((com.kulenina.questix.model.Clothing) equipment).getClothingType().name();
        }

        equipmentService.purchaseEquipment(equipment.getType(), itemType)
            .addOnSuccessListener(success -> {
                if (binding != null) {
                    binding.progressBar.setVisibility(View.GONE);
                }
                if (success) {
                    Toast.makeText(getContext(), "Successfully purchased " + equipment.getName() + "!",
                        Toast.LENGTH_SHORT).show();
                    loadUserData();
                } else {
                    Toast.makeText(getContext(), "Purchase failed", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                if (binding != null) {
                    binding.progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(getContext(), "Purchase failed: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null) {
            loadUserData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
