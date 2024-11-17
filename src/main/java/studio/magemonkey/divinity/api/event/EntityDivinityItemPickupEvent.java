package studio.magemonkey.divinity.api.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.modules.api.QModuleDrop;

@Getter
@RequiredArgsConstructor
public class EntityDivinityItemPickupEvent extends Event implements Cancellable {
    private static final HandlerList    handlers = new HandlerList();
    private final        ItemStack      item;
    private final        LivingEntity   li;
    private final        QModuleDrop<?> module;
    @Setter
    private              boolean        cancelled;

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }
}
