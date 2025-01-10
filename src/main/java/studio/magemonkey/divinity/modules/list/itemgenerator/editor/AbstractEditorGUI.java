package studio.magemonkey.divinity.modules.list.itemgenerator.editor;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.core.Version;
import studio.magemonkey.codex.manager.api.menu.Menu;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class AbstractEditorGUI extends Menu {
    public static final String            CURRENT_PLACEHOLDER = "%current%";
    static              AbstractEditorGUI instance;

    protected ItemGeneratorReference itemGenerator;

    public AbstractEditorGUI(Player player, int rows, String title, ItemGeneratorReference itemGenerator) {
        super(player, rows, title);
        this.itemGenerator = itemGenerator;
    }

    @Nullable
    public static AbstractEditorGUI getInstance() {return instance;}

    @Override
    public void open(int page) {
        if (AbstractEditorGUI.instance != null) {
            for (HumanEntity humanEntity : instance.getInventory().getViewers()) {
                if (!humanEntity.getUniqueId().equals(player.getUniqueId())) {
                    throw new IllegalStateException("Another editor is already open");
                }
            }
        }
        super.open(page);
        AbstractEditorGUI.instance = this;
    }

    public void shutdown() {AbstractEditorGUI.instance = null;}

    protected static ItemStack createItem(Material material, String name, String... lore) {
        return createItem(material, name, List.of(lore));
    }

    protected static ItemStack createItem(Material material, String name, List<String> lore) {
        return createItem(new ItemStack(material), name, lore);
    }

    protected static ItemStack createItem(ItemStack itemStack, String name, String... lore) {
        return createItem(itemStack, name, List.of(lore));
    }

    protected static ItemStack createItem(ItemStack itemStack, String name, List<String> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        ItemFlag hidePotionsFlag;
        try {
            hidePotionsFlag = ItemFlag.HIDE_ADDITIONAL_TOOLTIP;
        } catch (NoSuchFieldError ignored) {
            hidePotionsFlag = ItemFlag.valueOf("HIDE_POTION_EFFECTS");
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(hidePotionsFlag);
        if (Version.CURRENT.isHigher(Version.V1_19_R3)) {
            meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        }
        meta.setDisplayName(StringUT.color(name));
        List<String> coloredLore = new ArrayList<>(lore.size());
        for (String loreLine : lore) {
            coloredLore.add(StringUT.color(loreLine));
        }
        meta.setLore(coloredLore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    protected List<String> color(List<String> list) {
        List<String> coloredList = new ArrayList<>(list.size());
        for (String string : list) {
            coloredList.add(StringUT.color(string));
        }
        return coloredList;
    }

    protected List<String> color(String... strings) {
        List<String> coloredList = new ArrayList<>(strings.length);
        for (String string : strings) {
            coloredList.add(StringUT.color(string));
        }
        return coloredList;
    }

    protected void saveAndReopen() {
        itemGenerator.reload();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (parentMenu == null) {
                    open();
                } else {
                    fakeClosing = true;
                    parentMenu.openSubMenu(AbstractEditorGUI.this);
                    fakeClosing = false; // In case it wasn't open yet
                }
            }
        }.runTask(Divinity.getInstance());
    }

    protected void setDefault(String path) {
        this.itemGenerator.getConfig().set(path, ItemGeneratorManager.commonItemGenerator.get(path));
    }

    protected void sendSetMessage(String valueName, @Nullable String currentValue, Consumer<String> onMessage) {
        fakeClose();
        BaseComponent component = StringUT.parseJson("[\"\",{\"text\":\"\\u25b8 Enter the desired " + valueName +
                ". \"},{\"text\":\"Cancel\",\"underlined\":true,\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"cancel\"}}"
                + (currentValue == null ? ']'
                : ",{\"text\":\" \"},{\"text\":\"Current Value\",\"underlined\":true,\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\""
                        + currentValue + "\"}}]"));
        player.spigot().sendMessage(component);
        this.registerListener(new Listener() {
            @EventHandler
            public void onChat(AsyncPlayerChatEvent event) {
                if (!event.getPlayer().equals(AbstractEditorGUI.this.player)) {
                    return;
                }
                event.setCancelled(true);
                String message = event.getMessage().strip();
                if (message.equalsIgnoreCase("cancel")) {
                    unregisterListener(this);
                    saveAndReopen();
                } else {
                    try {
                        onMessage.accept(message);
                        unregisterListener(this);
                    } catch (IllegalArgumentException e) {
                        Divinity.getInstance().lang().ItemGenerator_Cmd_Editor_Error_InvalidInput.replace("%input%",
                                message).replace("%value%", valueName).send(player);
                        player.spigot().sendMessage(component);
                    }
                }
            }
        });
    }

    public static class ItemGeneratorReference {
        private ItemGeneratorManager.GeneratorItem handle;

        public ItemGeneratorReference(ItemGeneratorManager.GeneratorItem itemGenerator) {
            this.handle = itemGenerator;
        }

        public ItemGeneratorManager.GeneratorItem getHandle() {
            return handle;
        }

        public void reload() {
            JYML cfg = getConfig();
            cfg.save();
            handle = Objects.requireNonNull(Divinity.getInstance()
                    .getModuleManager()
                    .getModule(ItemGeneratorManager.class)).load(handle.getId(), cfg);
        }

        public String getId() {
            return handle.getId();
        }

        public JYML getConfig() {
            return handle.getConfig();
        }
    }
}
