package studio.magemonkey.divinity.stats.items.requirements.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.ILangMsg;
import studio.magemonkey.divinity.stats.items.api.ItemLoreStat;

public abstract class UserRequirement<Z> extends ItemLoreStat<Z> {

    public UserRequirement(
            @NotNull String id,
            @NotNull String name,
            @NotNull String format,
            @NotNull String placeholder,
            @NotNull String uniqueTag,
            @NotNull PersistentDataType<?, Z> dataType
    ) {
        super(id, name, format, placeholder, uniqueTag, dataType);
    }

    public final boolean isDynamic() {
        return this instanceof DynamicUserRequirement;
    }

    @NotNull
    public abstract String getBypassPermission();

    public boolean canUse(@NotNull Player player, @NotNull ItemStack item) {
        return this.canUse(player, this.getRaw(item));
    }

    public abstract boolean canUse(@NotNull Player player, @Nullable Z value);

    @NotNull
    public abstract ILangMsg getDenyMessage(@NotNull Player player, @NotNull ItemStack src);
}
