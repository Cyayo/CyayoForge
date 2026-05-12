package id.cyayo.forge.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        meta.setDisplayName(ColorUtil.color(name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        meta.setLore(Arrays.stream(lines)
                .map(ColorUtil::color)
                .collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        meta.setLore(lines.stream()
                .map(ColorUtil::color)
                .collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder glowing() {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder customModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
