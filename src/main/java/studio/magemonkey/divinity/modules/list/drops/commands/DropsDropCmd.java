package studio.magemonkey.divinity.modules.list.drops.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.PlayerUT;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.drops.DropManager;
import studio.magemonkey.divinity.modules.list.drops.object.DropTable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DropsDropCmd extends MCmd<DropManager> {

    public DropsDropCmd(@NotNull DropManager module) {
        super(module, new String[]{"drop"}, Perms.ADMIN);
    }

    @NotNull
    @Override
    public String usage() {
        return plugin.lang().Drop_Module_Cmd_Drop_Drop_Usage.getMsg();
    }

    @NotNull
    @Override
    public String description() {
        return plugin.lang().Drop_Module_Cmd_Drop_Drop_Desc.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return false;
    }

    @Override
    protected void perform(@NotNull CommandSender sender, @NotNull String cmd, @NotNull String[] args) {
        //TODO Set up drop for coordinates.
        // /drops drop (<player> || <world> <x> <y> <z>) <table> <level>
        if (args.length < 3) {
            printUsage(sender);
            return;
        }

        Player target = Bukkit.getServer().getPlayer(args[1]);
        if (target == null) {
            this.errPlayer(sender);
            return;
        }

        String tableName = args[2];

        int lMin = -1;
        int lMax = -1;
        if (args.length >= 4) {
            if (args[3].contains(":")) {
                String[] split = args[3].split(":");
                lMin = this.getNumI(sender, split[0], -1, true);
                lMax = this.getNumI(sender, split[1], -1, true);
            } else {
                lMin = lMax = this.getNumI(sender, args[3], -1, true);
            }
        }

        int level = Rnd.get(lMin, lMax);

        DropTable table = module.getTableById(tableName);
        if (table == null) {
            plugin.lang().Drop_Module_Cmd_Drop_Invalid_Table.send(sender);
            return;
        }

        List<ItemStack> drops = module.rollTable(target, table, level);
        drops.forEach(item -> {
            target.getLocation().getWorld().dropItem(target.getLocation().add(0, 0.5, 0), item);
        });
        plugin.lang().Drop_Module_Cmd_Give_Done
                .replace("%player%", target.getName())
                .replace("%item%", tableName)
                .replace("%amount%", drops.size())
                .send(sender);
    }

    @Override
    @NotNull
    public List<String> getTab(@NotNull Player player, int i, @NotNull String[] args) {
        if (i == 1) {
            return PlayerUT.getPlayerNames();
        }
        if (i == 2) {
            return module.getTables().stream().map(table -> table.getId()).collect(Collectors.toList());
        }
        if (i == 3) {
            return Arrays.asList("[level]", "-1", "1:5", "1");
        }

        return super.getTab(player, i, args);
    }
}
