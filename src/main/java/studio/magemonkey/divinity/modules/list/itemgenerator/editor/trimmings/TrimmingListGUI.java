package studio.magemonkey.divinity.modules.list.itemgenerator.editor.trimmings;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

import java.util.*;

public class TrimmingListGUI extends AbstractEditorGUI {
    public TrimmingListGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                6,
                "Editor/" + EditorGUI.ItemType.ARMOR_TRIMINGS.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        List<ArmorTrim>        list = new ArrayList<>();
        Map<ArmorTrim, Double> map  = new HashMap<>();
        ConfigurationSection cfg =
                itemGenerator.getConfig().getConfigurationSection(EditorGUI.ItemType.ARMOR_TRIMINGS.getPath());
        if (cfg != null) {
            for (String key : cfg.getKeys(false)) {
                if (key.equals("none")) {
                    map.put(null, cfg.getDouble(key));
                    continue;
                }
                String[] split = key.toLowerCase().split(":");
                if (split.length != 2) {
                    continue;
                }
                TrimMaterial trimMaterial;
                if (split[0].equals("*")) {
                    trimMaterial = null;
                } else {
                    trimMaterial = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(split[0]));
                    if (trimMaterial == null) {
                        continue;
                    }
                }
                TrimPattern trimPattern;
                if (split[1].equals("*")) {
                    trimPattern = null;
                } else {
                    trimPattern = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(split[1]));
                    if (trimPattern == null) {
                        continue;
                    }
                }
                ArmorTrim armorTrim = new ArmorTrim(trimMaterial, trimPattern);
                map.put(armorTrim, cfg.getDouble(key));
                list.add(armorTrim);
            }
        }
        ArmorTrim next = null;
        outer:
        for (TrimMaterial trimMaterial : Registry.TRIM_MATERIAL) { //
            for (TrimPattern trimPattern : Registry.TRIM_PATTERN) {
                ArmorTrim armorTrim = new ArmorTrim(trimMaterial, trimPattern);
                if (!map.containsKey(armorTrim)) {
                    next = armorTrim;
                    break outer;
                }
            }
        }
        if (next == null) {
            for (TrimMaterial trimMaterial : Registry.TRIM_MATERIAL) {
                ArmorTrim armorTrim = new ArmorTrim(trimMaterial, null);
                if (!map.containsKey(armorTrim)) {
                    next = armorTrim;
                    break;
                }
            }
        }
        if (next == null) {
            for (TrimPattern trimPattern : Registry.TRIM_PATTERN) {
                ArmorTrim armorTrim = new ArmorTrim(null, trimPattern);
                if (!map.containsKey(armorTrim)) {
                    next = armorTrim;
                    break;
                }
            }
        }

        if (next == null) {
            ArmorTrim armorTrim = new ArmorTrim(null, null);
            if (!map.containsKey(armorTrim)) {
                next = armorTrim;
            }
        }
        if (next != null) {
            list.add(null);
        }
        int i = 0;
        for (ArmorTrim trim : list) {
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
                double weight = map.getOrDefault(null, 0D);
                setSlot(i, new Slot(createItem(Material.FLINT,
                        "&eNone",
                        "&bWeight: &a" + weight,
                        "&6Left-Click: &eModify",
                        "&6Right-Click: &eRemove")) {
                    @Override
                    public void onLeftClick() {
                        sendSetMessage("weight for this trim",
                                String.valueOf(weight),
                                s -> {
                                    itemGenerator.getConfig().set(TrimmingListGUI.getPath(null), Double.parseDouble(s));
                                    saveAndReopen();
                                });
                    }

                    @Override
                    public void onRightClick() {
                        itemGenerator.getConfig().remove(TrimmingListGUI.getPath(null));
                        saveAndReopen();
                    }
                });
                i++;
            }
            if (trim == null) {
                ArmorTrim finalNext = Objects.requireNonNull(next);
                setSlot(i, new Slot(createItem(Material.REDSTONE, "&eAdd new trimming")) {
                    @Override
                    public void onLeftClick() {
                        itemGenerator.getConfig().set(getPath(finalNext), 0);
                        saveAndReopen();
                    }
                });
            } else {
                double weight = map.get(trim);
                setSlot(i, new Slot(trim.toItemStack(weight)) {
                    @Override
                    public void onLeftClick() {
                        openSubMenu(new TrimmingGUI(player, itemGenerator, new TrimmingEntry(trim, weight)));
                    }

                    @Override
                    public void onRightClick() {
                        itemGenerator.getConfig().remove(getPath(trim));
                        saveAndReopen();
                    }
                });
            }
        }
        if (list.get(list.size() - 1) == null) {
            list.remove(list.size() - 1);
        }
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }

    public static String getPath(ArmorTrim armorTrim) {
        return EditorGUI.ItemType.ARMOR_TRIMINGS.getPath() + '.' +
                (armorTrim == null ?
                        "none" :
                        (armorTrim.getMaterial() == null ? "*" : armorTrim.getMaterial().getKey().getKey()) + ':'
                                + (armorTrim.getPattern() == null ? "*" : armorTrim.getPattern().getKey().getKey()));
    }

    public static class ArmorTrim {
        private TrimMaterial trimMaterial;
        private TrimPattern  trimPattern;

        public ArmorTrim(TrimMaterial material, TrimPattern pattern) {
            this.trimMaterial = material;
            this.trimPattern = pattern;
        }

        @Nullable
        public TrimMaterial getMaterial() {return trimMaterial;}

        public void setMaterial(@Nullable TrimMaterial trimMaterial) {
            this.trimMaterial = trimMaterial;
        }

        @Nullable
        public TrimPattern getPattern() {
            return trimPattern;
        }

        public void setPattern(@Nullable TrimPattern trimPattern) {
            this.trimPattern = trimPattern;
        }

        public ItemStack toItemStack(double weight) {
            Material material;
            if (this.trimMaterial == null) {
                if (this.trimPattern == null) {
                    material = Material.CRAFTING_TABLE;
                } else {
                    material = TrimmingGUI.fromPattern(this.trimPattern);
                }
            } else {
                if (this.trimPattern == null) {
                    material = TrimmingGUI.fromMaterial(this.trimMaterial);
                } else {
                    material = Material.NETHERITE_CHESTPLATE;
                }
            }
            String trimMaterial = this.trimMaterial == null ? "*" : this.trimMaterial.getKey().getKey();
            String trimPattern  = this.trimPattern == null ? "*" : this.trimPattern.getKey().getKey();
            ItemStack itemStack = createItem(material,
                    "&e" + trimMaterial.substring(0, 1).toUpperCase() + trimMaterial.substring(1) + ' ' + trimPattern,
                    "&bWeight: &a" + weight,
                    "&6Left-Click: &eModify",
                    "&6Right-Click: &eRemove");
            ItemMeta meta = itemStack.getItemMeta();
            if (meta instanceof ArmorMeta) {
                ((ArmorMeta) meta).setTrim(new org.bukkit.inventory.meta.trim.ArmorTrim(this.trimMaterial,
                        this.trimPattern));
                itemStack.setItemMeta(meta);
            }
            return itemStack;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArmorTrim armorTrim = (ArmorTrim) o;
            return Objects.equals(trimMaterial, armorTrim.trimMaterial) && Objects.equals(trimPattern,
                    armorTrim.trimPattern);
        }

        @Override
        public int hashCode() {
            return Objects.hash(trimMaterial, trimPattern);
        }
    }

    @Getter
    @Setter
    public static class TrimmingEntry {
        private ArmorTrim armorTrim;
        private double    weight;

        public TrimmingEntry(ArmorTrim armorTrim, double weight) {
            this.armorTrim = armorTrim;
            this.weight = weight;
        }
    }
}
