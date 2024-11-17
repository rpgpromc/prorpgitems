package studio.magemonkey.divinity.modules.list.classes.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.PlayerUT;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;

import java.util.Arrays;
import java.util.List;

public class AddSkillPointsCmd extends MCmd<ClassManager> {

    public AddSkillPointsCmd(@NotNull ClassManager module) {
        super(module, new String[]{"addskillpoints"}, Perms.CLASS_CMD_ADD_SKILL_POINTS);
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Classes_Cmd_AddSkillPoints_Desc.getMsg();
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Classes_Cmd_AddSkillPoints_Usage.getMsg();
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

        int amount = this.getNumI(sender, args[2], 0);
        if (amount == 0) {
            return;
        }

        String playerName = args[1];
        Player player     = plugin.getServer().getPlayer(playerName);
        if (player == null) {
            this.errPlayer(sender);
            return;
        }

        this.module.addSkillPoints(player, amount);

        plugin.lang().Classes_Cmd_AddSkillPoints_Done
                .replace("%amount%", String.valueOf(amount))
                .replace("%name%", player.getName())
                .send(sender);
    }
}
