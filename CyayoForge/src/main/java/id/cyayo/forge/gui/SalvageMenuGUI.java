package id.cyayo.forge.gui;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.model.SalvageTask;
import id.cyayo.forge.util.ColorUtil;
import id.cyayo.forge.util.ItemBuilder;
import id.cyayo.forge.model.ForgeTier;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SalvageMenuGUI implements InventoryHolder {

    private final CyayoForge plugin;
    private final Player player;
    private final Inventory inventory;

    public SalvageMenuGUI(CyayoForge plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        FileConfiguration cfg = plugin.getSalvageMenuConfig().getConfig();
        String title = ColorUtil.color(cfg.getString("title", "&8⚒ Salvaging"));
        int rows = cfg.getInt("rows", 4);
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
        
        populate();
    }

    public void populate() {
        inventory.clear();
        FileConfiguration cfg = plugin.getSalvageMenuConfig().getConfig();

        // Background / Filler
        ItemStack filler = buildFiller(cfg);
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        // Render Slots
        ConfigurationSection slotsSec = cfg.getConfigurationSection("slots");
        if (slotsSec != null) {
            for (String key : slotsSec.getKeys(false)) {
                int index = slotsSec.getInt(key + ".index");
                String permission = slotsSec.getString(key + ".permission", "none");
                int slotId = Integer.parseInt(key.replace("slot_", ""));
                
                renderSlot(index, slotId, permission, cfg);
            }
        }
    }

    private void renderSlot(int index, int slotId, String permission, FileConfiguration cfg) {
        if (!permission.equalsIgnoreCase("none") && !player.hasPermission(permission)) {
            inventory.setItem(index, buildDisplayItem(cfg, "locked", slotId, null));
            return;
        }

        SalvageTask task = plugin.getPlayerDataManager().getData(player.getUniqueId()).getSalvageTask(slotId);
        if (task == null) {
            inventory.setItem(index, buildDisplayItem(cfg, "empty", slotId, null));
        } else if (task.isFinished()) {
            inventory.setItem(index, buildDisplayItem(cfg, "ready", slotId, task));
        } else {
            inventory.setItem(index, buildDisplayItem(cfg, "processing", slotId, task));
        }
    }

    private ItemStack buildDisplayItem(FileConfiguration cfg, String status, int slotId, SalvageTask task) {
        String path = "display." + status;
        
        // Jika sedang processing/ready, kita gunakan item aslinya sebagai base agar modelnya tetap
        ItemStack item;
        if (task != null) {
            item = task.getOriginalItem().clone();
        } else {
            Material mat = Material.valueOf(cfg.getString(path + ".material", "PAPER").toUpperCase());
            item = new ItemStack(mat);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item; // Should not happen for valid materials

        String name = cfg.getString(path + ".name", "&eSlot #" + slotId);
        List<String> loreAppend = cfg.getStringList(path + ".lore_append");
        List<String> loreBase = cfg.getStringList(path + ".lore");

        if (task != null) {
            name = name.replace("{item_name}", getItemName(task.getOriginalItem()))
                       .replace("{time_left}", formatTime(task.getTimeLeft()));
        }
        
        name = name.replace("{id}", String.valueOf(slotId));
        meta.setDisplayName(ColorUtil.color(name));

        List<String> finalLore = new ArrayList<>();
        if (task != null) {
            // Kita append lore asli itemnya jika diaktifkan
            boolean showOriginal = cfg.getBoolean("display.show_original_lore", true);
            if (showOriginal && meta.hasLore()) {
                finalLore.addAll(meta.getLore());
            }
            
            for (String line : loreAppend) {
                if (line.contains("{materials}")) {
                    finalLore.addAll(formatMaterials(task.getMaterials()));
                    finalLore.addAll(plugin.getSalvageManager().getBonusPreview(task.getOriginalItem()));
                } else if (line.contains("{stats}")) {
                    finalLore.addAll(buildStatsLore(task.getOriginalItem()));
                } else {
                    finalLore.add(ColorUtil.color(line.replace("{time_left}", formatTime(task.getTimeLeft()))
                                                      .replace("{item_name}", getItemName(task.getOriginalItem()))));
                }
            }
        } else {
            for (String line : loreBase) {
                finalLore.add(ColorUtil.color(line.replace("{id}", String.valueOf(slotId))));
            }
        }

        meta.setLore(finalLore);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> formatMaterials(List<Map<String, Object>> materials) {
        List<String> result = new ArrayList<>();
        String format = plugin.getConfigManager().getRawConfig().getString("salvage.material_list_format", " &8• &f{amount}x {material}");
        
        Map<String, Integer> toReturn = plugin.getSalvageManager().calculateReturnMaterials(materials);
        
        for (Map.Entry<String, Integer> entry : toReturn.entrySet()) {
            String id = entry.getKey();
            int amount = entry.getValue();
            
            String display = id;
            var matData = plugin.getMaterialConfig().getMaterial(id);
            if (matData != null) {
                // Cari display name jika ada di NBT list awal
                for (Map<String, Object> m : materials) {
                    if (id.equals(m.get("id"))) {
                        display = (String) m.get("display");
                        break;
                    }
                }
            }
            
            result.add(ColorUtil.color(format.replace("{amount}", String.valueOf(amount)).replace("{material}", display)));
        }
        return result;
    }

    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) return item.getItemMeta().getDisplayName();
        return item.getType().name();
    }

    private List<String> buildStatsLore(ItemStack item) {
        FileConfiguration cfg = plugin.getSalvageMenuConfig().getConfig();
        List<String> statsLore = cfg.getStringList("display.stats_lore");
        List<String> result = new ArrayList<>();
        
        ForgeTier tier = plugin.getForgeManager().getItemTier(item);
        String tierStr = plugin.getConfigManager().getTierDisplayName(tier);
        
        NBTItem nbt = NBTItem.get(item);
        var mainCfg = plugin.getConfigManager().getRawConfig();
        String qualityStatId = mainCfg.getString("minigame.custom_stat.id", "CUSTOM_QUALITY");
        String penempaStatId = mainCfg.getString("minigame.custom_penempa.id", "CUSTOM_PENEMPA");
        
        String quality = nbt.getString("MMOITEMS_" + qualityStatId.toUpperCase());
        if (quality == null || quality.isEmpty()) quality = "&7-";
        
        String penempa = nbt.getString("MMOITEMS_" + penempaStatId.toUpperCase());
        if (penempa == null || penempa.isEmpty()) penempa = "&7-";

        for (String line : statsLore) {
            result.add(ColorUtil.color(line
                .replace("{tier}", tierStr)
                .replace("{quality}", quality)
                .replace("{penempa}", penempa)));
        }
        return result;
    }

    private String formatTime(long seconds) {
        FileConfiguration cfg = plugin.getSalvageMenuConfig().getConfig();
        if (seconds <= 0) return id.cyayo.forge.util.ColorUtil.color(cfg.getString("format.time_seconds", "{s}s").replace("{s}", "0"));
        long m = seconds / 60;
        long s = seconds % 60;
        if (m > 0) return id.cyayo.forge.util.ColorUtil.color(cfg.getString("format.time_format", "{m}m {s}s")
                .replace("{m}", String.valueOf(m))
                .replace("{s}", String.valueOf(s)));
        return id.cyayo.forge.util.ColorUtil.color(cfg.getString("format.time_seconds", "{s}s").replace("{s}", String.valueOf(s)));
    }

    private ItemStack buildFiller(FileConfiguration cfg) {
        Material mat = Material.valueOf(cfg.getString("filler.material", "GRAY_STAINED_GLASS_PANE").toUpperCase());
        return new ItemBuilder(mat).name(ColorUtil.color(cfg.getString("filler.name", " "))).build();
    }

    @Override
    public Inventory getInventory() { return inventory; }
}
