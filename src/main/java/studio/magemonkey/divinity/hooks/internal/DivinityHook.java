package studio.magemonkey.divinity.hooks.internal;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.data.api.DivinityUser;
import studio.magemonkey.divinity.data.api.UserProfile;
import studio.magemonkey.divinity.hooks.HookClass;
import studio.magemonkey.divinity.hooks.HookLevel;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;

public class DivinityHook implements HookLevel, HookClass {
    private Divinity plugin;

    public DivinityHook(@NotNull Divinity plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getClass(@NotNull Player player) {
        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return "";

        UserProfile   prof  = user.getActiveProfile();
        UserClassData cData = prof.getClassData();
        if (cData == null) return "";

        return StringUT.colorOff(cData.getPlayerClass().getName());
    }

    @Override
    public int getLevel(@NotNull Player player) {
        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return 0;

        UserProfile   prof  = user.getActiveProfile();
        UserClassData cData = prof.getClassData();
        return cData != null ? cData.getLevel() : 0;
    }

    @Override
    public void giveExp(@NotNull Player player, int amount) {
        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return;

        UserProfile   prof  = user.getActiveProfile();
        UserClassData cData = prof.getClassData();
        if (cData == null) {
            return;
        }
        cData.addExp(amount);
    }

    @Override
    public void takeMana(@NotNull Player player, double amount, boolean ofMax) {
        ClassManager classManager = plugin.getModuleCache().getClassManager();
        if (classManager == null) return;

        classManager.consumeMana(player, amount, ofMax);
    }

}
