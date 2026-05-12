package id.cyayo.forge.model;

public enum ForgeType {
    WEAPON,
    ARMOR;

    public static ForgeType fromString(String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (Exception e) {
            return WEAPON;
        }
    }
}
