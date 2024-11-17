package studio.magemonkey.divinity.manager.profile;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.codex.manager.api.Loadable;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.data.api.DivinityUser;
import studio.magemonkey.divinity.data.api.UserProfile;
import studio.magemonkey.divinity.stats.EntityStats;

import java.util.*;
import java.util.regex.Pattern;

public class ProfileManager extends IListener<Divinity> implements Loadable {

    private JYML cfg;

    private boolean              selectOnJoin;
    private int                  changeCooldown;
    private Pattern              namePattern;
    private Map<String, Integer> profilesAmount;

    private SettingsGUI    guiSettings;
    private ProfileGUI     guiProfile;
    private ProfilesGUI    guiProfiles;
    private ProfileCommand profileCommand;

    private Map<String, Long> profileCooldown;
    private Set<Player>       profileCreation;

    public ProfileManager(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    public void setup() {
        try {
            this.cfg = JYML.loadOrExtract(plugin, "/profiles/settings.yml");
        } catch (InvalidConfigurationException e) {
            this.plugin.error("Failed to load profiles config (" + this.plugin.getName()
                    + "/profiles/settings.yml): Configuration error");
            e.printStackTrace();
            shutdown();
            return;
        }

        this.selectOnJoin = cfg.getBoolean("profile.select-on-join", false);
        this.changeCooldown = cfg.getInt("profile.change-cooldown");
        this.namePattern = Pattern.compile(cfg.getString("profile.name-regex", "[a-zA-Z0-9_]*"));

        this.profilesAmount = new HashMap<>();
        for (String rank : cfg.getSection("profiles-amount")) {
            int amount = cfg.getInt("profiles-amount." + rank);
            this.profilesAmount.put(rank.toLowerCase(), amount);
        }

        if (this.getChangeCooldown() > 0) {
            this.profileCooldown = new HashMap<>();
        }
        this.profileCreation = Collections.newSetFromMap(new WeakHashMap<>());

        try {
            this.guiSettings = new SettingsGUI(this, JYML.loadOrExtract(plugin, "/profiles/gui.settings.yml"));
        } catch (InvalidConfigurationException e) {
            this.plugin.error("Failed to load profiles config (" + this.plugin.getName()
                    + "/profiles/gui.settings.yml): Configuration error");
            e.printStackTrace();
            shutdown();
            return;
        }
        try {
            this.guiProfile = new ProfileGUI(this, JYML.loadOrExtract(plugin, "/profiles/gui.profile.yml"));
        } catch (InvalidConfigurationException e) {
            this.plugin.error("Failed to load profiles config (" + this.plugin.getName()
                    + "/profiles/gui.profile.yml): Configuration error");
            e.printStackTrace();
            shutdown();
            return;
        }
        try {
            this.guiProfiles = new ProfilesGUI(this, JYML.loadOrExtract(plugin, "/profiles/gui.profiles.yml"));
        } catch (InvalidConfigurationException e) {
            this.plugin.error("Failed to load profiles config (" + this.plugin.getName()
                    + "/profiles/gui.profiles.yml): Configuration error");
            e.printStackTrace();
            shutdown();
            return;
        }

        this.plugin.getCommandManager().registerCommand(this.profileCommand = new ProfileCommand(this));

        this.registerListeners();
        this.plugin.getLogger().info("Profile Manager has been enabled");
    }

    @Override
    public void shutdown() {
        this.unregisterListeners();

        if (this.guiSettings != null) {
            this.guiSettings.shutdown();
            this.guiSettings = null;
        }
        if (this.guiProfile != null) {
            this.guiProfile.shutdown();
            this.guiProfile = null;
        }
        if (this.guiProfiles != null) {
            this.guiProfiles.shutdown();
            this.guiProfiles = null;
        }
        if (this.profilesAmount != null) {
            this.profilesAmount.clear();
            this.profilesAmount = null;
        }
        if (this.profileCooldown != null) {
            this.profileCooldown.clear();
            this.profileCooldown = null;
        }
        if (this.profileCreation != null) {
            this.profileCreation.clear();
            this.profileCreation = null;
        }
        if (this.profileCommand != null) {
            this.plugin.getCommandManager().unregisterCommand(this.profileCommand);
            this.profileCommand = null;
        }
    }

    public boolean isSelectOnJoin() {
        return this.selectOnJoin;
    }

    public int getChangeCooldown() {
        return this.changeCooldown;
    }

    @NotNull
    public Pattern getNamePattern() {
        return this.namePattern;
    }

    public int getPlayerMaxProfiles(@NotNull Player player) {
        return Hooks.getGroupValueInt(player, this.profilesAmount, true);
    }

    @NotNull
    public SettingsGUI getSettingsGUI() {
        return this.guiSettings;
    }

    @NotNull
    public ProfileGUI getProfileGUI() {
        return this.guiProfile;
    }

    @NotNull
    public ProfilesGUI getProfilesGUI() {
        return this.guiProfiles;
    }

    public boolean canCreateMoreProfiles(@NotNull Player player) {
        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return false;

        int maxProfiles = this.getPlayerMaxProfiles(player);
        int hasProfiles = user.getProfileMap().size();
        if (maxProfiles >= 0 && hasProfiles >= maxProfiles) {
            return false;
        }
        return true;
    }

    public void startProfileCreation(@NotNull Player player) {
        int maxProfiles = this.getPlayerMaxProfiles(player);
        if (!this.canCreateMoreProfiles(player)) {
            plugin.lang().Profiles_Create_Error_Maximum.replace("%amount%", maxProfiles).send(player);
            return;
        }

        this.profileCreation.add(player);
        plugin.lang().Profiles_Create_Tip_Name.send(player);
    }

    public boolean createProfile(@NotNull Player player, @NotNull String name) {
        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return false;

        int maxProfiles = this.getPlayerMaxProfiles(player);
        if (!this.canCreateMoreProfiles(player)) {
            plugin.lang().Profiles_Create_Error_Maximum.replace("%amount%", maxProfiles).send(player);
            return false;
        }

        if (!this.getNamePattern().matcher(name).matches() || name.equalsIgnoreCase(JStrings.DEFAULT)) {
            plugin.lang().Profiles_Create_Error_Regex.send(player);
            return false;
        }

        if (user.getProfile(name) != null) {
            plugin.lang().Profiles_Create_Error_Exists.send(player);
            return false;
        }

        if (!user.addProfile(name)) {
            return false;
        }

        this.getProfilesGUI().open(player, 1);
        this.profileCreation.remove(player);
        return true;
    }

    public boolean switchProfile(@NotNull Player player, @NotNull UserProfile profile) {
        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return false;

        if (user.switchProfile(profile.getIdName())) {
            plugin.lang().Profiles_Switch_Done.replace("%profile%", profile.getIdName()).send(player);
            this.setProfileSwitchCooldown(player);
            EntityStats.get(player).updateAll();
            return true;
        }
        return false;
    }

    public void setProfileSwitchCooldown(@NotNull Player player) {
        if (this.getChangeCooldown() <= 0) return;

        long expireDate = System.currentTimeMillis() + this.getChangeCooldown() * 1000L;
        this.profileCooldown.computeIfAbsent(player.getName(), date -> expireDate);
    }

    public long getProfileSwitchCooldownDate(@NotNull Player player) {
        if (this.getChangeCooldown() <= 0) return 0L;

        long expireDate = this.profileCooldown.getOrDefault(player.getName(), 0L);
        if (expireDate < System.currentTimeMillis()) this.profileCooldown.remove(player.getName());

        return expireDate;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProfileSelectJoin(PlayerJoinEvent e) {
        if (!this.isSelectOnJoin()) return;

        this.plugin.getServer().getScheduler().runTask(plugin, () -> {
            this.getProfilesGUI().open(e.getPlayer(), 1);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProfileCreationChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (!this.profileCreation.contains(player)) return;

        e.setCancelled(true);
        e.getRecipients().clear();

        String msg = e.getMessage();
        if (msg.equalsIgnoreCase(JStrings.EXIT)) {
            player.sendTitle("", "", 1, 1, 1);
            this.profileCreation.remove(player);
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!this.createProfile(player, e.getMessage())) {
                plugin.lang().Profiles_Create_Tip_Error.send(player);
                this.profileCreation.remove(player);
                return;
            }

            plugin.lang().Profiles_Create_Tip_Done.send(player);
        });
    }
	
	/*@EventHandler(priority = EventPriority.HIGHEST)
	public void onProfileQuit(PlayerQuitEvent e) {
		Player player = e.getPlayer();
		RPGUser user = plugin.getUserManager().getOrLoadUser(player);
		
		user.getActiveProfile().setInventory(player.getInventory().getContents());
		player.getInventory().clear();
	}*/
}
