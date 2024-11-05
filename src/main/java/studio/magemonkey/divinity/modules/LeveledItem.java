package studio.magemonkey.divinity.modules;

import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.codex.util.eval.Evaluator;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.Config;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.ItemTags;
import studio.magemonkey.divinity.stats.items.requirements.ItemRequirements;
import studio.magemonkey.divinity.stats.items.requirements.item.ItemLevelRequirement;
import studio.magemonkey.divinity.stats.items.requirements.item.ItemModuleRequirement;
import studio.magemonkey.divinity.stats.items.requirements.item.ItemTierRequirement;
import studio.magemonkey.divinity.stats.items.requirements.item.ItemTypeRequirement;
import studio.magemonkey.divinity.stats.tiers.Tier;
import studio.magemonkey.divinity.stats.tiers.Tiered;
import studio.magemonkey.divinity.utils.LoreUT;

import java.util.*;

public abstract class LeveledItem extends ModuleItem implements Tiered {
    protected Tier                       tier;
    protected int                        levelMin;
    protected int                        levelMax;
    protected TreeMap<Integer, String[]> targetItemLevels;
    protected String[]                   targetItemTypes;
    protected String[]                   targetItemModules;
    protected String                     targetTier;

    // Creating new config
    @Deprecated
    public LeveledItem(@NotNull Divinity plugin, String path, QModuleDrop<?> module) throws
            InvalidConfigurationException {
        super(plugin, path, module);
    }

    // Load from existent config
    public LeveledItem(@NotNull Divinity plugin, @NotNull JYML cfg, @NotNull QModuleDrop<?> module) {
        super(plugin, cfg, module);
        this.updateConfig(cfg);

        validateTier(cfg);

        this.name = this.getTier().format(this.name);
        processLore(cfg, module);

        int levelMin = cfg.getInt("level.min", 1);
        int levelMax = cfg.getInt("level.max", 1);
        this.levelMin = Math.min(levelMin, levelMax);
        this.levelMax = Math.max(levelMin, levelMax);

        this.targetItemLevels = new TreeMap<>();
        for (String rLvl : cfg.getSection("target-requirements.level")) {
            int itemLvl = StringUT.getInteger(rLvl, -1);
            if (itemLvl <= 0) continue;

            String raw = cfg.getString("target-requirements.level." + rLvl);
            if (raw == null || raw.isEmpty()) continue;

            this.targetItemLevels.put(itemLvl, raw.split(":"));
        }

        List<String> types = cfg.getStringList("target-requirements.type");
        types.replaceAll(String::toLowerCase);
        // If wildcard is present then no need to require it
        if (!types.contains(JStrings.MASK_ANY)) {
            this.targetItemTypes = types.toArray(new String[types.size()]);
        }

        List<String> modules = cfg.getStringList("target-requirements.module");
        modules.replaceAll(String::toLowerCase);
        // If wildcard is present then no need to require it
        if (!modules.contains(JStrings.MASK_ANY)) {
            this.targetItemModules = modules.toArray(new String[modules.size()]);
        }

        this.targetTier = cfg.getString("target-requirements.tier", "");

        cfg.saveChanges();
    }

    private void validateTier(JYML cfg) {
        this.tier = Config.getTier(cfg.getString("tier", JStrings.DEFAULT));
        if (this.tier == null) {
            throw new IllegalArgumentException("Invalid Tier provided! Module Item must have valid Tier!");
        }
    }

    @Override
    protected String applyLoreReplacements(String lore) {
        return this.getTier().format(lore);
    }

    @Override
    protected void processLore(@NotNull JYML cfg, @NotNull QModuleDrop<?> module) {
        validateTier(cfg);
        super.processLore(cfg, module);
    }

    @Override
    protected void save(@NotNull JYML cfg) {

    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public List<String> getLore() {
        return this.lore;
    }

    @NotNull
    public QModuleDrop<?> getModule() {
        return this.module;
    }

    @NotNull
    public final ItemStack create() {
        return this.create(-1);
    }

    private void updateConfig(@NotNull JYML cfg) {
        cfg.addMissing("tier", JStrings.DEFAULT);

        cfg.addMissing("level.min", 1);
        cfg.addMissing("level.max", 1);

        if (!cfg.contains("target-requirements")) {
            cfg.addMissing("target-requirements.type", Arrays.asList(JStrings.MASK_ANY));
            cfg.addMissing("target-requirements.level.1", 0);
            cfg.addMissing("target-requirements.module", Arrays.asList(JStrings.MASK_ANY));
            if (this instanceof SocketItem) {
                cfg.addMissing("target-requirements.socket", JStrings.DEFAULT);
            }
        }
        cfg.saveChanges();
    }

    @NotNull
    public Tier getTier() {
        return this.tier;
    }

    public int getMinLevel() {
        return this.levelMin;
    }

    public int getMaxLevel() {
        return this.levelMax;
    }

    protected final int[] getTargetLevelRequirement(int itemLvl) {
        Map.Entry<Integer, String[]> e = this.targetItemLevels.floorEntry(itemLvl);
        if (e == null) return null;

        String[] both = e.getValue();

        int[] values = this.doMathExpression(itemLvl, both);
        // Fine min. value by min. possible level or null if both values not present.
        if (values[0] <= 0) {
            if (values[1] > 0) {
                values[0] = 1;
            } else {
                return null;
            }
        }
        return values;
    }

    protected final int[] doMathExpression(int itemLvl, @NotNull String[] both) {
        int[] values = new int[2];
        for (int i = 0; i < both.length; i++) {
            String   str      = both[i].replace(ItemTags.PLACEHOLDER_ITEM_LEVEL, String.valueOf(itemLvl)).trim();
            String[] intSplit = str.split("~");
            double   val1     = Evaluator.eval(intSplit[0], 1);
            double   val2     = intSplit.length >= 2 ? Evaluator.eval(intSplit[1], 1) : val1;

            double min = Math.min(val1, val2);
            double max = Math.max(val1, val2);

            values[i] = (int) Rnd.getDouble(min, max);
        }
        // Fine max. value by correcting to min if it's not present.
        if (values[1] <= 0) values[1] = values[0];

        return values;
    }

    @NotNull
    public ItemStack create(int lvl) {
        return this.build(this.validateLevel(lvl));
    }

    @NotNull
    protected ItemStack build(int lvl) {
        return build(null, lvl);
    }

    @NotNull
    protected ItemStack build(@Nullable ItemStack item, int lvl) {
        item = item == null ? super.build() : super.build(item);

        ItemStats.setId(item, this.getId());
        ItemStats.setModule(item, this.getModule().getId());
        ItemStats.setLevel(item, this.validateLevel(lvl));

        if (!(this instanceof RatedItem))
            this.replacePlaceholders(item);

        return item;
    }

    protected final int validateLevel(int lvl) {
        if (lvl == -1) {
            lvl = Rnd.get(this.getMinLevel(), this.getMaxLevel());
        } else {
            if (lvl > this.getMaxLevel()) lvl = this.getMaxLevel();
            else if (lvl < this.getMinLevel()) lvl = this.getMinLevel();
        }
        return lvl;
    }

    protected void replacePlaceholders(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int level   = ItemStats.getLevel(item);
        int sucRate = ItemStats.getSocketRate(item);

        String sLevel    = String.valueOf(level);
        String sLevelRom = NumberUT.toRoman(level);
        String sSucRate  = String.valueOf(sucRate);

        String name = meta.getDisplayName()
                .replace(ItemTags.PLACEHOLDER_ITEM_SUCCESS_RATE, sSucRate)
                .replace(ItemTags.PLACEHOLDER_ITEM_LEVEL_ROMAN, sLevelRom)
                .replace(ItemTags.PLACEHOLDER_ITEM_LEVEL, sLevel);
        meta.setDisplayName(name);


        List<String> metaLore = meta.getLore();
        List<String> lore     = metaLore != null ? metaLore : new ArrayList<>();
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i)
                    .replace(ItemTags.PLACEHOLDER_ITEM_SUCCESS_RATE, sSucRate)
                    .replace(ItemTags.PLACEHOLDER_ITEM_LEVEL_ROMAN, sLevelRom)
                    .replace(ItemTags.PLACEHOLDER_ITEM_LEVEL, sLevel);
            lore.set(i, StringUT.color(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);

        // Replace %TARGET_LEVEL% placeholder
        int[] reqLevels = this.getTargetLevelRequirement(level);
        if (reqLevels != null) {
            ItemLevelRequirement reqLevel = ItemRequirements.getItemRequirement(ItemLevelRequirement.class);
            if (reqLevel != null && reqLevel.hasPlaceholder(item)) {
                reqLevel.add(item, reqLevels, -1);
            }
        }

        // Replace %TARGET_TYPE% placeholder
        if (!ArrayUtils.isEmpty(this.targetItemTypes)) {
            ItemTypeRequirement reqType = ItemRequirements.getItemRequirement(ItemTypeRequirement.class);
            if (reqType != null && reqType.hasPlaceholder(item)) {
                reqType.add(item, this.targetItemTypes, -1);
            }
        }

        // Replace %TARGET_MODULE% placeholder
        if (!ArrayUtils.isEmpty(this.targetItemModules)) {
            ItemModuleRequirement reqMod = ItemRequirements.getItemRequirement(ItemModuleRequirement.class);
            if (reqMod != null && reqMod.hasPlaceholder(item)) {
                reqMod.add(item, this.targetItemModules, -1);
            }
        }

        // Replace %TARGET_TIER% placeholder
        if (this.targetTier != null && !this.targetTier.isEmpty()) {
            Tier tier = Config.getTier(this.targetTier);
            if (tier != null) {
                ItemTierRequirement tierRequirement = ItemRequirements.getItemRequirement(ItemTierRequirement.class);
                if (tierRequirement != null && tierRequirement.hasPlaceholder(item)) {
                    tierRequirement.add(item, this.targetTier, -1);
                }
            }
        }

        // Delete placeholders if requirements were not added or been disabled.
        LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_REQ_ITEM_LEVEL, null);
        LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_REQ_ITEM_TYPE, null);
        LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_REQ_ITEM_MODULE, null);
        LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_REQ_ITEM_TIER, null);
    }
}
