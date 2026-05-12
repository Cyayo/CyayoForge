package id.cyayo.forge.listener;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.model.ForgeSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final CyayoForge plugin;

    public PlayerListener(CyayoForge plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Cek apakah ada sesi tempa yang aktif
        if (plugin.getForgeManager().hasActiveSession(player.getUniqueId())) {
            // Berikan penalti cooldown untuk mencegah abuse disconnect
            plugin.getForgeManager().setPenalty(player.getUniqueId());

            ForgeSession session = plugin.getForgeManager().getSession(player.getUniqueId());
            
            if (session != null) {
                // Kembalikan material ke inventory player (Base inventory akan tersimpan saat disconnect)
                for (ItemStack mat : session.getMaterials()) {
                    if (mat != null && !mat.getType().isAir()) {
                        player.getInventory().addItem(mat).values().forEach(drop -> 
                            player.getWorld().dropItem(player.getLocation(), drop));
                    }
                }
                
                // Beritahu konsol (opsional)
                plugin.getLogger().info("Sesi tempa " + player.getName() + " dibatalkan karena disconnect. Material telah dikembalikan.");
            }
            
            // Bersihkan sesi agar tidak stuck
            plugin.getForgeManager().removeSession(player.getUniqueId());
        }
    }
}
