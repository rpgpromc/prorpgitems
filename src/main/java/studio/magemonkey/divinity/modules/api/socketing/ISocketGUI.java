package studio.magemonkey.divinity.modules.api.socketing;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.gui.*;
import studio.magemonkey.codex.util.ClickText;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.manager.interactions.api.AnimatedSuccessBar;
import studio.magemonkey.divinity.modules.SocketItem;
import studio.magemonkey.divinity.modules.api.socketing.merchant.MerchantGUI;
import studio.magemonkey.divinity.modules.api.socketing.merchant.MerchantSocket;
import studio.magemonkey.divinity.modules.list.fortify.FortifyManager;

import java.util.function.Function;

public abstract class ISocketGUI extends NGUI<Divinity> {

    //    private final List<UUID> open = new ArrayList<>();
    protected ModuleSocket<?> module;
    protected int             itemSlot;
    protected int             sourceSlot;
    protected int             resultSlot;

    protected ISocketGUI(@NotNull ModuleSocket<?> module, @NotNull JYML cfg) {
        super(module.plugin, cfg, "gui.");

        this.module = module;
        String path = "gui.";

        this.itemSlot = cfg.getInt(path + "item-slot");
        this.sourceSlot = cfg.getInt(path + "source-slot");
        this.resultSlot = cfg.getInt(path + "result-slot");

        GuiClick clickHandler = (p, type, e) -> {
            if (type == null) return;
            if (!type.getClass().equals(ContentType.class)) return;

            ContentType type2 = (ContentType) type;

            if (type2 == ContentType.EXIT) {
                this.takeItem(e.getInventory(), this.getResultSlot());
                p.closeInventory();
            }
            if (type2 == ContentType.ACCEPT) {
                this.startSocketing(p, e);
            }
        };

        for (String id : cfg.getSection(path + "content")) {
            GuiItem guiItem = cfg.getGuiItem(path + "content." + id + ".", ContentType.class);
            if (guiItem == null) continue;

            if (guiItem.getType() != null) {
                guiItem.setClick(clickHandler);
            }

            this.addButton(guiItem);
        }
    }

    protected void startSocketing(@NotNull Player player, @NotNull InventoryClickEvent e) {
        int chance = this.getChance(player, e.getInventory());

        Function<Boolean, Void> resultHandler = this.getResultHandler(player, e.getInventory());
//        open.add(player.getUniqueId());
        player.closeInventory();

        AnimatedSuccessBar.Builder animator = this.module.getAnimation();
        if (animator != null) {
            animator.setChance(chance).setResult(resultHandler).build().act(player);
        } else {
            resultHandler.apply(Rnd.get(true) < chance);
        }
    }

    protected abstract int getChance(@NotNull Player player, @NotNull Inventory inv);

    @NotNull
    protected final Function<Boolean, Void> getResultHandler(@NotNull Player p, @NotNull Inventory inv) {
        ItemStack target = this.takeItem(inv, getItemSlot());
        ItemStack gem    = this.takeItem(inv, getSourceSlot());
        ItemStack result = this.takeItem(inv, getResultSlot());

        Function<Boolean, Void> resultHandler = (isSuccess) -> {
            p.closeInventory();

            // Success
            if (isSuccess) {
                // Send message before item modifications to properly show their names.
                this.getResultMessage(result, gem, true, false).send(p);

                ItemUT.addItem(p, result);
                if (this.module.getItemCharges(gem) != 0) {
                    this.module.takeItemCharge(gem);
                    ItemUT.addItem(p, gem);
                }
                this.module.clearSilentRateBonus(p);
                this.module.actionsComplete.process(p);

//                open.remove(p.getUniqueId());
                return null;
            }

            SocketItem mItem = this.module.getModuleItem(gem);
            if (mItem == null) {
                this.module.error("Attempt to socket invalid item!");
//                open.remove(p.getUniqueId());
                return null;
            }

            String socketId  = mItem.getTargetSocketIdRequirement();
            int    socketHas = this.module.getFilledSocketsAmount(target, socketId);

            // Fail stack feature
            MerchantSocket merchant   = this.module.getMerchant();
            boolean        isMerchant = this instanceof MerchantGUI;
            if (!isMerchant || (isMerchant && merchant != null && merchant.isSocketSilentRateEnabled())) {
                this.module.addSilentRateBonus(p, socketHas);
            }

            FortifyManager fortify = plugin.getModuleCache().getFortifyManager();
            boolean        fActive = fortify != null && fortify.canFortify(target, this.module);
            boolean        fSave   = fortify != null && fActive && fortify.tryFortify(target);

            if (fActive && fortify != null) {
                fortify.unfortifyItem(target);

                if (fSave) {
                    plugin.lang().Fortify_Enchanting_Success
                            .replace("%item%", ItemUT.getItemName(target))
                            .send(p);
                } else {
                    plugin.lang().Fortify_Enchanting_Failure
                            .replace("%item%", ItemUT.getItemName(target))
                            .send(p);
                }
            }

            // Send message before the item modifications to display items properly.
            this.getResultMessage(target, gem, false, fSave).send(p);

            // Check if we should return target item to a player
            // when socketing is failed.
            if (fSave || !this.module.isDestroyTargetOnFail()) {
                // Check if we should wipe all item filled sockets
                // of this module when socketing is failed.
                if (!fSave && this.module.isWipeSocketsOnFail()) {
                    for (int i = 0; i < socketHas; i++) {
                        this.module.extractSocket(target, socketId, 0);
                    }
                }
                // Return target item back to player.
                ItemUT.addItem(p, target);
            }

            // Check if we should return gem to a player.
            if (!this.module.isDestroySourceOnFail()) {
                // Check if gem has charges to be able to return it.
                if (this.module.getItemCharges(gem) != 0) {
                    this.module.takeItemCharge(gem);
                    ItemUT.addItem(p, gem);
                }
            }
            this.module.actionsError.process(p);
//            open.remove(p.getUniqueId());
            return null;
        };

        return resultHandler;
    }

    public final int getItemSlot() {
        return this.itemSlot;
    }

    public final int getSourceSlot() {
        return this.sourceSlot;
    }

    public final int getResultSlot() {
        return this.resultSlot;
    }

    public void open(@NotNull Player player, @NotNull ItemStack target, @NotNull ItemStack src) {
        ItemStack result = new ItemStack(module.insertSocket(new ItemStack(target), new ItemStack(src)));

        this.addButton(player, new JIcon(target), this.itemSlot);
        this.addButton(player, new JIcon(src), this.sourceSlot);
        this.addButton(player, new JIcon(result), this.resultSlot);

        super.open(player, 1);
    }

    @Override
    public void onClose(@NotNull Player player, @NotNull InventoryCloseEvent e) {
//        if (open.contains(player.getUniqueId()))
//            return;
        Inventory inv = e.getInventory();

        ItemStack item = inv.getItem(this.getItemSlot());
        ItemStack src  = inv.getItem(this.getSourceSlot());

        // Return target item if not conusmed
        if (item != null) {
            ItemUT.addItem(player, item);
        }

        // Return source item if not consumed
        if (src != null) {
            ItemUT.addItem(player, src);
        }

        super.onClose(player, e);
    }

    @NotNull
    private ClickText getResultMessage(@NotNull ItemStack target,
                                       @NotNull ItemStack src,
                                       boolean isSuccess,
                                       boolean fSave) {
        String result = plugin.lang().Module_Item_Socketing_Result_Total.normalizeLines();

        String stateTarget = plugin.lang().Module_Item_Socketing_Result_State_Success.getMsg();
        if (!isSuccess) {
            if (fSave || !module.isDestroyTargetOnFail()) {
                stateTarget = plugin.lang().Module_Item_Socketing_Result_State_Saved.getMsg();
            } else {
                if (module.isWipeSocketsOnFail()) {
                    stateTarget = plugin.lang().Module_Item_Socketing_Result_State_Wiped.getMsg();
                } else {
                    stateTarget = plugin.lang().Module_Item_Socketing_Result_State_Destroyed.getMsg();
                }
            }
        }

        String stateSource = plugin.lang().Module_Item_Socketing_Result_State_Consumed.getMsg();
        if (!isSuccess) {
            if (module.isDestroySourceOnFail()) {
                stateSource = plugin.lang().Module_Item_Socketing_Result_State_Destroyed.getMsg();
            }
        }

        result = result
                .replace("%state-target%", stateTarget)
                .replace("%state-source%", stateSource);

        ClickText text = new ClickText(result);
        text.createPlaceholder("%item-target%", ItemUT.getItemName(target)).showItem(target);
        text.createPlaceholder("%item-source%", ItemUT.getItemName(src)).showItem(src);

        return text;
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
    protected boolean cancelPlayerClick() {
        return true;
    }
}
