package studio.magemonkey.divinity.modules.list.classes.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;

public class PlayerRegainManaEvent extends PlayerClassEvent implements Cancellable {

    private boolean cancelled;

    private double amount;

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public PlayerRegainManaEvent(
            @NotNull Player player,
            @NotNull UserClassData data,
            double amount
    ) {
        super(player, data);
        this.amount = amount;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
