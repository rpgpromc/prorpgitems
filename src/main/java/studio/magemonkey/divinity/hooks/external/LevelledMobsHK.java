package studio.magemonkey.divinity.hooks.external;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.hooks.HookState;
import studio.magemonkey.codex.hooks.NHook;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.hooks.HookMobLevel;

public class LevelledMobsHK extends NHook<Divinity> implements HookMobLevel {

    public LevelledMobsHK(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    protected HookState setup() {
        return HookState.SUCCESS;
    }

    @Override
    protected void shutdown() {}

    @Override
    public double getMobLevel(@NotNull Entity entity) {
        if (!(entity instanceof LivingEntity)) return 0.0D;
        if (Bukkit.getPluginManager().getPlugin("LevelledMobs") == null || !LevelledMobs.getInstance().isEnabled())
            return 0.0D;

        return LevelledMobs.getInstance().levelManager.getLevelOfMob((LivingEntity) entity);
    }
}
