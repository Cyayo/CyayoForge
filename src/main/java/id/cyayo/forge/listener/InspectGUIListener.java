package id.cyayo.forge.listener;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.gui.InspectGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class InspectGUIListener implements Listener {

    private final CyayoForge plugin;

    public InspectGUIListener(CyayoForge plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        // Cek jika inventory yang dibuka adalah InspectGUI
        if (event.getView().getTopInventory().getHolder() instanceof InspectGUI) {
            event.setCancelled(true);
            
            // Tambahan keamanan: Jika pemain mencoba klik slot manapun, batalkan.
            if (event.getClickedInventory() == null) return;
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof InspectGUI) {
            event.setCancelled(true);
        }
    }
}
