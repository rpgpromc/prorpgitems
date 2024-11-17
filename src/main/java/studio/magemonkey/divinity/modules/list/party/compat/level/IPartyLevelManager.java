package studio.magemonkey.divinity.modules.list.party.compat.level;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.party.PartyManager;
import studio.magemonkey.divinity.modules.list.party.PartyManager.Party;
import studio.magemonkey.divinity.modules.list.party.PartyManager.PartyExpMode;
import studio.magemonkey.divinity.modules.list.party.PartyManager.PartyMember;

import java.util.Set;

public abstract class IPartyLevelManager extends IListener<Divinity> {

    protected PartyManager partyManager;

    public IPartyLevelManager(@NotNull Divinity plugin, @NotNull PartyManager partyManager) {
        super(plugin);
        this.partyManager = partyManager;
    }

    public void setup() {
        this.registerListeners();
    }

    public void shutdown() {
        this.unregisterListeners();
    }

    protected final int getBalancedExp(@NotNull Player player, int amount) {
        PartyMember member = this.partyManager.getPartyMember(player);
        if (member == null || amount <= 0) return 0;

        Party party = member.getParty();
        if (party.getExpMode() != PartyExpMode.SHARED) return 0;

        int              expDistance = this.partyManager.getSettings().getMaxLevelExpBalanceDistance();
        Set<PartyMember> expGetters  = party.getMembersByDistance(member, expDistance);

        int   size = expGetters.size() + 1; // +1 For Executor
        int[] each = splitIntoParts(amount, size);

        int i = 0;
        for (PartyMember friend : expGetters) {
            Player pFriend = friend.getPlayer();
            if (pFriend == null) continue;

            this.giveExp(pFriend, each[i++]);
        }

        return each[i];
    }

    private static int[] splitIntoParts(int whole, int parts) {
        int[] arr       = new int[parts];
        int   remain    = whole;
        int   partsLeft = parts;
        for (int i = 0; partsLeft > 0; i++) {
            int size = (remain + partsLeft - 1) / partsLeft; // rounded up, aka ceiling
            arr[i] = size;
            remain -= size;
            partsLeft--;
        }
        return arr;
    }

    public abstract void giveExp(@NotNull Player player, int amount);
}
