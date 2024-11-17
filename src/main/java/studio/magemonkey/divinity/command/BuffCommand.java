package studio.magemonkey.divinity.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.commands.api.ISubCommand;
import studio.magemonkey.codex.util.PlayerUT;
import studio.magemonkey.codex.util.TimeUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.data.api.DivinityUser;
import studio.magemonkey.divinity.manager.effects.buffs.SavedBuff;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.api.ItemLoreStat;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BuffCommand extends ISubCommand<Divinity> {

    public BuffCommand(@NotNull Divinity plugin) {
        super(plugin, new String[]{"buff"}, Perms.ADMIN);
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Command_Buff_Desc.getMsg();
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Command_Buff_Usage.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return false;
    }

    @Override
    @NotNull
    public List<String> getTab(@NotNull Player player, int i, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (!Arrays.asList(args).contains("-r"))
            list.add("-r");
        else {
            List<String> temp = new ArrayList<>(Arrays.asList(args));
            temp.remove("-r");
            args = temp.toArray(new String[0]);
            i--;
        }

        if (i == 1) {
            list.addAll(PlayerUT.getPlayerNames());
        } else if (i == 2) {
            list.addAll(Arrays.asList("damage", "defense", "stat"));
        } else if (i == 3) {
            if (args[2].equalsIgnoreCase("damage")) {
                ItemStats.getDamages().forEach(d -> list.add(d.getId()));
            } else if (args[2].equalsIgnoreCase("defense")) {
                ItemStats.getDefenses().forEach(d -> list.add(d.getId()));
            } else if (args[2].equalsIgnoreCase("stat")) {
                ItemStats.getStats().forEach(d -> list.add(d.getId()));
            }
        } else if (i == 4) {
            list.addAll(Arrays.asList("<amount>", "10", "25%"));
        } else if (i == 5) {
            list.addAll(Arrays.asList("<duration>", "60", "300"));
        }

        return list;
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length != 6 && args.length != 7) {
            this.printUsage(sender);
            return;
        }

        boolean replace = false;
        if (Arrays.asList(args).contains("-r")) {
            List<String> list = new ArrayList<>(Arrays.asList(args));
            list.remove("-r");
            args = list.toArray(new String[0]);
            replace = true;
        }

        Player player = plugin.getServer().getPlayer(args[1]);
        if (player == null) {
            this.errPlayer(sender);
            return;
        }

        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) {
            this.errPlayer(sender);
            return;
        }

        Set<SavedBuff> userBuffs = null;

        String type   = args[2];
        String statId = args[3];

        boolean isModifier = args[4].endsWith("%");
        double  amount     = this.getNumD(sender, args[4].replace("%", ""), 0);
        int     seconds    = this.getNumI(sender, args[5], 0);
        if (seconds == 0) return;

        ItemLoreStat<?> stat = null;
        if (type.equalsIgnoreCase("damage")) {
            stat = ItemStats.getDamageById(statId);
            userBuffs = user.getActiveProfile().getDamageBuffs();
        } else if (type.equalsIgnoreCase("defense")) {
            stat = ItemStats.getDefenseById(statId);
            userBuffs = user.getActiveProfile().getDefenseBuffs();
        } else if (type.equalsIgnoreCase("stat")) {
            TypedStat.Type statType = TypedStat.Type.getByName(statId);
            if (statType != null) {
                stat = (ItemLoreStat<?>) ItemStats.getStat(statType);
                userBuffs = user.getActiveProfile().getItemStatBuffs();
            }
        }

        if (stat == null || userBuffs == null) {
            sender.sendMessage("Invalid stat id!");
            return;
        }

        if (replace) userBuffs.removeIf(buff -> buff.getStatId().equals(statId));
        SavedBuff buff = new SavedBuff(stat, amount, isModifier, seconds);
        userBuffs.add(buff);

        plugin.lang().Command_Buff_Done
                .replace("%time%", TimeUT.formatTime(seconds * 1000L))
                .replace("%stat%", stat.getName())
                .replace("%amount%", args[4])
                .replace("%player%", player.getName())
                .send(sender);

        plugin.lang().Command_Buff_Get
                .replace("%time%", TimeUT.formatTime(seconds * 1000L))
                .replace("%stat%", stat.getName())
                .replace("%amount%", args[4])
                .send(player);
    }
}
