package id.cyayo.forge.api;

import id.cyayo.forge.data.PlayerDataManager;
import id.cyayo.forge.manager.ForgeManager;
import id.cyayo.forge.manager.SalvageManager;
import id.cyayo.forge.config.RecipeConfig;
import id.cyayo.forge.config.MaterialConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Main API interface for CyayoForge.
 */
public interface CyayoForgeAPI {

    /**
     * @return the ForgeManager instance
     */
    ForgeManager getForgeManager();

    /**
     * @return the SalvageManager instance
     */
    SalvageManager getSalvageManager();

    /**
     * @return the PlayerDataManager instance
     */
    PlayerDataManager getPlayerDataManager();

    /**
     * @return the RecipeConfig instance
     */
    RecipeConfig getRecipeConfig();

    /**
     * @return the MaterialConfig instance
     */
    MaterialConfig getMaterialConfig();

    /**
     * @return the ConfigManager instance
     */
    id.cyayo.forge.config.ConfigManager getConfigManager();

    /**
     * Helper to check if a player is currently forging.
     * @param player the player to check
     * @return true if the player is forging
     */
    default boolean isForging(@NotNull org.bukkit.entity.Player player) {
        return getForgeManager().hasActiveSession(player.getUniqueId());
    }
}
