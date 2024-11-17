package studio.magemonkey.divinity.modules.list.itemgenerator.editor.enchantments;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.EditorGUI;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnchantmentListGUI extends AbstractEditorGUI {

    public EnchantmentListGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player,
                6,
                "[&d" + itemGenerator.getId() + "&r] editor/" + EditorGUI.ItemType.ENCHANTMENTS.getTitle(),
                itemGenerator);
    }

    @Override
    public void setContents() {
        Map<String, String> map = new LinkedHashMap<>();
        ConfigurationSection section = this.itemGenerator.getConfig()
                .getConfigurationSection(EditorGUI.ItemType.ENCHANTMENTS.getPath() + ".list");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                map.put(key, section.getString(key));
            }
        }
        List<String>  list                = new ArrayList<>(map.keySet());
        Enchantment[] vanillaEnchantments = Enchantment.values();
        if (list.size() < vanillaEnchantments.length) {
            list.add(null);
        }
        List<String> missingList = new ArrayList<>();
        for (Enchantment enchantment : vanillaEnchantments) {
            String key = enchantment.getKey().toString().substring("minecraft:".length());
            if (!list.contains(key)) {
                missingList.add(key);
            }
        }
        int i = 0;
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
            setSlot(i, key == null ?
                    new Slot(createItem(Material.REDSTONE, "&eAdd new enchantment")) {
                        @Override
                        public void onLeftClick() {
                            openSubMenu(new NewEnchantmentGUI(player, itemGenerator, missingList));
                        }
                    } :
                    new Slot(createItem(Material.ENCHANTED_BOOK,
                            "&e" + key,
                            "&bCurrent: &a" + map.get(key),
                            "&6Left-Click: &eSet",
                            "&6Right-Click: &eRemove")) {
                        @Override
                        public void onLeftClick() {
                            sendSetMessage("desired level range",
                                    map.get(key),
                                    s -> {
                                        String[] strings = s.split(":");
                                        if (strings.length > 2) {
                                            throw new IllegalArgumentException();
                                        }

                                        for (String string : strings) {
                                            Integer.parseInt(string); // Detect invalid input
                                        }
                                        itemGenerator.getConfig()
                                                .set(EditorGUI.ItemType.ENCHANTMENTS.getPath() + ".list." + key, s);
                                        saveAndReopen();
                                    });
                        }

                        @Override
                        public void onDrop() {
                            itemGenerator.getConfig()
                                    .remove(EditorGUI.ItemType.ENCHANTMENTS.getPath() + ".list." + key);
                            saveAndReopen();
                        }
                    });
        }
        if (list.get(list.size() - 1) == null) {
            list.remove(list.size() - 1);
        }
        this.setSlot(this.getPages() * this.inventory.getSize() - 9, getPrevButton());
        this.setSlot(this.getPages() * this.inventory.getSize() - 1, getNextButton());
    }
}
