package id.cyayo.forge;

import id.cyayo.forge.command.ForgeCommand;
import id.cyayo.forge.config.ConfigManager;
import id.cyayo.forge.config.MaterialConfig;
import id.cyayo.forge.config.MenuConfig;
import id.cyayo.forge.config.RecipeConfig;
import id.cyayo.forge.data.PlayerDataManager;
import id.cyayo.forge.listener.ForgeGUIListener;
import id.cyayo.forge.listener.MinigameListener;
import id.cyayo.forge.listener.PlayerListener;
import id.cyayo.forge.listener.SalvageGUIListener;
import id.cyayo.forge.manager.ForgeManager;
import id.cyayo.forge.manager.SalvageManager;
import id.cyayo.forge.placeholder.ForgeExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CyayoForge extends JavaPlugin {

    private static CyayoForge instance;

    private ConfigManager configManager;
    private MaterialConfig materialConfig;
    private RecipeConfig recipeConfig;
    private MenuConfig menuConfig;
    private id.cyayo.forge.config.MinigameMenuConfig minigameMenuConfig;
    private id.cyayo.forge.config.SalvageMenuConfig salvageMenuConfig;
    private id.cyayo.forge.config.SalvageBonusConfig salvageBonusConfig;
    private PlayerDataManager playerDataManager;
    private ForgeManager forgeManager;
    private SalvageManager salvageManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfigs();

        this.configManager = new ConfigManager(this);
        this.materialConfig = new MaterialConfig(this);
        this.recipeConfig = new RecipeConfig(this);
        this.menuConfig = new MenuConfig(this);
        this.minigameMenuConfig = new id.cyayo.forge.config.MinigameMenuConfig(this);
        this.salvageMenuConfig = new id.cyayo.forge.config.SalvageMenuConfig(this);
        this.salvageBonusConfig = new id.cyayo.forge.config.SalvageBonusConfig(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.forgeManager = new ForgeManager(this);
        this.salvageManager = new SalvageManager(this);

        registerListeners();
        registerCommands();
        registerPlaceholders();

        getLogger().info("CyayoForge v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        getLogger().info("CyayoForge disabled!");
    }

    private void saveDefaultConfigs() {
        saveDefaultConfig();
        saveResource("materials.yml", false);
        saveResource("recipes.yml", false);
        saveResource("menus/forging.yml", false);
        saveResource("menus/minigame.yml", false);
        saveResource("menus/salvage.yml", false);
        saveResource("salvage_bonus.yml", false);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new ForgeGUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MinigameListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SalvageGUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    private void registerCommands() {
        ForgeCommand cmd = new ForgeCommand(this);
        getCommand("cyayoforge").setExecutor(cmd);
        getCommand("cyayoforge").setTabCompleter(cmd);
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ForgeExpansion(this).register();
            getLogger().info("PlaceholderAPI hooked!");
        }
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        materialConfig.reload();
        recipeConfig.reload();
        menuConfig.reload();
        minigameMenuConfig.reload();
        salvageMenuConfig.reload();
        salvageBonusConfig.reload();
        playerDataManager.reload();
        forgeManager.reload();
    }

    public static CyayoForge getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public MaterialConfig getMaterialConfig() { return materialConfig; }
    public RecipeConfig getRecipeConfig() { return recipeConfig; }
    public MenuConfig getMenuConfig() { return menuConfig; }
    public id.cyayo.forge.config.MinigameMenuConfig getMinigameMenuConfig() { return minigameMenuConfig; }
    public id.cyayo.forge.config.SalvageMenuConfig getSalvageMenuConfig() { return salvageMenuConfig; }
    public id.cyayo.forge.config.SalvageBonusConfig getSalvageBonusConfig() { return salvageBonusConfig; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public ForgeManager getForgeManager() { return forgeManager; }
    public SalvageManager getSalvageManager() { return salvageManager; }
}
