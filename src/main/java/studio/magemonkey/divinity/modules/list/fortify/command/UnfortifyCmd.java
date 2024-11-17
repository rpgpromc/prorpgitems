package studio.magemonkey.divinity.modules.list.fortify.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.fortify.FortifyManager;

public class UnfortifyCmd extends MCmd<FortifyManager> {

    public UnfortifyCmd(@NotNull FortifyManager module) {
        super(module, new String[]{"unfortify"}, Perms.FORTIFY_CMD_UNFORTIFY);
    }

    @Override
    @NotNull
    public String usage() {
        return "";
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Fortify_Cmd_Unfortify_Desc.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return true;
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        Player    p    = (Player) sender;
        ItemStack item = p.getInventory().getItemInMainHand();
        if (ItemUT.isAir(item)) {
            plugin.lang().Error_NoItem.send(p);
            return;
        }

        module.unfortifyItem(item);
        p.getInventory().setItemInMainHand(item);

        plugin.lang().Command_Modify_Done.send(p);
    }

}
