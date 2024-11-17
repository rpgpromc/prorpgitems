package studio.magemonkey.divinity.modules.list.soulbound.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.soulbound.SoulboundManager;
import studio.magemonkey.divinity.stats.items.requirements.user.SoulboundRequirement;

import java.util.Arrays;
import java.util.List;

public class SoulboundSoulCmd extends MCmd<SoulboundManager> {

    private SoulboundRequirement reqSoul;

    public SoulboundSoulCmd(@NotNull SoulboundManager m, @NotNull SoulboundRequirement reqSoul) {
        super(m, new String[]{"soul"}, Perms.SOULBOUND_CMD_SOUL);
        this.reqSoul = reqSoul;
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Soulbound_Cmd_Soul_Usage.getMsg();
    }

    @Override
    @NotNull
    public List<String> getTab(@NotNull Player player, int i, @NotNull String[] args) {
        if (i == 1) {
            return Arrays.asList("add", "remove");
        }
        if (i == 2 && args[0].equalsIgnoreCase("add")) {
            return Arrays.asList("[pos]");
        }
        return super.getTab(player, i, args);
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Soulbound_Cmd_Soul_Desc.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return true;
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2 || args.length > 3) {
            this.printUsage(sender);
            return;
        }
        if (!args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("remove")) {
            this.printUsage(sender);
            return;
        }

        Player    player = (Player) sender;
        ItemStack item   = player.getInventory().getItemInMainHand();
        if (ItemUT.isAir(item)) {
            plugin.lang().Error_NoItem.send(player);
            return;
        }

        int     pos = args.length == 3 ? StringUT.getInteger(args[2], -1) : -1;
        boolean add = args[1].equalsIgnoreCase("add");
        if (add) {
            this.reqSoul.add(item, pos);
        } else {
            this.reqSoul.remove(item);
        }

        plugin.lang().Soulbound_Cmd_Soul_Done
                .replace("%state%", plugin.lang().getBool(add))
                .send(player);
    }
}
