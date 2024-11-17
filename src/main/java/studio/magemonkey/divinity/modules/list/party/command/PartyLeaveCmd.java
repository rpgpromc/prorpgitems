package studio.magemonkey.divinity.modules.list.party.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.party.PartyManager;

public class PartyLeaveCmd extends MCmd<PartyManager> {

    public PartyLeaveCmd(@NotNull PartyManager m) {
        super(m, new String[]{"leave"}, Perms.PARTY_CMD_LEAVE);
    }

    @Override
    @NotNull
    public String usage() {
        return "";
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Party_Cmd_Leave_Desc.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return true;
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;

        PartyManager.PartyMember member = module.getPartyMember(player);
        if (member == null) {
            plugin.lang().Party_Error_NotInParty.send(player);
            return;
        }

        member.leaveParty();
    }
}
