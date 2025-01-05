package studio.magemonkey.divinity;

import lombok.Getter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.CodexDataPlugin;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.commands.api.IGeneralCommand;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.migration.MigrationUtil;
import studio.magemonkey.codex.registry.DamageRegistry;
import studio.magemonkey.divinity.command.BuffCommand;
import studio.magemonkey.divinity.command.ModifyCommand;
import studio.magemonkey.divinity.command.SetCommand;
import studio.magemonkey.divinity.config.Config;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.config.Lang;
import studio.magemonkey.divinity.data.DivinityUserData;
import studio.magemonkey.divinity.data.UserManager;
import studio.magemonkey.divinity.data.api.DivinityUser;
import studio.magemonkey.divinity.hooks.EHook;
import studio.magemonkey.divinity.hooks.external.*;
import studio.magemonkey.divinity.hooks.external.mimic.MimicHook;
import studio.magemonkey.divinity.hooks.external.mythicmobs.MythicMobsHK;
import studio.magemonkey.divinity.hooks.external.mythicmobs.MythicMobsHKv5;
import studio.magemonkey.divinity.manager.EntityManager;
import studio.magemonkey.divinity.manager.damage.DamageManager;
import studio.magemonkey.divinity.manager.interactions.InteractionManager;
import studio.magemonkey.divinity.manager.listener.ListenerManager;
import studio.magemonkey.divinity.manager.profile.ProfileManager;
import studio.magemonkey.divinity.manager.worth.WorthManager;
import studio.magemonkey.divinity.modules.ModuleCache;
import studio.magemonkey.divinity.nms.engine.PMSManager;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.requirements.ItemRequirements;
import studio.magemonkey.divinity.utils.DivinityProvider;
import studio.magemonkey.divinity.utils.actions.conditions.CEntityLevel;
import studio.magemonkey.divinity.utils.actions.executors.ActionDamage;
import studio.magemonkey.divinity.utils.actions.executors.ActionParticleLine;
import studio.magemonkey.divinity.utils.actions.executors.ActionParticlePulse;
import studio.magemonkey.divinity.utils.actions.executors.ActionTakeMana;
import studio.magemonkey.divinity.utils.actions.params.AttackableParam;
import studio.magemonkey.divinity.utils.actions.params.PartyMemberParam;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Divinity
 *
 * @author ©2024 MageMonkeyStudio
 */
public class Divinity extends CodexDataPlugin<Divinity, DivinityUser> {

    @Getter
    public static Divinity instance;

    private Config    config;
    private Lang      lang;
    private EngineCfg engineCfg;

    private DivinityUserData dataHandler;

    private InteractionManager interactionManager;
    private WorthManager       worthManager;
    private DamageManager      dmgManager;
    private EntityManager      entityManager;
    private ListenerManager    listenerManager;
    private ProfileManager     profileManager;
    private ModuleCache        moduleCache;

    private PMSManager pms;

    public Divinity() {
        super();
        instance = this;
        MigrationUtil.renameDirectory("plugins/ProRPGItems", "plugins/Divinity");
    }

    public Divinity(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        instance = this;
        MigrationUtil.renameDirectory("plugins/ProRPGItems", "plugins/Divinity");
    }

    @Override
    public void onLoad() {
        MimicHook.hook(this);
    }

    @Override
    public void enable() {
        this.addCustomActions();

        this.pms = new PMSManager(this);
        this.pms.setup();

        String coreVersion = CodexEngine.getEngine().getDescription().getVersion();
        boolean minCoreVersionMet =
                DependencyRequirement.meetsVersion(DependencyRequirement.MIN_CORE_VERSION, coreVersion);

        boolean useProfiles;
        try {
            JYML profileConfig = JYML.loadOrExtract(this, "/profiles/settings.yml");
            useProfiles = profileConfig.getBoolean("enabled", true);
        } catch (InvalidConfigurationException e) {
            this.error("Failed to load profile settings (" + this.getName()
                    + "/profiles/settings.yml): Configuration error");
            this.error("Disabling profiles...");
            e.printStackTrace();
            useProfiles = false;

        }

        if (!minCoreVersionMet) {
            warn("Missing required Codex version. " + coreVersion + " installed. "
                    + DependencyRequirement.MIN_CORE_VERSION + " required. Disabling.");
            this.getPluginManager().disablePlugin(this);
            return;
        }

        this.interactionManager = new InteractionManager(this);
        this.interactionManager.setup();

        this.dmgManager = new DamageManager(this);
        this.dmgManager.setup();

        this.entityManager = new EntityManager(this);
        this.entityManager.setup();

        this.listenerManager = new ListenerManager(this);
        this.listenerManager.setup();

        if (useProfiles) {
            this.profileManager = new ProfileManager(this);
            this.profileManager.setup();
        }

        this.moduleCache = new ModuleCache(this);
        this.moduleCache.initialize();

        this.worthManager = new WorthManager(this);
        this.worthManager.setup();

        CodexEngine.getEngine()
                .getItemManager()
                .registerProvider(DivinityProvider.NAMESPACE, new DivinityProvider());
        DamageRegistry.registerProvider(dmgManager);
    }

    @Override
    public void disable() {
        DamageRegistry.unregisterProvider(DamageManager.class);
        if (this.interactionManager != null) {
            this.interactionManager.shutdown();
            this.interactionManager = null;
        }
        if (this.worthManager != null) {
            this.worthManager.shutdown();
            this.worthManager = null;
        }

        this.dmgManager.shutdown();
        this.entityManager.shutdown();
        this.listenerManager.shutdown();
        this.moduleCache.shutdown();

        if (this.profileManager != null) {
            this.profileManager.shutdown();
            this.profileManager = null;
        }

        this.pms.shutdown();

        ItemStats.clear();
        ItemRequirements.clear();
        List<EntityStats> toRemove = new ArrayList<>(EntityStats.getAll());
        toRemove.forEach(EntityStats::purge);

        DivinityUserData.destroy();

        CodexEngine.get().getItemManager().unregisterProvider(DivinityProvider.class);
    }

    @Override
    public void setConfig() {
        this.config = new Config(this);
        this.config.setup();

        try {
            this.engineCfg = new EngineCfg(this);
            this.engineCfg.setup();
        } catch (InvalidConfigurationException e) {
            this.error("Failed to load engine config (" + this.getName() + "/engine.yml): Configuration error");
            this.error("Disabling...");
            e.printStackTrace();
            this.getPluginManager().disablePlugin(this);
            return;
        }

        this.lang = new Lang(this);
        this.lang.setup();

        this.cfg().setupAttributes();

        this.cfg().save();
    }

    @Override
    public void registerHooks() {
        this.registerHook(EHook.CRACK_SHOT, CrackShotHK.class);
        this.registerHook(EHook.LEVELLED_MOBS, LevelledMobsHK.class);
        this.registerHook(EHook.LORINTHS_RPG_MOBS, LorinthsRpgMobsHK.class);
        this.registerHook(EHook.MAGIC, MagicHK.class);
        this.registerHook(EHook.MCMMO, McmmoHK.class);

        boolean mythic4 = true;
        try {
            Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
        } catch (ClassNotFoundException classNotFoundException) {
            mythic4 = false;
        }
        if (mythic4)
            this.registerHook(Hooks.MYTHIC_MOBS, MythicMobsHK.class);
        else
            this.registerHook(Hooks.MYTHIC_MOBS, MythicMobsHKv5.class);

        this.registerHook(EHook.MY_PET, MyPetHK.class);
        if (Hooks.hasPlaceholderAPI()) {
            this.registerHook(Hooks.PLACEHOLDER_API, PlaceholderAPIHK.class);
        }
        this.registerHook(EHook.PWING_RACES, PwingRacesHK.class);
        this.registerHook(EHook.SKILL_API, FabledHook.class);

//		this.registerHook(EHook.RACES_OF_THANA, RacesOfThanaHK.class);
//		this.registerHook(EHook.SKILLS, SkillsProHK.class);

        // Hooks also loaded dynamically by HookListener
    }

    @Override
    public void registerCommands(@NotNull IGeneralCommand<Divinity> mainCommand) {
        mainCommand.addSubCommand(new ModifyCommand(this));
        mainCommand.addSubCommand(new SetCommand(this));
        mainCommand.addSubCommand(new BuffCommand(this));
    }

    @Override
    public void registerEditor() {

    }

    @Override
    @NotNull
    public Config cfg() {
        return this.config;
    }

    @Override
    @NotNull
    public Lang lang() {
        return this.lang;
    }

    @Override
    protected boolean setupDataHandlers() {
        try {
            this.dataHandler = DivinityUserData.getInstance(this);
            this.dataHandler.setup();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }

        this.userManager = new UserManager(this);
        this.userManager.setup();

        return true;
    }

    @Override
    public DivinityUserData getData() {
        return this.dataHandler;
    }

    private void addCustomActions() {
        this.getActionsManager().registerParam(new PartyMemberParam());
        this.getActionsManager().registerParam(new AttackableParam());

        this.getActionsManager().registerCondition(new CEntityLevel(this));

        this.getActionsManager().registerExecutor(new ActionDamage(this));
        this.getActionsManager().registerExecutor(new ActionParticleLine(this));
        this.getActionsManager().registerExecutor(new ActionParticlePulse(this));
        this.getActionsManager().registerExecutor(new ActionTakeMana(this));
    }

    @NotNull
    public InteractionManager getInteractionManager() {
        return interactionManager;
    }

    @NotNull
    public ProfileManager getProfileManager() {
        return profileManager;
    }

    @NotNull
    public WorthManager getWorthManager() {
        return worthManager;
    }

    @NotNull
    public ModuleCache getModuleCache() {
        return moduleCache;
    }
}
