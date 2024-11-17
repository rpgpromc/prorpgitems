package studio.magemonkey.divinity.hooks.external;

import net.pwing.races.api.PwingRacesAPI;
import net.pwing.races.api.race.Race;
import net.pwing.races.api.race.RaceData;
import net.pwing.races.api.race.RaceManager;
import net.pwing.races.api.race.RacePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.hooks.HookState;
import studio.magemonkey.codex.hooks.NHook;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.hooks.HookClass;
import studio.magemonkey.divinity.hooks.HookLevel;

public class PwingRacesHK extends NHook<Divinity> implements HookClass, HookLevel {

    public PwingRacesHK(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public HookState setup() {
        return HookState.SUCCESS;
    }

    @Override
    public void shutdown() {

    }

    @Override
    @NotNull
    public String getClass(@NotNull Player player) {
        RaceManager raceManager = PwingRacesAPI.getRaceManager();
        RacePlayer  racePlayer  = raceManager.getRacePlayer(player);
        return racePlayer.getActiveRace().getName();
    }

    @Override
    public void takeMana(@NotNull Player player, double amount, boolean ofMax) {
        throw new UnsupportedOperationException("Your class plugin does not provides mana function.");
    }

    @Override
    public int getLevel(@NotNull Player player) {
        RaceManager raceManager = PwingRacesAPI.getRaceManager();
        RacePlayer  racePlayer  = raceManager.getRacePlayer(player);
        Race        race        = racePlayer.getActiveRace();

        return racePlayer.getRaceData(race).getLevel();
    }

    @Override
    public void giveExp(@NotNull Player player, int amount) {
        RaceManager raceManager = PwingRacesAPI.getRaceManager();
        RacePlayer  racePlayer  = raceManager.getRacePlayer(player);
        Race        race        = racePlayer.getActiveRace();
        RaceData    raceData    = racePlayer.getRaceData(race);
        raceData.setExperience(raceData.getExperience() + amount);
    }
}
