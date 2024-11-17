package studio.magemonkey.divinity.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.commands.api.ISubCommand;
import studio.magemonkey.codex.util.CollectionsUT;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.stats.bonus.StatBonus;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.*;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;
import studio.magemonkey.divinity.stats.items.attributes.stats.DurabilityStat;
import studio.magemonkey.divinity.stats.items.requirements.ItemRequirements;
import studio.magemonkey.divinity.stats.items.requirements.user.ClassRequirement;
import studio.magemonkey.divinity.stats.items.requirements.user.LevelRequirement;
import studio.magemonkey.divinity.utils.ItemUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetCommand extends ISubCommand<Divinity> {

    public SetCommand(@NotNull Divinity plugin) {
        super(plugin, new String[]{"set"}, Perms.ADMIN);
    }

    @Override
    @NotNull
    public String usage() {
        return "";
    }

    @Override
    @NotNull
    public String description() {
        return "Add item attributes.";
    }

    @Override
    public boolean playersOnly() {
        return true;
    }

    private static final String[] ARGS =
            new String[]{"level", "socket", "class", "damage", "defense", "stat", "ammo", "hand"};

    @Override
    @NotNull
    public List<String> getTab(@NotNull Player player, int i, @NotNull String[] args) {
        if (i == 1) {
            return Arrays.asList(ARGS);
        }

        String arg = args[1];
        if (i == 2) {
            if (arg.equalsIgnoreCase(ARGS[0])) {
                return Arrays.asList("<level>", "-1", "0", "1", "10");
            }
            if (arg.equalsIgnoreCase(ARGS[1])) {
                return CollectionsUT.getEnumsList(SocketAttribute.Type.class);
            }
            if (arg.equalsIgnoreCase(ARGS[2])) {
                return Arrays.asList("class1,class2,class3");
            }
            if (arg.equalsIgnoreCase(ARGS[3])) {
                List<String> list = new ArrayList<>();
                for (DamageAttribute d : ItemStats.getDamages()) {
                    list.add(d.getId());
                }
                return list;
            }
            if (arg.equalsIgnoreCase(ARGS[4])) {
                List<String> list = new ArrayList<>();
                for (DefenseAttribute d : ItemStats.getDefenses()) {
                    list.add(d.getId());
                }
                return list;
            }
            if (arg.equalsIgnoreCase(ARGS[5])) {
                return CollectionsUT.getEnumsList(SimpleStat.Type.class);
            }
            if (arg.equalsIgnoreCase(ARGS[6])) {
                return CollectionsUT.getEnumsList(AmmoAttribute.Type.class);
            }
            if (arg.equalsIgnoreCase(ARGS[7])) {
                return CollectionsUT.getEnumsList(HandAttribute.Type.class);
            }
        }

        if (i == 3) {
            if (arg.equalsIgnoreCase(ARGS[1])) {
                SocketAttribute.Type type = SocketAttribute.Type.getByName(args[2]);
                if (type != null) {
                    List<String> sockets = new ArrayList<>();
                    ItemStats.getSockets(type).forEach(socket -> {
                        sockets.add(socket.getId());
                    });
                    return sockets;
                }
            }
            if (arg.equalsIgnoreCase(ARGS[0])
                    || arg.equalsIgnoreCase(ARGS[2])
                    || arg.equalsIgnoreCase(ARGS[6])
                    || arg.equalsIgnoreCase(ARGS[7])) {
                return Arrays.asList("[position]");
            }
            if (arg.equalsIgnoreCase(ARGS[3])) { // Damage
                return Arrays.asList("<min>");
            }
            if (arg.equalsIgnoreCase(ARGS[4]) || arg.equalsIgnoreCase(ARGS[5])) { // Stat&Def
                return Arrays.asList("<value>");
            }
        }

        if (i == 4) {
            if (arg.equalsIgnoreCase(ARGS[3])) {
                return Arrays.asList("<max>");
            }
            if (arg.equalsIgnoreCase(ARGS[4])
                    || arg.equals(ARGS[1])
                    || arg.equalsIgnoreCase(ARGS[5])) { // Stat&Def
                return Arrays.asList("[position]");
            }
        }

        if (i == 5) {
            if (arg.equalsIgnoreCase(ARGS[3])) {
                return Arrays.asList("[position]");
            }
        }

        return super.getTab(player, i, args);
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length <= 1) {
            this.printHelp(sender);
            return;
        }

        Player    player = (Player) sender;
        ItemStack item   = player.getInventory().getItemInMainHand();

        if (ItemUT.isAir(item)) {
            plugin.lang().Error_NoItem.send(player);
            return;
        }

        int line = -1;

        switch (args[1].toLowerCase()) {
            case "level": {
                if (args.length < 3) {
                    this.printHelp(player);
                    return;
                }

                int level = this.getNumI(sender, args[2], -1);
                if (args.length == 4) {
                    line = this.getNumI(sender, args[3], -1);
                }

                LevelRequirement r1 = ItemRequirements.getUserRequirement(LevelRequirement.class);
                if (r1 == null) {
                    sender.sendMessage("Level Requirement is not registered!");
                    return;
                }

                r1.add(item, new int[]{level}, line);
                player.getInventory().setItemInMainHand(item);

                break;
            }

            case "socket": {
                if (args.length < 4) {
                    this.printUsage(sender);
                    return;
                }

                SocketAttribute.Type socketType = SocketAttribute.Type.getByName(args[2].toUpperCase());
                if (socketType == null) {
                    this.errType(sender, SocketAttribute.Type.class);
                    return;
                }

                String          socketId = args[3];
                SocketAttribute sb       = ItemStats.getSocket(socketType, socketId);
                if (sb == null) {
                    sender.sendMessage("No Such Socket Attribute!");
                    return;
                }
                sb.add(item, sb.getDefaultValue(), -1);

                break;
            }

            case "class": {
                if (args.length < 3) {
                    this.printHelp(player);
                    return;
                }
                if (args.length == 4) {
                    line = this.getNumI(sender, args[3], -1);
                }

                ClassRequirement r1 = ItemRequirements.getUserRequirement(ClassRequirement.class);
                if (r1 == null) {
                    sender.sendMessage("Class Requirement is not registered!");
                    return;
                }

                r1.add(item, args[2].split(","), line);
                player.getInventory().setItemInMainHand(item);

                break;
            }

            case "damage": {
                if (args.length < 5) {
                    this.printHelp(player);
                    return;
                }

                double val1 = StringUT.getDouble(args[3], 0, true);
                double val2 = StringUT.getDouble(args[4], 0, true);

                if (args.length == 6) {
                    line = this.getNumI(sender, args[5], -1);
                }

                DamageAttribute dt = ItemStats.getDamageById(args[2]);
                if (dt == null) {
                    sender.sendMessage(plugin.lang().Prefix.getMsg() + "Invalid damage type!");
                    return;
                }

                dt.add(item, new StatBonus(new double[]{val1, val2}, false, null), line);
                player.getInventory().setItemInMainHand(item);

                break;
            }

            case "defense": {
                if (args.length < 4) {
                    this.printHelp(player);
                    return;
                }

                double amount = StringUT.getDouble(args[3], 0, true);

                if (args.length == 5) {
                    line = this.getNumI(sender, args[4], -1);
                }

                DefenseAttribute dt = ItemStats.getDefenseById(args[2]);
                if (dt == null) {
                    sender.sendMessage(plugin.lang().Prefix.getMsg() + "Invalid defense type!");
                    return;
                }

                dt.add(item, new StatBonus(new double[]{amount}, false, null), line);
                player.getInventory().setItemInMainHand(item);

                break;
            }

            case "ammo": {
                if (args.length < 3) {
                    this.printHelp(player);
                    return;
                }
                if (!ItemUtils.isBow(item)) {
                    plugin.lang().Error_InvalidItem.send(player);
                    return;
                }

                AmmoAttribute.Type dt;
                try {
                    dt = AmmoAttribute.Type.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException ex) {
                    this.errType(sender, AmmoAttribute.Type.class);
                    return;
                }

                if (args.length == 4) {
                    line = this.getNumI(sender, args[3], -1);
                }

                AmmoAttribute ammo = ItemStats.getAmmo(dt);
                if (ammo != null) {
                    ammo.add(item, ammo.getType().name(), line);
                }

                break;
            }

            case "hand": {
                if (args.length < 3) {
                    this.printHelp(player);
                    return;
                }

                HandAttribute.Type dt;
                try {
                    dt = HandAttribute.Type.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException ex) {
                    this.errType(sender, HandAttribute.Type.class);
                    return;
                }

                HandAttribute hand = ItemStats.getHand(dt);
                if (hand == null) {
                    return;
                }

                if (args.length == 4) {
                    line = this.getNumI(sender, args[3], -1);
                }

                hand.add(item, "", line);
                break;
            }

            case "stat": {
                if (args.length < 4) {
                    this.printHelp(player);
                    return;
                }

                TypedStat.Type at = TypedStat.Type.getByName(args[2]);
                if (at == null) {
                    this.errType(sender, SimpleStat.Type.class);
                    return;
                }

                TypedStat stat = studio.magemonkey.divinity.stats.items.ItemStats.getStat(at);
                if (stat == null) {
                    sender.sendMessage("Stat is not registered!");
                    return;
                }

                double val = StringUT.getDouble(args[3], 0, true);

                if (args.length == 5) {
                    line = this.getNumI(sender, args[4], -1);
                }

                if (stat instanceof SimpleStat) {
                    SimpleStat rs = (SimpleStat) stat;
                    rs.add(item, new StatBonus(new double[]{val}, false, null), line);
                } else {
                    DurabilityStat ms = (DurabilityStat) stat;
                    ms.add(item, new double[]{val, val}, line);
                }

                player.getInventory().setItemInMainHand(item);

                break;
            }

            default: {
                printHelp(player);
                return;
            }
        }
        plugin.lang().Command_Modify_Done.send(player);
    }

    private void printHelp(CommandSender p) {
        plugin.lang().Command_Set_List.send(p);
    }
}
