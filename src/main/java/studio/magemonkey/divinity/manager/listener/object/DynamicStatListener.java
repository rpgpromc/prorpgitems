package studio.magemonkey.divinity.manager.listener.object;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.api.DynamicStat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DynamicStatListener extends IListener<Divinity> {

    public DynamicStatListener(@NotNull Divinity plugin) {
        super(plugin);
    }

    public static void updateItem(@Nullable Player p, @NotNull ItemStack item) {
        for (DynamicStat<?> dynamicStat : ItemStats.getDynamicStats()) {
            dynamicStat.updateItem(p, item);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack item = e.getItemDrop().getItemStack();
        updateItem(null, item);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPick(EntityPickupItemEvent e) {
        LivingEntity entity = e.getEntity();
        if (!(entity instanceof Player)) return;

        Player    player = (Player) entity;
        ItemStack cursor = e.getItem().getItemStack();
        updateItem(player, cursor);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInvOpen(InventoryOpenEvent e) {
        List<ItemStack> list   = new ArrayList<>();
        Player          player = (Player) e.getPlayer();

        list.addAll(Arrays.asList(e.getInventory().getContents()));
        list.addAll(Arrays.asList(player.getInventory().getArmorContents()));
        list.forEach(item -> {
            if (item != null) {
                updateItem(player, item);
            }
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInvClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();

        List<ItemStack> list = new ArrayList<>();
        list.addAll(Arrays.asList(player.getInventory().getContents()));
        list.addAll(Arrays.asList(player.getInventory().getArmorContents()));
        list.forEach(item -> {
            if (item != null) {
                updateItem(player, item);
            }
        });
    }
}
