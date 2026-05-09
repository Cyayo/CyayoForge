package id.cyayo.forge.listener;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.gui.ForgeGUI;
import id.cyayo.forge.model.ForgeType;
import id.cyayo.forge.model.MaterialData;
import id.cyayo.forge.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ForgeGUIListener implements Listener {

    private final CyayoForge plugin;
    private static final Set<Integer> MATERIAL_SLOTS = new HashSet<>(
            Arrays.asList(ForgeGUI.SLOT_MATERIALS[0], ForgeGUI.SLOT_MATERIALS[1],
                    ForgeGUI.SLOT_MATERIALS[2], ForgeGUI.SLOT_MATERIALS[3]));

    public ForgeGUIListener(CyayoForge plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof ForgeGUI gui)) return;

        // Selalu cancel event — kita handle semuanya manual
        event.setCancelled(true);

        int rawSlot  = event.getRawSlot();  // Slot absolut (0-44 GUI, 45+ player inv)
        int guiSize  = gui.getInventory().getSize(); // = 45

        // ── Klik di slot material GUI (19,20,21,22): ambil 1 item kembali ──
        if (MATERIAL_SLOTS.contains(rawSlot)) {
            int idx = gui.getMaterialSlotIndex(rawSlot);
            if (idx >= 0) {
                ItemStack taken = gui.removeMaterialAt(idx);
                if (taken != null) {
                    player.getInventory().addItem(taken).values()
                            .forEach(drop -> player.getWorld().dropItem(player.getLocation(), drop));
                    refreshGUI(gui);
                    plugin.getConfigManager().playSound(player, "gui.sound_remove_material");
                }
            }
            return;
        }

        // ── Klik di inventory player (rawSlot >= guiSize): masukkan item ke GUI ──
        if (rawSlot >= guiSize) {
            // event.getSlot() = slot di inventory player (0-35 untuk main inventory)
            // Kita perlu ambil item dari slot yang diklik di inventory player
            ItemStack clicked = player.getInventory().getItem(event.getSlot());
            if (clicked == null || clicked.getType().isAir()) return;

            // Validasi material
            String matId = plugin.getForgeManager().identifyMaterial(clicked);
            if (matId == null) {
                player.sendMessage(ColorUtil.color(
                        plugin.getConfigManager().getMessage("not_valid_material")));
                plugin.getConfigManager().playSound(player, "gui.sound_fail");
                return;
            }

            // Validasi tipe forging
            MaterialData mat = plugin.getMaterialConfig().getMaterial(matId);
            if (mat == null) mat = plugin.getForgeManager().getMaterialData(clicked);
            if (mat != null) {
                boolean allowed = gui.getCurrentType() == ForgeType.WEAPON
                        ? mat.getWeaponDamageMultiplier() > 0
                        : mat.getArmorHealthMultiplier() > 0;
                if (!allowed) {
                    String jenis = gui.getCurrentType() == ForgeType.WEAPON ? "senjata" : "armor";
                    player.sendMessage(ColorUtil.color(
                            plugin.getConfigManager().getMessage("material_wrong_type",
                                    "{type}", jenis)));
                    plugin.getConfigManager().playSound(player, "gui.sound_fail");
                    return;
                }
            }

            // Coba masukkan ke GUI
            boolean added = gui.addMaterial(clicked);
            if (added) {
                // Kurangi 1 dari slot player
                ItemStack newStack = clicked.clone();
                newStack.setAmount(clicked.getAmount() - 1);
                player.getInventory().setItem(event.getSlot(),
                        newStack.getAmount() <= 0 ? null : newStack);
                refreshGUI(gui);
                plugin.getConfigManager().playSound(player, "gui.sound_add_material");
            } else {
                player.sendMessage(ColorUtil.color(
                        plugin.getConfigManager().getMessage("gui_full")));
                plugin.getConfigManager().playSound(player, "gui.sound_fail");
            }
            return;
        }

        // ── Tombol menu selector (slot 4) ──
        if (rawSlot == ForgeGUI.SLOT_MENU) {
            ForgeType[] types = ForgeType.values();
            ForgeType next = types[(gui.getCurrentType().ordinal() + 1) % types.length];
            gui.switchType(next);
            plugin.getConfigManager().playSound(player, "gui.sound_switch_type");
            return;
        }

        // ── Tombol forge (slot 40) ──
        if (rawSlot == ForgeGUI.SLOT_FORGE) {
            handleForge(player, gui);
        }
    }

    private void handleForge(Player player, ForgeGUI gui) {
        if (plugin.getForgeManager().hasActiveSession(player.getUniqueId())) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("already_forging")));
            return;
        }
        if (!gui.hasAnyMaterial()) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("forging_failed")));
            plugin.getConfigManager().playSound(player, "gui.sound_fail");
            return;
        }
        int total = gui.getTotalMaterialCount();
        if (plugin.getRecipeConfig().getRecipe(gui.getCurrentType(), total) == null) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("forging_failed")));
            plugin.getConfigManager().playSound(player, "gui.sound_fail");
            return;
        }
        plugin.getForgeManager().initiateForge(player, gui);
    }

    private void refreshGUI(ForgeGUI gui) {
        gui.refreshMaterialSlots();
        gui.updatePreview();
        gui.updateForgeButton();
    }

    // Block drag di GUI
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ForgeGUI)
            event.setCancelled(true);
    }

    // Kembalikan material saat GUI ditutup (hanya jika tidak sedang forging)
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof ForgeGUI gui)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!plugin.getForgeManager().hasActiveSession(player.getUniqueId())) {
            gui.returnAllMaterials();
            plugin.getConfigManager().playSound(player, "gui.sound_close");
        }
    }
}
