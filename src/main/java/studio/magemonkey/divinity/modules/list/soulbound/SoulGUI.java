package studio.magemonkey.divinity.modules.list.soulbound;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.gui.*;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.Divinity;

public class SoulGUI extends NGUI<Divinity> {

    private int itemSlot;
    private int sourceSlot;
    private int resultSlot;

    public SoulGUI(@NotNull SoulboundManager soulboundManager) {
        super(soulboundManager.plugin, soulboundManager.getJYML(), "gui.");

        JYML   cfg  = soulboundManager.getJYML();
        String path = "gui.";

        this.itemSlot = cfg.getInt(path + "item-slot");
        this.sourceSlot = cfg.getInt(path + "source-slot");
        this.resultSlot = cfg.getInt(path + "result-slot");

        GuiClick click = new GuiClick() {
            @Override
            public void click(@NotNull Player p, @Nullable Enum<?> type, @NotNull InventoryClickEvent e) {
                if (type == ContentType.ACCEPT) {
                    Inventory inv    = e.getInventory();
                    ItemStack result = getItem(inv, resultSlot);
                    inv.setItem(itemSlot, result);

                    p.closeInventory();
                    plugin.lang().Soulbound_Item_Soulbound_Apply
                            .replace("%item%", ItemUT.getItemName(result))
                            .send(p);
                } else if (type == ContentType.EXIT) {
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

    public void open(@NotNull Player player,
                     @NotNull ItemStack target,
                     @NotNull ItemStack src,
                     @NotNull ItemStack result) {
        this.addButton(player, new JIcon(target), this.itemSlot);
        this.addButton(player, new JIcon(src), this.sourceSlot);
        this.addButton(player, new JIcon(result), this.resultSlot);

        super.open(player, 1);
    }

    @Override
    protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {

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
    protected boolean cancelClick(int slot) {
        return true;
    }

    @Override
    public void onClose(@NotNull Player player, @NotNull InventoryCloseEvent e) {
        Inventory inv  = e.getInventory();
        ItemStack item = inv.getItem(this.itemSlot);

        if (item != null) {
            ItemUT.addItem(player, item);
        }
    }
}
