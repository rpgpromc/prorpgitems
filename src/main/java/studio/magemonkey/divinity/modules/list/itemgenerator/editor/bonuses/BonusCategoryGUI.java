package studio.magemonkey.divinity.modules.list.itemgenerator.editor.bonuses;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.hooks.EHook;
import studio.magemonkey.divinity.hooks.external.FabledHook;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.materials.MainMaterialsGUI;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.DamageAttribute;
import studio.magemonkey.divinity.stats.items.attributes.DefenseAttribute;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;
import studio.magemonkey.fabled.Fabled;
import studio.magemonkey.fabled.manager.FabledAttribute;

import java.util.*;
import java.util.function.Function;

public class BonusCategoryGUI extends AbstractEditorGUI {
    private final MainBonusesGUI.ItemType category;

    public BonusCategoryGUI(Player player, ItemGeneratorReference itemGenerator, MainBonusesGUI.ItemType category) {
        super(player,
                6,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.BONUSES.getTitle(),
                itemGenerator);
        this.category = category;
    }

    private void loadLore(ConfigurationSection section, List<String> lore, Function<String, String> function) {
        for (String statId : section.getKeys(false)) {
            lore.add(StringUT.color(" " + function.apply(statId)
                    .replace("%value%", Objects.requireNonNull(section.getString(statId)))));
        }
    }

    @Override
    public void setContents() {
        JYML                      cfg            = this.itemGenerator.getConfig();
        Map<String, List<String>> map            = new HashMap<>();
        List<String>              list           = new ArrayList<>();
        ConfigurationSection      bonusesSection = cfg.getConfigurationSection(this.category.getPath());
        if (bonusesSection != null) {
            for (String key : bonusesSection.getKeys(false)) {
                List<String> lore = new ArrayList<>();
                ConfigurationSection section =
                        bonusesSection.getConfigurationSection(key + '.' + ItemType.DAMAGE.getPath());
                if (section != null && !section.getKeys(false).isEmpty()) {
                    lore.add("&2Damage types:");
                    loadLore(section, lore, s -> {
                        DamageAttribute damageAttribute = ItemStats.getDamageById(s);
                        return damageAttribute == null ? s + ": %value%" : damageAttribute.getFormat();
                    });
                }
                section = bonusesSection.getConfigurationSection(key + '.' + ItemType.DEFENSE.getPath());
                if (section != null) {
                    Set<String> keys = section.getKeys(false);
                    if (!keys.isEmpty()) {
                        lore.add("&2Defense types:");
                        loadLore(section, lore, s -> {
                            DefenseAttribute defenseAttribute = ItemStats.getDefenseById(s);
                            return defenseAttribute == null ? s + ": %value%" : defenseAttribute.getFormat();
                        });
                    }
                }
                section = bonusesSection.getConfigurationSection(key + '.' + ItemType.ITEM_STAT.getPath());
                if (section != null) {
                    Set<String> keys = section.getKeys(false);
                    if (!keys.isEmpty()) {
                        lore.add("&2Item Stats:");
                        loadLore(section, lore, s -> {
                            TypedStat.Type type = TypedStat.Type.getByName(s);
                            if (type != null) {
                                TypedStat stat = ItemStats.getStat(type);
                                if (stat instanceof SimpleStat) return ((SimpleStat) stat).getFormat();
                            }
                            return "&f" + s + ": &6%value%";
                        });
                    }
                }

                // Only permanent bonuses should handle these (i.e. class bonuses are applied dynamically)
                switch (this.category) {
                    case MATERIAL_MODIFIERS:
                    case MATERIAL: {
                        section =
                                bonusesSection.getConfigurationSection(key + '.' + ItemType.FABLED_ATTRIBUTE.getPath());
                        if (section != null) {
                            Set<String> keys = section.getKeys(false);
                            if (!keys.isEmpty()) {
                                lore.add("&2Fabled Attributes:");
                                loadLore(section, lore, s -> {
                                    FabledHook fabledHook =
                                            (FabledHook) Divinity.getInstance().getHook(EHook.SKILL_API);
                                    if (fabledHook != null) {
                                        FabledAttribute proAttribute = Fabled.getAttributeManager().getAttribute(s);
                                        if (proAttribute != null) return proAttribute.getName() + ": &6%value%";
                                    }
                                    return "&f" + s + ": &6%value%";
                                });
                            }
                        }
                        section = bonusesSection.getConfigurationSection(key + '.' + ItemType.AMMO.getPath());
                        if (section != null) {
                            Set<String> keys = section.getKeys(false);
                            if (!keys.isEmpty()) {
                                lore.add("&2Ammo:");
                                loadLore(section, lore, s -> "&f" + s + ": &6%value%");
                            }
                        }
                        section = bonusesSection.getConfigurationSection(key + '.' + ItemType.HAND.getPath());
                        if (section != null) {
                            Set<String> keys = section.getKeys(false);
                            if (!keys.isEmpty()) {
                                lore.add("&2Hands:");
                                loadLore(section, lore, s -> "&f" + s + ": &6%value%");
                            }
                        }
                        break;
                    }
                }
                map.put(key, lore);
                list.add(key);
            }
        }
        list.add(null);
        int i = 0;
        for (String group : list) {
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
            setSlot(i, group == null ?
                    new Slot(createItem(Material.REDSTONE, "&eAdd new " + this.category.getDescription())) {
                        @Override
                        public void onLeftClick() {
                            sendSetMessage(BonusCategoryGUI.this.category.getDescription(),
                                    null,
                                    s -> {
                                        String path = BonusCategoryGUI.this.category.getPath() + '.' + s;
                                        cfg.set(path, cfg.createSection(path));
                                        saveAndReopen();
                                    });
                        }
                    } :
                    new Slot(createItem(MainMaterialsGUI.getMaterialGroup(group),
                            "&e" + group, StringUT.replace(CURRENT_PLACEHOLDER, map.get(group),
                                    "&bCurrent:",
                                    "&a%current%",
                                    "&6Left-Click: &eModify",
                                    "&6Right-Click: &eRemove"))) {
                        @Override
                        public void onLeftClick() {
                            openSubMenu(new BonusesGUI(player,
                                    itemGenerator,
                                    BonusCategoryGUI.this.category.getPath() + '.' + group));
                        }

                        @Override
                        public void onRightClick() {
                            cfg.remove(BonusCategoryGUI.this.category.getPath() + '.' + group);
                            saveAndReopen();
                        }
                    });
        }
        list.remove(list.size() - 1);
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }

    public enum ItemType {
        DAMAGE("damage-types"),
        DEFENSE("defense-types"),
        ITEM_STAT("item-stats"),
        FABLED_ATTRIBUTE("fabled-attributes"),
        HAND("hands"),
        AMMO("ammo");

        private final String path;

        ItemType(String path) {this.path = path;}

        public String getPath() {return path;}
    }
}
