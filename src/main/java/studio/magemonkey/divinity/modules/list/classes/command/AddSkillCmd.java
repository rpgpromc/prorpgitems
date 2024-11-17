package studio.magemonkey.divinity.modules.list.classes.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.PlayerUT;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;
import studio.magemonkey.divinity.modules.list.classes.api.IAbstractSkill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddSkillCmd extends MCmd<ClassManager> {

    public AddSkillCmd(@NotNull ClassManager module) {
        super(module, new String[]{"addskill"}, Perms.CLASS_CMD_ADDSKILL);
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Classes_Cmd_AddSkill_Desc.getMsg();
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Classes_Cmd_AddSkill_Usage.getMsg();
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
            return new ArrayList<>(this.module.getSkillIds());
        }
        if (i == 3) {
            return Arrays.asList("-1", "1", "2", "3");
        }
        if (i == 4) {
            return Arrays.asList("true", "false");
        }
        return super.getTab(player, i, args);
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length < 4) {
            this.printUsage(sender);
            return;
        }

        String  playerName = args[1];
        String  skillId    = args[2];
        int     lvl        = this.getNumI(sender, args[3], 1);
        boolean force      = true;
        if (args.length >= 5) {
            force = Boolean.parseBoolean(args[4]);
        }

        Player player = plugin.getServer().getPlayer(playerName);
        if (player == null) {
            this.errPlayer(sender);
            return;
        }

        IAbstractSkill skill = module.getSkillById(skillId);
        if (skill == null) {
            plugin.lang().Classes_Cmd_AddSkill_Error_NoSkill.send(sender);
            return;
        }

        this.module.addSkill(player, skill, lvl, force);

        plugin.lang().Classes_Cmd_AddSkill_Done
                .replace("%skill%", skill.getName())
                .replace("%name%", player.getName())
                .send(sender);
    }
}
