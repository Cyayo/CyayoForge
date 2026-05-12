package id.cyayo.forge.listener;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.gui.MinigameGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MinigameListener implements Listener {

    private final CyayoForge plugin;

    public MinigameListener(CyayoForge plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MinigameGUI gui)) return;

        event.setCancelled(true);
        if (gui.isFinished() || !gui.isWaitingForClick()) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= gui.getInventory().getSize()) return;

        MinigameGUI.HitResult result = gui.handleClick(slot);
        if (result != null) gui.applyResult(result);
    }

    /**
     * Bug fix utama:
     * Saat ronde baru dimulai, inventory lama ditutup dan yang baru dibuka.
     * Ini memicu InventoryCloseEvent → jangan anggap sebagai cancel jika sedang transisi.
     *
     * Flag transitioning = true saat MinigameGUI sedang ganti ke ronde berikutnya.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MinigameGUI gui)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        // JANGAN cancel jika:
        // 1. Minigame sudah selesai normal (finished = true)
        // 2. Sedang transisi ke ronde berikutnya (transitioning = true)
        if (gui.isFinished() || gui.isTransitioning()) return;

        // Player menutup secara manual → batalkan forging
        gui.onForcedClose();
    }

}
