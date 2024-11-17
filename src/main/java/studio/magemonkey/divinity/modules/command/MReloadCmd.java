package studio.magemonkey.divinity.modules.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.api.QModule;

public class MReloadCmd extends MCmd<QModule> {

    public MReloadCmd(@NotNull QModule m) {
        super(m, new String[]{"reload"}, Perms.ADMIN);
    }

    @Override
    @NotNull
    public String usage() {
        return "";
    }

    @Override
    @NotNull
    public String description() {
        return "Reload the module.";
    }

    @Override
    public boolean playersOnly() {
        return false;
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        this.module.reload();
        plugin.lang().Module_Cmd_Reload.replace("%module%", module.name()).send(sender);
    }
}
