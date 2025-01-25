package studio.magemonkey.divinity.modules.list.itemgenerator.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ItemFlagsGUI extends AbstractEditorGUI {
    private static final String PATH = EditorGUI.ItemType.ITEM_FLAGS.getPath();

    public ItemFlagsGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                6,
                "Editor/" + EditorGUI.ItemType.ITEM_FLAGS.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        int i = 0;
        for (ItemFlag flag : ItemFlag.values()) {
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

            Material material;
            switch (flag) {
                case HIDE_ENCHANTS: {
                    material = Material.ENCHANTED_BOOK;
                    break;
                }
                case HIDE_ATTRIBUTES: {
                    material = Material.OAK_SIGN;
                    break;
                }
                case HIDE_UNBREAKABLE: {
                    material = Material.ANVIL;
                    break;
                }
                case HIDE_DESTROYS: {
                    material = Material.DIAMOND_PICKAXE;
                    break;
                }
                case HIDE_PLACED_ON: {
                    material = Material.OAK_PLANKS;
                    break;
                }
                case HIDE_DYE: {
                    material = Material.MAGENTA_DYE;
                    break;
                }
                default: {
                    material = Material.STONE;
                    break;
                }
            }

            if (flag.name().equals("HIDE_POTION_EFFECTS") || flag.name().equals("HIDE_ADDITIONAL_TOOLTIP")) {
                material = Material.POTION;
            }

            String name = flag.name().toLowerCase();
            this.setSlot(i, new Slot(createItem(material,
                    "&e" + name,
                    "&bCurrent: &a" + this.itemGenerator.getHandle().getFlags().contains(flag),
                    "&6Left-Click: &eToggle",
                    "&6Right-Click: &eSet to default value")) {
                @Override
                public void onLeftClick() {
                    Set<String> itemFlags = new HashSet<>(itemGenerator.getConfig().getStringList(PATH));
                    if (itemFlags.contains(JStrings.MASK_ANY)) {
                        itemFlags.remove(JStrings.MASK_ANY);
                        for (ItemFlag itemFlag : ItemFlag.values()) {
                            itemFlags.add(itemFlag.name().toLowerCase());
                        }
                    }
                    if (itemFlags.contains(name)) {
                        itemFlags.remove(name);
                    } else {
                        itemFlags.add(name);
                    }
                    boolean all = true;
                    for (ItemFlag itemFlag : ItemFlag.values()) {
                        if (!itemFlags.contains(itemFlag.name().toLowerCase())) {
                            all = false;
                            break;
                        }
                    }
                    if (all) {
                        itemFlags.clear();
                        itemFlags.add(JStrings.MASK_ANY);
                    }
                    itemGenerator.getConfig().set(PATH, new ArrayList<>(itemFlags));
                    saveAndReopen();
                }

                @Override
                public void onDrop() {
                    Set<String> defaultFlags =
                            new HashSet<>(ItemGeneratorManager.commonItemGenerator.getStringList(PATH));
                    Set<String> itemFlags = new HashSet<>(itemGenerator.getConfig().getStringList(PATH));
                    if (itemFlags.contains(JStrings.MASK_ANY)) {
                        itemFlags.remove(JStrings.MASK_ANY);
                        for (ItemFlag itemFlag : ItemFlag.values()) {
                            itemFlags.add(itemFlag.name().toLowerCase());
                        }
                    }
                    if (defaultFlags.contains(name) || defaultFlags.contains(JStrings.MASK_ANY)) {
                        itemFlags.add(name);
                    } else {
                        itemFlags.remove(name);
                    }
                    boolean all = true;
                    for (ItemFlag itemFlag : ItemFlag.values()) {
                        if (!itemFlags.contains(itemFlag.name().toLowerCase())) {
                            all = false;
                            break;
                        }
                    }
                    if (all) {
                        itemFlags.clear();
                        itemFlags.add(JStrings.MASK_ANY);
                    }
                    itemGenerator.getConfig().set(PATH, new ArrayList<>(itemFlags));
                    saveAndReopen();
                }
            });
        }
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }
}
