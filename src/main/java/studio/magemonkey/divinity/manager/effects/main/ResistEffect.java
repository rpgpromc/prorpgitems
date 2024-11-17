package studio.magemonkey.divinity.manager.effects.main;

import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.manager.effects.IEffectType;
import studio.magemonkey.divinity.manager.effects.IExpirableEffect;

import java.util.HashMap;
import java.util.Map;

public final class ResistEffect extends IExpirableEffect {

    private Map<IEffectType, Double> resist;

    private ResistEffect(@NotNull Builder builder) {
        super(builder);
        this.resist = new HashMap<>();
    }

    @Override
    @NotNull
    public IEffectType getType() {
        return IEffectType.RESIST;
    }

    public double getResist(@NotNull IEffectType type) {
        return this.resist.getOrDefault(type, 0D);
    }

    @Override
    public boolean onTrigger(boolean force) {
        return force;
    }

    @Override
    public void onClear() {

    }

    @Override
    public boolean resetOnDeath() {
        return true;
    }

    public static class Builder extends IExpirableEffect.Builder<Builder> {

        private Map<IEffectType, Double> resist;

        public Builder(double lifeTime) {
            super(lifeTime);
            this.resist = new HashMap<>();
        }

        @NotNull
        public Builder withResist(@NotNull IEffectType type, double resist) {
            this.resist.put(type, resist);
            return this.self();
        }

        @Override
        @NotNull
        public ResistEffect build() {
            return new ResistEffect(this);
        }

        @Override
        @NotNull
        protected Builder self() {
            return this;
        }
    }
}
