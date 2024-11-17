package studio.magemonkey.divinity.modules.list.itemgenerator.editor.sockets;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.SocketAttribute;

import java.util.ArrayList;
import java.util.List;

public class SocketListGUI extends AbstractEditorGUI {
    private final String name;
    private final String path;

    public SocketListGUI(Player player, ItemGeneratorReference itemGenerator, String name) {
        super(player,
                6,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.SOCKETS.getTitle(),
                itemGenerator);
        this.name = name;
        this.path = EditorGUI.ItemType.SOCKETS.getPath() + '.' + this.name + ".list.";
    }

    @Override
    public void setContents() {
        List<String> list = new ArrayList<>();
        for (SocketAttribute socketAttribute : ItemStats.getSockets(SocketAttribute.Type.valueOf(this.name))) {
            list.add(socketAttribute.getId());
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
                    "&bCurrent: &a" + itemGenerator.getConfig().getDouble(path + entry + ".chance"),
                    "&eModify")) {
                @Override
                public void onLeftClick() {
                    sendSetMessage("chance for a " + entry + ' ' + name + " socket",
                            String.valueOf(itemGenerator.getConfig().getDouble(path + entry + ".chance")),
                            s -> {
                                itemGenerator.getConfig().set(path + entry + ".chance", Double.parseDouble(s));
                                saveAndReopen();
                            });
                }
            });
        }
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }
}
