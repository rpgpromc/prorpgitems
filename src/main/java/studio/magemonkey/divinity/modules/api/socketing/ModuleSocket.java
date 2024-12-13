package studio.magemonkey.divinity.modules.api.socketing;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.compat.VersionManager;
import studio.magemonkey.codex.util.CollectionsUT;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.actions.ActionManipulator;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.manager.interactions.api.AnimatedSuccessBar;
import studio.magemonkey.divinity.modules.SocketItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.api.socketing.merchant.MerchantSocket;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.SocketAttribute;
import studio.magemonkey.divinity.stats.items.requirements.ItemRequirements;
import studio.magemonkey.divinity.stats.items.requirements.item.ItemSocketRequirement;

import java.util.*;
import java.util.Map.Entry;

public abstract class ModuleSocket<I extends SocketItem> extends QModuleDrop<I> {
    protected SocketAttribute.Type socketType;

    protected boolean                    allowDuplicatedSockets;
    protected ActionManipulator          actionsComplete;
    protected ActionManipulator          actionsError;
    protected AnimatedSuccessBar.Builder animation;

    protected TreeMap<Integer, Integer> silentRateBonusMap;
    protected int                       silentRateBonusCap;
    protected Map<String, Integer>      userSilentRateBonusMap;

    protected boolean failDestroyTarget;
    protected boolean failDestroySource;
    protected boolean failWipeSockets;

    protected UserGUI        guiUser;
    protected MerchantSocket merchant;

    public ModuleSocket(@NotNull Divinity plugin, @NotNull Class<I> clazz) {
        super(plugin, clazz);
    }

    @Override
    protected void loadSettings() {
        super.loadSettings();

        String path = "socketing.";
        this.allowDuplicatedSockets = cfg.getBoolean(path + "allow-duplicated-items");

        String path2 = path + "animated-bar.";
        this.cfg.addMissing(path2 + "enabled", true);
        this.cfg.addMissing(path2 + "bar-title", "&e&lSocketing...");
        this.cfg.addMissing(path2 + "bar-char", "◼");
        this.cfg.addMissing(path2 + "bar-size", 20);
        this.cfg.addMissing(path2 + "bar-format", "&a&l%success%%&r %bar%&r &c&l%failure%%");
        this.cfg.addMissing(path2 + "color-neutral", ChatColor.DARK_GRAY.name());
        this.cfg.addMissing(path2 + "color-success", ChatColor.GREEN.name());
        this.cfg.addMissing(path2 + "color-failure", ChatColor.RED.name());
        this.cfg.addMissing(path2 + "fill-interval", 1);
        this.cfg.addMissing(path2 + "fill-amount", 1);
        this.cfg.addMissing(path2 + "min-success", 50);

        if (this.cfg.getBoolean(path + "animated-bar.enabled")) {
            String barTitle  = cfg.getString(path2 + "bar-title", "&e&lSocketing...");
            String barChar   = cfg.getString(path2 + "bar-char", "◼");
            String barFormat = cfg.getString(path2 + "bar-format", "%bar%");
            int    barSize   = cfg.getInt(path2 + "bar-size", 20);
            ChatColor barColorNeutral =
                    CollectionsUT.getEnum(cfg.getString(path2 + "color-neutral", "DARK_GRAY"), ChatColor.class);
            ChatColor barColorGood =
                    CollectionsUT.getEnum(cfg.getString(path2 + "color-success", "GREEN"), ChatColor.class);
            ChatColor barColorBad =
                    CollectionsUT.getEnum(cfg.getString(path2 + "color-failure", "RED"), ChatColor.class);
            long fillInterval = cfg.getLong(path2 + "fill-interval", 1L);
            int  fillAmount   = cfg.getInt(path2 + "fill-amount", 1);

            this.animation = new AnimatedSuccessBar.Builder(plugin, barTitle, barChar)
                    .setBarFormat(barFormat)
                    .setColorNeutral(barColorNeutral == null ? ChatColor.DARK_GRAY : barColorNeutral)
                    .setColorSuccess(barColorGood == null ? ChatColor.GREEN : barColorGood)
                    .setColorBad(barColorBad == null ? ChatColor.RED : barColorBad)
                    .setBarSize(barSize).setFillInterval(fillInterval)
                    .setFillAmount(fillAmount);
        }

        this.actionsComplete = new ActionManipulator(plugin, cfg, path + "actions-complete");
        this.actionsError = new ActionManipulator(plugin, cfg, path + "actions-error");

        path = "socketing.failure.";
        this.failDestroyTarget = cfg.getBoolean(path + "destroy-target", true);
        this.failDestroySource = cfg.getBoolean(path + "destroy-source");
        this.failWipeSockets = cfg.getBoolean(path + "wipe-filled-sockets");

        this.guiUser = new UserGUI(this);

        // Setup Module Merchant.
        path = "socketing.merchant";
        this.cfg.addMissing(path + "enabled", true);
        if (this.cfg.getBoolean(path + "enabled")) {
            this.merchant = new MerchantSocket(this);
            this.merchant.setup();
        }

        // Setup Silent Rate Bonus.
        path = "socketing.failure.silent-rate-bonus.";
        if (!cfg.contains(path + "by-item-sockets")) {
            cfg.set(path + "by-item-sockets.0", 7);
            cfg.set(path + "by-item-sockets.1", 5);
            cfg.set(path + "by-item-sockets.2", 3);
        }
        this.silentRateBonusMap = new TreeMap<>();
        for (String sLvl : cfg.getSection(path + "by-item-sockets")) {
            int lvl = StringUT.getInteger(sLvl, -1);
            if (lvl < 0) continue;

            this.silentRateBonusMap.put(lvl, cfg.getInt(path + "by-item-sockets." + sLvl));
        }
        this.userSilentRateBonusMap = new HashMap<>();

        this.cfg.saveChanges();
    }

    @Override
    public void unload() {
        if (this.merchant != null) {
            this.merchant.shutdown();
            this.merchant = null;
        }
        if (this.guiUser != null) {
            this.guiUser.shutdown();
            this.guiUser = null;
        }
        if (this.userSilentRateBonusMap != null) {
            this.userSilentRateBonusMap.clear();
            this.userSilentRateBonusMap = null;
        }
        if (this.silentRateBonusMap != null) {
            this.silentRateBonusMap.clear();
            this.silentRateBonusMap = null;
        }
        this.animation = null;

        super.unload();
    }

    @Override
    protected boolean onDragDrop(
            @NotNull Player player,
            @NotNull ItemStack src,
            @NotNull ItemStack target,
            @NotNull I mItem,
            @NotNull InventoryClickEvent e) {

        if (!player.hasPermission(Perms.getSocketGuiUser(this))) {
            plugin.lang().Error_NoPerm.send(player);
            return false;
        }

        String id = mItem.getId();
        if (!this.allowDuplicatedSockets && this.hasSocketItem(target, id)) {
            plugin.lang().Module_Item_Socketing_Error_AlreadyHave
                    .replace("%item%", ItemUT.getItemName(target))
                    .send(player);
            return false;
        }

        if (plugin.getInteractionManager().isInAction(player)) {
            plugin.lang().Module_Item_Socketing_Error_InAction.send(player);
            return false;
        }

        VersionManager.getCompat().setCursor(e, null);
        this.startSocketing(player, target, src);
        src.setAmount(0);
        return true;
    }

    @NotNull
    public final SocketAttribute.Type getSocketType() {
        if (this.socketType == null) {
            this.socketType = SocketAttribute.Type.getByModule(this);
        }
        return this.socketType;
    }

    @Nullable
    public MerchantSocket getMerchant() {
        return merchant;
    }

    @Nullable
    public AnimatedSuccessBar.Builder getAnimation() {
        if (animation == null) return null;
        return animation.clone();
    }

    public final boolean isDestroyTargetOnFail() {
        return this.failDestroyTarget;
    }

    public final boolean isDestroySourceOnFail() {
        return this.failDestroySource;
    }

    public final boolean isWipeSocketsOnFail() {
        return this.failWipeSockets;
    }

    public final int getSilentRateBonusCap() {
        return this.silentRateBonusCap;
    }

    protected final int getSilentRateBonusBySockets(int lvl) {
        Map.Entry<Integer, Integer> e = this.silentRateBonusMap.floorEntry(lvl);
        if (e == null) return 0;

        return e.getValue();
    }

    public final void addSilentRateBonus(@NotNull Player player, int lvl) {
        int stack = this.getSilentRateBonusBySockets(lvl);
        if (stack == 0) return;

        String key = player.getName();
        if (this.userSilentRateBonusMap.containsKey(key)) {
            stack += this.userSilentRateBonusMap.get(key);
        }
        this.userSilentRateBonusMap.put(key, Math.min(this.getSilentRateBonusCap(), stack));
    }

    public final int getSilentRateBonus(@NotNull Player player) {
        return this.userSilentRateBonusMap.getOrDefault(player.getName(), 0);
    }

    public final void clearSilentRateBonus(@NotNull Player player) {
        this.userSilentRateBonusMap.remove(player.getName());
    }

    @NotNull
    public final List<Entry<I, Integer>> getItemSockets(@NotNull ItemStack item) {
        List<Entry<I, Integer>> sockets = new ArrayList<>();

        ItemMeta meta = item.getItemMeta();

        for (SocketAttribute socket : ItemStats.getSockets(this.getSocketType())) {
            for (String[] v : this.getFilledSocketKeys(meta, socket.getId()).values()) {
                String      id    = v[0];
                @Nullable I mItem = this.getItemById(id);
                if (mItem == null) continue;

                int lvl = StringUT.getInteger(v[1], -1);
                if (lvl < 0) continue;

                Entry<I, Integer> entry = new AbstractMap.SimpleEntry<>(mItem, lvl);
                sockets.add(entry);
            }
        }

        return sockets;
    }

    public final boolean hasSocketItem(@NotNull ItemStack item, @NotNull String itemId) {
        ItemMeta     meta = item.getItemMeta();
        List<String> lore = meta != null && meta.hasLore() ? meta.getLore() : null;
        for (SocketAttribute socket : ItemStats.getSockets(this.getSocketType())) {
            for (String[] values : this.getFilledSocketKeys(meta, socket.getId()).values()) {
                String id = values[0];
                if (id.equalsIgnoreCase(itemId)) {
                    return true;
                }
            }
        }
        return false;
    }
	
	/*public final boolean hasFreeSlot(@NotNull ItemStack item, @NotNull String socketId) {
		return this.getEmptySlotIndex(item, socketId) != -1;
	}
	
	public final int getEmptySlotIndex(@NotNull ItemStack item, @NotNull String socketId) {
		SocketAttribute socket = ItemStats.getSocket(this.getSocketType(), socketId);
		if (socket == null) return -1;
		
		return socket.getFirstEmptyIndex(item);
	}*/

    public final boolean hasSocketItems(@NotNull ItemStack item, @NotNull String socketCat) {
        return this.getFilledSocketsAmount(item, socketCat) > 0;
    }

    @NotNull
    public final Map<Integer, String[]> getFilledSocketKeys(
            @NotNull ItemStack item, @NotNull String socketId) {
        Map<Integer, String[]> sockets = new TreeMap<>();

        SocketAttribute socket = ItemStats.getSocket(this.getSocketType(), socketId);
        if (socket != null) {
            int total = socket.getAmount(item); // Total amount of empty and filled
            for (int index = 0; index < total; index++) {
                String[] values = socket.getRaw(item, index);
                if (values != null && !socket.isEmpty(values)) {
                    sockets.put(index, values); // Int key will represent valid index for filled socket
                }
            }
        }

        return sockets;
    }

    @NotNull
    public final Map<Integer, String[]> getFilledSocketKeys(ItemMeta meta, String socketId) {
        Map<Integer, String[]> sockets = new TreeMap<>();

        SocketAttribute socket = ItemStats.getSocket(this.getSocketType(), socketId);
        if (socket != null) {
            int total = socket.getAmount(meta); // Total amount of empty and filled
            for (int index = 0; index < total; index++) {
                String[] values = socket.getRaw(meta, index);
                if (values != null && !socket.isEmpty(values)) {
                    sockets.put(index, values); // Int key will represent valid index for filled socket
                }
            }
        }

        return sockets;
    }

    public final int getFilledSocketsAmount(@NotNull ItemStack item, @NotNull String socketId) {
        SocketAttribute socket = ItemStats.getSocket(this.getSocketType(), socketId);
        return socket != null ? socket.getFilledAmount(item) : 0;
    }

    public final int getFreeSocketsAmount(@NotNull ItemStack item, @NotNull String socketId) {
        SocketAttribute socket = ItemStats.getSocket(this.getSocketType(), socketId);
        return socket != null ? socket.getEmptyAmount(item) : 0;
    }

    @NotNull
    public List<ItemStack> extractSocket(@NotNull ItemStack target, @NotNull String socketId, int index) {
        SocketAttribute socket = ItemStats.getSocket(this.getSocketType(), socketId);
        if (socket == null) {
            this.error("Attempt to extract invalid socket type: '" + socketId + "' !");
            return Collections.singletonList(target);
        }

        // No socket at provided index, or it's already empty.
        String[] value = socket.getRaw(target, index);
        if (value == null || socket.isEmpty(value)) return Collections.singletonList(target);

        I               sock        = this.getItemById(value[0]);
        ItemStack       mSockItem   = sock.create(Integer.valueOf(value[1]));
        List<ItemStack> returnItems = new ArrayList<>();
//        List<Entry<I, Integer>> sockets     = this.getItemSockets(target);
//        sockets.forEach(entry -> returnItems.add(entry.getKey().create(entry.getValue(), 1)));
        returnItems.add(mSockItem);

        socket.add(target, socket.getDefaultValue(), index, -1);
        returnItems.add(0, target);
        return returnItems;
    }

    public final void startSocketing(
            @NotNull Player p, @NotNull ItemStack target, @NotNull ItemStack src) {

        this.splitDragItem(p, src, target);
        this.guiUser.open(p, target, src);
    }

    // ---------------------------------------------------------- //

    @NotNull
    public final ItemStack insertSocket(@NotNull ItemStack item, @NotNull ItemStack src) {
        ItemSocketRequirement socketReq = ItemRequirements.getItemRequirement(ItemSocketRequirement.class);
        if (socketReq == null) return item;

        String[] reqValue = socketReq.getRaw(src);
        if (reqValue == null || reqValue.length != 2) return item;

        String          socketId = reqValue[1];
        SocketAttribute socket   = ItemStats.getSocket(this.getSocketType(), socketId);
        if (socket == null) return item;

        int      index       = socket.getFirstEmptyIndex(item);
        String[] socketValue = new String[]{this.getItemId(src), String.valueOf(ItemStats.getLevel(src))};

        socket.add(item, socketValue, index, -1);
        return item;
    }
}
