package studio.magemonkey.divinity.utils;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.items.CodexItemManager;
import studio.magemonkey.codex.items.ItemType;
import studio.magemonkey.codex.items.providers.ICodexItemProvider;
import studio.magemonkey.codex.modules.IModule;
import studio.magemonkey.divinity.Divinity;
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

        id = CodexItemManager.stripPrefix(NAMESPACE, id);

        String[] split = id.split(":", 2);
        if (split.length == 2) { // Module name
            IModule<?> module = Divinity.getInstance().getModuleManager().getModule(split[0]);
            if (!(module instanceof QModuleDrop)) return null;
            ModuleItem moduleItem = ((QModuleDrop<?>) module).getItemById(split[1]);
            return moduleItem == null ? null : new DivinityItemType(moduleItem);
        } else { // Look in all modules
            for (IModule<?> module : Divinity.getInstance().getModuleManager().getModules()) {
                if (!(module instanceof QModuleDrop)) continue;

                ModuleItem item = ((QModuleDrop<? extends ModuleItem>) module).getItemById(id);
                if (item != null) {
                    return new DivinityItemType(item);
                }
            }
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
        id = CodexItemManager.stripPrefix(NAMESPACE, id);

        String itemId = ItemStats.getId(item);
        return itemId != null && itemId.equals(id);
    }

    public static class DivinityItemType extends ItemType {
        private final ModuleItem moduleItem;

        public DivinityItemType(ModuleItem moduleItem) {
            this.moduleItem = moduleItem;
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
