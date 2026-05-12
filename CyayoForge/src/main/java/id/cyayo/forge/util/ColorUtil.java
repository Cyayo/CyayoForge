package id.cyayo.forge.util;

import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([a-fA-F0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([a-fA-F0-9]{6})>");

    public static String color(String text) {
        if (text == null) return "";
        
        Matcher matcher1 = HEX_PATTERN_1.matcher(text);
        while (matcher1.find()) {
            String color = matcher1.group(1);
            text = text.replace("&#" + color, net.md_5.bungee.api.ChatColor.of("#" + color).toString());
        }
        
        Matcher matcher2 = HEX_PATTERN_2.matcher(text);
        while (matcher2.find()) {
            String color = matcher2.group(1);
            text = text.replace("<#" + color + ">", net.md_5.bungee.api.ChatColor.of("#" + color).toString());
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String strip(String text) {
        return ChatColor.stripColor(color(text));
    }
}
