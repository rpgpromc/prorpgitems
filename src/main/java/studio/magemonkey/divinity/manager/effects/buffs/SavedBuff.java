package studio.magemonkey.divinity.manager.effects.buffs;

import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.stats.items.api.ItemLoreStat;

public class SavedBuff {

    private String  stat;
    private double  amount;
    private boolean isModifier;
    private long    expireTime;

    public SavedBuff(@NotNull ItemLoreStat<?> stat, double amount, boolean isModifier, int duration) {
        this(stat.getId(), amount, isModifier, System.currentTimeMillis() + 1000L * duration);
    }

    public SavedBuff(@NotNull String stat, double amount, boolean isModifier, long expireTime) {
        this.stat = stat;
        this.amount = amount;
        this.isModifier = isModifier;
        this.expireTime = expireTime;
    }

    @NotNull
    public String getStatId() {
        return this.stat;
    }

    public double getAmount() {
        return this.amount;
    }

    public boolean isModifier() {
        return this.isModifier;
    }

    public long getExpireTime() {
        return this.expireTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > this.expireTime;
    }
}
