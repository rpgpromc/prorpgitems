package studio.magemonkey.divinity.modules.list.classes.gui;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageClass;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.spell.Spell;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.gui.*;
import studio.magemonkey.codex.util.CollectionsUT;
import studio.magemonkey.codex.util.DataUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.hooks.external.MagicHK;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;
import studio.magemonkey.divinity.modules.list.classes.ComboManager;
import studio.magemonkey.divinity.modules.list.classes.ComboManager.ComboKey;
import studio.magemonkey.divinity.modules.list.classes.api.IAbstractSkill;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;
import studio.magemonkey.divinity.modules.list.classes.api.UserSkillData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SkillListGUI extends NGUI<Divinity> {

    private ClassManager classManager;
    private int[]        objSlots;
    private String       previewName;
    private List<String> previewLore;

    private static final NamespacedKey TAG_PREVIEW = new NamespacedKey(Divinity.getInstance(), "QRPG_SKILL_PREVIEW");

    public SkillListGUI(@NotNull ClassManager classManager, @NotNull JYML cfg, @NotNull String path) {
        super(classManager.plugin, cfg, path);
        this.classManager = classManager;
        this.objSlots = cfg.getIntArray(path + "skill-slots");
        this.previewName = StringUT.color(cfg.getString(path + "upgrade-preview.name", "%name%"));
        this.previewLore = StringUT.color(cfg.getStringList(path + "upgrade-preview.lore"));

        GuiClick click = (p, type, e) -> {
            if (type == null) return;
            if (type == GSkillType.RESET) {
                classManager.reallocateSkillPoints(p);
                open(p, getUserPage(p, 0));
                return;
            }

            ContentType type2 = (ContentType) type;
            switch (type2) {
                case EXIT: {
                    p.closeInventory();
                    break;
                }
                case NEXT: {
                    open(p, getUserPage(p, 0) + 1);
                    break;
                }
                case BACK: {
                    open(p, getUserPage(p, 0) - 1);
                    break;
                }
                case RETURN: {
                    classManager.openStatsGUI(p);
                    break;
                }
                default: {

                    break;
                }
            }
        };

        for (String sId : cfg.getSection(path + "content")) {
            GuiItem guiItem = cfg.getGuiItem(path + "content." + sId, GSkillType.class);
            if (guiItem == null) continue;

            if (guiItem.getType() != null) {
                guiItem.setClick(click);
            }

            this.addButton(guiItem);
        }
    }

    static enum GSkillType {
        RESET,
        ;
    }

    @Override
    protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {
        UserClassData data = classManager.getUserData(player);
        if (data == null) return;

        // Add Magic Spells from Magic plugin.
        boolean isMagic = this.classManager.hasMagic();
        if (isMagic) {
            MagicHK magicHook = classManager.getMagic();
            if (magicHook != null) {
                MageController api       = magicHook.getAPI().getController();
                Mage           mage      = api.getMage(player);
                MageClass      mageClass = mage.unlockClass("mage");
                if (mageClass != null) {
                    for (String spellId : mageClass.getSpells()) {
                        if (spellId == null) continue;

                        Spell spell = mageClass.getSpell(spellId);
                        int   level = spell.getSpellKey().getLevel();
                        if (data.getSkillData(spellId) == null) {
                            data.getSkillsMap().put(spellId, new UserSkillData(spellId, level, new ComboKey[]{}));
                        }
                    }
                }
            }
        }

        int                       len   = this.objSlots.length;
        List<UserSkillData>       list  = new ArrayList<>(data.getSkills());
        List<List<UserSkillData>> split = CollectionsUT.split(list, len);

        int pages = split.size();
        if (pages < 1) {
            list = Collections.emptyList();
        } else {
            if (page > pages) page = pages;
            list = split.get(page - 1);
        }

        int count = 0;

        for (UserSkillData sData : list) {
            IAbstractSkill skill = classManager.getSkillById(sData.getId());
            ItemStack      icon2 = null;

            if (skill == null) {
                if (isMagic) {
                    MagicHK magicHook = classManager.getMagic();
                    if (magicHook != null) {
                        MageController api       = magicHook.getAPI().getController();
                        Mage           mage      = api.getMage(player);
                        MageClass      mageClass = mage.unlockClass("mage");

                        if (mageClass != null) {
                            Collection<String> spells = mageClass.getSpells();
                            // Remove Spells that mage don't have.
                            if (!spells.contains(sData.getId())) {
                                data.getSkillsMap().remove(sData.getId());
                                continue;
                            }
                            icon2 = api.createItem(sData.getId());
                        }
                    }
                }
            } else {
                icon2 = skill.getIcon(player, sData.getLevel());
            }
            if (icon2 == null) continue;

            JIcon icon = new JIcon(icon2);
            icon.setClick((p, type, e) -> {
                if (e.isRightClick() && skill != null) {
                    if (sData.getLevel() >= skill.getMaxLevel()) return;

                    ItemStack iconCur = e.getCurrentItem();
                    if (iconCur == null) return;

                    boolean isPreview = DataUT.getStringData(iconCur, TAG_PREVIEW) != null;

                    if (!isPreview) {
                        ItemStack iconUp = skill.getIcon(p, sData.getLevel() + 1);

                        ItemMeta meta1 = iconCur.getItemMeta();
                        ItemMeta meta2 = iconUp.getItemMeta();
                        if (meta1 == null || meta2 == null) return;

                        List<String> loreCur = new ArrayList<>();
                        List<String> loreUp  = meta2.getLore();

                        meta1.setDisplayName(previewName.replace("%name%", meta2.getDisplayName()));

                        for (String s : previewLore) {
                            if (s.contains("%lore%") && loreUp != null) {
                                for (String s2 : loreUp) {
                                    loreCur.add(s2);
                                }
                                continue;
                            }
                            loreCur.add(s
                                    .replace("%cost%", String.valueOf(skill.getSkillPointsCost(sData.getLevel()))));
                        }
                        meta1.setLore(loreCur);
                        iconCur.setItemMeta(meta1);
                        DataUT.setData(iconCur, TAG_PREVIEW, "true");
                        e.getInventory().setItem(e.getRawSlot(), iconCur);
                        return;
                    }

                    classManager.addSkill(p, skill, sData.getLevel() + 1, false);
                    open(p, 1);
                } else if (e.isLeftClick()) {
                    ComboManager comboManager = classManager.getComboManager();
                    if (comboManager == null) return;
                    if (isMagic || (skill != null && !skill.isPassive())) {
                        comboManager.openGUI(p, sData);
                    }
                }
            });

            this.addButton(player, icon, this.objSlots[count++]);
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
}
