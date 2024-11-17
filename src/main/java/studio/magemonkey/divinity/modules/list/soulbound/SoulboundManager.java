package studio.magemonkey.divinity.modules.list.soulbound;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.commands.CommandRegister;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.api.QModule;
import studio.magemonkey.divinity.modules.list.soulbound.command.SoulboundSoulCmd;
import studio.magemonkey.divinity.modules.list.soulbound.command.SoulboundUntradeCmd;
import studio.magemonkey.divinity.stats.items.requirements.ItemRequirements;
import studio.magemonkey.divinity.stats.items.requirements.user.SoulboundRequirement;
import studio.magemonkey.divinity.stats.items.requirements.user.UntradeableRequirement;

import java.util.*;

public class SoulboundManager extends QModule {

    private boolean bindOnDrop;
    private boolean bindOnPickup;
    private boolean bindOnClick;
    private boolean bindOnUse;

    private Set<String> interactBlockedCmds;
    private boolean     interactAllowDrop;
    private boolean     interactAllowDeathDrop;

    private SoulGUI                gui;
    private SoulboundRequirement   reqSoul;
    private UntradeableRequirement reqUntrade;

    public static String SOULBOUND_FORMAT_FREE;
    public static String SOULBOUND_FORMAT_APPLIED;

    public static String UNTRADE_FORMAT_FREE;
    public static String UNTRADE_FORMAT_APPLIED;

    public SoulboundManager(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.SOULBOUND;
    }

    @Override
    @NotNull
    public String version() {
        return "1.6.0";
    }

    @Override
    public void setup() {
        String path = "item-requirements.";
        if (cfg.getBoolean(path + "soulbound.enabled")) {
            String path2     = "item-requirements.soulbound.";
            String reqFormat = cfg.getString(path2 + "format.main", "&6%state%%name%: %value%");
            String reqName   = cfg.getString(path2 + "name", "Soulbound");

            SOULBOUND_FORMAT_FREE = StringUT.color(cfg.getString(path2 + "format.value.free", "Required"));
            SOULBOUND_FORMAT_APPLIED = StringUT.color(cfg.getString(path2 + "format.value.applied", "%player%"));

            this.reqSoul = new SoulboundRequirement(reqName, reqFormat);
            ItemRequirements.registerUserRequirement(this.reqSoul);
            this.moduleCommand.addSubCommand(new SoulboundSoulCmd(this, this.reqSoul));
        }
        if (cfg.getBoolean(path + "untradeable.enabled")) {
            String path2     = "item-requirements.untradeable.";
            String reqFormat = cfg.getString(path2 + "format.main", "&6%state%%name%: %value%");
            String reqName   = cfg.getString(path2 + "name", "Trade State");

            UNTRADE_FORMAT_FREE =
                    StringUT.color(cfg.getString(path2 + "format.value.free", "Will be untradeable on pickup"));
            UNTRADE_FORMAT_APPLIED = StringUT.color(cfg.getString(path2 + "format.value.applied", "Untradeable"));

            this.reqUntrade = new UntradeableRequirement(reqName, reqFormat);
            ItemRequirements.registerUserRequirement(this.reqUntrade);
            this.moduleCommand.addSubCommand(new SoulboundUntradeCmd(this, this.reqUntrade));
        }

        path = "bind-to-player.";
        this.bindOnDrop = cfg.getBoolean(path + "on-item-drop");
        this.bindOnPickup = cfg.getBoolean(path + "on-item-pickup");
        this.bindOnClick = cfg.getBoolean(path + "on-item-click");
        this.bindOnUse = cfg.getBoolean(path + "on-item-interact");

        path = "interact.";
        this.interactBlockedCmds = new HashSet<>(cfg.getStringList(path + "blocked-commands"));
        this.interactAllowDrop = cfg.getBoolean(path + "allow-drop");
        this.interactAllowDeathDrop = cfg.getBoolean(path + "drop-on-death");

        this.gui = new SoulGUI(this);
    }

    @Override
    public void shutdown() {
        if (this.gui != null) {
            this.gui.shutdown();
            this.gui = null;
        }
        if (this.interactBlockedCmds != null) {
            this.interactBlockedCmds.clear();
            this.interactBlockedCmds = null;
        }
    }

    private boolean hasOwner(@NotNull ItemStack item) {
        return this.getOwner(item) != null;
    }

    private boolean isOwner(@NotNull ItemStack item, @NotNull Player player) {
        UUID id = this.getOwner(item);
        return id != null && id.equals(player.getUniqueId());
    }

    /**
     * @param item Soulbounded/Untradeable ItemStack
     * @return Returns an UUID in String of the item Owner.
     */
    @Nullable
    private UUID getOwner(@NotNull ItemStack item) {
        if (this.reqSoul != null && this.reqSoul.isApplied(item) && !this.reqSoul.isRequired(item)) {
            return this.reqSoul.getRaw(item);
        }
        if (this.reqUntrade != null && this.reqUntrade.isApplied(item) && !this.reqUntrade.isRequired(item)) {
            return this.reqUntrade.getRaw(item);
        }
        return null;
    }

    private void openGUI(@NotNull Player player, @NotNull ItemStack item) {
        if (this.reqSoul == null) return;

        ItemStack src  = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) src.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(StringUT.color("&c" + player.getName()));
            meta.setOwningPlayer(player);
            src.setItemMeta(meta);
        }

        ItemStack result = new ItemStack(item);
        this.reqSoul.add(result, player.getUniqueId(), -1);
        this.gui.open(player, item, src, result);
    }

    // -------------------------------------------------------------------- //

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSoulPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;

        ItemStack item   = e.getItem().getItemStack();
        Player    player = (Player) e.getEntity();

        if (this.hasOwner(item)) {
            if (!this.isOwner(item, player)) {
                e.setCancelled(true);
                plugin.lang().Soulbound_Item_Interact_Error_Pickup.send(player);
            }
            return;
        }

        if (this.reqUntrade != null && this.reqUntrade.isApplied(item) && this.bindOnPickup) {
            this.reqUntrade.add(item, player.getUniqueId(), -1);
            this.reqUntrade.updateItem(player, item);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSoulStart(InventoryClickEvent e) {
        if (e.getSlotType() == SlotType.CRAFTING) return;
        if (e.getSlotType() == SlotType.ARMOR || e.getSlot() == 40) return;

        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        Player    p   = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();

        if (inv.getType() == InventoryType.CRAFTING) {
            if (this.reqSoul != null && this.reqSoul.isRequired(item) && e.isRightClick() && !e.isShiftClick()) {
                this.openGUI(p, item);
                e.setCurrentItem(null);
                e.setCancelled(true);
                return;
            }
        } else {
            if (this.hasOwner(item)) {
                if (!this.isOwner(item, p) && !p.hasPermission(Perms.BYPASS_REQ_USER_UNTRADEABLE)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        if (this.reqUntrade != null && this.reqUntrade.isApplied(item) && !this.hasOwner(item) && this.bindOnClick) {
            this.reqUntrade.add(item, p.getUniqueId(), -1);
            this.reqUntrade.updateItem(p, item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBindUntradeUse(PlayerInteractEvent e) {
        if (this.reqUntrade == null) return;
        if (e.useItemInHand() == Result.DENY) return;

        ItemStack item = e.getItem();
        if (item == null) return;

        if (this.reqUntrade.isApplied(item) && !this.hasOwner(item) && this.bindOnUse) {
            Player p = e.getPlayer();
            this.reqUntrade.add(item, p.getUniqueId(), -1);
            this.reqUntrade.updateItem(p, item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBindItemDrop(PlayerDropItemEvent e) {
        Item      drop = e.getItemDrop();
        ItemStack item = drop.getItemStack();

        if (this.hasOwner(item)) {
            if (!this.interactAllowDrop) {
                e.setCancelled(true);
            }
            return;
        }

        if (this.reqUntrade != null && this.reqUntrade.isApplied(item) && this.bindOnDrop) {
            Player p = e.getPlayer();
            this.reqUntrade.add(item, p.getUniqueId(), -1);
            this.reqUntrade.updateItem(p, item);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSoulboundCommands(PlayerCommandPreprocessEvent e) {
        if (this.interactBlockedCmds.isEmpty()) return;
        Player player = e.getPlayer();

        ItemStack item  = player.getInventory().getItemInMainHand();
        ItemStack item2 = player.getInventory().getItemInOffHand();

        if ((!ItemUT.isAir(item) && this.hasOwner(item)) || (!ItemUT.isAir(item2) && this.hasOwner(item2))) {
            String msg = e.getMessage();
            String cmd = StringUT.extractCommandName(msg);
            for (String alias : CommandRegister.getAliases(cmd)) {
                if (this.interactBlockedCmds.contains(alias)) {
                    e.setCancelled(true);
                    plugin.lang().Soulbound_Item_Interact_Error_Command.send(player);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSoulboundHopper(InventoryPickupItemEvent e) {
        ItemStack item = e.getItem().getItemStack();
        if (e.getInventory().getType() == InventoryType.HOPPER && hasOwner(item)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSoulboundDeath(PlayerDeathEvent e) {
        if (this.interactAllowDeathDrop) return;

        final List<ItemStack> list2 = new ArrayList<>();
        for (ItemStack item : new ArrayList<>(e.getDrops())) {
            if (this.hasOwner(item)) {
                e.getDrops().remove(item);
                list2.add(item);
            }
        }

        final Player player = e.getEntity();
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                for (ItemStack i : list2) {
                    player.getInventory().addItem(i);
                }
            }
        });
    }
}
