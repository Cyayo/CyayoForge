package id.cyayo.forge.manager;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.gui.ForgeGUI;
import id.cyayo.forge.gui.MinigameGUI;
import id.cyayo.forge.model.*;
import id.cyayo.forge.model.MaterialData.ExtraStat;
import id.cyayo.forge.util.ColorUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.skill.handler.SkillHandler;
import io.lumine.mythic.lib.skill.trigger.TriggerType;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.ItemTier;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.build.MMOItemBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.stat.type.StatHistory;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.stat.data.AbilityData;
import net.Indyuce.mmoitems.stat.data.AbilityListData;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;

public class ForgeManager {

    private final CyayoForge plugin;
    private final AnimationManager animationManager;
    private final Map<UUID, ForgeSession> activeSessions = new HashMap<>();
    private final Map<UUID, MinigameGUI>  activeMinigames = new HashMap<>();
    private final Map<UUID, Long> penaltyCooldowns = new HashMap<>();
    private final Gson gson = new Gson();

    public ForgeManager(CyayoForge plugin) {
        this.plugin           = plugin;
        this.animationManager = new AnimationManager(plugin);
    }

    public void reload() {}
    public AnimationManager getAnimationManager() { return animationManager; }

    // ─────────────────────────────────────────────
    // Session management
    // ─────────────────────────────────────────────

    public void registerMinigame(UUID uuid, MinigameGUI gui) { activeMinigames.put(uuid, gui); }
    public MinigameGUI getActiveMinigame(UUID uuid)          { return activeMinigames.get(uuid); }
    public void removeMinigame(UUID uuid)                     { activeMinigames.remove(uuid); }
    public ForgeSession getSession(UUID uuid)                 { return activeSessions.get(uuid); }

    public boolean hasActiveSession(UUID uuid) {
        return activeSessions.containsKey(uuid)
                || activeMinigames.containsKey(uuid)
                || animationManager.isAnimating(uuid);
    }

    public void removeSession(UUID uuid) {
        activeSessions.remove(uuid);
        activeMinigames.remove(uuid);
        animationManager.cancel(uuid);
    }

    // ─────────────────────────────────────────────
    // Penalty Cooldown
    // ─────────────────────────────────────────────

    public void setPenalty(UUID uuid) {
        if (!plugin.getConfigManager().getRawConfig().getBoolean("minigame.penalty.enabled", true)) return;
        int duration = plugin.getConfigManager().getRawConfig().getInt("minigame.penalty.duration", 60);
        penaltyCooldowns.put(uuid, System.currentTimeMillis() + (duration * 1000L));
    }

    public long getPenaltyTimeLeft(UUID uuid) {
        if (!penaltyCooldowns.containsKey(uuid)) return 0;
        long time = penaltyCooldowns.get(uuid);
        long left = (time - System.currentTimeMillis()) / 1000;
        if (left <= 0) {
            penaltyCooldowns.remove(uuid);
            return 0;
        }
        return left;
    }

    // ─────────────────────────────────────────────
    // Material identification (correct NBT keys)
    // ─────────────────────────────────────────────

    public String identifyMaterial(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        NBTItem nbt = NBTItem.get(item);
        if (nbt.hasType()) {
            String mmoId   = nbt.getString("MMOITEMS_ITEM_ID");
            String mmoType = nbt.getType();
            if (mmoId != null && !mmoId.isEmpty() && plugin.getMaterialConfig().isMaterial(mmoId)) {
                MaterialData d = plugin.getMaterialConfig().getMaterial(mmoId);
                if (d != null && d.isForgeable()) return mmoId.toUpperCase();
            }
            if (mmoType != null && plugin.getMaterialConfig().isMaterialType(mmoType)) {
                // Untuk tipe, kita cek material pertama dari tipe tersebut
                List<MaterialData> list = plugin.getMaterialConfig().getAllMaterials().values().stream()
                        .filter(m -> m.getMmoitemsType() != null && m.getMmoitemsType().equalsIgnoreCase(mmoType))
                        .toList();
                if (!list.isEmpty() && list.get(0).isForgeable())
                    return (mmoId != null && !mmoId.isEmpty()) ? mmoId.toUpperCase() : mmoType.toUpperCase();
            }
        }
        String vanilla = item.getType().name();
        return plugin.getMaterialConfig().isMaterial(vanilla) ? vanilla : null;
    }

    public MaterialData getMaterialData(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        NBTItem nbt = NBTItem.get(item);
        if (nbt.hasType()) {
            String mmoId   = nbt.getString("MMOITEMS_ITEM_ID");
            String mmoType = nbt.getType();
            if (mmoId != null && !mmoId.isEmpty()) {
                MaterialData d = plugin.getMaterialConfig().getMaterial(mmoId);
                if (d != null && d.isForgeable()) return d;
            }
            MaterialData d = plugin.getMaterialConfig().getMaterialFromNBT(mmoId, mmoType);
            if (d != null && d.isForgeable()) return d;
            return null;
        }
        return plugin.getMaterialConfig().getMaterial(item.getType().name());
    }

    public ForgeTier getItemTier(ItemStack item) {
        if (item == null) return ForgeTier.POOR;
        try {
            String t = NBTItem.get(item).getString("MMOITEMS_TIER");
            return (t == null || t.isEmpty()) ? ForgeTier.POOR : ForgeTier.fromString(t);
        } catch (Exception e) { return ForgeTier.POOR; }
    }

    public ForgeTier calculateMajorityTier(List<ItemStack> mats) {
        Map<ForgeTier, Integer> cnt = new EnumMap<>(ForgeTier.class);
        for (ItemStack i : mats) {
            if (i == null || i.getType().isAir()) continue;
            cnt.merge(getItemTier(i), i.getAmount(), Integer::sum);
        }
        return cnt.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(ForgeTier.POOR);
    }

    // ─────────────────────────────────────────────
    // Multiplier calculation
    // ─────────────────────────────────────────────

    public double calculateWeightedMultiplier(List<ItemStack> mats, ForgeType type) {
        double ws = 0; int tc = 0;
        for (ItemStack i : mats) {
            if (i == null || i.getType().isAir()) continue;
            MaterialData m = getMaterialData(i);
            if (m == null) continue;
            double mult = type == ForgeType.WEAPON ? m.getWeaponDamageMultiplier() : m.getArmorHealthMultiplier();
            ws += mult * i.getAmount(); tc += i.getAmount();
        }
        if (tc == 0) return 1.0;
        return Math.min(ws / tc, plugin.getConfigManager().getHardCap());
    }

    private double calcDefenseMultiplier(List<ItemStack> mats) {
        double ws = 0; int tc = 0;
        for (ItemStack i : mats) {
            if (i == null || i.getType().isAir()) continue;
            MaterialData m = getMaterialData(i);
            if (m == null || !m.hasDefenseMultiplier()) continue;
            ws += m.getArmorDefenseMultiplier() * i.getAmount(); tc += i.getAmount();
        }
        return tc == 0 ? 1.0 : ws / tc;
    }

    public double calculateWeightedArmorMultiplier(List<ItemStack> mats) {
        double ws = 0; int tc = 0;
        for (ItemStack i : mats) {
            if (i == null || i.getType().isAir()) continue;
            MaterialData m = getMaterialData(i);
            if (m == null) continue;
            ws += m.getArmorMultiplier() * i.getAmount(); tc += i.getAmount();
        }
        return tc == 0 ? 1.0 : ws / tc;
    }

    public double calculateWeightedCritDamageMultiplier(List<ItemStack> mats) {
        double ws = 0; int tc = 0;
        for (ItemStack i : mats) {
            if (i == null || i.getType().isAir()) continue;
            MaterialData m = getMaterialData(i);
            if (m == null) continue;
            ws += m.getCritDamageMultiplier() * i.getAmount(); tc += i.getAmount();
        }
        return tc == 0 ? 1.0 : ws / tc;
    }

    // ─────────────────────────────────────────────
    // Ability collection
    // ─────────────────────────────────────────────

    public List<AbilityModifier> collectAbilities(List<ItemStack> mats, ForgeType type) {
        if (type != ForgeType.WEAPON) return Collections.emptyList();
        double threshold = plugin.getConfigManager().getAbilityThresholdPercent() / 100.0;
        int total = mats.stream().filter(i -> i != null && !i.getType().isAir()).mapToInt(ItemStack::getAmount).sum();

        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack i : mats) {
            if (i == null || i.getType().isAir()) continue;
            String id = identifyMaterial(i); if (id == null) continue;
            counts.merge(id, i.getAmount(), Integer::sum);
        }

        Map<String, AbilityModifier> result = new HashMap<>();
        for (var e : counts.entrySet()) {
            if ((double) e.getValue() / total < threshold) continue;
            MaterialData m = plugin.getMaterialConfig().getMaterial(e.getKey());
            if (m == null || !m.hasWeaponAbilities()) continue;
            for (AbilityModifier ab : m.getWeaponAbilities())
                result.merge(ab.getAbilityId() + ":" + ab.getTrigger(), ab, AbilityModifier::mergeWith);
        }
        return new ArrayList<>(result.values());
    }

    // ─────────────────────────────────────────────
    // Forge initiation
    // ─────────────────────────────────────────────

    public void initiateForge(Player player, ForgeGUI gui) {
        List<ItemStack> mats = gui.getAllMaterials();
        int total = mats.stream().mapToInt(ItemStack::getAmount).sum();
        ForgeType type = gui.getCurrentType();

        ForgeRecipe recipe = plugin.getRecipeConfig().getRecipe(type, total, player);
        if (recipe == null) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("forging_failed")));
            return;
        }

        ForgeTier tier         = calculateMajorityTier(mats);
        double multiplier      = calculateWeightedMultiplier(mats, type);
        List<AbilityModifier> abilities = collectAbilities(mats, type);
        OutputTemplate output  = rollForPlayer(player, recipe);

        if (output == null) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("no_output_access")));
            return;
        }

        // Trigger ForgeStartEvent
        id.cyayo.forge.api.event.ForgeStartEvent startEvent = new id.cyayo.forge.api.event.ForgeStartEvent(player, new ArrayList<>(mats), recipe, type);
        org.bukkit.Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) return;

        int minRounds = plugin.getConfigManager().getMinigameRoundsMin();
        int maxRounds = plugin.getConfigManager().getMinigameRoundsMax();
        int totalRounds = minRounds + new java.util.Random().nextInt(Math.max(1, maxRounds - minRounds + 1));

        int maxPts = plugin.getConfigManager().getPointsForResult("PERFECT");
        ForgeSession session = new ForgeSession(player.getUniqueId(),
                new ArrayList<>(mats), recipe, tier, output, multiplier, abilities, totalRounds, maxPts);
        activeSessions.put(player.getUniqueId(), session);

        // Hapus material dari GUI (GUI sudah ditutup setelah ini)
        gui.clearMaterials();
        player.closeInventory();

        boolean skipAnim = player.hasPermission("cyayoforge.bypass.animation");
        boolean skipMini = player.hasPermission("cyayoforge.bypass.minigame");

        if (!plugin.getConfigManager().isAnimationEnabled() || skipAnim) {
            startMiniOrFinalize(player, session, skipMini);
        } else {
            animationManager.playAnimation(player, session,
                    () -> startMiniOrFinalize(player, session, skipMini));
        }
    }

    private OutputTemplate rollForPlayer(Player player, ForgeRecipe recipe) {
        OutputTemplate fallback = null;
        for (OutputTemplate out : recipe.getOutputs()) {
            // Check permission
            String perm = out.getPermission();
            boolean hasPerm = perm == null || perm.isEmpty() || (player != null && player.hasPermission(perm));

            // Check regions
            boolean inRegion = player != null && id.cyayo.forge.util.RegionUtil.isInAnyRegion(player, out.getRegions());

            // If neither permission nor region is satisfied, skip this item
            if (!hasPerm && !inRegion) continue;

            if (out.getBase() == 1) { if (fallback == null) fallback = out; continue; }
            if (out.roll()) return out;
        }
        return fallback;
    }

    private void startMiniOrFinalize(Player player, ForgeSession session, boolean skip) {
        if (!plugin.getConfigManager().isMinigameEnabled() || skip) {
            session.setQuality(plugin.getConfigManager().getBypassQuality());
            finalizeForge(player, session);
        } else {
            MinigameGUI mg = new MinigameGUI(plugin, player, session);
            player.openInventory(mg.getInventory());
            mg.startAnimation();
            registerMinigame(player.getUniqueId(), mg);
        }
    }

    // ─────────────────────────────────────────────
    // Finalization
    // ─────────────────────────────────────────────

    public void finalizeForge(Player player, ForgeSession session) {
        removeMinigame(player.getUniqueId());

        ItemStack output = buildOutput(session);
        if (output == null) {
            plugin.getLogger().warning("Gagal membuat item output untuk " + player.getName() + ". Menjalankan prosedur pengembalian material.");
            org.bukkit.Bukkit.getPluginManager().callEvent(new id.cyayo.forge.api.event.ForgeFailureEvent(player, session));
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("forging_failed")));
            
            // PENGAMAN: Pastikan material dikembalikan jika gagal build
            for (ItemStack m : session.getMaterials()) {
                if (m != null && !m.getType().isAir()) {
                    if (player.isOnline()) {
                        player.getInventory().addItem(m).values()
                                .forEach(d -> player.getWorld().dropItem(player.getLocation(), d));
                    } else {
                        player.getWorld().dropItem(player.getLocation(), m);
                    }
                }
            }
            removeSession(player.getUniqueId());
            return;
        }

        // Trigger Success Event
        org.bukkit.Bukkit.getPluginManager().callEvent(new id.cyayo.forge.api.event.ForgeSuccessEvent(player, session, output));

        // Inject quality
        output = applyQualityLore(output, session);
        // Inject penempa
        output = applySmithLore(output, player.getName());

        player.getInventory().addItem(output).values()
                .forEach(d -> player.getWorld().dropItem(player.getLocation(), d));

        String itemName = output.hasItemMeta() && output.getItemMeta().hasDisplayName()
                ? output.getItemMeta().getDisplayName() : output.getType().name();

        plugin.getPlayerDataManager().recordForge(
                player.getUniqueId(), session.getRecipe().getForgeType(), itemName);

        String qColor = switch (session.getQuality()) {
            case PERFECT -> "&6"; case EXQUISITE -> "&b"; case GOOD -> "&a";
            case FLAWED -> "&e";  case BROKEN -> "&c";
        };
        String qStr = plugin.getConfigManager().getQualityStatString().get(session.getQuality());
        player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("forging_success",
                "{item}", itemName, "{quality}", qColor + qStr,
                "{tier}", plugin.getConfigManager().getTierDisplayName(session.getResultTier()))));

        try {
            var cfg = plugin.getConfigManager().getRawConfig();
            String snd = cfg.getString("forging.sound_success.sound", "BLOCK_ANVIL_USE");
            float v = (float) cfg.getDouble("forging.sound_success.volume", 1.0);
            float p = (float) cfg.getDouble("forging.sound_success.pitch", 1.2);
            player.playSound(player.getLocation(), Sound.valueOf(snd), v, p);
        } catch (Exception ignored) {}

        removeSession(player.getUniqueId());
    }

    // ─────────────────────────────────────────────
    // Item building
    // ─────────────────────────────────────────────

    public ItemStack buildOutput(ForgeSession session) {
        try {
            String templateId = session.getSelectedOutput().getTemplateId(session.getResultTier());
            MMOItemTemplate template = findTemplate(templateId);
            if (template == null) { plugin.getLogger().warning("Template tidak ditemukan: " + templateId); return null; }

            ItemTier mmoTier = MMOItems.plugin.getTiers().get(session.getResultTier().name());
            MMOItemBuilder builder = new MMOItemBuilder(template, 0, mmoTier, false);
            MMOItem mmoItem = builder.build();

            applyBaseMultiplier(mmoItem, session);
            applyExtraStats(mmoItem, session);
            if (!session.getInjectedAbilities().isEmpty()) applyAbilities(mmoItem, session);
            applyCustomQualityStat(mmoItem, session);
            applyCustomSmithStat(mmoItem, session);

            ItemStack item = new ItemStackBuilder(mmoItem).build();
            return injectMaterialData(item, session);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Gagal membuat output tempa", e);
            return null;
        }
    }

    private MMOItemTemplate findTemplate(String id) {
        for (Type t : MMOItems.plugin.getTypes().getAll()) {
            try { MMOItemTemplate tmpl = MMOItems.plugin.getTemplates().getTemplate(t, id); if (tmpl != null) return tmpl; }
            catch (Exception ignored) {}
        }
        return null;
    }

    private void applyBaseMultiplier(MMOItem mmoItem, ForgeSession session) {
        try {
            double qBonus  = plugin.getConfigManager().getQualityDamageBonus()
                    .getOrDefault(session.getQuality(), 0.0);
            double factor  = 1.0 + qBonus / 100.0;
            double mult    = session.getFinalMultiplier();

            if (session.getRecipe().getForgeType() == ForgeType.WEAPON) {
                applyDoubleStatMult(mmoItem, ItemStats.ATTACK_DAMAGE, mult * factor);
                
                double critMult = calculateWeightedCritDamageMultiplier(session.getMaterials());
                if (critMult > 1.0) applyDoubleStatMult(mmoItem, ItemStats.CRITICAL_STRIKE_POWER, critMult);
            } else {
                applyDoubleStatMult(mmoItem, ItemStats.MAX_HEALTH, mult * factor);
                
                double defMult = calcDefenseMultiplier(session.getMaterials());
                if (defMult > 1.0) applyDoubleStatMult(mmoItem, ItemStats.ARMOR, defMult);
                
                double armMult = calculateWeightedArmorMultiplier(session.getMaterials());
                if (armMult > 1.0) applyDoubleStatMult(mmoItem, ItemStats.ARMOR, armMult);
            }
        } catch (Exception e) { plugin.getLogger().log(Level.WARNING, "Gagal menerapkan multiplier", e); }
    }

    private void applyDoubleStatMult(MMOItem mmoItem, ItemStat stat, double multiplier) {
        if (multiplier <= 1.0) return;

        // Mendapatkan history stat (Base + Modifiers + Gemstones dsb)
        StatHistory history = StatHistory.from(mmoItem, stat);
        
        // Hitung total nilai saat ini (tanpa upgrade)
        // recalculateUnupgraded mengembalikan StatData yang berisi total value
        DoubleData totalData = (DoubleData) history.recalculateUnupgraded(false);
        double currentTotal = totalData.getValue();
        
        // Hitung berapa bonus yang harus ditambahkan untuk mencapai multiplier target
        // Misal: total 10, mult 2.81 -> bonus = 10 * (2.81 - 1) = 18.1
        double bonusValue = currentTotal * (multiplier - 1.0);
        
        // Daftarkan sebagai bonus modifier baru dengan UUID acak agar tidak bentrok
        history.registerModifierBonus(UUID.randomUUID(), new DoubleData(bonusValue));
        
        // plugin.getLogger().info("[Forge] Stat " + stat.getId() + " total " + currentTotal + " -> bonus " + bonusValue);
    }

    /**
     * Injeksi stat tambahan fleksibel dari material.
     * Mendukung semua stat MMOItems via reflection pada ItemStats.
     * Mode: FIXED, MULTIPLY (dari base stat yang ada), RANDOM (min-max).
     */
    private void applyExtraStats(MMOItem mmoItem, ForgeSession session) {
        // Kumpulkan semua extra stats dari semua material (weighted/merge)
        Map<String, Double> mergedStats = new HashMap<>();

        for (ItemStack item : session.getMaterials()) {
            if (item == null || item.getType().isAir()) continue;
            MaterialData mat = getMaterialData(item);
            if (mat == null) continue;

            List<ExtraStat> extras = session.getRecipe().getForgeType() == ForgeType.WEAPON
                    ? mat.getWeaponExtraStats()
                    : mat.getArmorExtraStats();

            if (extras == null || extras.isEmpty()) continue;

            for (ExtraStat extra : extras) {
                // Ambil nilai base stat yang ada di item jika mode MULTIPLY
                double baseVal = 0;
                if (extra.getMode() == MaterialData.StatMode.MULTIPLY) {
                    ItemStat istat = getItemStat(extra.getStatKey());
                    if (istat != null && mmoItem.hasData(istat)) {
                        DoubleData d = (DoubleData) mmoItem.getData(istat);
                        if (d != null) baseVal = d.getValue();
                    }
                }

                double resolvedVal = extra.resolve(baseVal);

                // Merge: Ambil nilai TERTINGGI (Highest) jika stat sama
                mergedStats.merge(extra.getStatKey(), resolvedVal, Double::max);
            }
        }

        // Terapkan stat ke MMOItem
        for (Map.Entry<String, Double> e : mergedStats.entrySet()) {
            try {
                ItemStat stat = getItemStat(e.getKey());
                if (stat == null) {
                    plugin.getLogger().warning("Stat tidak dikenal: " + e.getKey());
                    continue;
                }
                
                double valueToAdd = e.getValue();
                
                if (mmoItem.hasData(stat)) {
                    DoubleData existing = (DoubleData) mmoItem.getData(stat);
                    if (existing != null) {
                        double currentVal = existing.getValue();
                        mmoItem.setData(stat, new DoubleData(currentVal + valueToAdd));
                    }
                } else {
                    mmoItem.setData(stat, new DoubleData(valueToAdd));
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Gagal inject stat: " + e.getKey(), ex);
            }
        }
    }

    /** Lookup ItemStat dari ItemStats class via field name (reflection). */
    private ItemStat getItemStat(String key) {
        try {
            Field f = ItemStats.class.getField(key.toUpperCase());
            return (ItemStat) f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void applyAbilities(MMOItem mmoItem, ForgeSession session) {
        try {
            double baseDmg = 0;
            if (mmoItem.hasData(ItemStats.ATTACK_DAMAGE)) {
                DoubleData d = (DoubleData) mmoItem.getData(ItemStats.ATTACK_DAMAGE);
                if (d != null) baseDmg = d.getValue();
            }
            double abilityDmg = baseDmg * (1.0 + plugin.getConfigManager().getAbilityDamagePercent() / 100.0);

            AbilityListData list = mmoItem.hasData(ItemStats.ABILITIES)
                    ? (AbilityListData) mmoItem.getData(ItemStats.ABILITIES)
                    : new AbilityListData();
            if (list == null) list = new AbilityListData();

            for (AbilityModifier mod : session.getInjectedAbilities()) {
                try {
                    SkillHandler<?> handler = MythicLib.plugin.getSkills().getHandler(mod.getAbilityId());
                    if (handler == null) continue;
                    TriggerType trigger = MMOUtils.backwardsCompatibleTriggerType(mod.getTrigger());
                    AbilityData ab = new AbilityData(handler, trigger);
                    ab.setModifier("damage", abilityDmg);

                    double scoreFactor = Math.max(0.0, Math.min(1.0, session.getPointPercentage() / 100.0));
                    double finalFactor = Math.max(0.0, Math.min(1.0, (scoreFactor * 0.7) + (Math.random() * 0.3)));

                    for (Map.Entry<String, double[]> entry : mod.getModifiers().entrySet()) {
                        double min = entry.getValue()[0];
                        double max = entry.getValue()[1];
                        double lowest = Math.min(min, max);
                        double highest = Math.max(min, max);
                        double val;

                        if (entry.getKey().equalsIgnoreCase("cooldown")) {
                            // Semakin bagus skor (finalFactor 1.0) -> cooldown semakin cepat (lowest)
                            val = highest - (highest - lowest) * finalFactor;
                        } else {
                            // Semakin bagus skor -> durasi/stat lain semakin besar (highest)
                            val = lowest + (highest - lowest) * finalFactor;
                        }
                        
                        ab.setModifier(entry.getKey(), val);
                    }

                    list.add(ab);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Gagal inject ability: " + mod.getAbilityId(), e);
                }
            }
            mmoItem.setData(ItemStats.ABILITIES, list);
        } catch (Exception e) { plugin.getLogger().log(Level.WARNING, "Gagal menerapkan abilities", e); }
    }

    private void applyCustomQualityStat(MMOItem mmoItem, ForgeSession session) {
        var cfg = plugin.getConfigManager().getRawConfig();
        if (!cfg.getBoolean("minigame.custom_stat.enabled", false)) return;

        String statId = cfg.getString("minigame.custom_stat.id", "CUSTOM_QUALITY");
        String qualityStr = plugin.getConfigManager().getQualityStatString()
                .getOrDefault(session.getQuality(), session.getQuality().name());

        ItemStat stat = MMOItems.plugin.getStats().get(statId);
        if (stat != null) {
            mmoItem.setData(stat, new StringData(qualityStr));
        }
    }

    private void applyCustomSmithStat(MMOItem mmoItem, ForgeSession session) {
        if (!session.isInjectSmith()) return;
        var cfg = plugin.getConfigManager().getRawConfig();
        if (!cfg.getBoolean("minigame.custom_penempa.enabled", false)) return;

        String statId = cfg.getString("minigame.custom_penempa.id", "CUSTOM_PENEMPA");
        String playerName = org.bukkit.Bukkit.getOfflinePlayer(session.getPlayerUUID()).getName();
        if (playerName == null) playerName = "Unknown";

        ItemStat stat = MMOItems.plugin.getStats().get(statId);
        if (stat != null) {
            mmoItem.setData(stat, new StringData(playerName));
        }
    }

    // ─────────────────────────────────────────────
    // Quality Lore injection
    // ─────────────────────────────────────────────

    public ItemStack applyQualityLore(ItemStack item, ForgeSession session) {
        String qualityStr = plugin.getConfigManager().getQualityStatString()
                .getOrDefault(session.getQuality(), session.getQuality().name());
        try {
            if (!item.hasItemMeta()) return item;
            var meta = item.getItemMeta();
            var lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<String>();
            
            String placeholder = plugin.getConfigManager().getRawConfig().getString("minigame.quality_placeholder", "%%QUALITY%%");
            String format = plugin.getConfigManager().getRawConfig().getString("minigame.quality_lore_format", " &7Quality : {value}");
            String replacement = ColorUtil.color(format.replace("{value}", qualityStr));
            
            boolean found = false;
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line.contains(placeholder)) {
                    lore.set(i, line.replace(placeholder, replacement));
                    found = true;
                }
            }
            
            if (!found && plugin.getConfigManager().getRawConfig().getBoolean("minigame.append_quality_if_no_placeholder", true)) {
                lore.add(replacement);
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Gagal inject quality ke lore", e);
        }
        return item;
    }

    public ItemStack applySmithLore(ItemStack item, String playerName) {
        try {
            if (!item.hasItemMeta()) return item;
            var meta = item.getItemMeta();
            var lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<String>();

            String placeholder = plugin.getConfigManager().getRawConfig().getString("minigame.penempa_placeholder", "%%PENEMPA%%");
            String format = plugin.getConfigManager().getRawConfig().getString("minigame.penempa_lore_format", " &7Penempa : {value}");
            String replacement = ColorUtil.color(format.replace("{value}", playerName));

            boolean found = false;
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line.contains(placeholder)) {
                    lore.set(i, line.replace(placeholder, replacement));
                    found = true;
                }
            }

            if (!found && plugin.getConfigManager().getRawConfig().getBoolean("minigame.append_penempa_if_no_placeholder", true)) {
                lore.add(replacement);
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Gagal inject penempa ke lore", e);
        }
        return item;
    }

    // ─────────────────────────────────────────────
    // Salvage Helpers
    // ─────────────────────────────────────────────

    private ItemStack injectMaterialData(ItemStack item, ForgeSession session) {
        if (item == null || session == null) return item;
        try {
            List<Map<String, Object>> data = new ArrayList<>();
            for (ItemStack mat : session.getMaterials()) {
                Map<String, Object> entry = new HashMap<>();
                String id = identifyMaterial(mat);
                if (id == null) continue;
                
                entry.put("id", id);
                entry.put("amount", mat.getAmount());
                
                String displayName = mat.hasItemMeta() && mat.getItemMeta().hasDisplayName() ? 
                                    mat.getItemMeta().getDisplayName() : 
                                    formatIdToName(mat.getType().name());
                entry.put("display", displayName);
                data.add(entry);
            }
            
            String json = gson.toJson(data);
            NBTItem nbt = NBTItem.get(item);
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYOFORGE_MATERIALS", json));
            
            // Injeksi nama penempa
            String playerName = org.bukkit.Bukkit.getOfflinePlayer(session.getPlayerUUID()).getName();
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYOFORGE_SMITH", playerName != null ? playerName : "Unknown"));
            
            // Injeksi kualitas
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYOFORGE_QUALITY", session.getQuality().name()));
            
            // Injeksi tanggal
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("CYAYOFORGE_DATE", new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date())));

            return nbt.toItem();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Gagal inject data tempa ke NBT", e);
            return item;
        }
    }

    public List<Map<String, Object>> getMaterialDataFromNBT(ItemStack item) {
        if (item == null) return Collections.emptyList();
        NBTItem nbt = NBTItem.get(item);
        String json = nbt.getString("CYAYOFORGE_MATERIALS");
        if (json == null || json.isEmpty()) return Collections.emptyList();
        
        try {
            return gson.fromJson(json, new TypeToken<List<Map<String, Object>>>(){}.getType());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public String getSmithFromNBT(ItemStack item) {
        if (item == null) return "Unknown";
        return NBTItem.get(item).getString("CYAYOFORGE_SMITH");
    }

    public String getQualityFromNBT(ItemStack item) {
        if (item == null) return "BROKEN";
        return NBTItem.get(item).getString("CYAYOFORGE_QUALITY");
    }

    public String getDateFromNBT(ItemStack item) {
        if (item == null) return "-";
        return NBTItem.get(item).getString("CYAYOFORGE_DATE");
    }

    private String formatIdToName(String id) {
        if (id == null) return "";
        String[] words = id.replace("_", " ").toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
