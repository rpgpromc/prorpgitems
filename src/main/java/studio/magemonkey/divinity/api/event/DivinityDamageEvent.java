package studio.magemonkey.divinity.api.event;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.api.events.ICancellableEvent;
import studio.magemonkey.divinity.manager.damage.DamageMeta;
import studio.magemonkey.divinity.modules.list.arrows.ArrowManager.QArrow;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.items.attributes.DamageAttribute;
import studio.magemonkey.divinity.stats.items.attributes.DefenseAttribute;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

public abstract class DivinityDamageEvent extends ICancellableEvent {

    protected LivingEntity victim;
    protected LivingEntity damager;
    protected Projectile   projectile;

    protected Map<DamageAttribute, Double>  damageMap;
    protected Map<DefenseAttribute, Double> defenseMap;
    protected Map<SimpleStat.Type, Double>  damagerItemStatsMap;

    protected DamageMeta        meta;
    protected EntityDamageEvent eventOrig;

    protected boolean cancelled = false;

    @Getter
    @Setter
    protected boolean exempt = false;

    public DivinityDamageEvent(
            @NotNull LivingEntity zertva,
            @NotNull EntityDamageEvent eventOrig,
            @NotNull DamageMeta meta
    ) {
        this(zertva, null, eventOrig, meta);
    }

    public DivinityDamageEvent(
            @NotNull LivingEntity zertva,
            @Nullable LivingEntity damager,
            @NotNull EntityDamageEvent eventOrig,
            @NotNull DamageMeta meta
    ) {
        this(zertva, damager, null, eventOrig, meta);
    }

    public DivinityDamageEvent(
            @NotNull LivingEntity zertva,
            @Nullable LivingEntity damager,
            @Nullable Projectile projectile,
            @NotNull EntityDamageEvent eventOrig,
            @NotNull DamageMeta meta
    ) {
        this.victim = zertva;
        this.damager = damager;
        this.projectile = projectile;

        this.damageMap = new HashMap<>();
        this.defenseMap = new HashMap<>();
        this.damagerItemStatsMap = new HashMap<>();

        this.meta = meta;
        this.eventOrig = eventOrig;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        this.eventOrig.setCancelled(this.isCancelled());
    }

    @NotNull
    public final DamageCause getCause() {
        return this.getOriginalEvent().getCause();
    }

    @NotNull
    public EntityDamageEvent getOriginalEvent() {
        return this.eventOrig;
    }

    @NotNull
    public LivingEntity getVictim() {
        return this.victim;
    }

    @Nullable
    public LivingEntity getDamager() {
        return this.damager;
    }

    @NotNull
    public DamageMeta getDamageMeta() {
        return this.meta;
    }

    @NotNull
    public Map<DamageAttribute, Double> getDamageMap() {
        return damageMap;
    }

    public void computeDamage(@NotNull DoubleUnaryOperator operator) {
        this.computeDamage(this.damageMap.keySet(), operator);
    }

    public void computeDamage(@NotNull DamageAttribute damage, @NotNull DoubleUnaryOperator operator) {
        this.computeDamage(Sets.newHashSet(damage), operator);
    }

    public void computeDamage(@NotNull Collection<DamageAttribute> damages, @NotNull DoubleUnaryOperator operator) {
        damages.forEach(dmgApp -> this.damageMap.compute(dmgApp,
                (dmgApp2, dmg) -> operator.applyAsDouble(this.damageMap.computeIfAbsent(dmgApp, dmg2 -> 0D))));
        this.damageMap.values().removeIf(dmg -> dmg == 0D);
    }

    @NotNull
    public Map<DefenseAttribute, Double> getDefenseMap() {
        return defenseMap;
    }

    public void computeDefense(@NotNull DoubleUnaryOperator operator) {
        this.computeDefense(this.defenseMap.keySet(), operator);
    }

    public void computeDefense(@NotNull DefenseAttribute defense, @NotNull DoubleUnaryOperator operator) {
        this.computeDefense(Sets.newHashSet(defense), operator);
    }

    public void computeDefense(@NotNull Collection<DefenseAttribute> defenses, @NotNull DoubleUnaryOperator operator) {
        defenses.forEach(defApp -> this.defenseMap.compute(defApp,
                (defKey, defVal) -> operator.applyAsDouble(this.defenseMap.computeIfAbsent(defApp, defVal2 -> 0D))));
        this.defenseMap.values().removeIf(dmg -> dmg == 0D);
    }

    @NotNull
    public Map<SimpleStat.Type, Double> getDamagerItemStatsMap() {
        return damagerItemStatsMap;
    }

    public double getDamagerItemStat(@NotNull SimpleStat.Type type) {
        return this.getDamagerItemStatsMap().getOrDefault(type, 0D);
    }

    @Nullable
    public ItemStack getWeapon() {
        return this.getDamageMeta().getWeapon();
    }

    @Nullable
    public Projectile getProjectile() {
        return this.projectile;
    }

    @Nullable
    public QArrow getArrow() {
        return this.getDamageMeta().getArrow();
    }

    public final boolean isProjectile() {
        return this.getProjectile() != null;
    }

    @NotNull
    public final EntityStats getVictimStats() {
        return EntityStats.get(this.getVictim());
    }

    @Nullable
    public final EntityStats getDamagerStats() {
        if (this.getDamager() == null) return null;

        return EntityStats.get(this.damager);
    }

    /**
     * Called at the start of Damage Event, before ANY calculations.
     * This is the first QuantumDamageEvent instance.
     */
    public static class Start extends DivinityDamageEvent {

        public Start(
                @NotNull LivingEntity zertva,
                @Nullable LivingEntity damager,
                @Nullable Projectile projectile,
                @NotNull Map<DamageAttribute, Double> damageMap,
                @NotNull Map<DefenseAttribute, Double> defenseMap,
                @NotNull Map<SimpleStat.Type, Double> statsMap,
                @NotNull EntityDamageEvent eventOrig,
                @NotNull DamageMeta meta,
                boolean exempt
        ) {
            super(zertva, damager, projectile, eventOrig, meta);
            this.damageMap = damageMap;
            this.defenseMap = defenseMap;
            this.damagerItemStatsMap = statsMap;
            this.exempt = exempt;
        }
    }

    /**
     * Called at the start of Damage Event, before damage calculations.
     * This is the second QuantumDamageEvent instance.
     */
    public static class Pre extends DivinityDamageEvent {

        public Pre(
                @NotNull LivingEntity zertva,
                @Nullable LivingEntity damager,
                @Nullable Projectile projectile,
                @NotNull EntityDamageEvent eventOrig,
                @NotNull DamageMeta meta
        ) {
            super(zertva, damager, projectile, eventOrig, meta);
        }
    }

    public static class Dodge extends DivinityDamageEvent {

        public Dodge(
                @NotNull LivingEntity zertva,
                @Nullable LivingEntity damager,
                @Nullable Projectile projectile,
                @NotNull EntityDamageEvent eventOrig,
                @NotNull DamageMeta meta
        ) {
            super(zertva, damager, projectile, eventOrig, meta);
        }

        // Override to avoid cancel original event
        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
    }

    /**
     * Called at the end of Damage Event, before return the damage value.
     * This is the latest QuantumDamageEvent instance.
     */
    public static class Exit extends DivinityDamageEvent {

        public Exit(
                @NotNull LivingEntity zertva,
                @Nullable LivingEntity damager,
                @Nullable Projectile projectile,
                @NotNull EntityDamageEvent eventOrig,
                @NotNull DamageMeta meta
        ) {
            super(zertva, damager, projectile, eventOrig, meta);
        }
    }
}
