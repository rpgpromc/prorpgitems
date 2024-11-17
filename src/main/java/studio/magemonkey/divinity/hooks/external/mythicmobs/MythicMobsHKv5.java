package studio.magemonkey.divinity.hooks.external.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.hooks.HookState;
import studio.magemonkey.divinity.Divinity;

import java.util.Optional;

public class MythicMobsHKv5 extends AbstractMythicMobsHK {

    private MythicBukkit mm;

    public MythicMobsHKv5(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public HookState setup() {
        this.mm = MythicBukkit.inst();
        return HookState.SUCCESS;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean isMythicMob(@NotNull Entity entity) {
        return getMythicInstance(entity) != null;
    }

    @Override
    @NotNull
    public String getMythicNameByEntity(@NotNull Entity entity) {
        AbstractMythicEntity mob = getMythicInstance(entity);
        return mob == null ? null : mob.getInternalName();
    }

    @Override
    public AbstractMythicEntity getMythicInstance(@NotNull Entity entity) {
        ActiveMob mob = getActiveMythicInstance(entity);

        return mob != null ? new MythicEntity5(mob.getType()) : null;
    }

    @Override
    public int getMythicVersion() {
        return 5;
    }

    @Override
    public double getMobLevel(@NotNull Entity entity) {
        ActiveMob mob = getActiveMythicInstance(entity);

        return mob != null ? Math.max(1, mob.getLevel()) : 1;
    }

    @Override
    public void setSkillDamage(@NotNull Entity entity, double amount) {
        if (!isMythicMob(entity)) return;
        ActiveMob am1 = getActiveMythicInstance(entity);
        am1.setLastDamageSkillAmount(amount);
    }

    public ActiveMob getActiveMythicInstance(@NotNull Entity entity) {
        Optional<ActiveMob> mob = mm.getMobManager().getActiveMob(entity.getUniqueId());

        return mob.isPresent() ? mob.get() : null;
    }
}
