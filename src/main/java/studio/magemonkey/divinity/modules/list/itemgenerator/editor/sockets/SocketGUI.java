package studio.magemonkey.divinity.modules.list.itemgenerator.editor.sockets;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.LoreGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.stats.MainStatsGUI;

import java.util.ArrayList;
import java.util.List;

public class SocketGUI extends AbstractEditorGUI {
    private final String name;

    public SocketGUI(Player player, ItemGeneratorReference itemGenerator, String name) {
        super(player,
                1,
                "Editor/" + EditorGUI.ItemType.SOCKETS.getTitle(),
                itemGenerator);
        this.name = name;
    }

    @Override
    public void setContents() {
        String path = EditorGUI.ItemType.SOCKETS.getPath() + '.' + name + '.';
        setSlot(0, new Slot(createItem(Material.BROWN_MUSHROOM,
                "&eMinimum " + this.name + " sockets",
                "&bCurrent: &a" + itemGenerator.getConfig().getInt(path + MainStatsGUI.ItemType.MINIMUM.getPath()),
                "&6Left-Click: &eSet",
                "&6Shift-Left-Click: &eDecrease",
                "&6Shift-Right-Click: &eIncrease",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onShiftLeftClick() {
                itemGenerator.getConfig()
                        .set(path + MainStatsGUI.ItemType.MINIMUM.getPath(),
                                Math.max(0,
                                        itemGenerator.getConfig().getInt(path + MainStatsGUI.ItemType.MINIMUM.getPath())
                                                - 1));
                saveAndReopen();
            }

            @Override
            public void onShiftRightClick() {
                itemGenerator.getConfig()
                        .set(path + MainStatsGUI.ItemType.MINIMUM.getPath(),
                                Math.max(0,
                                        itemGenerator.getConfig().getInt(path + MainStatsGUI.ItemType.MINIMUM.getPath())
                                                + 1));
                saveAndReopen();
            }

            @Override
            public void onLeftClick() {
                sendSetMessage(MainStatsGUI.ItemType.MINIMUM.name().toLowerCase() + ' ' + name + " sockets",
                        String.valueOf(itemGenerator.getConfig()
                                .getInt(path + MainStatsGUI.ItemType.MINIMUM.getPath())),
                        s -> {
                            int value = Integer.parseInt(s);
                            if (value >= 0) {
                                itemGenerator.getConfig().set(path + MainStatsGUI.ItemType.MINIMUM.getPath(), value);
                            } else {
                                throw new IllegalArgumentException();
                            }
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                itemGenerator.getConfig().set(path + MainStatsGUI.ItemType.MINIMUM.getPath(), 0);
                saveAndReopen();
            }
        });
        setSlot(1, new Slot(createItem(Material.RED_MUSHROOM,
                "&eMaximum " + this.name + " sockets",
                "&bCurrent: &a" + itemGenerator.getConfig().getInt(path + MainStatsGUI.ItemType.MAXIMUM.getPath()),
                "&6Left-Click: &eSet",
                "&6Shift-Left-Click: &eDecrease",
                "&6Shift-Right-Click: &eIncrease",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onShiftLeftClick() {
                itemGenerator.getConfig()
                        .set(path + MainStatsGUI.ItemType.MAXIMUM.getPath(),
                                Math.max(0,
                                        itemGenerator.getConfig().getInt(path + MainStatsGUI.ItemType.MAXIMUM.getPath())
                                                - 1));
                saveAndReopen();
            }

            @Override
            public void onShiftRightClick() {
                itemGenerator.getConfig()
                        .set(path + MainStatsGUI.ItemType.MAXIMUM.getPath(),
                                Math.max(0,
                                        itemGenerator.getConfig().getInt(path + MainStatsGUI.ItemType.MAXIMUM.getPath())
                                                + 1));
                saveAndReopen();
            }

            @Override
            public void onLeftClick() {
                sendSetMessage(MainStatsGUI.ItemType.MAXIMUM.name().toLowerCase() + ' ' + name + " sockets",
                        String.valueOf(itemGenerator.getConfig()
                                .getInt(path + MainStatsGUI.ItemType.MAXIMUM.getPath())),
                        s -> {
                            int value = Integer.parseInt(s);
                            if (value >= 0) {
                                itemGenerator.getConfig().set(path + MainStatsGUI.ItemType.MAXIMUM.getPath(), value);
                            } else {
                                throw new IllegalArgumentException();
                            }
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                itemGenerator.getConfig().set(path + MainStatsGUI.ItemType.MAXIMUM.getPath(), 0);
                saveAndReopen();
            }
        });
        setSlot(2, new Slot(createItem(Material.WRITABLE_BOOK,
                "&eLore format",
                StringUT.replace(CURRENT_PLACEHOLDER,
                        itemGenerator.getConfig().getStringList(path + MainStatsGUI.ItemType.LORE.getPath()),
                        "&bCurrent:",
                        "&a----------",
                        "&f%current%",
                        "&a----------",
                        "&6Left-Click: &eModify",
                        "&6Right-Click: &eSet to default value")
        )) {
            @Override
            public void onLeftClick() {
                openSubMenu(new LoreGUI(player,
                        title + " lore",
                        itemGenerator,
                        path + MainStatsGUI.ItemType.LORE.getPath()));
            }

            @Override
            public void onDrop() {
                itemGenerator.getConfig()
                        .set(path + MainStatsGUI.ItemType.LORE.getPath(),
                                StringUT.replace(CURRENT_PLACEHOLDER, List.of(name),
                                        "&8&m               &f  「 %current%S 」  &8&m               ",
                                        "%SOCKET_%current%_DEFAULT%"));
                saveAndReopen();
            }
        });
        List<String>         lore        = new ArrayList<>();
        ConfigurationSection listSection = itemGenerator.getConfig().getConfigurationSection(path + ".list");
        if (listSection != null) {
            for (String key : listSection.getKeys(false)) {
                lore.add("&a- " + key + ": &f" + listSection.getDouble(key + ".chance", 0));
            }
        }
        setSlot(3, new Slot(createItem(Material.IRON_SWORD,
                "&eList of chances per tier", StringUT.replace(CURRENT_PLACEHOLDER, lore,
                        "&bCurrent:",
                        "%current%",
                        "&eModify"))) {
            @Override
            public void onLeftClick() {
                openSubMenu(new SocketListGUI(player, itemGenerator, name));
            }
        });
    }
}
