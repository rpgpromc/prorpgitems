package studio.magemonkey.divinity.manager;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.event.DivinityDamageEvent;
import studio.magemonkey.divinity.api.event.EntityDivinityItemPickupEvent;
import studio.magemonkey.divinity.api.event.EntityEquipmentChangeEvent;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.EntityStatsTask;
import studio.magemonkey.divinity.stats.ProjectileStats;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityManager extends IListener<Divinity> {

    private static final String                     PACKET_DUPLICATOR_FIXER = "PACKET_DUPLICATOR_FIXER";
    private static final Map<UUID, EntityEquipment> previousEquipment       = new HashMap<>();
    private              EntityStatsTask            entityStatsTask;

    public EntityManager(@NotNull Divinity plugin) {
        super(plugin);
    }

    public static boolean isPacketDuplicatorFixed(@NotNull Entity entity) {
        if (entity.hasMetadata(PACKET_DUPLICATOR_FIXER)) {
            entity.removeMetadata(PACKET_DUPLICATOR_FIXER, Divinity.getInstance());
            return true;
        }
        return false;
    }

    public static boolean isEquipmentNew(LivingEntity entity) {
        EntityEquipment previous = previousEquipment.get(entity.getUniqueId());
        if (previous == null) return true;
        EntityEquipment current = entity.getEquipment();
        if (current == null) return false;

        ItemStack[] previousContents = previous.getArmorContents();
        ItemStack[] armorContents    = current.getArmorContents();
        for (int i = 0; i < previousContents.length; i++) {
            if (previousContents[i] == null && armorContents[i] == null) continue;
            if (previousContents[i] == null || armorContents[i] == null) return true;
            if (!previousContents[i].isSimilar(armorContents[i])) return true;
        }

        ItemStack previousHand = previous.getItemInMainHand();
        ItemStack hand         = current.getItemInMainHand();
        boolean   handMatch    = previousHand.isSimilar(hand);

        ItemStack previousOffhand = previous.getItemInOffHand();
        ItemStack offhand         = current.getItemInOffHand();
        boolean   offhandMatch    = previousOffhand.isSimilar(offhand);

        return !handMatch || !offhandMatch;
    }

    public void setup() {
        this.entityStatsTask = new EntityStatsTask(plugin);
        this.entityStatsTask.start();

        this.registerListeners();
    }

    public void shutdown() {
        this.unregisterListeners();
        if (this.entityStatsTask != null) {
            this.entityStatsTask.stop();
            this.entityStatsTask = null;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStatsDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        previousEquipment.remove(e.getEntity().getUniqueId());
        EntityStats.get(entity).handleDeath();
    }

    // Clear stats on player exit
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStatsQuit(PlayerQuitEvent e) {
        EntityStats.purge(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStatsJoin(PlayerJoinEvent e) {
        EntityStats.get(e.getPlayer());
        this.pushToUpdate(e.getPlayer(), 1D);
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        previousEquipment.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onStatsRegen(EntityRegainHealthEvent e) {
        Entity e1 = e.getEntity();
        if (!(e1 instanceof LivingEntity)) return;

        LivingEntity entity = (LivingEntity) e1;
        double       regen  = 1D + EntityStats.get(entity).getItemStat(TypedStat.Type.HEALTH_REGEN, false) / 100D;
        e.setAmount(e.getAmount() * regen);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!ProjectileStats.isPickable(e.getItem())) {
            e.setCancelled(true);
        }

        ItemStack      item       = e.getItem().getItemStack();
        QModuleDrop<?> moduleDrop = ItemStats.getModule(item);
        if (moduleDrop == null) return;

        EntityDivinityItemPickupEvent ev = new EntityDivinityItemPickupEvent(item, e.getEntity(), moduleDrop);
        plugin.getPluginManager().callEvent(ev);
        e.setCancelled(ev.isCancelled());
    }

    private final void pushToUpdate(@NotNull LivingEntity entity, double time) {
        EntityEquipment equip = new EntityEquipmentSnapshot(entity);
        previousEquipment.put(entity.getUniqueId(), equip);
        if (time <= 0D) {
            plugin.getServer().getScheduler().runTask(plugin, () -> EntityStats.get(entity).updateAll());
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                EntityStats.get(entity).updateAll();
            }
        }.runTask(Divinity.getInstance());
    }

    private final void addDuplicatorFixer(@NotNull Entity entity) {
        entity.setMetadata(PACKET_DUPLICATOR_FIXER, new FixedMetadataValue(plugin, "fixed"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStatsSprintPacketDuplicator(PlayerToggleSprintEvent e) {
        this.addDuplicatorFixer(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStatsDamagePacketDuplicator(DivinityDamageEvent.Exit e) {
        LivingEntity victim = e.getVictim();
        this.addDuplicatorFixer(victim);
        this.pushToUpdate(victim, 1.5D);

        LivingEntity damager = e.getDamager();
        if (damager != null) {
            this.addDuplicatorFixer(damager);
            this.pushToUpdate(damager, 1.5D);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStatsRegenPacketDuplicator(EntityRegainHealthEvent e) {
        this.addDuplicatorFixer(e.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStatsUpdateMobSpawn(CreatureSpawnEvent e) {
        this.pushToUpdate(e.getEntity(), 1D);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStatsUpdatePlayerRespawn(PlayerRespawnEvent e) {
        this.pushToUpdate(e.getPlayer(), 1D);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStatsUpdatePlayerHeld(PlayerItemHeldEvent e) {
        this.addDuplicatorFixer(e.getPlayer());
        this.pushToUpdate(e.getPlayer(), 0D);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityUpdateEquipmentChange(EntityEquipmentChangeEvent e) {
        this.pushToUpdate(e.getEntity(), 0.5D);
    }
}
