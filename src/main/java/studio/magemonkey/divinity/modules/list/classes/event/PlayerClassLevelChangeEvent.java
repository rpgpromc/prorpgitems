package studio.magemonkey.divinity.modules.list.classes.event;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;

public class PlayerClassLevelChangeEvent extends PlayerClassEvent {

    private int lvlNew;

    public PlayerClassLevelChangeEvent(
            @NotNull Player player,
            @NotNull UserClassData data,
            int lvlNew
    ) {
        super(player, data);
        this.lvlNew = lvlNew;
    }

    public int getNewLevel() {
        return this.lvlNew;
    }
}
