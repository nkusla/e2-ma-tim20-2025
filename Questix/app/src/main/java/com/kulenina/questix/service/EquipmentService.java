package com.kulenina.questix.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.kulenina.questix.model.*;
import com.kulenina.questix.repository.EquipmentRepository;
import com.kulenina.questix.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EquipmentService {
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final FirebaseAuth auth;

    public EquipmentService() {
        this.equipmentRepository = new EquipmentRepository();
        this.userRepository = new UserRepository();
        this.auth = FirebaseAuth.getInstance();
    }

    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public Task<List<Equipment>> getShopItems() {
        List<Equipment> shopItems = new ArrayList<>();

        for (Potion.PotionType potionType : Potion.PotionType.values()) {
            Potion potion = new Potion(getCurrentUserId(), potionType);
            potion.setId("shop_" + potionType.name());
            shopItems.add(potion);
        }

        for (Clothing.ClothingType clothingType : Clothing.ClothingType.values()) {
            Clothing clothing = new Clothing(getCurrentUserId(), clothingType);
            clothing.setId("shop_" + clothingType.name());
            shopItems.add(clothing);
        }

        return Tasks.forResult(shopItems);
    }

    public Task<Boolean> purchaseEquipment(Equipment.EquipmentType equipmentType, String itemType) {
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
            int bossRewardPrice = BossBattle.calculateCoinsReward(user.bossLevel);
            int price = 0;

            if (equipmentType == Equipment.EquipmentType.POTION) {
                Potion.PotionType potionType = Potion.PotionType.valueOf(itemType);
                equipment = new Potion(userId, potionType);
                price = equipment.getPrice(bossRewardPrice);
            } else if (equipmentType == Equipment.EquipmentType.CLOTHING) {
                Clothing.ClothingType clothingType = Clothing.ClothingType.valueOf(itemType);
                equipment = new Clothing(userId, clothingType);
                price = equipment.getPrice(bossRewardPrice);

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

            user.coins -= price;

            return equipmentRepository.create(equipment)
                .continueWithTask(createTask -> userRepository.update(user))
                .continueWith(updateTask -> true);
        });
    }

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
            int basePrice = BossBattle.calculateCoinsReward(user.bossLevel);
            int upgradePrice = Weapon.getUpgradePrice(basePrice);

            if (user.coins < upgradePrice) {
                throw new RuntimeException("Insufficient coins");
            }

            user.coins -= upgradePrice;
            weapon.upgrade();

            return Tasks.whenAll(
                equipmentRepository.update(weapon),
                userRepository.update(user)
            ).continueWith(updateTask -> true);
        });
    }

    // Register equipment drop from boss battles, handling combining with existing equipment
    public Task<Boolean> registerEquipmentDrop(Equipment droppedEquipment) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Tasks.forException(new RuntimeException("User not authenticated"));
        }

        if (droppedEquipment == null) {
            return Tasks.forResult(true); // No equipment dropped
        }

        return getUserEquipment(userId)
            .continueWithTask(task -> {
                List<Equipment> userEquipment = task.getResult();

                // Check for existing equipment of the same type
                Equipment existingEquipment = null;

                if (droppedEquipment instanceof Clothing) {
                    Clothing droppedClothing = (Clothing) droppedEquipment;
                    for (Equipment eq : userEquipment) {
                        if (eq instanceof Clothing) {
                            Clothing clothing = (Clothing) eq;
                            if (clothing.getClothingType() == droppedClothing.getClothingType() && !clothing.isExpired) {
                                existingEquipment = clothing;
                                break;
                            }
                        }
                    }
                } else if (droppedEquipment instanceof Weapon) {
                    Weapon droppedWeapon = (Weapon) droppedEquipment;
                    for (Equipment eq : userEquipment) {
                        if (eq instanceof Weapon) {
                            Weapon weapon = (Weapon) eq;
                            if (weapon.getWeaponType() == droppedWeapon.getWeaponType()) {
                                existingEquipment = weapon;
                                break;
                            }
                        }
                    }
                }

                // If existing equipment found, combine with it
                if (existingEquipment != null) {
                    if (existingEquipment instanceof Clothing && droppedEquipment instanceof Clothing) {
                        ((Clothing) existingEquipment).combineWith((Clothing) droppedEquipment);
                    } else if (existingEquipment instanceof Weapon && droppedEquipment instanceof Weapon) {
                        ((Weapon) existingEquipment).combineWith((Weapon) droppedEquipment);
                    }

                    // Update the existing equipment
                    return equipmentRepository.update(existingEquipment)
                        .continueWith(updateTask -> true);
                } else {
                    // Create new equipment
                    return equipmentRepository.create(droppedEquipment)
                        .continueWith(createTask -> true);
                }
            });
    }
}
