package studio.magemonkey.divinity.modules.list.classes.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;
import studio.magemonkey.divinity.modules.list.classes.api.IAbstractSkill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CastSkillCmd extends MCmd<ClassManager> {

    public CastSkillCmd(@NotNull ClassManager module) {
        super(module, new String[]{"cast"}, Perms.CLASS_CMD_CAST);
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Classes_Cmd_Cast_Usage.getMsg();
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Classes_Cmd_Cast_Desc.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return true;
    }

    @Override
    @NotNull
    public List<String> getTab(@NotNull Player player, int i, @NotNull String[] args) {
        if (i == 1) {
            return new ArrayList<>(this.module.getSkillIds());
        }
        if (i == 2) {
            return Arrays.asList("1", "2", "3");
        }
        if (i == 3) {
            return Arrays.asList("true", "false");
        }
        return super.getTab(player, i, args);
    }

    @Override
    protected void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            this.printUsage(sender);
            return;
        }

        Player         caster  = (Player) sender;
        String         skillId = args[1];
        IAbstractSkill skill   = this.module.getSkillById(skillId);
        if (skill == null) {
            plugin.lang().Classes_Cmd_Cast_Error_InvalidSkill.send(sender);
            return;
        }

        ItemStack weapon = caster.getInventory().getItemInMainHand();
        int       level  = args.length >= 3 ? this.getNumI(sender, args[2], 1) : 1;
        boolean   force  = args.length >= 4 ? Boolean.parseBoolean(args[3]) : false;

        skill.cast(caster, weapon, level, force);
    }
}
