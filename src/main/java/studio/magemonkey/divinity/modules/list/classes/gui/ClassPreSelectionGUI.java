package studio.magemonkey.divinity.modules.list.classes.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.gui.ContentType;
import studio.magemonkey.codex.manager.api.gui.GuiClick;
import studio.magemonkey.codex.manager.api.gui.GuiItem;
import studio.magemonkey.codex.manager.api.gui.NGUI;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;

public class ClassPreSelectionGUI extends NGUI<Divinity> {

    public ClassPreSelectionGUI(@NotNull ClassManager classManager, @NotNull JYML cfg, @NotNull String path) {
        super(classManager.plugin, cfg, path);

        GuiClick click = (p, type, e) -> {
            if (type == null) return;
            if (type.getClass().equals(PreSelectionType.class)) {
                PreSelectionType type2 = (PreSelectionType) type;
                switch (type2) {
                    case SELECT_CHILD: {
                        classManager.openSelectionGUI(p, false);
                        break;
                    }
                    case SELECT_MAIN: {
                        classManager.openSelectionGUI(p, true);
                        break;
                    }
                    default: {
                        break;
                    }
                }
            } else {
                ContentType type2 = (ContentType) type;
                switch (type2) {
                    case RETURN: {
                        classManager.openStatsGUI(p);
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
            }
        };

        for (String sId : cfg.getSection(path + "content")) {
            GuiItem guiItem = cfg.getGuiItem(path + "content." + sId, PreSelectionType.class);
            if (guiItem == null) continue;

            if (guiItem.getType() != null) {
                guiItem.setClick(click);
            }

            this.addButton(guiItem);
        }
    }

    static enum PreSelectionType {
        SELECT_MAIN,
        SELECT_CHILD,
        ;
    }

    @Override
    protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {

    }

    @Override
    protected boolean cancelClick(int slot) {
        return true;
    }

    @Override
    protected boolean cancelPlayerClick() {
        return true;
    }

    @Override
    protected boolean ignoreNullClick() {
        return true;
    }
}
