package studio.magemonkey.divinity.hooks.external.mimic;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.endlesscode.mimic.items.BukkitItemsRegistry;
import studio.magemonkey.codex.modules.ModuleManager;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.ModuleItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DivinityItemsRegistry implements BukkitItemsRegistry {

    static final String ID = "divinity";

    private static final String SEPARATOR = "/";

    private final Divinity divinity;

    public DivinityItemsRegistry(Divinity divinity) {
        this.divinity = divinity;
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isEnabled() {
        return divinity.isEnabled();
    }

    @NotNull
    @Override
    public Collection<String> getKnownIds() {
        return getDropModules()
                .flatMap(module ->
                        module.getItemIds()
                                .stream()
                                .map(item -> buildItemName(module, item))
                )
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSameItem(@NotNull ItemStack item, @NotNull String itemId) {
        String realItemId = getItemId(item);
        if (realItemId == null) return false;

        String[] parts = splitItemId(itemId);
        String   id    = parts[1];
        return realItemId.equals(id);
    }

    @Override
    public boolean isItemExists(@NotNull String itemId) {
        String[] parts    = splitItemId(itemId);
        String   moduleId = parts[0];
        String   id       = parts[1];

        return getDropModules(moduleId)
                .anyMatch(module -> module.getItemById(id) != null);
    }

    @Nullable
    @Override
    public String getItemId(@NotNull ItemStack item) {
        return getDropModules()
                .map(module -> {
                    String itemId = module.getItemId(item);
                    return itemId != null ? buildItemName(module, itemId) : null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    @Override
    public ItemStack getItem(@NotNull String itemId, @Nullable Object payload, int amount) {
        String[] parts    = splitItemId(itemId);
        String   moduleId = parts[0];
        String   id       = parts[1];

        Optional<ItemStack> searchResult = getDropModules(moduleId)
                .map(module -> module.getItemById(id))
                .filter(Objects::nonNull)
                .map(ModuleItem::create)
                .findFirst();

        if (searchResult.isPresent()) {
            ItemStack itemStack = searchResult.get();
            itemStack.setAmount(Math.max(Math.min(amount, itemStack.getMaxStackSize()), 1));
            return itemStack;
        } else {
            return null;
        }
    }

    private Stream<QModuleDrop<?>> getDropModules(String moduleId) {
        return moduleId.isEmpty()
                ? getDropModules()
                : getDropModules().filter(module -> moduleId.equals(module.getId()));
    }

    private Stream<QModuleDrop<?>> getDropModules() {
        ModuleManager<Divinity> moduleManager = divinity.getModuleManager();
        Objects.requireNonNull(
                moduleManager,
                "It seems you're trying to access items from Divinity before the plugin is loaded. " +
                        "Try to add Divinity to 'depend' or 'softpend' of your plugin."
        );

        return moduleManager
                .getModules()
                .stream()
                .filter(module -> module instanceof QModuleDrop)
                .map(module -> (QModuleDrop<?>) module);
    }

    private String[] splitItemId(String itemId) {
        return itemId.contains(SEPARATOR) ? itemId.split(SEPARATOR, 2) : new String[]{"", itemId};
    }

    private String buildItemName(QModuleDrop<?> module, String itemId) {
        return module.getId() + SEPARATOR + itemId;
    }
}
