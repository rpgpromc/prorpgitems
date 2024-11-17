package studio.magemonkey.divinity.manager.listener.object;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.event.DivinityDamageEvent;
import studio.magemonkey.divinity.api.event.DivinityProjectileLaunchEvent;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;
import studio.magemonkey.divinity.stats.items.attributes.stats.DurabilityStat;

public class ItemDurabilityListener extends IListener<Divinity> {

    private final DurabilityStat duraStat;

    public ItemDurabilityListener(@NotNull Divinity plugin, @NotNull DurabilityStat duraStat) {
        super(plugin);
        this.duraStat = duraStat;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDuraItemDamage(PlayerItemDamageEvent e) {
        ItemStack item = e.getItem();
        if (ItemStats.hasStat(item, null, TypedStat.Type.DURABILITY) || this.duraStat.isUnbreakable(item)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDuraBreak(BlockBreakEvent e) {
        Player    player = e.getPlayer();
        ItemStack item   = player.getInventory().getItemInMainHand();
        if (ItemUT.isAir(item)) return;

        this.duraStat.reduceDurability(player, item, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDuraFish(PlayerFishEvent e) {
        Player    player = e.getPlayer();
        ItemStack item   = player.getInventory().getItemInMainHand();
        if (ItemUT.isAir(item)) return;

        this.duraStat.reduceDurability(player, item, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDuraDamageWeapon(DivinityDamageEvent.Exit e) {
        // Reduce durability of victim's armor
        LivingEntity    victim      = e.getVictim();
        EntityEquipment equipVictim = victim.getEquipment();
        if (equipVictim != null) {
            ItemStack[] armor = equipVictim.getArmorContents();
            for (ItemStack armorItem : armor) {
                if (ItemUT.isAir(armorItem)) continue;
                this.duraStat.reduceDurability(victim, armorItem, 1);
            }
            equipVictim.setArmorContents(armor);

            if (e.getDamageMeta().isBlocked()) {
                ItemStack shield = equipVictim.getItemInMainHand();
                if (shield.getType() != Material.SHIELD) shield = equipVictim.getItemInOffHand();
                if (shield.getType() == Material.SHIELD)
                    this.duraStat.reduceDurability(victim, shield, 1);
            }
        }

        // Reduce durability of attacker's weapon
        LivingEntity damager = e.getDamager();
        if (damager == null || e.isProjectile()) return;

        EntityEquipment equipDamager = damager.getEquipment();
        if (equipDamager != null) {
            ItemStack weapon = equipDamager.getItemInMainHand();
            if (weapon.getType() != Material.SHIELD) this.duraStat.reduceDurability(damager, weapon, 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDurabilityBow(DivinityProjectileLaunchEvent e) {
        ItemStack bow = e.getWeapon();
        if (bow == null) return;

        this.duraStat.reduceDurability(e.getShooter(), bow, 1);
        e.setWeapon(bow);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDuraHoe(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (block == null) return;

        Material bType = block.getType();
        if (bType == Material.GRASS_BLOCK || bType == Material.DIRT || bType == Material.MYCELIUM) {
            Player    player = e.getPlayer();
            ItemStack hoe    = player.getInventory().getItemInMainHand();

            if (!ItemUT.isAir(hoe) && hoe.getType().name().endsWith("_HOE")) {
                this.duraStat.reduceDurability(player, hoe, 1);
            }
        }
    }
}
