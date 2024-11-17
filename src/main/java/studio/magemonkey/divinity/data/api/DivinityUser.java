package studio.magemonkey.divinity.data.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.data.users.IAbstractUser;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.divinity.Divinity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DivinityUser extends IAbstractUser<Divinity> {

    private final     Map<String, UserProfile> profiles;
    private transient UserProfile              activeProfile;

    // Create new
    public DivinityUser(@NotNull Divinity plugin, @NotNull Player player) {
        this(
                plugin,
                player.getUniqueId(),
                player.getName(),
                System.currentTimeMillis(),
                new LinkedHashMap<>()
        );
    }

    // Load from database
    public DivinityUser(
            @NotNull Divinity plugin,
            @NotNull UUID uuid,
            @NotNull String name,
            long lastOnline,
            @NotNull Map<String, UserProfile> profiles
    ) {
        super(plugin, uuid, name, lastOnline);

        this.profiles = profiles;
        if (this.profiles.isEmpty()) {
            this.setupDefaultProfile();
        } else {
            this.setupActiveProfileByDefault();
        }
    }

    private void setupDefaultProfile() {
        this.plugin.info("Setup default profile for '" + this.uuid + "'");
        this.setActiveProfile(new UserProfile());
        this.profiles.put(this.activeProfile.getIdName(), this.activeProfile);
    }

    private void setupActiveProfileByDefault() {
        for (UserProfile prof : this.profiles.values()) {
            if (prof.isDefault()) {
                this.setActiveProfile(prof);
                break;
            }
        }
        if (this.activeProfile == null) {
            for (UserProfile prof : this.profiles.values()) {
                this.setActiveProfile(prof);
                this.getActiveProfile().setDefault(true);
                break;
            }
        }
    }

    @NotNull
    public UserProfile getActiveProfile() {
        return this.activeProfile;
    }

    public void setActiveProfile(@NotNull UserProfile profile) {
		/*Player player = plugin.getServer().getPlayer(this.getUUID());
		
		if (player != null) {
			if (this.activeProfile != null) {
				// Save player equipment to current active profile.
				this.getActiveProfile().setInventory(player.getInventory().getContents());
				// Clear player current profile equipment.
				player.getInventory().clear();
			}
			// Give player new profile equipment.
			profile.applyEquipment(player);
		}
		*/

        this.activeProfile = profile;
    }

    @NotNull
    public Map<String, UserProfile> getProfileMap() {
        return this.profiles;
    }

    @Nullable
    public UserProfile getProfile(@NotNull String profileId) {
        return this.profiles.get(profileId.toLowerCase());
    }

    public boolean setDefaultProfile(@NotNull String profileId) {
        UserProfile profile = this.getProfile(profileId);
        if (profile == null) return false;

        this.getProfileMap().values().forEach(prof -> prof.setDefault(false));
        profile.setDefault(true);

        return true;
    }

    public boolean addProfile(@NotNull String profileId) {
        UserProfile profile = new UserProfile(profileId, false);
        return this.profiles.put(profileId.toLowerCase(), profile) == null;
    }

    public boolean switchProfile(@NotNull String profileId) {
        UserProfile profile = this.getProfile(profileId);
        if (profile == null) return false;

        this.setActiveProfile(profile);
        return true;
    }

    public boolean deleteProfile(@NotNull String profileId) {
        UserProfile profile = this.getProfile(profileId);
        if (profile == null || profile.getIdName().equalsIgnoreCase(JStrings.DEFAULT)) return false;

        if (this.getActiveProfile().equals(profile)) {
            return false;
        }

        this.profiles.remove(profile.getIdName());

        if (profile.isDefault()) {
            this.getProfileMap().values().stream().findFirst().get().setDefault(true);
        }

        profile = null;

        return true;
    }
}
