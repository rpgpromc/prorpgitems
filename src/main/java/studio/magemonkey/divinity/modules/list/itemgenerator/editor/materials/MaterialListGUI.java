package studio.magemonkey.divinity.modules.list.itemgenerator.editor.materials;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

import java.util.ArrayList;
import java.util.List;

public class MaterialListGUI extends AbstractEditorGUI {

    public MaterialListGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                6,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.MATERIALS.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        List<String> materialList =
                new ArrayList<>(itemGenerator.getConfig().getStringList(MainMaterialsGUI.ItemType.LIST.getPath()));
        materialList.add(null);
        int i = 0;
        for (String entry : materialList) {
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
            setSlot(i, entry == null ?
                    new Slot(createItem(Material.REDSTONE, "&eAdd new material")) {
                        @Override
                        public void onLeftClick() {
                            sendSetMessage("material",
                                    null,
                                    s -> {
                                        materialList.add(s);
                                        itemGenerator.getConfig()
                                                .set(MainMaterialsGUI.ItemType.LIST.getPath(), materialList);
                                        saveAndReopen();
                                    });
                        }
                    } :
                    new Slot(createItem(MainMaterialsGUI.getMaterial(entry),
                            "&e" + entry,
                            "&eRemove")) {
                        @Override
                        public void onLeftClick() {
                            materialList.remove(entry);
                            itemGenerator.getConfig().set(MainMaterialsGUI.ItemType.LIST.getPath(), materialList);
                            saveAndReopen();
                        }
                    });
        }
        materialList.remove(materialList.size() - 1);
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }
}
