package studio.magemonkey.divinity.modules.list.itemgenerator.editor.trimmings;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

public class TrimmingGUI extends AbstractEditorGUI {
    private final TrimmingListGUI.TrimmingEntry entry;

    public TrimmingGUI(Player player, ItemGeneratorReference itemGenerator, TrimmingListGUI.TrimmingEntry entry) {
        super(player,
                1,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.ARMOR_TRIMINGS.getTitle(),
                itemGenerator);
        this.entry = entry;
    }

    @Override
    public void setContents() {
        String name = entry.getArmorTrim().getMaterial() == null ? "Any"
                : entry.getArmorTrim().getMaterial().getKey().getKey();
        setSlot(0, new Slot(createItem(fromMaterial(entry.getArmorTrim().getMaterial()),
                "&eTrim Material",
                "&bCurrent: &a" + name.substring(0, 1).toUpperCase() + name.substring(1),
                "&eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new TrimmingMaterialGUI(player, itemGenerator, entry));
            }
        });
        name = entry.getArmorTrim().getPattern() == null ? "Any" : entry.getArmorTrim().getPattern().getKey().getKey();
        setSlot(1, new Slot(createItem(fromPattern(entry.getArmorTrim().getPattern()),
                "&eTrim Pattern",
                "&bCurrent: &a" + name.substring(0, 1).toUpperCase() + name.substring(1),
                "&eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new TrimmingPatternsGUI(player, itemGenerator, entry));
            }
        });
        setSlot(2, new Slot(createItem(Material.DROPPER,
                "&eWeight",
                "&bCurrent: &a" + entry.getWeight(),
                "&eSet")) {
            @Override
            public void onLeftClick() {
                sendSetMessage("weight for this trim",
                        String.valueOf(entry.getWeight()),
                        s -> {
                            entry.setWeight(Double.parseDouble(s));
                            itemGenerator.getConfig()
                                    .set(TrimmingListGUI.getPath(entry.getArmorTrim()), entry.getWeight());
                            saveAndReopen();
                        });
            }
        });
    }

    public static Material fromMaterial(TrimMaterial trimMaterial) {
        if (trimMaterial == TrimMaterial.QUARTZ) {
            return Material.QUARTZ;
        } else if (trimMaterial == TrimMaterial.IRON) {
            return Material.IRON_INGOT;
        } else if (trimMaterial == TrimMaterial.NETHERITE) {
            return Material.NETHERITE_INGOT;
        } else if (trimMaterial == TrimMaterial.REDSTONE) {
            return Material.REDSTONE;
        } else if (trimMaterial == TrimMaterial.COPPER) {
            return Material.COPPER_INGOT;
        } else if (trimMaterial == TrimMaterial.GOLD) {
            return Material.GOLD_INGOT;
        } else if (trimMaterial == TrimMaterial.EMERALD) {
            return Material.EMERALD;
        } else if (trimMaterial == TrimMaterial.DIAMOND) {
            return Material.DIAMOND;
        } else if (trimMaterial == TrimMaterial.LAPIS) {
            return Material.LAPIS_LAZULI;
        } else if (trimMaterial == TrimMaterial.AMETHYST) {
            return Material.AMETHYST_SHARD;
        } else if (trimMaterial == null) {
            return Material.CRAFTING_TABLE;
        }
        return Material.STONE;
    }

    public static Material fromPattern(TrimPattern trimPattern) {
        if (trimPattern == null) {
            return Material.CRAFTING_TABLE;
        }
        try {
            return Material.valueOf(trimPattern.getKey().getKey().toUpperCase()
                    + Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE.name().substring("sentry".length()));
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }
}