package id.cyayo.forge.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Static provider for the CyayoForge API.
 */
public final class CyayoForgeProvider {

    private static CyayoForgeAPI api;

    private CyayoForgeProvider() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Get the CyayoForge API instance.
     *
     * @return the API instance
     * @throws IllegalStateException if the API is not yet initialized
     */
    @NotNull
    public static CyayoForgeAPI get() {
        if (api == null) {
            throw new IllegalStateException("CyayoForge API is not yet initialized!");
        }
        return api;
    }

    /**
     * Set the API instance. This should only be called by the plugin itself.
     *
     * @param instance the API instance
     */
    @ApiStatus.Internal
    public static void register(@NotNull CyayoForgeAPI instance) {
        api = instance;
    }

    /**
     * Unregister the API instance.
     */
    @ApiStatus.Internal
    public static void unregister() {
        api = null;
    }
}
