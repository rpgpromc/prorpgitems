package studio.magemonkey.divinity.manager.damage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.api.items.PrefixHelper;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.codex.registry.provider.DamageTypeProvider;
import studio.magemonkey.codex.util.LocUT;
import studio.magemonkey.codex.util.NamespaceResolver;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.PartyAPI;
import studio.magemonkey.divinity.api.event.DivinityDamageEvent;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.hooks.external.mythicmobs.AbstractMythicMobsHK;
import studio.magemonkey.divinity.manager.effects.main.AdjustStatEffect;
import studio.magemonkey.divinity.manager.effects.main.DisarmEffect;
import studio.magemonkey.divinity.modules.list.party.PartyManager.Party;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.DamageAttribute;
import studio.magemonkey.divinity.stats.items.attributes.DefenseAttribute;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;
import studio.magemonkey.divinity.stats.items.attributes.stats.BleedStat;

import java.util.*;
import java.util.function.DoubleUnaryOperator;

@SuppressWarnings("deprecation")
public class DamageManager extends IListener<Divinity> implements DamageTypeProvider {
    //private CrackShotHK csHook;

    private AbstractMythicMobsHK mmHook;

    public DamageManager(Divinity plugin) {
        super(plugin);
    }

    @Nullable
    public static LivingEntity getTargetByDirection(@NotNull Entity damager) {
        return DamageManager.getTargetByDirection(damager, EngineCfg.COMBAT_MAX_GET_TARGET_DISTANCE);
    }

    @Nullable
    public static LivingEntity getTargetByDirection(@NotNull Entity damager, double range) {
        Location start = damager.getLocation();
        if (damager instanceof LivingEntity) {
            start = ((LivingEntity) damager).getEyeLocation();
        }
        Vector increase = start.getDirection();

        return DamageManager.getTargetByDirection(damager, start, increase, range);
    }

    @Nullable
    public static LivingEntity getTargetByDirection(@NotNull Entity damager,
                                                    @NotNull Location from,
                                                    @NotNull Location to) {
        return DamageManager.getTargetByDirection(damager, from, to, EngineCfg.COMBAT_MAX_GET_TARGET_DISTANCE);
    }

    @Nullable
    public static LivingEntity getTargetByDirection(@NotNull Entity damager,
                                                    @NotNull Location from,
                                                    @NotNull Location to,
                                                    double range) {
        Vector increase = LocUT.getDirectionTo(from, to);
        return DamageManager.getTargetByDirection(damager, from, increase, range);
    }

    @Nullable
    public static LivingEntity getTargetByDirection(@NotNull Entity damager,
                                                    @NotNull Location from,
                                                    @NotNull Vector dir,
                                                    double range) {
        LivingEntity target   = null;
        Vector       increase = dir;

        Party party = null;
        if (damager.getType() == EntityType.PLAYER) {
            party = PartyAPI.getPlayerParty((Player) damager);
        }

        for (int counter = 0; counter < range; counter++) {
            Location point = from.add(increase);

            Material wall = point.getBlock().getType();
            if (wall != Material.AIR || wall.isSolid()) {
                break;
            }

            for (Entity entity : point.getChunk().getEntities()) {
                if (!Hooks.canFights(damager, entity)) continue;
                LivingEntity entity2 = (LivingEntity) entity;
                if (entity2.isDead() || !entity2.isValid()) continue;
                if (entity2.getEyeLocation().distance(point) > 1.5) continue;
                if (party != null && entity2 instanceof Player && party.isMember((Player) entity2)) {
                    continue;
                }

                return entity2;
            }
        }
        return target;
    }

    @NotNull
    public static Set<LivingEntity> getTargetsByRange(@NotNull Entity damager, double range) {
        return getTargetsByRange(damager, damager.getLocation(), range);
    }

    @NotNull
    public static Set<LivingEntity> getTargetsByRange(@NotNull Entity damager, @NotNull Location from, double range) {
        Set<LivingEntity> set = new HashSet<>();

        Party party = null;
        if (damager.getType() == EntityType.PLAYER) {
            party = PartyAPI.getPlayerParty((Player) damager);
        }

        ProjectileSource shooter = null;
        if (damager instanceof Projectile) {
            shooter = ((Projectile) damager).getShooter();
        }

        for (Entity entity : damager.getWorld().getNearbyEntities(from, range, range, range)) {
            if (!Hooks.canFights(damager, entity)) continue;
            LivingEntity target = (LivingEntity) entity;
            if (target.isDead() || !target.isValid()) continue;

            if (party != null && target instanceof Player && party.isMember((Player) target)) {
                continue;
            }
            if (shooter != null && target.equals(shooter)) continue;

            set.add(target);
        }
        return set;
    }

    public void setup() {
        this.mmHook = plugin.getHook(AbstractMythicMobsHK.class);
        //this.csHook = plugin.getHook(CrackShotHK.class);

        this.registerListeners();
    }

    public void shutdown() {
        this.unregisterListeners();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamageFishHook(PlayerFishEvent e) {
        if (!EngineCfg.COMBAT_FISHING_HOOK_DO_DAMAGE) return;

        Entity caught = e.getCaught();
        if (!(caught instanceof LivingEntity)) return;

        Player       player = e.getPlayer();
        LivingEntity target = (LivingEntity) caught;

        target.damage(1D, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageRPGStart(@NotNull DivinityDamageEvent.Start e) {
        LivingEntity      victim     = e.getVictim();
        LivingEntity      damager    = e.getDamager();
        Projectile        projectile = e.getProjectile();
        DamageMeta        meta       = e.getDamageMeta();
        EntityDamageEvent orig       = e.getOriginalEvent();

        EntityStats statsDamager = e.getDamagerStats();
        EntityStats statsVictim  = e.getVictimStats();

        // Handle damager stats
        // If false then event is cancelled, then stop calculations
        if (damager != null && statsDamager != null)
            if (!this.handleDamageModifiers(e, victim, damager, statsVictim, statsDamager, projectile, orig, meta))
                return;

        DivinityDamageEvent.Pre eventPre = new DivinityDamageEvent.Pre(victim, damager, projectile, orig, meta);
        plugin.getPluginManager().callEvent(eventPre);
        if (eventPre.isCancelled()) return;

        String mythicFaction = "";
        if (this.mmHook != null && this.mmHook.isMythicMob(victim)) {
            mythicFaction = this.mmHook.getMythicInstance(victim).getFaction();
        }

        List<MetadataValue> metadata = damager != null && !e.isExempt()
                ? damager.getMetadata("custom-cooldown")
                : Collections.emptyList();

        double powerMod = metadata == null || metadata.isEmpty()
                ? (statsDamager != null ? statsDamager.getAttackPowerModifier() : 1D)
                : metadata.get(0).asInt();
        double directMod        = meta.getDirectModifier();
        double critMod          = meta.getCriticalModifier();
        double blockMod         = meta.getBlockModifier();
        double penetrateMod     = meta.getPenetrateModifier();
        double pveDamageMod     = meta.getPvEDamageModifier();
        double pveDefenseMod    = meta.getPvEDefenseModifier();
        double enchantFactorMod = this.getEnchantModifier(victim, e.getCause());
        double toughness        = e.getVictimStats().getItemStat(TypedStat.Type.ARMOR_TOUGHNESS, false);
        meta.setEnchantProtectionModifier(enchantFactorMod);

        Map<DefenseAttribute, Double> defenses = e.getDefenseMap();

        for (Map.Entry<DamageAttribute, Double> en : e.getDamageMap().entrySet()) {
            DamageAttribute dmgAtt  = en.getKey();
            double          dmgType = en.getValue();
//            Divinity.getInstance().getLogger().info("*" + dmgAtt.getName() + ": " + dmgType);

            dmgType *= pveDamageMod;
            dmgType *= critMod;
            if (dmgAtt != null) {
                dmgType *= dmgAtt.getDamageModifierByEntityType(victim);
                dmgType *= dmgAtt.getDamageModifierByMythicFaction(mythicFaction);
            }
            dmgType *= enchantFactorMod;
            if (!e.isExempt())
                dmgType *= powerMod;
            dmgType *= blockMod;
            double directType = dmgType * directMod; // Get direct value for this Damage Attribute
            dmgType = Math.max(0, dmgType - directType); // Deduct this value from damage

            if (dmgType > 0) {
                DefenseAttribute defAtt = dmgAtt != null ? dmgAtt.getAttachedDefense() : null;
                if (defAtt != null && defenses.containsKey(defAtt)) {
                    double def = Math.max(0, defenses.get(defAtt) * pveDefenseMod * penetrateMod);

                    double defCalced;
                    if (EngineCfg.LEGACY_COMBAT) {
                        defCalced = Math.max(0, dmgType * (1 - (def * defAtt.getProtectionFactor() * 0.01)));
                    } else {
                        defCalced = Math.max(0,
                                dmgType * (1 - Math.max(def / 5, def - 4 * dmgType / Math.max(1, toughness + 8))
                                        * defAtt.getProtectionFactor() * 0.05));
                    }
                    meta.setDefendedDamage(defAtt, dmgType - defCalced);
                    dmgType = defCalced;
                }
            }
            //Should we reactivate direct damage, remove directType here and deal the damage straight.
            meta.setDamage(dmgAtt, dmgType + directType);
//            Divinity.getInstance().getLogger().info(dmgAtt.getName() + ": " + meta.getDamage(dmgAtt) + " (" + directType + ")");

            // Actions Executor
            if (damager != null && dmgAtt != null) {
                dmgAtt.getHitActions().process(damager);
            }
        }

        double dmgTotal = meta.getTotalDamage();
//        Divinity.getInstance().getLogger().info("Damage total: " + dmgTotal);
//        Divinity.getInstance().getLogger().info("Defended: " + meta.getDefendedDamage());
        orig.setDamage(dmgTotal);

        // We want to terminate early since the post-effects could introduce RPGItems effects not intended for skills
        if (e.isExempt()) return;

        if (damager != null && statsDamager != null && dmgTotal > 0)
            if (!this.handleDamagePostEffects(e, victim, damager, statsVictim, statsDamager, projectile, orig, meta))
                return;

        DivinityDamageEvent.Exit eventExit = new DivinityDamageEvent.Exit(victim, damager, projectile, orig, meta);
        plugin.getPluginManager().callEvent(eventExit);
        if (eventExit.isCancelled()) return;

        if (damager != null && this.mmHook != null) this.mmHook.setSkillDamage(damager, dmgTotal);
    }

    private boolean handleDamageModifiers(
            @NotNull DivinityDamageEvent event,
            @NotNull LivingEntity victim,
            @NotNull LivingEntity damager,
            @NotNull EntityStats statsVictim,
            @NotNull EntityStats statsDamager,
            @Nullable Projectile projectile,
            @NotNull EntityDamageEvent e,
            @NotNull DamageMeta meta
    ) {

        double dodgeRate = statsVictim.getItemStat(TypedStat.Type.DODGE_RATE, false);
        if (dodgeRate > 0D) {
            double accurRate = event.getDamagerItemStat(TypedStat.Type.ACCURACY_RATE);
            if (Rnd.get(true) < dodgeRate && Rnd.get(true) >= accurRate) {

                DivinityDamageEvent.Dodge qDodge = new DivinityDamageEvent.Dodge(victim, damager, projectile, e, meta);
                plugin.getPluginManager().callEvent(qDodge);
                if (!qDodge.isCancelled()) {
                    e.setDamage(0D);
                    e.setCancelled(true);
                    meta.setDodge(true);
                    return false;
                }
            }
        }

        double critModifier = 1D;
        double critRate     = event.getDamagerItemStat(TypedStat.Type.CRITICAL_RATE);
        if (critRate > 0 && Rnd.get(true) < critRate) {
            critModifier = event.getDamagerItemStat(TypedStat.Type.CRITICAL_DAMAGE);
            if (critModifier == 0D) {
                critModifier = 1D;
            }
        }
        meta.setCriticalModifier(critModifier);

        SimpleStat.Type pvpDefenseType = TypedStat.Type.PVE_DEFENSE;
        SimpleStat.Type pvpDamageType  = TypedStat.Type.PVE_DAMAGE;

        if (statsVictim.isPlayer() && statsDamager.isPlayer()) {
            pvpDefenseType = TypedStat.Type.PVP_DEFENSE;
            pvpDamageType = TypedStat.Type.PVP_DAMAGE;
        }

        double directPercent = /*event.getDamagerItemStat(AbstractStat.Type.DIRECT_DAMAGE)*/0D / 100D;
        double penetration   = 1D - event.getDamagerItemStat(TypedStat.Type.PENETRATION) / 100D;

        double pvpDefense = 1D + statsVictim.getItemStat(pvpDefenseType, false) / 100D;
        double pvpDamage  = 1D + event.getDamagerItemStat(pvpDamageType) / 100D;

        meta.setPvEDamageModifier(pvpDamage);
        meta.setPvEDefenseModifier(pvpDefense);
        meta.setDirectModifier(directPercent);
        meta.setPenetrateModifier(penetration);

        double blockModifier = statsVictim.getItemStat(TypedStat.Type.BLOCK_DAMAGE, false);
        double blockRate     = statsVictim.getItemStat(TypedStat.Type.BLOCK_RATE, false);

        Player  player           = statsVictim.getPlayer();
        boolean isVanillaBlocked = false;
        if (player != null && player.isBlocking() && player.getCooldown(Material.SHIELD) <= 0) {
            // Damage in face = Block
            if (e.getDamage(DamageModifier.BLOCKING) < 0) {
                isVanillaBlocked = true;
                blockRate += EngineCfg.COMBAT_SHIELD_BLOCK_BONUS_RATE;
                blockModifier += EngineCfg.COMBAT_SHIELD_BLOCK_BONUS_DAMAGE_MOD;
            }

            if (blockRate > 0D) {
                double vanillaBlockModifier = 1D;
                if (Rnd.get(true) < blockRate) {
                    vanillaBlockModifier = 0D;
                    meta.setBlockModifier(1D - blockModifier / 100D);

                    if (isVanillaBlocked && player != null) {
                        applyShieldDamage(player);
                    }
                }
                // Fix/Disable vanilla shield block
                if (e.isApplicable(DamageModifier.BLOCKING)) {
                    e.setDamage(DamageModifier.BLOCKING, vanillaBlockModifier);
                }
            }
        }

        return true;
    }

    private void applyShieldDamage(Player player) {
        if (!player.isBlocking()) return;
        player.setCooldown(Material.SHIELD, 20 * EngineCfg.COMBAT_SHIELD_BLOCK_COOLDOWN);

        ItemStack offHand  = player.getInventory().getItemInOffHand();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack shield = offHand.getType() == Material.SHIELD ? offHand
                : mainHand.getType() == Material.SHIELD ? mainHand : null;
        if (shield == null) return;

        ItemMeta shieldMeta = shield.getItemMeta();
        int level =
                shieldMeta.getEnchantLevel(NamespaceResolver.getEnchantment("UNBREAKING",
                        "DURABILITY")); // DURABILITY/UNBREAKING
        if (Rnd.get(true) <= (100d / (level + 1))) {
            if (shieldMeta instanceof Damageable) {
                ((Damageable) shieldMeta).setDamage(((Damageable) shieldMeta).getDamage() + 1);
                shield.setItemMeta(shieldMeta);
            } else {
                shield.setDurability((short) (shield.getDurability() - 1));
            }
        }
    }

    private boolean handleDamagePostEffects(
            @NotNull DivinityDamageEvent event,
            @NotNull LivingEntity victim,
            @NotNull LivingEntity damager,
            @NotNull EntityStats statsVictim,
            @NotNull EntityStats statsDamager,
            @Nullable Projectile projectile,
            @NotNull EntityDamageEvent e,
            @NotNull DamageMeta meta
    ) {

        final double aoe = event.getDamagerItemStat(TypedStat.Type.AOE_DAMAGE);
        if (aoe > 0D && e.getCause() != DamageCause.ENTITY_SWEEP_ATTACK) {
            if (statsVictim.isIgnoreAOE()) {
                statsVictim.setIgnoreAOE(false);
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Set<LivingEntity> targets = DamageManager.getTargetsByRange(damager, 2D);
                    targets.remove(victim);
                    targets.remove(damager);
                    DoubleUnaryOperator operator = (dmg) -> dmg * (aoe / 100D);

                    for (LivingEntity targetAoe : targets) {
                        AdjustStatEffect aoeReducer = new AdjustStatEffect.Builder(-1)
                                .withCharges(1).withAdjust(ItemStats.getDamages(), operator).build();
                        aoeReducer.applyTo(damager);

                        EntityStats.get(targetAoe).setIgnoreAOE(true);
                        targetAoe.damage(1D, damager);
                    }
                });
            }
        }

        double dmgTotal = meta.getTotalDamage();

        double disarm = event.getDamagerItemStat(TypedStat.Type.DISARM_RATE);
        if (disarm > 0D && Rnd.get(true) < disarm) {
            DisarmEffect disarmEffect = new DisarmEffect();
            disarmEffect.applyTo(victim);
        }

        double burn = event.getDamagerItemStat(TypedStat.Type.BURN_RATE);
        if (burn > 0D && Rnd.get(true) < burn) victim.setFireTicks(100);

        double bleed = event.getDamagerItemStat(TypedStat.Type.BLEED_RATE);
        if (bleed > 0D && Rnd.get(true) < bleed) {
            BleedStat bleedStat = ItemStats.getStat(BleedStat.class);
            if (bleedStat != null) bleedStat.bleed(victim, dmgTotal);
        }

        double vamp = Math.max(0, dmgTotal * (event.getDamagerItemStat(TypedStat.Type.VAMPIRISM) / 100D));
        if (vamp > 0D) {
            EntityRegainHealthEvent eventRegain = new EntityRegainHealthEvent(damager, vamp, RegainReason.CUSTOM);
            plugin.getPluginManager().callEvent(eventRegain);
            if (!eventRegain.isCancelled()) {
                double max = EntityStats.getEntityMaxHealth(damager);
                damager.setHealth(Math.min(max, damager.getHealth() + vamp));
            }
        }

        double thorn = statsVictim.getItemStat(TypedStat.Type.THORNMAIL, false) / 100D;
        if (thorn > 0D) damager.damage(dmgTotal * thorn);

        return true;
    }

    private double getEnchantModifier(@NotNull LivingEntity zertva, @NotNull DamageCause cause) {
        EntityStats stats = EntityStats.get(zertva);

        double epfAll =
                stats.getEnchantProtectFactor(NamespaceResolver.getEnchantment("PROTECTION",
                        "PROTECTION_ENVIRONMENTAL")); // PROTECTION_ENVIRONMENTAL/PROTECTION
        double epfSpec = 0D;
        double epfMod  = 1D;

        if (cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK
                || cause == DamageCause.LAVA) {
            epfSpec =
                    stats.getEnchantProtectFactor(NamespaceResolver.getEnchantment("FIRE_PROTECTION",
                            "PROTECTION_FIRE")); // PROTECTION_FIRE/FIRE_PROTECTION
        } else if (cause == DamageCause.FALL) {
            epfSpec =
                    stats.getEnchantProtectFactor(NamespaceResolver.getEnchantment("FEATHER_FALLING",
                            "PROTECTION_FALL")); // PROTECTION_FALL/FEATHER_FALLING
        } else if (cause == DamageCause.PROJECTILE) {
            epfSpec =
                    stats.getEnchantProtectFactor(NamespaceResolver.getEnchantment("PROJECTILE_PROTECTION",
                            "PROTECTION_PROJECTILE")); // PROTECTION_PROJECTILE/PROJECTILE_PROTECTION
        } else if (cause == DamageCause.BLOCK_EXPLOSION || cause == DamageCause.ENTITY_EXPLOSION) {
            epfSpec =
                    stats.getEnchantProtectFactor(NamespaceResolver.getEnchantment("BLAST_PROTECTION",
                            "PROTECTION_EXPLOSION")); // PROTECTION_EXPLOSION/BLAST_PROTECTION
        }
        epfMod = Math.min(20D, (epfSpec + epfAll));

        return (1D - epfMod / 25D);
    }

    @Override
    public String pluginName() {
        return Divinity.getInstance().getName();
    }

    @Override
    public String getNamespace() {
        return Divinity.getInstance().getName().toUpperCase(Locale.US);
    }

    @Override
    public boolean dealDamage(@NotNull LivingEntity entity,
                              double amount,
                              String damageType,
                              @Nullable LivingEntity damager) {
        DamageAttribute damageAttribute =
                ItemStats.getDamageById(PrefixHelper.stripPrefix(getNamespace(), damageType));
        if (damageAttribute == null) return false;
        final boolean[] success = {false};
        Listener listener = new Listener() {
            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onDamage(DivinityDamageEvent.Start event) {
                if (event.getOriginalEvent().getEntity() != entity) return;
                Map<DamageAttribute, Double> damageMap = event.getDamageMap();
                damageMap.clear();
                damageMap.put(damageAttribute, amount);
                success[0] = true;
            }
        };
        try {
            Bukkit.getPluginManager().registerEvents(listener, Divinity.getInstance());
            entity.damage(amount, damager);
        } finally {
            HandlerList.unregisterAll(listener);
        }
        return success[0];
    }
}
