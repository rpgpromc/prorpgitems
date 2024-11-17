package studio.magemonkey.divinity.stats;

import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.manager.api.task.ITask;
import studio.magemonkey.divinity.Divinity;

import java.util.HashSet;

public class EntityStatsTask extends ITask<Divinity> {

    public static final int POTION_DURATION = 140; // TODO Config option
    private             int count;

    public EntityStatsTask(@NotNull Divinity plugin) {
        super(plugin, 1L, false);
        this.count = 1;
    }

    @Override
    public void action() {
        for (EntityStats stats : new HashSet<>(EntityStats.getAll())) {
            if (this.count % 2 == 0) {
                stats.updateAttackPower();
            }
			/*if (EngineCfg.PERFORMACE_STATS_BONUS_UPDATE_INTERVAL > 0) {
				if (this.count % EngineCfg.PERFORMACE_STATS_BONUS_UPDATE_INTERVAL == 0) {
					es.updateBonus();
				}
			}*/
            if (this.count % 100 == 0) {
                stats.triggerVisualEffects();
            }
            if (this.count % (POTION_DURATION - 40) == 0) {
                stats.triggerPotionEffects();
            }
            stats.triggerEffects();
        }

        if (this.count++ >= 100) {
            this.count = 1;
        }
    }

}
