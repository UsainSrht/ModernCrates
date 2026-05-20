package me.usainsrht.moderncrates.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateLocation;
import me.usainsrht.moderncrates.gui.PlayerMenuGui;
import me.usainsrht.moderncrates.gui.PreviewGui;
import me.usainsrht.moderncrates.gui.editor.MainMenuGui;
import me.usainsrht.moderncrates.util.SoundUtil;
import me.usainsrht.moderncrates.util.TextUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Registers all ModernCrates commands using Paper's Brigadier command API.
 */
@SuppressWarnings("UnstableApiUsage")
public class ModernCratesCommand {

    private final ModernCratesPlugin plugin;

    public ModernCratesCommand(ModernCratesPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        LifecycleEventManager<Plugin> manager = plugin.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(
                    buildCommand().build(),
                    "ModernCrates main command",
                    plugin.getPluginConfig().getCommandConfig().getAliases()
            );
        });
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        return Commands.literal("moderncrates")
                // /moderncrates - open player menu
                .executes(this::handleMenu)

                // /moderncrates reload
                .then(Commands.literal("reload")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                        .executes(this::handleReload))

                // /moderncrates editor
                .then(Commands.literal("editor")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                        .executes(this::handleEditor))

                // /moderncrates preview <crate>
                .then(Commands.literal("preview")
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests(suggestCrates())
                                .executes(this::handlePreview)))

                // /moderncrates set <crate> - add player's target block as crate location
                .then(Commands.literal("set")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests(suggestCrates())
                                .executes(this::handleSet)))

                // /moderncrates open <crate> [player]
                .then(Commands.literal("open")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests(suggestCrates())
                                .executes(this::handleOpen)
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .executes(this::handleOpenForPlayer))))

                // /moderncrates give key <crate> [player] [amount]
                .then(Commands.literal("give")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))

                        // give key <crate> [player] [amount]
                        .then(Commands.literal("key")
                                .then(Commands.argument("crate", StringArgumentType.word())
                                        .suggests(suggestCrates())
                                        // key <crate> - default to sender
                                        .executes(this::handleGiveKeySelf)
                                        .then(Commands.argument("player", ArgumentTypes.player())
                                                .executes(this::handleGiveKey)
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(this::handleGiveKeyAmount)))))

                        // give virtualkey <crate> [player] [amount]
                        .then(Commands.literal("virtualkey")
                                .then(Commands.argument("crate", StringArgumentType.word())
                                        .suggests(suggestCrates())
                                        .executes(this::handleGiveVirtualKeySelf)
                                        .then(Commands.argument("player", ArgumentTypes.player())
                                                .executes(this::handleGiveVirtualKey)
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(this::handleGiveVirtualKeyAmount)))))

                        // give crate <crate> [player] [amount]
                        .then(Commands.literal("crate")
                                .then(Commands.argument("crate", StringArgumentType.word())
                                        .suggests(suggestCrates())
                                        .executes(this::handleGiveCrateSelf)
                                        .then(Commands.argument("player", ArgumentTypes.player())
                                                .executes(this::handleGiveCrate)
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(this::handleGiveCrateAmount))))));
    }

    // --- Suggestion Providers ---

    private SuggestionProvider<CommandSourceStack> suggestCrates() {
        return (context, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            plugin.getCrateRegistry().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(remaining))
                    .forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    // --- Command Handlers ---

    private int handleMenu(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(TextUtil.parse("<red>This command can only be used by players."));
            return Command.SINGLE_SUCCESS;
        }

        PlayerMenuGui gui = new PlayerMenuGui(player, plugin);
        gui.open();
        return Command.SINGLE_SUCCESS;
    }

    private int handleReload(CommandContext<CommandSourceStack> ctx) {
        plugin.reloadPlugin();
        String msg = plugin.getPluginConfig().getPrefix() + plugin.getPluginConfig().getMessage("reload");
        ctx.getSource().getSender().sendMessage(TextUtil.parse(msg));
        SoundUtil.play(ctx.getSource().getSender() instanceof Player p ? p : null,
                plugin.getPluginConfig().getSound("reload"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleEditor(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(TextUtil.parse("<red>This command can only be used by players."));
            return Command.SINGLE_SUCCESS;
        }

        MainMenuGui gui = new MainMenuGui(player, plugin);
        gui.open();
        return Command.SINGLE_SUCCESS;
    }

    private int handlePreview(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(TextUtil.parse("<red>This command can only be used by players."));
            return Command.SINGLE_SUCCESS;
        }

        String crateId = StringArgumentType.getString(ctx, "crate");
        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) {
            sendNoCrate(ctx, crateId);
            return Command.SINGLE_SUCCESS;
        }

        PreviewGui gui = new PreviewGui(player, crate);
        gui.open();
        return Command.SINGLE_SUCCESS;
    }

    /**
     * /moderncrates set <crate> — Adds the sender's target block as a crate location.
     */
    private int handleSet(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(TextUtil.parse("<red>This command can only be used by players."));
            return Command.SINGLE_SUCCESS;
        }

        String crateId = StringArgumentType.getString(ctx, "crate");
        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) {
            sendNoCrate(ctx, crateId);
            return Command.SINGLE_SUCCESS;
        }

        Block target = player.getTargetBlockExact(10);
        if (target == null) {
            player.sendMessage(TextUtil.parse("<red>No block in sight (max 10 blocks)."));
            return Command.SINGLE_SUCCESS;
        }

        CrateLocation loc = new CrateLocation(target.getWorld().getName(), target.getX(), target.getY(), target.getZ());
        crate.addCrateLocation(loc);

        try {
            plugin.getCrateConfigParser().save(crate, new File(plugin.getDataFolder(), "crates"));
        } catch (Exception e) {
            player.sendMessage(TextUtil.parse("<red>Failed to save: " + e.getMessage()));
        }
        plugin.getHologramManager().removeHologram(crateId);
        plugin.getHologramManager().createHologram(crate);

        player.sendMessage(TextUtil.parse("<green>Location added to crate <white>" + crate.getName()
                + "<green>! (" + crate.getCrateLocations().size() + " total)"));
        return Command.SINGLE_SUCCESS;
    }

    private int handleOpen(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(TextUtil.parse("<red>This command can only be used by players."));
            return Command.SINGLE_SUCCESS;
        }

        String crateId = StringArgumentType.getString(ctx, "crate");
        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) {
            sendNoCrate(ctx, crateId);
            return Command.SINGLE_SUCCESS;
        }

        plugin.openCrate(player, crate);
        return Command.SINGLE_SUCCESS;
    }

    private int handleOpenForPlayer(CommandContext<CommandSourceStack> ctx) {
        String crateId = StringArgumentType.getString(ctx, "crate");

        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) {
            sendNoCrate(ctx, crateId);
            return Command.SINGLE_SUCCESS;
        }

        Player target = resolvePlayer(ctx, "player");
        if (target == null) return Command.SINGLE_SUCCESS;

        plugin.openCrate(target, crate);
        return Command.SINGLE_SUCCESS;
    }

    // --- Give key (self) ---
    private int handleGiveKeySelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(TextUtil.parse("<red>You must be a player or specify a target."));
            return Command.SINGLE_SUCCESS;
        }
        return giveKey(ctx, player, 1);
    }

    private int handleGiveKey(CommandContext<CommandSourceStack> ctx) {
        return giveKey(ctx, resolvePlayer(ctx, "player"), 1);
    }

    private int handleGiveKeyAmount(CommandContext<CommandSourceStack> ctx) {
        return giveKey(ctx, resolvePlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"));
    }

    private int giveKey(CommandContext<CommandSourceStack> ctx, Player target, int amount) {
        if (target == null) return Command.SINGLE_SUCCESS;
        String crateId = StringArgumentType.getString(ctx, "crate");
        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) { sendNoCrate(ctx, crateId); return Command.SINGLE_SUCCESS; }
        ItemStack key = plugin.getKeyManager().createKey(crate, amount);
        if (key != null) {
            Map<Integer, ItemStack> leftover = target.getInventory().addItem(key);
            if (!leftover.isEmpty()) {
                int leftoverAmount = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
                if (plugin.getPluginConfig().isGiveFullInventoryDrop()) {
                    // Give as virtual keys instead of dropping the key item
                    plugin.getVirtualKeyManager().addKeys(target, crateId, leftoverAmount);
                    String msg = plugin.getPluginConfig().getPrefix()
                            + plugin.getPluginConfig().getMessage("inventory_full_virtual_key")
                                    .replace("<crate>", crate.getName())
                                    .replace("<player>", target.getName());
                    target.sendMessage(TextUtil.parse(msg));
                } else {
                    String msg = plugin.getPluginConfig().getPrefix()
                            + plugin.getPluginConfig().getMessage("inventory_full_no_space")
                                    .replace("<crate>", crate.getName())
                                    .replace("<player>", target.getName());
                    target.sendMessage(TextUtil.parse(msg));
                }
                return Command.SINGLE_SUCCESS;
            }
        }
        sendKeyGiven(ctx, crate, target);
        return Command.SINGLE_SUCCESS;
    }

    // --- Give virtualkey (self) ---
    private int handleGiveVirtualKeySelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(TextUtil.parse("<red>You must be a player or specify a target."));
            return Command.SINGLE_SUCCESS;
        }
        return giveVirtualKey(ctx, player, 1);
    }

    private int handleGiveVirtualKey(CommandContext<CommandSourceStack> ctx) {
        return giveVirtualKey(ctx, resolvePlayer(ctx, "player"), 1);
    }

    private int handleGiveVirtualKeyAmount(CommandContext<CommandSourceStack> ctx) {
        return giveVirtualKey(ctx, resolvePlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"));
    }

    private int giveVirtualKey(CommandContext<CommandSourceStack> ctx, Player target, int amount) {
        if (target == null) return Command.SINGLE_SUCCESS;
        String crateId = StringArgumentType.getString(ctx, "crate");
        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) { sendNoCrate(ctx, crateId); return Command.SINGLE_SUCCESS; }
        plugin.getVirtualKeyManager().addKeys(target, crateId, amount);
        sendKeyGiven(ctx, crate, target);
        return Command.SINGLE_SUCCESS;
    }

    // --- Give crate item (self) ---
    private int handleGiveCrateSelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(TextUtil.parse("<red>You must be a player or specify a target."));
            return Command.SINGLE_SUCCESS;
        }
        return giveCrate(ctx, player, 1);
    }

    private int handleGiveCrate(CommandContext<CommandSourceStack> ctx) {
        return giveCrate(ctx, resolvePlayer(ctx, "player"), 1);
    }

    private int handleGiveCrateAmount(CommandContext<CommandSourceStack> ctx) {
        return giveCrate(ctx, resolvePlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"));
    }

    private int giveCrate(CommandContext<CommandSourceStack> ctx, Player target, int amount) {
        if (target == null) return Command.SINGLE_SUCCESS;
        String crateId = StringArgumentType.getString(ctx, "crate");
        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) { sendNoCrate(ctx, crateId); return Command.SINGLE_SUCCESS; }
        ItemStack crateItem = plugin.getKeyManager().createCrateItem(crate, amount);
        if (crateItem != null) {
            Map<Integer, ItemStack> leftover = target.getInventory().addItem(crateItem);
            if (!leftover.isEmpty()) {
                if (plugin.getPluginConfig().isGiveFullInventoryDrop()) {
                    leftover.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
                    String msg = plugin.getPluginConfig().getPrefix()
                            + plugin.getPluginConfig().getMessage("inventory_full_dropped")
                                    .replace("<crate>", crate.getName())
                                    .replace("<player>", target.getName());
                    target.sendMessage(TextUtil.parse(msg));
                } else {
                    String msg = plugin.getPluginConfig().getPrefix()
                            + plugin.getPluginConfig().getMessage("inventory_full_no_space")
                                    .replace("<crate>", crate.getName())
                                    .replace("<player>", target.getName());
                    target.sendMessage(TextUtil.parse(msg));
                }
                return Command.SINGLE_SUCCESS;
            }
        }
        String msg = plugin.getPluginConfig().getPrefix()
                + plugin.getPluginConfig().getMessage("crate_given")
                        .replace("<crate>", crate.getName())
                        .replace("<player>", target.getName());
        ctx.getSource().getSender().sendMessage(TextUtil.parse(msg));
        return Command.SINGLE_SUCCESS;
    }

    // --- Helpers ---

    private Player resolvePlayer(CommandContext<CommandSourceStack> ctx, String argName) {
        try {
            PlayerSelectorArgumentResolver resolver = ctx.getArgument(argName, PlayerSelectorArgumentResolver.class);
            List<Player> players = resolver.resolve(ctx.getSource());
            if (players.isEmpty()) {
                sendNoPlayer(ctx, argName);
                return null;
            }
            return players.get(0);
        } catch (Exception e) {
            return ctx.getSource().getSender() instanceof Player p ? p : null;
        }
    }

    private void sendKeyGiven(CommandContext<CommandSourceStack> ctx, Crate crate, Player target) {
        String msg = plugin.getPluginConfig().getPrefix()
                + plugin.getPluginConfig().getMessage("key_given")
                        .replace("<crate>", crate.getName())
                        .replace("<player>", target.getName());
        ctx.getSource().getSender().sendMessage(TextUtil.parse(msg));
    }

    private void sendNoCrate(CommandContext<CommandSourceStack> ctx, String crateId) {
        String msg = plugin.getPluginConfig().getPrefix()
                + plugin.getPluginConfig().getMessage("no_crate").replace("<crate>", crateId);
        ctx.getSource().getSender().sendMessage(TextUtil.parse(msg));
    }

    private void sendNoPlayer(CommandContext<CommandSourceStack> ctx, String playerArg) {
        String msg = plugin.getPluginConfig().getPrefix()
                + plugin.getPluginConfig().getMessage("no_player").replace("<player>", playerArg);
        ctx.getSource().getSender().sendMessage(TextUtil.parse(msg));
    }
}
