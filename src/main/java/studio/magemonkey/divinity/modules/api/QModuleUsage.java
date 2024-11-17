package studio.magemonkey.divinity.modules.api;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.TimeUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.event.DivinityPlayerItemUseEvent;
import studio.magemonkey.divinity.modules.UsableItem;
import studio.magemonkey.divinity.modules.list.consumables.ConsumablesManager;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.stats.DurabilityStat;
import studio.magemonkey.divinity.types.QClickType;
import studio.magemonkey.divinity.utils.ItemUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Deprecated
public abstract class QModuleUsage<I extends UsableItem> extends QModuleDrop<I> {
    protected Map<String, Map<String, Set<UsableItem.Cooldown>>> itemCooldown;

    public QModuleUsage(@NotNull Divinity plugin, @NotNull Class<I> clazz) {
        super(plugin, clazz);
    }

    protected void loadSettings() {
        this.itemCooldown = new HashMap<>();
        super.loadSettings();
    }

    public void unload() {
        super.unload();
        if (this.itemCooldown != null) {
            this.itemCooldown.clear();
            this.itemCooldown = null;
        }
    }

    private final boolean useItem(@NotNull Player p,
                                  @NotNull ItemStack item,
                                  @NotNull UsableItem uItem,
                                  @NotNull QClickType type) {
        UsableItem.Usage aUsage = uItem.getUsage(type);
        if (aUsage == null)
            return false;
        if (isOnCooldown(p, item, type)) {
            long   left = getCooldownLeft(p, item, type);
            String name = ItemUT.getItemName(item);
            String time = TimeUT.formatTime(left);
            (this.plugin.lang()).Module_Item_Usage_Cooldown
                    .replace("%time%", time).replace("%item%", name)
                    .send(p);
            return false;
        }
        DivinityPlayerItemUseEvent eve = new DivinityPlayerItemUseEvent(item, p, uItem, type);
        this.plugin.getPluginManager().callEvent(eve);
        if (eve.isCancelled())
            return false;
        int lvl  = ItemStats.getLevel(item);
        int uses = getItemCharges(item);
        if (uses > 0) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
                ItemStack reuse = new ItemStack(item);
                reuse.setAmount(1);
                takeItemCharge(reuse);
                if (reuse.getType() != Material.AIR)
                    ItemUT.addItem(p, reuse);
            } else {
                takeItemCharge(item);
            }
        } else {
            if (uItem instanceof ConsumablesManager.Consume && uses == 0) {
                item.setAmount(item.getAmount() - 1);
            } else {
                DurabilityStat duraStat = ItemStats.getStat(DurabilityStat.class);
                if (duraStat != null)
                    duraStat.reduceDurability(p, item, 1);
            }
        }
        aUsage.use(p, lvl);
        setCooldown(p, uItem, type);
        return true;
    }

    public boolean isOnCooldown(@NotNull Player p, @NotNull ItemStack item, @NotNull QClickType type) {
        if (!isItemOfThisModule(item))
            return false;
        String                                uuid = p.getUniqueId().toString();
        Map<String, Set<UsableItem.Cooldown>> map  = null;
        if (this.itemCooldown.containsKey(uuid))
            map = this.itemCooldown.get(uuid);
        if (map == null) {
            map = new HashMap<>();
        } else {
            for (Map.Entry<String, Set<UsableItem.Cooldown>> e : map.entrySet()) {
                Set<UsableItem.Cooldown> itemCooldowns = e.getValue();
                for (UsableItem.Cooldown iCooldown : itemCooldowns) {
                    if (iCooldown.isExpired())
                        itemCooldowns.remove(iCooldown);
                }
            }
        }
        String                   itemId = getItemId(item);
        Set<UsableItem.Cooldown> list   = map.get(itemId);
        if (list == null || list.isEmpty())
            return false;
        for (UsableItem.Cooldown i : list) {
            if (i.getClickType() == type)
                return !i.isExpired();
        }
        return false;
    }

    private final long getCooldownLeft(@NotNull Player p, @NotNull ItemStack item, @NotNull QClickType type) {
        String                                u   = p.getUniqueId().toString();
        Map<String, Set<UsableItem.Cooldown>> map = this.itemCooldown.get(u);
        String                                id  = getItemId(item);
        for (UsableItem.Cooldown i : map.get(id)) {
            if (i.getClickType() == type)
                return i.getTimeExpire() - System.currentTimeMillis();
        }
        return 0L;
    }

    private final void setCooldown(@NotNull Player p, @NotNull UsableItem aItem, @NotNull QClickType click) {
        String                                uuid = p.getUniqueId().toString();
        Map<String, Set<UsableItem.Cooldown>> aMap = null;
        if (this.itemCooldown.containsKey(uuid))
            aMap = this.itemCooldown.get(uuid);
        if (aMap == null)
            aMap = new HashMap<>();
        UsableItem.Usage aUsage = aItem.getUsage(click);
        if (aUsage == null || aUsage.getCooldown() <= 0.0D)
            return;
        String                   id   = aItem.getId();
        UsableItem.Cooldown      ic   = new UsableItem.Cooldown(id, click, aUsage.getCooldown());
        Set<UsableItem.Cooldown> list = aMap.get(id);
        if (list == null)
            list = new HashSet<>();
        for (UsableItem.Cooldown i : list) {
            if (i.getClickType() == click)
                list.remove(i);
        }
        list.add(ic);
        aMap.put(id, list);
        this.itemCooldown.put(uuid, aMap);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onItemConsume(PlayerInteractEvent e) {
        if (e.useItemInHand() == Event.Result.DENY)
            return;
        EquipmentSlot hand = e.getHand();
        if (hand == null)
            return;
        ItemStack item = e.getItem();
        if (item == null)
            return;
        if (item.getType().isEdible() || item.getType() == Material.POTION)
            return;
        UsableItem aItem = getModuleItem(item);
        if (aItem == null)
            return;
        Player     p     = e.getPlayer();
        Action     a     = e.getAction();
        boolean    shift = p.isSneaking();
        QClickType type  = QClickType.getFromAction(a, shift);
        if (type == null)
            return;
        if (!ItemUtils.isWeapon(item) && !ItemUtils.isBow(item) && item.getType() != Material.SHIELD)
            e.setCancelled(true);
        useItem(p, item, aItem, type);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemConsumeNatural(PlayerItemConsumeEvent e) {
        ItemStack  item  = new ItemStack(e.getItem());
        UsableItem aItem = getModuleItem(item);
        if (aItem == null)
            return;
        Player     p    = e.getPlayer();
        QClickType type = QClickType.RIGHT;
        e.setCancelled(true);
        ItemStack itemMain = p.getInventory().getItemInMainHand();
        if (itemMain != null && itemMain.isSimilar(item)) {
            useItem(p, itemMain, aItem, type);
            return;
        }
        ItemStack itemOff = p.getInventory().getItemInOffHand();
        if (itemOff != null && itemOff.isSimilar(item))
            useItem(p, itemOff, aItem, type);
    }
}
