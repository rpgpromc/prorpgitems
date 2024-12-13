package studio.magemonkey.divinity.modules.list.classes.object;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.NumberUT;

@Getter
@Setter
public class ClassAttribute {
    private double startValue;
    private double maxValue;
    private double perLevelValue;

    public ClassAttribute(
            double startValue,
            double maxValue,
            double perLevelValue
    ) {
        this.setStartValue(startValue);
        this.setMaxValue(maxValue);
        this.setPerLevelValue(perLevelValue);
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
