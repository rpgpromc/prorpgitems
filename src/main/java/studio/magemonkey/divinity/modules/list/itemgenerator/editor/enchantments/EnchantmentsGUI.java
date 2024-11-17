package studio.magemonkey.divinity.modules.list.itemgenerator.editor.enchantments;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

public class EnchantmentsGUI extends AbstractEditorGUI {

    public EnchantmentsGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                1,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.ENCHANTMENTS.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        setSlot(0, new Slot(createItem(Material.BROWN_MUSHROOM,
                "&eMinimum enchantments",
                "&bCurrent: &a" + this.itemGenerator.getHandle().getMinEnchantments(),
                "&6Left-Click: &eSet",
                "&6Shift-Left-Click: &eDecrease",
                "&6Shift-Right-Click: &eIncrease",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onShiftLeftClick() {
                itemGenerator.getConfig()
                        .set(ItemType.MINIMUM.getPath(EditorGUI.ItemType.ENCHANTMENTS.getPath()),
                                Math.max(0, itemGenerator.getHandle().getMinEnchantments() - 1));
                saveAndReopen();
            }

            @Override
            public void onShiftRightClick() {
                itemGenerator.getConfig()
                        .set(ItemType.MINIMUM.getPath(EditorGUI.ItemType.ENCHANTMENTS.getPath()),
                                Math.max(0, itemGenerator.getHandle().getMinEnchantments() + 1));
                saveAndReopen();
            }

            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.MINIMUM.name().toLowerCase() + " enchantments",
                        String.valueOf(itemGenerator.getHandle().getMinEnchantments()),
                        s -> {
                            itemGenerator.getConfig()
                                    .set(ItemType.MINIMUM.getPath(EditorGUI.ItemType.ENCHANTMENTS.getPath()),
                                            Integer.parseInt(s));
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.MINIMUM.getPath(EditorGUI.ItemType.ENCHANTMENTS.getPath()));
                saveAndReopen();
            }
        });
        setSlot(1, new Slot(createItem(Material.RED_MUSHROOM,
                "&eMaximum enchantments",
                "&bCurrent: &a" + this.itemGenerator.getHandle().getMaxEnchantments(),
                "&6Left-Click: &eSet",
                "&6Shift-Left-Click: &eDecrease",
                "&6Shift-Right-Click: &eIncrease",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onShiftLeftClick() {
                itemGenerator.getConfig()
                        .set(ItemType.MAXIMUM.getPath(EditorGUI.ItemType.ENCHANTMENTS.getPath()),
                                Math.max(0, itemGenerator.getHandle().getMaxEnchantments() - 1));
                saveAndReopen();
            }

            @Override
            public void onShiftRightClick() {
                itemGenerator.getConfig()
                        .set(ItemType.MAXIMUM.getPath(EditorGUI.ItemType.ENCHANTMENTS.getPath()),
                                Math.max(0, itemGenerator.getHandle().getMaxEnchantments() + 1));
                saveAndReopen();
            }

            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.MAXIMUM.name().toLowerCase() + " enchantments",
                        String.valueOf(itemGenerator.getHandle().getMaxEnchantments()),
                        s -> {
                            itemGenerator.getConfig()
                                    .set(ItemType.MAXIMUM.getPath(EditorGUI.ItemType.ENCHANTMENTS.getPath()),
                                            Integer.parseInt(s));
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.MAXIMUM.getPath(EditorGUI.ItemType.ENCHANTMENTS.getPath()));
                saveAndReopen();
            }
        });
        setSlot(2, new Slot(createItem(Material.BOW,
                "&eSafe enchantments only",
                "&bCurrent: &a" + this.itemGenerator.getHandle().isSafeEnchant(),
                "&6Left-Click: &eToggle",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onLeftClick() {
                itemGenerator.getConfig()
                        .set(ItemType.SAFE_ONLY.getPath(EditorGUI.ItemType.ENCHANTMENTS.getPath()),
                                !itemGenerator.getHandle().isSafeEnchant());
                saveAndReopen();
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.SAFE_ONLY.getPath(EditorGUI.ItemType.ENCHANTMENTS.getPath()));
                saveAndReopen();
            }
        });
        setSlot(3, new Slot(createItem(Material.EXPERIENCE_BOTTLE,
                "&eSafe enchantment levels only",
                "&bCurrent: &a" + this.itemGenerator.getHandle().isEnchantsSafeLevels(),
                "&6Left-Click: &eToggle",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onLeftClick() {
                itemGenerator.getConfig()
                        .set(ItemType.SAFE_LEVELS.getPath(EditorGUI.ItemType.ENCHANTMENTS.getPath()),
                                !itemGenerator.getHandle().isEnchantsSafeLevels());
                saveAndReopen();
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.SAFE_LEVELS.getPath(EditorGUI.ItemType.ENCHANTMENTS.getPath()));
                saveAndReopen();
            }
        });
        setSlot(4, new Slot(createItem(Material.ENCHANTED_BOOK,
                "&eList of enchantments",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new EnchantmentListGUI(player, itemGenerator));
            }
        });
    }

    public enum ItemType {
        MINIMUM,
        MAXIMUM,
        SAFE_ONLY,
        SAFE_LEVELS,
        LIST,
        ;

        public String getPath(String path) {
            return path + '.' + name().toLowerCase().replace('_', '-');
        }
    }
}
