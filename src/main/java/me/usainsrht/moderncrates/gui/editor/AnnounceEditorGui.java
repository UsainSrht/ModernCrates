package me.usainsrht.moderncrates.gui.editor;

import me.usainsrht.moderncrates.ModernCratesPlugin;
import me.usainsrht.moderncrates.api.crate.AnnounceConfig;
import me.usainsrht.moderncrates.api.crate.Crate;
import me.usainsrht.moderncrates.util.ItemBuilder;
import me.usainsrht.moderncrates.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class AnnounceEditorGui extends EditorGuiBase {

    private final Crate crate;

    public AnnounceEditorGui(Player player, ModernCratesPlugin plugin, Crate crate) {
        super(player, plugin);
        this.crate = crate;
    }

    @Override
    public void open() {
        AnnounceConfig ac = crate.getAnnounceConfig();

        inventory = Bukkit.createInventory(this, 36, TextUtil.parse("<dark_red><bold>Announce: " + crate.getName()));
        fillBlack();

        if (ac == null) {
            inventory.setItem(13, ItemBuilder.create("BARRIER", "<red>No announce config",
                    List.of("<gray>Click to create default")));
        } else {
            inventory.setItem(10, ItemBuilder.create(ac.isToEveryone() ? "LIME_DYE" : "GRAY_DYE",
                    "<yellow><bold>To Everyone: <white>" + ac.isToEveryone(), List.of("<gray>Click to toggle")));
            inventory.setItem(12, ItemBuilder.create("PAPER", "<yellow><bold>Single: <white>" + orNone(ac.getSingle()),
                    List.of("<gray>Click to set")));
            inventory.setItem(14, ItemBuilder.create("PAPER", "<yellow><bold>Multiple: <white>" + orNone(ac.getMultiple()),
                    List.of("<gray>Click to set")));
            inventory.setItem(16, ItemBuilder.create("PAPER", "<yellow><bold>Multiple Item: <white>" + orNone(ac.getMultipleItem()),
                    List.of("<gray>Click to set")));
        }

        inventory.setItem(27, ItemBuilder.create("ARROW", "<red><bold>Back", List.of("<gray>Return to crate editor")));
        inventory.setItem(31, ItemBuilder.create("LIME_WOOL", "<green><bold>Save", List.of("<gray>Save crate")));
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        AnnounceConfig ac = crate.getAnnounceConfig();

        if (slot == 13 && ac == null) {
            ac = new AnnounceConfig();
            ac.setToEveryone(true);
            ac.setSingle("<gold><player> <gray>won <gold><reward_name> <gray>from <gold>" + crate.getName());
            crate.setAnnounceConfig(ac);
            open();
            return;
        }
        if (ac == null) return;

        AnnounceConfig finalAc = ac;
        switch (slot) {
            case 10 -> { finalAc.setToEveryone(!finalAc.isToEveryone()); open(); }
            case 12 -> requestSignInput("Single message", input -> {
                finalAc.setSingle(input);
                open();
            });
            case 14 -> requestSignInput("Multiple msg", input -> {
                finalAc.setMultiple(input);
                open();
            });
            case 16 -> requestSignInput("Multi item msg", input -> {
                finalAc.setMultipleItem(input);
                open();
            });
            case 27 -> new CrateEditorGui(player, plugin, crate).open();
            case 31 -> { saveCrate(crate); player.sendMessage(TextUtil.parse("<green>Crate saved!")); }
        }
    }
}
