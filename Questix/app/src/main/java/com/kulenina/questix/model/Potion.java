package com.kulenina.questix.model;

public class Potion extends Equipment {
    public PotionType potionType;
    public int effectValue;
    public boolean isPermanent;

    public enum PotionType {
        SINGLE_POWER_20("Elixir of Vigor", 20, false, 50),
        SINGLE_POWER_40("Elixir of Might", 40, false, 70),
        PERMANENT_POWER_5("Essence of Growth", 5, true, 200),
        PERMANENT_POWER_10("Essence of Ascension", 10, true, 1000);

        private final String displayName;
        private final int effectValue;
        private final boolean isPermanent;
        private final int pricePercentage;

        PotionType(String displayName, int effectValue, boolean isPermanent, int pricePercentage) {
            this.displayName = displayName;
            this.effectValue = effectValue;
            this.isPermanent = isPermanent;
            this.pricePercentage = pricePercentage;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getEffectValue() {
            return effectValue;
        }

        public boolean isPermanent() {
            return isPermanent;
        }

        public int getPricePercentage() {
            return pricePercentage;
        }
    }

    public Potion() {
        super();
        this.type = EquipmentType.POTION;
    }

    public Potion(String userId, PotionType potionType) {
        super(userId, potionType.getDisplayName(), EquipmentType.POTION);
        this.potionType = potionType;
        this.effectValue = potionType.getEffectValue();
        this.isPermanent = potionType.isPermanent();
    }

    @Override
    public int getPrice(int baseBossReward) {
        return Math.round((baseBossReward * potionType.getPricePercentage()) / 100.0f);
    }

    @Override
    public String getEffectDescription() {
        String duration = isPermanent ? "Permanent" : "Single use";
        return duration + " power increase of " + effectValue + "%";
    }

    public PotionType getPotionType() {
        return potionType;
    }

    public void setPotionType(PotionType potionType) {
        this.potionType = potionType;
        this.effectValue = potionType.getEffectValue();
        this.isPermanent = potionType.isPermanent();
        this.name = potionType.getDisplayName();
    }

    public int getEffectValue() {
        return effectValue;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    @Override
    public void useBattle() {
        if (isPermanent)
            return;

        this.isExpired = true;
    }
}
