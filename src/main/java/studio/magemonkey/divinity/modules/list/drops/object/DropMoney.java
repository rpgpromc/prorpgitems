package studio.magemonkey.divinity.modules.list.drops.object;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.util.random.Rnd;

public class DropMoney extends DropNonItem {

    private double min, max;
    private boolean allowDecimals;

    public DropMoney(ConfigurationSection config) {
        super(config);

        min = config.getDouble("min-amount", 0.0);
        max = config.getDouble("max-amount", 0.0);
        allowDecimals = config.getBoolean("allow-decimals");
    }

    @Override
    public void execute(Player target) {
        if (CodexEngine.get().getVault() == null
                || CodexEngine.get().getVault().getEconomy() == null) {
            return;
        }

        CodexEngine.get()
                .getVault()
                .getEconomy()
                .depositPlayer(target, ((allowDecimals) ? Rnd.getDouble(min, max) : Rnd.get((int) min, (int) max)));
    }
}
