package studio.magemonkey.divinity.hooks.external;

import me.lorinth.rpgmobs.LorinthsRpgMobs;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.hooks.HookState;
import studio.magemonkey.codex.hooks.NHook;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.hooks.HookMobLevel;

public class LorinthsRpgMobsHK extends NHook<Divinity> implements HookMobLevel {

    public LorinthsRpgMobsHK(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    protected HookState setup() {
        return HookState.SUCCESS;
    }

    @Override
    protected void shutdown() {

    }

    @Override
    public double getMobLevel(@NotNull Entity entity) {
        return LorinthsRpgMobs.GetLevelOfEntity(entity).intValue();
    }

}
