package studio.magemonkey.divinity.modules.list.party.compat.level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.party.PartyManager;
import studio.magemonkey.fabled.Fabled;
import studio.magemonkey.fabled.api.enums.ExpSource;
import studio.magemonkey.fabled.api.event.PlayerExperienceGainEvent;

public class IPEFabled extends IPartyLevelManager {

    public IPEFabled(@NotNull Divinity plugin, @NotNull PartyManager partyManager) {
        super(plugin, partyManager);
    }

    @EventHandler
    public void onExpFabled(PlayerExperienceGainEvent e) {
        if (e.getSource() == ExpSource.PLUGIN) return;

        Player player = e.getPlayerData().getPlayer();
        int    exp    = this.getBalancedExp(player, (int) e.getExp());
        if (exp <= 0) return;

        e.setExp(exp);
    }

    @Override
    public void giveExp(@NotNull Player player, int amount) {
        Fabled.getPlayerAccountData(player).getActiveData().giveExp(amount, ExpSource.PLUGIN);
    }
}
