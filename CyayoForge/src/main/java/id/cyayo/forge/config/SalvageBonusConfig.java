package id.cyayo.forge.config;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.model.ForgeTier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class SalvageBonusConfig {

    private final CyayoForge plugin;
    private final Map<ForgeTier, BonusData> bonuses = new HashMap<>();
    private boolean enabled = true;

    public SalvageBonusConfig(CyayoForge plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        bonuses.clear();
        File file = new File(plugin.getDataFolder(), "salvage_bonus.yml");
        if (!file.exists()) plugin.saveResource("salvage_bonus.yml", false);

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.enabled = config.getBoolean("enabled", true);
        
        ConfigurationSection root = config.getConfigurationSection("bonus");
        if (root == null) return;

        for (String tierStr : root.getKeys(false)) {
            try {
                ForgeTier tier = ForgeTier.valueOf(tierStr.toUpperCase());
                ConfigurationSection sec = root.getConfigurationSection(tierStr);
                if (sec == null) continue;

                double chance = sec.getDouble("chance", 100.0);
                List<BonusItem> items = new ArrayList<>();
                
                List<Map<?, ?>> itemMaps = sec.getMapList("items");
                for (Map<?, ?> map : itemMaps) {
                    String typeName = map.containsKey("type") ? (String) map.get("type") : "VANILLA";
                    BonusType type = BonusType.valueOf(typeName.toUpperCase());
                    String id = (String) map.get("id");
                    String mmoType = (String) map.get("mmo_type");
                    String mat = (String) map.get("material");
                    String cmd = (String) map.get("command");
                    String display = (String) map.get("display");
                    
                    int min = 1;
                    if (map.containsKey("min")) min = ((Number) map.get("min")).intValue();
                    int max = 1;
                    if (map.containsKey("max")) max = ((Number) map.get("max")).intValue();
                    
                    items.add(new BonusItem(type, id, mmoType, mat, cmd, display, min, max));
                }

                bonuses.put(tier, new BonusData(chance, items));
            } catch (Exception ignored) {}
        }
    }

    public boolean isEnabled() { return enabled; }
    public BonusData getBonus(ForgeTier tier) {
        return bonuses.get(tier);
    }

    public enum BonusType { MMOITEMS, VANILLA, COMMAND }

    public static class BonusData {
        private final double chance;
        private final List<BonusItem> items;

        public BonusData(double chance, List<BonusItem> items) {
            this.chance = chance;
            this.items = items;
        }

        public double getChance() { return chance; }
        public List<BonusItem> getItems() { return items; }
    }

    public static class BonusItem {
        private final BonusType type;
        private final String id, mmoType, material, command, display;
        private final int min, max;

        public BonusItem(BonusType type, String id, String mmoType, String material, String command, String display, int min, int max) {
            this.type = type;
            this.id = id;
            this.mmoType = mmoType;
            this.material = material;
            this.command = command;
            this.display = display;
            this.min = min;
            this.max = max;
        }

        public BonusType getType() { return type; }
        public String getId() { return id; }
        public String getMmoType() { return mmoType; }
        public String getMaterial() { return material; }
        public String getCommand() { return command; }
        public String getDisplay() { return display; }
        public int getMin() { return min; }
        public int getMax() { return max; }

        public int rollAmount() {
            if (min >= max) return min;
            return min + new Random().nextInt(max - min + 1);
        }
    }
}
