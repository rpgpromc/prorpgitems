package studio.magemonkey.divinity.utils.actions.params;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.util.actions.params.IParamValue;

import java.util.HashSet;
import java.util.Set;

public class AttackableParam extends studio.magemonkey.codex.util.actions.params.list.AttackableParam {

    public AttackableParam() {
        super();
    }

    @Override
    public void autoValidate(@NotNull Entity exe, @NotNull Set<Entity> targets, @NotNull IParamValue val) {
        boolean b = val.getBoolean();
        for (Entity e : new HashSet<>(targets)) {
            boolean attackable = Hooks.canFights(exe, e);
            if (attackable != b) {
                targets.remove(e);
            }
        }
    }
}
