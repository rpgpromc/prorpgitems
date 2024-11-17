package studio.magemonkey.divinity.hooks.external.mimic;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.endlesscode.mimic.level.BukkitLevelSystem;
import ru.endlesscode.mimic.level.ExpLevelConverter;
import studio.magemonkey.divinity.api.DivinityAPI;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;

import java.util.Objects;

public class DivinityLevelSystem extends BukkitLevelSystem {

    private static final String ID = "divinity";

    private final ClassManager classManager;

    public DivinityLevelSystem(@NotNull Player player) {
        super(player);
        classManager = DivinityAPI.getModuleManager().getClassManager();
    }


    @NotNull
    @Override
    public ExpLevelConverter getConverter() {
        return new DivinityExpLevelConverter(getPlayer());
    }

    @Override
    public int getLevel() {
        return getClassData().getLevel();
    }

    @Override
    public void setLevel(int level) {
        getClassData().setLevel(level);
    }

    @Override
    public double getExp() {
        return getClassData().getExp();
    }

    @Override
    public void setExp(double exp) {
        double allowedExp = Math.min(Math.max(0, exp), getExpToNextLevel());
        getClassData().setExp((int) Math.round(allowedExp));
    }

    @Override
    public double getExpToNextLevel() {
        return getClassData().getExpToUp(false);
    }

    @Override
    public double getTotalExpToNextLevel() {
        return getClassData().getExpToUp(true);
    }

    @Override
    public void takeExp(double expAmount) {
        giveExp(-Math.min(expAmount, getTotalExp()));
    }

    @Override
    public void giveExp(double expAmount) {
        getClassData().addExp((int) Math.round(expAmount));
    }

    private UserClassData getClassData() {
        return Objects.requireNonNull(classManager.getUserData(getPlayer()));
    }

    public static class Provider implements BukkitLevelSystem.Provider {
        @NotNull
        @Override
        public BukkitLevelSystem getSystem(@NotNull Player player) {
            return new DivinityLevelSystem(player);
        }
    }
}
