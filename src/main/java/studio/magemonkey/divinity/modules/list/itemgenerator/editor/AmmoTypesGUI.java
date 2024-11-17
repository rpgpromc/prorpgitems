package studio.magemonkey.divinity.modules.list.itemgenerator.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.AmmoAttribute;

public class AmmoTypesGUI extends AbstractEditorGUI {

    public AmmoTypesGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                6,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.AMMO_TYPES.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        int i = 0;
        for (AmmoAttribute ammo : ItemStats.getAmmos()) {
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
            switch (ammo.getType()) {
                case SNOWBALL: {
                    material = Material.SNOWBALL;
                    break;
                }
                case EGG: {
                    material = Material.EGG;
                    break;
                }
                case FIREBALL: {
                    material = Material.FIRE_CHARGE;
                    break;
                }
                case WITHER_SKULL: {
                    material = Material.WITHER_SKELETON_SKULL;
                    break;
                }
                case SHULKER_BULLET: {
                    material = Material.SHULKER_SHELL;
                    break;
                }
                case LLAMA_SPIT: {
                    material = Material.LLAMA_SPAWN_EGG;
                    break;
                }
                case ENDER_PEARL: {
                    material = Material.ENDER_PEARL;
                    break;
                }
                case EXP_POTION: {
                    material = Material.EXPERIENCE_BOTTLE;
                    break;
                }
                default: {
                    material = Material.ARROW;
                    break;
                }
            }
            String id = ammo.getId().toUpperCase();
            this.setSlot(i, new Slot(createItem(material,
                    "&e" + ammo.getName(),
                    "&bCurrent: &a" + itemGenerator.getConfig()
                            .getDouble(EditorGUI.ItemType.AMMO_TYPES.getPath() + '.' + id, 0),
                    "&6Left-Click: &eSet",
                    "&6Right-Click: &eRemove")) {
                @Override
                public void onLeftClick() {
                    sendSetMessage(id,
                            String.valueOf(itemGenerator.getConfig()
                                    .getDouble(EditorGUI.ItemType.AMMO_TYPES.getPath() + '.' + id, 0)),
                            s -> {
                                double chance = Double.parseDouble(s);
                                if (chance == 0) {
                                    itemGenerator.getConfig()
                                            .remove(EditorGUI.ItemType.AMMO_TYPES.getPath() + '.' + id);
                                } else if (chance > 0) {
                                    itemGenerator.getConfig()
                                            .set(EditorGUI.ItemType.AMMO_TYPES.getPath() + '.' + id, chance);
                                } else {
                                    throw new IllegalArgumentException();
                                }
                                saveAndReopen();
                            });
                }

                @Override
                public void onRightClick() {
                    itemGenerator.getConfig().remove(EditorGUI.ItemType.AMMO_TYPES.getPath() + '.' + id);
                    saveAndReopen();
                }
            });
        }
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }
}
