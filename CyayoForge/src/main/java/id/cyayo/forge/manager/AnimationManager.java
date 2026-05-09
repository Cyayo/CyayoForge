package id.cyayo.forge.manager;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.model.ForgeSession;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class AnimationManager {

    private final CyayoForge plugin;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    public AnimationManager(CyayoForge plugin) {
        this.plugin = plugin;
    }

    public void playAnimation(Player player, ForgeSession session, Runnable onComplete) {
        int duration = plugin.getConfigManager().getAnimationDuration();
        int delayAfter = plugin.getConfigManager().getDelayBeforeMinigame();

        // Efek slowness & blindness
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                duration + delayAfter + 20, 5, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                duration + delayAfter + 20, 1, false, false));

        // Title
        String title    = plugin.getMenuConfig().getString("animation.title", "&8⚒ &6Menempa...");
        String subtitle = plugin.getMenuConfig().getString("animation.subtitle", "&7Harap tunggu...");
        player.sendTitle(
                id.cyayo.forge.util.ColorUtil.color(title),
                id.cyayo.forge.util.ColorUtil.color(subtitle),
                10, duration - 20, 10
        );

        // Suara dari config
        playAnimationSounds(player, duration);

        // Jadwalkan selesai
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            activeTasks.remove(player.getUniqueId());
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.resetTitle();
            onComplete.run();
        }, duration + delayAfter);

        activeTasks.put(player.getUniqueId(), task);
    }

    private void playAnimationSounds(Player player, int maxDelay) {
        var cfg = plugin.getConfigManager().getRawConfig();
        var soundList = cfg.getMapList("animation.sounds");
        if (soundList == null) return;

        for (Object obj : soundList) {
            if (!(obj instanceof Map<?, ?> map)) continue;
            try {
                String soundName = (String) map.get("sound");
                if (soundName == null || soundName.isEmpty()) continue;

                int delay = 0;
                Object delayObj = map.get("delay");
                if (delayObj instanceof Number n) delay = n.intValue();

                if (delay >= maxDelay) continue;

                float volume = 1.0f;
                float pitch  = 1.0f;
                Object volObj   = map.get("volume");
                Object pitchObj = map.get("pitch");
                if (volObj instanceof Number n)   volume = n.floatValue();
                if (pitchObj instanceof Number n) pitch  = n.floatValue();

                Sound sound = Sound.valueOf(soundName.toUpperCase());
                final float v = volume, p = pitch;
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> player.playSound(player.getLocation(), sound, v, p), delay);

            } catch (Exception ignored) {}
        }
    }

    public boolean isAnimating(UUID uuid) { return activeTasks.containsKey(uuid); }

    public void cancel(UUID uuid) {
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null) task.cancel();
    }
}
