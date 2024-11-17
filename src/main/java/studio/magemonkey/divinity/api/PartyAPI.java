package studio.magemonkey.divinity.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.party.PartyManager;
import studio.magemonkey.divinity.modules.list.party.PartyManager.Party;
import studio.magemonkey.divinity.modules.list.party.PartyManager.PartyMember;

public class PartyAPI {

    private static final Divinity plugin = Divinity.getInstance();

    @Nullable
    public static Party getPlayerParty(@NotNull Player player) {
        PartyManager party = plugin.getModuleCache().getPartyManager();
        if (party == null || !party.isLoaded()) return null;

        return party.getPlayerParty(player);
    }

    @Nullable
    public static PartyMember getPartyMember(@NotNull Player player) {
        PartyManager party = plugin.getModuleCache().getPartyManager();
        if (party == null || !party.isLoaded()) return null;

        return party.getPartyMember(player);
    }

    @Nullable
    public static PartyManager getPartyManager() {
        PartyManager party = plugin.getModuleCache().getPartyManager();
        if (party == null || !party.isLoaded()) return null;

        return party;
    }
}
