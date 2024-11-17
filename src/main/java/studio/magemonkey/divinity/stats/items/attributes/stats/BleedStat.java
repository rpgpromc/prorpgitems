package studio.magemonkey.divinity.stats.items.attributes.stats;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.codex.util.eval.Evaluator;
import studio.magemonkey.divinity.manager.effects.main.BleedEffect;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;

import java.util.function.Function;

public class BleedStat extends SimpleStat {

    private String  damageFormula;
    private boolean ofMaxHealth;
    private double  duration;

    public BleedStat(
            @NotNull String name,
            @NotNull String format,
            double cap,

            @NotNull String damageFormula,
            boolean ofMaxHealth,
            double duration
    ) {

        super(TypedStat.Type.BLEED_RATE, name, format, cap);
        this.damageFormula = damageFormula;
        this.ofMaxHealth = ofMaxHealth;
        this.duration = duration;
    }

    @NotNull
    public String getDamageFormula() {
        return this.damageFormula;
    }

    public boolean damageOfMaxHealth() {
        return this.ofMaxHealth;
    }

    public double getDuration() {
        return this.duration;
    }

    public void bleed(@NotNull LivingEntity target, double damage) {
        double dmgTick = Evaluator.eval(damageFormula.replace("%damage%", NumberUT.format(damage)), 1);

        Function<LivingEntity, Double> dmgFunc = (entity) -> {
            if (this.ofMaxHealth) {
                return EntityStats.getEntityMaxHealth(target) * dmgTick / 100D;
            }
            return dmgTick;
        };

        BleedEffect bleed = new BleedEffect.Builder(duration, 1.25, dmgFunc).build();
        bleed.applyTo(target);
    }
}
