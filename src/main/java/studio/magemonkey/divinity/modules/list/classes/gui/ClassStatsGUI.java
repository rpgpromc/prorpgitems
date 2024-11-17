package studio.magemonkey.divinity.modules.list.classes.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.gui.ContentType;
import studio.magemonkey.codex.manager.api.gui.GuiClick;
import studio.magemonkey.codex.manager.api.gui.GuiItem;
import studio.magemonkey.codex.manager.api.gui.NGUI;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;
import studio.magemonkey.divinity.modules.list.classes.api.RPGClass;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAttribute;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAttributeType;

import java.util.List;

public class ClassStatsGUI extends NGUI<Divinity> {

    private ClassManager classManager;

    public ClassStatsGUI(@NotNull ClassManager classManager, @NotNull JYML cfg, @NotNull String path) {
        super(classManager.plugin, cfg, path);
        this.classManager = classManager;

        GuiClick click = (p, type, e) -> {
            if (type == null) return;

            if (type.getClass().equals(GUIStatsType.class)) {
                GUIStatsType type2 = (GUIStatsType) type;
                switch (type2) {
                    case CLASS_ASPECTS: {
                        classManager.getAspectManager().openGUI(p);
                        break;
                    }
                    case CLASS_SKILLS: {
                        classManager.openSkillsGUI(p);
                        break;
                    }
                    default: {
                        break;
                    }
                }
                return;
            } else {
                ContentType type2 = (ContentType) type;
                switch (type2) {
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
        };

        for (String sId : cfg.getSection(path + "content")) {
            GuiItem guiItem = cfg.getGuiItem(path + "content." + sId, GUIStatsType.class);
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

    static enum GUIStatsType {
        CLASS_SKILLS,
        CLASS_ASPECTS,
        ;
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
    protected void replaceMeta(@NotNull Player player, @NotNull ItemStack item, @NotNull GuiItem guiItem) {
        super.replaceMeta(player, item, guiItem);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.getLore();
        if (lore == null) return;

        UserClassData cData = classManager.getUserData(player);
        if (cData == null) return;

        RPGClass rpgClass = cData.getPlayerClass();

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            line = line
                    .replace("%class%", rpgClass.getName())
                    .replace("%aspect-points%", String.valueOf(cData.getAspectPoints()))
                    .replace("%skill-points%", String.valueOf(cData.getSkillPoints()))
                    .replace("%exp-max%", String.valueOf(cData.getExpToUp(true)))
                    .replace("%exp%", String.valueOf(cData.getExp()))
                    .replace("%level-max%", String.valueOf(rpgClass.getMaxLevel()))
                    .replace("%level%", String.valueOf(cData.getLevel()));
            for (ClassAttributeType type : ClassAttributeType.values()) {
                ClassAttribute a = rpgClass.getAttribute(type);
                if (a == null) continue;

                double total  = cData.getAttribute(type);
                double aspect = total - rpgClass.getAttributeValue(type, cData.getLevel());
                //double has = c.getAttributeValue(type, cData.getLevel());
                line = a.replace(type, line, total, aspect, (cData.getLevel() - rpgClass.getStartLevel()));
            }

            lore.set(i, line);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
