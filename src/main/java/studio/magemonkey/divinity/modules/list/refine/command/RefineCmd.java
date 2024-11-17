package studio.magemonkey.divinity.modules.list.refine.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.refine.RefineManager;
import studio.magemonkey.divinity.modules.list.refine.RefineManager.RefineItem;

import java.util.List;

public class RefineCmd extends MCmd<RefineManager> {

    public RefineCmd(@NotNull RefineManager refineManager) {
        super(refineManager, new String[]{"refine"}, Perms.REFINE_CMD_REFINE);
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Refine_Cmd_Refine_Usage.getMsg();
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Refine_Cmd_Refine_Desc.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return true;
    }

    @Override
    @NotNull
    public List<String> getTab(@NotNull Player player, int i, @NotNull String[] args) {
        if (i == 1) {
            return module.getItemIds();
        }
        return super.getTab(player, i, args);
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length != 2) {
            this.printUsage(sender);
            return;
        }

        Player    player = (Player) sender;
        ItemStack item   = player.getInventory().getItemInMainHand();
        if (ItemUT.isAir(item)) {
            this.errItem(sender);
            return;
        }

        RefineItem stone = module.getItemById(args[1]);
        if (stone == null) {
            plugin.lang().Refine_Cmd_Refine_Error_Stone.send(player);
            return;
        }

        module.refineItem(item, stone);
        player.getInventory().setItemInMainHand(item);
        plugin.lang().Refine_Cmd_Refine_Done.send(sender);
    }
}
