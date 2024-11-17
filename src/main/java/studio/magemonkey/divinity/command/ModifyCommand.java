package studio.magemonkey.divinity.command;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.commands.api.ISubCommand;
import studio.magemonkey.codex.util.CollectionsUT;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.utils.ItemUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModifyCommand extends ISubCommand<Divinity> {

    public ModifyCommand(@NotNull Divinity plugin) {
        super(plugin, new String[]{"modify"}, Perms.ADMIN);
    }

    @Override
    @NotNull
    public String usage() {
        return "";
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Command_Modify_Desc.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return true;
    }

    private static final String[] ARGS = new String[]{"name", "lore", "flag", "nbt", "enchant", "potion", "color"};

    @Override
    @NotNull
    public List<String> getTab(@NotNull Player player, int i, @NotNull String[] args) {
        if (i == 1) {
            return Arrays.asList(ARGS);
        }

        String arg = args[1];
        if (i == 2) {
            if (arg.equalsIgnoreCase(ARGS[0])) {
                return Arrays.asList("<new name>");
            }
            if (arg.equalsIgnoreCase(ARGS[1])) {
                return Arrays.asList("add", "del", "clear");
            }
            if (arg.equalsIgnoreCase(ARGS[2])) {
                return Arrays.asList("add", "del");
            }
            if (arg.equalsIgnoreCase(ARGS[4])) {
                List<String> enchants = new ArrayList<>();
                for (Enchantment e : Enchantment.values()) {
                    enchants.add(e.getKey().getKey());
                }
                return enchants;
            }
            if (arg.equalsIgnoreCase(ARGS[5])) {
                List<String> enchants = new ArrayList<>();
                for (PotionEffectType e : PotionEffectType.values()) {
                    enchants.add(e.getName());
                }
                return enchants;
            }
            if (arg.equalsIgnoreCase(ARGS[6])) {
                return Arrays.asList("<R,G,B>", "255,255,255");
            }
        }

        String arg2 = args[2];
        if (i >= 3) {
            if (arg.equalsIgnoreCase(ARGS[1])) {
                if (arg2.equalsIgnoreCase("add")) {
                    return Arrays.asList("<text>");
                }
            }
        }

        if (i == 3) {
            if (arg.equalsIgnoreCase(ARGS[1])) {
                if (arg2.equalsIgnoreCase("del")) {
                    return Arrays.asList("<position>");
                }
            }
            if (arg.equalsIgnoreCase(ARGS[2])) { // Flag
                return CollectionsUT.getEnumsList(ItemFlag.class);
            }
            if (arg.equalsIgnoreCase(ARGS[4]) || arg.equalsIgnoreCase(ARGS[5])) { // Enchant
                return Arrays.asList("0", "1", "5", "10", "127");
            }
        }

        if (i == 4) {
            if (arg.equalsIgnoreCase(ARGS[5])) { // Potion
                return Arrays.asList("<duration>", "30", "60", "300");
            }
        }

        return super.getTab(player, i, args);
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        Player    p    = (Player) sender;
        ItemStack item = p.getInventory().getItemInMainHand();

        if (ItemUT.isAir(item)) {
            plugin.lang().Error_NoItem.send(sender);
            return;
        }

        if (args.length >= 3 && args[1].equalsIgnoreCase(ARGS[0])) { // Name
            String name = "";
            for (int i = 2; i < args.length; i++) {
                name = name + args[i] + " ";
            }
            ItemUtils.setName(item, name.trim());
        } else if (args.length >= 3 && args[1].equalsIgnoreCase(ARGS[1])) { // Lore
            if (args[2].equalsIgnoreCase("add") && args.length >= 4) {
                String line = "";
                int    l    = args.length;
                int    x    = l;
                int    pos  = -1;

                if (StringUtils.isNumeric(args[l - 1])) {
                    x = x - 1;
                    pos = Integer.parseInt(args[l - 1]);
                }
                for (int i = 3; i < x; i++) {
                    line = line + args[i] + " ";
                }

                ItemUtils.addLoreLine(item, line.trim(), pos);
            } else if (args[2].equalsIgnoreCase("del") && args.length >= 4) {
                int pos = this.getNumI(sender, args[3], -1);
                if (pos < 0) return;

                ItemUtils.delLoreLine(item, pos);
            } else if (args[2].equalsIgnoreCase("clear")) {
                ItemUtils.clearLore(item);
            } else {
                printHelp(p);
                return;
            }
        } else if (args.length == 4 && args[1].equalsIgnoreCase(ARGS[2])) { // FLAG
            String   flag = args[3].toUpperCase();
            ItemFlag f    = null;

            try {
                f = ItemFlag.valueOf(flag);
            } catch (IllegalArgumentException ex) {
                this.errType(sender, ItemFlag.class);
                return;
            }

            if (args[2].equalsIgnoreCase("add")) {
                ItemUtils.addFlag(item, f);
            } else if (args[2].equalsIgnoreCase("del")) {
                ItemUtils.delFlag(item, f);
            } else {
                printHelp(p);
                return;
            }
        } else if (args.length == 4 && args[1].equalsIgnoreCase(ARGS[4])) { // Enchant
            String      ench = args[2].toLowerCase();
            Enchantment e    = Enchantment.getByKey(NamespacedKey.minecraft(ench));
            if (e == null) {
                plugin.lang().Error_InvalidArgument.replace("%arg%", ench).send(p);
                return;
            }

            int lvl = StringUT.getInteger(args[3], -1, true);

            ItemUtils.addEnchant(item, e, lvl);
        } else if (args.length >= 5 && args[1].equalsIgnoreCase(ARGS[5])) { // Potion
            String           eff  = args[2].toUpperCase();
            PotionEffectType type = PotionEffectType.getByName(eff);
            if (type == null) {
                plugin.lang().Error_InvalidArgument.replace("%arg%", eff).send(p);
                return;
            }

            int lvl = StringUT.getInteger(args[3], -1, true);
            int dur = StringUT.getInteger(args[4], 20);

            boolean ambient   = false;
            boolean particles = true;

            if (args.length == 6) {
                ambient = Boolean.valueOf(args[5]);
            }
            if (args.length == 7) {
                particles = Boolean.valueOf(args[6]);
            }
            ItemUtils.addPotionEffect(item, type, lvl, dur, ambient, particles);
        } else if (args.length == 3 && args[1].equalsIgnoreCase(ARGS[6])) { // Color
            String[] s1 = args[2].split(",");

            int r = Math.max(0, Math.max(this.getNumI(sender, s1[0], 255), 255));
            int g = Math.max(0, Math.max(this.getNumI(sender, s1[0], 255), 255));
            int b = Math.max(0, Math.max(this.getNumI(sender, s1[0], 255), 255));

            ItemUtils.setColor(item, Color.fromRGB(r, g, b));
        } else {
            this.printHelp(p);
            return;
        }

        plugin.lang().Command_Modify_Done.send(p);
    }

    private void printHelp(Player p) {
        plugin.lang().Command_Modify_List.send(p);
    }
}
