package me.usainsrht.moderncrates.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.api.crate.CrateLocation;
import me.usainsrht.moderncrates.gui.PlayerMenuGui;
import me.usainsrht.moderncrates.gui.PreviewGui;
import me.usainsrht.moderncrates.gui.editor.MainMenuGui;
import me.usainsrht.moderncrates.util.PlaceholderUtil;
import me.usainsrht.moderncrates.util.TextUtil;
import me.usainsrht.yamlmessage.YamlMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
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
                                                        .executes(this::handleGiveCrateAmount))))))

                // /moderncrates virtualkey ...
                .then(buildVirtualKeyCommand());
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildVirtualKeyCommand() {
        return Commands.literal("virtualkey")
                .executes(this::handleVirtualKeyListSelf)

                // list [player]
                .then(Commands.literal("list")
                        .executes(this::handleVirtualKeyListSelf)
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                                .executes(this::handleVirtualKeyList)))

                // set <crate> <player> <amount>
                .then(Commands.literal("set")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests(suggestCrates())
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(this::handleSetVirtualKey)))))

                // give <crate> [player] [amount]
                .then(Commands.literal("give")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests(suggestCrates())
                                .executes(this::handleGiveVirtualKeySelf)
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .executes(this::handleGiveVirtualKey)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(this::handleGiveVirtualKeyAmount)))))

                // add <crate> [player] [amount]
                .then(Commands.literal("add")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests(suggestCrates())
                                .executes(this::handleGiveVirtualKeySelf)
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .executes(this::handleGiveVirtualKey)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(this::handleGiveVirtualKeyAmount)))))

                // take <crate> [player] [amount]
                .then(Commands.literal("take")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests(suggestCrates())
                                .executes(this::handleTakeVirtualKeySelf)
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .executes(this::handleTakeVirtualKey)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(this::handleTakeVirtualKeyAmount)))))

                // remove <crate> [player] [amount]
                .then(Commands.literal("remove")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests(suggestCrates())
                                .executes(this::handleTakeVirtualKeySelf)
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .executes(this::handleTakeVirtualKey)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(this::handleTakeVirtualKeyAmount)))))

                // rake <crate> [player] [amount]
                .then(Commands.literal("rake")
                        .requires(src -> src.getSender().hasPermission("moderncrates.admin"))
                        .then(Commands.argument("crate", StringArgumentType.word())
                                .suggests(suggestCrates())
                                .executes(this::handleTakeVirtualKeySelf)
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .executes(this::handleTakeVirtualKey)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(this::handleTakeVirtualKeyAmount)))));
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
            PlaceholderUtil.send(YamlMessage.chat("<red>This command can only be used by players."), ctx.getSource().getSender());
            return Command.SINGLE_SUCCESS;
        }

        PlayerMenuGui gui = new PlayerMenuGui(player, plugin);
        gui.open();
        return Command.SINGLE_SUCCESS;
    }

    private int handleReload(CommandContext<CommandSourceStack> ctx) {
        plugin.reloadPlugin();
        PlaceholderUtil.send(plugin.getPluginConfig().getMessage("reload"),
                ctx.getSource().getSender(), plugin.getPluginConfig().getPrefix());
        return Command.SINGLE_SUCCESS;
    }

    private int handleEditor(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            PlaceholderUtil.send(YamlMessage.chat("<red>This command can only be used by players."), ctx.getSource().getSender());
            return Command.SINGLE_SUCCESS;
        }

        MainMenuGui gui = new MainMenuGui(player, plugin);
        gui.open();
        return Command.SINGLE_SUCCESS;
    }

    private int handlePreview(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            PlaceholderUtil.send(YamlMessage.chat("<red>This command can only be used by players."), ctx.getSource().getSender());
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
            PlaceholderUtil.send(YamlMessage.chat("<red>This command can only be used by players."), ctx.getSource().getSender());
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
            PlaceholderUtil.send(YamlMessage.chat("<red>No block in sight (max 10 blocks)."), player);
            return Command.SINGLE_SUCCESS;
        }

        CrateLocation loc = new CrateLocation(target.getWorld().getName(), target.getX(), target.getY(), target.getZ());
        crate.addCrateLocation(loc);

        try {
            plugin.getCrateConfigParser().save(crate, new File(plugin.getDataFolder(), "crates"));
        } catch (Exception e) {
            PlaceholderUtil.send(YamlMessage.chat("<red>Failed to save: " + e.getMessage()), player);
        }
        plugin.getHologramManager().removeHologram(crateId);
        plugin.getHologramManager().createHologram(crate);

        PlaceholderUtil.send(YamlMessage.chat("<green>Location added to crate <white><crate><green>! (<amount> total)"),
                player, plugin.getPluginConfig().getPrefix(),
                PlaceholderUtil.combine(player,
                        PlaceholderUtil.parsed("crate", crate.getName()),
                        PlaceholderUtil.parsed("amount", String.valueOf(crate.getCrateLocations().size()))));
        return Command.SINGLE_SUCCESS;
    }

    private int handleOpen(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            PlaceholderUtil.send(YamlMessage.chat("<red>This command can only be used by players."), ctx.getSource().getSender());
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
            PlaceholderUtil.send(YamlMessage.chat("<red>You must be a player or specify a target."), ctx.getSource().getSender());
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
                    PlaceholderUtil.send(plugin.getPluginConfig().getMessage("inventory_full_virtual_key"),
                            target, plugin.getPluginConfig().getPrefix(), PlaceholderUtil.crateResolvers(target, crate));
                } else {
                    PlaceholderUtil.send(plugin.getPluginConfig().getMessage("inventory_full_no_space"),
                            target, plugin.getPluginConfig().getPrefix(), PlaceholderUtil.crateResolvers(target, crate));
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
            PlaceholderUtil.send(YamlMessage.chat("<red>You must be a player or specify a target."), ctx.getSource().getSender());
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
        sendVirtualKeyGiven(ctx, crateId, crate, target, amount);
        return Command.SINGLE_SUCCESS;
    }

    // --- Take / Remove virtualkey ---
    private int handleTakeVirtualKeySelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            PlaceholderUtil.send(YamlMessage.chat("<red>You must be a player or specify a target."), ctx.getSource().getSender());
            return Command.SINGLE_SUCCESS;
        }
        return takeVirtualKey(ctx, player, 1);
    }

    private int handleTakeVirtualKey(CommandContext<CommandSourceStack> ctx) {
        return takeVirtualKey(ctx, resolvePlayer(ctx, "player"), 1);
    }

    private int handleTakeVirtualKeyAmount(CommandContext<CommandSourceStack> ctx) {
        return takeVirtualKey(ctx, resolvePlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "amount"));
    }

    private int takeVirtualKey(CommandContext<CommandSourceStack> ctx, Player target, int amount) {
        if (target == null) return Command.SINGLE_SUCCESS;
        String crateId = StringArgumentType.getString(ctx, "crate");
        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null && plugin.getVirtualKeyManager().getKeys(target, crateId) <= 0) {
            sendNoCrate(ctx, crateId);
            return Command.SINGLE_SUCCESS;
        }
        int current = plugin.getVirtualKeyManager().getKeys(target, crateId);
        int taken = Math.min(current, amount);
        plugin.getVirtualKeyManager().removeKeys(target, crateId, amount);
        sendVirtualKeyTaken(ctx, crateId, crate, target, taken);
        return Command.SINGLE_SUCCESS;
    }

    // --- Set virtualkey ---
    private int handleSetVirtualKey(CommandContext<CommandSourceStack> ctx) {
        Player target = resolvePlayer(ctx, "player");
        if (target == null) return Command.SINGLE_SUCCESS;
        String crateId = StringArgumentType.getString(ctx, "crate");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        Crate crate = plugin.getCrateRegistry().get(crateId);
        if (crate == null && plugin.getVirtualKeyManager().getKeys(target, crateId) <= 0 && amount > 0) {
            sendNoCrate(ctx, crateId);
            return Command.SINGLE_SUCCESS;
        }
        plugin.getVirtualKeyManager().setKeys(target, crateId, amount);
        sendVirtualKeySet(ctx, crateId, crate, target, amount);
        return Command.SINGLE_SUCCESS;
    }

    // --- List virtual keys ---
    private int handleVirtualKeyListSelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            PlaceholderUtil.send(YamlMessage.chat("<red>You must be a player or specify a target player: /moderncrates virtualkey list <player>"), ctx.getSource().getSender());
            return Command.SINGLE_SUCCESS;
        }
        return listVirtualKeys(ctx, player);
    }

    private int handleVirtualKeyList(CommandContext<CommandSourceStack> ctx) {
        Player target = resolvePlayer(ctx, "player");
        if (target == null) return Command.SINGLE_SUCCESS;
        return listVirtualKeys(ctx, target);
    }

    private int listVirtualKeys(CommandContext<CommandSourceStack> ctx, Player target) {
        Map<String, Integer> playerKeys = plugin.getVirtualKeyManager().getPlayerKeys(target.getUniqueId());
        var sender = ctx.getSource().getSender();
        if (playerKeys.isEmpty()) {
            YamlMessage emptyMsg = plugin.getPluginConfig().getMessage("virtual_key_list_empty");
            if (emptyMsg.isEmpty()) {
                emptyMsg = YamlMessage.chat("<yellow><player> has no virtual keys.");
            }
            PlaceholderUtil.send(emptyMsg, sender, plugin.getPluginConfig().getPrefix(),
                    PlaceholderUtil.parsed("player", target.getName()));
            return Command.SINGLE_SUCCESS;
        }

        YamlMessage headerMsg = plugin.getPluginConfig().getMessage("virtual_key_list_header");
        if (headerMsg.isEmpty()) {
            headerMsg = YamlMessage.chat("<gold>Virtual Keys for <yellow><player><gold>:");
        }
        PlaceholderUtil.send(headerMsg, sender, plugin.getPluginConfig().getPrefix(),
                PlaceholderUtil.parsed("player", target.getName()));

        YamlMessage entryMsg = plugin.getPluginConfig().getMessage("virtual_key_list_entry");
        boolean hasCustomEntry = !entryMsg.isEmpty();

        for (Map.Entry<String, Integer> entry : playerKeys.entrySet()) {
            String crateId = entry.getKey();
            int amount = entry.getValue();
            Crate crate = plugin.getCrateRegistry().get(crateId);
            String crateDisplayName = crate != null ? crate.getName() : crateId + " <gray>(unloaded)";

            if (hasCustomEntry) {
                PlaceholderUtil.send(entryMsg, sender, null,
                        buildVirtualKeyResolvers(target, crate, crateId, crateDisplayName, amount));
            } else {
                sender.sendMessage(TextUtil.parse(" <gray>- <gold>" + crateDisplayName + "<gray>: <green>" + amount));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private TagResolver[] buildVirtualKeyResolvers(Player target, Crate crate, String crateId, String crateName, int amount) {
        List<TagResolver> list = new ArrayList<>();
        if (crate != null) {
            list.addAll(List.of(PlaceholderUtil.crateResolvers(target, crate)));
        } else {
            list.add(PlaceholderUtil.parsed("player", target.getName()));
            list.add(PlaceholderUtil.component("player_display", target.displayName()));
            list.add(PlaceholderUtil.parsed("crate", crateName));
            list.add(PlaceholderUtil.parsed("crate_id", crateId));
        }
        list.add(PlaceholderUtil.parsed("amount", String.valueOf(amount)));
        return list.toArray(new TagResolver[0]);
    }

    private void sendVirtualKeyGiven(CommandContext<CommandSourceStack> ctx, String crateId, Crate crate, Player target, int amount) {
        YamlMessage msg = plugin.getPluginConfig().getMessage("virtual_key_given");
        if (msg.isEmpty()) {
            msg = plugin.getPluginConfig().getMessage("key_given");
        }
        if (msg.isEmpty()) {
            msg = YamlMessage.chat("<green>Given <dark_green><amount> <green>virtual key(s) for <dark_green><crate> <green>to <dark_green><player><green>!");
        }
        String crateName = crate != null ? crate.getName() : crateId;
        PlaceholderUtil.send(msg,
                ctx.getSource().getSender(), plugin.getPluginConfig().getPrefix(),
                buildVirtualKeyResolvers(target, crate, crateId, crateName, amount));
    }

    private void sendVirtualKeyTaken(CommandContext<CommandSourceStack> ctx, String crateId, Crate crate, Player target, int amount) {
        YamlMessage msg = plugin.getPluginConfig().getMessage("virtual_key_taken");
        if (msg.isEmpty()) {
            msg = plugin.getPluginConfig().getMessage("key_taken");
        }
        if (msg.isEmpty()) {
            msg = YamlMessage.chat("<green>Removed <dark_green><amount> <green>virtual key(s) for <dark_green><crate> <green>from <dark_green><player><green>!");
        }
        String crateName = crate != null ? crate.getName() : crateId;
        PlaceholderUtil.send(msg,
                ctx.getSource().getSender(), plugin.getPluginConfig().getPrefix(),
                buildVirtualKeyResolvers(target, crate, crateId, crateName, amount));
    }

    private void sendVirtualKeySet(CommandContext<CommandSourceStack> ctx, String crateId, Crate crate, Player target, int amount) {
        YamlMessage msg = plugin.getPluginConfig().getMessage("virtual_key_set");
        if (msg.isEmpty()) {
            msg = YamlMessage.chat("<green>Set virtual keys for <dark_green><crate> <green>to <dark_green><amount> <green>for <dark_green><player><green>!");
        }
        String crateName = crate != null ? crate.getName() : crateId;
        PlaceholderUtil.send(msg,
                ctx.getSource().getSender(), plugin.getPluginConfig().getPrefix(),
                buildVirtualKeyResolvers(target, crate, crateId, crateName, amount));
    }

    // --- Give crate item (self) ---
    private int handleGiveCrateSelf(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            PlaceholderUtil.send(YamlMessage.chat("<red>You must be a player or specify a target."), ctx.getSource().getSender());
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
                    PlaceholderUtil.send(plugin.getPluginConfig().getMessage("inventory_full_dropped"),
                            target, plugin.getPluginConfig().getPrefix(), PlaceholderUtil.crateResolvers(target, crate));
                } else {
                    PlaceholderUtil.send(plugin.getPluginConfig().getMessage("inventory_full_no_space"),
                            target, plugin.getPluginConfig().getPrefix(), PlaceholderUtil.crateResolvers(target, crate));
                }
                return Command.SINGLE_SUCCESS;
            }
        }
        PlaceholderUtil.send(plugin.getPluginConfig().getMessage("crate_given"),
                ctx.getSource().getSender(), plugin.getPluginConfig().getPrefix(), PlaceholderUtil.crateResolvers(target, crate));
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
        PlaceholderUtil.send(plugin.getPluginConfig().getMessage("key_given"),
                ctx.getSource().getSender(), plugin.getPluginConfig().getPrefix(), PlaceholderUtil.crateResolvers(target, crate));
    }

    private void sendNoCrate(CommandContext<CommandSourceStack> ctx, String crateId) {
        PlaceholderUtil.send(plugin.getPluginConfig().getMessage("no_crate"),
                ctx.getSource().getSender(), plugin.getPluginConfig().getPrefix(),
                PlaceholderUtil.combine(PlaceholderUtil.parsed("crate", crateId)));
    }

    private void sendNoPlayer(CommandContext<CommandSourceStack> ctx, String playerArg) {
        PlaceholderUtil.send(plugin.getPluginConfig().getMessage("no_player"),
                ctx.getSource().getSender(), plugin.getPluginConfig().getPrefix(),
                PlaceholderUtil.combine(PlaceholderUtil.parsed("player", playerArg)));
    }
}
