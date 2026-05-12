package id.cyayo.forge.manager;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.model.ForgeQuality;
import id.cyayo.forge.model.ForgeTier;
import id.cyayo.forge.model.SalvageTask;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SalvageManager {

    private final CyayoForge plugin;

    public SalvageManager(CyayoForge plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getOpenInventory().getTopInventory().getHolder() instanceof id.cyayo.forge.gui.SalvageMenuGUI gui) {
                        gui.populate();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public long calculateSalvageTime(Player player, ItemStack item) {
        if (item == null) return 0;
        
        var cfg = plugin.getConfigManager().getRawConfig();
        
        // Ambil Tier & Quality dari item
        ForgeTier tier = plugin.getForgeManager().getItemTier(item);
        
        // Base time per tier
        long baseTime = cfg.getLong("salvage.base_time_per_tier." + tier.name(), 60);
        
        // Multiplier quality
        double multiplier = 1.0;
        if (cfg.getBoolean("salvage.use_quality_multiplier", true)) {
            // Kita coba ambil quality dari NBT atau Lore (via ForgeManager)
            // Di sini kita asumsikan ForgeManager bisa mendeteksi quality
            // Karena ini premium, kita cari tag QUALITY di item
            ForgeQuality quality = detectQuality(item);
            multiplier = cfg.getDouble("salvage.quality_time_multiplier." + quality.name(), 1.0);
        }
        
        double finalTime = baseTime * multiplier;
        
        // Timer Reduction Permissions
        double reduction = getTimerReduction(player);
        finalTime = finalTime * (1.0 - reduction);
        
        return (long) finalTime;
    }

    private ForgeQuality detectQuality(ItemStack item) {
        if (item == null) return ForgeQuality.GOOD;
        try {
            NBTItem nbt = NBTItem.get(item);
            var cfg = plugin.getConfigManager().getRawConfig();
            String statId = cfg.getString("minigame.custom_stat.id", "CUSTOM_QUALITY");
            
            // MMOItems simpan custom string stat di NBT dengan prefix MMOITEMS_
            String qStr = nbt.getString("MMOITEMS_" + statId.toUpperCase());
            if (qStr == null || qStr.isEmpty()) {
                // Coba cari dari Lore jika tidak ada di NBT (opsional, tapi NBT lebih akurat)
                return ForgeQuality.GOOD;
            }
            
            // Kita perlu map string hasil gradient ke Enum. 
            // Karena ini agak sulit, kita cek satu per satu dari config
            Map<ForgeQuality, String> qualityMap = plugin.getConfigManager().getQualityStatString();
            for (Map.Entry<ForgeQuality, String> entry : qualityMap.entrySet()) {
                if (qStr.contains(entry.getValue()) || entry.getValue().contains(qStr)) {
                    return entry.getKey();
                }
            }
        } catch (Exception ignored) {}
        return ForgeQuality.GOOD; 
    }

    private double getTimerReduction(Player player) {
        if (player.hasPermission("forge.salvage.timer.reduce.5")) return 1.0;
        if (player.hasPermission("forge.salvage.timer.reduce.4")) return 0.8;
        if (player.hasPermission("forge.salvage.timer.reduce.3")) return 0.6;
        if (player.hasPermission("forge.salvage.timer.reduce.2")) return 0.4;
        if (player.hasPermission("forge.salvage.timer.reduce.1")) return 0.2;
        return 0.0;
    }

    public void startSalvage(Player player, int slotIndex, ItemStack item) {
        List<Map<String, Object>> materials = plugin.getForgeManager().getMaterialDataFromNBT(item);
        if (materials.isEmpty()) return;
        
        long durationSeconds = calculateSalvageTime(player, item);
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000L);

        // Trigger SalvageStartEvent
        id.cyayo.forge.api.event.SalvageStartEvent startEvent = new id.cyayo.forge.api.event.SalvageStartEvent(player, item, slotIndex);
        org.bukkit.Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) return;
        
        SalvageTask task = new SalvageTask(slotIndex, item.clone(), materials, startTime, endTime);
        plugin.getPlayerDataManager().getData(player.getUniqueId()).addSalvageTask(task);
        plugin.getPlayerDataManager().save(player.getUniqueId());

        plugin.getConfigManager().sendMessage(player, "salvage_started");
    }

    public void cancelSalvage(Player player, int slotIndex) {
        var data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        SalvageTask task = data.getSalvageTask(slotIndex);
        if (task == null) return;
        
        // Kembalikan item asli
        player.getInventory().addItem(task.getOriginalItem()).values()
                .forEach(drop -> player.getWorld().dropItem(player.getLocation(), drop));
        
        data.removeSalvageTask(slotIndex);
        plugin.getPlayerDataManager().save(player.getUniqueId());

        plugin.getConfigManager().sendMessage(player, "salvage_cancelled");
    }

    public Map<String, Integer> calculateReturnMaterials(List<Map<String, Object>> materials) {
        double factor = plugin.getConfigManager().getRawConfig().getDouble("salvage.return_percentage", 0.5);
        
        // 1. Hitung total input
        int totalInput = 0;
        for (Map<String, Object> matData : materials) {
            if (matData.get("amount") instanceof Number) {
                totalInput += ((Number) matData.get("amount")).intValue();
            }
        }
        
        // 2. Batas global (50%)
        int maxReturnTotal = (int) Math.floor(totalInput * factor);
        if (maxReturnTotal <= 0 && totalInput > 0) maxReturnTotal = 1;
        
        // 3. Sortir material (Tier Proxy)
        List<Map<String, Object>> sorted = new java.util.ArrayList<>(materials);
        sorted.sort((m1, m2) -> {
            var data1 = plugin.getMaterialConfig().getMaterial((String) m1.get("id"));
            var data2 = plugin.getMaterialConfig().getMaterial((String) m2.get("id"));
            double v1 = data1 != null ? Math.max(data1.getWeaponDamageMultiplier(), data1.getArmorHealthMultiplier()) : 0;
            double v2 = data2 != null ? Math.max(data2.getWeaponDamageMultiplier(), data2.getArmorHealthMultiplier()) : 0;
            return Double.compare(v2, v1);
        });
        
        // 4. Hitung hasil final
        Map<String, Integer> result = new java.util.LinkedHashMap<>();
        int currentTotal = 0;
        
        for (Map<String, Object> matData : sorted) {
            if (currentTotal >= maxReturnTotal) break;
            
            String id = (String) matData.get("id");
            int amount = 0;
            if (matData.get("amount") instanceof Number) {
                amount = ((Number) matData.get("amount")).intValue();
            }
            
            int returnAmountForItem = amount == 1 ? 1 : (int) Math.floor(amount * factor);
            int available = maxReturnTotal - currentTotal;
            int actualReturn = Math.min(returnAmountForItem, available);
            
            if (actualReturn > 0) {
                result.put(id, actualReturn);
                currentTotal += actualReturn;
            }
        }
        
        return result;
    }

    public void claimSalvage(Player player, int slotIndex) {
        var data = plugin.getPlayerDataManager().getData(player.getUniqueId());
        SalvageTask task = data.getSalvageTask(slotIndex);
        if (task == null) return;

        if (!task.isFinished()) {
            plugin.getConfigManager().sendMessage(player, "salvage_claim_fail");
            return;
        }
        
        // 1. Berikan material utama (50% rule)
        List<ItemStack> allItemsGiven = new java.util.ArrayList<>();
        Map<String, Integer> toReturn = calculateReturnMaterials(task.getMaterials());
        String matFormat = plugin.getConfigManager().getMessage("salvage_material_entry");
        List<String> materialLines = new java.util.ArrayList<>();
        for (Map.Entry<String, Integer> entry : toReturn.entrySet()) {
            ItemStack matItem = plugin.getMaterialConfig().createMaterial(entry.getKey(), entry.getValue());
            if (matItem != null) {
                allItemsGiven.add(matItem.clone());
                player.getInventory().addItem(matItem).values()
                        .forEach(drop -> player.getWorld().dropItem(player.getLocation(), drop));
                
                String name = matItem.hasItemMeta() && matItem.getItemMeta().hasDisplayName() ? 
                             matItem.getItemMeta().getDisplayName() : formatIdToName(entry.getKey());
                
                String line = matFormat.replace("{amount}", String.valueOf(entry.getValue()))
                                       .replace("{material}", name);
                materialLines.add(id.cyayo.forge.util.ColorUtil.color(line));
            }
        }

        // 2. Berikan bonus tambahan berdasarkan Tier
        List<String> bonusLines = giveBonusMaterials(player, task, allItemsGiven);

        // Trigger Salvage Claim Event
        org.bukkit.Bukkit.getPluginManager().callEvent(new id.cyayo.forge.api.event.SalvageClaimEvent(player, task, allItemsGiven));

        // 3. Send Success Message
        String itemName = task.getOriginalItem().hasItemMeta() && task.getOriginalItem().getItemMeta().hasDisplayName() ?
                          task.getOriginalItem().getItemMeta().getDisplayName() : formatIdToName(task.getOriginalItem().getType().name());

        String bonusStr = "";
        if (!bonusLines.isEmpty()) {
            bonusStr = "\n" + plugin.getConfigManager().getMessage("salvage_bonus_header") + "\n" + String.join("\n", bonusLines);
        }

        plugin.getConfigManager().sendMessage(player, "salvage_success", 
            "{item}", itemName,
            "{materials}", String.join("\n", materialLines),
            "{bonus}", bonusStr
        );
        
        data.removeSalvageTask(slotIndex);
        plugin.getPlayerDataManager().save(player.getUniqueId());
    }

    private List<String> giveBonusMaterials(Player player, SalvageTask task, List<ItemStack> allItemsGiven) {
        List<String> result = new java.util.ArrayList<>();
        if (!plugin.getSalvageBonusConfig().isEnabled()) return result;
        
        id.cyayo.forge.model.ForgeTier tier = plugin.getForgeManager().getItemTier(task.getOriginalItem());
        var bonusData = plugin.getSalvageBonusConfig().getBonus(tier);
        if (bonusData == null) return result;

        double chance = bonusData.getChance();
        if (chance < 100.0) {
            double roll = ThreadLocalRandom.current().nextDouble(100.0);
            if (roll >= chance) return result;
        }

        String bonusFormat = plugin.getConfigManager().getMessage("salvage_bonus_entry");

        for (id.cyayo.forge.config.SalvageBonusConfig.BonusItem bonusItem : bonusData.getItems()) {
            int amount = bonusItem.rollAmount();
            if (amount <= 0) continue;

            if (bonusItem.getType() == id.cyayo.forge.config.SalvageBonusConfig.BonusType.COMMAND) {
                String cmd = bonusItem.getCommand().replace("{player}", player.getName());
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), cmd);
                if (bonusItem.getDisplay() != null) {
                    result.add(id.cyayo.forge.util.ColorUtil.color(bonusItem.getDisplay()));
                }
            } else {
                ItemStack item = createBonusItem(bonusItem, amount);
                if (item != null) {
                    allItemsGiven.add(item.clone());
                    player.getInventory().addItem(item).values()
                            .forEach(drop -> player.getWorld().dropItem(player.getLocation(), drop));
                    
                    String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? 
                                 item.getItemMeta().getDisplayName() : 
                                 formatIdToName(item.getType().name());
                    
                    String line = bonusFormat.replace("{amount}", String.valueOf(amount))
                                             .replace("{material}", name);
                    result.add(id.cyayo.forge.util.ColorUtil.color(line));
                }
            }
        }
        return result;
    }

    private ItemStack createBonusItem(id.cyayo.forge.config.SalvageBonusConfig.BonusItem bonusItem, int amount) {
        if (bonusItem.getType() == id.cyayo.forge.config.SalvageBonusConfig.BonusType.MMOITEMS) {
            String mmoType = bonusItem.getMmoType();
            String id = bonusItem.getId();
            if (mmoType == null || id == null) return null;
            
            net.Indyuce.mmoitems.api.Type type = net.Indyuce.mmoitems.api.Type.get(mmoType);
            if (type == null) return null;
            
            ItemStack item = net.Indyuce.mmoitems.MMOItems.plugin.getItem(type, id);
            if (item != null) item.setAmount(amount);
            return item;
        } else if (bonusItem.getType() == id.cyayo.forge.config.SalvageBonusConfig.BonusType.VANILLA) {
            try {
                org.bukkit.Material mat = org.bukkit.Material.valueOf(bonusItem.getMaterial().toUpperCase());
                return new ItemStack(mat, amount);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public List<String> getBonusPreview(ItemStack item) {
        List<String> preview = new java.util.ArrayList<>();
        if (!plugin.getSalvageBonusConfig().isEnabled()) return preview;

        id.cyayo.forge.model.ForgeTier tier = plugin.getForgeManager().getItemTier(item);
        var bonusData = plugin.getSalvageBonusConfig().getBonus(tier);
        if (bonusData == null || bonusData.getItems().isEmpty()) return preview;

        preview.add("");
        preview.add(id.cyayo.forge.util.ColorUtil.color("&6&l⭐ &eBonus Kemungkinan: &7(" + bonusData.getChance() + "%)"));
        
        for (id.cyayo.forge.config.SalvageBonusConfig.BonusItem bonusItem : bonusData.getItems()) {
            String display = "";
            if (bonusItem.getType() == id.cyayo.forge.config.SalvageBonusConfig.BonusType.COMMAND) {
                display = bonusItem.getDisplay() != null ? bonusItem.getDisplay() : plugin.getSalvageMenuConfig().getConfig().getString("format.command_bonus_default", "&7Bonus Perintah");
            } else {
                ItemStack matItem = createBonusItem(bonusItem, 1);
                if (matItem != null && matItem.hasItemMeta() && matItem.getItemMeta().hasDisplayName()) {
                    display = matItem.getItemMeta().getDisplayName();
                } else if (bonusItem.getType() == id.cyayo.forge.config.SalvageBonusConfig.BonusType.VANILLA) {
                    display = formatIdToName(bonusItem.getMaterial());
                } else {
                    display = formatIdToName(bonusItem.getId());
                }
                
                String amountRange = bonusItem.getMin() == bonusItem.getMax() ? 
                                    String.valueOf(bonusItem.getMin()) : 
                                    bonusItem.getMin() + "-" + bonusItem.getMax();
                display = plugin.getSalvageMenuConfig().getConfig().getString("format.list_amount", "&f{amount}x {display}")
                        .replace("{amount}", amountRange)
                        .replace("{display}", display);
            }
            
            preview.add(id.cyayo.forge.util.ColorUtil.color(plugin.getSalvageMenuConfig().getConfig().getString("format.list_prefix", " &8• ") + display));
        }
        return preview;
    }

    private String formatIdToName(String id) {
        String[] parts = id.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
