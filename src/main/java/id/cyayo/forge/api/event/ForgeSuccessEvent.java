package id.cyayo.forge.api.event;

import id.cyayo.forge.model.ForgeSession;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player successfully forges an item.
 */
public class ForgeSuccessEvent extends ForgeEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ItemStack result;

    public ForgeSuccessEvent(@NotNull Player who, @NotNull ForgeSession session, @NotNull ItemStack result) {
        super(who, session);
        this.result = result;
    }

    /**
     * @return the item stack that was produced
     */
    @NotNull
    public ItemStack getResult() {
        return result;
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
