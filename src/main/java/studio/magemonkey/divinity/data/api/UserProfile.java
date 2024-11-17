package studio.magemonkey.divinity.data.api;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.divinity.manager.effects.buffs.SavedBuff;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;
import studio.magemonkey.divinity.stats.items.api.ItemLoreStat;
import studio.magemonkey.divinity.stats.items.attributes.DamageAttribute;
import studio.magemonkey.divinity.stats.items.attributes.DefenseAttribute;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class UserProfile {

    private final String  name;
    private       boolean isDefault;

    private final Set<SavedBuff> buffDamage;
    private final Set<SavedBuff> buffDefense;
    private final Set<SavedBuff> buffStats;

    private UserEntityNamesMode namesMode;
    @Setter
    @Getter
    private boolean             hideHelmet;

    private UserClassData cData;
    private long          cCooldown;

    public UserProfile() {
        this(JStrings.DEFAULT, true);
    }

    public UserProfile(@NotNull String profileId, boolean isDefault) {
        this(
                profileId,  // Name
                isDefault,            // Is Default

                new HashSet<>(), // Damage Buffs List
                new HashSet<>(), // Defense Buffs List
                new HashSet<>(), // Item Stat Buffs List

                UserEntityNamesMode.DEFAULT,
                false,

                null, // Class Data
                0L // Class Selection Cooldown
        );
    }

    public UserProfile(
            @NotNull String name,
            boolean isDefault,

            @NotNull Set<SavedBuff> buffDamage,
            @NotNull Set<SavedBuff> buffDefense,
            @NotNull Set<SavedBuff> buffStats,

            @NotNull UserEntityNamesMode namesMode,
            boolean hideHelmet,

            @Nullable UserClassData cData,
            long cCooldown
    ) {
        this.name = name.toLowerCase();
        this.setDefault(isDefault);

        this.buffDamage = buffDamage;
        this.buffDefense = buffDefense;
        this.buffStats = buffStats;

        this.setNamesMode(namesMode);
        this.setHideHelmet(hideHelmet);

        this.setClassData(cData);
        if (this.cData != null) {
            this.cData.updateData();
        }
        this.setClassSelectionCooldown(cCooldown);
    }

    @NotNull
    public String getIdName() {
        return this.name;
    }

    public boolean isDefault() {
        return this.isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @NotNull
    public BiFunction<Boolean, Double, Double> getDamageBuff(@NotNull DamageAttribute dmgAtt) {
        return this.getBuff(dmgAtt, this.getDamageBuffs());
    }

    @NotNull
    public BiFunction<Boolean, Double, Double> getDefenseBuff(@NotNull DefenseAttribute defAtt) {
        return this.getBuff(defAtt, this.getDefenseBuffs());
    }

    @NotNull
    public BiFunction<Boolean, Double, Double> getItemStatBuff(@NotNull SimpleStat stat) {
        return this.getBuff(stat, this.getItemStatBuffs());
    }

    @NotNull
    public BiFunction<Boolean, Double, Double> getBuff(@NotNull ItemLoreStat<?> stat) {
        if (stat instanceof DamageAttribute) {
            return this.getDamageBuff((DamageAttribute) stat);
        }
        if (stat instanceof DefenseAttribute) {
            return this.getDefenseBuff((DefenseAttribute) stat);
        }
        if (stat instanceof SimpleStat) {
            return this.getItemStatBuff((SimpleStat) stat);
        }
        return (isBonus, result) -> result;
    }

    @NotNull
    private BiFunction<Boolean, Double, Double> getBuff(@NotNull ItemLoreStat<?> stat, @NotNull Set<SavedBuff> list) {
        List<SavedBuff> buffs = list.stream()
                .filter(buff -> buff.getStatId().equalsIgnoreCase(stat.getId()))
                .collect(Collectors.toList());

        if (buffs.isEmpty()) {
            return (isBonus, result) -> result;
        }

        return (isBonus, result) -> {
            double modifier = buffs.stream()
                    .filter(buff -> isBonus == buff.isModifier())
                    .mapToDouble(SavedBuff::getAmount)
                    .sum();

            return result + modifier;
        };
    }

    @NotNull
    public Set<SavedBuff> getDamageBuffs() {
        this.validateBuffs(this.buffDamage);
        return this.buffDamage;
    }

    @NotNull
    public Set<SavedBuff> getDefenseBuffs() {
        this.validateBuffs(this.buffDefense);
        return this.buffDefense;
    }

    @NotNull
    public Set<SavedBuff> getItemStatBuffs() {
        this.validateBuffs(this.buffStats);
        return this.buffStats;
    }

    public void addDamageBuff(@NotNull SavedBuff buff) {
        this.addBuff(buff, this.getDamageBuffs());
    }

    public void addDefenseBuff(@NotNull SavedBuff buff) {
        this.addBuff(buff, this.getDefenseBuffs());
    }

    public void addStatBuff(@NotNull SavedBuff buff) {
        this.addBuff(buff, this.getItemStatBuffs());
    }

    public void removeDamageBuff(@NotNull String statId) {
        this.removeBuff(statId, this.getDamageBuffs());
    }

    public void removeDefenseBuff(@NotNull String statId) {
        this.removeBuff(statId, this.getDefenseBuffs());
    }

    public void removeStatBuff(@NotNull String statId) {
        this.removeBuff(statId, this.getItemStatBuffs());
    }

    private void validateBuffs(@NotNull Set<SavedBuff> list) {
        list.removeIf(buff -> buff.isExpired());
    }

    private void addBuff(@NotNull SavedBuff buff, @NotNull Set<SavedBuff> to) {
        this.removeBuff(buff.getStatId(), to);
        to.add(buff);
    }

    private void removeBuff(@NotNull String statId, @NotNull Set<SavedBuff> from) {
        from.removeIf(buff -> buff.getStatId().equalsIgnoreCase(statId));
    }

    public void setClassData(@Nullable UserClassData cData) {
        this.cData = cData;
        if (this.cData == null) {
            this.setClassSelectionCooldown(0);
        }
    }

    @NotNull
    public UserEntityNamesMode getNamesMode() {
        return namesMode;
    }

    public void setNamesMode(@NotNull UserEntityNamesMode namesMode) {
        this.namesMode = namesMode;
    }

    @Nullable
    public UserClassData getClassData() {
        return this.cData;
    }

    public long getClassSelectionCooldown() {
        return this.cCooldown;
    }

    public void setClassSelectionCooldown(long cCooldown) {
        this.cCooldown = cCooldown;
    }
}
