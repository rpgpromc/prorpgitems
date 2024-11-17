package studio.magemonkey.divinity.modules.list.drops.object;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.config.EngineCfg;

public class DropExperience extends DropNonItem {
    private final double chance;
    private final int    min, max;

    public DropExperience(ConfigurationSection config) {
        super(config);
        this.chance = config.getDouble("chance", 100);
        this.min = config.getInt("min-amount", 1);
        this.max = config.getInt("max-amount", 1);
    }

    @Override
    public void execute(Player target) {
        if (Rnd.chance(chance)) {
            EngineCfg.HOOK_PLAYER_LEVEL_PLUGIN.giveExp(target, Rnd.get(min, max));
        }
    }
}
