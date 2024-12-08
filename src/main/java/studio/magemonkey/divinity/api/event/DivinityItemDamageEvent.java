package studio.magemonkey.divinity.api.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import studio.magemonkey.codex.api.events.ICancellableEvent;

@Getter
@RequiredArgsConstructor
public class DivinityItemDamageEvent extends ICancellableEvent {
    private final ItemStack    item;
    private final LivingEntity entity;
}
