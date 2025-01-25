package studio.magemonkey.divinity.modules.list.itemgenerator.editor.bonuses;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.hooks.EHook;
import studio.magemonkey.divinity.hooks.external.FabledHook;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.*;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewBonusStatGUI extends AbstractEditorGUI {
    private final String                    path;
    private final BonusCategoryGUI.ItemType statType;

    public NewBonusStatGUI(Player player,
                           ItemGeneratorReference itemGenerator,
                           String path,
                           BonusCategoryGUI.ItemType statType) {
        super(player,
                6,
                "Editor/" + EditorGUI.ItemType.MATERIALS.getTitle(),
                itemGenerator);
        this.path = path;
        this.statType = statType;
    }

    @Override
    public void setContents() {
        Material             material;
        List<String>         list         = new ArrayList<>();
        ConfigurationSection section      = itemGenerator.getConfig().getConfigurationSection(getPath());
        Set<String>          existingKeys = section == null ? new HashSet<>() : section.getKeys(false);
        switch (this.statType) {
            case DAMAGE: {
                material = Material.IRON_SWORD;
                for (DamageAttribute damageAttribute : ItemStats.getDamages()) {
                    boolean exists = false;
                    for (String existingKey : existingKeys) {
                        if (existingKey.equalsIgnoreCase(damageAttribute.getId())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        list.add(damageAttribute.getId());
                    }
                }
                break;
            }
            case DEFENSE: {
                material = Material.IRON_CHESTPLATE;
                for (DefenseAttribute defenseAttribute : ItemStats.getDefenses()) {
                    boolean exists = false;
                    for (String existingKey : existingKeys) {
                        if (existingKey.equalsIgnoreCase(defenseAttribute.getId())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        list.add(defenseAttribute.getId());
                    }
                }
                break;
            }
            case ITEM_STAT: {
                material = Material.OAK_SIGN;
                for (SimpleStat.Type itemStat : TypedStat.Type.values()) {
                    boolean exists = false;
                    for (String existingKey : existingKeys) {
                        if (existingKey.equalsIgnoreCase(itemStat.name())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        list.add(itemStat.name());
                    }
                }
                break;
            }
            case FABLED_ATTRIBUTE: {
                material = Material.BOOK;
                FabledHook fabledHook = (FabledHook) Divinity.getInstance().getHook(EHook.SKILL_API);
                if (fabledHook != null) {
                    for (FabledAttribute fabledAttribute : fabledHook.getAttributes()) {
                        boolean exists = false;
                        for (String existingKey : existingKeys) {
                            if (existingKey.equalsIgnoreCase(fabledAttribute.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            list.add(fabledAttribute.getId());
                        }
                    }
                }
                break;
            }
            case HAND: {
                material = Material.STICK;
                for (HandAttribute handAttribute : ItemStats.getHands()) {
                    boolean exists = false;
                    for (String existingKey : existingKeys) {
                        if (existingKey.equalsIgnoreCase(handAttribute.getId())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        list.add(handAttribute.getId());
                    }
                }
                break;
            }
            case AMMO: {
                material = Material.ARROW;
                for (AmmoAttribute ammoAttribute : ItemStats.getAmmos()) {
                    boolean exists = false;
                    for (String existingKey : existingKeys) {
                        if (existingKey.equalsIgnoreCase(ammoAttribute.getId())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        list.add(ammoAttribute.getId());
                    }
                }
                break;
            }
            default: {
                material = Material.STONE;
                break;
            }
        }
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
            setSlot(i, new Slot(createItem(material,
                    "&e" + stat,
                    "&6Left-Click: &eCreate")) {
                @Override
                public void onLeftClick() {
                    sendSetMessage(stat + ' ' + statType.name().replace('_', ' ').toLowerCase() + " value",
                            null,
                            s -> {
                                String[] split = s.split("%", 2);
                                if (split.length == 2 && !split[1].isEmpty()) throw new IllegalArgumentException();
                                Double.parseDouble(split[0]);
                                itemGenerator.getConfig().set(getPath() + '.' + stat, s);
                                saveAndReopen();
                                close(2);
                            });
                }
            });
        }
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }

    private String getPath() {
        return path + '.' + this.statType.getPath();
    }
}
