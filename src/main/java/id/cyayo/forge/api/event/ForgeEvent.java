package id.cyayo.forge.api.event;

import id.cyayo.forge.model.ForgeSession;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Base event for all forging related events.
 */
public abstract class ForgeEvent extends PlayerEvent {
    private final ForgeSession session;

    public ForgeEvent(@NotNull Player who, @NotNull ForgeSession session) {
        super(who);
        this.session = session;
    }

    /**
     * @return the forge session associated with this event
     */
    @NotNull
    public ForgeSession getSession() {
        return session;
    }
}
