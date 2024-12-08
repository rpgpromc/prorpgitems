package studio.magemonkey.divinity.modules.list.extractor.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.events.ICancellableEvent;
import studio.magemonkey.divinity.stats.items.attributes.SocketAttribute;

import java.util.List;

public class PlayerExtractSocketEvent extends ICancellableEvent {

    @Getter
    private Player    player;
    @Getter
    private ItemStack item, result;
    @Getter
    private List<ItemStack>      returnedSockets;
    @Getter
    private SocketAttribute.Type socketType;
    @Getter
    @Setter
    private boolean              isFailed;

    public PlayerExtractSocketEvent(
            @NotNull Player player,
            @NotNull ItemStack item,
            @NotNull ItemStack result,
            @NotNull List<ItemStack> returnedSockets,
            @NotNull SocketAttribute.Type socketType
    ) {
        this.item = item;
        this.result = result;
        this.socketType = socketType;
        this.returnedSockets = returnedSockets;
        this.player = player;
        this.setFailed(false);
    }
}
