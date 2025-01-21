package studio.magemonkey.divinity.modules.list.itemgenerator.editor.materials;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.items.exception.CodexItemException;
import studio.magemonkey.codex.api.items.exception.MissingItemException;
import studio.magemonkey.codex.api.items.exception.MissingProviderException;
import studio.magemonkey.codex.api.items.providers.VanillaProvider;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.divinity.config.Config;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;
import studio.magemonkey.divinity.types.ItemGroup;
import studio.magemonkey.divinity.types.ItemSubType;

public class MainMaterialsGUI extends AbstractEditorGUI {
    public MainMaterialsGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                1,
                "Editor/" + EditorGUI.ItemType.MATERIALS.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        boolean reversed = this.itemGenerator.getHandle().isMaterialReversed();
        setSlot(0, new Slot(createItem(reversed
                        ? Material.STRUCTURE_VOID
                        : Material.BARRIER,
                "&eIs whitelist/reversed",
                "&bCurrent: &a" + reversed,
                "&6Left-Click: &eToggle",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onLeftClick() {
                itemGenerator.getConfig().set(ItemType.REVERSE.getPath(), !reversed);
                saveAndReopen();
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.REVERSE.getPath());
                saveAndReopen();
            }
        });
        setSlot(1, new Slot(createItem(Material.BOOK,
                "&e" + (reversed
                        ? "Whitelist"
                        : "Blacklist"),
                StringUT.replace(CURRENT_PLACEHOLDER, itemGenerator.getConfig().getStringList(ItemType.LIST.getPath()),
                        "&bCurrent:",
                        "&a%current%",
                        "&6Left-Click: &eModify"
                ))) {
            @Override
            public void onLeftClick() {
                openSubMenu(new MaterialListGUI(player, itemGenerator));
            }
        });
        setSlot(2, new Slot(createItem(Material.END_CRYSTAL,
                "&eModel Data",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new MainModelDataGUI(player, itemGenerator));
            }
        });
    }

    public static ItemStack getMaterial(String string) {
        try {
            return CodexEngine.get().getItemManager().getItemType(string).create();
        } catch (MissingProviderException | MissingItemException ignored) {
        }

        try {
            return new ItemStack(Material.valueOf(string.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
        }

        String[] split = string.toUpperCase().split('\\' + JStrings.MASK_ANY, 2);
        if (split.length == 2) { // We have a wildcard
            // First attempt to look literally
            if (split[0].isEmpty()) {
                try {
                    return CodexEngine.get().getItemManager().getItemType(split[1]).create();
                } catch (CodexItemException ignored) {
                }
            } else if (split[1].isEmpty()) {
                try {
                    return CodexEngine.get().getItemManager().getItemType(split[0]).create();
                } catch (CodexItemException ignored) {
                }
            }

            // If not found, find first thing that matches
            for (studio.magemonkey.codex.api.items.ItemType material : Config.getAllRegisteredMaterials()) {
                String materialName = material.getNamespacedID().toUpperCase();
                if (split[0].isEmpty() && materialName.endsWith(split[1])
                        || split[1].isEmpty() && materialName.startsWith(split[0])) return material.create();
            }
        }
        return new ItemStack(Material.STONE);
    }

    public static ItemStack getMaterialGroup(String materialGroup) {
        try {
            return CodexEngine.get().getItemManager().getItemType(materialGroup).create();
        } catch (MissingProviderException | MissingItemException ignored) {
        }

        ItemSubType subType = Config.getSubTypeById(materialGroup);
        if (subType != null) {
            return subType.getMaterials()
                    .stream()
                    .findAny()
                    .orElse(new VanillaProvider.VanillaItemType(Material.STONE))
                    .create();
        }

        try {
            return ItemGroup.valueOf(materialGroup.toUpperCase())
                    .getMaterials()
                    .stream()
                    .findAny()
                    .orElse(new VanillaProvider.VanillaItemType(Material.STONE))
                    .create();
        } catch (IllegalArgumentException ignored) {
        }

        return getMaterial(materialGroup.toUpperCase());
    }

    public enum ItemType {
        REVERSE("reverse"),
        LIST("black-list"),
        MODEL_DATA("model-data"),
        ;

        private final String path;

        ItemType(String path) {
            this.path = "generator.materials." + path;
        }

        public String getPath() {return path;}
    }
}
