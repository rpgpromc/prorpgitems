package studio.magemonkey.divinity.modules.list.itemgenerator.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.config.Config;
import studio.magemonkey.divinity.stats.tiers.Tier;

public class TierGUI extends AbstractEditorGUI {

    public TierGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                6,
                "Editor/" + EditorGUI.ItemType.TIER.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        int i = 0;
        for (Tier tier : Config.getTiers().toArray(new Tier[0])) {
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

            setSlot(i, new Slot(createItem(tier.getId().equals(this.itemGenerator.getHandle().getTier().getId()) ?
                    Material.DIAMOND : Material.IRON_INGOT, tier.getName())) {
                @Override
                public void onLeftClick() {
                    itemGenerator.getConfig().set(EditorGUI.ItemType.TIER.getPath(), tier.getId());
                    saveAndReopen();
                }
            });
        }
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }
}
