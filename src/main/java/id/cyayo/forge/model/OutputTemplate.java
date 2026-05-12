package id.cyayo.forge.model;

import java.util.Map;

public class OutputTemplate {
    private final String id;
    private final int base;
    private final String permission; // null = no permission required
    private final java.util.List<String> regions;
    private final Map<ForgeTier, String> templates;

    public OutputTemplate(String id, int base, String permission, java.util.List<String> regions, Map<ForgeTier, String> templates) {
        this.id = id;
        this.base = base;
        this.permission = permission;
        this.regions = regions;
        this.templates = templates;
    }

    public String getId() { return id; }
    public int getBase() { return base; }

    /**
     * Returns the permission required to obtain this output.
     * Null or empty means no permission required (default).
     */
    public String getPermission() { return permission; }
    public boolean hasPermission() { return permission != null && !permission.isEmpty(); }
    public java.util.List<String> getRegions() { return regions; }

    public String getTemplateId(ForgeTier tier) {
        return templates.getOrDefault(tier, templates.get(ForgeTier.POOR));
    }

    public Map<ForgeTier, String> getTemplates() { return templates; }

    public boolean roll() {
        if (base <= 1) return true;
        return (int)(Math.random() * base) + 1 == 1;
    }
}
