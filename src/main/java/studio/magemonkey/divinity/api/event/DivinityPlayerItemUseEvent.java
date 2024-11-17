package studio.magemonkey.divinity.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.modules.LimitedItem;
import studio.magemonkey.divinity.types.QClickType;

public class DivinityPlayerItemUseEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled;

    private final ItemStack item;

    private final LimitedItem ci;

    private final Player p;

    private final QClickType click;

    public DivinityPlayerItemUseEvent(@NotNull ItemStack item,
                                      @NotNull Player p,
                                      @NotNull LimitedItem ci,
                                      @NotNull QClickType click) {
        this.item = item;
        this.ci = ci;
        this.p = p;
        this.click = click;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    public LimitedItem getItem() {
        return this.ci;
    }

    @NotNull
    public ItemStack getItemStack() {
        return this.item;
    }

    @NotNull
    public Player getPlayer() {
        return this.p;
    }

    @NotNull
    public QClickType getClickType() {
        return this.click;
    }
}
