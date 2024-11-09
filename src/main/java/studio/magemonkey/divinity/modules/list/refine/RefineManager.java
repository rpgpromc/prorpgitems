package studio.magemonkey.divinity.modules.list.refine;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.gui.*;
import studio.magemonkey.codex.util.*;
import studio.magemonkey.codex.util.actions.ActionManipulator;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.RatedItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.list.fortify.FortifyManager;
import studio.magemonkey.divinity.modules.list.refine.RefineManager.RefineItem;
import studio.magemonkey.divinity.modules.list.refine.command.DowngradeCmd;
import studio.magemonkey.divinity.modules.list.refine.command.RefineCmd;
import studio.magemonkey.divinity.stats.bonus.BonusCalculator;
import studio.magemonkey.divinity.stats.bonus.BonusMap;
import studio.magemonkey.divinity.stats.bonus.StatBonus;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.ItemTags;
import studio.magemonkey.divinity.stats.items.api.ItemLoreStat;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;
import studio.magemonkey.divinity.utils.ItemUtils;
import studio.magemonkey.divinity.utils.LoreUT;

import java.util.*;
import java.util.function.BiFunction;

public class RefineManager extends QModuleDrop<RefineItem> {

    private static final NamespacedKey             TAG_REFINE_LVL     =
            new NamespacedKey(Divinity.getInstance(), "REFINE_LVL");
    private static final String                    TAG_REFINE_LORE    = "refine_lore_global";
    private static final String                    TAG_REFINE_NAME    = "refine_name_display";
    private final        int                       silentRateBonusCap = 20; // TODO INIT
    private              int                       refineMaxLevel;
    private              TreeMap<Integer, Integer> refineDowngradeMap;
    private              ActionManipulator         actionsSuccess;
    private              ActionManipulator         actionsFailure;
    private              boolean                   formatNameAsPrefix;
    private              TreeMap<Integer, String>  formatNamePrefix;
    private              String                    formatLoreStat;
    private              List<String>              formatLoreText;
    private              TreeMap<Integer, Integer> silentRateBonusMap;
    private              Map<String, Integer>      userSilentRateBonusMap;
    private              GUI                       gui;

    public RefineManager(@NotNull Divinity plugin) {
        super(plugin, RefineItem.class);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.REFINE;
    }

    @Override
    @NotNull
    public String version() {
        return "1.81";
    }

    @Override
    public void setup() {
        this.moduleCommand.addSubCommand(new RefineCmd(this));
        this.moduleCommand.addSubCommand(new DowngradeCmd(this));

        String path = "refine.fail-silent-rate-bonus.";
        if (!cfg.contains(path + "by-refine-level")) {
            cfg.set(path + "by-refine-level.0", 10);
            cfg.set(path + "by-refine-level.1", 9);
            cfg.set(path + "by-refine-level.2", 8);
        }

        this.silentRateBonusMap = new TreeMap<>();
        for (String sLvl : cfg.getSection(path + "by-refine-level")) {
            int lvl = StringUT.getInteger(sLvl, -1);
            if (lvl < 0) continue;

            this.silentRateBonusMap.put(lvl, cfg.getInt(path + "by-refine-level." + sLvl));
        }
        this.userSilentRateBonusMap = new HashMap<>();

        this.actionsSuccess = new ActionManipulator(plugin, cfg, "refine.actions-on-success");
        this.actionsFailure = new ActionManipulator(plugin, cfg, "refine.actions-on-failure");

        path = "refine.";
        this.refineMaxLevel = cfg.getInt(path + "max-level", 15);
        this.refineDowngradeMap = new TreeMap<>();
        for (String sLvl : cfg.getSection(path + "fail-level-downgrade")) {
            int lvl = StringUT.getInteger(sLvl, -1);
            if (lvl < 1) continue;

            int down = cfg.getInt(path + "fail-level-downgrade." + sLvl);
            this.refineDowngradeMap.put(lvl, down);
        }

        path = "format.item-name.";
        this.formatNameAsPrefix = cfg.getBoolean(path + "as-prefix");
        this.formatNamePrefix = new TreeMap<>();
        for (String sLvl : cfg.getSection(path + "format-by-level")) {
            int lvl = StringUT.getInteger(sLvl, -1);
            if (lvl <= 0) continue;

            this.formatNamePrefix.put(lvl,
                    StringUT.color(cfg.getString(path + "format-by-level." + sLvl, "+%level% ")));
        }

        path = "format.item-lore.";
        this.formatLoreStat = StringUT.color(cfg.getString(path + "format", " &8(&7%+%%amount%&8)"));
        this.formatLoreText = StringUT.color(cfg.getStringList(path + "text"));

        this.cfg.saveChanges();

        this.gui = this.new GUI();
    }

    @Override
    public void shutdown() {
        if (this.gui != null) {
            this.gui.shutdown();
            this.gui = null;
        }
        if (this.userSilentRateBonusMap != null) {
            this.userSilentRateBonusMap.clear();
            this.userSilentRateBonusMap = null;
        }
        if (this.silentRateBonusMap != null) {
            this.silentRateBonusMap.clear();
            this.silentRateBonusMap = null;
        }
        if (this.refineDowngradeMap != null) {
            this.refineDowngradeMap.clear();
            this.refineDowngradeMap = null;
        }
        if (this.formatLoreText != null) {
            this.formatLoreText.clear();
            this.formatLoreText = null;
        }
        this.actionsFailure = null;
        this.actionsSuccess = null;
    }

    // -------------------------------------------------------

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
        String key = player.getName();
        if (this.userSilentRateBonusMap.containsKey(key)) {
            return this.userSilentRateBonusMap.get(key);
        }
        return 0;
    }

    public final void clearSilentRateBonus(@NotNull Player player) {
        this.userSilentRateBonusMap.remove(player.getName());
    }

    public boolean canRefine(@NotNull ItemStack item) {
        if (!item.hasItemMeta()) return false;
        return ItemUtils.isArmor(item) || ItemUtils.isWeapon(item);
    }

    /**
     * @param stone  Enchantment Stone
     * @param target Applicable item
     * @return Returns true if stone can be applied to this item.
     */
    public boolean isApplicable(@NotNull RefineItem stone, @NotNull ItemStack target) {
        String hasStone = this.getRefineStoneId(target);
        return hasStone == null || stone.getId().equalsIgnoreCase(hasStone);
    }

    public boolean isRefined(@NotNull ItemStack item) {
        return this.getRefineStoneId(item) != null;
    }

    public int getRefineLevel(@NotNull ItemStack item) {
        String tag = DataUT.getStringData(item, TAG_REFINE_LVL);
        if (tag != null) {
            return StringUT.getInteger(tag.split(":")[1], 0);
        }
        return 0;
    }

    @Nullable
    public String getRefineStoneId(@NotNull ItemStack item) {
        String tag = DataUT.getStringData(item, TAG_REFINE_LVL);
        if (tag != null) {
            return tag.split(":")[0];
        }
        return null;
    }

    @NotNull
    public BiFunction<Boolean, Double, Double> getRefinedBonus(@NotNull ItemStack item, @NotNull ItemLoreStat<?> stat) {
        BiFunction<Boolean, Double, Double> func = (isBonus, result) -> result;

        // Get item refine stone ID
        String stoneId = this.getRefineStoneId(item);
        if (stoneId == null) return func;

        // Check if it's valid
        RefineItem stone = this.getItemById(stoneId);
        if (stone == null) return func;

        int refineLvl = this.getRefineLevel(item);

        BonusMap bMap = stone.getRefineBonusMap(refineLvl);
        if (bMap == null) return func;

        BiFunction<Boolean, Double, Double> bFunc = bMap.getBonus(stat);
        if (bFunc != null) {
            return bFunc;
        }

        return func;
    }

    public void refineItem(@NotNull ItemStack item, @NotNull RefineItem stone) {
        if (!this.canRefine(item)) return;

        int lvl = this.getRefineLevel(item);
        if (lvl >= this.refineMaxLevel) return;

        this.resetFines(item);
        this.addFines(item, stone, lvl + 1);
    }

    public void downgradeItem(@NotNull ItemStack item, boolean force) {
        String stoneId = this.getRefineStoneId(item);
        if (stoneId == null) return;

        RefineItem stone = this.getItemById(stoneId);
        if (stone == null) return;

        int                         refineLvl = this.getRefineLevel(item);
        Map.Entry<Integer, Integer> entry     = this.refineDowngradeMap.floorEntry(refineLvl);

        int downAmount = entry != null ? entry.getValue() : 1;
        if (downAmount > refineLvl) downAmount = refineLvl;
        if (downAmount <= 0 && force) {
            downAmount = 1;
        }

        int downLvl = refineLvl - downAmount;

        this.resetFines(item);
        if (downLvl > 0) {
            this.addFines(item, stone, downLvl);
        }
    }

    @NotNull
    public String getNameWithoutLevel(@NotNull ItemStack item, @NotNull String name) {
        String storedName = ItemUT.getNameTag(item, TAG_REFINE_NAME);
        return storedName == null ? name : name.replace(name.replace(storedName, ""), "");
    }

    @NotNull
    public String getNameWithLevel(@NotNull ItemStack item, int lvl) {
        String name = ItemUT.getItemName(item);

        Map.Entry<Integer, String> entry = this.formatNamePrefix.floorEntry(lvl);
        if (entry == null) return name;

        String format = entry.getValue().replace("%level%", String.valueOf(lvl));
        if (this.formatNameAsPrefix) {
            return StringUT.colorFix(format + name);
        } else {
            return StringUT.colorFix(name + format);
        }
    }

    /**
     * @param item      An refined ItemStack
     * @param stone     EnchantmentStone (if level is greater than 0)
     * @param refineLvl Enchantment Stone level
     */
    private void setRefineLevel(
            @NotNull ItemStack item,
            @Nullable RefineItem stone,
            int refineLvl) {

        if (refineLvl > 0 && stone != null) {
            DataUT.setData(item, TAG_REFINE_LVL, new StringBuilder()
                    .append(stone.getId())
                    .append(":")
                    .append(refineLvl));
        } else {
            DataUT.removeData(item, TAG_REFINE_LVL);
        }
    }

    private void addFines(@NotNull ItemStack item, @NotNull RefineItem stone, int refineLvl) {
        BonusMap bMap = stone.getRefineBonusMap(refineLvl);
        if (bMap == null) return;

        FortifyManager fortifyManager = plugin.getModuleCache().getFortifyManager();
        if (fortifyManager != null) {
            fortifyManager.deformatItemName(item);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.getLore();
        if (lore == null) return;

        String        preName = ItemUT.getItemName(item);
        String        name    = this.getNameWithLevel(item, refineLvl);
        StringBuilder loreTag = new StringBuilder();

        if (!this.formatLoreText.isEmpty()) {
            String       stoneName = ItemUT.getItemName(stone.create(refineLvl));
            List<String> lore2     = new ArrayList<>();
            for (String fLine : this.formatLoreText) {
                if (fLine.equalsIgnoreCase(ItemTags.PLACEHOLDER_ITEM_LORE)) {
                    for (String metaLine : lore) {
                        lore2.add(metaLine);
                    }
                } else {
                    String line = StringUT.color(fLine.replace("%stone%", stoneName));
                    lore2.add(line);

                    if (loreTag.length() > 0) loreTag.append(LoreUT.TAG_SPLITTER);
                    loreTag.append(line);
                }
            }
            lore.clear();
            lore.addAll(lore2);
        }

        Map<ItemLoreStat<?>, Double> refineValues = new HashMap<>();
        bMap.getStatBonuses().forEach((bStat, bFunc) -> refineValues.put(bStat,
                BonusCalculator.SIMPLE_BONUS.apply(bStat.getTotal(item, null), Collections.singletonList(bFunc))));
        bMap.getDamageBonuses().forEach((bStat, bFunc) -> refineValues.put(bStat,
                BonusCalculator.SIMPLE_BONUS.apply(bStat.getTotal(item, null)[1], Collections.singletonList(bFunc))));
        bMap.getDefenseBonuses().forEach((bStat, bFunc) -> refineValues.put(bStat,
                BonusCalculator.SIMPLE_BONUS.apply(bStat.getTotal(item, null), Collections.singletonList(bFunc))));

        // Update item description with refine values
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);

        // Fix NBT attributes in case if refine modifies them
        this.setRefineLevel(item, stone, refineLvl);
        ItemStats.updateVanillaAttributes(item, null);
        ItemUT.addNameTag(item, TAG_REFINE_NAME, preName);
        ItemUT.addLoreTag(item, TAG_REFINE_LORE, loreTag.toString());

        if (fortifyManager != null) {
            fortifyManager.formatItemName(item);
        }
    }

    @NotNull
    private void resetFines(@NotNull ItemStack item) {
        FortifyManager fortifyManager = plugin.getModuleCache().getFortifyManager();
        if (fortifyManager != null) {
            fortifyManager.deformatItemName(item);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.getLore();
        if (lore == null) return;

        String stoneId = this.getRefineStoneId(item);
        if (stoneId == null) return;

        RefineItem stone = this.getItemById(stoneId);
        if (stone == null) return;

        String storedLore = ItemUT.getLoreTag(item, TAG_REFINE_LORE);
        if (storedLore != null) {
            String[] lines = storedLore.split(LoreUT.TAG_SPLITTER);
            for (String line : lines) {
                lore.remove(line);
            }
        }

        meta.setDisplayName(this.getNameWithoutLevel(item, meta.getDisplayName()));
        meta.setLore(lore);
        item.setItemMeta(meta);

        // Remove ALL possible refined stats even if it was removed from RefineStone config
        ItemStats.getStats().forEach((stat) -> {
            if (stat instanceof SimpleStat) {
                SimpleStat simpleStat = (SimpleStat) stat;
                int        i          = 0;
                for (StatBonus bonus : simpleStat.getAllRaw(item)) {
                    int pos = simpleStat.getLoreIndex(item, i);
                    if (pos >= 0) simpleStat.add(item, bonus, pos);
                    i++;
                }
            }
        });

        ItemStats.getDamages().forEach((stat) -> {
            int i = 0;
            for (StatBonus bonus : stat.getAllRaw(item)) {
                int pos = stat.getLoreIndex(item, i);
                if (pos >= 0) stat.add(item, bonus, pos);
                i++;
            }
        });

        ItemStats.getDefenses().forEach((stat) -> {
            int i = 0;
            for (StatBonus bonus : stat.getAllRaw(item)) {
                int pos = stat.getLoreIndex(item, i);
                if (pos >= 0) stat.add(item, bonus, pos);
                i++;
            }
        });

        if (fortifyManager != null) {
            fortifyManager.formatItemName(item);
        }

        this.setRefineLevel(item, stone, 0);
        ItemStats.updateVanillaAttributes(item, null);
        ItemUT.delLoreTag(item, TAG_REFINE_LORE);
        ItemUT.delNameTag(item, TAG_REFINE_NAME);
    }

    @NotNull
    public String getFormatLoreStat(ItemStack item, ItemLoreStat<?> stat, double value) {
        List<BiFunction<Boolean, Double, Double>> bonuses = new ArrayList<>();
        bonuses.add((isPercent, input) -> isPercent ? input : input + value);
        bonuses.add(this.getRefinedBonus(item, stat));
        double diff = BonusCalculator.SIMPLE_FULL.apply(0D, bonuses) - value;
        if (diff == 0) return "";
        return this.formatLoreStat.replace("%amount%", (diff < 0 ? EngineCfg.LORE_CHAR_NEGATIVE : "")
                        + ChatColor.stripColor(EngineCfg.LORE_STYLE_DAMAGE_FORMAT_SINGLE
                        .replace("%value%", String.valueOf(Math.round(diff * 100) / 100.0))))
                .replace("%+%", diff > 0 ? EngineCfg.LORE_CHAR_POSITIVE : "");
    }

    @NotNull
    private ClickText getResultMessage(
            @NotNull ItemStack target,
            @NotNull ItemStack src,
            boolean isSuccess,
            boolean fSave) {

        String result = plugin.lang().Refine_Enchanting_Result_Total.normalizeLines();

        String stateTarget = plugin.lang().Refine_Enchanting_Result_State_Success.getMsg();
        if (!isSuccess) {
            if (fSave) {
                stateTarget = plugin.lang().Refine_Enchanting_Result_State_Saved.getMsg();
            } else {
                stateTarget = plugin.lang().Refine_Enchanting_Result_State_Downgraded.getMsg();
            }
        }

        String stateSource = plugin.lang().Refine_Enchanting_Result_State_Consumed.getMsg();
        result = result
                .replace("%state-target%", stateTarget)
                .replace("%state-source%", stateSource);

        ClickText text = new ClickText(result);
        text.createPlaceholder("%item-target%", ItemUT.getItemName(target)).showItem(target);
        text.createPlaceholder("%item-source%", ItemUT.getItemName(src)).showItem(src);

        return text;
    }

    // -------------------------------------------------------------------- //
    // EVENTS

    @Override
    protected boolean onDragDrop(
            @NotNull Player player,
            @NotNull ItemStack src,
            @NotNull ItemStack target,
            @NotNull RefineItem stone,
            @NotNull InventoryClickEvent e) {

        String hasStoneId = this.getRefineStoneId(target);
        if (hasStoneId != null) {
            RefineItem hasStone = this.getItemById(hasStoneId);
            if (hasStone != null && !hasStoneId.equalsIgnoreCase(stone.getId())) {
                plugin.lang().Refine_Enchanting_Error_WrongStone
                        .replace("%item%", ItemUT.getItemName(target))
                        .replace("%stone%", ItemUT.getItemName(hasStone.create(1)))
                        .send(player);
                return false;
            }
        }

        int refineLvl = this.getRefineLevel(target);
        if (refineLvl >= this.refineMaxLevel) {
            plugin.lang().Refine_Enchanting_Error_MaxLevel
                    .replace("%item%", ItemUT.getItemName(target))
                    .send(player);
            return false;
        }

        InventoryUtil.setCursor(e, null);
        this.splitDragItem(player, src, target);
        this.gui.open(player, target, src);
        src.setAmount(0);

        return true;
    }

    // C L A S S E S

    public class RefineItem extends RatedItem {

        private final TreeMap<Integer, BonusMap> refineBonusMap;

        public RefineItem(@NotNull Divinity plugin, @NotNull JYML cfg) {
            super(plugin, cfg, RefineManager.this);

            this.refineBonusMap = new TreeMap<>();
            for (String sLvl : cfg.getSection("refine-by-level")) {
                int lvl = StringUT.getInteger(sLvl, -1);
                if (lvl <= 0) continue;

                BonusMap bMap = new BonusMap();
                bMap.loadStats(cfg, "refine-by-level." + sLvl + ".item-stats");
                bMap.loadDamages(cfg, "refine-by-level." + sLvl + ".damage-types");
                bMap.loadDefenses(cfg, "refine-by-level." + sLvl + ".defense-types");
                if (bMap.isEmpty()) continue;

                this.refineBonusMap.put(lvl, bMap);
            }
        }

        @Nullable
        public BonusMap getRefineBonusMap(int lvl) {
            Map.Entry<Integer, BonusMap> e = this.refineBonusMap.floorEntry(lvl);
            if (e == null) return null;

            return e.getValue();
        }

        @Override
        @NotNull
        protected ItemStack build(int lvl, int uses, int suc) {
            ItemStack item = super.build(lvl, uses, suc);

            ItemMeta meta = item.getItemMeta();
            if (meta == null) return item;

            List<String> lore = meta.getLore();
            if (lore == null) return item;

            BonusMap bMap = this.getRefineBonusMap(lvl);
            if (bMap == null) return item;

            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                line = bMap.replacePlaceholders(line);
                lore.set(i, line);
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            return item;
        }
    }

    class GUI extends NGUI<Divinity> {

        protected int itemSlot;
        protected int sourceSlot;
        protected int resultSlot;

        public GUI() {
            super(RefineManager.this.plugin, RefineManager.this.getJYML(), "gui.");

            String path = "gui.";

            this.itemSlot = cfg.getInt(path + "item-slot");
            this.sourceSlot = cfg.getInt(path + "source-slot");
            this.resultSlot = cfg.getInt(path + "result-slot");

            GuiClick click = (p, type, e) -> {
                if (type == null) return;
                ContentType type2 = (ContentType) type;

                if (type2 == ContentType.EXIT) {
                    clearResult(e.getInventory()); // Clear result on cancel
                    p.closeInventory();
                    p.updateInventory();
                } else if (type2 == ContentType.ACCEPT) {
                    Inventory inv = e.getInventory();

                    ItemStack target = inv.getItem(itemSlot);
                    ItemStack src    = inv.getItem(sourceSlot);
                    ItemStack result = inv.getItem(resultSlot);
                    if (target == null || src == null || result == null) return;

                    ItemStack srcCopy = new ItemStack(src);

                    RefineManager.this.takeItemCharge(src);
                    if (RefineManager.this.getItemCharges(src) == 0) {
                        inv.setItem(sourceSlot, null);
                    }
                    int chance = ItemStats.getSocketRate(srcCopy) + getSilentRateBonus(p);

                    // Success
                    if (Rnd.get(true) < chance) {
                        inv.setItem(itemSlot, result);
                        p.closeInventory();
                        p.updateInventory();

                        RefineManager.this.getResultMessage(result, srcCopy, true, false).send(p);
                        RefineManager.this.actionsSuccess.process(p);

                        clearSilentRateBonus(p); // Clear fail stack
                        return;
                    }

                    // Fail stack feature
                    addSilentRateBonus(p, getRefineLevel(target));
                    clearResult(inv); // Remove result item on fail

                    FortifyManager fortify = plugin.getModuleCache().getFortifyManager();
                    boolean        fActive = fortify != null && fortify.canFortify(target, RefineManager.this);
                    boolean        fSave   = fActive && fortify != null && fortify.tryFortify(target);

                    if (fActive && fortify != null) {
                        fortify.unfortifyItem(target);

                        if (fSave) {
                            plugin.lang().Fortify_Enchanting_Success
                                    .replace("%item%", ItemUT.getItemName(target))
                                    .send(p);
                        } else {
                            plugin.lang().Fortify_Enchanting_Failure
                                    .replace("%item%", ItemUT.getItemName(target))
                                    .send(p);
                        }
                    }

                    if (!fSave) {
                        downgradeItem(target, false);
                        inv.setItem(itemSlot, target);
                    }

                    p.closeInventory();
                    p.updateInventory();
                    RefineManager.this.getResultMessage(target, srcCopy, false, fSave).send(p);
                    RefineManager.this.actionsFailure.process(p);
                }
            };

            for (String id : cfg.getSection(path + "content")) {
                GuiItem guiItem = cfg.getGuiItem(path + "content." + id + ".", ContentType.class);
                if (guiItem == null) continue;

                if (guiItem.getType() != null) {
                    guiItem.setClick(click);
                }

                this.addButton(guiItem);
            }
        }

        protected final void clearTarget(@NotNull Inventory inv) {
            inv.setItem(this.itemSlot, null);
        }

        protected final void clearSource(@NotNull Inventory inv) {
            inv.setItem(this.sourceSlot, null);
        }

        protected final void clearResult(@NotNull Inventory inv) {
            inv.setItem(this.resultSlot, null);
        }

        @Override
        protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {

        }

        @Override
        protected void onReady(@NotNull Player player, @NotNull Inventory inv, int page) {
            ItemStack src = inv.getItem(this.sourceSlot);

            for (int i = 0; i < this.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item == null || item.getType() == Material.AIR) continue;
                this.replacePlaceholder(player, item, src);
            }
        }

        @Override
        protected boolean ignoreNullClick() {
            return true;
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
        public void onClose(@NotNull Player player, @NotNull InventoryCloseEvent e) {
            Inventory inv = e.getInventory();

            ItemStack item = inv.getItem(this.itemSlot);
            ItemStack src  = inv.getItem(this.sourceSlot);

            if (item != null) {
                ItemUT.addItem(player, item);
            }
            if (src != null) {
                ItemUT.addItem(player, src);
            }
        }

        public void open(@NotNull Player player, @NotNull ItemStack target, @NotNull ItemStack src) {
            RefineItem stone = getModuleItem(src);
            if (stone == null) {
                error("Invalid Refine Item to refine! Aborting the refine operation.");
                return;
            }

            ItemStack result = new ItemStack(target);
            refineItem(result, stone);

            this.addButton(player, new JIcon(target), this.itemSlot);
            this.addButton(player, new JIcon(src), this.sourceSlot);
            this.addButton(player, new JIcon(result), this.resultSlot);

            super.open(player, 1);
        }

        private void replacePlaceholder(Player player, ItemStack item, ItemStack src) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            List<String> lore = meta.getLore();
            if (lore == null) return;

            downgradeItem(new ItemStack(src), false);
            int down = getRefineLevel(src);

            lore.replaceAll(line -> line.replace("%downgrade%", String.valueOf(down)));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }
}
