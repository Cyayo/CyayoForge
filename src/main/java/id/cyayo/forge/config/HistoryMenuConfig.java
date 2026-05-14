package id.cyayo.forge.config;

import id.cyayo.forge.CyayoForge;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;

public class HistoryMenuConfig {

    private final CyayoForge plugin;
    private File file;
    private FileConfiguration config;

    public HistoryMenuConfig(CyayoForge plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        file = new File(plugin.getDataFolder(), "menus/history.yml");
        if (!file.exists()) {
            plugin.saveResource("menus/history.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
