package studio.magemonkey.divinity.manager.interactions.api;

import lombok.Data;
import lombok.experimental.Accessors;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.manager.api.task.ITask;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AnimatedSuccessBar extends ICustomInteraction {
    private final String    title;
    private final String    barChar;
    private final String    barFormat;
    private final ChatColor barColorNeutral;
    private final ChatColor barColorGood;
    private final ChatColor barColorBad;
    private final int       barSize;

    private final long fillInterval;
    private final int  fillAmount;

    private final double                  chance;
    private final Function<Boolean, Void> result;
    private       int                     succ, unsucc;

    private AnimatedSuccessBar(@NotNull Builder builder) {
        super(builder.plugin);

        this.title = builder.barTitle;
        this.barChar = builder.barChar;
        this.barFormat = builder.barFormat;
        this.barColorNeutral = builder.colorNeutral;
        this.barColorGood = builder.colorSuccess;
        this.barColorBad = builder.colorBad;
        this.barSize = builder.barSize;
        this.fillInterval = builder.fillInterval;
        this.fillAmount = builder.fillAmount;
        this.chance = builder.chance;
        this.result = builder.result;

        this.succ = 0;
        this.unsucc = 0;
    }

    @Override
    protected boolean doAction() {
        new Task().start();
        return true;
    }

    private void display() {
        double oneFillSucc = 100D / (double) this.barSize; // 10 succ = 1 fill

        StringBuilder barBuilder = new StringBuilder();
        for (int count = 0; count < this.barSize; count++) {
            if (this.succ >= oneFillSucc * count) {
                barBuilder.append(this.barColorGood);
            } else if (this.unsucc >= (this.barSize - count) * oneFillSucc) {
                barBuilder.append(this.barColorBad);
            } else {
                barBuilder.append(this.barColorNeutral);
            }
            barBuilder.append(this.barChar);
        }

        String bar = this.barFormat
                .replace("%failure%", String.valueOf(unsucc))
                .replace("%success%", String.valueOf(succ))
                .replace("%bar%", barBuilder.toString());

        boolean isFirst = succ + unsucc == 0;
        player.sendTitle(title, bar, isFirst ? 10 : 0, (int) fillInterval + 20, 40);
    }

    @Data
    @Accessors(chain = true)
    public static class Builder implements Cloneable {
        private final Divinity plugin;

        private final String    barTitle;
        private final String    barChar;
        private       String    barFormat;
        private       int       barSize;
        private       ChatColor colorNeutral;
        private       ChatColor colorSuccess;
        private       ChatColor colorBad;

        private long fillInterval;
        private int  fillAmount;

        private double chance;

        private Function<Boolean, Void> result;

        public Builder(@NotNull Divinity plugin, @NotNull String title, @NotNull String barChar) {
            this.plugin = plugin;
            this.barTitle = StringUT.color(title);
            this.barChar = StringUT.color(barChar);
            this.setBarFormat("%bar%");
            this.setColorNeutral(ChatColor.DARK_GRAY);
            this.setColorSuccess(ChatColor.GREEN);
            this.setColorBad(ChatColor.RED);
            this.setBarSize(20);
            this.setFillInterval(1);
            this.setFillAmount(1);
            this.setChance(50);
            this.setResult(b -> null);
        }

        @NotNull
        public Builder setBarFormat(@NotNull String format) {
            this.barFormat = StringUT.color(format);
            return this;
        }

        @NotNull
        @Override
        @SuppressWarnings("MethodDoesntCallSuperMethod")
        public Builder clone() {
            Builder clone = new Builder(plugin, barTitle, barChar);
            clone.barFormat = barFormat;
            clone.colorNeutral = colorNeutral;
            clone.colorSuccess = colorSuccess;
            clone.colorBad = colorBad;
            clone.barSize = barSize;
            clone.fillInterval = fillInterval;
            clone.fillAmount = fillAmount;
            clone.chance = chance;
            clone.result = result;
            return clone;
        }

        @NotNull
        public AnimatedSuccessBar build() {
            return new AnimatedSuccessBar(this);
        }
    }

    class Task extends ITask<Divinity> {
        private final List<Boolean> mappedResult = new ArrayList<>();

        Task() {
            super(AnimatedSuccessBar.this.plugin, AnimatedSuccessBar.this.fillInterval, true);
            int calculatedResult = Math.round(Rnd.get(true));
            int iterations       = (int) Math.ceil(100D / fillAmount);
            // Map the iteration to the relative success for the result
            for (int i = 0; i < iterations; i++) {
                mappedResult.add(i * fillAmount < calculatedResult);
            }
        }

        @Override
        public void action() {
            if (player == null || player.isDead()) {
                this.stop();
                return;
            }

            display();

            if (succ + unsucc >= 100) {
                plugin.getServer().getScheduler()
                        .runTask(plugin, () -> result.apply(succ >= 100 - chance));
                endAction();
                this.stop();
                return;
            }

            // Get random item from the mapped results
            int     index  = Rnd.get(0, mappedResult.size() - 1);
            boolean result = mappedResult.get(index);
            mappedResult.remove(index);

            if (result) succ += fillAmount;
            else unsucc += fillAmount;
        }
    }
}
