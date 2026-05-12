package id.cyayo.forge.model;

public enum ForgeQuality {
    BROKEN,
    FLAWED,
    GOOD,
    EXQUISITE,
    PERFECT;

    public static ForgeQuality fromString(String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (Exception e) {
            return GOOD;
        }
    }
}
