package id.cyayo.forge.config;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.model.*;
import id.cyayo.forge.util.RegionUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class RecipeConfig {

    private final CyayoForge plugin;
    // ForgeType -> TreeMap sorted by min material count ascending
    private final Map<ForgeType, TreeMap<Integer, ForgeRecipe>> recipes = new HashMap<>();

    public RecipeConfig(CyayoForge plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        recipes.clear();
        File file = new File(plugin.getDataFolder(), "recipes.yml");
        if (!file.exists()) plugin.saveResource("recipes.yml", false);

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (ForgeType type : ForgeType.values()) {
            recipes.put(type, new TreeMap<>());
            ConfigurationSection typeSec = config.getConfigurationSection(type.name().toLowerCase());
            if (typeSec == null) continue;

            for (String keyStr : typeSec.getKeys(false)) {
                try {
                    ConfigurationSection recipeSec = typeSec.getConfigurationSection(keyStr);
                    if (recipeSec == null) continue;

                    int minCount = recipeSec.getInt("recipe", 5);
                    String outputType = recipeSec.getString("type", keyStr.toUpperCase());
                    String categoryPermission = recipeSec.getString("permission", null);
                    List<String> categoryRegions = recipeSec.getStringList("regions");

                    List<OutputTemplate> outputs = new ArrayList<>();
                    ConfigurationSection outputsSec = recipeSec.getConfigurationSection("outputs");
                    if (outputsSec != null) {
                        for (String key : outputsSec.getKeys(false)) {
                            ConfigurationSection outSec = outputsSec.getConfigurationSection(key);
                            if (outSec == null) continue;

                            String id = outSec.getString("id", key);
                            int base = outSec.getInt("base", 1);
                            String permission = outSec.getString("permission", null);
                            List<String> regions = outSec.getStringList("regions");

                            Map<ForgeTier, String> templates = new EnumMap<>(ForgeTier.class);
                            ConfigurationSection tmplSec = outSec.getConfigurationSection("templates");
                            if (tmplSec != null) {
                                for (ForgeTier tier : ForgeTier.values()) {
                                    String tmplId = tmplSec.getString(tier.name());
                                    if (tmplId != null) templates.put(tier, tmplId);
                                }
                            }
                            outputs.add(new OutputTemplate(id, base, permission, regions, templates));
                        }
                    }

                    // Sort by base descending (rarest first), base=1 jadi fallback di akhir
                    outputs.sort((a, b) -> Integer.compare(b.getBase(), a.getBase()));

                    recipes.get(type).put(minCount, new ForgeRecipe(outputType, minCount, type, categoryPermission, categoryRegions, outputs));

                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Gagal memuat resep: " + type + "/" + keyStr, e);
                }
            }
        }

        int total = recipes.values().stream().mapToInt(Map::size).sum();
        plugin.getLogger().info("Berhasil memuat " + total + " resep tempa.");
    }

    /**
     * Temukan resep berdasarkan jumlah material dengan sistem RENTANG.
     * Misal: recipes di 5, 8, 12 → 6 material = resep 5 (DAGGER), 9 = resep 8 (KATANA)
     * Mengembalikan resep dengan min tertinggi yang <= materialCount.
     */
    public ForgeRecipe getRecipe(ForgeType type, int materialCount) {
        return getRecipe(type, materialCount, null);
    }

    public ForgeRecipe getRecipe(ForgeType type, int materialCount, org.bukkit.entity.Player player) {
        TreeMap<Integer, ForgeRecipe> map = recipes.get(type);
        if (map == null || map.isEmpty()) return null;
        
        // Cari semua resep yang minCount <= materialCount, urutkan dari yang terbesar (terdekat ke materialCount)
        NavigableMap<Integer, ForgeRecipe> subMap = map.headMap(materialCount, true).descendingMap();
        
        for (ForgeRecipe r : subMap.values()) {
            // Cek permission kategori & region
            String perm = r.getPermission();
            boolean hasPerm = perm == null || perm.isEmpty() || (player != null && player.hasPermission(perm));
            boolean inRegion = player != null && RegionUtil.isInAnyRegion(player, r.getRegions());

            if (!hasPerm && !inRegion) {
                continue;
            }
            return r;
        }
        
        return null;
    }

    public ForgeRecipe getRecipeByOutputType(String outputType) {
        for (TreeMap<Integer, ForgeRecipe> map : recipes.values()) {
            for (ForgeRecipe recipe : map.values()) {
                if (recipe.getOutputType().equalsIgnoreCase(outputType)) return recipe;
            }
        }
        return null;
    }

    public List<ForgeRecipe> getRecipes(ForgeType type) {
        TreeMap<Integer, ForgeRecipe> map = recipes.get(type);
        if (map == null) return Collections.emptyList();
        return new ArrayList<>(map.values());
    }
}
