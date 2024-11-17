package studio.magemonkey.divinity.modules.list.classes.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.gui.*;
import studio.magemonkey.codex.util.CollectionsUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.data.api.DivinityUser;
import studio.magemonkey.divinity.data.api.UserProfile;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;
import studio.magemonkey.divinity.modules.list.classes.api.RPGClass;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;

import java.util.ArrayList;
import java.util.List;

public class ClassSelectionGUI extends NGUI<Divinity> {

    private ClassManager classManager;
    private boolean      allowClose;
    private int[]        objSlots;
    private boolean      isMainSelector;

    public ClassSelectionGUI(@NotNull ClassManager classManager,
                             @NotNull JYML cfg,
                             @NotNull String path,
                             boolean main) {
        super(classManager.plugin, cfg, path);
        this.classManager = classManager;
        this.allowClose = cfg.getBoolean(path + "allow-close");
        this.objSlots = cfg.getIntArray(path + "class-slots");
        this.isMainSelector = main;

        GuiClick click = (p, type, e) -> {
            if (type == null || !type.getClass().equals(ContentType.class)) return;
            ContentType type2 = (ContentType) type;

            switch (type2) {
                case EXIT: {
                    p.closeInventory();
                    classManager.stopSelectRemind(p);
                    break;
                }
                case NEXT: {
                    this.open(p, this.getUserPage(p, 0) + 1);
                    break;
                }
                case BACK: {
                    this.open(p, this.getUserPage(p, 0) - 1);
                    break;
                }
                default: {
                    break;
                }
            }
        };

        for (String sId : cfg.getSection(path + "content")) {
            GuiItem guiItem = cfg.getGuiItem(path + "content." + sId, ContentType.class);
            if (guiItem == null) continue;

            Enum<?> eType = guiItem.getType();
            if (eType != null) {
                if (eType.getClass().equals(ContentType.class)) {
                    ContentType type2 = (ContentType) eType;
                    if (!this.allowClose && type2 == ContentType.EXIT) {
                        continue;
                    }

                    guiItem.setClick(click);
                }
            }
            this.addButton(guiItem);
        }
    }

    @Override
    protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {
        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return;

        UserProfile   prof  = user.getActiveProfile();
        UserClassData cData = prof.getClassData();
        RPGClass      cUser = cData != null ? cData.getPlayerClass() : null;

        int            len  = this.objSlots.length;
        List<RPGClass> list = new ArrayList<>();

        if (this.isMainSelector) {
            for (RPGClass cAll : this.classManager.getClasses()) {
                if (cAll.hasPermission(player) && !cAll.isChildClass()) {
                    list.add(cAll);
                }
            }
        } else if (cUser != null && cData != null && cUser.getLevelToChild() <= cData.getLevel()) {
            for (String childId : cUser.getChildClasses()) {
                RPGClass cChild = this.classManager.getClassById(childId);
                if (cChild != null && cChild.hasPermission(player)) {
                    list.add(cChild);
                }
            }
        }

        List<List<RPGClass>> split = CollectionsUT.split(list, len);

        int pages = split.size();
        if (pages < 1) list = new ArrayList<>();
        else {
            if (page > pages) page = pages;
            list = split.get(page - 1);
        }

        int i = 0;
        for (RPGClass c : list) {
            JIcon icon = new JIcon(c.getIcon());
            icon.setClick((p, type, e) -> {
                classManager.setPlayerClass(p, c, false);
                p.closeInventory();
            });

            int slot = this.objSlots[i++];
            this.addButton(player, icon, slot);
        }

        this.setUserPage(player, page, pages);
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

    @Override
    protected void onClose(@NotNull Player player, @NotNull InventoryCloseEvent e) {
        if (this.allowClose || classManager.isRemindDisabled(player)) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            open(player, 1);
        });
    }
}
