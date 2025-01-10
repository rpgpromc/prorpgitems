package studio.magemonkey.divinity.manager.listener.object;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.meta.NBTAttribute;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.codex.util.DataUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.stats.items.ItemStats;

public class ItemUpdaterListener extends IListener<Divinity> {


    public ItemUpdaterListener(@NotNull Divinity plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void click(InventoryClickEvent event) {
        ItemStack item   = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        update(item, null);
        HumanEntity entity = event.getWhoClicked();
        update(cursor, entity instanceof Player ? (Player) entity : null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void interact(PlayerInteractEvent event) {
        update(event.getItem(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void drop(PlayerDropItemEvent event) {
        update(event.getItemDrop().getItemStack(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void pickup(EntityPickupItemEvent event) {
        LivingEntity entity = event.getEntity();
        update(event.getItem().getItemStack(), entity instanceof Player ? (Player) entity : null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void attack(EntityDamageByEntityEvent event) {
        Entity at      = event.getDamager();
        Entity damaged = event.getEntity();

        if (at instanceof Player) {
            Player player = (Player) at;
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                update(armor, player);
            }
        }

        if (damaged instanceof Player) {
            Player player = (Player) damaged;
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                update(armor, player);
            }
        }
    }

    public void update(ItemStack item, @Nullable Player player) {
        if (item == null || item.getType() == Material.AIR) return;

        ItemType itemType = CodexEngine.get().getItemManager().getMainItemType(item);
        if (itemType == null || !itemType.getNamespace().equals("DIVINITY")) return;

        NamespacedKey key   = NamespacedKey.fromString("rpgpro.fixed_damage");
        boolean       fixed = DataUT.getBooleanData(item, key);
        ItemMeta      meta  = item.getItemMeta();

        if (meta != null) {
            if (fixed) {
                DataUT.removeData(item, key);
                meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            } else {
                meta.addItemFlags(ItemFlag.values());
            }
            item.setItemMeta(meta);
        }

        if (ItemStats.hasDamage(item, null, NBTAttribute.ATTACK_DAMAGE.getNmsName())
                && ItemStats.getDamageMinOrMax(item, null, NBTAttribute.ATTACK_DAMAGE.getNmsName(), 1) <= 0) {
            ItemStats.updateVanillaAttributes(item, player);
//            DataUT.setData(item, key, true);
        }

    }

}
