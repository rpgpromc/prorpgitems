package studio.magemonkey.divinity.modules.list.magicdust;

import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.external.VaultHK;
import studio.magemonkey.codex.hooks.external.citizens.CitizensHK;
import studio.magemonkey.codex.manager.api.gui.*;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.actions.ActionManipulator;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.LimitedItem;
import studio.magemonkey.divinity.modules.ModuleItem;
import studio.magemonkey.divinity.modules.RatedItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.list.magicdust.MagicDustManager.MagicDust;
import studio.magemonkey.divinity.modules.list.magicdust.command.DustOpenCmd;
import studio.magemonkey.divinity.modules.list.magicdust.event.PlayerImproveItemSocketRateEvent;
import studio.magemonkey.divinity.stats.items.ItemStats;

import java.util.*;

public class MagicDustManager extends QModuleDrop<MagicDust> {

    PaidGUI guiPaid;
    private ActionManipulator actionsApply;
    private ActionManipulator actionsError;

    public MagicDustManager(@NotNull Divinity plugin) {
        super(plugin, MagicDust.class);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.MAGIC_DUST;
    }

    @Override
    @NotNull
    public String version() {
        return "1.9.0";
    }

    @Override
    public void setup() {
        this.moduleCommand.addSubCommand(new DustOpenCmd(this));

        this.actionsApply = new ActionManipulator(plugin, cfg, "general.actions-apply");
        this.actionsError = new ActionManipulator(plugin, cfg, "general.actions-error");

        this.guiPaid = new PaidGUI(this.plugin);

        CitizensHK citizens = plugin.getCitizens();
        if (citizens != null) {
            TraitInfo trait = TraitInfo.create(MagicDustTrait.class).withName("magicdust");
            citizens.registerTrait(plugin, trait);
        }
    }

    @Override
    public void shutdown() {
        if (this.guiPaid != null) {
            this.guiPaid.shutdown();
            this.guiPaid = null;
        }
        this.actionsApply = null;
        this.actionsError = null;
    }

    // -------------------------------------------------------------------- //
    // EVENTS

    @Override
    protected boolean onDragDrop(
            @NotNull Player player,
            @NotNull ItemStack src,
            @NotNull ItemStack target,
            @NotNull MagicDust mDust,
            @NotNull InventoryClickEvent e) {

        if (!this.isRateableItem(target)) return false;

        int targetRate = ItemStats.getSocketRate(target);
        if (targetRate < 0) return false;

        // If item success rate is above the cap.
        if (targetRate >= mDust.getMaxRate()) {
            plugin.lang().MagicDust_Apply_Error_MaxRate
                    .replace("%source%", ItemUT.getItemName(src))
                    .replace("%max-rate%", String.valueOf(targetRate))
                    .send(player);
            this.actionsError.process(player);
            return false;
        }

        int rateAdd   = mDust.getRateByLevel(ItemStats.getLevel(src));
        int rateFinal = targetRate + rateAdd;

        PlayerImproveItemSocketRateEvent event =
                new PlayerImproveItemSocketRateEvent(player, target, targetRate, rateFinal);
        plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            this.actionsError.process(player);
            return false;
        }

        this.takeChargeClickEvent(player, src, e);

        // Save other items in stack
        // and then return them back to a player
        ItemStack lost = null;
        if (target.getAmount() > 1) {
            lost = new ItemStack(target);
            lost.setAmount(target.getAmount() - 1);
            target.setAmount(1);
        }

        e.setCurrentItem(this.changeItemRate(target, rateAdd));

        if (lost != null) {
            ItemUT.addItem(player, lost);
        }

        return true;
    }

    @NotNull
    public ItemStack changeItemRate(@NotNull ItemStack target, int toAdd) {
        QModuleDrop<?> moduleDrop = ItemStats.getModule(target);
        if (moduleDrop == null || !moduleDrop.isLoaded()) {
            return target;
        }

        int targetRate = ItemStats.getSocketRate(target);
        if (targetRate < 0) return target;

        ModuleItem mItem = moduleDrop.getModuleItem(target);
        if (mItem == null || !(mItem instanceof RatedItem)) return target;

        RatedItem rItem      = (RatedItem) mItem;
        int       targetLvl  = ItemStats.getLevel(target);
        int       targetUses = moduleDrop.getItemCharges(target);
        int       rateFinal  = Math.max(0, targetRate + toAdd);

        return rItem.create(targetLvl, targetUses, rateFinal);
    }

    public boolean isRateableItem(@NotNull ItemStack target) {
        QModuleDrop<?> moduleDrop = ItemStats.getModule(target);
        if (moduleDrop == null || !moduleDrop.isLoaded()) {
            return false;
        }

        ModuleItem mItem = moduleDrop.getModuleItem(target);
        return mItem != null && mItem instanceof RatedItem;
    }

    public void openGUIPaid(@NotNull Player player, @Nullable ItemStack target, boolean force) {
        if (!force && !player.hasPermission(Perms.MAGIC_DUST_GUI)) {
            plugin.lang().Error_NoPerm.send(player);
            return;
        }

        if (target == null) {
            target = new ItemStack(Material.AIR);
        }
        if (target.getType() != Material.AIR) {
            if (!this.isRateableItem(target)) {
                plugin.lang().MagicDust_GUI_Error_InvalidItem
                        .replace("%item%", ItemUT.getItemName(target))
                        .send(player);
                return;
            }
            this.splitDragItem(player, null, target);
        }
        this.guiPaid.open(player, target);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onImproveRate(PlayerImproveItemSocketRateEvent e) {
        Player    player    = e.getPlayer();
        ItemStack target    = e.getItem();
        int       rateFinal = e.getRateNew();

        this.actionsApply.process(player);

        plugin.lang().MagicDust_Apply_Done
                .replace("%item%", ItemUT.getItemName(target))
                .replace("%rate-new%", String.valueOf(rateFinal))
                .send(player);
    }

    // -------------------------------------------------------------------- //
    // CLASSES

    public class MagicDust extends LimitedItem {

        private final int                       rateMax;
        private final TreeMap<Integer, Integer> rateLvl;

        public MagicDust(@NotNull Divinity plugin, @NotNull JYML cfg) {
            super(plugin, cfg, MagicDustManager.this);

            this.rateMax = Math.min(100, cfg.getInt("rate-increasing.max-value", 80));

            this.rateLvl = new TreeMap<>();
            for (String sLvl : cfg.getSection("rate-increasing.values-by-level")) {
                int lvl = StringUT.getInteger(sLvl, -1);
                if (lvl < 0) continue;

                int rate = cfg.getInt("rate-increasing.values-by-level." + sLvl);
                this.rateLvl.put(lvl, rate);
            }
        }

        public int getMaxRate() {
            return this.rateMax;
        }

        public int getRateByLevel(int lvl) {
            Map.Entry<Integer, Integer> e = this.rateLvl.floorEntry(lvl);
            if (e == null) return 0;

            return e.getValue();
        }

        @Override
        @NotNull
        protected ItemStack build(int lvl, int uses) {
            ItemStack item = super.build(lvl, uses);

            ItemMeta meta = item.getItemMeta();
            if (meta == null) return item;

            List<String> lore = meta.getLore();
            if (lore == null) return item;

            int rate = this.getRateByLevel(lvl);

            lore.replaceAll(str -> str
                    .replace("%rate-max%", String.valueOf(this.getMaxRate()))
                    .replace("%rate-amount%", String.valueOf(rate)));
            meta.setLore(lore);
            item.setItemMeta(meta);

            return item;
        }
    }

    class PaidGUI extends NGUI<Divinity> {

        private final int          itemSlot;
        private final VaultHK      vault;
        private final boolean      hasVault;
        private final Set<GuiItem> buttons;

        public PaidGUI(@NotNull Divinity plugin) {
            super(plugin, cfg, "gui.paid.");

            this.buttons = new HashSet<>();
            this.vault = plugin.getVault();
            this.hasVault = this.vault != null && this.vault.getEconomy() != null;
            String path = "gui.paid.";

            this.itemSlot = cfg.getInt(path + "item-slot", 4);

            for (String itemId : cfg.getSection(path + "dust-buttons")) {
                GuiItem gi = cfg.getGuiItem(path + "dust-buttons." + itemId);
                if (gi == null) continue;

                String path2      = path + "dust-buttons." + itemId + ".";
                int    rateAmount = cfg.getInt(path2 + "rate-amount");
                if (rateAmount == 0) continue;
                double rateCost = this.hasVault ? cfg.getDouble(path2 + "rate-cost") : 0D;
                int    rateCap  = cfg.getInt(path2 + "rate-max", 80);

                ItemStack item = gi.getItem();
                ItemMeta  meta = item.getItemMeta();
                if (meta == null) continue;

                List<String> lore = meta.getLore();
                if (lore != null) {
                    lore.replaceAll(str -> str
                            .replace("%rate-max%", String.valueOf(rateCap)));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
                gi.setItem(item);

                gi.setClick(new GuiClick() {
                    @Override
                    public void click(@NotNull Player p, @Nullable Enum<?> type, @NotNull InventoryClickEvent e) {
                        Inventory inv    = e.getInventory();
                        ItemStack target = inv.getItem(itemSlot);
                        if (target == null || target.getType() == Material.AIR) return;

                        int rateHas = ItemStats.getSocketRate(target);
                        if (rateHas >= rateCap) {
                            plugin.lang().MagicDust_Apply_Error_MaxRate
                                    .replace("%source%", getTitle())
                                    .replace("%max-rate%", String.valueOf(rateHas))
                                    .send(p);
                            MagicDustManager.this.actionsError.process(p);
                            return;
                        }

                        // Calc rate add amount depends on current item rate.
                        int rateDiff    = (rateCap - rateHas);
                        int rateAmount2 = rateDiff >= rateAmount ? rateAmount : rateDiff;

                        // Do fair cost if not full rate amount will be added.
                        double rateCost2 = rateCost;
                        if (rateCost > 0 && rateAmount2 < rateAmount) {
                            double costDiff = (double) rateAmount2 / (double) rateAmount;
                            rateCost2 *= costDiff;
                        }

                        // Take user money for GUI dust operations.
                        if (rateCost2 > 0) {
                            double balance = vault.getBalance(p);
                            if (balance < rateCost2) {
                                plugin.lang().MagicDust_GUI_Error_TooExpensive
                                        .replace("%cost%", NumberUT.format(rateCost2))
                                        .replace("%balance%", NumberUT.format(balance))
                                        .send(p);
                                MagicDustManager.this.actionsError.process(p);
                                return;
                            }
                        }

                        // Call custom plugin event.
                        PlayerImproveItemSocketRateEvent event =
                                new PlayerImproveItemSocketRateEvent(p, target, rateHas, rateHas + rateAmount2);
                        plugin.getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            MagicDustManager.this.actionsError.process(p);
                            return;
                        }

                        // Take money only at the end of operation.
                        PaidGUI.this.vault.take(p, rateCost2);

                        // A bit hacky way to prevent duplication due to
                        // onClose event item return and JGUI re-open while it's opened.
                        inv.setItem(itemSlot, null);
                        ItemUT.addItem(p, target = changeItemRate(target, rateAmount2));
                        openGUIPaid(p, target, true);
                    }
                });

                this.buttons.add(gi);
            }

            GuiClick click = new GuiClick() {
                @Override
                public void click(@NotNull Player p, @Nullable Enum<?> type, @NotNull InventoryClickEvent e) {
                    if (type == ContentType.EXIT) {
                        p.closeInventory();
                    }
                }
            };

            for (String itemId : cfg.getSection(path + "content")) {
                GuiItem guiItem = cfg.getGuiItem(path + "content." + itemId, ContentType.class);
                if (guiItem == null) continue;

                if (guiItem.getType() != null) {
                    guiItem.setClick(click);
                }

                this.addButton(guiItem);
            }
        }

        public void open(@NotNull Player player, @NotNull ItemStack target) {
            // Fix GUI issues when re-open it.
            // NOT A BUG, but not intended JGUI usage.
            this.clearUserCache(player);
            this.LOCKED_CACHE.add(player.getName());

            this.addButton(player, new JIcon(target), this.itemSlot);

            int    rateHas = ItemStats.getSocketRate(target);
            double balance = this.hasVault ? this.vault.getBalance(player) : 0D;

            // Hack to replace GUI item placeholders.
            for (GuiItem gi : this.buttons) {
                ItemStack item = gi.getItem();
                ItemMeta  meta = item.getItemMeta();
                if (meta == null) continue;

                List<String> lore = meta.getLore();
                if (lore != null) {
                    // Accessing button values via module config is the only way.
                    int rateAmount = cfg.getInt("gui.paid.dust-buttons." + gi.getId() + ".rate-amount");
                    int rateCap    = cfg.getInt("gui.paid.dust-buttons." + gi.getId() + ".rate-max");
                    double rateCost =
                            this.hasVault ? cfg.getInt("gui.paid.dust-buttons." + gi.getId() + ".rate-cost") : 0D;

                    int rateDiff    = (rateCap - rateHas);
                    int rateAmount2 = rateDiff >= rateAmount ? rateAmount : rateDiff;

                    // Do fair cost if not full rate amount will be added.
                    if (rateCost > 0 && rateAmount2 < rateAmount) {
                        double costDiff = (double) rateAmount2 / (double) rateAmount;
                        rateCost *= costDiff;
                    }
                    double rateCost2 = rateCost;

                    lore.replaceAll(str -> str
                            .replace("%cost%", NumberUT.format(rateCost2))
                            .replace("%balance%", NumberUT.format(balance))
                            .replace("%rate-new%", String.valueOf(rateHas + rateAmount2))
                            .replace("%rate-old%", String.valueOf(rateHas)));
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }

                JIcon icon = new JIcon(item);
                icon.setClick(gi.getClick());

                for (int slot : gi.getSlots()) {
                    this.addButton(player, icon, slot);
                }
            }

            super.open(player, 1);
        }

        @Override
        protected boolean cancelClick(int slot) {
            return true;
        }

        @Override
        protected boolean cancelPlayerClick() {
            return true;
        }

        @Override
        protected boolean ignoreNullClick() {
            return true;
        }

        @Override
        protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {

        }

        @Override
        protected void click(@NotNull Player player,
                             @Nullable ItemStack item,
                             int slot,
                             @NotNull InventoryClickEvent e) {
            Inventory inv    = e.getInventory();
            ItemStack target = inv.getItem(itemSlot);

            // Return GUI item to player inventory.
            if (slot == this.itemSlot && target != null && target.getType() != Material.AIR) {
                inv.setItem(itemSlot, target);
                open(player, new ItemStack(Material.AIR));
                return;
            }

            // Place user item to the GUI.
            if (slot > this.getSize() && (target == null || target.getType() == Material.AIR)) {
                if (item == null || item.getType() == Material.AIR) return;
                openGUIPaid(player, item, true);
                return;
            }

            super.click(player, item, slot, e);
        }

        @Override
        protected void onClose(@NotNull Player player, @NotNull InventoryCloseEvent e) {
            Inventory inv    = e.getInventory();
            ItemStack target = inv.getItem(itemSlot);
            // Return user items on GUI close to avoid item losing.
            if (target != null && target.getType() != Material.AIR) {
                ItemUT.addItem(player, target);
            }
            super.onClose(player, e);
        }
    }
}
