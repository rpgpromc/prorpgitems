package studio.magemonkey.divinity.stats.items.attributes.api;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.compat.VersionManager;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.modules.list.gems.GemManager;
import studio.magemonkey.divinity.modules.list.refine.RefineManager;
import studio.magemonkey.divinity.stats.bonus.BonusCalculator;
import studio.magemonkey.divinity.stats.bonus.BonusMap;
import studio.magemonkey.divinity.stats.bonus.StatBonus;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.ItemTags;
import studio.magemonkey.divinity.stats.items.api.DuplicableItemLoreStat;
import studio.magemonkey.divinity.stats.items.api.DynamicStat;
import studio.magemonkey.divinity.utils.ItemUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public class SimpleStat extends DuplicableItemLoreStat<StatBonus> implements TypedStat, DynamicStat<StatBonus> {

    protected Type   statType;
    private   double cap;

    public SimpleStat(
            @NotNull Type statType,
            @NotNull String name,
            @NotNull String format,
            double cap
    ) {
        super(
                statType.name(),
                name,
                format,
                "%ITEM_STAT_" + statType.name() + "%",
                ItemTags.TAG_ITEM_STAT,
                StatBonus.DATA_TYPE
        );
        this.statType = statType;
        this.cap = cap;

        ItemStats.registerDynamicStat(this);

        // Legacy keys
        this.keys.add(NamespacedKey.fromString("prorpgitems:item_stat_" + this.getId()));
        this.keys.add(NamespacedKey.fromString("prorpgitems:qrpg_item_stat_" + this.getId()));
        this.keys.add(NamespacedKey.fromString("quantumrpg:qrpg_item_stat_" + this.getId()));
    }

    @Override
    @NotNull
    public Class<StatBonus> getParameterClass() {
        return StatBonus.class;
    }

    public double getTotal(@NotNull ItemStack item, @Nullable Player player, double def) {
        List<BiFunction<Boolean, Double, Double>> bonuses = get(item, player);
        double value =
                bonuses.isEmpty() ? def : BonusCalculator.SIMPLE_FULL.apply(0D, bonuses);
        return value;
    }

    public double getTotal(@NotNull ItemStack item, @Nullable Player player) {
        return getTotal(item, player, 0);
    }

    @NotNull
    public List<BiFunction<Boolean, Double, Double>> get(@NotNull ItemStack item, @Nullable Player player) {
        List<BiFunction<Boolean, Double, Double>> bonuses = new ArrayList<>();

        double  base    = 0;
        double  percent = 0;
        boolean has     = false;

        // Get from old format
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            for (NamespacedKey key : this.keys) {
                if (container.has(key, PersistentDataType.DOUBLE)) {
                    Double value = container.get(key, PersistentDataType.DOUBLE);
                    if (value != null) {
                        base += value;
                        has = true;
                        break;
                    }
                }
            }
        }

        for (StatBonus bonus : this.getAllRaw(item)) {
            if (!bonus.meetsRequirement(player)) continue;
            double[] value = bonus.getValue();
            if (value.length == 1 && bonus.isPercent()) {
                percent += value[0];
            } else {
                base += value[0];
                has = true;
            }
        }

        if (has) {
            double finalBase = base;
            bonuses.add((isPercent, input) -> isPercent ? input : input + finalBase);
            double finalPercent = percent;
            bonuses.add((isPercent, input) -> isPercent ? input + finalPercent : input);
        }

        // Support for Gems adding values.
        GemManager gems = Divinity.getInstance().getModuleCache().getGemManager();
        if (gems != null) {
            for (Map.Entry<GemManager.Gem, Integer> e : gems.getItemSockets(item)) {
                BonusMap bMap = e.getKey().getBonusMap(e.getValue());
                if (bMap == null) continue;

                BiFunction<Boolean, Double, Double> bif = bMap.getBonus(this);
                if (bif == null) continue;

                bonuses.add(bif);
            }
        }

        {
            StatBonus baseLine = this.getRaw(meta, 0);
            if (baseLine != null && baseLine.isBaseStat()) { // Is there a base stat?
                // Support for Refined attributes.
                RefineManager refine = Divinity.getInstance().getModuleCache().getRefineManager();
                if (refine != null && has) {
                    bonuses.add(refine.getRefinedBonus(item, this));
                }
            }
        }

        return bonuses;
    }

    public static double getDefaultAttackSpeed(@NotNull ItemStack item, double def) {
        double value = getDefaultAttackSpeed(item);
        return value == 0 ? def : value;
    }

    public static double getDefaultAttackSpeed(@NotNull ItemStack item) {
        return VersionManager.getNms().getDefaultSpeed(item);
    }

    @Override
    @NotNull
    public SimpleStat.Type getType() {
        return this.statType;
    }

    @Override
    public double getCapability() {
        return this.cap;
    }

    @Override
    public void setCapability(double cap) {
        this.cap = cap;
    }

    @Override
    @NotNull
    public ItemType getItemType() {
        return this.statType.getItemType();
    }

    @Override
    public boolean isPercent() {
        return this.statType.isPercent();
    }

    @Override
    public boolean canBeNegative() {
        return this.statType.canBeNegative();
    }

    @Override
    @Deprecated
    public boolean isMainItem(@NotNull ItemStack item) {
        if (this.isPercent() || this.getItemType() == ItemType.BOTH) return true;

        if (this.getItemType() == ItemType.ARMOR && ItemUtils.isArmor(item)) return true;
        return this.getItemType() == ItemType.WEAPON && ItemUtils.isWeapon(item);
    }

    @Override
    @Nullable
    public Type getDependStat() {
        switch (this.statType) {
            case BLOCK_RATE: {
                return Type.BLOCK_DAMAGE;
            }
            case BLOCK_DAMAGE: {
                return Type.BLOCK_RATE;
            }
            case CRITICAL_RATE: {
                return Type.CRITICAL_DAMAGE;
            }
            case CRITICAL_DAMAGE: {
                return Type.CRITICAL_RATE;
            }
            default: {
                return null;
            }
        }
    }

    @Override
    @NotNull
    public String formatValue(@NotNull ItemStack item, StatBonus statBonus) {
        boolean isBaseAttack = this.statType == Type.BASE_ATTACK_SPEED;
        double  val          = this.fineValue(statBonus.getValue()[0]);
        if (val == 0 && !isBaseAttack) return "";

        boolean bonus = !this.isMainItem(item);
        String  sVal  = NumberUT.format(val);

        boolean baseBonus     = isBaseAttack && ItemUtils.isArmor(item);
        boolean baseAttackAdd = isBaseAttack && baseBonus;

        if (baseAttackAdd || !isBaseAttack && (this.canBeNegative() || bonus)) {
            sVal = (val > 0 ? EngineCfg.LORE_CHAR_POSITIVE : EngineCfg.LORE_CHAR_NEGATIVE) + sVal;
        }
        if (this.isPercent()) {
            sVal += EngineCfg.LORE_CHAR_PERCENT;
        } else {
            if (this.statType == Type.CRITICAL_DAMAGE) sVal += EngineCfg.LORE_CHAR_MULTIPLIER;
        }

        if (statBonus.isBaseStat()) { // Apply refines
            RefineManager refine = Divinity.getInstance().getModuleCache().getRefineManager();
            if (refine != null) sVal += refine.getFormatLoreStat(item, this, statBonus.getValue()[0]);
        }
        return sVal;
    }

    @Override
    @NotNull
    public ItemStack updateItem(@Nullable Player p, @NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Replace legacy format
        for (NamespacedKey key : this.keys) {
            if (container.has(key, PersistentDataType.DOUBLE)) {
                Double value = Objects.requireNonNull(container.get(key, PersistentDataType.DOUBLE));
                add(item, new StatBonus(new double[]{value}, false, null), -1, -1);
                meta = item.getItemMeta();
                break;
            }
        }

        int amount = this.getAmount(item);
        if (amount == 0) return item;
        List<String> lore = meta.getLore();
        if (lore == null) return item;

        for (int i = 0; i < amount; i++) {
            int    loreIndex = -1;
            String metaId    = "";
            for (NamespacedKey key : this.keys) {
                metaId = key.getKey() + i;
                loreIndex = ItemUT.getLoreIndex(item, metaId);
                if (loreIndex >= 0) break;
            }
            if (loreIndex < 0) continue;

            @Nullable StatBonus arr = this.getRaw(item, i);
            if (arr == null) continue;
            String formatNew = this.getFormat(p, item, arr);
            lore.set(loreIndex, formatNew);
            meta.setLore(lore);
            item.setItemMeta(meta);
            ItemUT.addLoreTag(item, metaId, formatNew);
        }

        return item;
    }

    @Override
    @NotNull
    public String getFormat(@Nullable Player p, @NotNull ItemStack item, @NotNull StatBonus value) {
        StatBonus.Condition<?> condition = value.getCondition();
        return StringUT.colorFix(super.getFormat(item, value)
                .replace("%condition%", condition == null || !EngineCfg.LORE_STYLE_REQ_USER_DYN_UPDATE
                        ? ""
                        : condition.getFormat(p, item)));
    }

    public enum ItemType {
        ARMOR,
        WEAPON,
        BOTH,
    }
}
