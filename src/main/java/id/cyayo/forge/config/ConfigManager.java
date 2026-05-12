package id.cyayo.forge.config;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.model.ForgeQuality;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ConfigManager {

    private final CyayoForge plugin;
    private FileConfiguration config;

    // Animation
    private boolean animationEnabled;
    private int animationDuration;
    private int delayBeforeMinigame;

    // Minigame
    private boolean minigameEnabled;
    private int phaseDurationMin;
    private int phaseDurationMax;
    private int minigameRoundsMin;
    private int minigameRoundsMax;
    private int pointPerfect, pointGood, pointBroken, pointMiss;
    private int countdownDelayTicks;
    private Map<String, Double> permissionBonuses;
    private Map<ForgeQuality, Double> qualityThresholds;
    private Map<ForgeQuality, Double> qualityDamageBonus;
    private Map<ForgeQuality, String> qualityStatString;
    private Map<id.cyayo.forge.model.ForgeTier, String> tierDisplayNames;
    private Map<String, String> placeholderKeys;
    private String bypassQualityMode;
    private ForgeQuality bypassQualityFixed;

    // Forging
    private double hardCap;
    private double abilityDamagePercent;
    private double abilityThresholdPercent;

    // Messages
    private Map<String, Object> messages;

    // Data
    private String dataStorage;

    public ConfigManager(CyayoForge plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Animation
        animationEnabled    = config.getBoolean("animation.enabled", true);
        animationDuration   = config.getInt("animation.duration-ticks", 200);
        delayBeforeMinigame = config.getInt("animation.delay-before-minigame", 10);

        // Minigame
        minigameEnabled   = config.getBoolean("minigame.enabled", true);
        if (config.isInt("minigame.phase-duration-ticks")) {
            phaseDurationMin = config.getInt("minigame.phase-duration-ticks", 10);
            phaseDurationMax = phaseDurationMin;
        } else {
            phaseDurationMin = config.getInt("minigame.phase-duration-ticks.min", 8);
            phaseDurationMax = config.getInt("minigame.phase-duration-ticks.max", 15);
        }
        minigameRoundsMin = config.getInt("minigame.rounds.min", 5);
        minigameRoundsMax = config.getInt("minigame.rounds.max", 10);
        pointPerfect = config.getInt("minigame.points.perfect", 3);
        pointGood    = config.getInt("minigame.points.good", 1);
        pointBroken  = config.getInt("minigame.points.broken", -1);
        pointMiss    = config.getInt("minigame.points.miss", -2);
        countdownDelayTicks = config.getInt("minigame.countdown_delay_ticks", 12);
        
        permissionBonuses = new HashMap<>();
        ConfigurationSection bonusSec = config.getConfigurationSection("minigame.permission_bonuses");
        if (bonusSec != null) {
            for (String key : bonusSec.getKeys(false)) {
                permissionBonuses.put(key, bonusSec.getDouble(key));
            }
        }

        qualityThresholds = new EnumMap<>(ForgeQuality.class);
        qualityThresholds.put(ForgeQuality.PERFECT,   config.getDouble("minigame.quality_threshold.perfect", 90.0));
        qualityThresholds.put(ForgeQuality.EXQUISITE, config.getDouble("minigame.quality_threshold.exquisite", 75.0));
        qualityThresholds.put(ForgeQuality.GOOD,      config.getDouble("minigame.quality_threshold.good", 50.0));
        qualityThresholds.put(ForgeQuality.FLAWED,    config.getDouble("minigame.quality_threshold.flawed", 25.0));
        qualityThresholds.put(ForgeQuality.BROKEN,    config.getDouble("minigame.quality_threshold.broken", 0.0));

        qualityDamageBonus = new EnumMap<>(ForgeQuality.class);
        qualityDamageBonus.put(ForgeQuality.PERFECT,   config.getDouble("minigame.quality_bonus.perfect.bonus", 10.0));
        qualityDamageBonus.put(ForgeQuality.EXQUISITE, config.getDouble("minigame.quality_bonus.exquisite.bonus", 7.0));
        qualityDamageBonus.put(ForgeQuality.GOOD,      config.getDouble("minigame.quality_bonus.good.bonus", 5.0));
        qualityDamageBonus.put(ForgeQuality.FLAWED,    config.getDouble("minigame.quality_bonus.flawed.bonus", 0.0));
        qualityDamageBonus.put(ForgeQuality.BROKEN,    config.getDouble("minigame.quality_bonus.broken.bonus", -5.0));

        qualityStatString = new EnumMap<>(ForgeQuality.class);
        qualityStatString.put(ForgeQuality.PERFECT,   config.getString("minigame.quality_stat_string.perfect", "Sempurna"));
        qualityStatString.put(ForgeQuality.EXQUISITE, config.getString("minigame.quality_stat_string.exquisite", "Indah"));
        qualityStatString.put(ForgeQuality.GOOD,      config.getString("minigame.quality_stat_string.good", "Bagus"));
        qualityStatString.put(ForgeQuality.FLAWED,    config.getString("minigame.quality_stat_string.flawed", "Cacat"));
        qualityStatString.put(ForgeQuality.BROKEN,    config.getString("minigame.quality_stat_string.broken", "Rusak"));

        tierDisplayNames = new EnumMap<>(id.cyayo.forge.model.ForgeTier.class);
        for (id.cyayo.forge.model.ForgeTier tier : id.cyayo.forge.model.ForgeTier.values()) {
            tierDisplayNames.put(tier, config.getString("minigame.tier_display_names." + tier.name(), tier.name()));
        }

        placeholderKeys = new HashMap<>();
        ConfigurationSection phSec = config.getConfigurationSection("gui.placeholders");
        if (phSec != null) {
            for (String key : phSec.getKeys(false)) {
                placeholderKeys.put(key, phSec.getString(key));
            }
        } else {
            // Default placeholders
            placeholderKeys.put("tier", "{tier}");
            placeholderKeys.put("multiplier", "{multiplier}");
            placeholderKeys.put("output_type", "{output_type}");
            placeholderKeys.put("material_composition", "{material_composition}");
            placeholderKeys.put("output_list", "{output_list}");
            placeholderKeys.put("category_lore", "{category_lore}");
        }

        bypassQualityMode  = config.getString("minigame.bypass_quality_mode", "FIXED").toUpperCase();
        bypassQualityFixed = ForgeQuality.fromString(config.getString("minigame.bypass_quality_fixed", "FLAWED"));

        // Forging
        hardCap                = config.getDouble("forging.hard_cap", 2.5);
        abilityDamagePercent   = config.getDouble("forging.ability_damage_percent", 12.0);
        abilityThresholdPercent = config.getDouble("forging.ability_threshold_percent", 25.0);

        // Messages
        messages = new HashMap<>();
        java.io.File msgFile = new java.io.File(plugin.getDataFolder(), "messages.yml");
        if (!msgFile.exists()) plugin.saveResource("messages.yml", false);
        FileConfiguration msgConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(msgFile);
        ConfigurationSection msgSec = msgConfig.getConfigurationSection("messages");
        if (msgSec != null) {
            for (String key : msgSec.getKeys(false)) {
                messages.put(key, msgSec.get(key));
            }
        }

        dataStorage = config.getString("data.storage", "YAML");
    }

    public int getCountdownDelayTicks() { return countdownDelayTicks; }

    public double getPlayerMinigameBonus(org.bukkit.entity.Player player) {
        if (player == null || permissionBonuses.isEmpty()) return 0.0;
        double maxBonus = 0.0;
        for (Map.Entry<String, Double> entry : permissionBonuses.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                maxBonus = Math.max(maxBonus, entry.getValue());
            }
        }
        return maxBonus;
    }

    public ForgeQuality resolveQuality(double pointPercentage) {
        if (pointPercentage >= qualityThresholds.get(ForgeQuality.PERFECT))   return ForgeQuality.PERFECT;
        if (pointPercentage >= qualityThresholds.get(ForgeQuality.EXQUISITE)) return ForgeQuality.EXQUISITE;
        if (pointPercentage >= qualityThresholds.get(ForgeQuality.GOOD))      return ForgeQuality.GOOD;
        if (pointPercentage >= qualityThresholds.get(ForgeQuality.FLAWED))    return ForgeQuality.FLAWED;
        return ForgeQuality.BROKEN;
    }

    public ForgeQuality getBypassQuality() {
        if ("RANDOM".equals(bypassQualityMode)) {
            ForgeQuality[] vals = ForgeQuality.values();
            return vals[new Random().nextInt(vals.length)];
        }
        return bypassQualityFixed;
    }

    public int getPointsForResult(String result) {
        return switch (result.toUpperCase()) {
            case "PERFECT" -> pointPerfect;
            case "GOOD"    -> pointGood;
            case "BROKEN"  -> pointBroken;
            case "MISS"    -> pointMiss;
            default        -> 0;
        };
    }

    public String getMessage(String key) {
        Object obj = messages.get(key);
        if (obj == null) return "&cPesan tidak ditemukan: " + key;
        if (obj instanceof String) return (String) obj;
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (list.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i).toString());
                if (i < list.size() - 1) sb.append("\n");
            }
            return sb.toString();
        }
        return obj.toString();
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        if (msg.isEmpty()) return "";
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public void sendMessage(org.bukkit.entity.Player player, String key, String... replacements) {
        Object val = config.get(key);
        if (val instanceof List) {
            List<String> lines = (List<String>) val;
            for (String line : lines) {
                String processed = line;
                for (int i = 0; i < replacements.length; i += 2) {
                    processed = processed.replace(replacements[i], replacements[i + 1]);
                }
                player.sendMessage(id.cyayo.forge.util.ColorUtil.color(processed));
            }
        } else {
            String msg = getMessage(key, replacements);
            if (!msg.isEmpty()) {
                player.sendMessage(id.cyayo.forge.util.ColorUtil.color(msg));
            }
        }
    }

    // Getters
    public boolean isAnimationEnabled()    { return animationEnabled; }
    public int getAnimationDuration()      { return animationDuration; }
    public int getDelayBeforeMinigame()    { return delayBeforeMinigame; }
    public boolean isMinigameEnabled()     { return minigameEnabled; }
    public int getPhaseDurationMin()       { return phaseDurationMin; }
    public int getPhaseDurationMax()       { return phaseDurationMax; }
    public int getMinigameRoundsMin()      { return minigameRoundsMin; }
    public int getMinigameRoundsMax()      { return minigameRoundsMax; }
    public double getHardCap()             { return hardCap; }
    public double getAbilityDamagePercent(){ return abilityDamagePercent; }
    public double getAbilityThresholdPercent() { return abilityThresholdPercent; }
    public String getDataStorage()         { return dataStorage; }
    public Map<ForgeQuality, Double> getQualityDamageBonus()  { return qualityDamageBonus; }
    public Map<ForgeQuality, String> getQualityStatString()   { return qualityStatString; }
    public String getTierDisplayName(id.cyayo.forge.model.ForgeTier tier) { return tierDisplayNames.getOrDefault(tier, tier.name()); }
    public String getPlaceholderKey(String key) { return placeholderKeys.getOrDefault(key, "{" + key + "}"); }
    public FileConfiguration getRawConfig() { return config; }

    /** Helper to play sound from config path. */
    public void playSound(org.bukkit.entity.Player player, String path) {
        try {
            if (!config.contains(path + ".sound")) return;
            String sndName = config.getString(path + ".sound", "");
            if (sndName == null || sndName.isEmpty()) return;
            float v = (float) config.getDouble(path + ".volume", 1.0);
            float p = (float) config.getDouble(path + ".pitch", 1.0);
            player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(sndName.toUpperCase()), v, p);
        } catch (Exception ignored) {}
    }
}
