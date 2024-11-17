package studio.magemonkey.divinity.modules.api.socketing.merchant;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.PlayerUT;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.api.socketing.ModuleSocket;
import studio.magemonkey.divinity.modules.command.MCmd;

import java.util.Arrays;
import java.util.List;

public class MerchantCmd extends MCmd<ModuleSocket<?>> {

    private MerchantSocket merchant;

    public MerchantCmd(@NotNull ModuleSocket<?> module, @NotNull MerchantSocket merchant) {
        super(module, new String[]{"merchant"}, Perms.getSocketCmdMerchant(module));
        this.merchant = merchant;
    }

    @Override
    public boolean playersOnly() {
        return false;
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Module_Socketing_Cmd_Merchant_Usage.getMsg();
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Module_Socketing_Cmd_Merchant_Desc.getMsg();
    }

    @Override
    @NotNull
    public List<String> getTab(@NotNull Player player, int i, @NotNull String[] args) {
        if (player.hasPermission(Perms.getSocketCmdMerchantOthers(this.module))) {
            if (i == 1) {
                return PlayerUT.getPlayerNames();
            }
            if (i == 2) {
                return Arrays.asList("true", "false");
            }
        }
        return super.getTab(player, i, args);
    }

    @Override
    protected void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2 && !(sender instanceof Player)) {
            this.printUsage(sender);
            return;
        }
        if (args.length > 1 && !sender.hasPermission(Perms.getSocketCmdMerchantOthers(this.module))) {
            this.errPerm(sender);
            return;
        }

        String pName  = args.length >= 2 ? args[1] : sender.getName();
        Player player = plugin.getServer().getPlayer(pName);
        if (player == null) {
            this.errPlayer(sender);
            return;
        }

        boolean force = args.length >= 3 ? Boolean.parseBoolean(args[2]) : false;

        this.merchant.openMerchantGUI(player, force);
    }
}
