package com.promcteam.divinity.stats.items.requirements.item;

import com.promcteam.codex.config.api.ILangMsg;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import com.promcteam.divinity.QuantumRPG;
import com.promcteam.divinity.config.EngineCfg;
import com.promcteam.divinity.stats.items.ItemStats;
import com.promcteam.divinity.stats.items.ItemTags;
import com.promcteam.divinity.stats.items.requirements.api.ItemRequirement;

public class ItemLevelRequirement extends ItemRequirement<int[]> {

    public ItemLevelRequirement(
            @NotNull String name,
            @NotNull String format
    ) {
        super(
                "level",
                name,
                format,
                ItemTags.PLACEHOLDER_REQ_ITEM_LEVEL,
                ItemTags.TAG_REQ_ITEM_LEVEL,
                PersistentDataType.INTEGER_ARRAY);

        // Legacy keys
        this.keys.add(NamespacedKey.fromString("prorpgitems:qrpg_req_item_levellevel"));
        this.keys.add(NamespacedKey.fromString("quantumrpg:qrpg_req_item_levellevel"));
    }

    @Override
    @NotNull
    public Class<int[]> getParameterClass() {
        return int[].class;
    }

    @Override
    public boolean canApply(@NotNull ItemStack src, @NotNull ItemStack target) {
        int[] arr = this.getRaw(src);
        if (arr == null) return true;

        int min       = arr[0];
        int max       = arr.length == 2 ? arr[1] : min;
        int itemLevel = ItemStats.getLevel(target);

        return min == max ? (itemLevel >= min) : (itemLevel >= min && itemLevel <= max);
    }

    @Override
    @NotNull
    public String formatValue(@NotNull ItemStack item, int[] levels) {
        if (levels.length == 0 || (levels.length == 1 && levels[0] <= 0)) return "";

        if (levels.length == 1 || (levels.length == 2 && levels[0] == levels[1])) {
            return EngineCfg.LORE_STYLE_REQ_USER_LVL_FORMAT_SINGLE.replace("%min%", String.valueOf(levels[0]));
        }

        int min = Math.min(levels[0], levels[1]);
        int max = Math.max(levels[0], levels[1]);
        return EngineCfg.LORE_STYLE_REQ_USER_LVL_FORMAT_RANGE
                .replace("%max%", String.valueOf(max))
                .replace("%min%", String.valueOf(min));
    }

    @Override
    public ILangMsg getApplyMessage(@NotNull ItemStack src, @NotNull ItemStack target) {
        int[] arr = this.getRaw(src);
        if (arr == null) throw new IllegalStateException("Item does not have stat!");

        return QuantumRPG.getInstance().lang().Module_Item_Apply_Error_Level.replace("%value%",
                this.formatValue(src, arr));
    }
}
