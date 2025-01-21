package studio.magemonkey.divinity.modules.list.itemgenerator.editor.trimmings;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

public class TrimmingMaterialGUI extends AbstractEditorGUI {
    private final TrimmingListGUI.TrimmingEntry entry;

    public TrimmingMaterialGUI(Player player,
                               ItemGeneratorReference itemGenerator,
                               TrimmingListGUI.TrimmingEntry entry) {
        super(player,
                6,
                "Editor/" + EditorGUI.ItemType.ARMOR_TRIMINGS.getTitle(),
                itemGenerator);
        this.entry = entry;
    }

    @Override
    public void setContents() {
        int i = 0;
        for (TrimMaterial material : Registry.TRIM_MATERIAL) {
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
            if (i == 1) {
                setSlot(i, new Slot(createItem(Material.CRAFTING_TABLE,
                        "&eAny")) {
                    @Override
                    public void onLeftClick() {
                        itemGenerator.getConfig().remove(TrimmingListGUI.getPath(entry.getArmorTrim()));
                        entry.setArmorTrim(new TrimmingListGUI.ArmorTrim(null, entry.getArmorTrim().getPattern()));
                        itemGenerator.getConfig().set(TrimmingListGUI.getPath(entry.getArmorTrim()), entry.getWeight());
                        saveAndReopen();
                        close();
                    }
                });
                i++;
            }
            String name = material.getKey().getKey();
            setSlot(i, new Slot(createItem(TrimmingGUI.fromMaterial(material),
                    "&e" + name.substring(0, 1).toUpperCase() + name.substring(1))) {
                @Override
                public void onLeftClick() {
                    itemGenerator.getConfig().remove(TrimmingListGUI.getPath(entry.getArmorTrim()));
                    entry.setArmorTrim(new TrimmingListGUI.ArmorTrim(material, entry.getArmorTrim().getPattern()));
                    itemGenerator.getConfig().set(TrimmingListGUI.getPath(entry.getArmorTrim()), entry.getWeight());
                    saveAndReopen();
                    close();
                }
            });
        }
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }
}
