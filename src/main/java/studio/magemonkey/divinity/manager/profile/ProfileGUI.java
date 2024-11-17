package studio.magemonkey.divinity.manager.profile;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.gui.ContentType;
import studio.magemonkey.codex.manager.api.gui.GuiClick;
import studio.magemonkey.codex.manager.api.gui.GuiItem;
import studio.magemonkey.codex.manager.api.gui.NGUI;
import studio.magemonkey.divinity.Divinity;

public class ProfileGUI extends NGUI<Divinity> {

    private ProfileManager profileManager;

    ProfileGUI(@NotNull ProfileManager profileManager, @NotNull JYML cfg) {
        super(profileManager.plugin, cfg, "");
        this.profileManager = profileManager;

        GuiClick click = (p, type, e) -> {
            if (type == null) return;

            Class<?> clazz = type.getClass();
            if (clazz.equals(ContentType.class)) {
                ContentType type2 = (ContentType) type;
                if (type2 == ContentType.EXIT) {
                    p.closeInventory();
                }
                return;
            }

            if (clazz.equals(ItemType.class)) {
                ItemType type2 = (ItemType) type;
                switch (type2) {
                    case PROFILE_CHANGE: {
                        this.profileManager.getProfilesGUI().open(p, 1);
                        break;
                    }
                    case PROFILE_SETTINGS: {
                        this.profileManager.getSettingsGUI().open(p, 1);
                        break;
                    }
                }
                return;
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

    @Override
    protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {

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
        PROFILE_CHANGE,
        PROFILE_SETTINGS,
        ;
    }
}
