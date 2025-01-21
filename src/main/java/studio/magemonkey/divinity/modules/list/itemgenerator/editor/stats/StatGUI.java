package studio.magemonkey.divinity.modules.list.itemgenerator.editor.stats;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

public class StatGUI extends AbstractEditorGUI {
    private final EditorGUI.ItemType itemType;
    private final String             path;

    public StatGUI(Player player, ItemGeneratorReference itemGenerator, EditorGUI.ItemType itemType, String path) {
        super(player, 1, "Editor/" + itemType.getTitle(), itemGenerator);
        this.itemType = itemType;
        this.path = path;
    }

    @Override
    public void setContents() {
        setSlot(0, new Slot(createItem(Material.DROPPER,
                "&eChance",
                "&bCurrent: &a" + itemGenerator.getConfig().getDouble(ItemType.CHANCE.getPath(this.path)),
                "&6Left-Click: &eSet",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.CHANCE.getTitle(),
                        String.valueOf(itemGenerator.getConfig().getDouble(path)),
                        s -> {
                            itemGenerator.getConfig().set(ItemType.CHANCE.getPath(path), Double.parseDouble(s));
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                itemGenerator.getConfig().set(ItemType.CHANCE.getPath(path), 0);
                saveAndReopen();
            }
        });
        setSlot(1, new Slot(createItem(Material.EXPERIENCE_BOTTLE,
                "&eScale by Level",
                "&bCurrent: &a" + itemGenerator.getConfig().getDouble(ItemType.SCALE_BY_LEVEL.getPath(this.path)),
                "&6Left-Click: &eSet",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.SCALE_BY_LEVEL.getTitle(),
                        String.valueOf(itemGenerator.getConfig().getDouble(ItemType.SCALE_BY_LEVEL.getPath(path))),
                        s -> {
                            itemGenerator.getConfig().set(ItemType.SCALE_BY_LEVEL.getPath(path), Double.parseDouble(s));
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                itemGenerator.getConfig().set(ItemType.SCALE_BY_LEVEL.getPath(path), 0);
                saveAndReopen();
            }
        });
        setSlot(2, new Slot(createItem(Material.BROWN_MUSHROOM,
                "&eMinimum Value",
                "&bCurrent: &a" + itemGenerator.getConfig().getDouble(ItemType.MIN.getPath(this.path)),
                "&6Left-Click: &eSet",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.MIN.getTitle(),
                        String.valueOf(itemGenerator.getConfig().getDouble(ItemType.MIN.getPath(path))),
                        s -> {
                            itemGenerator.getConfig().set(ItemType.MIN.getPath(path), Double.parseDouble(s));
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                itemGenerator.getConfig().set(ItemType.MIN.getPath(path), 0);
                saveAndReopen();
            }
        });
        setSlot(3, new Slot(createItem(Material.RED_MUSHROOM,
                "&eMaximum Value",
                "&bCurrent: &a" + itemGenerator.getConfig().getDouble(ItemType.MAX.getPath(this.path)),
                "&6Left-Click: &eSet",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.MAX.getTitle(),
                        String.valueOf(itemGenerator.getConfig().getDouble(ItemType.MAX.getPath(path))),
                        s -> {
                            itemGenerator.getConfig().set(ItemType.MAX.getPath(path), Double.parseDouble(s));
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                itemGenerator.getConfig().set(ItemType.MAX.getPath(path), 0);
                saveAndReopen();
            }
        });
        boolean flatRange = itemGenerator.getConfig().getBoolean(ItemType.FLAT_RANGE.getPath(this.path));
        setSlot(4, new Slot(createItem(flatRange ? Material.STRUCTURE_VOID : Material.BARRIER,
                "&eFlat Range",
                "&bCurrent: &a" + flatRange,
                "&6Left-Click: &eToggle",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onLeftClick() {
                itemGenerator.getConfig().set(ItemType.FLAT_RANGE.getPath(path), !flatRange);
                saveAndReopen();
            }

            @Override
            public void onRightClick() {
                itemGenerator.getConfig().set(ItemType.FLAT_RANGE.getPath(path), false);
                saveAndReopen();
            }
        });
        if (this.itemType != EditorGUI.ItemType.FABLED_ATTRIBUTES) {
            boolean round = itemGenerator.getConfig().getBoolean(ItemType.ROUND.getPath(this.path));
            setSlot(5, new Slot(createItem(round ? Material.SNOWBALL : Material.SNOW_BLOCK,
                    "&eRound",
                    "&bCurrent: &a" + round,
                    "&6Left-Click: &eToggle",
                    "&6Right-Click: &eSet to default value")) {
                @Override
                public void onLeftClick() {
                    itemGenerator.getConfig().set(ItemType.ROUND.getPath(path), !round);
                    saveAndReopen();
                }

                @Override
                public void onRightClick() {
                    itemGenerator.getConfig().set(ItemType.ROUND.getPath(path), false);
                    saveAndReopen();
                }
            });
        }
    }

    public enum ItemType {
        CHANCE("chance"),
        SCALE_BY_LEVEL("scale-by-level"),
        MIN("min"),
        MAX("max"),
        FLAT_RANGE("flat-range"),
        ROUND("round"),
        ;

        private final String path;

        ItemType(String path) {this.path = path;}

        public String getPath(String rootPath) {return rootPath + '.' + path;}

        public String getTitle() {return this.name().replace('_', ' ').toLowerCase();}
    }
}
