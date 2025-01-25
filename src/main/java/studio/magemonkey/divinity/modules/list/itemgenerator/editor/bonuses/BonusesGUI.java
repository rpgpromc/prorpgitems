package studio.magemonkey.divinity.modules.list.itemgenerator.editor.bonuses;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BonusesGUI extends AbstractEditorGUI {
    private final String path;

    public BonusesGUI(Player player, ItemGeneratorReference itemGenerator, String path) {
        super(player,
                6,
                "Editor/" + EditorGUI.ItemType.BONUSES.getTitle(),
                itemGenerator);
        this.path = path;
    }

    @Override
    public void setContents() {
        JYML                 cfg                  = this.itemGenerator.getConfig();
        Map<String, String>  map                  = new HashMap<>();
        List<String>         list                 = new ArrayList<>();
        ConfigurationSection configurationSection = cfg.getConfigurationSection(this.path);
        if (configurationSection != null) {
            for (String path : new String[]{
                    BonusCategoryGUI.ItemType.DAMAGE.getPath(),
                    BonusCategoryGUI.ItemType.DEFENSE.getPath(),
                    BonusCategoryGUI.ItemType.ITEM_STAT.getPath(),
                    BonusCategoryGUI.ItemType.FABLED_ATTRIBUTE.getPath(),
                    BonusCategoryGUI.ItemType.AMMO.getPath(),
                    BonusCategoryGUI.ItemType.HAND.getPath()}) {
                ConfigurationSection section = configurationSection.getConfigurationSection(path);
                if (section != null) {
                    for (String id : section.getKeys(false)) {
                        String path2 = path + '.' + id;
                        map.put(path2, section.getString(id));
                        list.add(path2);
                    }
                }
            }
        }
        list.add(null);
        int i = 0;
        for (String stat : list) {
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
            if (stat == null) {
                setSlot(i, new Slot(createItem(Material.REDSTONE, "&eAdd new stat bonus")) {
                    @Override
                    public void onLeftClick() {
                        openSubMenu(new BonusStatTypeGUI(player, itemGenerator, path));
                    }
                });
            } else {
                Material material;
                String   id;
                String   value = map.get(stat);
                if (stat.startsWith(BonusCategoryGUI.ItemType.DAMAGE.getPath())) {
                    material = Material.IRON_SWORD;
                    id = stat.substring(BonusCategoryGUI.ItemType.DAMAGE.getPath().length() + 1) + " damage";
                } else if (stat.startsWith(BonusCategoryGUI.ItemType.DEFENSE.getPath())) {
                    material = Material.IRON_CHESTPLATE;
                    id = stat.substring(BonusCategoryGUI.ItemType.DEFENSE.getPath().length() + 1) + " defense";
                } else if (stat.startsWith(BonusCategoryGUI.ItemType.ITEM_STAT.getPath())) {
                    material = Material.OAK_SIGN;
                    id = stat.substring(BonusCategoryGUI.ItemType.ITEM_STAT.getPath().length() + 1) + " stat";
                } else if (stat.startsWith(BonusCategoryGUI.ItemType.FABLED_ATTRIBUTE.getPath())) {
                    material = Material.BOOK;
                    id = stat.substring(BonusCategoryGUI.ItemType.FABLED_ATTRIBUTE.getPath().length() + 1)
                            + " Fabled attribute";
                } else if (stat.startsWith(BonusCategoryGUI.ItemType.AMMO.getPath())) {
                    material = Material.ARROW;
                    id = stat.substring(BonusCategoryGUI.ItemType.AMMO.getPath().length() + 1) + " ammo";
                } else if (stat.startsWith(BonusCategoryGUI.ItemType.HAND.getPath())) {
                    material = Material.STICK;
                    id = stat.substring(BonusCategoryGUI.ItemType.HAND.getPath().length() + 1) + " hand";
                } else {
                    material = Material.OAK_SIGN;
                    id = stat;
                }
                String path = this.path + '.' + stat;
                setSlot(i, new Slot(createItem(material,
                        "&e" + id,
                        "&bCurrent: &a" + value,
                        "&6Left-Click: &eSet",
                        "&6Right-Click: &eRemove")) {
                    @Override
                    public void onLeftClick() {
                        sendSetMessage(id + " value",
                                itemGenerator.getConfig().getString(path),
                                s -> {
                                    String[] split = s.split("%", 2);
                                    if (split.length == 2 && !split[1].isEmpty()) throw new IllegalArgumentException();
                                    Double.parseDouble(split[0]);
                                    cfg.set(path, s);
                                    saveAndReopen();
                                });
                    }

                    @Override
                    public void onRightClick() {
                        cfg.remove(path);
                        saveAndReopen();
                    }
                });
            }
        }
        list.remove(list.size() - 1);
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }
}
