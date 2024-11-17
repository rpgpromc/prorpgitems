package studio.magemonkey.divinity.modules.list.classes.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.PlayerUT;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;

import java.util.List;

public class ResetSkillPointsCmd extends MCmd<ClassManager> {

    public ResetSkillPointsCmd(@NotNull ClassManager module) {
        super(module, new String[]{"resetskillpoints"}, Perms.CLASS_CMD_RESET_SKILL_POINTS);
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Classes_Cmd_ResetSkillPoints_Usage.getMsg();
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Classes_Cmd_ResetSkillPoints_Desc.getMsg();
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
        return super.getTab(player, i, args);
    }

    @Override
    protected void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            this.printUsage(sender);
            return;
        }

        String pName  = args[1];
        Player player = plugin.getServer().getPlayer(pName);
        if (player == null) {
            plugin.lang().Error_NoPlayer.replace("%player%", pName).send(sender);
            return;
        }

        this.module.reallocateSkillPoints(player);
        this.plugin.lang().Classes_Cmd_ResetSkillPoints_Done
                .replace("%player%", player.getName())
                .send(sender);
    }
}
