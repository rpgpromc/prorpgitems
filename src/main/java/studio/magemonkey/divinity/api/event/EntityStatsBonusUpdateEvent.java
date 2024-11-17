package studio.magemonkey.divinity.api.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.stats.EntityStats;

public class EntityStatsBonusUpdateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    protected LivingEntity entity;

    protected EntityStats stats;

    public EntityStatsBonusUpdateEvent(@NotNull LivingEntity entity, @NotNull EntityStats stats) {
        this.entity = entity;
        this.stats = stats;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    public final HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public LivingEntity getEntity() {
        return this.entity;
    }

    @NotNull
    public final EntityStats getStats() {
        return this.stats;
    }
}
