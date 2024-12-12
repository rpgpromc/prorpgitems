package studio.magemonkey.divinity.modules.list.identify.event;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.events.ICancellableEvent;
import studio.magemonkey.divinity.modules.list.identify.IdentifyManager.IdentifyTome;
import studio.magemonkey.divinity.modules.list.identify.IdentifyManager.UnidentifiedItem;

public class PlayerIdentifyItemEvent extends ICancellableEvent {

    private UnidentifiedItem target;
    private IdentifyTome     tome;
    private ItemStack        result;
    private Player           player;

    public PlayerIdentifyItemEvent(
            @NotNull UnidentifiedItem target,
            @NotNull IdentifyTome tome,
            @NotNull ItemStack result,
            @NotNull Player player
    ) {
        this.target = target;
        this.tome = tome;
        this.result = result;
        this.player = player;
    }

    @NotNull
    public IdentifyTome getTome() {
        return this.tome;
    }

    @NotNull
    public UnidentifiedItem getTargetItem() {
        return this.target;
    }

    @NotNull
    public ItemStack getResult() {
        return this.result;
    }

    @NotNull
    public Player getPlayer() {
        return this.player;
    }
}
