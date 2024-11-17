package studio.magemonkey.divinity.modules.list.itemgenerator.editor.skills;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.hooks.EHook;
import studio.magemonkey.divinity.hooks.external.FabledHook;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

import java.util.List;

public class NewSkillGUI extends AbstractEditorGUI {
    private final List<String> list;

    public NewSkillGUI(Player player, ItemGeneratorReference itemGenerator, List<String> missingSkills) {
        super(player,
                6,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.SKILLS.getTitle(),
                itemGenerator);
        this.list = missingSkills;
    }

    @Override
    public void setContents() {
        FabledHook fabledHook = (FabledHook) Divinity.getInstance().getHook(EHook.SKILL_API);
        int        i          = 0;
        for (String key : list) {
            i++;
            if (i % this.inventory.getSize() == 53) {
                this.setSlot(i, getNextButton());
                i++;
            } else if (i % 9 == 8) {
                i++;
            }
            if (i % this.inventory.getSize() == 45) {
                this.setSlot(i, getPrevButton());
                i++;
            } else if (i % 9 == 0) {
                i++;
            }
            ItemStack itemStack = createItem(Material.JACK_O_LANTERN,
                    "&e" + key, "&6Left-Click: &eAdd");
            if (fabledHook != null) {
                ItemStack indicator = fabledHook.getSkillIndicator(key);
                if (indicator != null) {
                    itemStack.setType(indicator.getType());
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if (itemMeta != null) {
                        ItemMeta indicatorMeta = indicator.getItemMeta();
                        if (indicatorMeta != null && indicatorMeta.hasCustomModelData()) {
                            itemMeta.setCustomModelData(indicatorMeta.getCustomModelData());
                            itemStack.setItemMeta(itemMeta);
                        }
                    }
                }
            }
            setSlot(i, new Slot(itemStack) {
                @Override
                public void onLeftClick() {
                    String path = EditorGUI.ItemType.SKILLS.getPath() + ".list." + key;
                    JYML   cfg  = itemGenerator.getConfig();
                    cfg.set(SkillGUI.ItemType.CHANCE.getPath(path), 0);
                    cfg.set(SkillGUI.ItemType.MIN.getPath(path), 1);
                    cfg.set(SkillGUI.ItemType.MAX.getPath(path), 1);
                    cfg.set(SkillGUI.ItemType.LORE.getPath(path), List.of("&b" + key + " &7Lvl. &f%level%"));
                    saveAndReopen();
                    close();
                }
            });
        }
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }
}
