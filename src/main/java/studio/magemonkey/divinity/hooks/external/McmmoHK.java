package studio.magemonkey.divinity.hooks.external;

import com.gmail.nossr50.api.ExperienceAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.hooks.HookState;
import studio.magemonkey.codex.hooks.NHook;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.hooks.HookLevel;

public class McmmoHK extends NHook<Divinity> implements HookLevel {

    public McmmoHK(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public HookState setup() {
        return HookState.SUCCESS;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public int getLevel(@NotNull Player player) {
        return ExperienceAPI.getPowerLevel(player);
    }

    @Override
    public void giveExp(@NotNull Player player, int amount) {
        // Don't really know which skill would be right here since exp can come from several sources,
        // will leave empty unless this is brought up again
    }
}
