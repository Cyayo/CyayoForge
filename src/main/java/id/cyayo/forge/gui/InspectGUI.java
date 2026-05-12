package id.cyayo.forge.gui;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.model.ForgeQuality;
import id.cyayo.forge.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InspectGUI implements InventoryHolder {

    private final CyayoForge plugin;
    private final Inventory inventory;
    private final ItemStack targetItem;

    public InspectGUI(CyayoForge plugin, ItemStack item) {
        this.plugin = plugin;
        this.targetItem = item;
        
        ConfigurationSection config = plugin.getInspectMenuConfig().getConfig();
        String title = ColorUtil.color(config.getString("title", "&8Item Inspection"));
        int size = config.getInt("size", 27);
        
        this.inventory = Bukkit.createInventory(this, size, title);
        
        setupMenu();
    }

    private void setupMenu() {
        ConfigurationSection config = plugin.getInspectMenuConfig().getConfig();
        List<String> layout = config.getStringList("layout");
        ConfigurationSection itemSection = config.getConfigurationSection("items");
        
        if (itemSection == null) return;

        ItemStack filler = createItem(itemSection.getConfigurationSection("filler"));
        int targetSlot = config.getInt("target-slot", 13);
        
        List<Integer> materialSlots = new ArrayList<>();
        int smithSlot = -1;
        int qualitySlot = -1;

        for (int r = 0; r < layout.size(); r++) {
            String line = layout.get(r);
            for (int c = 0; c < Math.min(line.length(), 9); c++) {
                int slot = r * 9 + c;
                if (slot >= inventory.getSize()) break;
                
                char symbol = line.charAt(c);
                switch (symbol) {
                    case '#':
                        inventory.setItem(slot, filler);
                        break;
                    case 'M':
                        materialSlots.add(slot);
                        break;
                    case 'S':
                        smithSlot = slot;
                        break;
                    case 'Q':
                        qualitySlot = slot;
                        break;
                    case 'I':
                        targetSlot = slot;
                        break;
                }
            }
        }

        // Set target item
        inventory.setItem(targetSlot, targetItem);

        // Populate Materials
        populateMaterials(materialSlots);

        // Populate Smith
        if (smithSlot != -1) {
            inventory.setItem(smithSlot, createSmithItem(itemSection.getConfigurationSection("smith")));
        }

        // Populate Quality
        if (qualitySlot != -1) {
            inventory.setItem(qualitySlot, createQualityItem(itemSection.getConfigurationSection("quality")));
        }
    }

    private void populateMaterials(List<Integer> slots) {
        List<Map<String, Object>> materials = plugin.getForgeManager().getMaterialDataFromNBT(targetItem);
        if (materials == null || materials.isEmpty()) return;

        ConfigurationSection style = plugin.getInspectMenuConfig().getConfig().getConfigurationSection("material-style");
        String nameFormat = style != null ? style.getString("name", "&e{name}") : "&e{name}";
        List<String> loreFormat = style != null ? style.getStringList("lore") : new ArrayList<>();

        for (int i = 0; i < Math.min(materials.size(), slots.size()); i++) {
            Map<String, Object> data = materials.get(i);
            String id = (String) data.get("id");
            int amount = ((Number) data.get("amount")).intValue();
            String display = (String) data.get("display");

            ItemStack matItem = plugin.getMaterialConfig().createMaterial(id, amount);
            if (matItem == null) {
                matItem = new ItemStack(Material.BARRIER);
            }

            ItemMeta meta = matItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtil.color(nameFormat.replace("{name}", display).replace("{amount}", String.valueOf(amount))));
                List<String> lore = new ArrayList<>();
                for (String line : loreFormat) {
                    lore.add(ColorUtil.color(line.replace("{name}", display).replace("{amount}", String.valueOf(amount))));
                }
                meta.setLore(lore);
                matItem.setItemMeta(meta);
            }
            
            inventory.setItem(slots.get(i), matItem);
        }
    }

    private ItemStack createSmithItem(ConfigurationSection section) {
        if (section == null) return null;
        ItemStack item = createItem(section);
        String smithName = plugin.getForgeManager().getSmithFromNBT(targetItem);
        String date = plugin.getForgeManager().getDateFromNBT(targetItem);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(meta.getDisplayName().replace("{smith}", smithName)));
            if (meta.hasLore()) {
                List<String> lore = new ArrayList<>();
                for (String line : meta.getLore()) {
                    lore.add(ColorUtil.color(line.replace("{smith}", smithName).replace("{date}", date)));
                }
                meta.setLore(lore);
            }
            
            if (meta instanceof SkullMeta skull && smithName != null && !smithName.isEmpty() && !smithName.equals("Unknown")) {
                try {
                    skull.setOwningPlayer(Bukkit.getOfflinePlayer(smithName));
                } catch (Exception ignored) {}
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createQualityItem(ConfigurationSection section) {
        if (section == null) return null;
        ItemStack item = createItem(section);
        String qualityKey = plugin.getForgeManager().getQualityFromNBT(targetItem);
        ForgeQuality quality = ForgeQuality.fromString(qualityKey);
        
        String display = plugin.getConfigManager().getQualityStatString().getOrDefault(quality, quality.name());
        double bonus = plugin.getConfigManager().getQualityDamageBonus().getOrDefault(quality, 0.0);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(meta.getDisplayName().replace("{quality_display}", display)));
            if (meta.hasLore()) {
                List<String> lore = new ArrayList<>();
                for (String line : meta.getLore()) {
                    lore.add(ColorUtil.color(line.replace("{quality_display}", display).replace("{bonus}", String.valueOf(bonus))));
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(ConfigurationSection section) {
        if (section == null) return new ItemStack(Material.AIR);
        Material mat = Material.valueOf(section.getString("material", "STONE").toUpperCase());
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(section.getString("name", " ")));
            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) coloredLore.add(ColorUtil.color(line));
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
