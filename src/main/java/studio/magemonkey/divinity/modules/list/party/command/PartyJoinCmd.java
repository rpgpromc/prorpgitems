package studio.magemonkey.divinity.modules.list.party.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.party.PartyManager;

import java.util.ArrayList;
import java.util.List;

public class PartyJoinCmd extends MCmd<PartyManager> {

    public PartyJoinCmd(@NotNull PartyManager m) {
        super(m, new String[]{"join"}, Perms.PARTY_CMD_JOIN);
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Party_Cmd_Join_Usage.getMsg();
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Party_Cmd_Join_Desc.getDefaultMsg();
    }

    @Override
    public boolean playersOnly() {
        return true;
    }

    @Override
    @NotNull
    public List<String> getTab(@NotNull Player player, int i, @NotNull String[] args) {
        if (i == 1) {
            List<String> list = new ArrayList<>();
            for (PartyManager.Party pp : module.getParties()) {
                if (module.hasInvite(player, pp)) {
                    list.add(pp.getId());
                }
            }
            return list;
        }
        return super.getTab(player, i, args);
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;
        if (args.length != 2) {
            this.printUsage(sender);
            return;
        }

        String name = args[1];
        module.joinParty(player, name);
    }
}
