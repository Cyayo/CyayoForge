package id.cyayo.forge.listener;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.data.PlayerForgeData;
import id.cyayo.forge.gui.HistoryGUI;
import id.cyayo.forge.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class HistoryGUIListener implements Listener {

    private final CyayoForge plugin;

    public HistoryGUIListener(CyayoForge plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HistoryGUI gui)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player admin)) return;
        
        FileConfiguration cfg = plugin.getHistoryMenuConfig().getConfig();
        if (!admin.hasPermission("cyayoforge.admin.history")) {
            admin.sendMessage(ColorUtil.color(cfg.getString("messages.no_permission", "&cNo permission.")));
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Find the actual item from history
        PlayerForgeData data = plugin.getPlayerDataManager().getData(gui.getTargetUuid());
        List<ItemStack> history = data.getForgeHistory();
        
        int clickedSlot = event.getSlot();
        List<Integer> slots = cfg.getIntegerList("history-slots");
        if (slots.isEmpty()) slots = java.util.Arrays.asList(11, 12, 13, 14, 15);
        
        int historyIndex = -1;
        
        // We need to reverse the logic from HistoryGUI:
        // historyIndex = history.size() - 1 - i
        // slotIndex = slots.size() - 1 - i
        // So: i = slots.size() - 1 - slotIndex
        // historyIndex = history.size() - 1 - (slots.size() - 1 - slotIndex)
        // historyIndex = history.size() - slots.size() + slotIndex
        
        int slotIndexInList = -1;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) == clickedSlot) {
                slotIndexInList = i;
                break;
            }
        }
        
        if (slotIndexInList != -1) {
            // Since we map history[i] to slots[i], the index is the same
            historyIndex = slotIndexInList;
            
            if (historyIndex >= 0 && historyIndex < history.size()) {
                ItemStack original = history.get(historyIndex).clone();
                admin.getInventory().addItem(original).values().forEach(drop -> admin.getWorld().dropItem(admin.getLocation(), drop));
                
                String itemName = original.hasItemMeta() && original.getItemMeta().hasDisplayName() ? 
                                 original.getItemMeta().getDisplayName() : original.getType().name();
                
                admin.sendMessage(ColorUtil.color(cfg.getString("messages.take_success", "&aTaken {item}")
                        .replace("{item}", itemName)
                        .replace("{player}", gui.getTargetName())));
            }
        }
    }
}
