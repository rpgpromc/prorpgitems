package studio.magemonkey.divinity.modules.list.classes;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.spell.MageSpell;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.manager.api.task.ITask;
import studio.magemonkey.codex.util.FileUT;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.TimeUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.event.EntityStatsBonusUpdateEvent;
import studio.magemonkey.divinity.data.api.DivinityUser;
import studio.magemonkey.divinity.data.api.UserProfile;
import studio.magemonkey.divinity.hooks.external.MagicHK;
import studio.magemonkey.divinity.manager.profile.ProfileManager;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.api.QModule;
import studio.magemonkey.divinity.modules.list.classes.api.IAbstractSkill;
import studio.magemonkey.divinity.modules.list.classes.api.RPGClass;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;
import studio.magemonkey.divinity.modules.list.classes.api.UserSkillData;
import studio.magemonkey.divinity.modules.list.classes.command.*;
import studio.magemonkey.divinity.modules.list.classes.event.PlayerRegainManaEvent;
import studio.magemonkey.divinity.modules.list.classes.gui.ClassPreSelectionGUI;
import studio.magemonkey.divinity.modules.list.classes.gui.ClassSelectionGUI;
import studio.magemonkey.divinity.modules.list.classes.gui.ClassStatsGUI;
import studio.magemonkey.divinity.modules.list.classes.gui.SkillListGUI;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAspect;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAspectBonus;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAttributeType;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.bonus.BonusMap;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassManager extends QModule {

    boolean selectPopChildTime;
    private AspectManager        aspectManager;
    private ComboManager         comboManager;
    private LevelingManager      levelingManager;
    private RPGClass             selectDefClass;
    private boolean              selectOnce;
    private int                  selectCooldown;
    private boolean              selectPopJoin;
    private ClassSelectionGUI    selectMainGUI;
    private ClassSelectionGUI    selectChildGUI;
    private ClassPreSelectionGUI preSelectGUI;
    private SkillListGUI         skillsGUI;
    private ClassStatsGUI        statsGUI;

    private boolean  barHealthEnabled;
    private String   barHealthTitle;
    private BarColor barHealthColor;
    private BarStyle barHealthStyle;
    private boolean  barManaEnabled;
    private String   barManaTitle;
    private BarColor barManaColor;
    private BarStyle barManaStyle;

    private Set<String> noRemind;

    private Map<String, IAbstractSkill> skills;
    private Map<String, RPGClass>       classes;

    private Map<Player, BossBar> barHp;
    private Map<Player, BossBar> barMana;

    private BarTask barTask;

    private MagicHK magicHook;

    public ClassManager(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.CLASSES;
    }

    @Override
    @NotNull
    public String version() {
        return "1.33";
    }

    @Override
    public void setup() {
        cfg.addMissing("boss-bar.health.enabled", true);
        cfg.addMissing("boss-bar.mana.enabled", true);
        cfg.saveChanges();

        this.skills = new HashMap<>();
        this.classes = new HashMap<>();

        this.noRemind = new HashSet<>();
        this.barHp = new WeakHashMap<>();
        this.barMana = new WeakHashMap<>();


        for (ClassAttributeType type : ClassAttributeType.values()) {
            type.setName(plugin.lang().getEnum(type));
        }

        if (cfg.getBoolean("combo.enabled")) {
            this.comboManager = new ComboManager(this);
            this.comboManager.setup();
        }

        this.aspectManager = new AspectManager(this);
        this.aspectManager.setup();

        this.levelingManager = new LevelingManager(this);
        this.levelingManager.setup();

        this.plugin.getConfigManager().extractFullPath(this.getFullPath() + "classes", "yml", false);
        this.plugin.getConfigManager().extractFullPath(this.getFullPath() + "skills", "jar", true);

        this.magicHook = plugin.isHooked(MagicHK.class) ? plugin.getHook(MagicHK.class) : null;


    }

    public void loadClasses() {
        super.onPostSetup();
        for (JYML cfg : JYML.loadAll(this.getFullPath() + "classes", true)) {
            try {
                RPGClass rpgClass = new RPGClass(plugin, cfg);
                this.classes.put(rpgClass.getId(), rpgClass);
            } catch (Exception ex) {
                this.error("Could not load class: " + cfg.getFile().getName());
                ex.printStackTrace();
            }
        }

        for (RPGClass cClass : this.getClasses()) {
            for (String child : new HashSet<>(cClass.getChildClasses())) {
                RPGClass cChild = this.getClassById(child);
                if (cChild == null) {
                    this.error("Invalid child class " + child + " of " + cClass.getId() + " calss!");
                    cClass.getChildClasses().remove(child);
                    continue;
                }
                if (cChild.isChildClass()) {
                    cClass.getChildClasses().remove(child);
                    error("Child class can't have more than 1 parent class! (" + cChild.getId() + ")");
                    continue;
                }
                cChild.setParent(cClass);
            }
        }

        for (File f : FileUT.getFiles(this.getFullPath() + "skills", false)) {
            try {
                this.loadExternalSkill(f);
            } catch (Exception ex) {
                this.error("Could not load skill: " + f.getName());
                ex.printStackTrace();
            }
        }

        for (IAbstractSkill skill : this.skills.values()) {
            try {
                skill.load(this);
            } catch (Exception ex) {
                this.error("Could not setup skill: " + skill.getId());
                ex.printStackTrace();
            }
        }

        this.info("Classes Loaded: " + this.classes.size());
        this.info("Skills Loaded: " + this.skills.size());


        // ------------------------------- SETTINGS ------------------------------
        String path = "selection.";

        String cDef = cfg.getString(path + "default-class", "none");
        if (!cDef.equalsIgnoreCase("none")) {
            this.selectDefClass = this.getClassById(cDef);
            if (this.selectDefClass == null) {
                this.error("Invalid default class! No class will be given to new players.");
            }
        }

        this.selectOnce = cfg.getBoolean(path + "select-once");
        this.selectCooldown = cfg.getInt(path + "cooldown");
        this.selectPopJoin = cfg.getBoolean(path + "gui.popup.on-join");
        this.selectPopChildTime = cfg.getBoolean(path + "gui.popup.on-child-level");
        this.selectMainGUI = new ClassSelectionGUI(this, cfg, path + "gui.main.", true);
        this.selectChildGUI = new ClassSelectionGUI(this, cfg, path + "gui.main.", false);
        this.preSelectGUI = new ClassPreSelectionGUI(this, cfg, path + "gui.type.");

        path = "boss-bar.";
        this.barHealthEnabled = cfg.getBoolean(path + "health.enabled");
        this.barManaEnabled = cfg.getBoolean(path + "mana.enabled");
        this.barHealthTitle = StringUT.color(cfg.getString(path + "health.title", ""));
        this.barManaTitle = StringUT.color(cfg.getString(path + "mana.title", ""));
        try {
            this.barHealthColor = BarColor.valueOf(cfg.getString(path + "health.color", "RED"));
            this.barHealthStyle = BarStyle.valueOf(cfg.getString(path + "health.style", "SOLID"));
            this.barManaColor = BarColor.valueOf(cfg.getString(path + "mana.color", "BLUE"));
            this.barManaStyle = BarStyle.valueOf(cfg.getString(path + "mana.style", "SOLID"));
        } catch (IllegalArgumentException ex) {
            this.barHealthColor = BarColor.RED;
            this.barHealthStyle = BarStyle.SOLID;
            this.barManaColor = BarColor.BLUE;
            this.barManaStyle = BarStyle.SOLID;
        }

        this.moduleCommand.addSubCommand(new AddAspectPointsCmd(this));
        this.moduleCommand.addSubCommand(new AddExpCmd(this));
        this.moduleCommand.addSubCommand(new AddLevelCmd(this));
        this.moduleCommand.addSubCommand(new AddSkillCmd(this));
        this.moduleCommand.addSubCommand(new AddSkillPointsCmd(this));
        this.moduleCommand.addSubCommand(new AspectsCmd(this));
        this.moduleCommand.addSubCommand(new ResetCmd(this));
        this.moduleCommand.addSubCommand(new SelectCmd(this));
        this.moduleCommand.addSubCommand(new SetClassCmd(this));
        this.moduleCommand.addSubCommand(new SkillsCmd(this));
        this.moduleCommand.addSubCommand(new StatsCmd(this));
        this.moduleCommand.addSubCommand(new CastSkillCmd(this));
        this.moduleCommand.addSubCommand(new ResetAspectPointsCmd(this));
        this.moduleCommand.addSubCommand(new ResetSkillPointsCmd(this));

        path = "gui.skill-list.";
        this.skillsGUI = new SkillListGUI(this, cfg, path);
        path = "gui.class-stats.";
        this.statsGUI = new ClassStatsGUI(this, cfg, path);

        this.barTask = new BarTask(plugin);
        this.barTask.start();
    }

    @Override
    public void shutdown() {
        if (this.barTask != null) {
            this.barTask.stop();
            this.barTask = null;
        }
        if (this.comboManager != null) {
            this.comboManager.shutdown();
            this.comboManager = null;
        }

        if (this.levelingManager != null) {
            this.levelingManager.shutdown();
            this.levelingManager = null;
        }

        if (this.barHp != null) {
            this.barHp.forEach((p, bar) -> bar.removeAll());
            this.barHp.clear();
            this.barHp = null;
        }
        if (this.barMana != null) {
            this.barMana.forEach((p, bar) -> bar.removeAll());
            this.barMana.clear();
            this.barMana = null;
        }

        if (this.selectMainGUI != null) {
            this.selectMainGUI.shutdown();
            this.selectMainGUI = null;
        }
        if (this.selectChildGUI != null) {
            this.selectChildGUI.shutdown();
            this.selectChildGUI = null;
        }
        if (this.preSelectGUI != null) {
            this.preSelectGUI.shutdown();
            this.preSelectGUI = null;
        }
        if (this.skillsGUI != null) {
            this.skillsGUI.shutdown();
            this.skillsGUI = null;
        }
        if (this.statsGUI != null) {
            this.statsGUI.shutdown();
            this.statsGUI = null;
        }

        this.classes.clear();
        this.classes = null;

        for (IAbstractSkill s : this.skills.values()) {
            s.unload();
        }
        this.skills.clear();
        this.skills = null;

        IAbstractSkill.COOLDOWNS.clear();

        if (this.aspectManager != null) {
            this.aspectManager.shutdown();
            this.aspectManager = null;
        }
    }

    @Override
    protected void onReload() {
        super.onReload();

        this.plugin.getServer().getOnlinePlayers().forEach(p -> {
            if (p == null) return;
            this.updateClassData(p);
        });
    }

    @NotNull
    public AspectManager getAspectManager() {
        if (this.aspectManager == null) {
            throw new NullPointerException("Not loaded yet.");
        }
        return this.aspectManager;
    }

    @Nullable
    public ComboManager getComboManager() {
        return this.comboManager;
    }

    @NotNull
    public LevelingManager getLevelingManager() {
        return levelingManager;
    }

    public boolean hasMagic() {
        return this.magicHook != null;
    }

    @Nullable
    public MagicHK getMagic() {
        if (this.hasMagic()) {
            return this.magicHook;
        }
        return null;
    }

    public void stopSelectRemind(@NotNull Player player) {
        this.noRemind.add(player.getName());
    }

    public boolean isRemindDisabled(@NotNull Player player) {
        return this.noRemind.contains(player.getName());
    }
    // TODO Boosters


    public void addAspectPoints(@NotNull Player player, int amount) {
        UserClassData cData = this.getUserData(player);
        if (cData == null) return;

        cData.setAspectPoints(cData.getAspectPoints() + amount);

        plugin.lang().Classes_Leveling_Points_Aspect_Get
                .replace("%amount%", String.valueOf(amount))
                .send(player);
    }

    public void addSkillPoints(@NotNull Player player, int amount) {
        UserClassData cData = this.getUserData(player);
        if (cData == null) return;

        cData.setSkillPoints(cData.getSkillPoints() + amount);

        plugin.lang().Classes_Leveling_Points_Skill_Get
                .replace("%amount%", String.valueOf(amount))
                .send(player);
    }

    public void updateBar(@NotNull Player player) {
        this.updateHealthBar(player);
        this.updateManaBar(player);
        this.getLevelingManager().updateExpBossBar(player);
    }

    private void updateHealthBar(@NotNull Player player) {
        if (!this.barHealthEnabled) return;

        BossBar bb = this.barHp.computeIfAbsent(player,
                bar -> plugin.getServer().createBossBar("", this.barHealthColor, this.barHealthStyle));

        double hp  = player.getHealth();
        double max = Math.max(hp, EntityStats.getEntityMaxHealth(player));

        String title = this.barHealthTitle
                .replace("%cur%", NumberUT.format(hp))
                .replace("%max%", NumberUT.format(max));

        if (Hooks.hasPlugin(Hooks.PLACEHOLDER_API)) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }

        double result = 1D - ((max - hp) / max);
        if (result > 1.0) result = 1.0;
        if (result < 0) result = 0.0;

        bb.setProgress(result);
        bb.setTitle(title);
        bb.setVisible(true);
        if (!bb.getPlayers().contains(player)) {
            bb.addPlayer(player);
        }
    }

    private void updateManaBar(@NotNull Player player) {
        if (!this.barManaEnabled) return;

        BossBar bb = this.barMana.computeIfAbsent(player,
                bar -> plugin.getServer().createBossBar("", this.barManaColor, this.barManaStyle));

        UserClassData data = this.getUserData(player);
        if (data == null) {
            bb.removeAll();
            this.barMana.remove(player);
            return;
        }

        double cur = data.getMana();
        double max = Math.max(cur, data.getManaMax());

        String title = this.barManaTitle
                .replace("%cur%", NumberUT.format(cur))
                .replace("%max%", NumberUT.format(max));

        if (Hooks.hasPlugin(Hooks.PLACEHOLDER_API)) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }

        double result = 1D - ((max - cur) / max);
        if (result > 1.0) result = 1.0;
        if (result < 0) result = 0.0;

        bb.setProgress(result);
        bb.setTitle(title);
        bb.setVisible(true);
        if (!bb.getPlayers().contains(player)) {
            bb.addPlayer(player);
        }
    }

    public void updateClassData(@NotNull Player player) {
        UserClassData cData = this.getUserData(player);
        if (cData != null) {
            if (this.getClassById(cData.getClassId()) == null) {
                this.resetClassData(player);
                return;
            }
            cData.updateData(); // Fill stats and attributes for current level
        }

        for (ClassAttributeType type : ClassAttributeType.values()) {
            Attribute         a  = type.getVanillaAttribute();
            AttributeInstance ai = player.getAttribute(a);
            if (ai == null) continue;

            double val = ai.getDefaultValue();
            if (val == 0) val = type.getDefaultValue();
            if (cData != null) val = cData.getAttribute(type);

            ai.setBaseValue(val);
            //System.out.println("ATT " + a.name() + ": " + ai.getBaseValue());
        }

        EntityStats.get(player).updateAll();
        this.updateBar(player);
    }

    /**
     * @param player Player instance
     * @return Returns bonus map with Damage, Defense and Item Stat bonuses.
     */
    @NotNull
    public List<BonusMap> getClassEntityStatsBonuses(@NotNull Player player) {
        List<BonusMap> bonuses = new ArrayList<>();

        UserClassData cData = this.getUserData(player);
        if (cData == null) return bonuses;

        RPGClass rpgClass = cData.getPlayerClass();
        for (Map.Entry<ClassAspect, ClassAspectBonus> e : rpgClass.getAspectBonuses().entrySet()) {
            ClassAspect aspect = e.getKey();
            for (int aspectLvl = 0; aspectLvl < cData.getAspect(aspect.getId()); aspectLvl++) {
                bonuses.add(e.getValue().getBonusMap());
            }
        }

        return bonuses;
    }

    public void regainMana(@NotNull Player player, double amount, boolean ofMax) {
        UserClassData data = this.getUserData(player);
        if (data == null) return;

        double cur = data.getMana();
        double max = data.getManaMax();
        if (cur >= max) return;

        if (ofMax) {
            amount = (max / 100D) * amount;
        }

        PlayerRegainManaEvent e = new PlayerRegainManaEvent(player, data, amount);
        plugin.getPluginManager().callEvent(e);
        if (e.isCancelled()) return;
    }

    public void consumeMana(@NotNull Player player, double amount, boolean ofMax) {
        UserClassData data = this.getUserData(player);
        if (data == null) return;

        double cur = data.getMana();

        if (ofMax) {
            double max = data.getMana();
            amount = max / 100D * amount;
        }

        data.setMana((int) Math.max(0, cur - amount));
        this.updateManaBar(player);
    }

    public void openPreSelectionGUI(@NotNull Player player) {
        this.preSelectGUI.open(player, 1);
    }

    public void openSelectionGUI(@NotNull Player p, boolean main) {
        if (main) {
            if (!this.isAllowedToChangeClass(p)) return;
            this.selectMainGUI.open(p, 1);
        } else {
            UserClassData cData = this.getUserData(p);
            if (cData == null) {
                plugin.lang().Classes_Error_NoClass.send(p);
                return;
            }
            if (!cData.getPlayerClass().hasChildClass()) {
                plugin.lang().Classes_Select_Error_NoChild.send(p);
                return;
            }
            if (!cData.isTimeToChildClass()) {
                plugin.lang().Classes_Select_Error_NoChildYet.send(p);
                return;
            }

            this.selectChildGUI.open(p, 1);
        }
    }

    public void setClassSelectionCooldown(@NotNull Player player) {
        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return;

        UserProfile prof = user.getActiveProfile();
        long        end  = -1L;
        if (!this.selectOnce && this.selectCooldown > 0) {
            end = System.currentTimeMillis() + this.selectCooldown * 1000L * 60L;
        }
        prof.setClassSelectionCooldown(end);
    }

    public boolean isAllowedToChangeClass(@NotNull Player player) {
        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return false;

        UserProfile   prof  = user.getActiveProfile();
        UserClassData cData = prof.getClassData();
        long          end   = prof.getClassSelectionCooldown();

        if (this.selectOnce && (cData != null || end < 0L)) {
            plugin.lang().Classes_Select_Error_Once.send(player);
            return false;
        }

        if (end == 0L) return true;

        if (System.currentTimeMillis() > end) {
            return true;
        } else {
            String left = TimeUT.formatTimeLeft(end);
            plugin.lang().Classes_Select_Error_Cooldown.replace("%time%", left).send(player);
            return false;
        }
    }

    @Nullable
    public UserClassData getUserData(@NotNull Player player) {
        if (Hooks.isNPC(player)) return null;

        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return null;

        UserProfile   prof  = user.getActiveProfile();
        UserClassData cData = prof.getClassData();
        return cData;
    }

    public void setDefaultPlayerClass(@NotNull Player player) {
        if (this.selectDefClass == null) return;
        this.setPlayerClass(player, this.selectDefClass, true);
    }

    public void setPlayerClass(@NotNull Player player, @NotNull RPGClass cNew, boolean force) {
        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return;

        UserProfile   prof     = user.getActiveProfile();
        UserClassData cData    = prof.getClassData();
        UserClassData cDataNew = new UserClassData(cNew);

        if (cData != null) {
            RPGClass cHas = cData.getPlayerClass();

            // Allow to change class without cooldown
            // only if new class is a child of current.
            if (!force) {
                if (!cNew.isChildClass(cHas) && !this.isAllowedToChangeClass(player)) {
                    return;
                }
            }

            // User select or improve their specialization of current class
            if (cNew.isChildClass(cHas)) {
                cDataNew.inheritData(cData);
            }
        }

        prof.setClassData(cDataNew);
        cDataNew.getPlayerClass().executeSelectActions(player);

        plugin.lang().Classes_Select_Done.replace("%class%", cNew.getName()).send(player);

        this.updateClassData(player);
        if (!force) {
            this.setClassSelectionCooldown(player);
        }
    }

    public void resetClassData(@NotNull Player player) {
        DivinityUser user = this.plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return;

        UserProfile prof = user.getActiveProfile();
        prof.setClassData(null);

        this.setDefaultPlayerClass(player);
    }

    @NotNull
    public List<RPGClass> getClasses() {
        return new ArrayList<>(this.classes.values());
    }

    @NotNull
    public Set<String> getClassIds() {
        return this.classes.keySet();
    }

    @Nullable
    public RPGClass getClassById(@NotNull String id) {
        return this.classes.get(id.toLowerCase());
    }

    public void addSkill(@NotNull Player player, @NotNull IAbstractSkill skill, int lvl, boolean force) {
        UserClassData cData = this.getUserData(player);
        if (cData == null) return;

        lvl = Math.min(lvl, skill.getMaxLevel());
        if (!force) {
            if (!skill.canUse(player, lvl, true)) {
                return;
            }

            // Check if already have this skill to upgrade it
            UserSkillData sData = cData.getSkillData(skill.getId());
            if (sData != null) {
                if (sData.getLevel() < lvl) {
                    int upCost = skill.getSkillPointsCost(sData.getLevel());
                    int has    = cData.getSkillPoints();
                    if (has < upCost) {
                        plugin.lang().Classes_Skill_Learn_Error_TooExpensive.send(player);
                        return;
                    }
                    cData.setSkillPoints(has - upCost);
                } else {
                    plugin.lang().Classes_Skill_Learn_Error_Has.send(player);
                    return;
                }
            }
        }

        cData.addSkill(skill, lvl);

        plugin.lang().Classes_Skill_Learn_Done
                .replace("%skill%", skill.getName())
                .replace("%lvl%", String.valueOf(lvl))
                .replace("%rlvl%", NumberUT.toRoman(lvl))
                .send(player);
    }

    public void reallocateSkillPoints(@NotNull Player player) {
        UserClassData data = this.getUserData(player);
        if (data == null) {
            plugin.lang().Classes_Error_NoClass.send(player);
            return;
        }

        int total = 0;
        for (UserSkillData sData : data.getSkills()) {
            IAbstractSkill skill = this.getSkillById(sData.getId());
            if (skill == null) continue;

            for (int lvl = 1; lvl < sData.getLevel(); lvl++) {
                total += skill.getSkillPointsCost(lvl);
            }
            sData.setLevel(1);
        }

        data.setSkillPoints(data.getSkillPoints() + total);
    }

    public void openStatsGUI(@NotNull Player player) {
        if (this.getUserData(player) == null) {
            plugin.lang().Classes_Error_NoClass.send(player);
            return;
        }
        this.statsGUI.open(player, 1);
    }

    public void openSkillsGUI(@NotNull Player player) {
        if (this.getUserData(player) == null) {
            plugin.lang().Classes_Error_NoClass.send(player);
            return;
        }
        this.skillsGUI.open(player, 1);
    }

    @Nullable
    public IAbstractSkill getSkillById(@NotNull String id) {
        return this.skills.get(id.toLowerCase());
    }

    @NotNull
    public Set<String> getSkillIds() {
        return this.skills.keySet();
    }

    @SuppressWarnings("resource")
    private void loadExternalSkill(File jar) {
        if (!jar.getName().endsWith(".jar")) return;

        try {
            JarFile               jarFile = new JarFile(jar);
            Enumeration<JarEntry> e       = jarFile.entries();
            URL[]                 urls    = {new URL("jar:file:" + jar.getPath() + "!/")};
            ClassLoader           cl      = URLClassLoader.newInstance(urls, plugin.getClazzLoader());

            while (e.hasMoreElements()) {
                JarEntry je = (JarEntry) e.nextElement();
                if (je.isDirectory() || !je.getName().endsWith(".class")) {
                    continue;
                }
                String className = je.getName().substring(0, je.getName().length() - 6);
                className = className.replace('/', '.');
                Class<?> c = Class.forName(className, false, cl); // second was 'true'
                if (IAbstractSkill.class.isAssignableFrom(c)) {
                    Class<? extends IAbstractSkill> requirementClass = c.asSubclass(IAbstractSkill.class);
                    Constructor<? extends IAbstractSkill> cstrctr =
                            requirementClass.getConstructor(Divinity.class);
                    IAbstractSkill qskill = cstrctr.newInstance(plugin);
                    if (qskill == null) continue;

                    this.skills.put(qskill.getId().toLowerCase(), qskill);
                    this.info("Loaded Skill: " + qskill.getId() + " by " + qskill.getAuthor() + " [" + jar.getName()
                            + "]");
                }
            }
            //jarFile.close();
        } catch (Exception e) {
            this.error("Could not load skill: " + jar.getName() + " (#2)");
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClassJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        this.updateClassData(player);

        DivinityUser user = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return;

        UserProfile   prof  = user.getActiveProfile();
        UserClassData cData = prof.getClassData();
        if (cData != null) {
            return;
        }

        this.setDefaultPlayerClass(player);
        if (prof.getClassData() != null) return;
        if (this.isRemindDisabled(player)) return;

        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager.isSelectOnJoin()) return;

        if (this.selectPopJoin) {
            this.plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                this.openSelectionGUI(player, true);
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClassBarUpdateDamageTake(EntityDamageEvent e) {
        Entity e1 = e.getEntity();
        if (Hooks.isNPC(e1)) return;

        if (e1.getType() != EntityType.PLAYER) return;
        Player player = (Player) e1;
        this.updateHealthBar(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClassHealthRegen(EntityRegainHealthEvent e) {
        Entity e1 = e.getEntity();
        if (Hooks.isNPC(e1) || e1.getType() != EntityType.PLAYER) return;

        Player player = (Player) e1;
        this.updateHealthBar(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClassManaRegen(PlayerRegainManaEvent e) {
        Player player = e.getPlayer();
        if (Hooks.isNPC(player)) return;

        UserClassData data   = e.getClassData();
        double        amount = e.getAmount();
        double        stat   = EntityStats.get(player).getItemStat(TypedStat.Type.MANA_REGEN, false);
        amount *= (1D + stat / 100D);
        e.setAmount(amount);

        data.setMana((int) Math.min(data.getManaMax(), data.getMana() + e.getAmount()));
        this.updateManaBar(player);
    }

    public boolean castSkill(@NotNull Player p, @NotNull ComboManager.ComboKey[] combo) {
        UserClassData cData = this.getUserData(p);
        if (cData == null) return false;

        UserSkillData sData = cData.getSkillData(combo);
        if (sData == null) return false;

        IAbstractSkill skill = this.getSkillById(sData.getId());

        if (skill == null) {
            if (this.hasMagic()) {
                MagicHK magicHook = this.getMagic();
                if (magicHook != null) {
                    MageController api   = magicHook.getAPI().getController();
                    Mage           mage  = api.getMage(p);
                    MageSpell      spell = mage.getSpell(sData.getId());
                    return spell.cast();
                }
            }
            return false;
        }
        if (skill.isPassive()) {
            return false;
        }

        if (skill.cast(p, p.getInventory().getItemInMainHand(), sData.getLevel(), false)) {
            plugin.lang().Classes_Skill_Cast_Done
                    .replace("%skill%", skill.getName())
                    .send(p);
            return true;
        }
        return false;
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStatsBonusUpdate(EntityStatsBonusUpdateEvent e) {
        if (e.getStats().isPlayer()) {
            Player player = (Player) e.getEntity();
            this.updateBar(player);
        }
    }


    class BarTask extends ITask<Divinity> {

        private int count;

        public BarTask(@NotNull Divinity plugin) {
            super(plugin, 1, false);
            this.count = 0;
        }

        @Override
        public void action() {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player == null) continue;

                UserClassData data = getUserData(player);
                if (data != null) {
                    double regen = data.getPlayerClass().getManaRegen();
                    regainMana(player, regen, true);
                }

                if (this.count == 5) {
                    updateBar(player);
                }
            }
            if (this.count++ >= 5) {
                this.count = -1;
            }
        }
    }
}
