package studio.magemonkey.divinity.modules.list.classes.object;

import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.VersionManager;
import studio.magemonkey.codex.util.StringUT;

public enum ClassAttributeType {
    ARMOR(0D),
    ARMOR_TOUGHNESS(0D),
    ATTACK_DAMAGE(1D),
    ATTACK_SPEED(4D),
    FLYING_SPEED(0.4D),
    KNOCKBACK_RESISTANCE(0D),
    LUCK(0D),
    MAX_HEALTH(20D),
    MOVEMENT_SPEED(0.1D),
    ;

    private Attribute att;
    private String    name;
    private double    defValue;

    ClassAttributeType(double def) {
        this.att = VersionManager.getNms().getAttribute(this.name());
        this.name = this.name();
        this.defValue = def;
    }

    @NotNull
    public Attribute getVanillaAttribute() {
        return this.att;
    }

    public double getDefaultValue() {
        return this.defValue;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull String name) {
        this.name = StringUT.color(name);
    }
}
