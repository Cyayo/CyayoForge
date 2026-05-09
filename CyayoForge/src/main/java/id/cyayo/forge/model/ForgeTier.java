package id.cyayo.forge.model;

public enum ForgeTier {
    POOR(1),
    GOOD(2),
    GREAT(3),
    ANCIENT(4),
    RELIC(5),
    SECRET(6);

    private final int level;

    ForgeTier(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    public static ForgeTier fromString(String s) {
        if (s == null) return POOR;
        try {
            return valueOf(s.toUpperCase());
        } catch (Exception e) {
            return POOR;
        }
    }
}
