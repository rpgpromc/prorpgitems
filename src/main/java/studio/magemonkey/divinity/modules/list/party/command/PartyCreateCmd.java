package studio.magemonkey.divinity.modules.list.party.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.party.PartyManager;

public class PartyCreateCmd extends MCmd<PartyManager> {

    public PartyCreateCmd(@NotNull PartyManager m) {
        super(m, new String[]{"create"}, Perms.PARTY_CMD_CREATE);
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Party_Cmd_Create_Usage.getMsg();
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Party_Cmd_Create_Desc.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return true;
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;
        String id     = player.getName();

        if (args.length == 2) {
            id = args[1];
        }

        module.createParty(player, id);
    }
}
