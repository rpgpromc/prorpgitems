package studio.magemonkey.divinity.modules.list.itemgenerator.editor.requirements;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RequirementsGUI extends AbstractEditorGUI {
    private final String   path;
    private final Material material;

    public RequirementsGUI(Player player, ItemGeneratorReference itemGenerator, String path, Material material) {
        super(player,
                6,
                "Editor/" + EditorGUI.ItemType.REQUIREMENTS.getTitle(),
                itemGenerator);
        this.path = path;
        this.material = material;
    }

    @Override
    public void setContents() {
        TreeMap<Integer, String> requirements = getRequirements();
        List<Integer>            levels       = new ArrayList<>(requirements.keySet());
        levels.add(null);
        int i = 0;
        for (Integer level : levels) {
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
                    new Slot(createItem(Material.REDSTONE,
                            "&eAdd new requirement")) {
                        @Override
                        public void onLeftClick() {
                            sendSetMessage("level for the new requirement",
                                    null,
                                    s -> {
                                        int newLevel = Integer.parseInt(s);
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                sendSetMessage("requirement for level " + newLevel,
                                                        null,
                                                        s1 -> {
                                                            TreeMap<Integer, String> requirements = getRequirements();
                                                            requirements.put(newLevel, s1);
                                                            setRequirements(requirements);
                                                            saveAndReopen();
                                                        });
                                            }
                                        }.runTask(Divinity.getInstance());
                                    });
                        }
                    } :
                    new Slot(createItem(material,
                            "&e" + level,
                            "&bCurrent: &a" + requirements.get(level),
                            "&6Left-Click: &eSet",
                            "&6Right-Click: &eRemove")) {
                        @Override
                        public void onLeftClick() {
                            sendSetMessage("requirement for level " + level,
                                    null,
                                    s -> {
                                        TreeMap<Integer, String> requirements = getRequirements();
                                        requirements.put(level, s);
                                        setRequirements(requirements);
                                        saveAndReopen();
                                    });
                        }

                        @Override
                        public void onRightClick() {
                            TreeMap<Integer, String> requirements = getRequirements();
                            requirements.remove(level);
                            setRequirements(requirements);
                            saveAndReopen();
                        }
                    });
        }
        levels.remove(levels.size() - 1);
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }

    protected TreeMap<Integer, String> getRequirements() {
        ConfigurationSection requirementsSection =
                this.itemGenerator.getConfig().getConfigurationSection(this.path);
        TreeMap<Integer, String> requirements = new TreeMap<>();
        if (requirementsSection != null) {
            for (String key : requirementsSection.getKeys(false)) {
                int itemLvl = StringUT.getInteger(key, -1);
                if (itemLvl <= 0) {
                    continue;
                }

                String requirement = requirementsSection.getString(key);
                if (requirement == null || requirement.isEmpty()) {
                    continue;
                }

                requirements.put(itemLvl, requirement);
            }
        }
        return requirements;
    }

    protected void setRequirements(TreeMap<Integer, String> requirements) {
        itemGenerator.getConfig().remove(this.path);
        for (Map.Entry<Integer, String> entry : requirements.entrySet()) {
            itemGenerator.getConfig().set(this.path + '.' + entry.getKey(), entry.getValue());
        }
    }
}
