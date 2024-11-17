package studio.magemonkey.divinity.stats.items.api;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.util.DataUT;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.stats.bonus.StatBonus;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.utils.LoreUT;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class DuplicableItemLoreStat<Z> extends ItemLoreStat<Z> {

    public DuplicableItemLoreStat(
            @NotNull String id,
            @NotNull String name,
            @NotNull String format,
            @NotNull String placeholder,
            @NotNull String uniqueTag,
            @NotNull PersistentDataType<?, Z> dataType
    ) {
        super(id, name, format, placeholder, uniqueTag, dataType);
    }

    @NotNull
    protected final List<NamespacedKey> getKeys(int index) {
        List<NamespacedKey> indexedKeys = new ArrayList<>();
        for (NamespacedKey key : this.keys) {
            indexedKeys.add(NamespacedKey.fromString(key.toString() + index));
        }
        return indexedKeys;
    }

    @NotNull
    protected final NamespacedKey getKey(int index) {
        return Objects.requireNonNull(NamespacedKey.fromString(this.keys.get(0).toString() + index));
    }

    @Override
    public boolean add(@NotNull ItemStack item, @NotNull Z value, int line) {
        return this.add(item, value, -1, line);
    }

    public boolean add(@NotNull ItemStack item, @NotNull Z value, int index, int line) {
        if (index < 0 && value instanceof StatBonus) {
            StatBonus statBonus = ((StatBonus) value);

            if (statBonus.isBaseStat()) { // Look for legacy format
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer container = meta.getPersistentDataContainer();
                    for (NamespacedKey key : this.keys) {
                        if (container.has(key, PersistentDataType.DOUBLE) || container.has(key, DataUT.DOUBLE_ARRAY)) {
                            line = ItemUT.getLoreIndex(item, key.getKey());
                            index = 0;
                            container.remove(key);
                            item.setItemMeta(meta);
                            break;
                        }
                    }
                }
            }
            if (index < 0) {
                // Look for existing stats with same condition
                for (int i = 0, size = this.getAmount(item); i < size; i++) {
                    StatBonus existing = (StatBonus) this.getRaw(item, i);
                    if (existing != null && Objects.equals(existing.getCondition(), (statBonus).getCondition())) {
                        index = i;
                        break;
                    }
                }
            }
        }
        if (index < 0) index = this.getAmount(item);
        if (line < 0) line = this.getLoreIndex(item, index);

        //System.out.println("Adding new index: " + index);
        item = this.preparePlaceholder(item, index, line); // Replace current text with placeholder
        item = this.remove(item, index, true); // Remove meta tag. Does not remove lore as it replaced above.

        String format = this instanceof DynamicStat
                ? ((DynamicStat<Z>) this).getFormat(null, item, value)
                : this.getFormat(item, value);

        // Now replace the placeholder with requirement value
        // or delete it in case of requirement removal.
        if (format.isEmpty()) {
            LoreUT.replacePlaceholder(item, this.getPlaceholder(), null);
            for (NamespacedKey key : keys) {
                ItemUT.delLoreTag(item, key.getKey() + index);
            }
        } else {
            LoreUT.replacePlaceholder(item, this.getPlaceholder(), format);

            ItemMeta meta = item.getItemMeta();
            if (meta == null) return false;

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(this.getKey(index), this.dataType, value);
            item.setItemMeta(meta);

            ItemUT.addLoreTag(item, this.getMetaId(item, index), format);
        }

        ItemStats.updateVanillaAttributes(item, null);

        return !format.isEmpty();
    }

    @NotNull
    public final ItemStack remove(@NotNull ItemStack item, int index, boolean onlyTag) {
        if (index < 0) index = this.getAmount(item) - 1;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (!onlyTag) {
            int pos = this.getLoreIndex(item, index);
            if (pos >= 0) {
                List<String> lore = meta.getLore();
                if (lore != null) {
                    lore.remove(pos);
                    meta.setLore(lore);
                }
            }
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        for (NamespacedKey key : this.getKeys(index)) {
            if (container.has(key, this.dataType)) container.remove(key);
        }

        item.setItemMeta(meta);

        for (NamespacedKey key : this.keys) {
            ItemUT.delLoreTag(item, key.getKey() + index);
        }

        if (!onlyTag) {
            this.reorderTags(item);
        }

        return item;
    }

    // Reorder index values of current Item Stat.
    // When current Item Stat got removed from the item,
    // current stat keys on item will missing removed index (like 1,2,3,4 -> 1,2,4)
    // that may cause unexpected plugin behavior.
    private final void reorderTags(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container  = meta.getPersistentDataContainer();
        List<Z>                 valuesLeft = new ArrayList<>();

        container.getKeys().stream()
                .filter(namespacedKey -> {
                    for (NamespacedKey key : this.keys) {
                        if (namespacedKey.toString().startsWith(key.toString()) && container.has(namespacedKey,
                                this.dataType)) return true;
                    }
                    return false;
                }).forEach(namespacedKey -> {
                    @Nullable Z val = container.get(namespacedKey, this.dataType);
                    if (val != null) {
                        valuesLeft.add(val);
                    }
                });

        //System.out.println("Applying new values...");
        for (int newIndex = 0; newIndex < valuesLeft.size(); newIndex++) {
            //System.out.println("Applied index " + newIndex);
            container.set(this.getKey(newIndex), this.dataType, valuesLeft.get(newIndex));
        }

        item.setItemMeta(meta);
    }

    @Nullable
    public final Z getRaw(ItemStack item, int index) {
        return getRaw(item.getItemMeta(), index);
    }

    @Nullable
    public final Z getRaw(ItemMeta meta, int index) {
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        for (NamespacedKey key : this.getKeys(index)) {
            if (container.has(key, this.dataType)) return container.get(key, this.dataType);
        }
        return null;
    }

    @NotNull
    public final List<Z> getAllRaw(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return List.of();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.getKeys().stream()
                .filter(namespacedKey -> {
                    for (NamespacedKey key : this.keys) {
                        if (namespacedKey.toString().startsWith(key.toString()) && container.has(namespacedKey,
                                this.dataType)) return true;
                    }
                    return false;
                })
                .map(key -> container.get(key, dataType))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public final int getAmount(@NotNull ItemStack item) {
        return getAmount(item.getItemMeta());
    }

    public final int getAmount(ItemMeta meta) {
        if (meta == null) return 0;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return (int) container.getKeys().stream()
                .filter(namespacedKey -> {
                    for (NamespacedKey key : this.keys) {
                        if (namespacedKey.toString().startsWith(key.toString()) && container.has(namespacedKey,
                                this.dataType)) return true;
                    }
                    return false;
                }).count();
    }

    public final int getLoreIndex(@NotNull ItemStack item, int index) {
        if (index < 0) return -1;
        int count = 0;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return -1;
        List<String> lore = meta.getLore();
        if (lore == null) return -1;

        for (int i = 0; i < lore.size(); i++) {
            int found = 0;
            for (NamespacedKey key : this.keys) {
                found = ItemUT.getLoreIndex(item, key.getKey() + i);
                if (found >= 0) break;

                // Try legacy format
                found = ItemUT.getLoreIndex(item, key.getKey());
                if (found >= 0) break;
            }

            if (found >= 0 && index == count++) {
                return found;
            }
        }
        return -1;
    }

    @NotNull
    protected final ItemStack preparePlaceholder(@NotNull ItemStack item, int index, int line) {
        if (!this.hasPlaceholder(item)) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return item;
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();

            int pos = this.getLoreIndex(item, index);
            LoreUT.addOrReplace(lore, pos, line, this.getPlaceholder());

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
