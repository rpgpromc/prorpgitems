package studio.magemonkey.divinity.hooks.external;

import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.hooks.HookState;
import studio.magemonkey.codex.hooks.NHook;
import studio.magemonkey.divinity.Divinity;

public class MagicHK extends NHook<Divinity> {

    private MagicAPI api;

    public MagicHK(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    protected HookState setup() {
        Plugin magicPlugin = plugin.getPluginManager().getPlugin(this.getPlugin());
        if (magicPlugin == null || !(magicPlugin instanceof MagicAPI)) {
            return HookState.ERROR;
        }
        this.api = (MagicAPI) magicPlugin;
        return HookState.SUCCESS;
    }

    @Override
    protected void shutdown() {

    }

    @NotNull
    public MagicAPI getAPI() {
        return this.api;
    }
}
