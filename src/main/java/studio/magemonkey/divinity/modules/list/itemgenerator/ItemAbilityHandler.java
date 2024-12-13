package studio.magemonkey.divinity.modules.list.itemgenerator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.armor.ArmorEquipEvent;
import studio.magemonkey.codex.compat.VersionManager;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.codex.manager.api.Loadable;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.hooks.EHook;
import studio.magemonkey.divinity.hooks.external.FabledHook;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager.GeneratorItem;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.stats.DurabilityStat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemAbilityHandler extends IListener<Divinity> implements Loadable {

    private final ItemGeneratorManager itemGen;
    private final List<UUID>           noSpam = new ArrayList<>();

    ItemAbilityHandler(@NotNull ItemGeneratorManager itemGen) {
        super(itemGen.plugin);
        this.itemGen = itemGen;
    }

    @Override
    public void setup() {
        this.registerListeners();
    }

    @Override
    public void shutdown() {
        this.unregisterListeners();
    }

    private boolean registerSentMessage(Player player) {
        if (noSpam.contains(player.getUniqueId())) return false;

        noSpam.add(player.getUniqueId());
        //TODO Make this cooldown configurable.
        Bukkit.getScheduler().runTaskLater(this.plugin,
                () -> noSpam.remove(player.getUniqueId()), 60L);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        FabledHook fabledHook = (FabledHook) this.plugin.getHook(EHook.SKILL_API);
        if (fabledHook != null) {
            fabledHook.updateSkills(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void playerDeath(PlayerDeathEvent event) {
        FabledHook fabledHook = (FabledHook) this.plugin.getHook(EHook.SKILL_API);
        if (fabledHook != null) {
            fabledHook.updateSkills(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void itemSwap(PlayerSwapHandItemsEvent event) {
        FabledHook fabledHook = (FabledHook) this.plugin.getHook(EHook.SKILL_API);
        if (fabledHook != null) {
            fabledHook.updateSkills(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemHeldEvent(PlayerItemHeldEvent event) {
        FabledHook fabledHook = (FabledHook) this.plugin.getHook(EHook.SKILL_API);
        if (fabledHook != null) {
            fabledHook.updateSkills(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        FabledHook fabledHook = (FabledHook) this.plugin.getHook(EHook.SKILL_API);
        if (fabledHook != null) {
            fabledHook.updateSkills(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player     player     = (Player) event.getEntity();
        FabledHook fabledHook = (FabledHook) this.plugin.getHook(EHook.SKILL_API);
        if (fabledHook != null) {
            fabledHook.updateSkills(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorEquip(ArmorEquipEvent event) {
        FabledHook fabledHook = (FabledHook) this.plugin.getHook(EHook.SKILL_API);
        if (fabledHook != null) {
            fabledHook.updateSkills(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        updateSkills(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        updateSkills(event);
    }

    private void updateSkills(InventoryEvent event) {
        Inventory top    = VersionManager.getCompat().getTopInventory(event);
        Inventory bottom = VersionManager.getCompat().getBottomInventory(event);
        for (Inventory inventory : new Inventory[]{top, bottom}) {
            if (!(inventory instanceof PlayerInventory)) {
                continue;
            }
            PlayerInventory playerInventory = (PlayerInventory) inventory;
            HumanEntity     humanEntity     = playerInventory.getHolder();
            if (!(humanEntity instanceof Player)) {
                return;
            }
            FabledHook fabledHook = (FabledHook) this.plugin.getHook(EHook.SKILL_API);
            if (fabledHook != null) {
                fabledHook.updateSkills((Player) humanEntity);
            }
        }
    }

    private boolean useItem(@NotNull Player player, @NotNull ItemStack item) {
        int uses = this.itemGen.getItemCharges(item);
        if (uses == 0) {
            if (registerSentMessage(player)) {
                plugin.lang().Module_Item_Usage_NoCharges
                        .replace("%item%", ItemUT.getItemName(item))
                        .send(player);
            }
            return false;
        }

        // TODO Usage Event

        if (uses > 0) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);

                ItemStack reuse = new ItemStack(item);
                reuse.setAmount(1);
                this.itemGen.takeItemCharge(reuse);

                if (reuse.getType() != Material.AIR) {
                    ItemUT.addItem(player, reuse);
                }
            } else {
                this.itemGen.takeItemCharge(item);
            }
        } else {
            DurabilityStat duraStat = ItemStats.getStat(DurabilityStat.class);
            if (duraStat != null) {
                duraStat.reduceDurability(player, item, 1);
            }
        }
        return true;
    }

    // ------------------------------------------------------------- //
    // E V E N T S

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemConsumeNatural(PlayerItemConsumeEvent e) {

        ItemStack     item  = new ItemStack(e.getItem());
        GeneratorItem aItem = this.itemGen.getModuleItem(item);
        if (aItem == null) return;

        Player player = e.getPlayer();

        e.setCancelled(true);

        ItemStack itemMain = player.getInventory().getItemInMainHand();
        if (!ItemUT.isAir(itemMain) && itemMain.isSimilar(item)) {
            this.useItem(player, itemMain);
            return;
        }

        ItemStack itemOff = player.getInventory().getItemInOffHand();
        if (!ItemUT.isAir(itemOff) && itemOff.isSimilar(item)) {
            this.useItem(player, itemOff);
        }
    }
}
