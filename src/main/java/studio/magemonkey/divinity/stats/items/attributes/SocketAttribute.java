package studio.magemonkey.divinity.stats.items.attributes;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.util.DataUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.SocketItem;
import studio.magemonkey.divinity.modules.api.socketing.ModuleSocket;
import studio.magemonkey.divinity.modules.list.essences.EssencesManager;
import studio.magemonkey.divinity.modules.list.gems.GemManager;
import studio.magemonkey.divinity.modules.list.runes.RuneManager;
import studio.magemonkey.divinity.stats.items.ItemTags;
import studio.magemonkey.divinity.stats.items.api.DuplicableItemLoreStat;
import studio.magemonkey.divinity.stats.items.attributes.api.Emptible;
import studio.magemonkey.divinity.stats.tiers.Tier;
import studio.magemonkey.divinity.stats.tiers.Tiered;

public class SocketAttribute extends DuplicableItemLoreStat<String[]> implements Tiered, Emptible<String[]> {

    private Type            type;
    private ModuleSocket<?> module;
    private Tier            tier;

    private String formatValueEmpty;
    private String formatValueFilled;

    public SocketAttribute(
            @NotNull Type type,

            @NotNull String id,
            @NotNull String name,
            @NotNull String format,

            @NotNull Tier tier,

            @NotNull String formatValueEmpty,
            @NotNull String formatValueFilled
    ) {
        super(id,
                name,
                format,
                "%SOCKET_" + type.name() + "_" + id + "%",
                ItemTags.TAG_ITEM_SOCKET + type.name() + '_',
                DataUT.STRING_ARRAY);
        this.type = type;
        this.tier = tier;

        this.name = this.getTier().format(this.name);
        this.formatValueEmpty =
                this.getTier().format(StringUT.color(formatValueEmpty).replace("%name%", this.getName()));
        this.formatValueFilled =
                this.getTier().format(StringUT.color(formatValueFilled).replace("%name%", this.getName()));

        // Legacy keys
        this.keys.add(NamespacedKey.fromString(
                "prorpgitems:item_socket_" + type.name().toLowerCase() + "_" + this.getId()));
        this.keys.add(NamespacedKey.fromString("prorpgitems:item_socket_" + type.name().toLowerCase() + this.getId()));
        this.keys.add(NamespacedKey.fromString(
                "prorpgitems:qrpg_item_socket_" + type.name().toLowerCase() + this.getId()));
        this.keys.add(NamespacedKey.fromString(
                "quantumrpg:qrpg_item_socket_" + type.name().toLowerCase() + this.getId()));
    }

    @Override
    @NotNull
    public Class<String[]> getParameterClass() {
        return String[].class;
    }

    public static enum Type {

        GEM,
        RUNE,
        ESSENCE,
        ;

        @Nullable
        public static SocketAttribute.Type getByModule(@NotNull ModuleSocket<?> mod) {
            if (mod instanceof GemManager) {
                return SocketAttribute.Type.GEM;
            } else if (mod instanceof RuneManager) {
                return SocketAttribute.Type.RUNE;
            } else if (mod instanceof EssencesManager) {
                return SocketAttribute.Type.ESSENCE;
            } else return null;
        }

        @Nullable
        public static SocketAttribute.Type getByName(@NotNull String name) {
            try {
                return SocketAttribute.Type.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        @Nullable
        public ModuleSocket<?> getModule() {
            switch (this) {
                case GEM: {
                    return Divinity.getInstance().getModuleCache().getGemManager();
                }
                case RUNE: {
                    return Divinity.getInstance().getModuleCache().getRuneManager();
                }
                case ESSENCE: {
                    return Divinity.getInstance().getModuleCache().getEssenceManager();
                }
            }
            return null;
        }
    }

    @NotNull
    public ModuleSocket<?> getModule() {
        if (this.module == null) {
            this.module = this.type.getModule();
        }
        if (this.module == null) {
            throw new IllegalStateException("NULL module in Socket Attribute!");
        }
        return this.module;
    }

    @NotNull
    public SocketAttribute.Type getType() {
        return this.type;
    }

    @Override
    @NotNull
    public Tier getTier() {
        return this.tier;
    }

    public final int getFirstEmptyIndex(@NotNull ItemStack item) {
        int total = this.getAmount(item);
        for (int index = 0; index < total; index++) {
            String[] storedValue = this.getRaw(item, index);
            if (storedValue != null && this.isEmpty(storedValue)) {
                return index;
            }
        }
        return -1;
    }

    public final int getEmptyAmount(@NotNull ItemStack item) {
        int total = this.getAmount(item);
        int count = 0;

        for (int index = 0; index < total; index++) {
            String[] storedValue = this.getRaw(item, index);
            if (storedValue != null && this.isEmpty(storedValue)) {
                count++;
            }
        }
        return count;
    }

    public final int getFilledAmount(@NotNull ItemStack item) {
        int total = this.getAmount(item);
        int count = 0;

        for (int index = 0; index < total; index++) {
            String[] storedValue = this.getRaw(item, index);
            if (storedValue != null && !this.isEmpty(storedValue)) {
                count++;
            }
        }
        return count;
    }

    @Override
    @NotNull
    public String formatValue(@NotNull ItemStack item, @NotNull String[] value) {
        String format = this.formatValueEmpty;

        if (!this.isEmpty(value)) {
            String     itemId = value[0];
            SocketItem mItem  = this.getModule().getItemById(itemId);
            if (mItem == null) {
                Divinity.getInstance().error("Invalid socket item '" + itemId + "' for Socket Attribute.");
                return format;
            }

            int srcLevel = StringUT.getInteger(value[1], 0);
            return this.formatValueFilled.replace("%value%", mItem.getSocketDisplay(srcLevel));
        }
        return format;
    }

    @Override
    public boolean isEmpty(@NotNull String[] value) {
        return value.length != 2 || value[0].isEmpty() || value[1].isEmpty();
    }

    @Override
    public String[] getDefaultValue() {
        return new String[]{"", ""};
    }
}
