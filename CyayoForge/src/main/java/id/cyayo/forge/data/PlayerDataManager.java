package id.cyayo.forge.data;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.model.ForgeType;
import id.cyayo.forge.model.SalvageTask;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class PlayerDataManager {

    private final CyayoForge plugin;
    private final Map<UUID, PlayerForgeData> dataMap = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public PlayerDataManager(CyayoForge plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        dataMap.clear();
        dataFile = new File(plugin.getDataFolder(), "data/players.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Cannot create players.yml", e);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        for (String uuidStr : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                int total = dataConfig.getInt(uuidStr + ".total", 0);
                int weapon = dataConfig.getInt(uuidStr + ".weapon", 0);
                int armor = dataConfig.getInt(uuidStr + ".armor", 0);
                String lastForged = dataConfig.getString(uuidStr + ".last_forged", "None");
                
                PlayerForgeData data = new PlayerForgeData(uuid, total, weapon, armor, lastForged);
                
                // Load Salvage Tasks
                ConfigurationSection salvageSec = dataConfig.getConfigurationSection(uuidStr + ".salvage_tasks");
                if (salvageSec != null) {
                    for (String slotKey : salvageSec.getKeys(false)) {
                        try {
                            int slotIndex = Integer.parseInt(slotKey);
                            ItemStack originalItem = salvageSec.getItemStack(slotKey + ".item");
                            long startTime = salvageSec.getLong(slotKey + ".start_time");
                            long endTime = salvageSec.getLong(slotKey + ".end_time");
                            List<Map<String, Object>> materials = (List<Map<String, Object>>) salvageSec.getList(slotKey + ".materials");
                            
                            if (originalItem != null) {
                                SalvageTask task = new SalvageTask(slotIndex, originalItem, materials, startTime, endTime);
                                data.addSalvageTask(task);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Gagal memuat salvage task untuk " + uuidStr + " slot " + slotKey);
                        }
                    }
                }
                
                dataMap.put(uuid, data);
            } catch (Exception ignored) {}
        }
    }

    public PlayerForgeData getData(UUID uuid) {
        return dataMap.computeIfAbsent(uuid, PlayerForgeData::new);
    }

    public void recordForge(UUID uuid, ForgeType type, String itemName) {
        PlayerForgeData data = getData(uuid);
        data.incrementTotal();
        if (type == ForgeType.WEAPON) data.incrementWeapon();
        else data.incrementArmor();
        data.setLastForged(itemName);
        save(uuid);
    }

    public void resetPlayer(UUID uuid) {
        dataMap.put(uuid, new PlayerForgeData(uuid));
        save(uuid);
    }

    public void resetAll() {
        dataMap.clear();
        dataConfig = new YamlConfiguration();
        saveFile();
    }

    public void save(UUID uuid) {
        PlayerForgeData data = dataMap.get(uuid);
        if (data == null) return;
        String path = uuid.toString();
        dataConfig.set(path + ".total", data.getTotal());
        dataConfig.set(path + ".weapon", data.getWeapon());
        dataConfig.set(path + ".armor", data.getArmor());
        dataConfig.set(path + ".last_forged", data.getLastForged());
        
        // Save Salvage Tasks
        dataConfig.set(path + ".salvage_tasks", null); // Clear existing
        for (SalvageTask task : data.getSalvageTasks()) {
            String slotPath = path + ".salvage_tasks." + task.getSlotIndex();
            dataConfig.set(slotPath + ".item", task.getOriginalItem());
            dataConfig.set(slotPath + ".start_time", task.getStartTime());
            dataConfig.set(slotPath + ".end_time", task.getEndTime());
            dataConfig.set(slotPath + ".materials", task.getMaterials());
        }
        
        saveFile();
    }

    public void saveAll() {
        for (UUID uuid : dataMap.keySet()) {
            save(uuid);
        }
    }

    private void saveFile() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Cannot save players.yml", e);
        }
    }
}
