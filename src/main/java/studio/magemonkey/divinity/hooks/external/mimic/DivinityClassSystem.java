package studio.magemonkey.divinity.hooks.external.mimic;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.endlesscode.mimic.classes.BukkitClassSystem;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.api.DivinityAPI;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;
import studio.magemonkey.divinity.modules.list.classes.api.RPGClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DivinityClassSystem extends BukkitClassSystem {

    private static final String ID = "divinity";

    private final ClassManager classManager;

    public DivinityClassSystem(@NotNull Player player) {
        super(player);
        classManager = DivinityAPI.getModuleManager().getClassManager();
    }

    @NotNull
    @Override
    public List<String> getClasses() {
        RPGClass playerClass = getClassData();
        if (playerClass == null) return Collections.emptyList();

        List<RPGClass> classes = new ArrayList<>();
        classes.add(playerClass);
        classes.addAll(playerClass.getParents());

        return classes.stream()
                .map(RPGClass::getName)
                .map(StringUT::colorOff)
                .collect(Collectors.toList());
    }

    @Nullable
    private RPGClass getClassData() {
        return Objects.requireNonNull(classManager.getUserData(getPlayer()))
                .getPlayerClass();
    }

    public static class Provider implements BukkitClassSystem.Provider {
        @NotNull
        @Override
        public BukkitClassSystem getSystem(@NotNull Player player) {
            return new DivinityClassSystem(player);
        }
    }
}
