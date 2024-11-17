package studio.magemonkey.divinity.modules.list.classes.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;

public class SkillsCmd extends MCmd<ClassManager> {

    public SkillsCmd(@NotNull ClassManager module) {
        super(module, new String[]{"skills"}, Perms.CLASS_CMD_SKILLS);
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Classes_Cmd_Skills_Desc.getMsg();
    }

    @Override
    @NotNull
    public String usage() {
        return "";
    }

    @Override
    public boolean playersOnly() {
        return true;
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;
        module.openSkillsGUI(player);
    }
}
