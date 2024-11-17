package studio.magemonkey.divinity.manager.profile;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.gui.*;
import studio.magemonkey.codex.util.CollectionsUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.data.api.DivinityUser;
import studio.magemonkey.divinity.data.api.UserProfile;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfilesGUI extends NGUI<Divinity> {

    private ProfileManager profileManager;
    private ItemStack      profileIcon;
    private int[]          profileSlots;

    ProfilesGUI(@NotNull ProfileManager profileManager, @NotNull JYML cfg) {
        super(profileManager.plugin, cfg, "");
        this.profileManager = profileManager;

        this.profileIcon = cfg.getItem("profiles.icon");
        this.profileSlots = cfg.getIntArray("profiles.slots");

        GuiClick click = (p, type, e) -> {
            if (type == null) return;
            Class<?> clazz = type.getClass();

            if (clazz.equals(ContentType.class)) {
                ContentType type2 = (ContentType) type;
                switch (type2) {
                    case NEXT: {
                        this.open(p, this.getUserPage(p, 0) + 1);
                        break;
                    }
                    case BACK: {
                        this.open(p, this.getUserPage(p, 0) - 1);
                        break;
                    }
                    case RETURN: {
                        this.profileManager.getProfileGUI().open(p, 1);
                        break;
                    }
                    case EXIT: {
                        p.closeInventory();
                        break;
                    }
                    default: {
                        break;
                    }
                }
                return;
            }

            if (clazz.equals(ItemType.class)) {
                ItemType type2 = (ItemType) type;
                if (type2 == ItemType.PROFILE_CREATE) {
                    this.profileManager.startProfileCreation(p);
                    p.closeInventory();
                    return;
                }
            }
        };

        for (String id : cfg.getSection("content")) {
            GuiItem guiItem = cfg.getGuiItem("content." + id, ItemType.class);
            if (guiItem == null) continue;

            if (guiItem.getType() != null) {
                guiItem.setClick(click);
            }
            this.addButton(guiItem);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {
        int length = this.profileSlots.length;

        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return;

        List<UserProfile>       list  = new ArrayList<>(user.getProfileMap().values());
        List<List<UserProfile>> split = CollectionsUT.split(list, length);

        int pages = split.size();
        if (pages < 1) list = Collections.emptyList();
        else list = split.get(Math.min(page, split.size()) - 1);

        int count = 0;
        for (UserProfile profile : list) {
            ItemStack icon = new ItemStack(this.profileIcon);
            icon.setAmount(count + 1);

            ItemMeta meta = icon.getItemMeta();
            if (meta == null) continue;

            if (meta instanceof SkullMeta) {
                ((SkullMeta) meta).setOwner(profile.getIdName());
            }

            meta.setDisplayName(meta.getDisplayName().replace("%profile%", profile.getIdName()));

            List<String> lore = meta.getLore();
            if (lore != null) {
                UserClassData classData = profile.getClassData();

                lore.replaceAll(line -> line
                        .replace("%class-level%", classData != null ? String.valueOf(classData.getLevel()) : "0")
                        .replace("%class-name%", classData != null ? classData.getPlayerClass().getName() : "?")
                        .replace("%default%", plugin.lang().getBool(profile.isDefault()))
                        .replace("%active%", plugin.lang().getBool(user.getActiveProfile().equals(profile)))
                );
                meta.setLore(lore);
            }
            icon.setItemMeta(meta);

            GuiClick click = (p2, type, e) -> {
                if (e.isRightClick()) {
                    if (e.isShiftClick() && user.deleteProfile(profile.getIdName())) {
                        open(p2, 1);
                        return;
                    }
                    if (!profile.isDefault()) {
                        user.getProfileMap().values().forEach(profile2 -> profile2.setDefault(false));
                        profile.setDefault(true);
                        open(p2, 1);
                        return;
                    }
                    return;
                }

                if (!user.getActiveProfile().equals(profile)) {
                    this.profileManager.switchProfile(p2, profile);

                    if (user.getActiveProfile().getClassData() == null) {
                        ClassManager classManager = plugin.getModuleCache().getClassManager();
                        if (classManager != null && !classManager.isRemindDisabled(p2)) {
                            classManager.openSelectionGUI(p2, true);
                            return;
                        }
                    }

                    open(p2, 1);
                }
            };

            JIcon jicon = new JIcon(icon);
            jicon.setClick(click);
            this.addButton(player, jicon, this.profileSlots[count++]);
        }
    }

    @Override
    protected boolean ignoreNullClick() {
        return true;
    }

    @Override
    protected boolean cancelClick(int slot) {
        return true;
    }

    @Override
    protected boolean cancelPlayerClick() {
        return true;
    }

    private static enum ItemType {
        PROFILE_CREATE,
        ;
    }
}
