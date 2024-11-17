package studio.magemonkey.divinity.modules.list.classes.object;

import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.NumberUT;

public class ClassAttribute {

    private double startValue;
    private double maxValue;
    private double lvlValue;

    public ClassAttribute(
            double startValue,
            double maxValue,
            double lvlValue
    ) {
        this.setStartValue(startValue);
        this.setMaxValue(maxValue);
        this.setPerLevelValue(lvlValue);
    }

    public double getStartValue() {
        return this.startValue;
    }

    public void setStartValue(double startValue) {
        this.startValue = startValue;
    }

    public double getMaxValue() {
        return this.maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public double getPerLevelValue() {
        return this.lvlValue;
    }

    public void setPerLevelValue(double lvlValue) {
        this.lvlValue = lvlValue;
    }

    @NotNull
    public String replace(@NotNull ClassAttributeType qa, @NotNull String line, double cur, double aspect, int lvl) {
        String plName   = "%att_name_" + qa.name() + "%";
        String plStart  = "%att_start_" + qa.name() + "%";
        String plLvl    = "%att_lvl_" + qa.name() + "%";
        String plTotal  = "%att_total_" + qa.name() + "%";
        String plAspect = "%att_aspect_" + qa.name() + "%";
        String d        = NumberUT.format(cur);

        return line
                .replace(plAspect, NumberUT.format(aspect))
                .replace(plTotal, d)
                .replace(plName, qa.getName())
                .replace(plLvl, NumberUT.format(this.getPerLevelValue() * lvl))
                .replace(plStart, NumberUT.format(this.getStartValue()));
    }
}
