package studio.magemonkey.divinity.modules.list.itemgenerator.editor.skills;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

public class MainSkillsGUI extends AbstractEditorGUI {

    public MainSkillsGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                1,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.SKILLS.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        setSlot(0, new Slot(createItem(Material.BROWN_MUSHROOM,
                "&eMinimum skills",
                "&bCurrent: &a" + itemGenerator.getHandle().getAbilityGenerator().getMinAmount(),
                "&6Left-Click: &eSet",
                "&6Shift-Left-Click: &eDecrease",
                "&6Shift-Right-Click: &eIncrease",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onShiftLeftClick() {
                itemGenerator.getConfig()
                        .set(EditorGUI.ItemType.SKILLS.getPath() + ".minimum",
                                Math.max(0, itemGenerator.getHandle().getAbilityGenerator().getMinAmount() - 1));
                saveAndReopen();
            }

            @Override
            public void onShiftRightClick() {
                itemGenerator.getConfig()
                        .set(EditorGUI.ItemType.SKILLS.getPath() + ".minimum",
                                itemGenerator.getHandle().getAbilityGenerator().getMinAmount() + 1);
                saveAndReopen();
            }

            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.MINIMUM.name() + " skills",
                        String.valueOf(itemGenerator.getHandle().getAbilityGenerator().getMinAmount()),
                        s -> {
                            itemGenerator.getConfig()
                                    .set(EditorGUI.ItemType.SKILLS.getPath() + ".minimum", Integer.parseInt(s));
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                setDefault(EditorGUI.ItemType.SKILLS.getPath() + ".minimum");
                saveAndReopen();
            }
        });
        setSlot(1, new Slot(createItem(Material.RED_MUSHROOM,
                "&eMaximum skills",
                "&bCurrent: &a" + itemGenerator.getHandle().getAbilityGenerator().getMaxAmount(),
                "&6Left-Click: &eSet",
                "&6Shift-Left-Click: &eDecrease",
                "&6Shift-Right-Click: &eIncrease",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onShiftLeftClick() {
                itemGenerator.getConfig()
                        .set(EditorGUI.ItemType.SKILLS.getPath() + ".maximum",
                                Math.max(0, itemGenerator.getHandle().getAbilityGenerator().getMaxAmount() - 1));
                saveAndReopen();
            }

            @Override
            public void onShiftRightClick() {
                itemGenerator.getConfig()
                        .set(EditorGUI.ItemType.SKILLS.getPath() + ".maximum",
                                itemGenerator.getHandle().getAbilityGenerator().getMaxAmount() + 1);
                saveAndReopen();
            }

            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.MAXIMUM.name() + " skills",
                        String.valueOf(itemGenerator.getHandle().getAbilityGenerator().getMaxAmount()),
                        s -> {
                            itemGenerator.getConfig()
                                    .set(EditorGUI.ItemType.SKILLS.getPath() + ".maximum", Integer.parseInt(s));
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                setDefault(EditorGUI.ItemType.SKILLS.getPath() + ".maximum");
                saveAndReopen();
            }
        });
        setSlot(2, new Slot(createItem(Material.FIRE_CHARGE,
                "&eList of skills",
                "&eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new SkillListGUI(player, itemGenerator));
            }
        });
    }

    public enum ItemType {
        MINIMUM,
        MAXIMUM,
        LIST,
    }
}
