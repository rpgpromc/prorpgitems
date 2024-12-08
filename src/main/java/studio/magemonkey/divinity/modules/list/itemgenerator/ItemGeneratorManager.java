package studio.magemonkey.divinity.modules.list.itemgenerator;

import lombok.Getter;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.VersionManager;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.items.exception.MissingItemException;
import studio.magemonkey.codex.api.items.exception.MissingProviderException;
import studio.magemonkey.codex.api.items.providers.ICodexItemProvider;
import studio.magemonkey.codex.api.items.providers.VanillaProvider;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.core.Version;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.Reflex;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.Config;
import studio.magemonkey.divinity.hooks.EHook;
import studio.magemonkey.divinity.hooks.external.FabledHook;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.LimitedItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager.GeneratorItem;
import studio.magemonkey.divinity.modules.list.itemgenerator.ResourceManager.ResourceCategory;
import studio.magemonkey.divinity.modules.list.itemgenerator.api.IAttributeGenerator;
import studio.magemonkey.divinity.modules.list.itemgenerator.command.CreateCommand;
import studio.magemonkey.divinity.modules.list.itemgenerator.command.EditCommand;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.AbstractEditorGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.generators.AbilityGenerator;
import studio.magemonkey.divinity.modules.list.itemgenerator.generators.AttributeGenerator;
import studio.magemonkey.divinity.modules.list.itemgenerator.generators.SingleAttributeGenerator;
import studio.magemonkey.divinity.modules.list.itemgenerator.generators.TypedStatGenerator;
import studio.magemonkey.divinity.modules.list.sets.SetManager;
import studio.magemonkey.divinity.stats.bonus.BonusMap;
import studio.magemonkey.divinity.stats.bonus.StatBonus;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.ItemTags;
import studio.magemonkey.divinity.stats.items.api.ItemLoreStat;
import studio.magemonkey.divinity.stats.items.attributes.DamageAttribute;
import studio.magemonkey.divinity.stats.items.attributes.DefenseAttribute;
import studio.magemonkey.divinity.stats.items.attributes.SocketAttribute;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;
import studio.magemonkey.divinity.stats.items.requirements.ItemRequirements;
import studio.magemonkey.divinity.stats.items.requirements.api.DynamicUserRequirement;
import studio.magemonkey.divinity.stats.items.requirements.user.BannedClassRequirement;
import studio.magemonkey.divinity.stats.items.requirements.user.ClassRequirement;
import studio.magemonkey.divinity.stats.items.requirements.user.LevelRequirement;
import studio.magemonkey.divinity.stats.items.requirements.user.SoulboundRequirement;
import studio.magemonkey.divinity.stats.tiers.Tier;
import studio.magemonkey.divinity.utils.DivinityProvider;
import studio.magemonkey.divinity.utils.ItemUtils;
import studio.magemonkey.divinity.utils.LoreUT;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ItemGeneratorManager extends QModuleDrop<GeneratorItem> {

    public static  YamlConfiguration  commonItemGenerator;
    @Getter
    private static ResourceManager    resourceManager;
    private        ItemAbilityHandler abilityHandler;

    public static final String PLACE_GEN_DAMAGE      = "%GENERATOR_DAMAGE%";
    public static final String PLACE_GEN_DEFENSE     = "%GENERATOR_DEFENSE%";
    public static final String PLACE_GEN_STATS       = "%GENERATOR_STATS%";
    public static final String PLACE_GEN_SOCKETS     = "%GENERATOR_SOCKETS_%TYPE%%";
    public static final String PLACE_GEN_ABILITY     = "%GENERATOR_SKILLS%";
    public static final String PLACE_GEN_FABLED_ATTR = "%GENERATOR_FABLED_ATTR%";

    public ItemGeneratorManager(@NotNull Divinity plugin) {
        super(plugin, GeneratorItem.class);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.ITEM_GENERATOR;
    }

    @Override
    @NotNull
    public String version() {
        return "2.0.0";
    }

    @Override
    public void setup() {
        if (ItemGeneratorManager.commonItemGenerator == null) {
            try (InputStreamReader in = new InputStreamReader(Objects.requireNonNull(plugin.getClass()
                    .getResourceAsStream(this.getPath() + "items/common.yml")))) {
                ItemGeneratorManager.commonItemGenerator = YamlConfiguration.loadConfiguration(in);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        try (InputStream in = plugin.getClass().getResourceAsStream(this.getPath() + "settings.yml")) {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(new String(in.readAllBytes()));
            cfg.addMissing("editor-gui", configuration.get("editor-gui"));
            cfg.saveChanges();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.resourceManager = new ResourceManager(this);

        this.abilityHandler = new ItemAbilityHandler(this);
        this.abilityHandler.setup();
    }

    @Override
    protected void onPostSetup() {
        super.onPostSetup();
        this.moduleCommand.addSubCommand(new CreateCommand(this));
        this.moduleCommand.addSubCommand(new EditCommand(this));
    }

    @Override
    public void shutdown() {
        AbstractEditorGUI editorGUI = AbstractEditorGUI.getInstance();
        if (editorGUI != null) {
            editorGUI.shutdown();
        }
        this.unregisterListeners();
        if (this.abilityHandler != null) {
            this.abilityHandler.shutdown();
            this.abilityHandler = null;
        }

        if (this.resourceManager != null) {
            this.resourceManager.shutdown();
            this.resourceManager = null;
        }
    }

    // loadItems is PostSetup method.
    @Override
    protected void loadItems() {
        super.loadItems();

        this.resourceManager.setup();
    }

    @NotNull
    public GeneratorItem load(String id, JYML cfg) {
        GeneratorItem itemGenerator = new GeneratorItem(plugin, cfg);
        items.put(id, itemGenerator);
        return itemGenerator;
    }

    public class GeneratorItem extends LimitedItem {

        @Getter
        private double prefixChance;
        @Getter
        private double suffixChance;

        private boolean       materialsWhitelist;
        private Set<ItemType> materialsList;

        private List<Integer>              modelDataList;
        private Map<String, List<Integer>> modelDataSpecial;

        private Map<String, BonusMap>                     materialModifiers;
        private Map<String, Map<ItemLoreStat<?>, String>> materialBonuses;
        private Map<String, Map<ItemLoreStat<?>, String>> classBonuses;

        private TreeMap<Integer, String[]> reqUserLvl;
        private TreeMap<Integer, String[]> reqUserClass;
        private TreeMap<Integer, String[]> reqBannedUserClass;

        private       int                        enchantsMinAmount;
        private       int                        enchantsMaxAmount;
        private       boolean                    enchantsSafeOnly;
        @Getter
        private       boolean                    enchantsSafeLevels;
        private       Map<Enchantment, String[]> enchantsList;
        private final TreeMap<Double, String>    armorTrims = new TreeMap<>();

        private Set<IAttributeGenerator> attributeGenerators;
        private AbilityGenerator         abilityGenerator;

        public GeneratorItem(@NotNull Divinity plugin, @NotNull JYML cfg) {
            super(plugin, cfg, ItemGeneratorManager.this);

            String path = "generator.";
            this.prefixChance = cfg.getDouble(path + "prefix-chance");
            this.suffixChance = cfg.getDouble(path + "suffix-chance");

            // Pre-cache available materials for Generator.
            this.materialsWhitelist = cfg.getBoolean(path + "materials.reverse");
            if (this.materialsWhitelist) {
                this.materialsList = new HashSet<>();
                Set<String> startWildcards = new HashSet<>();
                Set<String> endWildcards   = new HashSet<>();

                for (String mat : cfg.getStringList(path + "materials.black-list")) {
                    String[] split = mat.split('\\' + JStrings.MASK_ANY, 2);
                    if (split.length == 2) { // We have a wildcard
                        if (split[0].isEmpty()) startWildcards.add(split[1].toUpperCase());
                        else if (split[1].isEmpty()) endWildcards.add(split[0].toUpperCase());
                        continue;
                    }
                    try {
                        ItemType itemType = CodexEngine.get().getItemManager().getItemType(mat);
                        if (itemType.getNamespace().equals(DivinityProvider.NAMESPACE))
                            continue; // Avoid self-reference
                        this.materialsList.add(itemType);
                    } catch (MissingProviderException | MissingItemException ignored) {
                    }
                }

                for (ItemType itemType : Config.getAllRegisteredMaterials()) {
                    String name = itemType.getNamespacedID().toUpperCase();
                    if (startWildcards.stream().anyMatch(name::endsWith) || endWildcards.stream()
                            .anyMatch(name::startsWith)) {
                        this.materialsList.add(itemType);
                    }
                }
            } else {
                this.materialsList = new HashSet<>(Config.getAllRegisteredMaterials());
                Set<String> materials = new HashSet<>(cfg.getStringList(path + "materials.black-list"));
                this.materialsList.removeIf(matAll -> {
                    String namespacedID          = matAll.getNamespacedID();
                    String upperCaseNamespacedID = namespacedID.toUpperCase();
                    for (String mat : materials) {
                        String[] split = mat.split('\\' + JStrings.MASK_ANY, 2);
                        if (split.length == 2) { // We have a wildcard
                            if (split[0].isEmpty() && upperCaseNamespacedID.endsWith(split[1])) return true;
                            else if (split[1].isEmpty() && upperCaseNamespacedID.startsWith(split[0])) return true;
                        }
                        if (namespacedID.equals(mat)) return true;
                    }
                    return false;
                });
            }

            // Load Model Data values for specified item groups.
            path = "generator.materials.model-data.";
            this.modelDataList = cfg.getIntegerList(path + "default");
            this.modelDataSpecial = new HashMap<>();
            for (String specGroup : cfg.getSection(path + "special")) {
                List<Integer> specList = cfg.getIntegerList(path + "special." + specGroup);
                this.modelDataSpecial.put(specGroup.toLowerCase(), specList);
            }

            // Load Bonuses

            // Migrate old Stat Modifiers to Material Bonuses
            if (cfg.isConfigurationSection("generator.materials.stat-modifiers")) {
                ConfigurationSection section = cfg.getConfigurationSection("generator.materials.stat-modifiers");
                cfg.remove("generator.materials.stat-modifiers");
                cfg.set("generator.bonuses.material", section);
                cfg.save();
            }

            // Load Material bonuses
            path = "generator.bonuses.material-modifiers.";
            this.materialModifiers = new HashMap<>();
            for (String group : cfg.getSection("generator.bonuses.material-modifiers")) {
                if (!ItemUtils.parseItemGroup(group)) {
                    error("Invalid item group provided: '" + group + "' in '" + path + "'. File: " + cfg.getFile()
                            .getName());
                    continue;
                }

                BonusMap bMap  = new BonusMap();
                String   path2 = path + group + ".";
                bMap.loadDamages(cfg, path2 + "damage-types");
                bMap.loadDefenses(cfg, path2 + "defense-types");
                bMap.loadStats(cfg, path2 + "item-stats");
                bMap.loadFabledAttributes(cfg, path2 + "fabled-attributes");
                bMap.loadAmmo(cfg, path2 + "ammo");
                bMap.loadHands(cfg, path2 + "hands");

                this.materialModifiers.put(group.toLowerCase(), bMap);
            }

            // Load Material bonuses
            path = "generator.bonuses.material.";
            this.materialBonuses = new HashMap<>();
            for (String group : cfg.getSection("generator.bonuses.material")) {
                Map<ItemLoreStat<?>, String> statMap = new HashMap<>();

                String path2 = path + group + ".damage-types";
                for (String id : cfg.getSection(path2)) {
                    DamageAttribute dt = ItemStats.getDamageById(id);
                    if (dt == null) continue;

                    String sVal = cfg.getString(path2 + "." + id);
                    if (sVal == null) continue;

                    String[] split = sVal.split("%", 2);
                    boolean  perc  = split.length == 2 && split[1].isEmpty();
                    double   val   = StringUT.getDouble(split[0], 0, true);

                    statMap.put(dt, val + (perc ? "%" : ""));
                }

                path2 = path + group + ".defense-types";
                for (String id : cfg.getSection(path2)) {
                    DefenseAttribute dt = ItemStats.getDefenseById(id);
                    if (dt == null) continue;

                    String sVal = cfg.getString(path2 + "." + id);
                    if (sVal == null) continue;

                    String[] split = sVal.split("%", 2);
                    boolean  perc  = split.length == 2 && split[1].isEmpty();
                    double   val   = StringUT.getDouble(split[0], 0, true);

                    statMap.put(dt, val + (perc ? "%" : ""));
                }

                path2 = path + group + ".item-stats";
                for (String id : cfg.getSection(path2)) {
                    SimpleStat.Type dt = TypedStat.Type.getByName(id);
                    if (dt == null) continue;

                    ItemLoreStat<?> mainStat = (ItemLoreStat<?>) ItemStats.getStat(dt);

                    String sVal = cfg.getString(path2 + "." + id);
                    if (sVal == null) continue;

                    String[] split = sVal.split("%", 2);
                    boolean  perc  = split.length == 2 && split[1].isEmpty();
                    double   val   = StringUT.getDouble(split[0], 0, true);

                    statMap.put(mainStat, val + (perc ? "%" : ""));
                }

                this.materialBonuses.put(group, statMap);
            }

            // Load Class bonuses
            path = "generator.bonuses.class.";
            this.classBonuses = new HashMap<>();
            for (String group : cfg.getSection("generator.bonuses.class")) {
                Map<ItemLoreStat<?>, String> statMap = new HashMap<>();

                String path2 = path + group + ".damage-types";
                for (String id : cfg.getSection(path2)) {
                    DamageAttribute dt = ItemStats.getDamageById(id);
                    if (dt == null) continue;

                    String sVal = cfg.getString(path2 + "." + id);
                    if (sVal == null) continue;

                    String[] split = sVal.split("%", 2);
                    boolean  perc  = split.length == 2 && split[1].isEmpty();
                    double   val   = StringUT.getDouble(split[0], 0, true);

                    statMap.put(dt, val + (perc ? "%" : ""));
                }

                path2 = path + group + ".defense-types";
                for (String id : cfg.getSection(path2)) {
                    DefenseAttribute dt = ItemStats.getDefenseById(id);
                    if (dt == null) continue;

                    String sVal = cfg.getString(path2 + "." + id);
                    if (sVal == null) continue;

                    String[] split = sVal.split("%", 2);
                    boolean  perc  = split.length == 2 && split[1].isEmpty();
                    double   val   = StringUT.getDouble(split[0], 0, true);

                    statMap.put(dt, val + (perc ? "%" : ""));
                }

                path2 = path + group + ".item-stats";
                for (String id : cfg.getSection(path2)) {
                    SimpleStat.Type dt = TypedStat.Type.getByName(id);
                    if (dt == null) continue;

                    ItemLoreStat<?> mainStat = (ItemLoreStat<?>) ItemStats.getStat(dt);

                    String sVal = cfg.getString(path2 + "." + id);
                    if (sVal == null) continue;

                    String[] split = sVal.split("%", 2);
                    boolean  perc  = split.length == 2 && split[1].isEmpty();
                    double   val   = StringUT.getDouble(split[0], 0, true);

                    statMap.put(mainStat, val + (perc ? "%" : ""));
                }

                this.classBonuses.put(group, statMap);
            }

            // Load User Requirements.
            path = "generator.user-requirements-by-level.";
            if (ItemRequirements.isRegisteredUser(LevelRequirement.class)) {
                this.reqUserLvl = new TreeMap<>();
                for (String sLvl : cfg.getSection(path + "level")) {
                    int itemLvl = StringUT.getInteger(sLvl, -1);
                    if (itemLvl <= 0) continue;

                    String reqRaw = cfg.getString(path + "level." + sLvl);
                    if (reqRaw == null || reqRaw.isEmpty()) continue;

                    this.reqUserLvl.put(itemLvl,
                            Arrays.stream(reqRaw.split(":"))
                                    .filter(s -> !s.isBlank())
                                    .collect(Collectors.toList())
                                    .toArray(new String[]{}));
                }
            }
            if (ItemRequirements.isRegisteredUser(ClassRequirement.class)) {
                this.reqUserClass = new TreeMap<>();
                for (String sLvl : cfg.getSection(path + "class")) {
                    int itemLvl = StringUT.getInteger(sLvl, -1);
                    if (itemLvl <= 0) continue;

                    String reqRaw = cfg.getString(path + "class." + sLvl, "");

                    this.reqUserClass.put(itemLvl,
                            Arrays.stream(reqRaw.split(","))
                                    .filter(s -> !s.isBlank())
                                    .collect(Collectors.toList())
                                    .toArray(new String[]{}));
                }
            }
            if (ItemRequirements.isRegisteredUser(BannedClassRequirement.class)) {
                this.reqBannedUserClass = new TreeMap<>();
                for (String sLvl : cfg.getSection(path + "banned-class")) {
                    int itemLvl = StringUT.getInteger(sLvl, -1);
                    if (itemLvl <= 0) continue;

                    String reqRaw = cfg.getString(path + "banned-class." + sLvl, "");

                    this.reqBannedUserClass.put(itemLvl, reqRaw.split(","));
                }
            }

            // Pre-cache enchantments.
            path = "generator.enchantments.";
            cfg.addMissing(path + "safe-levels", true);

            this.enchantsMinAmount = Math.max(0, cfg.getInt(path + "minimum"));
            this.enchantsMaxAmount = Math.max(0, cfg.getInt(path + "maximum"));
            this.enchantsSafeOnly = cfg.getBoolean(path + "safe-only");
            this.enchantsSafeLevels = cfg.getBoolean(path + "safe-levels");
            this.enchantsList = new HashMap<>();
            for (String sId : cfg.getSection(path + "list")) {
                Enchantment en = Enchantment.getByKey(NamespacedKey.minecraft(sId.toLowerCase()));
                if (en == null) {
                    error("Invalid enchantment provided: " + sId + " (" + cfg.getFile().getName() + ")");
                    continue;
                }

                String reqRaw = cfg.getString(path + "list." + sId);
                if (reqRaw == null || reqRaw.isEmpty()) continue;

                this.enchantsList.put(en, reqRaw.split(":"));
            }

            if (Version.CURRENT.isHigher(Version.V1_19_R3)) {
                path = "generator.armor-trimmings";
                double totalWeight = 0;
                for (String key : cfg.getSection(path)) {
                    double weight = cfg.getDouble(path + '.' + key);
                    if (weight == 0) {
                        continue;
                    }
                    if (key.equals("none")) {
                        totalWeight += weight;
                        armorTrims.put(totalWeight, null);
                        continue;
                    }
                    String[] split = key.toLowerCase().split(":");
                    if (split.length != 2) {
                        continue;
                    }
                    if (!split[0].equals("*")
                            && VersionManager.getArmorUtil().getTrimMaterial(NamespacedKey.minecraft(split[0])) == null) {
                        continue;
                    }
                    if (!split[1].equals("*") &&
                            VersionManager.getArmorUtil().getTrimPattern(NamespacedKey.minecraft(split[1])) == null) {
                        continue;
                    }
                    totalWeight += weight;
                    armorTrims.put(totalWeight, key);
                }
            }

            this.attributeGenerators = new HashSet<>();

            // Pre-cache Ammo Attributes
            this.addAttributeGenerator(new SingleAttributeGenerator<>(this.plugin,
                    this,
                    "generator.ammo-types.",
                    ItemStats.getAmmos(),
                    ItemTags.PLACEHOLDER_ITEM_AMMO));
            this.addAttributeGenerator(new SingleAttributeGenerator<>(this.plugin,
                    this,
                    "generator.hand-types.",
                    ItemStats.getHands(),
                    ItemTags.PLACEHOLDER_ITEM_HAND));

            this.addAttributeGenerator(new AttributeGenerator<>(this.plugin,
                    this,
                    "generator.damage-types.",
                    ItemStats.getDamages(),
                    ItemGeneratorManager.PLACE_GEN_DAMAGE));
            this.addAttributeGenerator(new AttributeGenerator<>(this.plugin,
                    this,
                    "generator.defense-types.",
                    ItemStats.getDefenses(),
                    ItemGeneratorManager.PLACE_GEN_DEFENSE));
            this.addAttributeGenerator(new TypedStatGenerator(this.plugin,
                    this,
                    "generator.item-stats.",
                    ItemStats.getStats(),
                    ItemGeneratorManager.PLACE_GEN_STATS));
            this.addAttributeGenerator(this.abilityGenerator = new AbilityGenerator(this.plugin,
                    this,
                    PLACE_GEN_ABILITY));
            FabledHook fabledHook = (FabledHook) Divinity.getInstance().getHook(EHook.SKILL_API);
            this.addAttributeGenerator(new AttributeGenerator<>(this.plugin,
                    this,
                    "generator.fabled-attributes.",
                    fabledHook != null && fabledHook.isFabledLoaded() ? fabledHook.getAttributes() : List.of(),
                    ItemGeneratorManager.PLACE_GEN_FABLED_ATTR));
            if (fabledHook != null) {
                cfg.addMissing("generator.fabled-attributes", commonItemGenerator.get("generator.fabled-attributes"));
                cfg.addMissing("generator.skills", commonItemGenerator.get("generator.skills"));
            }

            // Pre-cache Socket Attributes
            for (SocketAttribute.Type socketType : SocketAttribute.Type.values()) {
                this.addAttributeGenerator(new AttributeGenerator<>(this.plugin,
                        this,
                        "generator.sockets." + socketType.name() + ".",
                        ItemStats.getSockets(socketType),
                        ItemGeneratorManager.PLACE_GEN_SOCKETS.replace("%TYPE%", socketType.name())));
            }


            // --------------- END OF CONFIG ---------------------- //

            cfg.saveChanges();
        }

        protected final int[] getUserLevelRequirement(int itemLvl) {
            if (this.reqUserLvl == null) return new int[]{0};

            Map.Entry<Integer, String[]> e = this.reqUserLvl.floorEntry(itemLvl);
            if (e == null) return new int[]{0};

            return this.doMathExpression(itemLvl, e.getValue());
        }

        @Nullable
        protected final String[] getUserClassRequirement(int itemLvl) {
            if (this.reqUserClass == null) return null;

            Map.Entry<Integer, String[]> e = this.reqUserClass.floorEntry(itemLvl);
            if (e == null) return null;

            return e.getValue();
        }

        @Nullable
        protected final String[] getUserBannedClassRequirement(int itemLvl) {
            if (this.reqBannedUserClass == null) return null;

            Map.Entry<Integer, String[]> e = this.reqBannedUserClass.floorEntry(itemLvl);
            if (e == null) return null;

            return e.getValue();
        }

        public boolean isMaterialReversed() {
            return this.materialsWhitelist;
        }

        @NotNull
        public Set<ItemType> getMaterialsList() {
            return materialsList;
        }

        public int getMinEnchantments() {
            return this.enchantsMinAmount;
        }

        public int getMaxEnchantments() {
            return this.enchantsMaxAmount;
        }

        public boolean isSafeEnchant() {
            return enchantsSafeOnly;
        }

        @NotNull
        public BiFunction<Boolean, Double, Double> getMaterialModifiers(@NotNull ItemStack item,
                                                                        @NotNull ItemLoreStat<?> stat) {
            for (Map.Entry<String, BonusMap> e : this.materialModifiers.entrySet()) {
                if (ItemUtils.compareItemGroup(item, e.getKey())) {
                    BonusMap                            bMap = e.getValue();
                    BiFunction<Boolean, Double, Double> bif  = bMap.getBonus(stat);
                    if (bif == null) {
                        continue;
                    }

                    return bif;
                }
            }
            return (isBonus, result) -> result;
        }

        public Collection<StatBonus> getMaterialBonuses(ItemStack item, ItemLoreStat<?> stat) {
            List<StatBonus> list = new ArrayList<>();
            for (Map.Entry<String, Map<ItemLoreStat<?>, String>> entry : this.materialBonuses.entrySet()) {
                if (!ItemUtils.compareItemGroup(item, entry.getKey())) continue;
                for (Map.Entry<ItemLoreStat<?>, String> entry1 : entry.getValue().entrySet()) {
                    if (entry1.getKey().equals(stat)) {
                        String   sVal  = entry1.getValue();
                        String[] split = sVal.split("%", 2);
                        list.add(new StatBonus(
                                new double[]{Double.parseDouble(split[0])},
                                split.length == 2 && split[1].isEmpty(),
                                new StatBonus.Condition<>()));
                    }
                }
            }
            return list;
        }

        public Collection<StatBonus> getClassBonuses(ItemLoreStat<?> stat) {
            List<StatBonus> list = new ArrayList<>();
            for (Map.Entry<String, Map<ItemLoreStat<?>, String>> entry : this.classBonuses.entrySet()) {
                for (Map.Entry<ItemLoreStat<?>, String> entry1 : entry.getValue().entrySet()) {
                    if (entry1.getKey().equals(stat)) {
                        String   sVal  = entry1.getValue();
                        String[] split = sVal.split("%", 2);
                        list.add(new StatBonus(
                                new double[]{Double.parseDouble(split[0])},
                                split.length == 2 && split[1].isEmpty(),
                                new StatBonus.Condition<>(ItemRequirements.getUserRequirement(ClassRequirement.class),
                                        new String[]{entry.getKey()})));
                    }
                }
            }
            return list;
        }

        @NotNull
        public Set<IAttributeGenerator> getAttributeGenerators() {
            return attributeGenerators;
        }

        public boolean addAttributeGenerator(@NotNull IAttributeGenerator generator) {
            return this.attributeGenerators.add(generator);
        }

        @NotNull
        public AbilityGenerator getAbilityGenerator() {
            return abilityGenerator;
        }

        public double getScaleOfLevel(double scale, int itemLevel) {
            return scale == 1D ? (scale) : ((scale * 100D - 100D) * (double) (itemLevel - 1) / 100D + 1D);
        }

        @Override
        @NotNull
        public ItemStack create(int lvl, int uses) {
            return this.create(lvl, uses, null);
        }

        @NotNull
        public ItemStack create(int lvl, int uses, @Nullable ItemType mat) {
            lvl = this.validateLevel(lvl);
            if (uses < 1) uses = this.getCharges(lvl);

            return this.build(lvl, uses, mat);
        }

        @NotNull
        protected ItemStack build(int itemLvl, int uses, @Nullable ItemType mat) {
            ItemStack item;
            if (mat != null && materialsList.contains(mat)) {
                item = super.build(mat.create(), itemLvl, uses);
            } else {
                ItemType itemType = Rnd.get(new ArrayList<>(this.materialsList));
                item = super.build(itemType == null ? null : itemType.create(), itemLvl, uses);
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null) return item;

            List<Integer> dataValues = new ArrayList<>();
            for (Map.Entry<String, List<Integer>> e : this.modelDataSpecial.entrySet()) {
                if (ItemUtils.compareItemGroup(item, e.getKey())) {
                    dataValues.addAll(e.getValue());
                }
            }
            if (dataValues.isEmpty()) {
                dataValues.addAll(this.modelDataList);
            }
            if (!dataValues.isEmpty()) {
                Integer mInt = Rnd.get(dataValues);
                if (mInt != null) {
                    meta.setCustomModelData(mInt.intValue());
                }
            }

            // Prepare prefixes and suffixes
            String prefixTier = "";
            String suffixTier = "";

            String prefixMaterial = "";
            String suffixMaterial = "";

            String prefixItemType = "";
            String suffixItemType = "";

            String itemGroupId   = ItemUtils.getItemGroupIdFor(item);
            String itemGroupName = ItemUtils.getItemGroupNameFor(item);

            String itemMaterial = CodexEngine.get().getItemManager().getItemTypes(item).stream()
                    .filter(itemType -> itemType.getCategory() != ICodexItemProvider.Category.PRO)
                    .max(Comparator.comparing(ItemType::getCategory))
                    .orElseGet(() -> new VanillaProvider.VanillaItemType(item.getType())).getNamespacedID();

            if (Rnd.get(true) <= prefixChance) {
                prefixTier = Rnd.get(resourceManager.getPrefix(ResourceCategory.TIER, this.getTier().getId()));
                prefixMaterial = Rnd.get(resourceManager.getPrefix(ResourceCategory.MATERIAL, itemMaterial));
                prefixItemType = Rnd.get(resourceManager.getPrefix(ResourceCategory.SUBTYPE, itemGroupId));
            }
            if (Rnd.get(true) <= suffixChance) {
                suffixTier = Rnd.get(resourceManager.getSuffix(ResourceCategory.TIER, this.getTier().getId()));
                suffixMaterial = Rnd.get(resourceManager.getSuffix(ResourceCategory.MATERIAL, itemMaterial));
                suffixItemType = Rnd.get(resourceManager.getSuffix(ResourceCategory.SUBTYPE, itemGroupId));
            }

            // Replace prefix and suffix
            if (meta.hasDisplayName()) {
                String metaName = meta.getDisplayName()
                        .replace("%item_type%", itemGroupName)
                        .replace("%suffix_tier%", suffixTier != null ? suffixTier : "")
                        .replace("%prefix_tier%", prefixTier != null ? prefixTier : "")

                        .replace("%prefix_type%", prefixItemType != null ? prefixItemType : "")
                        .replace("%suffix_type%", suffixItemType != null ? suffixItemType : "")

                        .replace("%prefix_material%", prefixMaterial != null ? prefixMaterial : "")
                        .replace("%suffix_material%", suffixMaterial != null ? suffixMaterial : "");
                metaName = StringUT.oneSpace(metaName);
                meta.setDisplayName(metaName);
            }

            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.replaceAll(line -> line.replace("%item_type%", itemGroupName));
                meta.setLore(lore);
            }

            // +-------------------------+
            //           COLORED
            //      LEATHER AND SHIELDS
            // +-------------------------+
            // TODO More options, mb generator?
            if (meta instanceof BlockStateMeta) {
                BlockStateMeta bmeta  = (BlockStateMeta) meta;
                Banner         banner = (Banner) bmeta.getBlockState();

                DyeColor bBaseColor    = Rnd.get(DyeColor.values());
                DyeColor bPatternColor = Rnd.get(DyeColor.values());
                banner.setBaseColor(bBaseColor);

                try {
                    PatternType bPattern = Rnd.get(PatternType.values());
                    banner.addPattern(new Pattern(bPatternColor, bPattern));
                } catch (IncompatibleClassChangeError ignored) {
                    try {
                        Class<?> pattern  = Reflex.getClass("org.bukkit.block.banner.PatternType");
                        Object[] patterns = pattern.getEnumConstants();
                        Object   bPattern = Rnd.get(patterns);
                        banner.addPattern(Pattern.class.getConstructor(DyeColor.class, pattern)
                                .newInstance(bPatternColor, bPattern));
                    } catch (InvocationTargetException | InstantiationException | NoSuchMethodException |
                             IllegalAccessException e) {
                        plugin.getLogger().warning("Failed to create banner pattern: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                banner.update();
                bmeta.setBlockState(banner);
            }

            if (!armorTrims.isEmpty()) {
                String trimString = armorTrims.ceilingEntry(Rnd.nextDouble() * armorTrims.lastKey()).getValue();
                VersionManager.getArmorUtil().addTrim(meta, trimString.split(":")[0], trimString.split(":")[1]);
            }

            item.setItemMeta(meta);

            // Add enchants
            int enchRoll =
                    Rnd.get(this.getMinEnchantments(), this.getMaxEnchantments());
            int                                    enchCount = 0;
            List<Map.Entry<Enchantment, String[]>> enchants  = new ArrayList<>(this.enchantsList.entrySet());
            Collections.shuffle(enchants);

            for (Map.Entry<Enchantment, String[]> e : enchants) {
                if (enchCount >= enchRoll) {
                    break;
                }
                Enchantment enchant    = e.getKey();
                int[]       enchLevels = this.doMathExpression(itemLvl, e.getValue());
                int         enchLevel  = Rnd.get(enchLevels[0], enchLevels[1]);
                if (enchLevel < 1) continue;

                if (this.isSafeEnchant()) {
                    if (!enchant.canEnchantItem(item) || !ItemUtils.checkEnchantConflict(item, enchant)) {
                        continue;
                    }
                }
                if (this.isEnchantsSafeLevels()) {
                    enchLevel = Math.min(enchant.getMaxLevel(), enchLevel);
                }
                item.addUnsafeEnchantment(enchant, enchLevel);
                enchCount++;
            }

            // Quick fix for skull textures of ModuleItem because of ItemGen materials.
            // TODO
            ItemUT.addSkullTexture(item, this.hash, this.getId());

            this.getAttributeGenerators().forEach(generator -> generator.generate(item, itemLvl));

            LoreUT.replacePlaceholder(item, PLACE_GEN_DAMAGE, null);
            LoreUT.replacePlaceholder(item, PLACE_GEN_DEFENSE, null);

            LevelRequirement reqLevel = ItemRequirements.getUserRequirement(LevelRequirement.class);
            if (reqLevel != null) {
                reqLevel.add(item, this.getUserLevelRequirement(itemLvl), -1);
            }

            String[]                         userClass = this.getUserClassRequirement(itemLvl);
            DynamicUserRequirement<String[]> reqClass  = null;
            if (userClass == null || userClass.length == 0) {
                userClass = this.getUserBannedClassRequirement(itemLvl);
                if (userClass != null && userClass.length > 0) {
                    reqClass = ItemRequirements.getUserRequirement(BannedClassRequirement.class);
                }
            } else {
                reqClass = ItemRequirements.getUserRequirement(ClassRequirement.class);
            }
            if (reqClass != null) {
                reqClass.add(item, userClass, -1);
            }

            LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_REQ_USER_LEVEL, null);
            LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_REQ_USER_CLASS, null);
            LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_REQ_USER_BANNED_CLASS, null);

            // Replace %SOULBOUND% placeholder.
            SoulboundRequirement reqSoul = ItemRequirements.getUserRequirement(SoulboundRequirement.class);
            if (reqSoul != null && reqSoul.hasPlaceholder(item)) {
                reqSoul.add(item, -1);
            }

            // Replace %SET% placeholder.
            SetManager setManager = plugin.getModuleCache().getSetManager();
            if (setManager != null) {
                setManager.updateItemSet(item, null);
            }
            LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_ITEM_SET, null);

            // Replace %ENCHANTS% placeholder
            LoreUT.replaceEnchants(item);
            LoreUT.replacePlaceholder(item, "%TYPE%", itemGroupName);
            LoreUT.replacePlaceholder(item, "%MATERIAL%", plugin.lang().getEnum(item.getType()));

            // Delete left Attribute placeholders.
            meta = item.getItemMeta();
            if (meta == null) return item;
            lore = meta.getLore();
            if (lore == null) return item;

            for (TypedStat at : ItemStats.getStats()) {
                lore.remove(at.getPlaceholder());
            }
            for (ItemLoreStat<?> at : ItemStats.getDamages()) {
                lore.remove(at.getPlaceholder());
            }
            for (ItemLoreStat<?> at : ItemStats.getDefenses()) {
                lore.remove(at.getPlaceholder());
            }
            FabledHook fabledHook = (FabledHook) Divinity.getInstance().getHook(EHook.SKILL_API);
            if (fabledHook != null) {
                for (ItemLoreStat<?> at : fabledHook.getAttributes()) {
                    lore.remove(at.getPlaceholder());
                }
            }
            for (SocketAttribute.Type socketType : SocketAttribute.Type.values()) {
                for (ItemLoreStat<?> at : ItemStats.getSockets(socketType)) {
                    lore.remove(at.getPlaceholder());
                }
            }
            // TODO
            meta.setLore(lore);
            item.setItemMeta(meta);

            return item;
        }
    }

    public static List<String> getMatchingTierPrefixes(Tier tier) {
        return new ArrayList<>(resourceManager.getPrefix(ResourceCategory.TIER, tier.getId()));
    }

    public static List<String> getMatchingTierSuffixes(Tier tier) {
        return new ArrayList<>(resourceManager.getSuffix(ResourceCategory.TIER, tier.getId()));
    }

    public static List<String> getMatchingMaterialPrefixes(Material material) {
        return new ArrayList<>(resourceManager.getPrefix(ResourceCategory.MATERIAL, material.name()));
    }

    public static List<String> getMatchingMaterialSuffixes(Material material) {
        return new ArrayList<>(resourceManager.getSuffix(ResourceCategory.MATERIAL, material.name()));
    }

    public static List<String> getMatchingTypePrefixes(ItemStack item) {
        return new ArrayList<>(resourceManager.getPrefix(ResourceCategory.SUBTYPE, ItemUtils.getItemGroupIdFor(item)));
    }

    public static List<String> getMatchingTypeSuffixes(ItemStack item) {
        return new ArrayList<>(resourceManager.getSuffix(ResourceCategory.SUBTYPE, ItemUtils.getItemGroupIdFor(item)));
    }


    public static Map<ResourceCategory, List<String>> getMatchingPrefixes(Material material, Collection<Tier> tiers) {
        Map<ResourceCategory, List<String>> entries = new HashMap<>();
        entries.put(ResourceCategory.TIER, new ArrayList<>());
        tiers.forEach(tier -> entries.get(ResourceCategory.TIER).addAll(getMatchingTierPrefixes(tier)));
        entries.put(ResourceCategory.MATERIAL, resourceManager.getPrefix(ResourceCategory.MATERIAL, material.name()));
        entries.put(ResourceCategory.SUBTYPE,
                resourceManager.getPrefix(ResourceCategory.SUBTYPE,
                        ItemUtils.getItemGroupIdFor(new ItemStack(material))));
        return entries;
    }

    public static Map<ResourceCategory, List<String>> getMatchingSuffixes(Material material, Collection<Tier> tiers) {
        Map<ResourceCategory, List<String>> entries = new HashMap<>();
        entries.put(ResourceCategory.TIER, new ArrayList<>());
        tiers.forEach(tier -> entries.get(ResourceCategory.TIER).addAll(getMatchingTierSuffixes(tier)));
        entries.put(ResourceCategory.MATERIAL, resourceManager.getSuffix(ResourceCategory.MATERIAL, material.name()));
        entries.put(ResourceCategory.SUBTYPE,
                resourceManager.getSuffix(ResourceCategory.SUBTYPE,
                        ItemUtils.getItemGroupIdFor(new ItemStack(material))));
        return entries;
    }

    /* Kind of a method to play around for all combinations since those might be required on third party plugins (Also on Fusion) */
    public static List<String> getAllCombinations(GeneratorItem item, Material material) {
        String                              name          = item.getName();
        String                              itemGroupName = ItemUtils.getItemGroupNameFor(new ItemStack(material));
        List<String>                        names         = new ArrayList<>();
        Map<ResourceCategory, List<String>> prefixes      = getMatchingPrefixes(material, Config.getTiers());
        Map<ResourceCategory, List<String>> suffixes      = getMatchingSuffixes(material, Config.getTiers());

        names.add(name.replace("%item_type%", itemGroupName)
                .replace("%prefix_tier%", "")
                .replace("%prefix_material%", "")
                .replace("%prefix_type%", "")
                .replace("%suffix_tier%", "")
                .replace("%suffix_material%", "")
                .replace("%suffix_type%", ""));

        /* All prefix combination */
        // Only tier
        if (name.contains("%prefix_tier%") && !name.contains("%prefix_material%") && !name.contains("%prefix_type%")) {
            for (String _prefixTier : prefixes.get(ResourceCategory.TIER)) {
                String modifiedName = name.replace("%item_type%", itemGroupName)
                        .replace("%prefix_tier%", _prefixTier)
                        .replace("%prefix_material%", "")
                        .replace("%prefix_type%", "");
                names.add(modifiedName.replace("%suffix_tier%", "")
                        .replace("%suffix_material%", "")
                        .replace("%suffix_type%", ""));
                names.addAll(interpolatePrefixWithAllSuffixes(modifiedName, suffixes));
            }
        }
        // Only material
        if (!name.contains("%prefix_tier%") && !name.contains("%prefix_material%") && name.contains("%prefix_type%")) {
            for (String _prefixMaterial : prefixes.get(ResourceCategory.MATERIAL)) {
                String modifiedName = name.replace("%item_type%", itemGroupName)
                        .replace("%prefix_tier%", "")
                        .replace("%prefix_material%", _prefixMaterial)
                        .replace("%prefix_type%", "");
                names.add(modifiedName.replace("%suffix_tier%", "")
                        .replace("%suffix_material%", "")
                        .replace("%suffix_type%", ""));
                names.addAll(interpolatePrefixWithAllSuffixes(modifiedName, suffixes));
            }
        }
        // Only type
        if (!name.contains("%prefix_tier%") && name.contains("%prefix_material%") && !name.contains("%prefix_type%")) {
            for (String _prefixItemType : prefixes.get(ResourceCategory.SUBTYPE)) {
                String modifiedName = name.replace("%item_type%", itemGroupName)
                        .replace("%prefix_tier%", "")
                        .replace("%prefix_material%", "")
                        .replace("%prefix_type%", _prefixItemType);
                names.add(modifiedName.replace("%suffix_tier%", "")
                        .replace("%suffix_material%", "")
                        .replace("%suffix_type%", ""));
                names.addAll(interpolatePrefixWithAllSuffixes(modifiedName, suffixes));
            }
        }
        // Tier and Material
        if (name.contains("%prefix_tier%") && name.contains("%prefix_material%") && !name.contains("%prefix_type%")) {
            for (String _prefixTier : prefixes.get(ResourceCategory.TIER)) {
                for (String _prefixMaterial : prefixes.get(ResourceCategory.MATERIAL)) {
                    String modifiedName = name.replace("%item_type%", itemGroupName)
                            .replace("%prefix_tier%", _prefixTier)
                            .replace("%prefix_material%", _prefixMaterial)
                            .replace("%prefix_type%", "");
                    names.add(modifiedName.replace("%suffix_tier%", "")
                            .replace("%suffix_material%", "")
                            .replace("%suffix_type%", ""));
                    names.addAll(interpolatePrefixWithAllSuffixes(modifiedName, suffixes));
                }
            }
        }
        // Tier and Type
        if (name.contains("%prefix_tier%") && !name.contains("%prefix_material%") && name.contains("%prefix_type%")) {
            for (String _prefixTier : prefixes.get(ResourceCategory.TIER)) {
                for (String _prefixItemType : prefixes.get(ResourceCategory.SUBTYPE)) {
                    String modifiedName = name.replace("%item_type%", itemGroupName)
                            .replace("%prefix_tier%", _prefixTier)
                            .replace("%prefix_material%", "")
                            .replace("%prefix_type%", _prefixItemType);
                    names.add(modifiedName.replace("%suffix_tier%", "")
                            .replace("%suffix_material%", "")
                            .replace("%suffix_type%", ""));
                    names.addAll(interpolatePrefixWithAllSuffixes(modifiedName, suffixes));
                }
            }
        }
        // Material and Type
        if (!name.contains("%prefix_tier%") && name.contains("%prefix_material%") && name.contains("%prefix_type%")) {
            for (String _prefixMaterial : prefixes.get(ResourceCategory.MATERIAL)) {
                for (String _prefixItemType : prefixes.get(ResourceCategory.SUBTYPE)) {
                    String modifiedName = name.replace("%item_type%", itemGroupName)
                            .replace("%prefix_tier%", "")
                            .replace("%prefix_material%", _prefixMaterial)
                            .replace("%prefix_type%", _prefixItemType);
                    names.add(modifiedName.replace("%suffix_tier%", "")
                            .replace("%suffix_material%", "")
                            .replace("%suffix_type%", ""));
                    names.addAll(interpolatePrefixWithAllSuffixes(modifiedName, suffixes));
                }
            }
        }
        // All prefix combination
        if (name.contains("%prefix_tier%") && name.contains("%prefix_material%") && name.contains("%prefix_type%")) {
            for (String _prefixTier : prefixes.get(ResourceCategory.TIER)) {
                for (String _prefixMaterial : prefixes.get(ResourceCategory.MATERIAL)) {
                    for (String _prefixItemType : prefixes.get(ResourceCategory.SUBTYPE)) {
                        String modifiedName = name.replace("%item_type%", itemGroupName)
                                .replace("%prefix_tier%", _prefixTier)
                                .replace("%prefix_material%", _prefixMaterial)
                                .replace("%prefix_type%", _prefixItemType);
                        names.add(modifiedName.replace("%suffix_tier%", "")
                                .replace("%suffix_material%", "")
                                .replace("%suffix_type%", ""));
                        names.addAll(interpolatePrefixWithAllSuffixes(modifiedName, suffixes));
                    }
                }
            }
        }


        // All suffix single
        // Only tier
        if (name.contains("%suffix_tier%") && !name.contains("%suffix_material%") && !name.contains("%suffix_type%")) {
            for (String _suffixTier : suffixes.get(ResourceCategory.TIER)) {
                names.add(name.replace("%item_type%", itemGroupName)
                        .replace("%prefix_tier%", "")
                        .replace("%prefix_material%", "")
                        .replace("%prefix_type%", "")
                        .replace("%suffix_tier%", _suffixTier)
                        .replace("%suffix_material%", "")
                        .replace("%suffix_type%", ""));
            }
        }
        // Only material
        if (!name.contains("%suffix_tier%") && !name.contains("%suffix_material%") && name.contains("%suffix_type%")) {
            for (String _suffixMaterial : suffixes.get(ResourceCategory.MATERIAL)) {
                names.add(name.replace("%item_type%", itemGroupName)
                        .replace("%prefix_tier%", "")
                        .replace("%prefix_material%", "")
                        .replace("%prefix_type%", "")
                        .replace("%suffix_tier%", "")
                        .replace("%suffix_material%", _suffixMaterial)
                        .replace("%suffix_type%", ""));
            }
        }
        // Only type
        if (!name.contains("%suffix_tier%") && name.contains("%suffix_material%") && !name.contains("%suffix_type%")) {
            for (String _suffixItemType : suffixes.get(ResourceCategory.SUBTYPE)) {
                names.add(name.replace("%item_type%", itemGroupName)
                        .replace("%prefix_tier%", "")
                        .replace("%prefix_material%", "")
                        .replace("%prefix_type%", "")
                        .replace("%suffix_tier%", "")
                        .replace("%suffix_material%", "")
                        .replace("%suffix_type%", _suffixItemType));
            }
        }
        // Tier and Material
        if (name.contains("%suffix_tier%") && name.contains("%suffix_material%") && !name.contains("%suffix_type%")) {
            for (String _suffixTier : suffixes.get(ResourceCategory.TIER)) {
                for (String _suffixMaterial : suffixes.get(ResourceCategory.MATERIAL)) {
                    names.add(name.replace("%item_type%", itemGroupName)
                            .replace("%prefix_tier%", "")
                            .replace("%prefix_material%", "")
                            .replace("%prefix_type%", "")
                            .replace("%suffix_tier%", _suffixTier)
                            .replace("%suffix_material%", _suffixMaterial)
                            .replace("%suffix_type%", ""));
                }
            }
        }
        // Tier and Type
        if (name.contains("%suffix_tier%") && !name.contains("%suffix_material%") && name.contains("%suffix_type%")) {
            for (String _suffixTier : suffixes.get(ResourceCategory.TIER)) {
                for (String _suffixItemType : suffixes.get(ResourceCategory.SUBTYPE)) {
                    names.add(name.replace("%item_type%", itemGroupName)
                            .replace("%prefix_tier%", "")
                            .replace("%prefix_material%", "")
                            .replace("%prefix_type%", "")
                            .replace("%suffix_tier%", _suffixTier)
                            .replace("%suffix_material%", "")
                            .replace("%suffix_type%", _suffixItemType));
                }
            }
        }
        // Material and Type
        if (!name.contains("%suffix_tier%") && name.contains("%suffix_material%") && name.contains("%suffix_type%")) {
            for (String _suffixMaterial : suffixes.get(ResourceCategory.MATERIAL)) {
                for (String _suffixItemType : suffixes.get(ResourceCategory.SUBTYPE)) {
                    names.add(name.replace("%item_type%", itemGroupName)
                            .replace("%prefix_tier%", "")
                            .replace("%prefix_material%", "")
                            .replace("%prefix_type%", "")
                            .replace("%suffix_tier%", "")
                            .replace("%suffix_material%", _suffixMaterial)
                            .replace("%suffix_type%", _suffixItemType));
                }
            }
        }
        // All suffix combination
        if (name.contains("%suffix_tier%") && name.contains("%suffix_material%") && name.contains("%suffix_type%")) {
            for (String _suffixTier : suffixes.get(ResourceCategory.MATERIAL)) {
                for (String _suffixMaterial : suffixes.get(ResourceCategory.MATERIAL)) {
                    for (String _suffixItemType : suffixes.get(ResourceCategory.SUBTYPE)) {
                        names.add(name.replace("%item_type%", itemGroupName)
                                .replace("%prefix_tier%", "")
                                .replace("%prefix_material%", "")
                                .replace("%prefix_type%", "")
                                .replace("%suffix_tier%", _suffixTier)
                                .replace("%suffix_material%", _suffixMaterial)
                                .replace("%suffix_type%", _suffixItemType));
                    }
                }
            }
        }

        return names;
    }

    private static List<String> interpolatePrefixWithAllSuffixes(String modifiedName,
                                                                 Map<ResourceCategory, List<String>> suffixes) {
        List<String> names = new ArrayList<>();
        // Only tier
        if (modifiedName.contains("%suffix_tier%") && !modifiedName.contains("%suffix_material%")
                && !modifiedName.contains("%suffix_type%")) {
            for (String _suffixTier : suffixes.get(ResourceCategory.TIER)) {
                names.add(modifiedName.replace("%suffix_tier%", _suffixTier)
                        .replace("%suffix_material%", "")
                        .replace("%suffix_type%", ""));
            }
        }
        // Only material
        if (!modifiedName.contains("%suffix_tier%") && !modifiedName.contains("%suffix_material%")
                && modifiedName.contains("%suffix_type%")) {
            for (String _suffixMaterial : suffixes.get(ResourceCategory.MATERIAL)) {
                names.add(modifiedName.replace("%suffix_tier%", "")
                        .replace("%suffix_material%", _suffixMaterial)
                        .replace("%suffix_type%", ""));
            }
        }
        // Only type
        if (!modifiedName.contains("%suffix_tier%") && modifiedName.contains("%suffix_material%")
                && !modifiedName.contains("%suffix_type%")) {
            for (String _suffixItemType : suffixes.get(ResourceCategory.SUBTYPE)) {
                names.add(modifiedName.replace("%suffix_tier%", "")
                        .replace("%suffix_material%", "")
                        .replace("%suffix_type%", _suffixItemType));
            }
        }
        // Tier and Material
        if (modifiedName.contains("%suffix_tier%") && modifiedName.contains("%suffix_material%")
                && !modifiedName.contains("%suffix_type%")) {
            for (String _suffixTier : suffixes.get(ResourceCategory.TIER)) {
                for (String _suffixMaterial : suffixes.get(ResourceCategory.MATERIAL)) {
                    names.add(modifiedName.replace("%suffix_tier%", _suffixTier)
                            .replace("%suffix_material%", _suffixMaterial)
                            .replace("%suffix_type%", ""));
                }
            }
        }
        // Tier and Type
        if (modifiedName.contains("%suffix_tier%") && !modifiedName.contains("%suffix_material%")
                && modifiedName.contains("%suffix_type%")) {
            for (String _suffixTier : suffixes.get(ResourceCategory.TIER)) {
                for (String _suffixItemType : suffixes.get(ResourceCategory.SUBTYPE)) {
                    names.add(modifiedName.replace("%suffix_tier%", _suffixTier)
                            .replace("%suffix_material%", "")
                            .replace("%suffix_type%", _suffixItemType));
                }
            }
        }
        // Material and Type
        if (!modifiedName.contains("%suffix_tier%") && modifiedName.contains("%suffix_material%")
                && modifiedName.contains("%suffix_type%")) {
            for (String _suffixMaterial : suffixes.get(ResourceCategory.MATERIAL)) {
                for (String _suffixItemType : suffixes.get(ResourceCategory.SUBTYPE)) {
                    names.add(modifiedName.replace("%suffix_tier%", "")
                            .replace("%suffix_material%", _suffixMaterial)
                            .replace("%suffix_type%", _suffixItemType));
                }
            }
        }
        // All suffix combination
        for (String _suffixTier : suffixes.get(ResourceCategory.MATERIAL)) {
            for (String _suffixMaterial : suffixes.get(ResourceCategory.MATERIAL)) {
                for (String _suffixItemType : suffixes.get(ResourceCategory.SUBTYPE)) {
                    names.add(modifiedName.replace("%suffix_tier%", _suffixTier)
                            .replace("%suffix_material%", _suffixMaterial)
                            .replace("%suffix_type%", _suffixItemType));
                }
            }
        }

        return names;
    }
}
