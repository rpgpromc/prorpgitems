package studio.magemonkey.divinity.manager.interactions;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.codex.manager.api.Loadable;
import studio.magemonkey.divinity.Divinity;

import java.util.Map;
import java.util.WeakHashMap;

public class InteractionManager extends IListener<Divinity> implements Loadable {

    // TODO In Abstract action add task and check for other things
    // in map add action instead of boolean

    private Map<Player, Boolean> actionPlayers;

    public InteractionManager(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    public void setup() {
        this.actionPlayers = new WeakHashMap<>();

        this.registerListeners();
    }

    @Override
    public void shutdown() {
        this.unregisterListeners();

        if (this.actionPlayers != null) {
            this.actionPlayers.clear();
            this.actionPlayers = null;
        }
    }

    public boolean isInAction(@NotNull Player player) {
        return this.actionPlayers.containsKey(player);
    }

    public void addInAction(@NotNull Player player) {
        if (!this.isInAction(player)) {
            this.actionPlayers.put(player, true);
        }
    }

    public void removeFromAction(@NotNull Player player) {
        this.actionPlayers.remove(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onActionMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (this.isInAction(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onActionClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        if (this.isInAction(player)) {
            e.setCancelled(true);
        }
    }
}
