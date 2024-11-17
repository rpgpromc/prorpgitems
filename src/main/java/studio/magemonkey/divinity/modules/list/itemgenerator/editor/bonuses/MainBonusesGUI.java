package studio.magemonkey.divinity.modules.list.itemgenerator.editor.bonuses;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

public class MainBonusesGUI extends AbstractEditorGUI {

    public MainBonusesGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                1,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.BONUSES.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        setSlot(0, new Slot(createItem(Material.IRON_ORE,
                "&eMaterial Modifiers",
                "&7Modify the base value of the stat, based on the material group of the item",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new BonusCategoryGUI(player, itemGenerator, ItemType.MATERIAL_MODIFIERS));
            }
        });
        setSlot(1, new Slot(createItem(Material.IRON_INGOT,
                "&eMaterial bonuses",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new BonusCategoryGUI(player, itemGenerator, ItemType.MATERIAL));
            }
        });
        setSlot(2, new Slot(createItem(Material.JACK_O_LANTERN,
                "&eClass bonuses",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new BonusCategoryGUI(player, itemGenerator, ItemType.CLASS));
            }
        });
    }

    public enum ItemType {
        MATERIAL_MODIFIERS("material or group"),
        MATERIAL("material or group"),
        CLASS("class"),
        ;

        private final String description;

        ItemType(String description) {
            this.description = description;
        }

        public String getPath() {return "generator.bonuses." + name().toLowerCase().replace('_', '-');}

        public String getDescription() {return description;}
    }
}
