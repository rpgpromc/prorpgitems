package studio.magemonkey.divinity.utils.actions.executors;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.actions.actions.IActionExecutor;
import studio.magemonkey.codex.util.actions.params.IParamResult;
import studio.magemonkey.codex.util.actions.params.IParamType;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.utils.ParticleUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ActionParticleLine extends IActionExecutor {

    public ActionParticleLine(@NotNull Divinity plugin) {
        super(plugin, "PARTICLE_LINE");
    }

    @Override
    public void registerParams() {
        this.registerParam(IParamType.DELAY);
        this.registerParam(IParamType.NAME);
        this.registerParam(IParamType.TARGET);
        this.registerParam(IParamType.AMOUNT);
        this.registerParam(IParamType.SPEED);
        this.registerParam(IParamType.OFFSET);
    }

    @Override
    protected void execute(Entity exe, Set<Entity> targets, IParamResult result) {
        String name = result.getParamValue(IParamType.NAME).getString(null);
        if (name == null) return;

        double[] offset = result.getParamValue(IParamType.OFFSET).getDoubleArray();

        int amount = result.getParamValue(IParamType.AMOUNT).getInt(30);

        float speed = (float) result.getParamValue(IParamType.SPEED).getDouble(0.1);

        if (!targets.isEmpty()) {
            for (Entity e : targets) {
                Location loc;
                if (e instanceof LivingEntity) {
                    loc = ((LivingEntity) e).getEyeLocation();
                } else loc = e.getLocation();

                ParticleUtils.drawParticleLine(
                        exe.getLocation(), loc, name,
                        (float) offset[0], (float) offset[1], (float) offset[2], speed, amount);
            }
        } else {
            if (exe instanceof LivingEntity) {
                LivingEntity caster = (LivingEntity) exe;
                Location to =
                        caster.getTargetBlock(null, (int) EngineCfg.COMBAT_MAX_GET_TARGET_DISTANCE).getLocation();

                ParticleUtils.drawParticleLine(
                        caster.getEyeLocation().clone().add(0, -0.5, 0), to, name,
                        (float) offset[0], (float) offset[1], (float) offset[2], speed, amount);
            }
        }
    }

    @Override
    public boolean mustHaveTarget() {
        return false;
    }

    @Override
    @NotNull
    public List<String> getDescription() {
        // TODO Auto-generated method stub
        return Arrays.asList("Draws a particle line");
    }
}
