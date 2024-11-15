package studio.magemonkey.divinity.stats.items.attributes;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.util.*;
import studio.magemonkey.codex.util.actions.ActionManipulator;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.modules.list.gems.GemManager;
import studio.magemonkey.divinity.modules.list.gems.GemManager.Gem;
import studio.magemonkey.divinity.modules.list.refine.RefineManager;
import studio.magemonkey.divinity.stats.bonus.BonusCalculator;
import studio.magemonkey.divinity.stats.bonus.BonusMap;
import studio.magemonkey.divinity.stats.bonus.StatBonus;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.ItemTags;
import studio.magemonkey.divinity.stats.items.api.DuplicableItemLoreStat;
import studio.magemonkey.divinity.stats.items.api.DynamicStat;
import studio.magemonkey.divinity.utils.ItemUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;

public class DamageAttribute extends DuplicableItemLoreStat<StatBonus> implements DynamicStat<StatBonus> {

    private final ActionManipulator   actionEngine;
    private final Set<String>         attachedDamageCauses;
    private final Map<String, Double> biomeModifier;
    private final Map<String, Double> entityTypeModifier;
    private final Map<String, Double> mythicFactionModifier;

    @Getter
    private int              priority;
    private DefenseAttribute defenseAttached;

    public DamageAttribute(
            @NotNull String id,
            @NotNull String name,
            @NotNull String format,
            int priority,
            @NotNull ActionManipulator actionEngine,
            @NotNull Set<String> attachedDamageCauses,
            @NotNull Map<String, Double> biome,
            @NotNull Map<String, Double> entityTypeModifier,
            @NotNull Map<String, Double> mythicFactionModifier
    ) {
        super(id, name, format, "%DAMAGE_" + id + "%", ItemTags.TAG_ITEM_DAMAGE, StatBonus.DATA_TYPE);
        this.priority = priority;
        this.actionEngine = actionEngine;
        this.attachedDamageCauses = attachedDamageCauses;
        this.biomeModifier = biome;
        this.entityTypeModifier = entityTypeModifier;
        this.mythicFactionModifier = mythicFactionModifier;
        this.defenseAttached = null;

        ItemStats.registerDynamicStat(this);

        // Legacy keys
        this.keys.add(NamespacedKey.fromString("prorpgitems:item_damage_" + this.getId()));
        this.keys.add(NamespacedKey.fromString("prorpgitems:qrpg_item_damage_" + this.getId()));
        this.keys.add(NamespacedKey.fromString("quantumrpg:qrpg_item_damage_" + this.getId()));
    }

    @Override
    @NotNull
    public Class<StatBonus> getParameterClass() {
        return StatBonus.class;
    }

    public boolean isDefault() {
        return this.equals(ItemStats.getDamageByDefault());
    }

    public boolean isAttached(@NotNull DamageCause cause) {
        return this.attachedDamageCauses.contains(cause.name())
                || this.attachedDamageCauses.contains(JStrings.MASK_ANY);
    }

    public boolean isAttached(@NotNull DefenseAttribute def) {
        return this.defenseAttached != null && this.defenseAttached.getId().equalsIgnoreCase(def.getId());
    }

    public void setAttachedDefense(@Nullable DefenseAttribute defense) {
        this.defenseAttached = defense;
    }

    @Nullable
    public DefenseAttribute getAttachedDefense() {
        return this.defenseAttached;
    }

    public static double getVanillaDamage(@NotNull ItemStack item) {
        return Divinity.getInstance().getPMS().getDefaultDamage(item);
    }

    public double[] getTotal(@NotNull ItemStack item, @Nullable Player player) {
        return BonusCalculator.RANGE_FULL.apply(new double[]{0, 0}, get(item, player));
    }

    @NotNull
    public List<BiFunction<Boolean, double[], double[]>> get(@NotNull ItemStack item, @Nullable Player player) {
        List<BiFunction<Boolean, double[], double[]>> bonuses = new ArrayList<>();
        double[]                                      base    = new double[]{0, 0};
        double                                        percent = 0;
        boolean                                       has     = false;

        // Get from old format
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            for (NamespacedKey key : this.keys) {
                if (container.has(key, DataUT.DOUBLE_ARRAY)) {
                    double[] value = container.get(key, DataUT.DOUBLE_ARRAY);
                    if (value != null) {
                        base[0] += value[0];
                        base[1] += value[1];
                        has = true;
                        break;
                    }
                }
            }
        }

        for (StatBonus bonus : this.getAllRaw(item)) {
            if (!bonus.meetsRequirement(player)) continue;
            double[] value = bonus.getValue();
            if (value.length == 1) {
                if (bonus.isPercent()) {
                    percent += value[0];
                } else {
                    base[0] += value[0];
                    base[1] += value[0];
                    has = true;
                }
            } else {
                base[0] += value[0];
                base[1] += value[1];
                has = true;
            }
        }

        base[0] = Math.max(0, base[0]);
        base[1] = Math.max(base[0], base[1]);

        if (base[1] == 0 && ItemUtils.isWeapon(item) && this.isDefault() && ItemStats.getDamages().stream()
                .filter(damageAttribute -> !damageAttribute.isDefault())
                .noneMatch(damageAttribute -> ItemStats.hasDamage(item, player, damageAttribute))) {
            base[0] = base[1] = DamageAttribute.getVanillaDamage(item);
            has = true;
        }

        bonuses.add((isPercent, input) -> isPercent ? input : (
                input.length == 2
                        ? new double[]{input[0] + base[0], input[1] + base[1]}
                        : new double[]{input[0] + base[0], input[0] + base[1]}));
        {
            double finalPercent = percent;
            bonuses.add((isPercent, input) -> isPercent ? new double[]{input[0] + finalPercent} : input);
        }

        {
            StatBonus baseLine = this.getRaw(meta, 0);
            if (baseLine != null && baseLine.isBaseStat()) { // Is there a base stat?
                // Support for Refined attributes.
                RefineManager refineManager = Divinity.getInstance().getModuleCache().getRefineManager();
                if (refineManager != null && has) {
                    BiFunction<Boolean, Double, Double> refineManagerBonus = refineManager.getRefinedBonus(item, this);
                    bonuses.add((isPercent, input) ->
                            input.length == 2
                                    ? new double[]{
                                    refineManagerBonus.apply(isPercent, input[0]),
                                    refineManagerBonus.apply(isPercent, input[1])}
                                    : new double[]{
                                            refineManagerBonus.apply(isPercent, input[0])});
                }
            }
        }

        // Support for filled socket Gems.
        GemManager gems = Divinity.getInstance().getModuleCache().getGemManager();
        if (gems != null) {
            for (Entry<Gem, Integer> e : gems.getItemSockets(item)) {
                BonusMap bMap = e.getKey().getBonusMap(e.getValue());
                if (bMap == null) continue;
                BiFunction<Boolean, Double, Double> gemBonus = bMap.getBonus(this);
                if (gemBonus == null) continue;
                bonuses.add((isPercent, input) ->
                        input.length == 2
                                ? new double[]{
                                gemBonus.apply(isPercent, input[0]),
                                gemBonus.apply(isPercent, input[1])}
                                : new double[]{
                                        gemBonus.apply(isPercent, input[0])});
            }
        }

        return bonuses;
    }

    @NotNull
    public ActionManipulator getHitActions() {
        return this.actionEngine;
    }

    @NotNull
    public Map<String, Double> getBiomeDamageModifiers() {
        return this.biomeModifier;
    }

    public double getDamageModifierByBiome(@NotNull Biome b) {
        return this.biomeModifier.getOrDefault(EnumUT.getName(b), 1D);
    }

    public double getDamageModifierByEntityType(@NotNull Entity e) {
        return this.entityTypeModifier.getOrDefault(e.getType().name(), 1D);
    }

    public double getDamageModifierByMythicFaction(@Nullable String faction) {
        if (faction == null) return 1D;
        return this.mythicFactionModifier.getOrDefault(faction.toLowerCase(), 1D);
    }

    @Override
    @NotNull
    public String formatValue(@NotNull ItemStack item, @NotNull StatBonus value) {
        double[] array = value.getValue();
        String   sVal;
        if (array.length == 1) {
            sVal = NumberUT.format(array[0]);
            if (value.isPercent()) {
                sVal += EngineCfg.LORE_CHAR_PERCENT;
            } else if (value.isBaseStat()) {
                RefineManager refine = Divinity.getInstance().getModuleCache().getRefineManager();
                if (refine != null) sVal += refine.getFormatLoreStat(item, this, array[0]);
            }
            sVal = EngineCfg.LORE_STYLE_DAMAGE_FORMAT_SINGLE.replace("%value%", sVal);
        } else {
            String sMin = NumberUT.format(array[0]);
            String sMax = NumberUT.format(array[1]);
            if (value.isBaseStat()) { // Apply refines
                RefineManager refine = Divinity.getInstance().getModuleCache().getRefineManager();
                if (refine != null) {
                    sMin += refine.getFormatLoreStat(item, this, array[0]);
                    sMax += refine.getFormatLoreStat(item, this, array[1]);
                }
            }
            sVal = EngineCfg.LORE_STYLE_DAMAGE_FORMAT_RANGE
                    .replace("%min%", sMin)
                    .replace("%max%", sMax);
        }
        return sVal;
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
            } else if (container.has(key, DataUT.DOUBLE_ARRAY)) {
                double[] value = Objects.requireNonNull(container.get(key, DataUT.DOUBLE_ARRAY));
                add(item, new StatBonus(value, false, null), -1, -1);
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
}
