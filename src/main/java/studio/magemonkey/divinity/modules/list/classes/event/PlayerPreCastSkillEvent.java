package studio.magemonkey.divinity.modules.list.classes.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.modules.list.classes.api.IAbstractSkill;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;

public class PlayerPreCastSkillEvent extends PlayerClassEvent implements Cancellable {

    private boolean cancelled;

    private IAbstractSkill skill;

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public PlayerPreCastSkillEvent(
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
