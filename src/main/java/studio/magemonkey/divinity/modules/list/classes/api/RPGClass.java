package studio.magemonkey.divinity.modules.list.classes.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.LoadableItem;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.actions.ActionManipulator;
import studio.magemonkey.codex.util.eval.Evaluator;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAspect;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAspectBonus;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAttribute;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAttributeType;
import studio.magemonkey.divinity.stats.bonus.BonusMap;

import java.util.*;

public class RPGClass extends LoadableItem {

    private String            name;
    private List<String>      desc;
    private boolean           isPerm;
    private ItemStack         icon;
    private ActionManipulator actionsSelect;

    private int                             lvlStart;
    private int                             lvlMax;
    private int                             lvlStartExp;
    private String                          lvlFormula;
    private int                             lvlSkillPoints;
    private int                             lvlAspectPoints;
    private TreeMap<Integer, Integer>       lvlExpMap;
    private Map<Integer, ActionManipulator> lvlActions;

    private Set<String> specList;
    private int         specLvlSelect;

    private String                    manaChar;
    private String                    manaName;
    private TreeMap<Integer, Integer> manaByLevel;
    private double                    manaRegen;

    private Map<ClassAttributeType, ClassAttribute> attributes;
    private Map<ClassAspect, ClassAspectBonus>      aspects;

    private RPGClass cParent;

    public RPGClass(@NotNull Divinity plugin, @NotNull JYML cfg) {
        super(plugin, cfg);

        String name = cfg.getString("name");
        if (name == null) name = cfg.getFile().getName().replace(".yml", "");
        this.name = StringUT.color(name);
        this.desc = cfg.getStringList("description");
        this.isPerm = cfg.getBoolean("permission-required");

        this.icon = cfg.getItem("icon");
        this.actionsSelect = new ActionManipulator(plugin, cfg, "actions-on-select");

        String path = "leveling.";
        this.lvlStart = cfg.getInt(path + "start-level", 1);
        this.lvlMax = cfg.getInt(path + "max-level", 100);
        this.lvlStartExp = cfg.getInt(path + "start-exp", 25);
        this.lvlFormula = cfg.getString(path + "exp-formula");
        this.lvlSkillPoints = cfg.getInt(path + "skill-points");
        this.lvlAspectPoints = cfg.getInt(path + "aspect-points");

        // Map with Max(Next Level) Exp values for each level.
        this.lvlExpMap = new TreeMap<>();
        if (this.lvlMax > 0) {
            for (int lvl = this.lvlStart; lvl < (this.lvlMax + 1); lvl++) {
                double prevExp = 0D;
                if (lvl <= this.lvlStart) prevExp = this.lvlStartExp;
                else prevExp = this.lvlExpMap.get(lvl - 1);

                String formula    = this.getExpFormula().replace("%exp%", NumberUT.format(prevExp));
                double expToLevel = Evaluator.eval(formula, 1);
                this.lvlExpMap.put(lvl, (int) expToLevel);
            }
        }

        this.lvlActions = new TreeMap<>();
        for (String sLvl : cfg.getSection(path + "actions-on-level")) {
            int lvl = StringUT.getInteger(sLvl, -1);
            if (lvl <= this.lvlStart) continue;

            ActionManipulator engine = new ActionManipulator(plugin, cfg, path + "actions-on-level." + sLvl);
            this.lvlActions.put(lvl, engine);
        }

        path = "sub-class.";
        this.specList = cfg.getStringSet(path + "sub-classes");
        this.specLvlSelect = cfg.getInt(path + "level-to-change");

        // -----------------------------------------
        // Setup Mana Settings
        // -----------------------------------------
        path = "mana.";
        cfg.addMissing(path + "max-value-by-level.1", 50);

        this.manaChar = StringUT.color(cfg.getString(path + "symbol", "•"));
        this.manaName = StringUT.color(cfg.getString(path + "name", "Mana"));
        this.manaByLevel = new TreeMap<>();
        for (String sLvl : cfg.getSection(path + "max-value-by-level")) {
            int lvl = StringUT.getInteger(sLvl, -1);
            if (lvl <= 0) continue;

            this.manaByLevel.put(lvl, cfg.getInt(path + "max-value-by-level." + sLvl));
        }
        this.manaRegen = cfg.getDouble(path + "regen-of-max", 2.5D);

        // -----------------------------------------
        // Setup class attributes
        // -----------------------------------------
        this.attributes = new HashMap<>();
        for (ClassAttributeType attributeType : ClassAttributeType.values()) {
            path = "attributes." + attributeType.name() + ".";

            cfg.addMissing(path + "start-value", attributeType.getDefaultValue());
            cfg.addMissing(path + "max-value", -1D);
            cfg.addMissing(path + "per-level-value", 0D);

            double aStart = cfg.getDouble(path + "start-value", attributeType.getDefaultValue());
            double aMax   = cfg.getDouble(path + "max-value", -1D);
            double aLvl   = cfg.getDouble(path + "per-level-value");

            ClassAttribute classAttribute = new ClassAttribute(aStart, aMax, aLvl);
            this.attributes.put(attributeType, classAttribute);
        }

        ClassManager classManager = plugin.getModuleCache().getClassManager();

        // Linked to support Aspect order from the config.
        this.aspects = new LinkedHashMap<>();
        for (String aspectId : cfg.getSection("aspects")) {
            path = "aspects." + aspectId + ".";

            // Aspect validate
            ClassAspect aspect = classManager == null ? null : classManager.getAspectManager().getAspectById(aspectId);
            if (classManager == null)
                plugin.error("Class manager is null.");
            if (aspect == null) {
                plugin.error("Invalid aspect '" + aspectId + "' in " + cfg.getFile().getName());
                continue;
            }

            cfg.addMissing(path + "max-value", 100);

            int                             aMax     = cfg.getInt(path + "max-value", -1);
            Map<ClassAttributeType, Double> pointAtt = new HashMap<>();

            for (ClassAttributeType qa : ClassAttributeType.values()) {
                String path2 = path + "per-point-values.attributes." + qa.name();
                double val   = cfg.getDouble(path2);
                pointAtt.put(qa, val);
            }

            BonusMap bonusMap = new BonusMap();
            bonusMap.loadDamages(cfg, path + "per-point-values.damage-types");
            bonusMap.loadDefenses(cfg, path + "per-point-values.defense-types");
            bonusMap.loadStats(cfg, path + "per-point-values.item-stats");

            ClassAspectBonus aspectBonus = new ClassAspectBonus(aMax, pointAtt, bonusMap);

            this.aspects.put(aspect, aspectBonus);
        }


        // Description update
        for (int i = 0; i < desc.size(); i++) {
            String line = desc.get(i);
            for (ClassAttributeType attributeType : ClassAttributeType.values()) {
                ClassAttribute attribute = this.getAttribute(attributeType);
                if (attribute == null) continue;

                line = attribute.replace(attributeType, line, 0, 0, 1);
            }
            this.desc.set(i, line);
        }
        this.desc = StringUT.color(this.desc);


        cfg.saveChanges();
    }

    @Override
    protected void save(@NotNull JYML cfg) {

    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public List<String> getDescription() {
        return new ArrayList<>(this.desc);
    }

    public boolean isPermissionRequired() {
        return this.isPerm;
    }

    public boolean hasPermission(@NotNull Player player) {
        if (!this.isPermissionRequired()) return true;

        String node = Perms.CLASS_CLASS + "." + this.getId();
        return player.hasPermission(node);
    }

    @NotNull
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(this.icon);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        if (meta.hasDisplayName()) {
            meta.setDisplayName(meta.getDisplayName().replace("%name%", this.getName()));
        }

        List<String> lore = meta.getLore();
        if (lore != null) {
            List<String> lore2 = new ArrayList<>();
            for (String line : lore) {
                if (line.contains("%description%")) {
                    for (String desc : this.getDescription()) {
                        lore2.add(desc);
                    }
                    continue;
                } else {
                    lore2.add(line.replace("%name%", this.getName()));
                }
            }
            meta.setLore(lore2);
        }

        item.setItemMeta(meta);

        return item;
    }

    public void executeSelectActions(@NotNull Player exec) {
        this.actionsSelect.process(exec);
    }

    public void executeLevelActions(@NotNull Player exec, int lvl) {
        if (this.lvlActions.containsKey(lvl)) {
            ActionManipulator engine = this.lvlActions.get(lvl);
            engine.process(exec);
        }
    }

    public int getStartLevel() {
        return this.lvlStart;
    }

    public int getMaxLevel() {
        return this.lvlMax;
    }

    public int getStartExp() {
        return this.lvlStartExp;
    }

    public int getNeedExpForLevel(int lvl) {
        Map.Entry<Integer, Integer> e = this.lvlExpMap.floorEntry(lvl);
        return e != null ? e.getValue() : this.getStartExp();
    }

    @NotNull
    public String getExpFormula() {
        return this.lvlFormula;
    }

    public int getSkillPointsPerLevel() {
        return this.lvlSkillPoints;
    }

    public int getAspectPointsPerLevel() {
        return this.lvlAspectPoints;
    }

    public boolean isChildClass() {
        return this.cParent != null;
    }

    public boolean isChildClass(@NotNull RPGClass from) {
        for (RPGClass parent : this.getParents()) {
            if (parent == null) continue;
            if (parent.getChildClasses().contains(this.getId())) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getChildClasses() {
        return this.specList;
    }

    public boolean hasChildClass() {
        return !this.getChildClasses().isEmpty();
    }

    public int getLevelToChild() {
        return this.specLvlSelect;
    }

    @NotNull
    public String getManaSymbol() {
        return this.manaChar;
    }

    @NotNull
    public String getManaName() {
        return this.manaName;
    }

    public int getManaMax(int classLevel) {
        Map.Entry<Integer, Integer> entry = this.manaByLevel.floorEntry(classLevel);
        return entry == null ? 0 : entry.getValue();
    }

    public double getManaRegen() {
        return this.manaRegen;
    }

    @NotNull
    public String formatMana(@NotNull String str) {
        str = str
                .replace("%mana-name%", this.getManaName())
                .replace("%mana-char%", this.getManaSymbol());

        return str;
    }

    @Nullable
    public ClassAttribute getAttribute(@NotNull ClassAttributeType type) {
        return this.attributes.get(type);
    }

    public double getAttributeValue(@NotNull ClassAttributeType type, int lvl) {
        ClassAttribute a = this.getAttribute(type);
        if (a == null) return 0D;

        lvl -= this.lvlStart; // Ignore start levels

        double start = a.getStartValue();
        double per   = a.getPerLevelValue();
        double total = start + (per * lvl);

        if (a.getMaxValue() >= 0) {
            return Math.min(a.getMaxValue(), total);
        }
        return total;
    }

    @NotNull
    public Map<ClassAspect, ClassAspectBonus> getAspectBonuses() {
        return this.aspects;
    }

    @Nullable
    public ClassAspectBonus getAspectBonus(@NotNull ClassAspect aspect) {
        return this.aspects.get(aspect);
    }

    @Nullable
    public RPGClass getParent() {
        return this.cParent;
    }

    @Nullable
    public RPGClass getFirstParent() {
        RPGClass p = this.cParent;
        while (p != null) {
            p = p.getFirstParent();
        }
        return p;
    }

    @NotNull
    public Set<RPGClass> getParents() {
        Set<RPGClass> set = new HashSet<>();

        RPGClass p = this.cParent;
        while (p != null) {
            set.add(p);
            p = p.getFirstParent();
        }

        return set;
    }

    public void setParent(@NotNull RPGClass cParent) {
        this.cParent = cParent;
    }
}
