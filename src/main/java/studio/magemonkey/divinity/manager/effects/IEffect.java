package studio.magemonkey.divinity.manager.effects;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.stats.EntityStats;

import java.util.HashMap;
import java.util.Map;

public abstract class IEffect {

    protected LivingEntity                        caster;
    protected LivingEntity                        target;
    protected Map<PotionEffectType, PotionEffect> potions;
    protected int                                 charges;
    protected double                              threshold;

    protected String msgApply;
    protected String msgExpire;
    protected String msgReject;

    protected IEffect(@NotNull Builder<?> builder) {
        this.caster = builder.caster;
        this.potions = new HashMap<>(builder.potions);
        this.charges = builder.charges;
        this.threshold = builder.threshold;

        this.msgApply = builder.msgApply;
        this.msgExpire = builder.msgExpire;
        this.msgReject = builder.msgReject;
    }

    public IEffect(@Nullable LivingEntity caster) {
        this(caster, 1);
    }

    public IEffect(@Nullable LivingEntity caster, int charges) {

    }

    public double getThreshold() {
        return this.threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void setApplyMessage(@NotNull String msg) {
        this.msgApply = StringUT.color(msg);
    }

    public void setExpireMessage(@NotNull String msg) {
        this.msgExpire = StringUT.color(msg);
    }

    public void setRejectMessage(@NotNull String msg) {
        this.msgReject = StringUT.color(msg);
    }

    public boolean applyTo(@NotNull LivingEntity e) {
        if (e.isDead() || !e.isValid()) return false;

        this.target = e;
        EntityStats es = EntityStats.get(e);

        double resist = es.getEffectResist(this.getType(), false);
        if (resist > this.getThreshold()) {
            if (this.caster != null && this.msgReject != null) {
                this.caster.sendMessage(this.msgReject);
            }
            return false;
        }

        es.addEffect(this);
        if (this.msgApply != null) {
            e.sendMessage(this.msgApply);
        }
        return true;
    }

    @NotNull
    public abstract IEffectType getType();

    public boolean isPositive() {
        return this.getType().isPositive();
    }

    public boolean isType(@NotNull IEffectType type) {
        return type == this.getType();
    }

    @Nullable
    public LivingEntity getCaster() {
        return this.caster;
    }

    @NotNull
    public LivingEntity getTarget() {
        return this.target;
    }

    public final int getCharges() {
        return this.charges;
    }

    public final int takeCharge() {
        return --this.charges;
    }

    public void trigger(boolean force) {
        if (force || this.onTrigger(force)) {
            if (this.getCharges() > 0) this.takeCharge();
        }
    }

    public void clear() {
        this.onClear();
    }

    public boolean isExpired() {
        return this.getCharges() == 0;
    }

    protected abstract boolean onTrigger(boolean force);

    protected abstract void onClear();

    public abstract boolean resetOnDeath();

    protected final void applyPotionEffects() {
        for (PotionEffect p : this.potions.values()) {
            PotionEffect has = target.getPotionEffect(p.getType());
            if (has != null && has.getAmplifier() > p.getAmplifier()) continue;

            this.target.addPotionEffect(p);
        }
    }

    protected final void removePotionEffects() {
        for (PotionEffect p : this.potions.values()) {
            PotionEffect has = target.getPotionEffect(p.getType());
            if (has != null && has.getAmplifier() != p.getAmplifier()) continue;

            target.removePotionEffect(p.getType());
        }
    }

    abstract static class Builder<B extends Builder<B>> {

        private LivingEntity                        caster;
        private Map<PotionEffectType, PotionEffect> potions;
        private int                                 charges;
        private double                              threshold;

        private String msgApply;
        private String msgExpire;
        private String msgReject;

        public Builder() {
            this.caster = null;
            this.potions = new HashMap<>();
            this.charges = -1;
            this.threshold = 0D;
        }

        @NotNull
        public B withCaster(@Nullable LivingEntity caster) {
            this.caster = caster;
            return this.self();
        }

        @NotNull
        public B withCharges(int charges) {
            this.charges = charges;
            return this.self();
        }

        @NotNull
        public B withThreshold(double threshold) {
            this.threshold = threshold;
            return this.self();
        }

        @NotNull
        public B setApplyMessage(@NotNull String message) {
            this.msgApply = StringUT.color(message);
            return this.self();
        }

        @NotNull
        public B setExpireMessage(@NotNull String message) {
            this.msgExpire = StringUT.color(message);
            return this.self();
        }

        @NotNull
        public B setRejectMessage(@NotNull String message) {
            this.msgReject = StringUT.color(message);
            return this.self();
        }

        @NotNull
        public B addPotionEffects(@NotNull PotionEffect... pe) {
            for (PotionEffect effect : pe) {
                this.potions.putIfAbsent(effect.getType(), effect);
            }
            return this.self();
        }

        @NotNull
        public abstract IEffect build();

        @NotNull
        protected abstract B self();
    }
}
