package id.cyayo.forge.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player starts a salvage task.
 */
public class SalvageStartEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    private final ItemStack item;
    private final int slotIndex;

    public SalvageStartEvent(@NotNull Player who, @NotNull ItemStack item, int slotIndex) {
        super(who);
        this.item = item;
        this.slotIndex = slotIndex;
    }

    /**
     * @return the item being salvaged
     */
    @NotNull
    public ItemStack getItem() {
        return item;
    }

    /**
     * @return the slot index where the salvage is happening
     */
    public int getSlotIndex() {
        return slotIndex;
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
