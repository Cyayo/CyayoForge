package id.cyayo.forge.gui;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.data.PlayerForgeData;
import id.cyayo.forge.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HistoryGUI implements InventoryHolder {

    private final CyayoForge plugin;
    private final Inventory inventory;
    private final UUID targetUuid;
    private final String targetName;

    public HistoryGUI(CyayoForge plugin, UUID targetUuid, String targetName) {
        this.plugin = plugin;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        
        FileConfiguration cfg = plugin.getHistoryMenuConfig().getConfig();
        String title = ColorUtil.color(cfg.getString("title", "&8Forge History: {player}")
                .replace("{player}", targetName));
        int rows = cfg.getInt("rows", 4);
        int size = Math.max(9, Math.min(54, rows * 9));
        
        this.inventory = Bukkit.createInventory(this, size, title);
        
        setupMenu();
    }

    private void setupMenu() {
        FileConfiguration cfg = plugin.getHistoryMenuConfig().getConfig();
        PlayerForgeData data = plugin.getPlayerDataManager().getData(targetUuid);
        List<ItemStack> history = data.getForgeHistory();

        // Fill background
        ConfigurationSection fillerSec = cfg.getConfigurationSection("items.filler");
        if (fillerSec != null) {
            ItemStack filler = new ItemStack(Material.valueOf(fillerSec.getString("material", "GRAY_STAINED_GLASS_PANE").toUpperCase()));
            ItemMeta fillerMeta = filler.getItemMeta();
            if (fillerMeta != null) {
                fillerMeta.setDisplayName(ColorUtil.color(fillerSec.getString("name", " ")));
                filler.setItemMeta(fillerMeta);
            }
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }
        }

        // Slots for history (Oldest on left, Newest on right of the sequence)
        List<Integer> slots = cfg.getIntegerList("history-slots");
        if (slots.isEmpty()) slots = java.util.Arrays.asList(10, 11, 12, 13, 14, 15, 16, 17, 18);
        
        // history is oldest to newest (0 = oldest, size-1 = newest)
        // slots is left to right (0 = leftmost, size-1 = rightmost)
        // We map 1:1 so oldest is at leftmost, newest is at the current rightmost.
        for (int i = 0; i < Math.min(history.size(), slots.size()); i++) {
            int slot = slots.get(i);
            ItemStack item = history.get(i);
            
            if (item != null) {
                ItemStack display = item.clone();
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add(" ");
                    lore.add(ColorUtil.color("&eClick to take a copy"));
                    meta.setLore(lore);
                    display.setItemMeta(meta);
                }
                inventory.setItem(slot, display);
            }
        }
        
        // Info item
        ConfigurationSection infoSec = cfg.getConfigurationSection("items.info");
        if (infoSec != null) {
            int slot = infoSec.getInt("slot", 4);
            ItemStack info = new ItemStack(Material.valueOf(infoSec.getString("material", "BOOK").toUpperCase()));
            ItemMeta infoMeta = info.getItemMeta();
            if (infoMeta != null) {
                infoMeta.setDisplayName(ColorUtil.color(infoSec.getString("name", "&6Info")
                        .replace("{player}", targetName)));
                List<String> infoLore = new ArrayList<>();
                for (String line : infoSec.getStringList("lore")) {
                    infoLore.add(ColorUtil.color(line
                            .replace("{player}", targetName)
                            .replace("{total}", String.valueOf(data.getTotal()))
                            .replace("{weapon_count}", String.valueOf(data.getWeapon()))
                            .replace("{armor_count}", String.valueOf(data.getArmor()))
                            .replace("{last_item}", data.getLastForgedName())
                            .replace("{count}", String.valueOf(history.size()))));
                }
                infoMeta.setLore(infoLore);
                info.setItemMeta(infoMeta);
            }
            inventory.setItem(slot, info);
        }
    }

    public UUID getTargetUuid() { return targetUuid; }
    public String getTargetName() { return targetName; }

    @Override
    public Inventory getInventory() { return inventory; }
}
