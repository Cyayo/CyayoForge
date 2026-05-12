package id.cyayo.forge.api.event;

import id.cyayo.forge.model.SalvageTask;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Called when a player claims materials from a finished salvage task.
 */
public class SalvageClaimEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final SalvageTask task;
    private final List<ItemStack> materialsReturned;

    public SalvageClaimEvent(@NotNull Player who, @NotNull SalvageTask task, @NotNull List<ItemStack> materialsReturned) {
        super(who);
        this.task = task;
        this.materialsReturned = materialsReturned;
    }

    /**
     * @return the salvage task associated with this claim
     */
    @NotNull
    public SalvageTask getTask() {
        return task;
    }

    /**
     * @return the list of items returned to the player
     */
    @NotNull
    public List<ItemStack> getMaterialsReturned() {
        return materialsReturned;
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
