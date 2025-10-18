package com.kulenina.questix.model;

public class Clothing extends Equipment {
    public ClothingType clothingType;
    public int effectValue;
    public int remainingBattles;

    public enum ClothingType {
        GLOVES("Power Gloves", 10, 60),
        SHIELD("Shield", 10, 60),
        BOOTS("Speed Boots", 40, 80);

        private final String displayName;
        private final int effectValue;
        private final int pricePercentage;

        ClothingType(String displayName, int effectValue, int pricePercentage) {
            this.displayName = displayName;
            this.effectValue = effectValue;
            this.pricePercentage = pricePercentage;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getEffectValue() {
            return effectValue;
        }

        public int getPricePercentage() {
            return pricePercentage;
        }
    }

    public Clothing() {
        super();
        this.type = EquipmentType.CLOTHING;
        this.remainingBattles = 2;
    }

    public Clothing(String userId, ClothingType clothingType) {
        super(userId, clothingType.getDisplayName(), EquipmentType.CLOTHING);
        this.clothingType = clothingType;
        this.effectValue = clothingType.getEffectValue();
        this.remainingBattles = 2;
    }

    @Override
    public int getPrice(int baseBossReward) {
        return (baseBossReward * clothingType.getPricePercentage()) / 100;
    }

    @Override
    public String getEffectDescription() {
        String effect = "";
        switch (clothingType) {
            case GLOVES:
                effect = "Power +" + effectValue + "%";
                break;
            case SHIELD:
                effect = "Attack success +" + effectValue + "%";
                break;
            case BOOTS:
                effect = effectValue + "% chance for extra attack";
                break;
        }
        return effect + " (Lasts " + remainingBattles + " battles)";
    }

    public ClothingType getClothingType() {
        return clothingType;
    }

    public void setClothingType(ClothingType clothingType) {
        this.clothingType = clothingType;
        this.effectValue = clothingType.getEffectValue();
        this.name = clothingType.getDisplayName();
    }

    public int getEffectValue() {
        return effectValue;
    }

    public void setEffectValue(int effectValue) {
        this.effectValue = effectValue;
    }

    public int getRemainingBattles() {
        return remainingBattles;
    }

    public void setRemainingBattles(int remainingBattles) {
        this.remainingBattles = remainingBattles;
    }

    @Override
    public void useBattle() {
        this.remainingBattles--;

        if (this.remainingBattles <= 0) {
            this.isExpired = true;
        }
    }

    public void combineWith(Clothing other) {
        if (other.clothingType == this.clothingType) {
            this.effectValue += other.effectValue;
            this.remainingBattles += other.remainingBattles;
            if (this.remainingBattles > 0) {
                this.isExpired = false;
            }
        }
    }
}
