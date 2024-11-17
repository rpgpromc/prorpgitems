package studio.magemonkey.divinity.modules.command;

import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.commands.api.ISubCommand;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.api.QModule;

public abstract class MCmd<M extends QModule> extends ISubCommand<Divinity> {

    protected M module;

    public MCmd(@NotNull M module, @NotNull String[] aliases, String permission) {
        super(module.plugin, aliases, permission);
        this.module = module;
    }
}
