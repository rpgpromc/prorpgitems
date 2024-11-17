package studio.magemonkey.divinity.modules.list.party.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.loot.LootHolder;
import studio.magemonkey.divinity.modules.list.loot.LootManager;
import studio.magemonkey.divinity.modules.list.party.PartyManager;

public class PartyRollCmd extends MCmd<PartyManager> {

    public PartyRollCmd(@NotNull PartyManager module) {
        super(module, new String[]{"roll"}, Perms.PARTY_CMD_ROLL);
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Party_Cmd_Roll_Desc.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return true;
    }

    @Override
    @NotNull
    public String usage() {
        return "";
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        LootManager lootManager = plugin.getModuleCache().getLootManager();
        if (lootManager == null) return;

        Player              player = (Player) sender;
        LootHolder.RollTask roll   = lootManager.getPartyRollTask(player);
        if (roll == null) {
            plugin.lang().Party_Cmd_Roll_Error_Nothing.send(player);
            return;
        }

        int r = (int) Rnd.get(true);
        roll.addRoll(player, r);
    }

}
