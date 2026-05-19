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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class PlayerDataManager {

    private final CyayoForge plugin;
    private final Map<UUID, PlayerForgeData> dataMap = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;
    private File historyFile;
    private FileConfiguration historyConfig;

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

        historyFile = new File(plugin.getDataFolder(), "data/history.yml");
        if (!historyFile.exists()) {
            try { historyFile.createNewFile(); } catch (IOException ignored) {}
        }
        historyConfig = YamlConfiguration.loadConfiguration(historyFile);

        for (String uuidStr : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                int total = dataConfig.getInt(uuidStr + ".total", 0);
                int weapon = dataConfig.getInt(uuidStr + ".weapon", 0);
                int armor = dataConfig.getInt(uuidStr + ".armor", 0);
                
                List<ItemStack> history = new ArrayList<>();
                
                // MIGRATION & LOADING LOGIC
                // 1. Coba baca dari history.yml (lokasi baru)
                List<?> historyList = historyConfig.getList(uuidStr + ".forge_history");
                
                // 2. Jika tidak ada, coba baca dari players.yml (lokasi lama)
                if (historyList == null) {
                    historyList = dataConfig.getList(uuidStr + ".forge_history");
                }
                
                if (historyList != null) {
                    for (Object obj : historyList) {
                        if (obj instanceof ItemStack item) {
                            history.add(item);
                        } else if (obj instanceof String base64) {
                            ItemStack parsed = itemStackFromBase64(base64);
                            if (parsed != null) history.add(parsed);
                        } else if (obj instanceof Map) {
                            try {
                                history.add(ItemStack.deserialize((Map<String, Object>) obj));
                            } catch (Exception ignored) {}
                        }
                    }
                }
                
                PlayerForgeData data = new PlayerForgeData(uuid, total, weapon, armor, history);
                
                // Load Salvage Tasks
                ConfigurationSection salvageSec = dataConfig.getConfigurationSection(uuidStr + ".salvage_tasks");
                if (salvageSec != null) {
                    for (String slotKey : salvageSec.getKeys(false)) {
                        try {
                            int slotIndex = Integer.parseInt(slotKey);
                            ItemStack originalItem = null;
                            if (salvageSec.isString(slotKey + ".item")) {
                                originalItem = itemStackFromBase64(salvageSec.getString(slotKey + ".item"));
                            } else if (salvageSec.isItemStack(slotKey + ".item")) {
                                originalItem = salvageSec.getItemStack(slotKey + ".item");
                            }
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

    public void recordForge(UUID uuid, ForgeType type, ItemStack item) {
        PlayerForgeData data = getData(uuid);
        data.incrementTotal();
        if (type == ForgeType.WEAPON) data.incrementWeapon();
        else data.incrementArmor();
        
        int maxHistory = plugin.getHistoryMenuConfig().getConfig().getInt("max-history", 5);
        data.addForgeHistory(item, maxHistory);
        save(uuid);
    }

    public void resetPlayer(UUID uuid) {
        dataMap.put(uuid, new PlayerForgeData(uuid));
        save(uuid);
    }

    public void resetAll() {
        dataMap.clear();
        dataConfig = new YamlConfiguration();
        historyConfig = new YamlConfiguration();
        saveFiles();
    }

    public void save(UUID uuid) {
        PlayerForgeData data = dataMap.get(uuid);
        if (data == null) return;
        String path = uuid.toString();
        dataConfig.set(path + ".total", data.getTotal());
        dataConfig.set(path + ".weapon", data.getWeapon());
        dataConfig.set(path + ".armor", data.getArmor());
        dataConfig.set(path + ".forge_history", null); // Hapus dari players.yml jika ada
        
        // Save to history.yml using Base64 to prevent YAML serialization loss
        List<String> b64History = new ArrayList<>();
        for (ItemStack item : data.getForgeHistory()) {
            if (item == null) continue;
            String b64 = itemStackToBase64(item);
            if (b64 != null) b64History.add(b64);
        }
        historyConfig.set(path + ".forge_history", b64History);
        
        // Save Salvage Tasks
        dataConfig.set(path + ".salvage_tasks", null); // Clear existing
        for (SalvageTask task : data.getSalvageTasks()) {
            String slotPath = path + ".salvage_tasks." + task.getSlotIndex();
            
            ItemStack origItem = task.getOriginalItem();
            if (origItem != null) {
                String b64Item = itemStackToBase64(origItem);
                dataConfig.set(slotPath + ".item", b64Item != null ? b64Item : origItem);
            } else {
                dataConfig.set(slotPath + ".item", null);
            }
            
            dataConfig.set(slotPath + ".start_time", task.getStartTime());
            dataConfig.set(slotPath + ".end_time", task.getEndTime());
            dataConfig.set(slotPath + ".materials", task.getMaterials());
        }
        
        saveFiles();
    }

    public void saveAll() {
        for (UUID uuid : dataMap.keySet()) {
            save(uuid);
        }
    }

    private void saveFiles() {
        try {
            dataConfig.save(dataFile);
            historyConfig.save(historyFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Cannot save player data/history", e);
        }
    }

    private String itemStackToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream io = new ByteArrayOutputStream();
            BukkitObjectOutputStream os = new BukkitObjectOutputStream(io);
            os.writeObject(item);
            os.flush();
            byte[] serializedObject = io.toByteArray();
            return Base64.getEncoder().encodeToString(serializedObject);
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack itemStackFromBase64(String base64) {
        try {
            byte[] serializedObject = Base64.getDecoder().decode(base64);
            ByteArrayInputStream in = new ByteArrayInputStream(serializedObject);
            BukkitObjectInputStream is = new BukkitObjectInputStream(in);
            return (ItemStack) is.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}
