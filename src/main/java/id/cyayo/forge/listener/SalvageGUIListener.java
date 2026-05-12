package id.cyayo.forge.listener;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.gui.SalvageMenuGUI;
import id.cyayo.forge.gui.SalvageProcessGUI;
import id.cyayo.forge.model.SalvageTask;
import id.cyayo.forge.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class SalvageGUIListener implements Listener {

    private final CyayoForge plugin;

    public SalvageGUIListener(CyayoForge plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // ── Handle SalvageMenuGUI ──
        if (event.getInventory().getHolder() instanceof SalvageMenuGUI gui) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            ConfigurationSection slotsSec = plugin.getSalvageMenuConfig().getConfig().getConfigurationSection("slots");
            if (slotsSec == null) return;
            
            for (String key : slotsSec.getKeys(false)) {
                int index = slotsSec.getInt(key + ".index");
                if (slot == index) {
                    int slotId = Integer.parseInt(key.replace("slot_", ""));
                    String perm = slotsSec.getString(key + ".permission", "none");
                    
                    if (!perm.equalsIgnoreCase("none") && !player.hasPermission(perm)) {
                        plugin.getConfigManager().playSound(player, "gui.sound_fail");
                        return;
                    }
                    
                    handleSlotClick(player, slotId);
                    return;
                }
            }
        }
        
        // ── Handle SalvageProcessGUI ──
        if (event.getInventory().getHolder() instanceof SalvageProcessGUI gui) {
            event.setCancelled(true);
            int rawSlot = event.getRawSlot();
            int inputSlot = plugin.getSalvageMenuConfig().getConfig().getInt("process_menu.input_slot", 13);
            int startSlot = plugin.getSalvageMenuConfig().getConfig().getInt("process_menu.start_button.slot", 31);
            int cancelSlot = plugin.getSalvageMenuConfig().getConfig().getInt("process_menu.cancel_button.slot", 30);
            
            // Klik di inventory player
            if (rawSlot >= event.getInventory().getSize()) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType().isAir()) return;
                
                // Validasi: Apakah item bisa di-salvage?
                List<Map<String, Object>> materials = plugin.getForgeManager().getMaterialDataFromNBT(clicked);
                if (materials.isEmpty()) {
                    plugin.getConfigManager().sendMessage(player, "salvage_invalid_item");
                    plugin.getConfigManager().playSound(player, "gui.sound_fail");
                    return;
                }
                
                // Pindahkan ke GUI
                if (gui.getItem() != null) {
                    player.getInventory().addItem(gui.getItem());
                }
                gui.setItem(clicked.clone());
                clicked.setAmount(0);
                gui.populate();
                plugin.getConfigManager().playSound(player, "gui.sound_add_material");
                return;
            }
            
            // Klik di Input Slot (Ambil kembali)
            if (rawSlot == inputSlot) {
                if (gui.getItem() != null) {
                    player.getInventory().addItem(gui.getItem());
                    gui.setItem(null);
                    gui.populate();
                    plugin.getConfigManager().playSound(player, "gui.sound_remove_material");
                }
                return;
            }
            
            // Klik Start
            if (rawSlot == startSlot) {
                if (gui.getItem() == null) {
                    plugin.getConfigManager().sendMessage(player, "salvage_no_item");
                    plugin.getConfigManager().playSound(player, "gui.sound_fail");
                    return;
                }
                
                plugin.getSalvageManager().startSalvage(player, gui.getSlotIndex(), gui.getItem());
                gui.setItem(null); // Penting agar tidak dikembalikan saat GUI ditutup
                
                player.openInventory(new SalvageMenuGUI(plugin, player).getInventory());
                plugin.getConfigManager().playSound(player, "gui.sound_open");
                return;
            }
            
            // Klik Cancel - Sudah dihapus di GUI, tapi kita cegah jika player klik slot 30
            if (rawSlot == cancelSlot) {
                // Diabaikan karena tombol sudah tidak ada
            }
        }
    }


    private void handleSlotClick(Player player, int slotId) {
        SalvageTask task = plugin.getPlayerDataManager().getData(player.getUniqueId()).getSalvageTask(slotId);
        
        if (task == null) {
            // Buka Process GUI
            player.openInventory(new SalvageProcessGUI(plugin, player, slotId).getInventory());
            plugin.getConfigManager().playSound(player, "gui.sound_open");
        } else if (task.isFinished()) {
            plugin.getSalvageManager().claimSalvage(player, slotId);
            
            plugin.getConfigManager().playSound(player, "forging.sound_success");
            player.openInventory(new SalvageMenuGUI(plugin, player).getInventory());
        } else {
            // Batalkan? User minta bisa dibatalkan
            plugin.getSalvageManager().cancelSalvage(player, slotId);
            plugin.getConfigManager().playSound(player, "gui.sound_close");
            player.openInventory(new SalvageMenuGUI(plugin, player).getInventory());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SalvageMenuGUI || 
            event.getInventory().getHolder() instanceof SalvageProcessGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SalvageProcessGUI gui) {
            if (gui.getItem() != null) {
                event.getPlayer().getInventory().addItem(gui.getItem());
            }
        }
    }
}
