package studio.magemonkey.divinity.manager.effects.main;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.manager.effects.IEffect;
import studio.magemonkey.divinity.manager.effects.IEffectType;

public class DisarmEffect extends IEffect {

    public DisarmEffect() {
        this(null);
    }

    public DisarmEffect(@Nullable LivingEntity caster) {
        this(caster, 1);
    }

    public DisarmEffect(@Nullable LivingEntity caster, int charges) {
        super(caster, charges);
    }

    @Override
    @NotNull
    public IEffectType getType() {
        return IEffectType.DISARM;
    }

    @Override
    protected boolean onTrigger(boolean force) {
        EntityEquipment equipment = this.target.getEquipment();
        if (equipment == null) return true;

        ItemStack hand = equipment.getItemInMainHand();
        if (ItemUT.isAir(hand)) {
            return true;
        }

        equipment.setItemInMainHand(null);
        this.target.getWorld().dropItemNaturally(target.getLocation(), hand).setPickupDelay(50);

        return true;
    }

    @Override
    protected void onClear() {

    }

    @Override
    public boolean resetOnDeath() {
        return true;
    }
}
