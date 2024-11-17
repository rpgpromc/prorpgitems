package studio.magemonkey.divinity.modules.list.identify.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.identify.IdentifyManager;

public class IdentifyCmd extends MCmd<IdentifyManager> {

    public IdentifyCmd(@NotNull IdentifyManager m) {
        super(m, new String[]{"identify"}, Perms.IDENTIFY_CMD_IDENTIFY);
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Identify_Cmd_Identify_Desc.getMsg();
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
        Player    player = (Player) sender;
        ItemStack hand   = player.getInventory().getItemInMainHand();

        ItemStack unlock = module.getIdentifiedOf(hand);
        if (unlock == null) {
            return;
        }

        player.getInventory().setItemInMainHand(unlock);

        plugin.lang().Identify_Identify_Success
                .replace("%item%", ItemUT.getItemName(unlock))
                .send(player);
    }

}
