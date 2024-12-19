package studio.magemonkey.divinity.utils;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.items.PrefixHelper;
import studio.magemonkey.codex.api.items.providers.ICodexItemProvider;
import studio.magemonkey.codex.modules.IModule;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.LeveledItem;
import studio.magemonkey.divinity.modules.ModuleItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.stats.items.ItemStats;

import java.util.Objects;

public class DivinityProvider implements ICodexItemProvider<DivinityProvider.DivinityItemType> {
    public static final String NAMESPACE = "DIVINITY";

    @Override
    public String pluginName() {
        return "Divinity";
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public Category getCategory() {
        return Category.PRO;
    }

    @Override
    public DivinityItemType getItem(String id) {
        if (id == null || id.isBlank()) return null;

        id = PrefixHelper.stripPrefix(NAMESPACE, id);

        String[]   split      = id.split(":", 3);
        ModuleItem moduleItem = null;
        if (split.length >= 2) { // Module name
            IModule<?> module = Divinity.getInstance().getModuleManager().getModule(split[0]);
            if (!(module instanceof QModuleDrop)) return null;
            moduleItem = ((QModuleDrop<?>) module).getItemById(split[1]);
        } else { // Look in all modules
            for (IModule<?> module : Divinity.getInstance().getModuleManager().getModules()) {
                if (!(module instanceof QModuleDrop)) continue;

                moduleItem = ((QModuleDrop<? extends ModuleItem>) module).getItemById(id);
                break;
            }
        }

        if (moduleItem != null) {
            int level = -1;

            // If the split has a 3rd element, that should be the level.
            // Attempt to parse it as an integer and use that for the level, defaulting to -1
            // if it fails or if the length is not 3
            if (split.length >= 3) {
                try {
                    level = Integer.parseInt(split[2]);
                } catch (NumberFormatException ignored) {
                }
            }

            return new DivinityItemType(moduleItem, level);
        }

        return null;
    }

    @Override
    @Nullable
    public DivinityProvider.DivinityItemType getItem(ItemStack itemStack) {
        String id = ItemStats.getId(itemStack);
        if (id == null) return null;
        return getItem(id);
    }

    @Override
    public boolean isCustomItem(ItemStack item) {
        return ItemStats.getId(item) != null;
    }

    @Override
    public boolean isCustomItemOfId(ItemStack item, String id) {
        id = PrefixHelper.stripPrefix(NAMESPACE, id);

        String itemId = ItemStats.getId(item);
        return itemId != null && itemId.equals(id);
    }

    public static class DivinityItemType extends ItemType {
        private final ModuleItem moduleItem;
        @Getter
        private final int        level;

        /**
         * Constructs a new DivinityItemType. The level parameter is only used for {@link LeveledItem}s
         * and is ignored for other types of items.
         * @param moduleItem The module item
         * @param level The level of the item, use <code>-1</code> for a random level.
         */
        public DivinityItemType(ModuleItem moduleItem, int level) {
            this.moduleItem = moduleItem;
            this.level = level;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public String getID() {
            return this.moduleItem.getId();
        }

        @Override
        public Category getCategory() {
            return Category.PRO;
        }

        @Override
        public ItemStack create() {
            if (this.moduleItem instanceof LeveledItem) {
                return ((LeveledItem) this.moduleItem).create(this.level);
            }

            return this.moduleItem.create();
        }

        @Override
        public boolean isInstance(@Nullable ItemStack itemStack) {
            if (itemStack == null) return false;
            String id = ItemStats.getId(itemStack);
            return id != null && id.equals(this.moduleItem.getId())
                    && Objects.equals(ItemStats.getModule(itemStack), this.moduleItem.getModule());
        }
    }
}
