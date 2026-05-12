package id.cyayo.forge.placeholder;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.data.PlayerForgeData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class ForgeExpansion extends PlaceholderExpansion {

    private final CyayoForge plugin;

    public ForgeExpansion(CyayoForge plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "cyayoforge"; }
    @Override public String getAuthor() { return "Cyayo"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public boolean canRegister() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "0";
        if (params == null) return null;
        PlayerForgeData data = plugin.getPlayerDataManager().getData(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "total_forged"        -> String.valueOf(data.getTotal());
            case "total_forged_weapon" -> String.valueOf(data.getWeapon());
            case "total_forged_armor"  -> String.valueOf(data.getArmor());
            case "last_forged"         -> data.getLastForged();
            case "penalty_time"        -> String.valueOf(plugin.getForgeManager().getPenaltyTimeLeft(player.getUniqueId()));
            default                    -> null;
        };
    }
}
