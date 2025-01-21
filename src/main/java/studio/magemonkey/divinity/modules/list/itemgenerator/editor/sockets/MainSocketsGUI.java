package studio.magemonkey.divinity.modules.list.itemgenerator.editor.sockets;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

import java.util.ArrayList;
import java.util.List;

public class MainSocketsGUI extends AbstractEditorGUI {
    public MainSocketsGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                6,
                "Editor/" + EditorGUI.ItemType.SOCKETS.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        ConfigurationSection cfg =
                this.itemGenerator.getConfig().getConfigurationSection(EditorGUI.ItemType.SOCKETS.getPath());
        List<String> list = new ArrayList<>();
        if (cfg != null) {
            list.addAll(cfg.getKeys(false));
        }
        int i = 0;
        for (String entry : list) {
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
            setSlot(i, new Slot(createItem(Material.EMERALD,
                    "&e" + entry,
                    "&eModify")) {
                @Override
                public void onLeftClick() {
                    openSubMenu(new SocketGUI(player, itemGenerator, entry));
                }
            });
        }
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }
}
