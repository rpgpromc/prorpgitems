package studio.magemonkey.divinity.modules.list.money;

import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.commands.list.HelpCommand;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.external.VaultHK;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.ModuleItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.command.MReloadCmd;

public class MoneyManager extends QModuleDrop<MoneyManager.QMoney> {

    public MoneyManager(@NotNull Divinity plugin) {
        super(plugin, QMoney.class);
    }

    @Override
    public String getId() {
        return EModule.MONEY;
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public void setup() {

        VaultHK vault = plugin.getVault();

        if (vault == null || vault.getEconomy() == null) {
            this.error("Economy not found! Module will be disabled.");
            this.interruptLoad();
            return;
        }
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void onPostSetup() {

        this.loadSettings();
        this.loadItems();

        if (this.moduleCommand != null) {
            this.moduleCommand.addDefaultCommand(new HelpCommand<>(this.plugin));
            this.moduleCommand.addSubCommand(new MReloadCmd(this));
        }
    }


    public class QMoney extends ModuleItem {

        private static final String MIN_MONEY      = "money.min";
        private static final String MAX_MONEY      = "money.max";
        private static final String ALLOW_DECIMALS = "money.allow-decimals";
        double min, max;
        boolean allowDecimals;

        public QMoney(@NotNull Divinity plugin, @NotNull JYML cfg) {
            super(plugin, cfg, MoneyManager.this);
            min = cfg.getDouble(MIN_MONEY, 0.0);
            max = cfg.getDouble(MAX_MONEY, 0.0);
            allowDecimals = cfg.getBoolean(ALLOW_DECIMALS);
        }

        public double getAmount() {

            return (allowDecimals) ? Rnd.getDouble(min, max) : Rnd.get((int) min, (int) max);
        }

    }

}
