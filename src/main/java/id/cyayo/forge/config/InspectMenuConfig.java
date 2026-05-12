package id.cyayo.forge.config;

import id.cyayo.forge.CyayoForge;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class InspectMenuConfig {

    private final CyayoForge plugin;
    private FileConfiguration config;

    public InspectMenuConfig(CyayoForge plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "menus/inspect.yml");
        if (!file.exists()) {
            plugin.saveResource("menus/inspect.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
