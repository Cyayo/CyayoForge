package id.cyayo.forge.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public class ColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public static String color(String text) {
        if (text == null || text.isEmpty()) return "";

        // Global Fix: Handle multi-line strings by processing each line independently
        // This prevents formatting (like strikethrough) from bleeding into the next line
        if (text.contains("\n")) {
            String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                lines[i] = color(lines[i]);
            }
            return String.join("\n", lines);
        }

        // 1. Handle Legacy Hex Format (§x§r§r§g§g§b§b or &x&r&r&g&g&b&b)
        // This is crucial for compatibility with MMOItems and high-version servers
        text = text.replaceAll("[§&]x[§&]([0-9a-fA-F])[§&]([0-9a-fA-F])[§&]([0-9a-fA-F])[§&]([0-9a-fA-F])[§&]([0-9a-fA-F])[§&]([0-9a-fA-F])", "<#$1$2$3$4$5$6>");

        // 2. Handle Simple Hex Format (&#RRGGBB)
        text = text.replaceAll("&#([a-fA-F0-9]{6})", "<#$1>");

        // 3. Pre-process standard legacy codes (§ and &) to MiniMessage tags
        // Color codes (0-9, a-f) should reset formatting to mimic legacy behavior
        String processed = text
                .replace("§", "&")
                .replace("&0", "<reset><black>").replace("&1", "<reset><dark_blue>").replace("&2", "<reset><dark_green>")
                .replace("&3", "<reset><dark_aqua>").replace("&4", "<reset><dark_red>").replace("&5", "<reset><dark_purple>")
                .replace("&6", "<reset><gold>").replace("&7", "<reset><gray>").replace("&8", "<reset><dark_gray>")
                .replace("&9", "<reset><blue>").replace("&a", "<reset><green>").replace("&b", "<reset><aqua>")
                .replace("&c", "<reset><red>").replace("&d", "<reset><light_purple>").replace("&e", "<reset><yellow>")
                .replace("&f", "<reset><white>")
                // Formatting codes (l-k) do NOT reset
                .replace("&l", "<bold>").replace("&m", "<strikethrough>")
                .replace("&n", "<underline>").replace("&o", "<italic>").replace("&r", "<reset>")
                .replace("&k", "<obfuscated>");

        // 4. Final Processing with MiniMessage
        try {
            Component component = MINI_MESSAGE.deserialize(processed);
            // Serialize back to legacy section format (§) which all Bukkit versions understand
            return LEGACY_SERIALIZER.serialize(component);
        } catch (Exception e) {
            // Ultimate fallback for very broken tags
            return ChatColor.translateAlternateColorCodes('&', text);
        }
    }

    public static String strip(String text) {
        return ChatColor.stripColor(color(text));
    }
}
