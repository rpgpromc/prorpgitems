package studio.magemonkey.divinity.hooks.external.mythicmobs;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.hooks.HookState;
import studio.magemonkey.divinity.Divinity;

public class MythicMobsHK extends AbstractMythicMobsHK {

    private MythicMobs mm;

    public MythicMobsHK(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public HookState setup() {
        this.mm = MythicMobs.inst();
        return HookState.SUCCESS;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean isMythicMob(@NotNull Entity entity) {
        return mm.getAPIHelper().isMythicMob(entity);
    }

    @Override
    @NotNull
    public String getMythicNameByEntity(@NotNull Entity entity) {
        return mm.getAPIHelper().getMythicMobInstance(entity).getType().getInternalName();
    }

    public AbstractMythicEntity getMythicInstance(@NotNull Entity entity) {
        return new MythicEntity4(mm.getAPIHelper().getMythicMobInstance(entity).getType());
    }

    @Override
    public int getMythicVersion() {
        return 4;
    }

    @Override
    public double getMobLevel(@NotNull Entity entity) {
        ActiveMob activeMob = mm.getMobManager().getMythicMobInstance(entity);
        if (activeMob == null) return 1D;

        return Math.max(1, activeMob.getLevel());
    }

    @Override
    public void setSkillDamage(@NotNull Entity entity, double amount) {
        if (!isMythicMob(entity)) return;
        ActiveMob activeMob = mm.getMobManager().getMythicMobInstance(entity);
        activeMob.setLastDamageSkillAmount(amount);
    }
}
