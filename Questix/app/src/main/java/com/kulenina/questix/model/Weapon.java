package com.kulenina.questix.model;

public class Weapon extends Equipment {
    public WeaponType weaponType;
    public double effectValue;
    public int upgradeLevel;

    public enum WeaponType {
        SWORD("Sword", 5.0),
        BOW("Bow and Arrow", 5.0);

        private final String displayName;
        private final double baseEffectValue;

        WeaponType(String displayName, double baseEffectValue) {
            this.displayName = displayName;
            this.baseEffectValue = baseEffectValue;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getBaseEffectValue() {
            return baseEffectValue;
        }
    }

    public Weapon() {
        super();
        this.type = EquipmentType.WEAPON;
        this.upgradeLevel = 0;
    }

    public Weapon(String userId, WeaponType weaponType) {
        super(userId, weaponType.getDisplayName(), EquipmentType.WEAPON);
        this.weaponType = weaponType;
        this.effectValue = weaponType.getBaseEffectValue();
        this.upgradeLevel = 0;
        this.isActive = true;
    }

    @Override
    public int getPrice(int baseBossReward) {
        // Weapons cannot be purchased, only obtained from boss battles
        return 0;
    }

    public static int getUpgradePrice(int baseBossReward) {
        return  Math.round((baseBossReward * 60) / 100.0f);
    }

    @Override
    public String getEffectDescription() {
        String effect = "";
        switch (weaponType) {
            case SWORD:
                effect = "Power +" + String.format("%.2f", effectValue) + "%";
                break;
            case BOW:
                effect = "Coin reward +" + String.format("%.2f", effectValue) + "%";
                break;
        }

        if (upgradeLevel > 0) {
            effect += " (Upgraded " + upgradeLevel + " times)";
        }

        return effect;
    }

    public WeaponType getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(WeaponType weaponType) {
        this.weaponType = weaponType;
        this.effectValue = weaponType.getBaseEffectValue();
        this.name = weaponType.getDisplayName();
    }

    public double getEffectValue() {
        return effectValue;
    }

    public int getUpgradeLevel() {
        return upgradeLevel;
    }

    public void upgrade() {
        this.upgradeLevel++;
        this.effectValue += 0.01;
    }

    public void combineWith(Weapon other) {
        if (other.weaponType == this.weaponType) {
            this.effectValue += 0.02;
        }
    }

    @Override
    public void useBattle() {
        return;
    }
}
