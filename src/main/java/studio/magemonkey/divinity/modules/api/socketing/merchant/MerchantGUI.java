package studio.magemonkey.divinity.modules.api.socketing.merchant;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.hooks.external.VaultHK;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.divinity.modules.SocketItem;
import studio.magemonkey.divinity.modules.api.socketing.ISocketGUI;
import studio.magemonkey.divinity.modules.api.socketing.ModuleSocket;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.requirements.ItemRequirements;

import java.util.List;

public class MerchantGUI extends ISocketGUI {

    private final MerchantSocket merchant;
    private final VaultHK        vaultHook;

    MerchantGUI(@NotNull ModuleSocket<?> module, @NotNull MerchantSocket merchant) {
        super(module, merchant.getConfig());
        this.merchant = merchant;
        this.vaultHook = plugin.getVault();

        if (this.vaultHook == null || !this.vaultHook.hasEconomy()) {
            module.warn("No Vault compatible Economy plugin found. Merchant GUI will be free.");
        }
    }

    @Override
    protected void startSocketing(@NotNull Player player, @NotNull InventoryClickEvent e) {
        Inventory inv = e.getInventory();

        ItemStack resultItem = this.getItem(inv, this.getResultSlot());
        if (ItemUT.isAir(resultItem)) return;

        if (this.vaultHook != null && this.vaultHook.hasEconomy()) {
            double price   = this.getPrice(player, inv);
            double balance = this.getBalance(player);
            if (balance < price) {
                plugin.lang().Module_Item_Socketing_Merchant_Error_TooExpensive.send(player);
                return;
            }

            this.vaultHook.take(player, price);
            this.plugin.lang().Module_Item_Socketing_Merchant_Notify_Pay
                    .replace("%amount%", NumberUT.format(price)).send(player);
        }

        super.startSocketing(player, e);
    }

    @Override
    protected int getChance(@NotNull Player p, @NotNull Inventory inv) {
        ItemStack gem      = this.getItem(inv, getSourceSlot());
        int       chance   = ItemStats.getSocketRate(gem);
        int       bonusMax = this.merchant.getSocketChanceBonusMax();

        // Prevent to override default item socket chance if it's higher than bonus cap.
        if (chance < bonusMax) {
            int bonus = this.merchant.getSocketChanceBonusAmount();
            chance = Math.min(bonusMax, chance + bonus);
        }

        // If silent rate bonus is enabled for merchant, than we'll use it and ignore merchant cap.
        if (this.merchant.isSocketSilentRateEnabled()) {
            chance += this.module.getSilentRateBonus(p);
        }

        return Math.min(100, chance);
    }

    private final double getPrice(@NotNull Player player, @NotNull Inventory inv) {
        ItemStack socketItem = this.getItem(inv, this.getSourceSlot());
        ItemStack targetItem = this.getItem(inv, this.getItemSlot());

        double socketPrice = plugin.getWorthManager().getItemWorth(socketItem) * this.merchant.getSocketWorthModifier();
        double targetPrice = plugin.getWorthManager().getItemWorth(targetItem) * this.merchant.getItemWorthModifier();
        double price       = socketPrice + targetPrice;
        return price;
    }

    private final double getBalance(@NotNull Player player) {
        return this.vaultHook.getBalance(player);
    }

    @Override
    protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {

    }

    @Override
    protected void onReady(@NotNull Player player, @NotNull Inventory inv, int page) {
        super.onReady(player, inv, page);
    }

    @Override
    protected void replaceMeta(@NotNull Player player, @NotNull Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (ItemUT.isAir(item)) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            List<String> lore = meta.getLore();
            if (lore == null) continue;

            lore.replaceAll(line -> line
                    .replace("%balance%", NumberUT.format(this.getBalance(player)))
                    .replace("%cost%", NumberUT.format(this.getPrice(player, inv)))
                    .replace("%chance%", String.valueOf(this.getChance(player, inv)))
            );

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    @Override
    protected void click(@NotNull Player player, @Nullable ItemStack item, int slot, @NotNull InventoryClickEvent e) {
        Inventory inv       = e.getInventory();
        ItemStack targetHas = this.getItem(inv, this.getItemSlot());
        ItemStack socketHas = this.getItem(inv, this.getSourceSlot());

        boolean hasTarget = !ItemUT.isAir(targetHas);
        boolean hasSocket = !ItemUT.isAir(socketHas);

        if (item != null && this.isPlayerInv(slot)) {
            SocketItem mItem = this.module.getModuleItem(item);
            if (mItem != null) {
                if (hasSocket) {
                    e.setCancelled(true);
                    return;
                }
                if (hasTarget) {
                    if (!ItemRequirements.canApply(player, item, targetHas)) {
                        e.setCancelled(true);
                        return;
                    }
                }
                ItemStack mCopy = new ItemStack(item);
                mCopy.setAmount(1);
                item.setAmount(item.getAmount() - 1);
                inv.setItem(this.getSourceSlot(), mCopy);
                hasSocket = true;
                socketHas = mCopy;
            } else {
                if (hasTarget) {
                    e.setCancelled(true);
                    return;
                }
                if (hasSocket && !ItemRequirements.canApply(player, item, socketHas)) {
                    e.setCancelled(true);
                    return;
                }
                if (!ItemStats.getSockets(this.module.getSocketType()).stream()
                        .anyMatch(att -> att.getEmptyAmount(item) > 0)) {
                    e.setCancelled(true);
                    return;
                }
                ItemStack iCopy = new ItemStack(item);
                iCopy.setAmount(1);
                item.setAmount(item.getAmount() - 1);
                inv.setItem(this.getItemSlot(), iCopy);
                hasTarget = true;
                targetHas = iCopy;
            }
        } else {
            if (slot == this.getItemSlot() && hasTarget) {
                ItemUT.addItem(player, this.takeItem(inv, this.getItemSlot()));
                hasTarget = false;
                targetHas = new ItemStack(Material.AIR);
            } else if (slot == this.getSourceSlot() && hasSocket) {
                ItemUT.addItem(player, this.takeItem(inv, this.getSourceSlot()));
                hasSocket = false;
                socketHas = new ItemStack(Material.AIR);
            }
        }

        this.takeItem(inv, this.getResultSlot());
        if (hasTarget && hasSocket) {
            ItemStack result =
                    new ItemStack(this.module.insertSocket(new ItemStack(targetHas), new ItemStack(socketHas)));
            inv.setItem(this.getResultSlot(), result);
        }

        this.plugin.getServer().getScheduler().runTask(plugin, () -> this.update(player, inv));

        super.click(player, item, slot, e);
    }

    private final void update(@NotNull Player player, @NotNull Inventory inv) {
        this.fillGUI(inv, player);
    }
}
