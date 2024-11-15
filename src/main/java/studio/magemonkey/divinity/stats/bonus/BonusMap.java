package studio.magemonkey.divinity.stats.bonus;

import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.hooks.EHook;
import studio.magemonkey.divinity.hooks.external.FabledHook;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.api.ItemLoreStat;
import studio.magemonkey.divinity.stats.items.attributes.*;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class BonusMap {

    private final Map<ItemLoreStat<?>, BiFunction<Boolean, Double, Double>> bonus;

    public BonusMap() {
        this.bonus = new HashMap<>();
    }

    public BonusMap(@NotNull BonusMap from) {
        this();
        BonusMap.this.bonus.putAll(from.bonus);
    }

    public void clear() {
        this.bonus.clear();
    }

    public boolean isEmpty() {
        return this.bonus.isEmpty();
    }

    @NotNull
    public Map<ItemLoreStat<?>, BiFunction<Boolean, Double, Double>> getBonuses() {
        return this.bonus;
    }

    @NotNull
    public Map<SimpleStat, BiFunction<Boolean, Double, Double>> getStatBonuses() {
        return this.bonus.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof SimpleStat)
                .collect(Collectors.toMap(key -> (SimpleStat) key.getKey(), Map.Entry::getValue, (has, add) -> has));
    }

    @NotNull
    public Map<DamageAttribute, BiFunction<Boolean, Double, Double>> getDamageBonuses() {
        return this.bonus.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof DamageAttribute)
                .collect(Collectors.toMap(key -> (DamageAttribute) key.getKey(),
                        Map.Entry::getValue,
                        (has, add) -> has));
    }

    @NotNull
    public Map<DefenseAttribute, BiFunction<Boolean, Double, Double>> getDefenseBonuses() {
        return this.bonus.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof DefenseAttribute)
                .collect(Collectors.toMap(key -> (DefenseAttribute) key.getKey(),
                        Map.Entry::getValue,
                        (has, add) -> has));
    }

    @NotNull
    public Map<FabledAttribute, BiFunction<Boolean, Double, Double>> getFabledAttributeBonuses() {
        return this.bonus.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof FabledAttribute)
                .collect(Collectors.toMap(key -> (FabledAttribute) key.getKey(),
                        Map.Entry::getValue,
                        (has, add) -> has));
    }

    @NotNull
    public Map<AmmoAttribute, BiFunction<Boolean, Double, Double>> getAmmoBonuses() {
        return this.bonus.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof AmmoAttribute)
                .collect(Collectors.toMap(key -> (AmmoAttribute) key.getKey(), Map.Entry::getValue, (has, add) -> has));
    }

    @NotNull
    public Map<HandAttribute, BiFunction<Boolean, Double, Double>> getHandBonuses() {
        return this.bonus.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof HandAttribute)
                .collect(Collectors.toMap(key -> (HandAttribute) key.getKey(), Map.Entry::getValue, (has, add) -> has));
    }

    @Nullable
    public BiFunction<Boolean, Double, Double> getBonus(@NotNull ItemLoreStat<?> stat) {
        return this.bonus.getOrDefault(stat, null);
    }

    public void loadStats(@NotNull JYML cfg, @NotNull String path) {
        for (String id : cfg.getSection(path)) {
            SimpleStat.Type dt = TypedStat.Type.getByName(id);
            if (dt == null) continue;

            ItemLoreStat<?> mainStat = (ItemLoreStat<?>) ItemStats.getStat(dt);

            String sVal = cfg.getString(path + "." + id);
            if (sVal == null) continue;

            String[] split = sVal.split("%", 2);
            boolean  perc  = split.length == 2 && split[1].isEmpty();
            double   val   = StringUT.getDouble(split[0], 0, true);

            BiFunction<Boolean, Double, Double> func = (isBonus, apply) -> perc == isBonus ? apply + val : apply;
            this.bonus.put(mainStat, func);
        }
    }

    public void loadDamages(@NotNull JYML cfg, @NotNull String path) {
        for (String id : cfg.getSection(path)) {
            DamageAttribute dt = ItemStats.getDamageById(id);
            if (dt == null) continue;

            String sVal = cfg.getString(path + "." + id);
            if (sVal == null) continue;

            String[] split = sVal.split("%", 2);
            boolean  perc  = split.length == 2 && split[1].isEmpty();
            double   val   = StringUT.getDouble(split[0], 0, true);

            BiFunction<Boolean, Double, Double> func = (bonus, apply) -> perc == bonus ? apply + val : apply;
            this.bonus.put(dt, func);
        }
    }

    public void loadDefenses(@NotNull JYML cfg, @NotNull String path) {
        for (String id : cfg.getSection(path)) {
            DefenseAttribute dt = ItemStats.getDefenseById(id);
            if (dt == null) continue;

            String sVal = cfg.getString(path + "." + id);
            if (sVal == null) continue;

            String[] split = sVal.split("%", 2);
            boolean  perc  = split.length == 2 && split[1].isEmpty();
            double   val   = StringUT.getDouble(split[0], 0, true);

            BiFunction<Boolean, Double, Double> func = (bonus, apply) -> perc == bonus ? apply + val : apply;
            this.bonus.put(dt, func);
        }
    }

    public void loadFabledAttributes(@NotNull JYML cfg, @NotNull String path) {
        for (String id : cfg.getSection(path)) {
            FabledHook fabledHook = (FabledHook) Divinity.getInstance().getHook(EHook.SKILL_API);
            if (fabledHook == null) continue;
            Collection<FabledAttribute> attributes = fabledHook.getAttributes();

            FabledAttribute stat = attributes.stream()
                    .filter(attribute -> attribute.getId().equalsIgnoreCase(id))
                    .findAny().orElse(null);
            if (stat == null) continue;

            String sVal = cfg.getString(path + "." + id);
            if (sVal == null) continue;

            String[] split = sVal.split("%", 2);
            boolean  perc  = split.length == 2 && split[1].isEmpty();
            double   val   = StringUT.getDouble(split[0], 0, true);

            BiFunction<Boolean, Double, Double> func = (isBonus, apply) -> perc == isBonus ? apply + val : apply;
            this.bonus.put(stat, func);
        }
    }

    public void loadAmmo(@NotNull JYML cfg, @NotNull String path) {
        for (String id : cfg.getSection(path)) {
            AmmoAttribute stat;
            try {
                stat = ItemStats.getAmmo(AmmoAttribute.Type.valueOf(id.toUpperCase()));
                if (stat == null) continue;
            } catch (IllegalArgumentException e) {
                continue;
            }

            String sVal = cfg.getString(path + "." + id);
            if (sVal == null) continue;

            String[] split = sVal.split("%", 2);
            boolean  perc  = split.length == 2 && split[1].isEmpty();
            double   val   = StringUT.getDouble(split[0], 0, true);

            BiFunction<Boolean, Double, Double> func = (isBonus, apply) -> perc == isBonus ? apply + val : apply;
            this.bonus.put(stat, func);
        }
    }

    public void loadHands(@NotNull JYML cfg, @NotNull String path) {
        for (String id : cfg.getSection(path)) {
            HandAttribute stat;
            try {
                stat = ItemStats.getHand(HandAttribute.Type.valueOf(id.toUpperCase()));
                if (stat == null) continue;
            } catch (IllegalArgumentException e) {
                continue;
            }

            String sVal = cfg.getString(path + "." + id);
            if (sVal == null) continue;

            String[] split = sVal.split("%", 2);
            boolean  perc  = split.length == 2 && split[1].isEmpty();
            double   val   = StringUT.getDouble(split[0], 0, true);

            BiFunction<Boolean, Double, Double> func = (isBonus, apply) -> perc == isBonus ? apply + val : apply;
            this.bonus.put(stat, func);
        }
    }

    @NotNull
    public String replacePlaceholders(@NotNull String str) {
        for (Map.Entry<ItemLoreStat<?>, BiFunction<Boolean, Double, Double>> e : this.bonus.entrySet()) {
            double valRaw   = e.getValue().apply(false, 0D);
            double valBonus = e.getValue().apply(true, 0D);

            double valTotal  = valRaw != 0D ? valRaw : valBonus;
            String valFormat = NumberUT.format(valTotal);
            String valStr    = valBonus != 0D ? valFormat += EngineCfg.LORE_CHAR_PERCENT : valFormat;

            str = str.replace(e.getKey().getPlaceholder(), valStr);
        }
        return str;
    }
}
