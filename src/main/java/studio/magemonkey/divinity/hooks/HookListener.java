package studio.magemonkey.divinity.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginEnableEvent;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.hooks.external.*;
import studio.magemonkey.divinity.hooks.external.mythicmobs.MythicMobsHK;
import studio.magemonkey.divinity.hooks.external.mythicmobs.MythicMobsHKv5;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;
import studio.magemonkey.divinity.modules.list.party.PartyManager;

public class HookListener extends IListener<Divinity> {

    public HookListener(@NotNull Divinity plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        switch (event.getPlugin().getName()) {
            case "LevelledMobs": {
                this.plugin.registerHook(EHook.LEVELLED_MOBS, LevelledMobsHK.class);
                break;
            }
            case "LorinthsRpgMobs": {
                this.plugin.registerHook(EHook.LORINTHS_RPG_MOBS, LorinthsRpgMobsHK.class);
                break;
            }
            case "Magic": {
                this.plugin.registerHook(EHook.MAGIC, MagicHK.class);
                break;
            }
            case "mcMMO": {
                this.plugin.registerHook(EHook.MCMMO, McmmoHK.class);
                break;
            }
            case "MythicMobs": {
                boolean mythic4 = true;
                try {
                    Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
                } catch (ClassNotFoundException classNotFoundException) {
                    mythic4 = false;
                }
                if (mythic4) {
                    this.plugin.registerHook(Hooks.MYTHIC_MOBS, MythicMobsHK.class);
                } else {
                    this.plugin.registerHook(Hooks.MYTHIC_MOBS, MythicMobsHKv5.class);
                }
                break;
            }
            case "PlaceholderAPI": {
                this.plugin.registerHook(Hooks.PLACEHOLDER_API, PlaceholderAPIHK.class);
                break;
            }
            case "PwingRaces": {
                this.plugin.registerHook(EHook.PWING_RACES, PwingRacesHK.class);
                break;
            }
            case "Fabled": {
                this.plugin.registerHook(EHook.SKILL_API, FabledHook.class);
                this.plugin.setConfig();
                PartyManager partyManager = this.plugin.getModuleManager().getModule(PartyManager.class);
                if (partyManager != null) {
                    partyManager.reload();
                }
                FabledHook fabledHook = (FabledHook) this.plugin.getHook(EHook.SKILL_API);
                if (fabledHook != null) {
                    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                        fabledHook.updateSkills(player);
                    }
                }
                ItemGeneratorManager itemgen = Divinity.getInstance()
                        .getModuleManager()
                        .getModule(ItemGeneratorManager.class);
                if (itemgen != null) itemgen.reload(); // Load missed Fabled attributes
                break;
            }
        }
    }
}
