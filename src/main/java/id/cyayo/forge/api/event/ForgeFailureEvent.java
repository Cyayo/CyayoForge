package id.cyayo.forge.api.event;

import id.cyayo.forge.model.ForgeSession;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player fails to forge an item (e.g. buildOutput returns null).
 */
public class ForgeFailureEvent extends ForgeEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public ForgeFailureEvent(@NotNull Player who, @NotNull ForgeSession session) {
        super(who, session);
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
