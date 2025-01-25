package studio.magemonkey.divinity.modules.list.itemgenerator.editor.bonuses;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

public class BonusStatTypeGUI extends AbstractEditorGUI {
    private final String path;

    public BonusStatTypeGUI(Player player, ItemGeneratorReference itemGenerator, String path) {
        super(player,
                1,
                "Editor/" + EditorGUI.ItemType.BONUSES.getTitle(),
                itemGenerator);
        this.path = path;
    }

    @Override
    public void setContents() {
        setSlot(0, new Slot(createItem(Material.IRON_SWORD, "&eAdd new damage type")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new NewBonusStatGUI(player, itemGenerator, path, BonusCategoryGUI.ItemType.DAMAGE));
            }
        });
        setSlot(1, new Slot(createItem(Material.IRON_CHESTPLATE, "&eAdd new defense type")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new NewBonusStatGUI(player, itemGenerator, path, BonusCategoryGUI.ItemType.DEFENSE));
            }
        });
        setSlot(2, new Slot(createItem(Material.OAK_SIGN, "&eAdd new item stat")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new NewBonusStatGUI(player, itemGenerator, path, BonusCategoryGUI.ItemType.ITEM_STAT));
            }
        });
        if (this.path.startsWith(MainBonusesGUI.ItemType.MATERIAL.getPath())) {
            setSlot(3, new Slot(createItem(Material.BOOK, "&eAdd new FabledAttribute stat")) {
                @Override
                public void onLeftClick() {
                    openSubMenu(new NewBonusStatGUI(player,
                            itemGenerator,
                            path,
                            BonusCategoryGUI.ItemType.FABLED_ATTRIBUTE));
                }
            });
            setSlot(4, new Slot(createItem(Material.ARROW, "&eAdd new ammo stat")) {
                @Override
                public void onLeftClick() {
                    openSubMenu(new NewBonusStatGUI(player, itemGenerator, path, BonusCategoryGUI.ItemType.AMMO));
                }
            });
            setSlot(5, new Slot(createItem(Material.STICK, "&eAdd new hand stat")) {
                @Override
                public void onLeftClick() {
                    openSubMenu(new NewBonusStatGUI(player, itemGenerator, path, BonusCategoryGUI.ItemType.HAND));
                }
            });
        }
    }

}
