package studio.magemonkey.divinity.modules.list.itemgenerator.editor.materials;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainModelDataGUI extends AbstractEditorGUI {

    public MainModelDataGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                6,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.MATERIALS.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        JYML                       cfg  = this.itemGenerator.getConfig();
        Map<String, List<Integer>> map  = new HashMap<>();
        List<String>               list = new ArrayList<>();
        map.put("default", cfg.getIntegerList(MainMaterialsGUI.ItemType.MODEL_DATA.getPath() + ".default"));
        list.add("default");
        ConfigurationSection modelDataCfg =
                cfg.getConfigurationSection(MainMaterialsGUI.ItemType.MODEL_DATA.getPath() + ".special");
        if (modelDataCfg != null) {
            for (String key : modelDataCfg.getKeys(false)) {
                String path = "special." + key;
                map.put(path, modelDataCfg.getIntegerList(key));
                list.add(path);
            }
        }
        list.add(null);
        int i = 0;
        for (String path : list) {
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
            if (path == null) {
                setSlot(i, new Slot(createItem(Material.REDSTONE, "&eAdd new material")) {
                    @Override
                    public void onLeftClick() {
                        sendSetMessage("material for the model data",
                                null,
                                s -> {
                                    cfg.set(MainMaterialsGUI.ItemType.MODEL_DATA.getPath() + ".special." + s,
                                            List.of());
                                    saveAndReopen();
                                });
                    }
                });
            } else if (path.equals("default")) {
                List<String> stringList = new ArrayList<>();
                for (int cmd : map.get(path)) {
                    stringList.add(String.valueOf(cmd));
                }
                setSlot(i, new Slot(createItem(Material.STONE,
                        "&e" + path, StringUT.replace(CURRENT_PLACEHOLDER, stringList,
                                "&bCurrent:",
                                "&a%current%",
                                "&6Left-Click: &eModify"))) {
                    @Override
                    public void onLeftClick() {
                        openSubMenu(new ModelDataGUI(player, itemGenerator, path));
                    }
                });
            } else {
                String       id         = path.replace("special.", "");
                List<String> stringList = new ArrayList<>();
                for (int cmd : map.get(path)) {
                    stringList.add(String.valueOf(cmd));
                }
                setSlot(i, new Slot(createItem(MainMaterialsGUI.getMaterialGroup(id),
                        "&e" + id, StringUT.replace(CURRENT_PLACEHOLDER, stringList,
                                "&bCurrent:",
                                "&a%current%",
                                "&6Left-Click: &eModify",
                                "&6Right-Click: &eRemove"))) {
                    @Override
                    public void onLeftClick() {
                        openSubMenu(new ModelDataGUI(player, itemGenerator, path));
                    }

                    @Override
                    public void onRightClick() {
                        map.remove(path);
                        cfg.remove(MainMaterialsGUI.ItemType.MODEL_DATA.getPath());
                        for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
                            cfg.set(MainMaterialsGUI.ItemType.MODEL_DATA.getPath() + '.' + entry.getKey(),
                                    entry.getValue());
                        }
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
