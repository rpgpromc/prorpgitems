package studio.magemonkey.divinity.manager.listener.object;

import org.bukkit.Material;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.VersionManager;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.codex.registry.attribute.AttributeRegistry;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.event.DivinityDamageEvent;
import studio.magemonkey.divinity.api.event.DivinityProjectileLaunchEvent;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.hooks.EHook;
import studio.magemonkey.divinity.hooks.external.FabledHook;
import studio.magemonkey.divinity.manager.damage.DamageMeta;
import studio.magemonkey.divinity.modules.list.arrows.ArrowManager;
import studio.magemonkey.divinity.modules.list.arrows.ArrowManager.QArrow;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.ProjectileStats;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.AmmoAttribute;
import studio.magemonkey.divinity.stats.items.attributes.DamageAttribute;
import studio.magemonkey.divinity.stats.items.attributes.DefenseAttribute;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;
import studio.magemonkey.divinity.utils.ItemUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class VanillaWrapperListener extends IListener<Divinity> {

    private static final String META_PROJECTILE_EVENT_FIXER = "QRPG_EVENT_FIX";

    public VanillaWrapperListener(@NotNull Divinity plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVanillaShootBow(EntityShootBowEvent e) {
        ItemStack bow = e.getBow();

        LivingEntity shooter = e.getEntity();
        Projectile   pj      = (Projectile) e.getProjectile();
        Vector       orig    = pj.getVelocity();
        double       power   = e.getForce() / 3;

        // Quick fix for "high" skeleton damage
        if (shooter instanceof Skeleton) {
            power = 0.35D;
        }

        if (bow != null) {
            AmmoAttribute ammo = ItemStats.getAmmo(bow);
            if (ammo != null && ammo.getType() != AmmoAttribute.Type.ARROW) {
                boolean bounce = pj.doesBounce(),
                        glow = pj.isGlowing(),
                        gravity = pj.hasGravity();
                pj = ammo.getProjectile(shooter);
                pj.setVelocity(orig);
                pj.setBounce(bounce);
                pj.setGlowing(glow);
                pj.setGravity(gravity);
                e.setProjectile(pj);
            }
        }
        // Prevent duplicated event call
        pj.setMetadata(META_PROJECTILE_EVENT_FIXER, new FixedMetadataValue(plugin, "true"));

        DivinityProjectileLaunchEvent eve =
                new DivinityProjectileLaunchEvent(pj, pj.getLocation(), shooter, bow, power, true);
        plugin.getPluginManager().callEvent(eve);
        if (eve.isCancelled()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVanillaProjectLaunch(ProjectileLaunchEvent e) {
        Entity e1 = e.getEntity();
        if (e1.hasMetadata(META_PROJECTILE_EVENT_FIXER)) {
            e1.removeMetadata(META_PROJECTILE_EVENT_FIXER, plugin);
            return;
        }

        LivingEntity shooter;
        ItemStack    bow   = null;
        double       power = 1D;

        if (!(e1 instanceof Projectile)) return;

        Projectile       pp = (Projectile) e1;
        ProjectileSource ps = pp.getShooter();
        if (!(ps instanceof LivingEntity)) return;

        shooter = (LivingEntity) ps;

        String          pjType = pp.getType().name();
        EntityEquipment eq     = shooter.getEquipment();
        if (eq != null) {
            // Fix main hand damage when launched from off-hand
            ItemStack off = eq.getItemInOffHand();
            if (ItemUT.isAir(off) || !off.getType().name().equalsIgnoreCase(pjType)) {
                bow = eq.getItemInMainHand();
            }
        }

        DivinityProjectileLaunchEvent eve =
                new DivinityProjectileLaunchEvent(e1, e1.getLocation(), shooter, bow, power, false);
        plugin.getPluginManager().callEvent(eve);
        if (eve.isCancelled()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVanillaDamage(EntityDamageEvent e) {
        boolean isEde = e instanceof EntityDamageByEntityEvent;
        if (isEde && plugin.getPluginManager().isPluginEnabled("Fabled")) {
            EntityDamageByEntityEvent ede        = (EntityDamageByEntityEvent) e;
            FabledHook                fabledHook = (FabledHook) this.plugin.getHook(EHook.SKILL_API);
            if (Objects.requireNonNull(fabledHook).isFakeDamage(ede)) return;
        }
//        long l1 = System.currentTimeMillis();

        Entity eVictim = e.getEntity();
        if (!(eVictim instanceof LivingEntity)) return;

        // Disable vanilla armor effect.
        if (e.isApplicable(DamageModifier.ARMOR)) {
            e.setDamage(DamageModifier.ARMOR, 0D);
        }

        double damageStart = e.getDamage();

        ItemStack  weapon     = null;
        Projectile projectile = null;

        LivingEntity damager = null;
        LivingEntity victim  = (LivingEntity) eVictim;
        DamageCause  cause   = e.getCause();

        EntityStats statsDamager = null;
        EntityStats statsVictim  = EntityStats.get(victim);

        DamageMeta meta = new DamageMeta(victim, damager, weapon, cause);
        statsVictim.setLastDamageMeta(meta);

        boolean isFullDamage = false;

        FabledHook skillApi = (FabledHook) Divinity.getInstance().getHook(EHook.SKILL_API);
        // Here only check for damager
        // to be sure to use his stats or not.
        labelFullDamage:
        if (isEde) {
            EntityDamageByEntityEvent ede = (EntityDamageByEntityEvent) e;

            // Check if damager can fight victim
            Entity edeDamager = ede.getDamager();
            if (!Hooks.canFights(edeDamager, victim)) {
                ede.setCancelled(true);
                return;
            }

            if (edeDamager instanceof LivingEntity) {
                if (cause == DamageCause.ENTITY_SWEEP_ATTACK && EngineCfg.COMBAT_DISABLE_VANILLA_SWEEP) {
                    ede.setCancelled(true);
                    return;
                }

                damager = (LivingEntity) edeDamager;

                meta.setDamager(damager);
                statsDamager = EntityStats.get(damager);
                statsDamager.setLastDamageMeta(meta);
                weapon = statsDamager.getItemInMainHand();

                if (ItemUtils.isBow(weapon) && !EngineCfg.COMBAT_BOWS_DO_FULL_MELEE_DAMAGE) {
                    break labelFullDamage;
                }
            } else if (edeDamager instanceof Projectile) {
                projectile = (Projectile) edeDamager;
                ProjectileSource shooter = projectile.getShooter();

                if (!(shooter instanceof LivingEntity)) {
                    break labelFullDamage;
                }

                // Fix extra damage from ender pearls
                if (projectile instanceof EnderPearl && shooter.equals(victim)) {
                    break labelFullDamage;
                }

                damager = (LivingEntity) shooter;
                meta.setDamager(damager);
                statsDamager = EntityStats.get(damager);
                statsDamager.setLastDamageMeta(meta);
                weapon = ProjectileStats.getSrcWeapon(projectile);

                // Applying custom arrow object to damage meta,
                // if it's present, to use additional stats from it.
                ArrowManager arr = plugin.getModuleCache().getArrowManager();
                if (arr != null) {
                    QArrow arrow = arr.getArrow(projectile);
                    int    level = arr.getArrowLevel(projectile);
                    meta.setArrow(arrow, level);
                    if (arrow != null) {
                        statsDamager.setArrowBonus(arrow, level);
                        statsVictim.setArrowBonus(arrow, level);
                    }
                }

                // Anti-weapon damage bug, when shot was from a bow,
                // but user swap his weapon to replace bow stats/damage.
                if (weapon != null && weapon.getType() != Material.TRIDENT
                        && !weapon.isSimilar(statsDamager.getItemInMainHand())) {
                    damageStart = 1D;
                    break labelFullDamage;
                }

                // Applying attack power multiplier depends on
                // projectile launch power for proper damage values.
                double power = ProjectileStats.getPower(projectile);
                statsDamager.setAttackPower(power);


            }
            // If label not broken, then we have to use all damager damage attributes
            isFullDamage = statsDamager != null;
        }
        meta.setWeapon(weapon);

        boolean skillShouldIgnore = skillApi != null && damager != null && skillApi.ignoreDivinity(damager);

        // +----------------------------------------------------+
        // | Get all DamageAttribute values of the damager.     |
        // | For projectiles we're use the meta with saved data.|
        // | For melee damage we're use damager's stats for full|
        // | damage, or a default attribute with event damage.  |
        // +----------------------------------------------------+
        final Map<DamageAttribute, Double>  damages  = new HashMap<>();
        final Map<SimpleStat.Type, Double>  stats    = new HashMap<>();
        final Map<DefenseAttribute, Double> defenses = new HashMap<>();

        // Pre-cache damager damage types.
        if (statsDamager != null && !skillShouldIgnore && isFullDamage) {
            damages.putAll(statsDamager.getDamageTypes(false));
        }
        if (damages.isEmpty()) {
            DamageAttribute dmgCause = ItemStats.getDamageByCause(cause);
            if (dmgCause == null) dmgCause = ItemStats.getDamageByDefault();
            damages.put(dmgCause, damageStart);
            damageStart = 0;
        }

        if (statsDamager != null) {
            stats.putAll(statsDamager.getItemStats(false));
        }

        defenses.putAll(statsVictim.getDefenseTypes(false));

        // +----------------------------------------------------+
        // | Make 'damageStart' to be only additional damage,   |
        // | that is added to the default hand/weapon damage by |
        // | AttributeModifiers and such other things.          |
        // +----------------------------------------------------+
        if (statsDamager != null && !skillShouldIgnore) {
            // Deduct vanilla weapon or hand damage value.
            if (weapon != null && !ItemUT.isAir(weapon)) {
                // If the weapon is a true divinity item, then we need to factor in the damager's attack damage attribute.
                // If it's not (it's vanilla), then we'll just use the vanilla damage minus one for the damage.
                double damagerAttackDamage = 0;

                if (Divinity.getInstance().getModuleCache().getTierManager().isItemOfThisModule(weapon)) {
                    AttributeInstance attackDamageAttribute =
                            damager.getAttribute(VersionManager.getNms().getAttribute("ATTACK_DAMAGE"));
                    damagerAttackDamage =
                            attackDamageAttribute != null ? attackDamageAttribute.getBaseValue() : 1;
                }

                double defaultDamage = DamageAttribute.getVanillaDamage(weapon) + damagerAttackDamage;
                long countCustomDamage = damages.keySet().stream()
                        .filter(att -> {
                            DamageAttribute def = ItemStats.getDamageByDefault();
                            // Heh... well. If we have a damage type that's doing more than 1 damage and is not the
                            // default damage type and is doing more than 1 damage (since 1 is sort of our hard-coded
                            // default), then we can assume that this damage is intended to override the vanilla damage.
                            return !att.equals(def) || (att.equals(def) && damages.get(att) != 1);
                        }).count();
                if (projectile != null && countCustomDamage > 0) {
                    // If it's a projectile, the NMS for default damage doesn't work. so we'll just assume that the
                    // event damage is the default.
                    defaultDamage = e.getDamage();
                }

//                Divinity.getInstance().getLogger().info("Default damage is " + defaultDamage);
                damageStart = Math.max(0D, damageStart - defaultDamage);
//            } else {
//                damageStart = damageStart - 1D; // Reduce the damage by 1 for non-weapon item/hand.
            }
            // Probably can't be lower than 1, but anyway xD
            damageStart /= Math.max(1D, damages.size());
        } else {
            damageStart = 0D;
        }

        // +----------------------------------------------------+
        // | Add additional damage to all damager's attributes. |
        // +----------------------------------------------------+
        final double damageStart2 = damageStart;
        damages.keySet().forEach((dmgAtt) -> damages.compute(dmgAtt, (dmgKey, dmgVal) -> dmgVal + damageStart2));

        scaleValuesWithCore(damager, projectile, damages, defenses, victim);

        DivinityDamageEvent.Start eventStart =
                new DivinityDamageEvent.Start(victim, damager, projectile, damages, defenses,
                        stats, e, meta, skillShouldIgnore);
        plugin.getPluginManager().callEvent(eventStart);
        if (eventStart.isCancelled() || e.isCancelled()) {
//            Divinity.getInstance().info("Damage event was cancelled.");
            return;
        }

        // Remove arrow bonus added above after we got all the stats.
        // We're doing it here to allow arrow bonus affect victim's stats.
        if (statsDamager != null) statsDamager.setArrowBonus(null, 0);
        statsVictim.setArrowBonus(null, 0);

        // +----------------------------------------------------+
        // | Fix final damage value of the vanilla damage event.|
        // +----------------------------------------------------+
//        Divinity.getInstance().info("Damage Final Check: " + e.getFinalDamage() + "/" + e.getDamage());
        if (e.getFinalDamage() != e.getDamage()) {
            double absorption = Math.min(e.getDamage(), victim.getAbsorptionAmount());
            for (DamageModifier dmgModifier : DamageModifier.values()) {
                if (dmgModifier == DamageModifier.ABSORPTION) continue;
                if (e.isApplicable(dmgModifier)) {
                    if (dmgModifier == DamageModifier.BASE) {
//                        Divinity.getInstance().info("FINAL - " + dmgModifier.name() + ": " + e.getDamage());
                        e.setDamage(dmgModifier, e.getDamage() - absorption);
                    } else if (dmgModifier == DamageModifier.ABSORPTION) {
                        e.setDamage(dmgModifier, absorption);
                    } else e.setDamage(dmgModifier, 0); // Fix
                }
            }
        }

//        Divinity.getInstance().info("event took: " + (System.currentTimeMillis() - l1) + " millis");
    }

    /**
     * <p>Scale damage and defense values with Codex attributes.</p>
     *
     * <p>Without external plugins, this will do nothing, but does allow for other plugins
     * to influence any of the Divinity attributes as well as the basic
     * physical, projectile, or melee attributes.</p>
     *
     * <p>It should be noted that projectile and melee adjustments will be made regardless of
     * the Divinity stat or if it's a 'physical-defense' or 'physical-damage'. This means that
     * if the stat to be adjusted is 'physical-damage', the any 'physical-damage' modifiers
     * will be applied, followed by either 'melee-damage' or 'projectile-damage'.</p>
     *
     * @param damager    The damager in the scenario, used for damage scaling
     * @param projectile The projectile in the scenario, or null if it's a melee attack
     * @param damages    The damage map to scale
     * @param defenses   The defense map to scale
     * @param victim     The victim in the scenario, used for defense scaling
     */
    private static void scaleValuesWithCore(LivingEntity damager,
                                            Projectile projectile,
                                            Map<DamageAttribute, Double> damages,
                                            Map<DefenseAttribute, Double> defenses,
                                            LivingEntity victim) {
        // If they're a player, but not a CitizensNPC, then we'll
        // apply attributes registered with Codex
        if (damager instanceof Player && !damager.getClass().getName().equals("PlayerNPC")) {
            // Scale damages
            damages.forEach((dmgAtt, value) -> {
                if (dmgAtt == null) return;

                String id = dmgAtt.getId();
                if (id.equals("physical")) id = AttributeRegistry.PHYSICAL_DAMAGE;
                else id = "rpgdamage-" + id;

                double damage = value;
                damage = AttributeRegistry.scaleAttribute(
                        id,
                        damager,
                        damage
                );

                if (projectile != null) {
                    damage = AttributeRegistry.scaleAttribute(
                            AttributeRegistry.PROJECTILE_DAMAGE,
                            damager,
                            damage
                    );
                } else {
                    damage = AttributeRegistry.scaleAttribute(
                            AttributeRegistry.MELEE_DAMAGE,
                            damager,
                            damage
                    );
                }

                damages.put(dmgAtt, damage);
            });
        }

        if (victim instanceof Player && !victim.getClass().getName().equals("PlayerNPC")) {
            // Scale defenses
            defenses.forEach((defAtt, value) -> {
                if (defAtt == null) return;

                String id = defAtt.getId();
                if (id.equals("physical")) id = AttributeRegistry.PHYSICAL_DEFENSE;
                else id = "rpgdefense-" + id;

                double defense = value;
                defense = AttributeRegistry.scaleAttribute(
                        id,
                        victim,
                        defense
                );

                if (projectile != null) {
                    defense = AttributeRegistry.scaleAttribute(
                            AttributeRegistry.PROJECTILE_DEFENSE,
                            victim,
                            defense
                    );
                } else {
                    defense = AttributeRegistry.scaleAttribute(
                            AttributeRegistry.MELEE_DEFENSE,
                            victim,
                            defense
                    );
                }

                defenses.put(defAtt, defense);
            });
        }
    }
}
