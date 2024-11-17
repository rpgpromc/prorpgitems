package studio.magemonkey.divinity.modules.list.classes.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;

public class SelectCmd extends MCmd<ClassManager> {

    public SelectCmd(@NotNull ClassManager module) {
        super(module, new String[]{"select"}, Perms.CLASS_CMD_SELECT);
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Classes_Cmd_Select_Desc.getMsg();
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
        this.module.openPreSelectionGUI(player);
    }

}
