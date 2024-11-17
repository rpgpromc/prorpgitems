package studio.magemonkey.divinity.modules.list.sell;

import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.external.VaultHK;
import studio.magemonkey.codex.manager.api.gui.ContentType;
import studio.magemonkey.codex.manager.api.gui.GuiClick;
import studio.magemonkey.codex.manager.api.gui.GuiItem;
import studio.magemonkey.codex.manager.api.gui.NGUI;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.sell.event.PlayerPreSellItemEvent;
import studio.magemonkey.divinity.modules.list.sell.event.PlayerSellItemEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SellGUI extends NGUI<Divinity> {

    private final int[]   itemSlots;
    private final VaultHK vault;

    public SellGUI(@NotNull SellManager sellManager) {
        super(sellManager.plugin, sellManager.getJYML(), "gui.");

        JYML   cfg  = sellManager.getJYML();
        String path = "gui.";
        this.vault = plugin.getVault();

        this.itemSlots = cfg.getIntArray(path + "item-slots");

        GuiClick click = (p, type, e) -> {
            if (type == ContentType.ACCEPT) {
                Inventory              inv        = e.getInventory();
                Map<ItemStack, Double> priceMap   = new HashMap<>();
                double                 priceTotal = 0;

                for (int slot : itemSlots) {
                    ItemStack target = inv.getItem(slot);
                    if (target == null) continue;

                    double price = plugin.getWorthManager().getItemWorth(target);
                    if (price <= 0) {
                        ItemUT.addItem(p, target);
                    } else {
                        priceTotal += price;
                        priceMap.computeIfAbsent(target, price2 -> 0D);
                        priceMap.computeIfPresent(target, (itemKey, priceVal) -> priceVal + price);
                        //priceMap.compute(target, (itemKey, itemPrice) -> priceMap.computeIfAbsent(target, price2 -> 0D) + price);
                    }
                }
                inv.setContents(new ItemStack[]{});

                if (priceTotal > 0) {
                    PlayerPreSellItemEvent event1 = new PlayerPreSellItemEvent(p, priceMap);
                    plugin.getPluginManager().callEvent(event1);
                    if (event1.isCancelled()) {
                        sellManager.actionsError.process(p);
                        return;
                    }

                    vault.give(p, event1.getPrice());

                    PlayerSellItemEvent event = new PlayerSellItemEvent(p, event1.getPriceMap());
                    plugin.getPluginManager().callEvent(event);
                }

                p.closeInventory();
            } else if (type == ContentType.EXIT) {
                p.closeInventory();
            }
        };

        for (String itemId : cfg.getSection(path + "content")) {
            GuiItem gi = cfg.getGuiItem(path + "content." + itemId, ContentType.class);
            if (gi == null) continue;

            if (gi.getType() != null) {
                gi.setClick(click);
            }

            this.addButton(gi);
        }
    }

    @Override
    protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {

    }

    @Override
    protected void onReady(@NotNull Player player, @NotNull Inventory inv, int page) {
        super.onReady(player, inv, page);
        this.update(inv);
    }

    @Override
    protected boolean ignoreNullClick() {
        return false;
    }

    @Override
    protected boolean cancelClick(int slot) {
        return slot < this.getSize() && !ArrayUtils.contains(this.itemSlots, slot);
    }

    @Override
    protected boolean cancelPlayerClick() {
        return false;
    }

    @Override
    public void click(Player player, @Nullable ItemStack item, int slot, InventoryClickEvent e) {
        // Recalc items price only if items are being added or removed from the GUI.
        // Delay needs because event called before the item is added/taken.
        if (!this.cancelClick(slot)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> this.update(e.getInventory()));
        }

        super.click(player, item, slot, e);
    }

    @Override
    public void onClose(@NotNull Player player, @NotNull InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        for (int slot : this.itemSlots) {
            ItemStack item = inv.getItem(slot);
            if (item != null) {
                ItemUT.addItem(player, item);
            }
        }
    }

    // Replace placeholders on default GUI items.
    // As these items are loaded into JGUI database,
    // we may just replace them in their slots.
    private void update(@NotNull Inventory inv) {
        for (GuiItem guiItem : this.getContent().values()) {
            ItemStack item = guiItem.getItem();
            ItemMeta  meta = item.getItemMeta();
            if (meta == null) continue;

            String cost = NumberUT.format(this.getTotalPrice(inv));

            if (meta.hasDisplayName()) {
                meta.setDisplayName(meta.getDisplayName().replace("%cost%", cost));
            }

            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.replaceAll(str -> str.replace("%cost%", cost));
                meta.setLore(lore);
            }
            item.setItemMeta(meta);

            for (int i : guiItem.getSlots()) {
                inv.setItem(i, item);
            }
        }
    }

    private double getTotalPrice(@NotNull Inventory inv) {
        double cost = 0;

        for (int i : this.itemSlots) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;

            cost += this.plugin.getWorthManager().getItemWorth(item);
        }

        return cost;
    }
}
