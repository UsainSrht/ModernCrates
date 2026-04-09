package me.usainsrht.moderncrates.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.gui.PlayerMenuGui;
import me.usainsrht.moderncrates.gui.PreviewGui;
import me.usainsrht.moderncrates.gui.editor.MainMenuGui;
import me.usainsrht.moderncrates.util.SoundUtil;
import me.usainsrht.moderncrates.util.TextUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

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

                // /moderncrates open <crate> [player]
                .then(Commands.literal("open")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests(suggestCrates())
                                .executes(this::handleOpen)
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(suggestPlayers())
                                        .executes(this::handleOpenForPlayer))))

                // /moderncrates give key <crate> <player> [amount]
                .then(Commands.literal("give")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                        .then(Commands.literal("key")
                                .then(Commands.argument("crate", StringArgumentType.word())
                                        .suggests(suggestCrates())
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests(suggestPlayers())
                                                .executes(this::handleGiveKey)
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(this::handleGiveKeyAmount)))))

                        // /moderncrates give virtualkey <crate> <player> [amount]
                        .then(Commands.literal("virtualkey")
                                .then(Commands.argument("crate", StringArgumentType.word())
                                        .suggests(suggestCrates())
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests(suggestPlayers())
                                                .executes(this::handleGiveVirtualKey)
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(this::handleGiveVirtualKeyAmount)))))

                        // /moderncrates give crate <crate> <player> [amount]
                        .then(Commands.literal("crate")
                                .then(Commands.argument("crate", StringArgumentType.word())
                                        .suggests(suggestCrates())
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .suggests(suggestPlayers())
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

    private SuggestionProvider<CommandSourceStack> suggestPlayers() {
        return (context, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(remaining))
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
        String playerName = StringArgumentType.getString(ctx, "player");

        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) {
            sendNoCrate(ctx, crateId);
            return Command.SINGLE_SUCCESS;
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            String msg = plugin.getPluginConfig().getPrefix()
                    + plugin.getPluginConfig().getMessage("no_player").replace("<player>", playerName);
            ctx.getSource().getSender().sendMessage(TextUtil.parse(msg));
            return Command.SINGLE_SUCCESS;
        }

        plugin.openCrate(target, crate);
        return Command.SINGLE_SUCCESS;
    }

    private int handleGiveKey(CommandContext<CommandSourceStack> ctx) {
        return giveKey(ctx, 1);
    }

    private int handleGiveKeyAmount(CommandContext<CommandSourceStack> ctx) {
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        return giveKey(ctx, amount);
    }

    private int giveKey(CommandContext<CommandSourceStack> ctx, int amount) {
        String crateId = StringArgumentType.getString(ctx, "crate");
        String playerName = StringArgumentType.getString(ctx, "player");

        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) { sendNoCrate(ctx, crateId); return Command.SINGLE_SUCCESS; }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) { sendNoPlayer(ctx, playerName); return Command.SINGLE_SUCCESS; }

        ItemStack key = plugin.getKeyManager().createKey(crate, amount);
        if (key != null) {
            target.getInventory().addItem(key);
        }

        String msg = plugin.getPluginConfig().getPrefix()
                + plugin.getPluginConfig().getMessage("key_given")
                        .replace("<crate>", crate.getName())
                        .replace("<player>", target.getName());
        ctx.getSource().getSender().sendMessage(TextUtil.parse(msg));
        return Command.SINGLE_SUCCESS;
    }

    private int handleGiveVirtualKey(CommandContext<CommandSourceStack> ctx) {
        return giveVirtualKey(ctx, 1);
    }

    private int handleGiveVirtualKeyAmount(CommandContext<CommandSourceStack> ctx) {
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        return giveVirtualKey(ctx, amount);
    }

    private int giveVirtualKey(CommandContext<CommandSourceStack> ctx, int amount) {
        String crateId = StringArgumentType.getString(ctx, "crate");
        String playerName = StringArgumentType.getString(ctx, "player");

        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) { sendNoCrate(ctx, crateId); return Command.SINGLE_SUCCESS; }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) { sendNoPlayer(ctx, playerName); return Command.SINGLE_SUCCESS; }

        plugin.getVirtualKeyManager().addKeys(target, crateId, amount);

        String msg = plugin.getPluginConfig().getPrefix()
                + plugin.getPluginConfig().getMessage("key_given")
                        .replace("<crate>", crate.getName())
                        .replace("<player>", target.getName());
        ctx.getSource().getSender().sendMessage(TextUtil.parse(msg));
        return Command.SINGLE_SUCCESS;
    }

    private int handleGiveCrate(CommandContext<CommandSourceStack> ctx) {
        return giveCrate(ctx, 1);
    }

    private int handleGiveCrateAmount(CommandContext<CommandSourceStack> ctx) {
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        return giveCrate(ctx, amount);
    }

    private int giveCrate(CommandContext<CommandSourceStack> ctx, int amount) {
        String crateId = StringArgumentType.getString(ctx, "crate");
        String playerName = StringArgumentType.getString(ctx, "player");

        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null) { sendNoCrate(ctx, crateId); return Command.SINGLE_SUCCESS; }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) { sendNoPlayer(ctx, playerName); return Command.SINGLE_SUCCESS; }

        ItemStack crateItem = plugin.getKeyManager().createCrateItem(crate, amount);
        if (crateItem != null) {
            target.getInventory().addItem(crateItem);
        }

        String msg = plugin.getPluginConfig().getPrefix()
                + plugin.getPluginConfig().getMessage("crate_given")
                        .replace("<crate>", crate.getName())
                        .replace("<player>", target.getName());
        ctx.getSource().getSender().sendMessage(TextUtil.parse(msg));
        return Command.SINGLE_SUCCESS;
    }

    private void sendNoCrate(CommandContext<CommandSourceStack> ctx, String crateId) {
        String msg = plugin.getPluginConfig().getPrefix()
                + plugin.getPluginConfig().getMessage("no_crate").replace("<crate>", crateId);
        ctx.getSource().getSender().sendMessage(TextUtil.parse(msg));
    }

    private void sendNoPlayer(CommandContext<CommandSourceStack> ctx, String playerName) {
        String msg = plugin.getPluginConfig().getPrefix()
                + plugin.getPluginConfig().getMessage("no_player").replace("<player>", playerName);
        ctx.getSource().getSender().sendMessage(TextUtil.parse(msg));
    }
}
