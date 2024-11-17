package studio.magemonkey.divinity.hooks.internal;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.hooks.HookClass;
import studio.magemonkey.divinity.hooks.HookLevel;
import studio.magemonkey.divinity.hooks.HookMobLevel;

public class DefaultHook implements HookLevel, HookClass, HookMobLevel {
    @Override
    public int getLevel(@NotNull Player player) {
        return player.getLevel();
    }

    @Override
    public void giveExp(@NotNull Player player, int amount) {
        player.giveExp(amount);
    }

    @Override
    @NotNull
    public String getClass(@NotNull Player player) {
        String klass = "";
        for (PermissionAttachmentInfo pio : player.getEffectivePermissions()) {
            String perm = pio.getPermission();
            if (perm.startsWith(Perms.CLASS)) {
                String ending = perm.substring(perm.lastIndexOf("."), perm.length());
                klass = ending.replace(".", "");
            }
        }
        return klass;
    }

    @Override
    public void takeMana(@NotNull Player player, double amount, boolean ofMax) {
        throw new UnsupportedOperationException("Your class plugin does not provides mana function.");
    }

    @Override
    public double getMobLevel(@NotNull Entity entity) {
        return 1;
    }
}
