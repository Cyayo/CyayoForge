package id.cyayo.forge.config;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.model.AbilityModifier;
import id.cyayo.forge.model.MaterialData;
import id.cyayo.forge.model.MaterialData.ExtraStat;
import id.cyayo.forge.model.MaterialData.StatMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class MaterialConfig {

    private final CyayoForge plugin;
    private final Map<String, MaterialData> materials     = new HashMap<>();
    private final Map<String, List<MaterialData>> byType  = new HashMap<>();

    public MaterialConfig(CyayoForge plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        materials.clear();
        byType.clear();

        File file = new File(plugin.getDataFolder(), "materials.yml");
        if (!file.exists()) plugin.saveResource("materials.yml", false);

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("materials");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            try {
                ConfigurationSection sec = root.getConfigurationSection(id);
                if (sec == null) continue;

                String typeStr = sec.getString("type", "MMOITEMS").toUpperCase();
                MaterialData.MaterialType type = MaterialData.MaterialType.valueOf(typeStr);
                String mmoType = sec.getString("mmoitems_type", null);
                if (mmoType != null) mmoType = mmoType.toUpperCase();

                ConfigurationSection baseSec = sec.getConfigurationSection("base");
                double weaponDmgMult = baseSec != null ? baseSec.getDouble("damage_multiplier", 1.0) : 1.0;
                double armorHpMult   = baseSec != null ? baseSec.getDouble("health_multiplier", 1.0) : 1.0;
                double armorDefMult  = baseSec != null ? baseSec.getDouble("defense_multiplier", 0.0) : 0.0;
                double armorMult     = baseSec != null ? baseSec.getDouble("armor_multiplier", 1.0) : 1.0;
                double critDmgMult   = baseSec != null ? baseSec.getDouble("critical_damage_multiplier", 1.0) : 1.0;

                // ── Weapon ──
                List<ExtraStat>      weaponExtra    = new ArrayList<>();
                List<AbilityModifier> weaponAbilities = new ArrayList<>();

                ConfigurationSection weapSec = sec.getConfigurationSection("weapon");
                if (weapSec != null) {
                    weaponExtra    = parseExtraStats(weapSec.getConfigurationSection("stats"));
                    weaponAbilities = parseAbilities(weapSec);
                }

                // ── Armor ──
                List<ExtraStat> armorExtra = new ArrayList<>();

                ConfigurationSection armSec = sec.getConfigurationSection("armor");
                if (armSec != null) {
                    armorExtra   = parseExtraStats(armSec.getConfigurationSection("stats"));
                }

                boolean forgeable = sec.getBoolean("forgeable", true);

                MaterialData data = new MaterialData(
                        id.toUpperCase(), type, mmoType,
                        weaponDmgMult, armorHpMult, armorDefMult,
                        armorMult, critDmgMult,
                        weaponExtra, armorExtra, weaponAbilities,
                        forgeable
                );

                materials.put(id.toUpperCase(), data);
                if (mmoType != null)
                    byType.computeIfAbsent(mmoType, k -> new ArrayList<>()).add(data);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Gagal memuat material: " + id, e);
            }
        }

        plugin.getLogger().info("Berhasil memuat " + materials.size() + " material.");
    }

    // ─────────────────────────────────────────────
    // Parse extra stats section
    // ─────────────────────────────────────────────

    private List<ExtraStat> parseExtraStats(ConfigurationSection statsSec) {
        List<ExtraStat> list = new ArrayList<>();
        if (statsSec == null) return list;

        for (String key : statsSec.getKeys(false)) {
            try {
                ConfigurationSection s = statsSec.getConfigurationSection(key);
                if (s == null) {
                    // Format singkat: ATTACK_SPEED: 1.5
                    double val = statsSec.getDouble(key, 0);
                    if (val != 0) list.add(new ExtraStat(key.toUpperCase(), StatMode.FIXED, val, 0, 0));
                    continue;
                }

                String modeStr = s.getString("mode", "FIXED").toUpperCase();
                StatMode mode  = StatMode.valueOf(modeStr);

                ExtraStat stat = switch (mode) {
                    case FIXED, MULTIPLY -> new ExtraStat(key.toUpperCase(), mode,
                            s.getDouble("value", 0), 0, 0);
                    case RANDOM          -> new ExtraStat(key.toUpperCase(), mode, 0,
                            s.getDouble("min", 0), s.getDouble("max", 0));
                };
                list.add(stat);

            } catch (Exception e) {
                plugin.getLogger().warning("Gagal memuat stat '" + key + "': " + e.getMessage());
            }
        }
        return list;
    }

    // ─────────────────────────────────────────────
    // Parse abilities list
    // ─────────────────────────────────────────────

    private List<AbilityModifier> parseAbilities(ConfigurationSection weapSec) {
        List<AbilityModifier> list = new ArrayList<>();
        List<?> abilitiesList = weapSec.getList("abilities");
        if (abilitiesList == null) return list;

        for (Object obj : abilitiesList) {
            if (!(obj instanceof Map<?, ?> abMap)) continue;
            try {
                String abilityId = (String) abMap.get("ability");
                Object trigObj   = abMap.get("trigger");
                String trigger   = trigObj instanceof String ? (String) trigObj : "RIGHT_CLICK";
                if (abilityId == null || abilityId.isEmpty()) continue;

                Map<String, double[]> parsedMods = new HashMap<>();
                Object modsObj = abMap.get("modifiers");
                if (modsObj instanceof Map<?, ?> mods) {
                    for (Map.Entry<?, ?> entry : mods.entrySet()) {
                        String modName = String.valueOf(entry.getKey());
                        if (entry.getValue() instanceof Map<?, ?> m) {
                            double min = toDouble(m.get("min"));
                            double max = toDouble(m.get("max"));
                            parsedMods.put(modName, new double[]{min, max});
                        } else if (entry.getValue() instanceof Number n) {
                            parsedMods.put(modName, new double[]{n.doubleValue(), n.doubleValue()});
                        }
                    }
                }
                list.add(new AbilityModifier(abilityId, trigger, parsedMods));
            } catch (Exception e) {
                plugin.getLogger().warning("Gagal memuat ability: " + e.getMessage());
            }
        }
        return list;
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        if (obj instanceof String s) { try { return Double.parseDouble(s); } catch (Exception ignored) {} }
        return 0;
    }

    // ─────────────────────────────────────────────
    // Lookup methods
    // ─────────────────────────────────────────────

    public MaterialData getMaterial(String id) {
        return id != null ? materials.get(id.toUpperCase()) : null;
    }

    public MaterialData getMaterialFromNBT(String mmoItemId, String mmoItemType) {
        if (mmoItemId != null && !mmoItemId.isEmpty()) {
            MaterialData d = materials.get(mmoItemId.toUpperCase());
            if (d != null) return d;
        }
        if (mmoItemType != null) {
            List<MaterialData> list = byType.get(mmoItemType.toUpperCase());
            if (list != null && !list.isEmpty()) return list.get(0);
        }
        return null;
    }

    public boolean isMaterial(String id)     { return id != null && materials.containsKey(id.toUpperCase()); }
    public boolean isMaterialType(String t)  { return t != null && byType.containsKey(t.toUpperCase()); }
    public Map<String, MaterialData> getAllMaterials() { return Collections.unmodifiableMap(materials); }

    public org.bukkit.inventory.ItemStack createMaterial(String id, int amount) {
        MaterialData data = getMaterial(id);
        if (data == null) {
            // Coba vanilla
            try {
                org.bukkit.Material mat = org.bukkit.Material.valueOf(id.toUpperCase());
                return new org.bukkit.inventory.ItemStack(mat, amount);
            } catch (Exception e) {
                return null;
            }
        }

        if (data.getType() == MaterialData.MaterialType.MMOITEMS) {
            try {
                // Gunakan MMOItems API - Versi standar biasanya hanya butuh Type dan ID
                org.bukkit.inventory.ItemStack item = net.Indyuce.mmoitems.MMOItems.plugin.getItem(
                    net.Indyuce.mmoitems.api.Type.get(data.getMmoitemsType()), 
                    data.getId()
                );
                if (item != null) item.setAmount(amount);
                return item;
            } catch (Exception e) {
                plugin.getLogger().warning("Gagal membuat item MMOItems: " + data.getMmoitemsType() + ":" + data.getId());
                return null;
            }
        } else {
            try {
                org.bukkit.Material mat = org.bukkit.Material.valueOf(data.getId());
                return new org.bukkit.inventory.ItemStack(mat, amount);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
