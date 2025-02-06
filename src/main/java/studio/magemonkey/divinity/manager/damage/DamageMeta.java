package studio.magemonkey.divinity.manager.damage;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.divinity.modules.list.arrows.ArrowManager.QArrow;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.items.attributes.DamageAttribute;
import studio.magemonkey.divinity.stats.items.attributes.DefenseAttribute;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class DamageMeta {

    private final Map<DamageAttribute, Double>  damageByTypes;
    private final Map<DefenseAttribute, Double> damageDefended;
    private       ItemStack                     weapon;
    private       LivingEntity                  damager;
    private       LivingEntity                  victim;
    private       QArrow                        arrow                     = null;
    private       int                           arrowLevel                = 0;
    private       boolean                       dodge                     = false;
    private       double                        blockModifier             = 1D;
    private       double                        pveDmgModifier            = 1D;
    private       double                        pveDefModifier            = 1D;
    private       double                        directModifier            = 0D;
    private       double                        criticalModifier          = 1D;
    private       double                        penetrateModifier         = 1D;
    private       double                        enchantProtectionModifier = 1D;
    @Getter
    @Setter
    private       double                        invulnerableProtection    = 0D;
    private       String                        entityName;
    private       String                        damagerName;

    private long combatStartTime = 0L;

    public DamageMeta(
            @NotNull LivingEntity zertva,
            @Nullable LivingEntity damager,
            @Nullable ItemStack item,
            @NotNull DamageCause cause) {

        this.setWeapon(item);
        this.setDamager(damager);
        this.setVictim(zertva);
        this.damageByTypes = new HashMap<>();
        this.damageDefended = new HashMap<>();

        this.combatStartTime = System.currentTimeMillis();
    }

    public void setArrow(@Nullable QArrow arrow, int arrowLevel) {
        this.arrow = arrow;
        this.arrowLevel = arrowLevel;
    }

    @Nullable
    public QArrow getArrow() {
        return this.arrow;
    }

    public int getArrowLevel() {
        return this.arrowLevel;
    }

    @Nullable
    public ItemStack getWeapon() {
        return this.weapon;
    }

    public void setWeapon(@Nullable ItemStack weapon) {
        this.weapon = weapon;
    }

    @NotNull
    public LivingEntity getVictim() {
        return this.victim;
    }

    public void setVictim(@NotNull LivingEntity victim) {
        this.victim = victim;
        this.entityName = EntityStats.getEntityName(this.victim);
    }

    @NotNull
    public String getVictimName() {
        return this.entityName;
    }

    @Nullable
    public LivingEntity getDamager() {
        return this.damager;
    }

    public void setDamager(@Nullable LivingEntity damager) {
        this.damager = damager;
        if (this.damager != null) {
            this.damagerName = EntityStats.getEntityName(this.damager);
        }
    }

    @Nullable
    public String getDamagerName() {
        return this.damagerName;
    }

    @NotNull
    public Map<DamageAttribute, Double> getDamages() {
        return this.damageByTypes;
    }

    public double getDamage(@NotNull DamageAttribute type) {
        if (this.damageByTypes.containsKey(type)) {
            return this.damageByTypes.get(type);
        }
        return 0;
    }

    public void setDamage(@NotNull DamageAttribute dmgAtt, double amount) {
        this.damageByTypes.put(dmgAtt, Math.max(0, amount));
    }

    public double getDefendedDamage() {
        return this.damageDefended.values().stream().mapToDouble(d -> d).sum();
    }

    public double getDefendedDamage(@NotNull DamageAttribute dmgAtt) {
        DefenseAttribute defAtt = dmgAtt.getAttachedDefense();
        if (defAtt != null && this.damageDefended.containsKey(defAtt)) {
            return this.damageDefended.get(defAtt);
        }
        return 0D;
    }

    public void setDefendedDamage(@NotNull DefenseAttribute def, double amount) {
        if (amount <= 0) {
            this.damageDefended.remove(def);
            return;
        }
        this.damageDefended.put(def, amount);
    }

    public long getCombatStartTime() {
        return System.currentTimeMillis() - this.combatStartTime;
    }

    @Deprecated // TODO Engine option
    public boolean isCombatEnded() {
        return this.getCombatStartTime() > 5000L;
    }

    public double getEnchantProtectionModifier() {
        return this.enchantProtectionModifier;
    }

    public void setEnchantProtectionModifier(double epf) {
        this.enchantProtectionModifier = epf;
    }

    public void setDodge(boolean dodge) {
        this.dodge = dodge;
    }

    public double getBlockModifier() {
        return this.blockModifier;
    }

    public void setBlockModifier(double blockMod) {
        this.blockModifier = blockMod;
    }

    public double getPvEDamageModifier() {
        return this.pveDmgModifier;
    }

    public void setPvEDamageModifier(double pveDmgMod) {
        this.pveDmgModifier = pveDmgMod;
    }

    public double getPvEDefenseModifier() {
        return this.pveDefModifier;
    }

    public void setPvEDefenseModifier(double pvpDmgMod) {
        this.pveDefModifier = pvpDmgMod;
    }

    public double getPenetrateModifier() {
        return this.penetrateModifier;
    }

    public void setPenetrateModifier(double penetrateMod) {
        this.penetrateModifier = penetrateMod;
    }

    public double getCriticalModifier() {
        return this.criticalModifier;
    }

    public void setCriticalModifier(double critMod) {
        this.criticalModifier = critMod;
    }

    public double getDirectModifier() {
        return this.directModifier;
    }

    public void setDirectModifier(double directMod) {
        this.directModifier = directMod;
    }

    public boolean isCritical() {
        return this.getCriticalModifier() > 0 && this.getCriticalModifier() != 1D;
    }

    public boolean isDodged() {
        return this.dodge;
    }

    public boolean isBlocked() {
        return this.getBlockModifier() != 1D;
    }

    public double getTotalDamage() {
        boolean containsPhysical = this.damageByTypes.entrySet().stream()
                .filter(entry -> entry.getKey().getId().equals("physical")).count() >= 1;
//		double damage = this.damageByTypes.size() == 1 && containsPhysical ? 0 : -1;
        double damage = 0;
        for (Map.Entry<DamageAttribute, Double> e : this.damageByTypes.entrySet()) {
            double dmgByType = e.getValue();
            damage += dmgByType;
        }

        damage -= this.getInvulnerableProtection();
        damage = Math.round(damage * 100.0) / 100.0;

        return damage;
    }

    public void addMissingDmg(double damage) {
        double me = this.getTotalDamage();
        if (me == damage) return;

        double part = 0;
        if (me < damage) {
            part = damage - me;
        } else {
            part = -(me - damage);
        }

        // Adds missing damage for pvp/pve damage
        if (!this.damageByTypes.isEmpty()) {
            part = part / damageByTypes.size();
            for (Entry<DamageAttribute, Double> e : damageByTypes.entrySet()) {
                double val = e.getValue();
                val += part;
                damageByTypes.put(e.getKey(), val);
            }
            return;
        }
    }
}
