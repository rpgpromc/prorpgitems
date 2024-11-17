package studio.magemonkey.divinity.modules.list.party.compat.quest;

import me.Cutiemango.MangoQuest.Main;
import me.Cutiemango.MangoQuest.QuestUtil;
import me.Cutiemango.MangoQuest.data.QuestPlayerData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.party.PartyManager;
import studio.magemonkey.divinity.modules.list.party.PartyManager.PartyMember;

import java.util.Set;

public class IPOMangoQuest extends IPartyObjective {

    public IPOMangoQuest(@NotNull Divinity plugin, @NotNull PartyManager partyManager) {
        super(plugin, partyManager);
    }

    @Override
    public void progressObjective(
            @NotNull Player exec, @NotNull Set<PartyMember> objGeters, @NotNull Event e) {

        if (e instanceof EntityDeathEvent) {
            this.progressMobKill(exec, objGeters, (EntityDeathEvent) e);
        }
    }

    private void progressMobKill(
            @NotNull Player exec, @NotNull Set<PartyMember> objGeters, @NotNull EntityDeathEvent e) {

        LivingEntity entity = e.getEntity();

        for (PartyMember mm : objGeters) {
            QuestPlayerData playerData = QuestUtil.getData(mm.getPlayer());
            if (playerData != null) {
                if (Main.getHooker().hasMythicMobEnabled()) {
                    if (Main.getHooker().getMythicMobsAPI().isMythicMob(entity)) {
                        String type = Main.getHooker()
                                .getMythicMobsAPI()
                                .getMythicMobInstance(entity)
                                .getType()
                                .getInternalName();
                        playerData.killMythicMob(type);
                        return;
                    }
                }
                playerData.killMob(entity);
            }
        }
    }
}
