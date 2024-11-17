package studio.magemonkey.divinity.modules.list.classes.event;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.modules.list.classes.api.IAbstractSkill;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;

public class PlayerCastSkillEvent extends PlayerClassEvent {

    private IAbstractSkill skill;

    public PlayerCastSkillEvent(
            @NotNull Player player,
            @NotNull UserClassData data,
            @NotNull IAbstractSkill skill
    ) {
        super(player, data);
        this.skill = skill;
    }

    @NotNull
    public IAbstractSkill getSkill() {
        return this.skill;
    }
}
