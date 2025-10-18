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

import com.google.android.material.chip.Chip;
import com.kulenina.questix.R;
import com.kulenina.questix.adapter.EquipmentInventoryAdapter;
import com.kulenina.questix.databinding.FragmentEquipmentInventoryBinding;
import com.kulenina.questix.model.Equipment;
import com.kulenina.questix.service.AuthService;
import com.kulenina.questix.service.EquipmentService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EquipmentInventoryFragment extends Fragment implements EquipmentInventoryAdapter.OnEquipmentActivationListener {
    private FragmentEquipmentInventoryBinding binding;
    private EquipmentService equipmentService;
    private AuthService authService;
    private EquipmentInventoryAdapter adapter;
    private List<Equipment> allEquipment = new ArrayList<>();
    private Equipment.EquipmentType currentFilter = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_equipment_inventory, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        equipmentService = new EquipmentService();
        authService = new AuthService();

        setupRecyclerView();
        setupFilterChips();
        loadEquipment();
    }

    private void setupRecyclerView() {
        adapter = new EquipmentInventoryAdapter(this);
        binding.recyclerViewEquipment.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewEquipment.setAdapter(adapter);
    }

    private void setupFilterChips() {
        binding.chipAll.setOnClickListener(v -> {
            currentFilter = null;
            filterEquipment();
        });

        binding.chipPotions.setOnClickListener(v -> {
            currentFilter = Equipment.EquipmentType.POTION;
            filterEquipment();
        });

        binding.chipClothing.setOnClickListener(v -> {
            currentFilter = Equipment.EquipmentType.CLOTHING;
            filterEquipment();
        });

        binding.chipWeapons.setOnClickListener(v -> {
            currentFilter = Equipment.EquipmentType.WEAPON;
            filterEquipment();
        });
    }

    private void loadEquipment() {
        if (!authService.isUserLoggedIn()) {
            Toast.makeText(getContext(), "Please log in to view your equipment", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.layoutEmptyState.setVisibility(View.GONE);
        binding.recyclerViewEquipment.setVisibility(View.GONE);

        String userId = authService.getCurrentUser().getUid();
        equipmentService.getUserEquipment(userId)
            .addOnSuccessListener(equipment -> {
                binding.progressBar.setVisibility(View.GONE);
                allEquipment = equipment;
                filterEquipment();
            })
            .addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load equipment: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
                showEmptyState();
            });
    }

    private void filterEquipment() {
        List<Equipment> filteredList;

        if (currentFilter == null) {
            filteredList = allEquipment;
        } else {
            filteredList = allEquipment.stream()
                .filter(equipment -> equipment.getType() == currentFilter)
                .collect(Collectors.toList());
        }

        if (filteredList.isEmpty()) {
            showEmptyState();
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerViewEquipment.setVisibility(View.VISIBLE);
            adapter.setEquipmentList(filteredList);
        }
    }

    private void showEmptyState() {
        binding.layoutEmptyState.setVisibility(View.VISIBLE);
        binding.recyclerViewEquipment.setVisibility(View.GONE);
    }

    @Override
    public void onEquipmentActivationChanged(Equipment equipment, boolean isActive) {
        binding.progressBar.setVisibility(View.VISIBLE);

        if (isActive) {
            equipmentService.activateEquipment(equipment.getId())
                .addOnSuccessListener(success -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (success) {
                        equipment.setActive(true);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Failed to activate equipment", Toast.LENGTH_SHORT).show();
                        loadEquipment();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadEquipment();
                });
        } else {
            equipmentService.deactivateEquipment(equipment.getId())
                .addOnSuccessListener(success -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (success) {
                        equipment.setActive(false);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Failed to deactivate equipment", Toast.LENGTH_SHORT).show();
                        loadEquipment();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadEquipment();
                });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEquipment();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
