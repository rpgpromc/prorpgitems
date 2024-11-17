package studio.magemonkey.divinity.modules.api;

import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.commands.list.HelpCommand;
import studio.magemonkey.codex.modules.IModule;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.api.socketing.ModuleSocket;
import studio.magemonkey.divinity.modules.command.*;

public abstract class QModule extends IModule<Divinity> {

    public QModule(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    protected void onPostSetup() {
        if (this instanceof QModuleDrop) {
            QModuleDrop<?> md = (QModuleDrop<?>) this;
            md.loadSettings();
            md.loadItems();
        }

        if (this.moduleCommand != null) {
            this.moduleCommand.addDefaultCommand(new HelpCommand<>(this.plugin));
            if (this.isDropable()) {
                QModuleDrop<?> md = (QModuleDrop<?>) this;
                this.moduleCommand.addSubCommand(new MGetCmd(md));
                this.moduleCommand.addSubCommand(new MGiveCmd(md));
                this.moduleCommand.addSubCommand(new MDropCmd(md));
                this.moduleCommand.addSubCommand(new MListCmd(md));
            }
            this.moduleCommand.addSubCommand(new MReloadCmd(this));
        }
    }

    public final boolean isDropable() {
        return this instanceof QModuleDrop;
    }

    public final boolean isSocketable() {
        return this instanceof ModuleSocket;
    }
}
