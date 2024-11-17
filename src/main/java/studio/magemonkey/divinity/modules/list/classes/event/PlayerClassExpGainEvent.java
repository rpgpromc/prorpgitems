package studio.magemonkey.divinity.modules.list.classes.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;
import studio.magemonkey.divinity.modules.list.classes.object.ExpSource;

public class PlayerClassExpGainEvent extends PlayerClassEvent implements Cancellable {

    private boolean cancelled;

    private int       amount;
    private String    src;
    private ExpSource expSrc;

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public PlayerClassExpGainEvent(
            @NotNull Player player,
            @NotNull UserClassData data,
            int amount,
            @NotNull String src,
            @NotNull ExpSource expSrc
    ) {
        super(player, data);
        this.amount = amount;
        this.src = src;
        this.expSrc = expSrc;
    }

    public int getExp() {
        return this.amount;
    }

    public void setExp(int exp) {
        this.amount = exp;
    }

    @NotNull
    public String getSource() {
        return this.src;
    }

    @NotNull
    public ExpSource getExpSource() {
        return this.expSrc;
    }
}
