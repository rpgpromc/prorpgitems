package studio.magemonkey.divinity.utils;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.Codex;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.items.PrefixHelper;
import studio.magemonkey.codex.api.items.exception.MissingItemException;
import studio.magemonkey.codex.api.items.exception.MissingProviderException;
import studio.magemonkey.codex.api.items.providers.ICodexItemProvider;
import studio.magemonkey.codex.modules.IModule;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.LeveledItem;
import studio.magemonkey.divinity.modules.ModuleItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;
import studio.magemonkey.divinity.stats.items.ItemStats;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DivinityProvider implements ICodexItemProvider<DivinityProvider.DivinityItemType> {
    public static final String NAMESPACE = "DIVINITY";

    private static Pattern levelPattern    = Pattern.compile(".*~level:(\\d+).*");
    private static Pattern materialPattern = Pattern.compile(".*~material:(\\w+).*");

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

        int      level    = -1;
        ItemType material = null;

        Matcher levelMatcher = levelPattern.matcher(id);
        if (levelMatcher.matches()) {
            try {
                level = Integer.parseInt(levelMatcher.group(1));
            } catch (NumberFormatException ignored) {
                Codex.warn("Failed to get level for Divinity item " + id
                        + ". Using -1 instead.");
            }
            id = id.replace("~level:" + levelMatcher.group(1), "");
        }

        Matcher materialMatcher = materialPattern.matcher(id);
        if (materialMatcher.matches()) {
            try {
                material = CodexEngine.get().getItemManager()
                        .getItemType(materialMatcher.group(1));
            } catch (MissingProviderException | MissingItemException ignored) {
                Codex.warn("Failed to get material item for Divinity item " + id
                        + ". Using the item's configuration instead.");
            }
            id = id.replace("~material:" + materialMatcher.group(1), "");
        }


        String[]   split      = id.split(":", 2);
        ModuleItem moduleItem = null;
        if (split.length >= 2) { // Module name
            IModule<?> module = Divinity.getInstance().getModuleManager().getModule(split[0]);
            if (!(module instanceof QModuleDrop)) return null;
            moduleItem = ((QModuleDrop<?>) module).getItemById(split[1]);
        } else { // Look in all modules
            for (IModule<?> module : Divinity.getInstance().getModuleManager().getModules()) {
                if (!(module instanceof QModuleDrop)) continue;

                moduleItem = ((QModuleDrop<? extends ModuleItem>) module).getItemById(id);
                if (moduleItem != null) break;
            }
        }

        if (moduleItem != null) return new DivinityItemType(moduleItem, level, material);

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
        @Getter
        private final ModuleItem moduleItem;
        @Getter
        private final int        level;
        @Nullable
        @Getter
        private       ItemType   material;

        /**
         * Constructs a new DivinityItemType. The level parameter is only used for {@link LeveledItem}s
         * and is ignored for other types of items.
         * @param moduleItem The module item
         * @param level The level of the item, use <code>-1</code> for a random level.
         * @param material The material of the item, or <code>null</code> if none
         */
        public DivinityItemType(ModuleItem moduleItem, int level, @Nullable ItemType material) {
            this.moduleItem = moduleItem;
            this.level = level;
            this.material = material;
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
            if (this.moduleItem instanceof ItemGeneratorManager.GeneratorItem) {
                return ((ItemGeneratorManager.GeneratorItem) this.moduleItem)
                        .create(this.level, -1, this.material);
            }

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
