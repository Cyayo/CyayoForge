package id.cyayo.forge.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class RegionUtil {

    private static Boolean worldGuardEnabled = null;

    public static boolean isWorldGuardEnabled() {
        if (worldGuardEnabled == null) {
            try {
                Class.forName("com.sk89q.worldguard.WorldGuard");
                worldGuardEnabled = org.bukkit.Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
            } catch (ClassNotFoundException e) {
                worldGuardEnabled = false;
            }
        }
        return worldGuardEnabled;
    }

    public static boolean isInAnyRegion(Player player, List<String> regions) {
        if (regions == null || regions.isEmpty()) return false;
        if (!isWorldGuardEnabled()) return false;

        Location loc = player.getLocation();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));

        for (ProtectedRegion region : set) {
            if (regions.contains(region.getId())) {
                return true;
            }
        }
        return false;
    }
}
