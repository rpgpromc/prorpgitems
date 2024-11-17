package studio.magemonkey.divinity.modules.list.dismantle;

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
import studio.magemonkey.codex.manager.api.gui.ContentType;
import studio.magemonkey.codex.manager.api.gui.GuiClick;
import studio.magemonkey.codex.manager.api.gui.GuiItem;
import studio.magemonkey.codex.manager.api.gui.NGUI;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.dismantle.DismantleManager.OutputContainer;
import studio.magemonkey.divinity.modules.list.dismantle.DismantleManager.OutputItem;
import studio.magemonkey.divinity.modules.list.dismantle.event.PlayerDismantleItemEvent;
import studio.magemonkey.divinity.modules.list.dismantle.event.PlayerPreDismantleItemEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DismantleGUI extends NGUI<Divinity> {

    protected DismantleManager dismantleManager;

    protected int[] itemSlots;
    protected int[] resultSlots;

    public DismantleGUI(@NotNull DismantleManager dismantleManager) {
        super(dismantleManager.plugin, dismantleManager.getJYML(), "gui.");
        this.dismantleManager = dismantleManager;

        JYML   cfg  = dismantleManager.getJYML();
        String path = "gui.";

        this.itemSlots = cfg.getIntArray(path + "item-slots");
        this.resultSlots = cfg.getIntArray(path + "result-slots");

        GuiClick click = (p, type, e) -> {
            if (type == ContentType.ACCEPT) {
                Inventory                       inv    = e.getInventory();
                Map<ItemStack, OutputContainer> result = new HashMap<>();

                double cost = 0;
                for (int slot : itemSlots) {
                    ItemStack target = inv.getItem(slot);
                    if (target == null) continue;

                    OutputContainer oCont = dismantleManager.getResult(target);
                    if (oCont == null) continue;

                    cost += oCont.getCost() * target.getAmount();
                    result.put(target, oCont);
                }
                if (result.isEmpty()) return;

                PlayerPreDismantleItemEvent event = new PlayerPreDismantleItemEvent(p, cost, result);
                plugin.getPluginManager().callEvent(event);

                cost = event.getCost();
                if (!dismantleManager.payForDismantle(p, cost)) {
                    event.setCancelled(true);
                }
                if (event.isCancelled()) {
                    dismantleManager.actionsError.process(p);
                    return;
                }

                for (Map.Entry<ItemStack, OutputContainer> entry : result.entrySet()) {
                    ItemStack       target = entry.getKey();
                    OutputContainer c      = entry.getValue();
                    for (int i = 0; i < target.getAmount(); i++) {
                        for (OutputItem src : c.getItems()) {
                            src.give(p);
                        }
                    }
                }
                for (int slot : itemSlots) {
                    inv.setItem(slot, null);
                }

                PlayerDismantleItemEvent event2 = new PlayerDismantleItemEvent(p, cost, result);
                plugin.getPluginManager().callEvent(event2);

                p.closeInventory();
            } else if (type == ContentType.EXIT) {
                p.closeInventory();
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

    @Override
    protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {

    }


    @Override
    protected void onReady(@NotNull Player player, @NotNull Inventory inv, int page) {
        // Update placeholders
        this.update(inv);
        super.onReady(player, inv, page);
    }

    @Override
    protected boolean ignoreNullClick() {
        return false;
    }

    @Override
    protected boolean cancelClick(int slot) {
        return !ArrayUtils.contains(this.itemSlots, slot);
    }

    @Override
    protected boolean cancelPlayerClick() {
        return false;
    }

    @Override
    public void click(@NotNull Player player, @Nullable ItemStack item, int slot, InventoryClickEvent e) {
        if (slot >= this.getSize()) {
            if (item != null && !this.dismantleManager.isDismantleable(item)) {
                e.setCancelled(true);
                return;
            }
        }

        if (!this.cancelClick(slot) || (slot > this.getSize() && e.isShiftClick())) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                this.update(e.getInventory());
            });
        }

        super.click(player, item, slot, e);
    }

    @Override
    public void onClose(@NotNull Player player, InventoryCloseEvent e) {
        Inventory inv = e.getInventory();

        for (int slot : this.itemSlots) {
            ItemStack target = inv.getItem(slot);
            if (target != null) {
                ItemUT.addItem(player, target);
            }
        }
    }

    private void update(@NotNull Inventory inv) {
        // Calculate the dismantle cost and amount of result items.
        double                  cost   = 0;
        Map<ItemStack, Integer> result = new HashMap<>();
        for (int slot : this.itemSlots) {
            ItemStack target = inv.getItem(slot);
            if (target == null) continue;

            OutputContainer oCont = dismantleManager.getResult(target);
            if (oCont == null) continue;

            cost += oCont.getCost() * target.getAmount();

            for (int i = 0; i < target.getAmount(); i++) {
                for (OutputItem src : oCont.getItems()) {
                    ItemStack preview = src.getPreview();
                    if (preview == null) continue;

                    ItemStack pCopy  = new ItemStack(preview);
                    int       amount = pCopy.getAmount();
                    pCopy.setAmount(1);
                    if (result.containsKey(pCopy)) {
                        amount += result.get(pCopy);
                    }
                    result.put(pCopy, amount);
                }
            }
        }
        String sCost = NumberUT.format(cost);

        // Clear result slots
        for (int i = 0; i < resultSlots.length; i++) {
            inv.setItem(resultSlots[i], null);
        }

        // Replace the cost placeholder on default GUI items.
        // These items are added in the JGUI items map on GUI load,
        // so we can just override them with .setItem method.
        for (GuiItem guiItem : this.getContent().values()) {
            ItemStack item = guiItem.getItem();

            // Clear previous 'result'
            for (int slot : guiItem.getSlots()) {
                if (ArrayUtils.contains(resultSlots, slot)) {
                    inv.setItem(slot, item);
                }
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            if (meta.hasDisplayName()) {
                meta.setDisplayName(meta.getDisplayName().replace("%cost%", sCost));
            }

            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.replaceAll(str -> str.replace("%cost%", sCost));
                meta.setLore(lore);
            }
            item.setItemMeta(meta);

            for (int i : guiItem.getSlots()) {
                inv.setItem(i, item);
            }
        }

        int j = 0;
        for (Map.Entry<ItemStack, Integer> e : result.entrySet()) {
            int   amount = e.getValue();
            int[] parts  = NumberUT.splitIntoParts(amount, (int) Math.ceil(amount / 64D));
            for (int amountPart : parts) {
                if (j >= this.resultSlots.length) return;

                ItemStack preview = new ItemStack(e.getKey());
                preview.setAmount(amountPart);
                inv.setItem(this.resultSlots[j++], preview);
            }
        }
    }
}
