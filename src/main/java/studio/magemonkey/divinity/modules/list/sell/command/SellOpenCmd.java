package studio.magemonkey.divinity.modules.list.sell.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.PlayerUT;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.sell.SellManager;

import java.util.Arrays;
import java.util.List;

public class SellOpenCmd extends MCmd<SellManager> {

    public SellOpenCmd(@NotNull SellManager sellManager) {
        super(sellManager, new String[]{"open"}, Perms.SELL_CMD_OPEN);
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Sell_Cmd_Open_Usage.getMsg();
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Sell_Cmd_Open_Desc.getMsg();
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
            return Arrays.asList("true", "false");
        }
        return super.getTab(player, i, args);
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2 && !(sender instanceof Player)) {
            this.errSender(sender);
            return;
        }

        String pName = sender.getName();
        if (args.length >= 2) {
            pName = args[1];
        }

        Player player = plugin.getServer().getPlayer(pName);
        if (player == null) {
            this.errPlayer(sender);
            return;
        }

        boolean force = args.length >= 3 ? Boolean.parseBoolean(args[2]) : (args.length < 2 ? true : false);

        this.module.openSellGUI(player, force);

        if (!sender.equals(player)) {
            plugin.lang().Sell_Cmd_Open_Done_Others
                    .replace("%player%", player.getName())
                    .send(sender);
        }
    }
}
