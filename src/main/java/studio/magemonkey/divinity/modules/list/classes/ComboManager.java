package studio.magemonkey.divinity.modules.list.classes;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.compat.VersionManager;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.codex.manager.api.Loadable;
import studio.magemonkey.codex.manager.api.gui.GuiClick;
import studio.magemonkey.codex.manager.api.gui.GuiItem;
import studio.magemonkey.codex.manager.api.gui.NGUI;
import studio.magemonkey.codex.manager.api.task.ITask;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.MsgUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.manager.effects.IEffectType;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;
import studio.magemonkey.divinity.modules.list.classes.api.UserSkillData;
import studio.magemonkey.divinity.modules.list.classes.event.PlayerComboProcessEvent;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.utils.ItemUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ComboManager extends IListener<Divinity> implements Loadable {

    private ClassManager classManager;
    private JYML         cfg;

    private Set<String> allowedGroups;
    private int         comboLength;
    private long        comboCheckTime;
    private int         comboKeyTime;
    private ComboKey    comboStartKey;
    private ComboKey    comboStartAltKey;
    private String      comboKeyNameNext;
    private String      comboKeyNameSeparator;
    private String      comboSoundKeyClick;
    private String      comboSoundEmpty;

    private Set<String> comboStop;
    private ComboTask   comboTask;

    private Map<Player, ComboInfo> comboUser;

    private static String                   guiTitle      = null;
    private static int                      guiSize       = 54;
    private static int[]                    guiComboSlots = new int[1];
    private static Map<ComboKey, ItemStack> guiKeyCache   = new HashMap<>();

    ComboManager(@NotNull ClassManager classManager) {
        super(classManager.plugin);
        this.classManager = classManager;
    }

    @Override
    public void setup() {
        this.comboStop = new HashSet<>();
        this.comboUser = new WeakHashMap<>();
        try {
            this.cfg = JYML.loadOrExtract(plugin, classManager.getPath() + "combo.yml");
        } catch (InvalidConfigurationException e) {
            this.plugin.error(
                    "Failed to load combo config (" + classManager.getPath() + "combo.yml): Configuration error");
            e.printStackTrace();
            shutdown();
            return;
        }

        String path = "settings.";
        if (!cfg.contains(path + "allowed-items")) {
            cfg.set(path + "weapon-only", null);
            cfg.set(path + "allowed-items", Arrays.asList("WEAPON"));
        }
        this.allowedGroups = cfg.getStringSet(path + "allowed-items")
                .stream().map(String::toLowerCase).collect(Collectors.toSet());

        path = "settings.combo.";
        this.comboLength = Math.min(ComboKey.values().length, cfg.getInt(path + "length"));
        this.comboCheckTime = cfg.getInt(path + "check-time", 5);
        this.comboKeyTime = cfg.getInt(path + "key-time", 30);

        path = "settings.combo.keys.";
        try {
            this.comboStartKey = ComboKey.valueOf(cfg.getString(path + "start-main", "RIGHT").toUpperCase());
            this.comboStartAltKey = ComboKey.valueOf(cfg.getString(path + "start-alternative", "LEFT").toUpperCase());
        } catch (IllegalArgumentException ex) {
            this.comboStartKey = ComboKey.RIGHT;
            this.comboStartAltKey = ComboKey.LEFT;
        }

        path = "settings.combo.keys.names.";
        this.comboKeyNameNext = StringUT.color(cfg.getString(path + "next", "&8&l?"));
        this.comboKeyNameSeparator = StringUT.color(cfg.getString(path + "separator", " &f&l- "));
        for (ComboKey key : ComboKey.values()) {
            String name = cfg.getString(path + key.name());
            if (name == null) continue;
            key.setName(name);
        }

        path = "settings.combo.sounds.";
        this.comboSoundKeyClick = cfg.getString(path + "key-click", "UI_BUTTON_CLICK");
        this.comboSoundEmpty = cfg.getString(path + "combo-empty", "ENTITY_VILLAGER_NO");

        this.comboTask = new ComboTask();
        this.comboTask.start();

        this.registerListeners();
        this.cfg.saveChanges();
    }

    @Override
    public void shutdown() {
        if (this.comboTask != null) {
            this.comboTask.stop();
            this.comboTask = null;
        }
        if (this.comboUser != null) {
            this.comboUser.clear();
            this.comboUser = null;
        }
        if (this.comboStop != null) {
            this.comboStop.clear();
            this.comboStop = null;
        }
        this.unregisterListeners();
    }

    public void openGUI(@NotNull Player player, @NotNull UserSkillData data) {
        if (guiTitle == null) {
            guiTitle = cfg.getString("gui.title", "Skill Combo");
            guiSize = cfg.getInt("gui.size", 54);
            guiComboSlots = cfg.getIntArray("gui.combo-slots");
        }

        ComboManager.GUI gui = new ComboManager.GUI(guiTitle, guiSize, data);
        gui.open(player, 1);
    }

    @Nullable
    public ComboInfo getCombo(@NotNull Player player) {
        return this.comboUser.get(player);
    }

    private void printComboInfo(@NotNull Player player, @NotNull ComboInfo comboInfo) {
        StringBuilder msg = new StringBuilder();

        for (ComboKey key : comboInfo.getKeys()) {
            if (key == null) continue;

            if (msg.length() > 0) {
                msg.append(this.comboKeyNameSeparator);
            }
            msg.append(key.getName());
        }
        if (!comboInfo.isComplete()) {
            int max = this.comboLength;
            int has = comboInfo.getCount();

            for (int i = 0; i < (max - has); i++) {
                msg.append(this.comboKeyNameSeparator).append(this.comboKeyNameNext);
            }
        }
        MsgUT.sendActionBar(player, StringUT.color(msg.toString()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClassCombo(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;

        Block     block = e.getClickedBlock();
        ItemStack wpn   = e.getItem();

        if (!ItemUT.isAir(wpn) && e.useItemInHand() == Result.DENY) return;
        if (wpn == null || !this.isAllowedItem(wpn)) return;

        if (block != null && (block.getType().isInteractable() || e.useInteractedBlock() == Result.DENY)) {
            return;
        }

        Player player = e.getPlayer();

        if (!ItemUT.isAir(wpn) && wpn.getType() == Material.FISHING_ROD
                && this.getCombo(player) == null
                && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        ComboKey key = ComboKey.getFromAction(e.getAction(), player.isSneaking());
        if (key == null) return;

        if (this.processCombo(player, key)) {
            ItemStack off = player.getInventory().getItemInOffHand();
            if (ItemUT.isAir(off) || off.getType() != Material.SHIELD) {
                e.setUseItemInHand(Result.DENY);
                e.setUseInteractedBlock(Result.DENY);
            }
            if (!ItemUT.isAir(wpn) && wpn.getType() == Material.FISHING_ROD
                    && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                this.stopCombo(e.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClassComboQ(PlayerDropItemEvent e) {
        Player   player = e.getPlayer();
        ComboKey key    = ComboKey.Q;

        if (this.processCombo(player, key)) {
            e.setCancelled(true);
            this.stopCombo(player); // Fix drop animation LMB
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClassComboF(PlayerSwapHandItemsEvent e) {
        Player   player = e.getPlayer();
        ComboKey key    = ComboKey.F;

        if (this.processCombo(player, key)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClassComboFish(PlayerFishEvent e) {
        this.stopCombo(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClassCombo2(PlayerInteractEntityEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) return;

        Player    player = e.getPlayer();
        ItemStack wpn    = player.getInventory().getItemInMainHand();
        if (ItemUT.isAir(wpn) || !this.isAllowedItem(wpn)) {
            return;
        }

        ComboKey key = ComboKey.getFromAction(Action.RIGHT_CLICK_BLOCK, player.isSneaking());
        if (key == null) return;

        this.processCombo(player, key);

        this.stopCombo(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onClassCombo3(EntityDamageByEntityEvent e) {
        Entity e2 = e.getDamager();
        if (!(e2 instanceof Player)) return;

        Player    player = (Player) e.getDamager();
        ItemStack wpn    = player.getInventory().getItemInMainHand();
        if (ItemUT.isAir(wpn) || !this.isAllowedItem(wpn)) {
            return;
        }

        ComboKey key = ComboKey.getFromAction(Action.LEFT_CLICK_BLOCK, player.isSneaking());
        if (key == null) return;

        this.processCombo(player, key);
        this.stopCombo(player);
    }

    public void playAttackAnim(@NotNull Player player, boolean main) {
        int animId = main ? 0 : 3;
        this.stopCombo(player);
        VersionManager.getNms().sendAttackPacket(player, animId);
    }

    public void stopCombo(@NotNull Player p) {
        this.comboStop.add(p.getName());
    }

    private boolean isAllowedItem(@NotNull ItemStack item) {
        return this.allowedGroups.contains(JStrings.MASK_ANY) ||
                ItemUtils.compareItemGroup(item, this.allowedGroups.toArray(new String[this.allowedGroups.size()]));
    }

    private boolean isAltComboItem(@Nullable ItemStack wpn) {
        if (wpn == null) return false;

        if (ItemUtils.isBow(wpn)
                || wpn.getType() == Material.FISHING_ROD
                || wpn.getType() == Material.TRIDENT) {
            return true;
        }

        return false;
    }

    private boolean processCombo(@NotNull Player player, @NotNull ComboKey key) {
        EntityStats es = EntityStats.get(player);
        if (es.hasEffect(IEffectType.CONTROL_STUN) || es.hasEffect(IEffectType.SILENCE)) {
            // TODO Message
            return false;
        }

        UserClassData classData = this.classManager.getUserData(player);
        if (classData == null) return false;

        if (this.comboStop.contains(player.getName())) {
            this.comboStop.remove(player.getName());
            return false;
        }

        ComboInfo comboBuilder = this.getCombo(player);
        if (comboBuilder == null) {
            if (this.comboLength > 1) {
                ComboKey  start = this.comboStartKey;
                ItemStack wpn   = player.getInventory().getItemInMainHand();
                if (this.isAltComboItem(wpn)) {
                    start = this.comboStartAltKey;
                }

                if (key != start) return false;
            }
            comboBuilder = new ComboInfo();
        } else {
            if (!comboBuilder.isExpired() && comboBuilder.isComplete()) return false;
        }

        PlayerComboProcessEvent e = new PlayerComboProcessEvent(player, classData, key, comboBuilder);
        plugin.getPluginManager().callEvent(e);
        if (e.isCancelled()) return false;

        if (comboBuilder.addKey(key, this.comboKeyTime)) {
            MsgUT.sound(player, this.comboSoundKeyClick);
            this.printComboInfo(player, comboBuilder);
        }
        this.comboUser.put(player, comboBuilder);
        return true;
    }

    public static enum ComboKey {

        LEFT("L"),
        RIGHT("R"),
        SHIFT_LEFT("SL"),
        SHIFT_RIGHT("SR"),
        Q("Q"),
        F("F"),
        ;

        private String name;

        private ComboKey(@NotNull String name) {
            this.setName(name);
        }

        public void setName(@NotNull String name) {
            this.name = StringUT.color(name);
        }

        @NotNull
        public String getName() {
            return this.name;
        }

        @Nullable
        public static ComboKey getFromAction(@NotNull Action a, boolean sneak) {
            if (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK) {
                return sneak ? SHIFT_LEFT : LEFT;
            } else if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
                return sneak ? SHIFT_RIGHT : RIGHT;
            }
            return null;
        }
    }

    public class ComboInfo {

        private int        count;
        private ComboKey[] keys;
        private long       ticksLeft;

        public ComboInfo() {
            this.count = 0;
            this.ticksLeft = 0;
            this.keys = new ComboKey[ComboManager.this.comboLength];
        }

        public boolean addKey(@NotNull ComboKey key, int ticks) {
            if (this.isComplete() || this.isExpired()) return false;
            this.keys[this.count++] = key;
            this.ticksLeft = ticks;

            return true;
        }

        public boolean isExpired() {
            return this.count > 0 && this.ticksLeft <= 0;
        }

        public void tick(long lvlComboCheckTime) {
            this.ticksLeft = Math.max(0L, this.ticksLeft - lvlComboCheckTime);
        }

        public ComboKey[] getKeys() {
            return this.keys;
        }

        public int getCount() {
            return this.count;
        }

        public boolean isComplete() {
            return this.count == ComboManager.this.comboLength;
        }
    }

    class ComboTask extends ITask<Divinity> {

        public ComboTask() {
            super(ComboManager.this.plugin, comboCheckTime, true);
        }

        @Override
        public void action() {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player == null) continue;

                ComboInfo comboInfo = getCombo(player);
                if (comboInfo == null) {
                    continue;
                }
                comboInfo.tick(comboCheckTime);
                if (comboInfo.isExpired()) {
                    plugin.lang().Classes_Skill_Cast_Cancel.send(player);
                    comboUser.remove(player);
                    continue;
                }
                if (comboInfo.isComplete()) {
                    comboUser.remove(player);

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!classManager.castSkill(player, comboInfo.getKeys())) {
                            MsgUT.sound(player, comboSoundEmpty);
                        }
                    });

                    continue;
                }
            }
        }
    }

    class GUI extends NGUI<Divinity> {

        private UserSkillData data;

        public GUI(@NotNull String title, int size, @NotNull UserSkillData data) {
            super(ComboManager.this.plugin, title, size);
            this.data = data;

            GuiClick click = new GuiClick() {
                @Override
                public void click(@NotNull Player p, @Nullable Enum<?> type, @NotNull InventoryClickEvent e) {
                    if (type == null) return;

                    Inventory inv = e.getInventory();
                    if (type.getClass().equals(GUIComboType.class)) {
                        GUIComboType type2 = (GUIComboType) type;

                        switch (type2) {
                            case COMBO_CANCEL: {
                                classManager.openSkillsGUI(p);
                                break;
                            }
                            case COMBO_CLEAR: {
                                for (int slot : guiComboSlots) {
                                    inv.setItem(slot, null);
                                }
                                break;
                            }
                            case COMBO_F:
                            case COMBO_Q:
                            case COMBO_SHIFT_LEFT:
                            case COMBO_SHIFT_RIGHT:
                            case COMBO_LEFT:
                            case COMBO_RIGHT: {
                                for (int comboSlot : guiComboSlots) {
                                    // Find the latest free slot.
                                    ItemStack c = inv.getItem(comboSlot);
                                    if (c != null && c.getType() != Material.AIR) continue;

                                    // Get clicked combo button.
                                    GuiItem button = GUI.this.getButton(p, e.getRawSlot());
                                    if (button == null) return;

                                    // Add slot reference for the original combo button.
                                    // This adds combo button to skill combo display,
                                    // so we can access original button by this reference.
                                    GuiItem buttonClone = new GuiItem(button);
                                    buttonClone.setSlots(new int[]{comboSlot});
                                    GUI.this.addButton(buttonClone);

                                    // Display current combo button.
                                    inv.setItem(comboSlot, e.getCurrentItem());
                                    break;
                                }

                                break;
                            }
                            case COMBO_SAVE: {
                                ComboKey[] comboBuilder = new ComboKey[comboLength];
                                int        count        = 0;

                                for (int slot : guiComboSlots) {
                                    // Get original GUI combo button by created reference above.
                                    GuiItem comboButton = GUI.this.getButton(p, slot);
                                    if (comboButton == null) {
                                        //p.sendMessage("Null Button at " + slot);
                                        continue;
                                    }

                                    Enum<?> gType = comboButton.getType();
                                    if (gType == null || !gType.getClass().equals(GUIComboType.class)) return;

                                    ComboKey key = ((GUIComboType) gType).getAssignedKey();
                                    if (key == null) continue;

                                    comboBuilder[count++] = key;
                                    if (count == comboLength) break;
                                }

                                if (count > 0 && count != comboLength) {
                                    // TODO p.sendMessage("Not full combo!");
                                    return;
                                }

                                data.setCombo(comboBuilder);
                                p.closeInventory();
                                classManager.openSkillsGUI(p);

                                break;
                            }
                            default: {
                                break;
                            }
                        }
                    }
                }
            };


            for (String sId : cfg.getSection("gui.content")) {
                String  path = "gui.content." + sId;
                GuiItem gi   = cfg.getGuiItem(path, GUIComboType.class);
                if (gi == null) continue;

                Enum<?> eType = gi.getType();
                if (eType != null && eType.getClass().equals(GUIComboType.class)) {
                    GUIComboType btnType = (GUIComboType) eType;
                    ComboKey     key     = btnType.getAssignedKey();
                    if (key != null) {
                        guiKeyCache.putIfAbsent(key, gi.getItem());
                    }
                    gi.setClick(click);
                }
                this.addButton(gi);
            }
        }

        @Override
        protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {
            // Show current skill combo
            int count = 0;
            for (ComboKey key : this.data.getCombo()) {
                ItemStack item = guiKeyCache.get(key);
                if (item == null) continue;

                inv.setItem(guiComboSlots[count++], item);
            }
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
        public boolean destroyWhenNoViewers() {
            return true;
        }
    }

    static enum GUIComboType {
        COMBO_LEFT(ComboKey.LEFT),
        COMBO_RIGHT(ComboKey.RIGHT),
        COMBO_SHIFT_LEFT(ComboKey.SHIFT_LEFT),
        COMBO_SHIFT_RIGHT(ComboKey.SHIFT_RIGHT),
        COMBO_Q(ComboKey.Q),
        COMBO_F(ComboKey.F),
        COMBO_SAVE,
        COMBO_CANCEL,
        COMBO_CLEAR,
        ;

        private ComboKey key;

        private GUIComboType() {
            this(null);
        }

        private GUIComboType(@Nullable ComboKey key) {
            this.key = key;
        }

        @Nullable
        public ComboKey getAssignedKey() {
            return this.key;
        }
    }
}
