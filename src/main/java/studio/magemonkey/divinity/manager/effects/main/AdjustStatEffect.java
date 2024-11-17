package studio.magemonkey.divinity.manager.effects.main;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.divinity.manager.effects.IEffectType;
import studio.magemonkey.divinity.manager.effects.IExpirableEffect;
import studio.magemonkey.divinity.stats.items.api.ItemLoreStat;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

public class AdjustStatEffect extends IExpirableEffect {

    private final Map<ItemLoreStat<?>, Integer>                       count;
    private       Map<? extends ItemLoreStat<?>, DoubleUnaryOperator> functions;

    private AdjustStatEffect(@NotNull Builder builder) {
        super(builder);
        this.functions = new HashMap<>(builder.functions);
        this.count = new HashMap<>();
    }

    @Nullable
    public DoubleUnaryOperator getAdjust(@NotNull ItemLoreStat<?> stat, boolean safe) {
        DoubleUnaryOperator operator = this.functions.get(stat);
        if (operator == null) return null;

        // Check how many times we got the adjust effect compare to effect charges
        // so we remove that stat if adjust effect triggered more than charges amount aka expired.
        if (!safe) {
            this.count.putIfAbsent(stat, 0);
            int count = this.count.compute(stat, (k, v) -> v + 1);
            //If this has been used more than the number of charges, remove it
            if (count >= this.getCharges()) {
                this.count.remove(stat);
                this.functions.remove(stat);
            }
        }
        return operator;
    }

    @NotNull
    public Set<? extends ItemLoreStat<?>> getAdjustedStats() {
        return this.functions.keySet();
    }

    public boolean isAdjusted(@NotNull ItemLoreStat<?> stat) {
        return this.functions.containsKey(stat);
    }

    @Override
    public boolean onTrigger(boolean force) {
        return this.functions.isEmpty();
    }

    @Override
    public void onClear() {
        if (this.functions != null) {
            this.functions.clear();
            this.functions = null;
        }
    }

    @Override
    public boolean resetOnDeath() {
        return true;
    }

    @Override
    @NotNull
    public IEffectType getType() {
        return IEffectType.ADJUST_STAT;
    }

    public static class Builder extends IExpirableEffect.Builder<Builder> {

        private final Map<ItemLoreStat<?>, DoubleUnaryOperator> functions;

        public Builder(double lifeTime) {
            super(lifeTime);
            this.functions = new HashMap<>();
        }

        @SuppressWarnings("unchecked")
        @NotNull
        public Builder withAdjust(@NotNull ItemLoreStat<?> stat, @NotNull DoubleUnaryOperator operator) {
            return this.withAdjust(Sets.newHashSet(stat), operator);
        }

        @NotNull
        public Builder withAdjust(@NotNull Collection<? extends ItemLoreStat<?>> stats,
                                  @NotNull DoubleUnaryOperator operator) {
            stats.forEach(stat -> this.functions.put(stat, operator));
            return this.self();
        }

        @Override
        @NotNull
        public AdjustStatEffect build() {
            return new AdjustStatEffect(this);
        }

        @Override
        @NotNull
        protected Builder self() {
            return this;
        }
    }
}
