package studio.magemonkey.divinity.utils.actions.conditions;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.util.actions.conditions.IConditionValidator;
import studio.magemonkey.codex.util.actions.params.IParamResult;
import studio.magemonkey.codex.util.actions.params.IParamType;
import studio.magemonkey.codex.util.actions.params.IParamValue;
import studio.magemonkey.codex.util.actions.params.IParamValue.IOperator;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.EngineCfg;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class CEntityLevel extends IConditionValidator {

    public CEntityLevel(@NotNull Divinity plugin) {
        super(plugin, "ENTITY_LEVEL");
    }

    @Override
    public void registerParams() {
        this.registerParam(IParamType.TARGET);
        this.registerParam(IParamType.AMOUNT);
        this.registerParam(IParamType.MESSAGE);
    }

    @Override
    public boolean mustHaveTarget() {
        return true;
    }

    @Override
    @Nullable
    protected Predicate<Entity> validate(Entity exe, Set<Entity> targets, IParamResult result) {
        IParamValue value = result.getParamValue(IParamType.AMOUNT);
        IOperator   oper  = value.getOperator();
        int         lvl   = value.getInt(-1);
        if (lvl < 0) return null;

        return (e) -> {
            double eLvl = 0;
            if (e instanceof Player) eLvl = EngineCfg.HOOK_PLAYER_LEVEL_PLUGIN.getLevel((Player) e);
            else eLvl = EngineCfg.HOOK_MOB_LEVEL_PLUGIN.getMobLevel(e);

            return oper.check((int) eLvl, lvl);
        };
    }

    @Override
    @NotNull
    public List<String> getDescription() {
        return Arrays.asList("Checks for entity level");
    }
}
