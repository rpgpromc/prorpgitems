package studio.magemonkey.divinity.nms.engine;

import studio.magemonkey.codex.util.reflection.ReflectionManager;
import studio.magemonkey.codex.util.reflection.ReflectionUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PMS {

    protected ReflectionUtil reflectionUtil;

    public PMS() {
        reflectionUtil = ReflectionManager.getReflectionUtil();
    }

    public float getAttackCooldown(@NotNull Player p) {
        return p.getAttackCooldown();
    }

    public void changeSkull(Block b, String hash) {
        reflectionUtil.changeSkull(b, hash);
    }

    public double getDefaultDamage(@NotNull ItemStack itemStack) {
        return reflectionUtil.getDefaultDamage(itemStack);
    }

    public double getDefaultSpeed(@NotNull ItemStack itemStack) {
        return reflectionUtil.getDefaultSpeed(itemStack);
    }

    public double getDefaultArmor(@NotNull ItemStack itemStack) {
        return reflectionUtil.getDefaultArmor(itemStack);
    }

    public double getDefaultToughness(@NotNull ItemStack itemStack) {
        return reflectionUtil.getDefaultToughness(itemStack);
    }
}
