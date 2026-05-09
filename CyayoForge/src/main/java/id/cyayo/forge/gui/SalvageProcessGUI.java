package id.cyayo.forge.gui;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.util.ColorUtil;
import id.cyayo.forge.util.ItemBuilder;
import id.cyayo.forge.model.ForgeTier;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SalvageProcessGUI implements InventoryHolder {

    private final CyayoForge plugin;
    private final Player player;
    private final int slotIndex;
    private final Inventory inventory;
    
    private ItemStack currentItem;

    public SalvageProcessGUI(CyayoForge plugin, Player player, int slotIndex) {
        this.plugin = plugin;
        this.player = player;
        this.slotIndex = slotIndex;

        FileConfiguration cfg = plugin.getSalvageMenuConfig().getConfig();
        String title = ColorUtil.color(cfg.getString("process_menu.title", "&8⚒ Masukkan Item"));
        this.inventory = Bukkit.createInventory(this, 45, title);
        
        populate();
    }

    public void populate() {
        inventory.clear();
        FileConfiguration cfg = plugin.getSalvageMenuConfig().getConfig();

        // Background / Filler
        Material fillerMat = Material.valueOf(cfg.getString("filler.material", "GRAY_STAINED_GLASS_PANE").toUpperCase());
        ItemStack filler = new ItemBuilder(fillerMat).name(" ").build();
        for (int i = 0; i < 45; i++) inventory.setItem(i, filler);

        // Input Slot
        int inputSlot = cfg.getInt("process_menu.input_slot", 13);
        if (currentItem != null) {
            inventory.setItem(inputSlot, buildDisplayItem(currentItem));
        } else {
            inventory.setItem(inputSlot, null); // Biarkan kosong agar player bisa menaruh item
        }

        updateButtons();
    }

    public void updateButtons() {
        FileConfiguration cfg = plugin.getSalvageMenuConfig().getConfig();
        
        // Start Button
        int startSlot = cfg.getInt("process_menu.start_button.slot", 31);
        if (currentItem != null && !currentItem.getType().isAir()) {
            List<Map<String, Object>> materials = plugin.getForgeManager().getMaterialDataFromNBT(currentItem);
            if (!materials.isEmpty()) {
                long time = plugin.getSalvageManager().calculateSalvageTime(player, currentItem);
                
                String name = cfg.getString("process_menu.start_button.ready.name", "&aMulai Salvage");
                List<String> loreTemplate = cfg.getStringList("process_menu.start_button.ready.lore");
                List<String> finalLore = new ArrayList<>();
                
                for (String line : loreTemplate) {
                    if (line.contains("{materials}")) {
                        finalLore.addAll(formatMaterials(materials));
                        finalLore.addAll(plugin.getSalvageManager().getBonusPreview(currentItem));
                    } else {
                        finalLore.add(ColorUtil.color(line.replace("{time}", formatTime(time))));
                    }
                }
                
                Material mat = Material.valueOf(cfg.getString("process_menu.start_button.ready.material", "ANVIL").toUpperCase());
                inventory.setItem(startSlot, new ItemBuilder(mat).name(ColorUtil.color(name)).lore(finalLore).glowing().build());
            } else {
                inventory.setItem(startSlot, buildNoItemButton(cfg));
            }
        } else {
            inventory.setItem(startSlot, buildNoItemButton(cfg));
        }

        // Cancel Button sudah dihapus sesuai permintaan
    }

    private ItemStack buildNoItemButton(FileConfiguration cfg) {
        String name = cfg.getString("process_menu.start_button.no_item.name", "&cLetakkan Item!");
        List<String> lore = cfg.getStringList("process_menu.start_button.no_item.lore");
        Material mat = Material.valueOf(cfg.getString("process_menu.start_button.no_item.material", "BARRIER").toUpperCase());
        return new ItemBuilder(mat).name(ColorUtil.color(name)).lore(lore).build();
    }

    private List<String> formatMaterials(List<Map<String, Object>> materials) {
        List<String> result = new ArrayList<>();
        String format = plugin.getConfigManager().getRawConfig().getString("salvage.material_list_format", " &8• &f{amount}x {material}");
        
        Map<String, Integer> toReturn = plugin.getSalvageManager().calculateReturnMaterials(materials);
        
        for (Map.Entry<String, Integer> entry : toReturn.entrySet()) {
            String id = entry.getKey();
            int amount = entry.getValue();
            
            String display = id;
            for (Map<String, Object> m : materials) {
                if (id.equals(m.get("id"))) {
                    display = (String) m.get("display");
                    break;
                }
            }
            
            result.add(ColorUtil.color(format.replace("{amount}", String.valueOf(amount)).replace("{material}", display)));
        }
        return result;
    }

    private ItemStack buildDisplayItem(ItemStack item) {
        FileConfiguration cfg = plugin.getSalvageMenuConfig().getConfig();
        boolean showOriginal = cfg.getBoolean("process_menu.item_display.show_original_lore", true);
        if (showOriginal) return item;

        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) return clone;

        List<String> lore = new ArrayList<>();
        List<String> customLore = cfg.getStringList("process_menu.item_display.custom_lore");
        
        // Extract stats
        ForgeTier tier = plugin.getForgeManager().getItemTier(item);
        String tierStr = plugin.getConfigManager().getTierDisplayName(tier);
        
        NBTItem nbt = NBTItem.get(item);
        var mainCfg = plugin.getConfigManager().getRawConfig();
        String qualityStatId = mainCfg.getString("minigame.custom_stat.id", "CUSTOM_QUALITY");
        String penempaStatId = mainCfg.getString("minigame.custom_penempa.id", "CUSTOM_PENEMPA");
        
        // MMOItems string stats are usually stored as MMOITEMS_<ID> in NBT
        String quality = nbt.getString("MMOITEMS_" + qualityStatId.toUpperCase());
        if (quality == null || quality.isEmpty()) quality = "&7-";
        
        String penempa = nbt.getString("MMOITEMS_" + penempaStatId.toUpperCase());
        if (penempa == null || penempa.isEmpty()) penempa = "&7-";

        for (String line : customLore) {
            lore.add(ColorUtil.color(line
                .replace("{tier}", tierStr)
                .replace("{quality}", quality)
                .replace("{penempa}", penempa)));
        }
        
        meta.setLore(lore);
        clone.setItemMeta(meta);
        return clone;
    }

    private String formatTime(long seconds) {
        FileConfiguration cfg = plugin.getSalvageMenuConfig().getConfig();
        if (seconds <= 0) return id.cyayo.forge.util.ColorUtil.color(cfg.getString("format.time_instant", "Instant"));
        long m = seconds / 60;
        long s = seconds % 60;
        if (m > 0) return id.cyayo.forge.util.ColorUtil.color(cfg.getString("format.time_format", "{m}m {s}s")
                .replace("{m}", String.valueOf(m))
                .replace("{s}", String.valueOf(s)));
        return id.cyayo.forge.util.ColorUtil.color(cfg.getString("format.time_seconds", "{s}s").replace("{s}", String.valueOf(s)));
    }

    public void setItem(ItemStack item) {
        this.currentItem = item;
        updateButtons();
    }

    public ItemStack getItem() { return currentItem; }
    public int getSlotIndex() { return slotIndex; }

    @Override
    public Inventory getInventory() { return inventory; }
}
