package studio.magemonkey.divinity.modules.list.itemgenerator.editor;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.Divinity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class UsesByLevelGUI extends AbstractEditorGUI {

    public UsesByLevelGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                6,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.USES_BY_LEVEL.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        Map<Integer, Integer> map  = getUsesByLevel(this.itemGenerator.getConfig());
        List<Integer>         list = new ArrayList<>(map.keySet());
        list.add(null);
        int i = 0;
        for (Integer level : list) {
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

            setSlot(i, level == null ?
                    new Slot(createItem(Material.REDSTONE, "&eAdd new level")) {
                        @Override
                        public void onLeftClick() {
                            sendSetMessage("level",
                                    null,
                                    s -> {
                                        int level = Integer.parseInt(s);
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                sendSetMessage(
                                                        EditorGUI.ItemType.USES_BY_LEVEL.getTitle() + " " + level,
                                                        String.valueOf(map.get(level)),
                                                        s1 -> {
                                                            map.put(level, Integer.parseInt(s1));
                                                            setUsesByLevel(map);
                                                            saveAndReopen();
                                                        });
                                            }
                                        }.runTask(Divinity.getInstance());
                                    });
                        }
                    } :
                    new Slot(createItem(Material.CAULDRON,
                            "&e" + level,
                            "&bCurrent: &a" + map.get(level),
                            "&6Left-Click: &eSet",
                            "&6Right-Click: &eRemove")) {
                        @Override
                        public void onLeftClick() {
                            sendSetMessage(EditorGUI.ItemType.USES_BY_LEVEL.getTitle() + " " + level,
                                    String.valueOf(map.get(level)),
                                    s -> {
                                        map.put(level, Integer.parseInt(s));
                                        setUsesByLevel(map);
                                        saveAndReopen();
                                    });
                        }

                        @Override
                        public void onRightClick() {
                            map.remove(level);
                            setUsesByLevel(map);
                            saveAndReopen();
                        }
                    }
            );
        }
        list.remove(list.size() - 1);
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }

    public static Map<Integer, Integer> getUsesByLevel(JYML cfg) {
        Map<Integer, Integer> map = new TreeMap<>();
        ConfigurationSection configurationSection =
                cfg.getConfigurationSection(EditorGUI.ItemType.USES_BY_LEVEL.getPath());
        if (configurationSection != null) {
            for (String key : configurationSection.getKeys(false)) {
                try {
                    map.put(Integer.parseInt(key), configurationSection.getInt(key));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return map;
    }

    protected void setUsesByLevel(Map<Integer, Integer> usesByLevel) {
        JYML   cfg  = this.itemGenerator.getConfig();
        String path = EditorGUI.ItemType.USES_BY_LEVEL.getPath();
        cfg.remove(EditorGUI.ItemType.USES_BY_LEVEL.getPath());
        path = path + '.';
        for (Map.Entry<Integer, Integer> entry : usesByLevel.entrySet()) {
            cfg.set(path + entry.getKey(), entry.getValue());
        }
    }
}
