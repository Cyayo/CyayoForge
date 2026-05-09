package id.cyayo.forge.command;

import id.cyayo.forge.CyayoForge;
import id.cyayo.forge.gui.ForgeGUI;
import id.cyayo.forge.gui.SalvageMenuGUI;
import id.cyayo.forge.model.ForgeQuality;
import id.cyayo.forge.model.ForgeType;
import id.cyayo.forge.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ForgeCommand implements CommandExecutor, TabCompleter {

    private final CyayoForge plugin;

    public ForgeCommand(CyayoForge plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(plugin.getConfigManager().getMessage("only_players")));
                return true;
            }
            openForGUI(sender, player, ForgeType.WEAPON);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "open" -> {
                if (args.length < 2) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(plugin.getConfigManager().getMessage("usage_open")));
                        return true;
                    }
                    openForGUI(sender, player, ForgeType.WEAPON);
                } else {
                    if (!sender.hasPermission("cyayoforge.open")) {
                        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("no_permission")));
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(ColorUtil.color(
                                plugin.getConfigManager().getMessage("player_not_found", "{player}", args[1])));
                        return true;
                    }
                    ForgeType type = args.length > 2 ? ForgeType.fromString(args[2]) : ForgeType.WEAPON;
                    openForGUI(sender, target, type);
                    if (sender != target)
                        sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(
                                plugin.getConfigManager().getMessage("admin_open_success", "{player}", target.getName())));
                }
            }

            case "reload" -> {
                if (!sender.hasPermission("cyayoforge.reload")) {
                    sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("no_permission")));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("reload_success")));
            }



            case "reset" -> {
                if (!sender.hasPermission("cyayoforge.reset")) {
                    sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("no_permission")));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(plugin.getConfigManager().getMessage("usage_reset")));
                    return true;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    plugin.getPlayerDataManager().resetAll();
                    sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("reset_all_success")));
                } else {
                    Player target = Bukkit.getPlayer(args[1]);
                    UUID uuid = target != null ? target.getUniqueId() : null;
                    if (uuid == null) {
                        sender.sendMessage(ColorUtil.color(
                                plugin.getConfigManager().getMessage("player_not_found", "{player}", args[1])));
                        return true;
                    }
                    plugin.getPlayerDataManager().resetPlayer(uuid);
                    sender.sendMessage(ColorUtil.color(
                            plugin.getConfigManager().getMessage("reset_success", "{player}", args[1])));
                }
            }

            case "version" -> {
                sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(
                        plugin.getConfigManager().getMessage("version_info",
                                "{version}", plugin.getDescription().getVersion(),
                                "{authors}", String.join(", ", plugin.getDescription().getAuthors()))));
            }

            case "give" -> {
                if (!sender.hasPermission("cyayoforge.give")) {
                    sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("no_permission")));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(plugin.getConfigManager().getMessage("usage_give")));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("player_not_found", "{player}", args[1])));
                    return true;
                }

                String typeStr = args[2];
                id.cyayo.forge.model.ForgeRecipe recipe = plugin.getRecipeConfig().getRecipeByOutputType(typeStr);

                if (recipe == null) {
                    sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(
                            plugin.getConfigManager().getMessage("recipe_not_found", "{type}", typeStr)));
                    return true;
                }

                id.cyayo.forge.model.ForgeTier tier;
                if (args.length > 3) {
                    tier = id.cyayo.forge.model.ForgeTier.fromString(args[3]);
                } else {
                    tier = id.cyayo.forge.model.ForgeTier.values()[(int) (Math.random() * id.cyayo.forge.model.ForgeTier.values().length)];
                }

                id.cyayo.forge.model.ForgeQuality quality;
                if (args.length > 4) {
                    try {
                        quality = id.cyayo.forge.model.ForgeQuality.valueOf(args[4].toUpperCase());
                    } catch (Exception e) {
                        sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(
                                plugin.getConfigManager().getMessage("invalid_quality", "{quality}", args[4])));
                        return true;
                    }
                } else {
                    quality = id.cyayo.forge.model.ForgeQuality.values()[(int) (Math.random() * id.cyayo.forge.model.ForgeQuality.values().length)];
                }

                // Roll output template
                id.cyayo.forge.model.OutputTemplate selected = null;
                for (id.cyayo.forge.model.OutputTemplate out : recipe.getOutputs()) {
                    if (out.getBase() == 1) { if (selected == null) selected = out; continue; }
                    if (out.roll()) { selected = out; break; }
                }
                if (selected == null) selected = recipe.getOutputs().get(0);

                // Create dummy session
                id.cyayo.forge.model.ForgeSession session = new id.cyayo.forge.model.ForgeSession(
                        target.getUniqueId(),
                        new ArrayList<>(), // empty materials
                        recipe,
                        tier,
                        selected,
                        1.0, // default multiplier
                        new ArrayList<>(), // empty abilities
                        0, 0
                );
                session.setQuality(quality);
                
                boolean injectSmith = plugin.getConfigManager().getRawConfig().getBoolean("command.give.inject_penempa", false);
                session.setInjectSmith(injectSmith);

                ItemStack item = plugin.getForgeManager().buildOutput(session);
                if (item == null) {
                    sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(plugin.getConfigManager().getMessage("give_fail")));
                    return true;
                }

                item = plugin.getForgeManager().applyQualityLore(item, session);
                if (injectSmith) {
                    item = plugin.getForgeManager().applySmithLore(item, sender.getName());
                }

                target.getInventory().addItem(item).values().forEach(d -> target.getWorld().dropItem(target.getLocation(), d));

                String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                        ? item.getItemMeta().getDisplayName() : item.getType().name();

                sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(
                        plugin.getConfigManager().getMessage("command_give_success",
                                "{item}", itemName,
                                "{player}", target.getName(),
                                "{tier}", tier.name(),
                                "{quality}", quality.name())));
            }

            case "salvage" -> {
                if (args.length < 2) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(plugin.getConfigManager().getMessage("usage_salvage")));
                        return true;
                    }
                    if (!player.hasPermission("cyayoforge.salvage")) {
                        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("no_permission")));
                        return true;
                    }
                    player.openInventory(new SalvageMenuGUI(plugin, player).getInventory());
                    plugin.getConfigManager().playSound(player, "gui.sound_open");
                } else {
                    if (!sender.hasPermission("cyayoforge.admin.salvage")) {
                        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("no_permission")));
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(ColorUtil.color(
                                plugin.getConfigManager().getMessage("player_not_found", "{player}", args[1])));
                        return true;
                    }
                    target.openInventory(new SalvageMenuGUI(plugin, target).getInventory());
                    plugin.getConfigManager().playSound(target, "gui.sound_open");
                    if (sender != target)
                        sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(
                                plugin.getConfigManager().getMessage("admin_open_success", "{player}", target.getName())));
                }
            }

            default -> sender.sendMessage(id.cyayo.forge.util.ColorUtil.color(plugin.getConfigManager().getMessage("unknown_command")));
        }
        return true;
    }

    private void openForGUI(CommandSender opener, Player target, ForgeType type) {
        // Jika player membuka sendiri, cek permission .use
        if (opener == target && !target.hasPermission("cyayoforge.use")) {
            opener.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("no_permission")));
            return;
        }

        // Cek penalti cooldown (jika player membuka sendiri)
        if (opener == target) {
            long timeLeft = plugin.getForgeManager().getPenaltyTimeLeft(target.getUniqueId());
            if (timeLeft > 0) {
                opener.sendMessage(ColorUtil.color(
                        plugin.getConfigManager().getMessage("penalty_active", "{time}", timeLeft + "s")));
                return;
            }
        }

        if (plugin.getForgeManager().hasActiveSession(target.getUniqueId())) {
            opener.sendMessage(ColorUtil.color(
                    plugin.getConfigManager().getMessage("already_forging")));
            return;
        }
        ForgeGUI gui = new ForgeGUI(plugin, target, type);
        target.openInventory(gui.getInventory());
        plugin.getConfigManager().playSound(target, "gui.sound_open");
    }



    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1)
            return filter(List.of("open", "salvage", "reload", "reset", "give", "version"), args[0]);

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "open", "salvage", "reset", "give" ->
                        Bukkit.getOnlinePlayers().stream().map(Player::getName)
                                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                default -> List.of();
            };
        }

        if (args.length == 3) {
            return switch (args[0].toLowerCase()) {
                case "open" -> filter(Arrays.stream(ForgeType.values()).map(Enum::name).toList(), args[2]);
                case "reset" -> filter(List.of("all"), args[2]);
                case "give" -> {
                    List<String> types = new ArrayList<>();
                    for (ForgeType ft : ForgeType.values()) {
                        types.addAll(plugin.getRecipeConfig().getRecipes(ft).stream().map(id.cyayo.forge.model.ForgeRecipe::getOutputType).toList());
                    }
                    yield filter(types, args[2]);
                }
                default -> List.of();
            };
        }

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("give")) {
                return filter(Arrays.stream(id.cyayo.forge.model.ForgeTier.values()).map(Enum::name).toList(), args[3]);
            }
        }

        if (args.length == 5) {
            if (args[0].equalsIgnoreCase("give")) {
                return filter(Arrays.stream(ForgeQuality.values()).map(Enum::name).toList(), args[4]);
            }
        }

        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
