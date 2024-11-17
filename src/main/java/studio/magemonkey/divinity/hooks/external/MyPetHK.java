package studio.magemonkey.divinity.hooks.external;

import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.hooks.HookState;
import studio.magemonkey.codex.hooks.NHook;
import studio.magemonkey.divinity.Divinity;

public class MyPetHK extends NHook<Divinity> {

    public MyPetHK(@NotNull Divinity plugin) {
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

    public boolean isPet(@NotNull Entity entity) {
        return entity instanceof MyPetBukkitEntity;
    }

    @Nullable
    public Player getPetOwner(@NotNull Entity entity) {
        return ((MyPetBukkitEntity) entity).getMyPet().getOwner().getPlayer();
    }
}
