package studio.magemonkey.divinity.manager.listener.object;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.HandAttribute;

import java.util.Set;

public class ItemHandListener extends IListener<Divinity> {

    public ItemHandListener(@NotNull Divinity plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHandHeld(PlayerItemHeldEvent e) {
        int    slot   = e.getNewSlot();
        Player player = e.getPlayer();

        ItemStack toHold = player.getInventory().getItem(slot);
        if (toHold == null || toHold.getType() == Material.AIR) return;

        ItemStack inOff = player.getInventory().getItemInOffHand();
        if (ItemUT.isAir(inOff)) return;

        HandAttribute handTo  = ItemStats.getHand(toHold);
        HandAttribute handOff = ItemStats.getHand(inOff);

        if ((handTo != null && handTo.getType() == HandAttribute.Type.TWO)
                || (handOff != null && handOff.getType() == HandAttribute.Type.TWO)) {
            plugin.lang().Module_Item_Interact_Error_Hand.send(player);
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHandSwap(PlayerSwapHandItemsEvent e) {
        Player player = e.getPlayer();

        ItemStack off = e.getOffHandItem();
        if (off == null || off.getType() == Material.AIR) return;

        HandAttribute handOff = ItemStats.getHand(off);
        if (handOff != null && handOff.getType() == HandAttribute.Type.TWO) {
            plugin.lang().Module_Item_Interact_Error_Hand.send(player);
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHandDrag(InventoryDragEvent e) {
        if (e.getInventory().getType() != InventoryType.CRAFTING) return;

        ItemStack drag = e.getOldCursor();
        if (ItemUT.isAir(drag)) return;

        HandAttribute hand = ItemStats.getHand(drag);
        if (hand == null || hand.getType() != HandAttribute.Type.TWO) return;

        Player       player = (Player) e.getWhoClicked();
        Set<Integer> slots  = e.getRawSlots();

        if (slots.contains(40)) { // Offhand
            plugin.lang().Module_Item_Interact_Error_Hand.send(player);
            e.setCancelled(true);
            e.setResult(Event.Result.DENY);
            String server = Bukkit.getServer().getName();
            if (server.contains("Mohist")) {
                // Mohist is dumb... and doesn't properly reset slots
                // after the event is cancelled
                player.getInventory().setItemInOffHand(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onHandClose(InventoryCloseEvent e) {
        Player    player = (Player) e.getPlayer();
        ItemStack off    = player.getInventory().getItemInOffHand();
        if (ItemUT.isAir(off)) return;

        ItemStack main = player.getInventory().getItemInMainHand();

        HandAttribute pickh = ItemStats.getHand(main);
        HandAttribute offh  = ItemStats.getHand(off);

        if ((pickh != null && pickh.getType() == HandAttribute.Type.TWO)
                || (offh != null && offh.getType() == HandAttribute.Type.TWO)) {

            ItemUT.addItem(player, new ItemStack(off));
            player.getInventory().setItemInOffHand(null);
            player.updateInventory();
            plugin.lang().Module_Item_Interact_Error_Hand.send(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHandHoldOffClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getType() != InventoryType.CRAFTING) return;

        ItemStack cursor = e.getCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;

        if (e.getSlot() == 40) { // Offhand
            Player        player  = (Player) e.getWhoClicked();
            HandAttribute handAtt = ItemStats.getHand(cursor);
            if (handAtt != null && handAtt.getType() == HandAttribute.Type.TWO) {
                plugin.lang().Module_Item_Interact_Error_Hand.send(player);
                e.setCancelled(true);
                player.updateInventory();
            } else {
                if (this.holdMainTwo(player)) {
                    plugin.lang().Module_Item_Interact_Error_Hand.send(player);
                    e.setCancelled(true);
                    player.updateInventory();
                }
            }
        }
    }

    private boolean holdMainTwo(@NotNull Player player) {
        ItemStack     main = player.getInventory().getItemInMainHand();
        HandAttribute hand = ItemStats.getHand(main);
        return hand != null && hand.getType() == HandAttribute.Type.TWO;
    }
}
