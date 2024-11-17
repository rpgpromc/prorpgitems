package studio.magemonkey.divinity.modules.list.itemgenerator.editor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;

import java.util.ArrayList;
import java.util.List;

public class LoreGUI extends AbstractEditorGUI {
    private final String path;

    public LoreGUI(Player player, String title, ItemGeneratorReference itemGenerator, String path) {
        super(player, 6, title, itemGenerator);
        this.path = path;
    }

    @Override
    public void setContents() {
        List<String> lore = new ArrayList<>(itemGenerator.getConfig().getStringList(path));
        lore.add(null);
        int i = 0;
        for (int j = 0; j < lore.size(); j++) {
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
            String loreLine = lore.get(j);
            int    k        = j;
            setSlot(i, loreLine == null ?
                    new Slot(createItem(Material.REDSTONE, "&eAdd new lore line")) {
                        @Override
                        public void onLeftClick() {
                            lore.add("");
                            sendSetMessage("lore line " + k,
                                    null,
                                    s -> {
                                        lore.set(k, s);
                                        itemGenerator.getConfig().set(path, lore);
                                        saveAndReopen();
                                    });
                        }
                    } :
                    new Slot(createItem(Material.WRITABLE_BOOK,
                            loreLine.isEmpty() ? "''" : loreLine,
                            "&6Left-Click: &eSet",
                            "&6Shift-Left-Click: &eAdd to left",
                            "&6Shift-Right-Click: &eAdd to right",
                            "&6Right-Click: &eRemove")) {
                        @Override
                        public void onShiftLeftClick() {
                            lore.add(k, "");
                            sendSetMessage("lore line " + k,
                                    null,
                                    s -> {
                                        lore.set(k, s);
                                        itemGenerator.getConfig().set(path, lore);
                                        saveAndReopen();
                                    });
                        }

                        @Override
                        public void onShiftRightClick() {
                            lore.add(k + 1, "");
                            sendSetMessage("lore line " + (k + 1),
                                    null,
                                    s -> {
                                        lore.set(k + 1, s);
                                        itemGenerator.getConfig().set(path, lore);
                                        saveAndReopen();
                                    });
                        }

                        @Override
                        public void onLeftClick() {
                            sendSetMessage("lore line " + k,
                                    lore.get(k),
                                    s -> {
                                        lore.set(k, s);
                                        itemGenerator.getConfig().set(path, lore);
                                        saveAndReopen();
                                    });
                        }

                        @Override
                        public void onRightClick() {
                            lore.remove(k);
                            itemGenerator.getConfig().set(path, lore);
                            saveAndReopen();
                        }
                    });
        }
        lore.remove(lore.size() - 1);
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }
}
