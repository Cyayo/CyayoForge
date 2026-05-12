package id.cyayo.forge.api.event;

import id.cyayo.forge.model.ForgeRecipe;
import id.cyayo.forge.model.ForgeType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Called when a player starts the forging process (before animation/minigame).
 */
public class ForgeStartEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    private final List<ItemStack> materials;
    private final ForgeRecipe recipe;
    private final ForgeType type;

    public ForgeStartEvent(@NotNull Player who, @NotNull List<ItemStack> materials, @NotNull ForgeRecipe recipe, @NotNull ForgeType type) {
        super(who);
        this.materials = materials;
        this.recipe = recipe;
        this.type = type;
    }

    /**
     * @return the list of materials being used
     */
    @NotNull
    public List<ItemStack> getMaterials() {
        return materials;
    }

    /**
     * @return the recipe being used
     */
    @NotNull
    public ForgeRecipe getRecipe() {
        return recipe;
    }

    /**
     * @return the type of forge (WEAPON/ARMOR)
     */
    @NotNull
    public ForgeType getType() {
        return type;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
