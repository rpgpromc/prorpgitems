package studio.magemonkey.divinity.modules.list.sell.event;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.manager.api.event.IEvent;

import java.util.HashMap;
import java.util.Map;

public class PlayerSellItemEvent extends IEvent {

    private Player                 player;
    private double                 price;
    private Map<ItemStack, Double> priceMap;

    public PlayerSellItemEvent(
            @NotNull Player player,
            @NotNull Map<ItemStack, Double> priceMap
    ) {
        this.player = player;
        this.priceMap = priceMap;
        this.priceMap.forEach((item, itemCost) -> {
            this.price += itemCost;
        });
    }

    @NotNull
    public Player getPlayer() {
        return this.player;
    }

    @NotNull
    public Map<ItemStack, Double> getPriceMap() {
        return new HashMap<>(this.priceMap);
    }

    public double getPrice() {
        return this.price;
    }
}
