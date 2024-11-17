package studio.magemonkey.divinity.modules.list.classes.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.PlayerUT;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.data.api.DivinityUser;
import studio.magemonkey.divinity.data.api.UserProfile;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;
import studio.magemonkey.divinity.modules.list.classes.object.ExpSource;

import java.util.Arrays;
import java.util.List;

public class AddLevelCmd extends MCmd<ClassManager> {

    public AddLevelCmd(@NotNull ClassManager module) {
        super(module, new String[]{"addlevel"}, Perms.CLASS_CMD_ADDLEVEL);
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Classes_Cmd_AddLevel_Desc.getMsg();
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Classes_Cmd_AddLevel_Usage.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return false;
    }

    @Override
    @NotNull
    public List<String> getTab(@NotNull Player player, int i, @NotNull String[] args) {
        if (i == 1) {
            return PlayerUT.getPlayerNames();
        }
        if (i == 2) {
            return Arrays.asList("<amount>");
        }
        return super.getTab(player, i, args);
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length != 3) {
            this.printUsage(sender);
            return;
        }

        int levels = this.getNumI(sender, args[2], 0);
        if (levels == 0) {
            return;
        }

        Player player = plugin.getServer().getPlayer(args[1]);
        String name   = "";

        for (int i = 0; i < levels; i++) {
            if (player != null) {
                UserClassData cData = this.module.getUserData(player);
                if (cData == null) {
                    plugin.lang().Classes_Cmd_AddLevel_Error_NoClass.send(sender);
                    return;
                }

                int amount = cData.getExpToUp(true);
                if (levels < 0) amount = -amount;

                this.module.getLevelingManager().addExp(player, amount, sender.getName(), ExpSource.INTERNAL);
                name = player.getName();
            } else {
                DivinityUser user = plugin.getUserManager().getOrLoadUser(args[1], false);
                if (user == null) {
                    this.errPlayer(sender);
                    return;
                }
                UserProfile   prof  = user.getActiveProfile();
                UserClassData cData = prof.getClassData();
                if (cData == null) {
                    plugin.lang().Classes_Cmd_AddLevel_Error_NoClass.send(sender);
                    return;
                }

                int amount = cData.getExpToUp(true);
                if (levels < 0) amount = -amount;

                cData.addExp(amount);
                name = user.getName();
            }
        }

        plugin.lang().Classes_Cmd_AddLevel_Done
                .replace("%amount%", String.valueOf(levels))
                .replace("%name%", name)
                .send(sender);
    }
}
