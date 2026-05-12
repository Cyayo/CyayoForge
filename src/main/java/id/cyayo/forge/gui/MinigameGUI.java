package id.cyayo.forge.gui;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.model.ForgeSession;
import id.cyayo.forge.util.ColorUtil;
import id.cyayo.forge.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MinigameGUI implements InventoryHolder {

    public enum HitResult { PERFECT, GOOD, BROKEN, MISS }
    public enum Phase { MERAH_1, KUNING_1, HIJAU, KUNING_2, MERAH_2, HILANG }

    private static final int[] CLICK_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final CyayoForge plugin;
    private final Player player;
    private final ForgeSession session;
    private final Inventory inventory;
    private ItemStack backgroundItem;

    private BukkitTask task;
    private int activeSlot  = -1;
    private Phase phase     = Phase.HILANG;
    private int phaseTick   = 0;
    private int phaseDurationTicks;
    private final Random rng = new Random();
    private final List<BukkitTask> countdownTasks = new ArrayList<>();

    // Flags penting untuk fix bug
    private volatile boolean waitingForClick = false;
    private volatile boolean finished        = false;
    /** True saat GUI ini sedang diganti ke ronde berikutnya (bukan cancel) */
    private volatile boolean transitioning   = false;

    public MinigameGUI(CyayoForge plugin, Player player, ForgeSession session) {
        this.plugin   = plugin;
        this.player   = player;
        this.session  = session;

        calculatePhaseDuration();

        String title = ColorUtil.color(plugin.getMinigameMenuConfig().getConfig().getString("title", "&8⚒ Menempa &8- &eMinigame"));
        this.inventory = Bukkit.createInventory(this, 45, title);
        buildLayout();
    }

    private void calculatePhaseDuration() {
        int pdMin = plugin.getConfigManager().getPhaseDurationMin();
        int pdMax = plugin.getConfigManager().getPhaseDurationMax();
        this.phaseDurationTicks = pdMin + rng.nextInt(Math.max(1, pdMax - pdMin + 1));
    }

    // ─────────────────────────────────────────────
    // Layout
    // ─────────────────────────────────────────────

    private void buildLayout() {
        inventory.clear();
        var cfg = plugin.getMinigameMenuConfig().getConfig();
        Material bgMat = Material.valueOf(cfg.getString("background.material", "GRAY_STAINED_GLASS_PANE").toUpperCase());
        String bgName = ColorUtil.color(cfg.getString("background.name", " "));
        
        this.backgroundItem = new ItemBuilder(bgMat).name(bgName).build();
        for (int i = 0; i < 45; i++) inventory.setItem(i, backgroundItem);
        
        String idleStatus = cfg.getString("status.idle", "&eKlik item saat warna yang tepat!");
        refreshInfo(idleStatus);
    }

    private void refreshInfo(String statusLine) {
        var cfg = plugin.getMinigameMenuConfig().getConfig();
        int slot = cfg.getInt("info_item.slot", 4);
        Material mat = Material.valueOf(cfg.getString("info_item.material", "BOOK").toUpperCase());
        String name = ColorUtil.color(cfg.getString("info_item.name", "&eMinigame Tempa"));
        List<String> loreTemplate = cfg.getStringList("info_item.lore");
        
        List<String> lore = new ArrayList<>();
        for (String line : loreTemplate) {
            String processed = line
                    .replace("{current_round}", String.valueOf(session.getCurrentRound() + 1))
                    .replace("{total_rounds}", String.valueOf(session.getTotalRounds()))
                    .replace("{total_points}", String.valueOf(session.getTotalPoints()))
                    .replace("{status}", ColorUtil.color(statusLine))
                    .replace("{perfect}", String.valueOf(plugin.getConfigManager().getPointsForResult("PERFECT")))
                    .replace("{good}", String.valueOf(plugin.getConfigManager().getPointsForResult("GOOD")))
                    .replace("{broken}", String.valueOf(plugin.getConfigManager().getPointsForResult("BROKEN")))
                    .replace("{miss}", String.valueOf(plugin.getConfigManager().getPointsForResult("MISS")));
            lore.add(ColorUtil.color(processed));
        }
        
        inventory.setItem(slot, new ItemBuilder(mat).name(name).lore(lore).build());
    }

    // ─────────────────────────────────────────────
    // Animation start
    // ─────────────────────────────────────────────

    public void startAnimation() {
        if (session.getCurrentRound() == 0) {
            startCountdown();
        } else {
            startHitPhase();
        }
    }

    private void startCountdown() {
        if (finished) return;
        var cfg = plugin.getMinigameMenuConfig().getConfig();
        
        List<Integer> slotList = cfg.getIntegerList("countdown.slots");
        int[] slots = slotList.isEmpty() ? new int[]{20, 22, 24} : slotList.stream().mapToInt(i -> i).toArray();
        
        Material mat3 = Material.valueOf(cfg.getString("countdown.materials.3", "RED_DYE").toUpperCase());
        Material mat2 = Material.valueOf(cfg.getString("countdown.materials.2", "YELLOW_DYE").toUpperCase());
        Material mat1 = Material.valueOf(cfg.getString("countdown.materials.1", "LIME_DYE").toUpperCase());
        Material[] mats = {mat3, mat2, mat1};
        
        String name3 = cfg.getString("countdown.names.3", "&c3");
        String name2 = cfg.getString("countdown.names.2", "&e2");
        String name1 = cfg.getString("countdown.names.1", "&a1");
        String[] names = {ColorUtil.color(name3), ColorUtil.color(name2), ColorUtil.color(name1)};
        
        // Tampilkan 3 dye
        for (int i = 0; i < 3; i++) {
            inventory.setItem(slots[i], new ItemBuilder(mats[i]).name(names[i]).build());
        }
        playSound("sound_countdown");

        // Hilangkan satu per satu
        for (int i = 0; i < 3; i++) {
            int finalI = i;
            countdownTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (finished || !player.isOnline()) return;
                
                inventory.setItem(slots[finalI], backgroundItem);
                
                if (finalI == 2) {
                    startHitPhase();
                } else {
                    playSound("sound_countdown");
                }
            }, (long) (i + 1) * plugin.getConfigManager().getCountdownDelayTicks())); 
        }
    }

    private void startHitPhase() {
        if (finished) return;
        activeSlot = CLICK_SLOTS[rng.nextInt(CLICK_SLOTS.length)];
        phase      = Phase.MERAH_1;
        phaseTick  = 0;
        waitingForClick = true;

        var cfg = plugin.getMinigameMenuConfig().getConfig();
        Material mat = Material.valueOf(cfg.getString("hit_phase.red.material", "RED_DYE").toUpperCase());
        String name = ColorUtil.color(cfg.getString("hit_phase.red.name", "&cRusak..."));
        setColor(activeSlot, mat, name);
        playSound("sound_spawn");

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, phaseDurationTicks, 1L);
    }

    private void tick() {
        if (finished) { stopTask(); return; }
        phaseTick++;
        if (phaseTick < phaseDurationTicks) return;
        phaseTick = 0;

        phase = switch (phase) {
            case MERAH_1  -> Phase.KUNING_1;
            case KUNING_1 -> Phase.HIJAU;
            case HIJAU    -> Phase.KUNING_2;
            case KUNING_2 -> Phase.MERAH_2;
            default       -> Phase.HILANG;
        };

        var cfg = plugin.getMinigameMenuConfig().getConfig();
        switch (phase) {
            case KUNING_1, KUNING_2 -> {
                Material mat = Material.valueOf(cfg.getString("hit_phase.yellow.material", "YELLOW_DYE").toUpperCase());
                String name = ColorUtil.color(cfg.getString("hit_phase.yellow.name", "&eKlik! &7(Bagus)"));
                setColor(activeSlot, mat, name);
            }
            case HIJAU    -> {
                Material mat = Material.valueOf(cfg.getString("hit_phase.lime.material", "LIME_DYE").toUpperCase());
                String name = ColorUtil.color(cfg.getString("hit_phase.lime.name", "&aSempurna!"));
                setColor(activeSlot, mat, name);
            }
            case MERAH_2  -> {
                Material mat = Material.valueOf(cfg.getString("hit_phase.red.material", "RED_DYE").toUpperCase());
                String name = ColorUtil.color(cfg.getString("hit_phase.red.name", "&cRusak..."));
                setColor(activeSlot, mat, name);
            }
            case HILANG   -> { clearSlot(); waitingForClick = false; stopTask(); onMiss(); }
            default -> {}
        }
    }

    private void setColor(int slot, Material mat, String name) {
        inventory.setItem(slot, new ItemBuilder(mat).name(name).build());
    }

    private void clearSlot() {
        if (activeSlot >= 0)
            inventory.setItem(activeSlot, backgroundItem);
        
        // Clear countdown slots just in case
        var cfg = plugin.getMinigameMenuConfig().getConfig();
        List<Integer> slotList = cfg.getIntegerList("countdown.slots");
        if (slotList.isEmpty()) slotList = List.of(20, 22, 24);
        for (int s : slotList) inventory.setItem(s, backgroundItem);
    }

    // ─────────────────────────────────────────────
    // Click handling
    // ─────────────────────────────────────────────

    /** Dipanggil dari listener. Returns result atau null jika tidak valid. */
    public HitResult handleClick(int slot) {
        if (finished || !waitingForClick || slot != activeSlot) return null;
        stopTask();
        waitingForClick = false;

        HitResult result = switch (phase) {
            case HIJAU           -> HitResult.PERFECT;
            case KUNING_1, KUNING_2 -> HitResult.GOOD;
            case MERAH_1, MERAH_2 -> HitResult.BROKEN;
            default              -> HitResult.MISS;
        };
        clearSlot();
        return result;
    }

    // ─────────────────────────────────────────────
    // Result processing
    // ─────────────────────────────────────────────

    private void onMiss() {
        int pts = plugin.getConfigManager().getPointsForResult("MISS");
        session.addPoints(pts);
        playSound("sound_miss");
        
        String status = plugin.getMinigameMenuConfig().getConfig().getString("status.miss", "&4Meleset! &7({pts} poin)")
                .replace("{pts}", String.valueOf(pts));
        refreshInfo(status);
        
        session.nextRound();
        scheduleNext();
    }

    public void applyResult(HitResult result) {
        if (finished) return;
        int pts = switch (result) {
            case PERFECT -> plugin.getConfigManager().getPointsForResult("PERFECT");
            case GOOD    -> plugin.getConfigManager().getPointsForResult("GOOD");
            case BROKEN  -> plugin.getConfigManager().getPointsForResult("BROKEN");
            case MISS    -> plugin.getConfigManager().getPointsForResult("MISS");
        };
        session.addPoints(pts);

        String snd = switch (result) {
            case PERFECT -> "sound_perfect";
            case GOOD    -> "sound_good";
            case BROKEN  -> "sound_broken";
            case MISS    -> "sound_miss";
        };
        playSound(snd);

        String infoKey = switch (result) {
            case PERFECT -> "status.perfect";
            case GOOD    -> "status.good";
            case BROKEN  -> "status.broken";
            case MISS    -> "status.miss";
        };
        String status = plugin.getMinigameMenuConfig().getConfig().getString(infoKey, "&7Result: {pts}")
                .replace("{pts}", String.valueOf(pts));
        
        refreshInfo(status);
        session.nextRound();
        scheduleNext();
    }

    private void scheduleNext() {
        if (finished) return;

        if (session.isMinigameDone()) {
            // Minigame selesai — resolve quality lalu finalize
            finished = true;
            playSound("sound_finish");
            
            double basePercentage = session.getPointPercentage();
            double bonusPercent = plugin.getConfigManager().getPlayerMinigameBonus(player);
            double finalPercentage = Math.min(100.0, basePercentage * (1.0 + (bonusPercent / 100.0)));
            
            session.setQuality(plugin.getConfigManager().resolveQuality(finalPercentage));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Set transitioning agar close event tidak trigger cancel
                transitioning = true;
                
                if (player.isOnline()) {
                    player.closeInventory();
                    plugin.getForgeManager().finalizeForge(player, session);
                } else {
                    // FALLBACK: Jika player offline saat mau finalize, batalkan dan kembalikan material ke tanah
                    onForcedClose();
                }
            }, 5L); // Kurangi delay jadi 5 tick (0.25 detik) saja agar lebih responsif dan aman

        } else {
            // Ronde berikutnya — Tetap di GUI yang sama
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || finished) return;
                
                // Reset state untuk ronde berikutnya
                activeSlot = -1;
                phase = Phase.HILANG;
                phaseTick = 0;
                waitingForClick = false;
                
                calculatePhaseDuration();
                buildLayout();
                startAnimation();
            }, 15L);
        }
    }

    // ─────────────────────────────────────────────
    // Cancellation (tutup manual / DC)
    // ─────────────────────────────────────────────

    /** Dipanggil saat player menutup GUI secara manual atau disconnect. */
    public void onForcedClose() {
        if (finished || transitioning) return; // Jangan cancel jika sudah selesai atau sedang transisi
        finished = true;
        stopTask();
        
        // Berikan penalti cooldown
        plugin.getForgeManager().setPenalty(player.getUniqueId());

        // Kembalikan material ke player
        for (ItemStack mat : session.getMaterials()) {
            if (mat == null || mat.getType().isAir()) continue;
            if (player.isOnline()) {
                player.getInventory().addItem(mat).values()
                        .forEach(drop -> player.getWorld().dropItem(player.getLocation(), drop));
            } else {
                player.getWorld().dropItem(player.getLocation(), mat);
            }
        }

        if (player.isOnline()) {
            player.sendMessage(ColorUtil.color(
                    plugin.getConfigManager().getMessage("forging_cancelled")));
        }
        plugin.getForgeManager().removeSession(player.getUniqueId());
    }

    // ─────────────────────────────────────────────
    // Sound & task utils
    // ─────────────────────────────────────────────

    private void playSound(String cfgKey) {
        try {
            var cfg = plugin.getConfigManager().getRawConfig();
            String path = "minigame." + cfgKey;
            if (!cfg.contains(path + ".sound")) return;
            String sndName = cfg.getString(path + ".sound", "");
            if (sndName == null || sndName.isEmpty()) return;
            float v = (float) cfg.getDouble(path + ".volume", 1.0);
            float p = (float) cfg.getDouble(path + ".pitch", 1.0);
            player.playSound(player.getLocation(), Sound.valueOf(sndName.toUpperCase()), v, p);
        } catch (Exception ignored) {}
    }

    public void stopTask() {
        if (task != null && !task.isCancelled()) task.cancel();
        for (BukkitTask t : countdownTasks) {
            if (t != null && !t.isCancelled()) t.cancel();
        }
        countdownTasks.clear();
    }

    // ─────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────

    public boolean isFinished()        { return finished; }
    public boolean isTransitioning()   { return transitioning; }
    public boolean isWaitingForClick() { return waitingForClick; }
    public int getActiveSlot()         { return activeSlot; }
    public ForgeSession getSession()   { return session; }

    @Override
    public Inventory getInventory() { return inventory; }
}
