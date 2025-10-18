package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.kulenina.questix.model.*;
import com.kulenina.questix.repository.EquipmentRepository;
import com.kulenina.questix.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class EquipmentService {
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final FirebaseAuth auth;
    private final Random random;

    public EquipmentService() {
        this.equipmentRepository = new EquipmentRepository();
        this.userRepository = new UserRepository();
        this.auth = FirebaseAuth.getInstance();
        this.random = new Random();
    }

    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    // --- SHOP OPERATIONS ---

    public Task<List<Equipment>> getShopItems() {
        List<Equipment> shopItems = new ArrayList<>();

        // Add all potion types
        for (Potion.PotionType potionType : Potion.PotionType.values()) {
            Potion potion = new Potion(getCurrentUserId(), potionType);
            potion.setId("shop_" + potionType.name());
            shopItems.add(potion);
        }

        // Add all clothing types
        for (Clothing.ClothingType clothingType : Clothing.ClothingType.values()) {
            Clothing clothing = new Clothing(getCurrentUserId(), clothingType);
            clothing.setId("shop_" + clothingType.name());
            shopItems.add(clothing);
        }

        return Tasks.forResult(shopItems);
    }

    public Task<Boolean> purchaseEquipment(String equipmentType, String itemType) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new RuntimeException("User not authenticated"));
        }

        return Tasks.whenAllSuccess(
            userRepository.read(userId),
            getUserEquipment(userId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            User user = (User) results.get(0);
            @SuppressWarnings("unchecked")
            List<Equipment> userEquipment = (List<Equipment>) results.get(1);

            if (user == null) {
                throw new RuntimeException("User not found");
            }

            Equipment equipment = null;
            int price = 0;

            // Use fixed base price for calculations (mock value)
            int basePrice = 100; // Mock base price

            if ("POTION".equals(equipmentType)) {
                Potion.PotionType potionType = Potion.PotionType.valueOf(itemType);
                equipment = new Potion(userId, potionType);
                price = equipment.getPrice(basePrice);
            } else if ("CLOTHING".equals(equipmentType)) {
                Clothing.ClothingType clothingType = Clothing.ClothingType.valueOf(itemType);
                equipment = new Clothing(userId, clothingType);
                price = equipment.getPrice(basePrice);

                Clothing existingClothing = null;
                for (Equipment eq : userEquipment) {
                    if (eq instanceof Clothing) {
                        Clothing clothing = (Clothing) eq;
                        if (clothing.getClothingType() == clothingType && !clothing.isExpired) {
                            existingClothing = clothing;
                            break;
                        }
                    }
                }

                if (existingClothing != null) {
                    if (user.coins < price) {
                        throw new RuntimeException("Insufficient coins");
                    }

                    user.coins -= price;
                    existingClothing.combineWith((Clothing) equipment);

                    return Tasks.whenAll(
                        equipmentRepository.update(existingClothing),
                        userRepository.update(user)
                    ).continueWith(updateTask -> true);
                }
            }

            if (equipment == null) {
                throw new RuntimeException("Invalid equipment type");
            }

            if (user.coins < price) {
                throw new RuntimeException("Insufficient coins");
            }

            // Deduct coins
            user.coins -= price;
            equipment.setId(UUID.randomUUID().toString());

            // Save equipment and update user
            return equipmentRepository.create(equipment)
                .continueWithTask(createTask -> userRepository.update(user))
                .continueWith(updateTask -> true);
        });
    }

    // --- EQUIPMENT MANAGEMENT ---

    public Task<List<Equipment>> getUserEquipment(String userId) {
        return equipmentRepository.readAll()
            .continueWith(task -> {
                List<Equipment> allEquipment = task.getResult();
                return allEquipment.stream()
                    .filter(equipment -> userId.equals(equipment.getUserId()))
                    .collect(Collectors.toList());
            });
    }

    public Task<List<Equipment>> getActiveEquipment(String userId) {
        return getUserEquipment(userId)
            .continueWith(task -> {
                List<Equipment> userEquipment = task.getResult();
                return userEquipment.stream()
                    .filter(Equipment::isActive)
                    .collect(Collectors.toList());
            });
    }

    public Task<Boolean> activateEquipment(String equipmentId) {
        return equipmentRepository.read(equipmentId)
            .continueWithTask(task -> {
                Equipment equipment = task.getResult();
                if (equipment == null) {
                    throw new RuntimeException("Equipment not found");
                }

                if (!getCurrentUserId().equals(equipment.getUserId())) {
                    throw new RuntimeException("Permission denied");
                }

                equipment.setActive(true);

                // Special handling for clothing activation
                if (equipment instanceof Clothing) {
                    Clothing clothing = (Clothing) equipment;
                    clothing.setActive(true);
                }

                return equipmentRepository.update(equipment)
                    .continueWith(updateTask -> true);
            });
    }

    public Task<Boolean> deactivateEquipment(String equipmentId) {
        return equipmentRepository.read(equipmentId)
            .continueWithTask(task -> {
                Equipment equipment = task.getResult();
                if (equipment == null) {
                    throw new RuntimeException("Equipment not found");
                }

                if (!getCurrentUserId().equals(equipment.getUserId())) {
                    throw new RuntimeException("Permission denied");
                }

                equipment.setActive(false);
                return equipmentRepository.update(equipment)
                    .continueWith(updateTask -> true);
            });
    }

    // --- WEAPON UPGRADES ---

    public Task<Boolean> upgradeWeapon(String weaponId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new RuntimeException("User not authenticated"));
        }

        return Tasks.whenAllSuccess(
            equipmentRepository.read(weaponId),
            userRepository.read(userId)
        ).continueWithTask(task -> {
            List<Object> results = task.getResult();
            Equipment equipment = (Equipment) results.get(0);
            User user = (User) results.get(1);

            if (equipment == null || !(equipment instanceof Weapon)) {
                throw new RuntimeException("Weapon not found");
            }

            if (!userId.equals(equipment.getUserId())) {
                throw new RuntimeException("Permission denied");
            }

            Weapon weapon = (Weapon) equipment;
            int basePrice = 100; // Mock base price
            int upgradePrice = weapon.getUpgradePrice(basePrice);

            if (user.coins < upgradePrice) {
                throw new RuntimeException("Insufficient coins");
            }

            // Deduct coins and upgrade weapon
            user.coins -= upgradePrice;
            weapon.upgrade();

            return Tasks.whenAll(
                equipmentRepository.update(weapon),
                userRepository.update(user)
            ).continueWith(updateTask -> true);
        });
    }
}
