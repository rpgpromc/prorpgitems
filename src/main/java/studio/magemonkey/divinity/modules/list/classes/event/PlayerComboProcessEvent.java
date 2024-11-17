package studio.magemonkey.divinity.modules.list.classes.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.modules.list.classes.ComboManager.ComboInfo;
import studio.magemonkey.divinity.modules.list.classes.ComboManager.ComboKey;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;

public class PlayerComboProcessEvent extends PlayerClassEvent implements Cancellable {

    private boolean cancelled;

    private ComboKey  key;
    private ComboInfo combo;

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public PlayerComboProcessEvent(
            @NotNull Player player,
            @NotNull UserClassData data,
            @NotNull ComboKey key,
            @NotNull ComboInfo combo
    ) {
        super(player, data);
        this.key = key;
        this.combo = combo;
    }

    @NotNull
    public ComboKey getCurrentKey() {
        return this.key;
    }

    @NotNull
    public ComboInfo getCombo() {
        return this.combo;
    }
}
