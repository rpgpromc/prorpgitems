package studio.magemonkey.divinity.modules.list.itemgenerator.editor.enchantments;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

import java.util.List;

public class NewEnchantmentGUI extends AbstractEditorGUI {
    private final List<String> list;

    public NewEnchantmentGUI(Player player, ItemGeneratorReference itemGenerator, List<String> missingList) {
        super(player,
                6,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.ENCHANTMENTS.getTitle(),
                itemGenerator);
        this.list = missingList;
    }

    @Override
    public void setContents() {
        int i = 0;
        for (String key : list) {
            i++;
            if (i % this.inventory.getSize() == 53) {
                this.setSlot(i, getNextButton());
                i++;
            } else if (i % 9 == 8) {
                i++;
            }
            if (i % this.inventory.getSize() == 45) {
                this.setSlot(i, getPrevButton());
                i++;
            } else if (i % 9 == 0) {
                i++;
            }
            setSlot(i, new Slot(createItem(Material.ENCHANTED_BOOK,
                    "&e" + key,
                    "&6Left-Click: &eSet")) {
                @Override
                public void onLeftClick() {
                    sendSetMessage("level range",
                            null,
                            s -> {
                                String[] strings = s.split(":");
                                if (strings.length > 2) {
                                    throw new IllegalArgumentException();
                                }
                                for (String string : strings) {
                                    Integer.parseInt(string);
                                } // Detect invalid input
                                itemGenerator.getConfig()
                                        .set(EditorGUI.ItemType.ENCHANTMENTS.getPath() + ".list." + key, s);
                                saveAndReopen();
                                close();
                            });
                }
            });
        }
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }
}
