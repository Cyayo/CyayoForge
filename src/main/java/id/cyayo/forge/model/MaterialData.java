package id.cyayo.forge.model;

import java.util.List;
import java.util.Map;

public class MaterialData {

    public enum MaterialType { MMOITEMS, VANILLA }

    /** Tipe stat tambahan yang bisa diinjeksi */
    public enum StatMode { FIXED, MULTIPLY, RANDOM }

    /** Satu stat tambahan yang akan diinjeksi ke output */
    public static class ExtraStat {
        private final String statKey;   // Kunci stat MMOItems, e.g. "ATTACK_SPEED", "CRITICAL_STRIKE_CHANCE"
        private final StatMode mode;    // FIXED, MULTIPLY, atau RANDOM
        private final double value;     // Nilai untuk FIXED/MULTIPLY
        private final double min;       // Min untuk RANDOM
        private final double max;       // Max untuk RANDOM

        public ExtraStat(String statKey, StatMode mode, double value, double min, double max) {
            this.statKey = statKey;
            this.mode    = mode;
            this.value   = value;
            this.min     = min;
            this.max     = max;
        }

        public String getStatKey() { return statKey; }
        public StatMode getMode()  { return mode; }

        /** Hitung nilai akhir berdasarkan mode */
        public double resolve(double baseValue) {
            return switch (mode) {
                case FIXED    -> value;
                case MULTIPLY -> baseValue * value;
                case RANDOM   -> min + (Math.random() * (max - min));
            };
        }

        /** Untuk FIXED/MULTIPLY tanpa base */
        public double resolveRaw() {
            if (mode == StatMode.RANDOM) return min + (Math.random() * (max - min));
            return value;
        }

        public double getValue() { return value; }
        public double getMin()   { return min; }
        public double getMax()   { return max; }
    }

    private final String id;
    private final MaterialType type;
    private final String mmoitemsType;

    // Stats dasar (multiplier)
    private final double weaponDamageMultiplier;
    private final double armorHealthMultiplier;
    private final double armorDefenseMultiplier;
    private final double armorMultiplier;
    private final double critDamageMultiplier;

    // Stats tambahan fleksibel (injeksi ke output)
    private final List<ExtraStat> weaponExtraStats;
    private final List<ExtraStat> armorExtraStats;

    // Abilities
    private final List<AbilityModifier> weaponAbilities;

    private final boolean forgeable;

    public MaterialData(String id, MaterialType type, String mmoitemsType,
                        double weaponDamageMultiplier,
                        double armorHealthMultiplier,
                        double armorDefenseMultiplier,
                        double armorMultiplier,
                        double critDamageMultiplier,
                        List<ExtraStat> weaponExtraStats,
                        List<ExtraStat> armorExtraStats,
                        List<AbilityModifier> weaponAbilities,
                        boolean forgeable) {
        this.id                     = id;
        this.type                   = type;
        this.mmoitemsType           = mmoitemsType;
        this.weaponDamageMultiplier = weaponDamageMultiplier;
        this.armorHealthMultiplier  = armorHealthMultiplier;
        this.armorDefenseMultiplier = armorDefenseMultiplier;
        this.armorMultiplier        = armorMultiplier;
        this.critDamageMultiplier   = critDamageMultiplier;
        this.weaponExtraStats       = weaponExtraStats;
        this.armorExtraStats        = armorExtraStats;
        this.weaponAbilities        = weaponAbilities;
        this.forgeable              = forgeable;
    }

    public String getId()                          { return id; }
    public MaterialType getType()                  { return type; }
    public String getMmoitemsType()                { return mmoitemsType; }
    public double getWeaponDamageMultiplier()      { return weaponDamageMultiplier; }
    public double getArmorHealthMultiplier()       { return armorHealthMultiplier; }
    public double getArmorDefenseMultiplier()      { return armorDefenseMultiplier; }
    public double getArmorMultiplier()             { return armorMultiplier; }
    public double getCritDamageMultiplier()        { return critDamageMultiplier; }
    public boolean hasDefenseMultiplier()          { return armorDefenseMultiplier > 0; }
    public List<ExtraStat> getWeaponExtraStats()   { return weaponExtraStats; }
    public List<ExtraStat> getArmorExtraStats()    { return armorExtraStats; }
    public List<AbilityModifier> getWeaponAbilities() { return weaponAbilities; }
    public boolean hasWeaponAbilities()            { return weaponAbilities != null && !weaponAbilities.isEmpty(); }
    public boolean hasWeaponExtraStats()           { return weaponExtraStats != null && !weaponExtraStats.isEmpty(); }
    public boolean hasArmorExtraStats()            { return armorExtraStats != null && !armorExtraStats.isEmpty(); }
    public boolean isForgeable()                   { return forgeable; }
}
