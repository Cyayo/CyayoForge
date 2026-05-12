package id.cyayo.forge.gui;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.model.*;
import id.cyayo.forge.util.ColorUtil;
import id.cyayo.forge.util.ItemBuilder;
import id.cyayo.forge.util.RegionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ForgeGUI implements InventoryHolder {

    public static final int SLOT_MENU        = 4;
    public static final int[] SLOT_MATERIALS = {19, 20, 21, 22};
    public static final int SLOT_PREVIEW     = 25;
    public static final int SLOT_FORGE       = 40;

    // Penyimpanan internal material (bukan di inventory slot)
    private final ItemStack[] storedMaterials = new ItemStack[4];

    private final CyayoForge plugin;
    private final Player player;
    private final Inventory inventory;
    private ForgeType currentType;

    public ForgeGUI(CyayoForge plugin, Player player, ForgeType type) {
        this.plugin      = plugin;
        this.player      = player;
        this.currentType = type;

        FileConfiguration cfg = plugin.getMenuConfig().getConfig();
        String title = ColorUtil.color(cfg.getString("title", "&8⚒ CyayoForge"));
        this.inventory = Bukkit.createInventory(this, 45, title);
        populate();
    }

    // ─────────────────────────────────────────────
    // Populate GUI
    // ─────────────────────────────────────────────

    public void populate() {
        inventory.clear();
        FileConfiguration cfg = plugin.getMenuConfig().getConfig();

        // Background
        ItemStack bg = buildItem(cfg, "background", Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) inventory.setItem(i, bg);

        setMenuSelector(cfg);
        refreshMaterialSlots();
        updatePreview();
        updateForgeButton();
    }

    private void setMenuSelector(FileConfiguration cfg) {
        String path = "menu_selector." + currentType.name().toLowerCase();
        ItemStack item = buildItem(cfg, path, Material.IRON_SWORD, "&7Menu");
        inventory.setItem(SLOT_MENU, new ItemBuilder(item).glowing().build());
    }

    // ─────────────────────────────────────────────
    // Material slot refresh
    // ─────────────────────────────────────────────

    public void refreshMaterialSlots() {
        FileConfiguration cfg = plugin.getMenuConfig().getConfig();
        for (int i = 0; i < SLOT_MATERIALS.length; i++) {
            int slot = SLOT_MATERIALS[i];
            if (storedMaterials[i] != null && !storedMaterials[i].getType().isAir()) {
                inventory.setItem(slot, storedMaterials[i].clone());
            } else {
                inventory.setItem(slot, buildItem(cfg, "material_slot_empty",
                        Material.WHITE_STAINED_GLASS_PANE, "&7[ Slot Material ]"));
            }
        }
    }

    // ─────────────────────────────────────────────
    // Material management
    // ─────────────────────────────────────────────

    /** Tambahkan 1 item dari player ke slot material. Returns true jika berhasil. */
    public boolean addMaterial(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        // Cari slot dengan item yang sama dulu
        for (int i = 0; i < storedMaterials.length; i++) {
            if (storedMaterials[i] != null && !storedMaterials[i].getType().isAir()
                    && storedMaterials[i].isSimilar(item)) {
                storedMaterials[i].setAmount(storedMaterials[i].getAmount() + 1);
                return true;
            }
        }
        // Cari slot kosong
        for (int i = 0; i < storedMaterials.length; i++) {
            if (storedMaterials[i] == null || storedMaterials[i].getType().isAir()) {
                ItemStack toStore = item.clone();
                toStore.setAmount(1);
                storedMaterials[i] = toStore;
                return true;
            }
        }
        return false; // Semua 4 slot terisi dengan jenis berbeda
    }

    /** Ambil 1 item dari slot (index 0-3). Returns item atau null. */
    public ItemStack removeMaterialAt(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= storedMaterials.length) return null;
        ItemStack mat = storedMaterials[slotIndex];
        if (mat == null || mat.getType().isAir()) return null;

        ItemStack taken = mat.clone();
        taken.setAmount(1);
        int remaining = mat.getAmount() - 1;
        storedMaterials[slotIndex] = remaining <= 0 ? null : mat;
        if (remaining > 0) storedMaterials[slotIndex].setAmount(remaining);
        return taken;
    }

    /** Kembalikan semua material ke player. */
    public void returnAllMaterials() {
        for (int i = 0; i < storedMaterials.length; i++) {
            if (storedMaterials[i] != null && !storedMaterials[i].getType().isAir()) {
                player.getInventory().addItem(storedMaterials[i]).values()
                        .forEach(drop -> player.getWorld().dropItem(player.getLocation(), drop));
                storedMaterials[i] = null;
            }
        }
    }

    /** Ambil semua material sebagai list untuk proses forging. */
    public List<ItemStack> getAllMaterials() {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack mat : storedMaterials)
            if (mat != null && !mat.getType().isAir()) list.add(mat.clone());
        return list;
    }

    /** Hapus semua material (dipanggil saat forging dimulai). */
    public void clearMaterials() {
        for (int i = 0; i < storedMaterials.length; i++) storedMaterials[i] = null;
    }

    public int getTotalMaterialCount() {
        int total = 0;
        for (ItemStack mat : storedMaterials)
            if (mat != null && !mat.getType().isAir()) total += mat.getAmount();
        return total;
    }

    public boolean hasAnyMaterial() {
        for (ItemStack mat : storedMaterials)
            if (mat != null && !mat.getType().isAir()) return true;
        return false;
    }

    /** Konversi slot GUI ke index storedMaterials. -1 jika bukan slot material. */
    public int getMaterialSlotIndex(int guiSlot) {
        for (int i = 0; i < SLOT_MATERIALS.length; i++)
            if (SLOT_MATERIALS[i] == guiSlot) return i;
        return -1;
    }

    // ─────────────────────────────────────────────
    // Preview & Forge button
    // ─────────────────────────────────────────────

    public void updatePreview() {
        FileConfiguration cfg = plugin.getMenuConfig().getConfig();
        int total = getTotalMaterialCount();
        ForgeRecipe recipe = plugin.getRecipeConfig().getRecipe(currentType, total, player);

        if (recipe == null || total == 0) {
            String path = currentType == ForgeType.WEAPON ? "preview.no_recipe_weapon" : "preview.no_recipe_armor";
            List<String> loreTemplate = cfg.getStringList(path + ".lore");
            List<String> finalLore = new ArrayList<>();

            for (String line : loreTemplate) {
                if (line.contains("{available_recipes}")) {
                    List<ForgeRecipe> allRecipes = plugin.getRecipeConfig().getRecipes(currentType);
                    String lineFormat = cfg.getString("preview.available_recipe_line", "<#6e6e6e>- <#a3a3a3>{type} <#6e6e6e>[<#e8e8e8>{count}<#6e6e6e>]");
                    
                    for (ForgeRecipe r : allRecipes) {
                        // Cek permission kategori & region
                        String catPerm = r.getPermission();
                        boolean hasCatPerm = catPerm == null || catPerm.isEmpty() || player.hasPermission(catPerm);
                        boolean inCatRegion = RegionUtil.isInAnyRegion(player, r.getRegions());
                        
                        if (!hasCatPerm && !inCatRegion) {
                            continue;
                        }
                        
                        // Cek apakah pemain punya izin/region untuk setidaknya satu output di resep ini
                        boolean hasAnyAccess = false;
                        for (OutputTemplate out : r.getOutputs()) {
                            String perm = out.getPermission();
                            boolean hasItemPerm = perm == null || perm.isEmpty() || player.hasPermission(perm);
                            boolean inItemRegion = RegionUtil.isInAnyRegion(player, out.getRegions());
                            
                            if (hasItemPerm || inItemRegion) {
                                hasAnyAccess = true;
                                break;
                            }
                        }
                        
                        if (hasAnyAccess) {
                            String typeDisplay = plugin.getMenuConfig().getConfig().getString("output_type_names." + r.getOutputType().toUpperCase(), formatIdToName(r.getOutputType()));
                            finalLore.add(ColorUtil.color(lineFormat
                                    .replace("{type}", typeDisplay)
                                    .replace("{count}", String.valueOf(r.getMinMaterialCount()))));
                        }
                    }
                } else {
                    finalLore.add(ColorUtil.color(line));
                }
            }

            inventory.setItem(SLOT_PREVIEW, new ItemBuilder(getMatSafe(cfg.getString(path + ".material", "BARRIER")))
                    .name(ColorUtil.color(cfg.getString(path + ".name", "&cTidak Ada Yang Bisa Dibuat")))
                    .lore(finalLore)
                    .build());
            return;
        }

        String outputType = recipe.getOutputType();
        ConfigurationSection perTypeSec = cfg.getConfigurationSection("preview.per_type." + outputType);
        ConfigurationSection hasRecipeSec = cfg.getConfigurationSection("preview.has_recipe");

        // Info dasar item preview
        Material mat = getMatSafe(perTypeSec != null ? perTypeSec.getString("material") : (hasRecipeSec != null ? hasRecipeSec.getString("material") : "PAPER"));
        String name = perTypeSec != null && perTypeSec.contains("name") ? perTypeSec.getString("name") : (hasRecipeSec != null ? hasRecipeSec.getString("name", "&e" + outputType) : "&e" + outputType);
        
        List<String> loreTemplate = hasRecipeSec != null ? hasRecipeSec.getStringList("lore") : new ArrayList<>();
        List<String> finalLore = new ArrayList<>();

        List<ItemStack> mats = getAllMaterials();
        ForgeTier tier = plugin.getForgeManager().calculateMajorityTier(mats);
        double multiplier = plugin.getForgeManager().calculateWeightedMultiplier(mats, currentType);

        for (String line : loreTemplate) {
            if (line.contains(plugin.getConfigManager().getPlaceholderKey("output_list"))) {
                for (OutputTemplate out : recipe.getOutputs()) {
                    String perm = out.getPermission();
                    boolean hasPerm = perm == null || perm.isEmpty() || player.hasPermission(perm);
                    boolean inRegion = RegionUtil.isInAnyRegion(player, out.getRegions());

                    if (!hasPerm && !inRegion) continue;
                    String lineTemplate = out.getBase() == 1
                            ? cfg.getString("preview.output_line_guaranteed", "&a✦ &f{id} &8(&a1/1&8)")
                            : cfg.getString("preview.output_line_rare", "&e◆ &f{id} &8(&e1/{base}&8)");
                    finalLore.add(ColorUtil.color(lineTemplate
                            .replace("{id}", formatIdToName(out.getId()))
                            .replace("{base}", String.valueOf(out.getBase()))));
                }
            } else if (line.contains(plugin.getConfigManager().getPlaceholderKey("category_lore"))) {
                if (perTypeSec != null && perTypeSec.contains("lore")) {
                    for (String l : perTypeSec.getStringList("lore")) {
                        finalLore.add(ColorUtil.color(l));
                    }
                }
            } else {
                finalLore.add(replacePlaceholders(line, tier, multiplier, outputType, mats, total));
            }
        }

        ItemBuilder builder = new ItemBuilder(mat).name(ColorUtil.color(name.replace("{output_type}", outputType))).lore(finalLore);
        if (perTypeSec != null && perTypeSec.contains("custom-model-data"))
            builder.customModelData(perTypeSec.getInt("custom-model-data"));
        else if (hasRecipeSec != null && hasRecipeSec.contains("custom-model-data"))
            builder.customModelData(hasRecipeSec.getInt("custom-model-data"));
            
        inventory.setItem(SLOT_PREVIEW, builder.build());
    }

    public void updateForgeButton() {
        FileConfiguration cfg = plugin.getMenuConfig().getConfig();
        int total = getTotalMaterialCount();
        ForgeRecipe recipe = plugin.getRecipeConfig().getRecipe(currentType, total, player);
        boolean canForge   = recipe != null && total > 0;

        String path = canForge ? "forge_button.ready" : "forge_button.not_ready";
        Material mat = getMatSafe(cfg.getString(path + ".material", "ANVIL"));
        String name  = cfg.getString(path + ".name", canForge ? "&aTempa!" : "&cBelum Bisa Menempa");
        List<String> loreTemplate = cfg.getStringList(path + ".lore");
        List<String> finalLore = new ArrayList<>();

        if (canForge) {
            List<ItemStack> mats  = getAllMaterials();
            double multiplier     = plugin.getForgeManager().calculateWeightedMultiplier(mats, currentType);
            ForgeTier tier        = plugin.getForgeManager().calculateMajorityTier(mats);
            String outputType     = recipe.getOutputType();
            
            for (String line : loreTemplate) {
                if (line.contains(plugin.getConfigManager().getPlaceholderKey("material_composition"))) {
                    if (!mats.isEmpty()) {
                        List<String> headers = cfg.getStringList("forge_button.composition_format.header");
                        if (headers.isEmpty()) headers = java.util.List.of("&8&m──────────────────", "&7Komposisi Material:");
                        for (String h : headers) finalLore.add(ColorUtil.color(h));
                        
                        String lineFormat = cfg.getString("forge_button.composition_format.line", "&8- &f{material} &7(&e{percentage}%&7)");
                        for (ItemStack m : mats) {
                            double percentage = (m.getAmount() * 100.0) / total;
                            String matName = m.hasItemMeta() && m.getItemMeta().hasDisplayName() ? 
                                             m.getItemMeta().getDisplayName() : 
                                             formatIdToName(m.getType().name());
                            finalLore.add(ColorUtil.color(lineFormat
                                    .replace("{material}", matName)
                                    .replace("{percentage}", String.format(java.util.Locale.US, "%.1f", percentage).replace(".0", ""))));
                        }
                    }
                } else {
                    finalLore.add(replacePlaceholders(line, tier, multiplier, outputType, mats, total));
                }
            }
        } else {
            for (String line : loreTemplate) {
                finalLore.add(ColorUtil.color(line));
            }
        }

        ItemBuilder builder = new ItemBuilder(mat).name(ColorUtil.color(name)).lore(finalLore);
        if (canForge) builder.glowing();
        if (cfg.contains(path + ".custom-model-data"))
            builder.customModelData(cfg.getInt(path + ".custom-model-data"));
        inventory.setItem(SLOT_FORGE, builder.build());
    }

    // ─────────────────────────────────────────────
    // Menu switch
    // ─────────────────────────────────────────────

    public void switchType(ForgeType type) {
        returnAllMaterials();
        this.currentType = type;
        populate();
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────

    private String replacePlaceholders(String line, ForgeTier tier, double multiplier, String outputType, List<ItemStack> mats, int total) {
        String tierKey = plugin.getConfigManager().getPlaceholderKey("tier");
        String multKey = plugin.getConfigManager().getPlaceholderKey("multiplier");
        String typeKey = plugin.getConfigManager().getPlaceholderKey("output_type");

        String result = line;
        if (tier != null) {
            result = result.replace(tierKey, plugin.getConfigManager().getTierDisplayName(tier));
        }
        
        String typeDisplay = outputType;
        if (outputType != null) {
            typeDisplay = plugin.getMenuConfig().getConfig().getString("output_type_names." + outputType.toUpperCase(), formatIdToName(outputType));
        }
        
        result = result.replace(multKey, String.format(java.util.Locale.US, "%.2f", multiplier))
                       .replace(typeKey, typeDisplay);
        
        return ColorUtil.color(result);
    }

    private ItemStack buildItem(FileConfiguration cfg, String path,
                                Material def, String defName) {
        ConfigurationSection sec = cfg.getConfigurationSection(path);
        Material mat  = sec != null ? getMatSafe(sec.getString("material")) : null;
        if (mat == null) mat = def;
        String name   = sec != null ? sec.getString("name", defName) : defName;
        List<String> lore = sec != null ? sec.getStringList("lore") : new ArrayList<>();
        ItemBuilder b = new ItemBuilder(mat).name(name).lore(lore);
        if (sec != null && sec.contains("custom-model-data"))
            b.customModelData(sec.getInt("custom-model-data"));
        return b.build();
    }

    private Material getMatSafe(String name) {
        if (name == null) return Material.STONE;
        try { return Material.valueOf(name.toUpperCase()); }
        catch (Exception e) { return Material.STONE; }
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

    public ForgeType getCurrentType()          { return currentType; }
    public Player getPlayer()                  { return player; }
    public ItemStack[] getStoredMaterials()    { return storedMaterials; }

    @Override
    public Inventory getInventory() { return inventory; }
}
