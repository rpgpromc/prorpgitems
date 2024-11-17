package studio.magemonkey.divinity.modules.list.classes.api;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.codex.manager.api.gui.JIcon;
import studio.magemonkey.codex.util.FileUT;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.TimeUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.classes.ClassManager;
import studio.magemonkey.divinity.modules.list.classes.event.PlayerCastSkillEvent;
import studio.magemonkey.divinity.modules.list.classes.event.PlayerPreCastSkillEvent;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Фогус Мультимедиа
 */
public abstract class IAbstractSkill extends IListener<Divinity> {

    protected JYML         cfg;
    protected ClassManager classManager;

    protected String       name;
    protected int          maxLvl;
    protected List<String> desc;
    protected ItemStack    icon;

    protected TreeMap<Integer, Integer>              spCost;
    protected TreeMap<Integer, Double>               manaCost;
    protected TreeMap<Integer, Integer>              cooldown;
    protected TreeMap<Integer, Map<String, Integer>> lvlClassReq;
    protected TreeMap<Integer, Map<String, Integer>> lvlSkillReq;

    public static final Map<String, Map<String, Long>> COOLDOWNS = new HashMap<>();

    public IAbstractSkill(@NotNull Divinity plugin) {
        super(plugin);
    }

    @NotNull
    public abstract String getId();

    public final void load(@NotNull ClassManager m) {
        this.classManager = m;

        try {
            File f = new File(m.getFullPath() + "/skills/" + this.getId());
            if (!f.exists()) {
                f.mkdirs();
            }

            File f2 = new File(m.getFullPath() + "/skills/" + this.getId(), "config.yml");
            if (!f2.exists()) {
                FileUT.copy(this.getClass().getClassLoader().getResourceAsStream("/config.yml"), f2);
            }

            this.cfg = new JYML(f2);
            this.cfg.options().copyDefaults(true);
            this.cfg.addMissing("name", this.getId());
            this.cfg.addMissing("max-level", 1);
            if (!cfg.contains("description")) {
                cfg.set("description", Arrays.asList("&7Skill description here..."));
            }
            if (!cfg.contains("icon")) {
                JIcon icon = new JIcon(Material.GOLDEN_AXE);
                icon.setName("%name%");
                icon.addLore("%description%");
                cfg.setItem("icon", icon.build());
            }

            if (!cfg.contains("skill-points-cost-by-level")) {
                cfg.addMissing("skill-points-cost-by-level.1", 5);
            }
            if (!cfg.contains("mana-cost-by-level")) {
                cfg.addMissing("mana-cost-by-level.1", 5);
            }
            if (!cfg.contains("cooldown-by-level")) {
                cfg.addMissing("cooldown-by-level.1", 5);
            }
            if (!cfg.contains("required-classes-and-levels")) {
                cfg.set("required-classes-and-levels.1.Warrior", 10);
                cfg.set("required-classes-and-levels.1.Templar", 7);
                cfg.set("required-classes-and-levels.1.Archer", 11);
            }
            if (!cfg.contains("required-skills-and-levels")) {
                cfg.set("required-skills-and-levels.1.SomeSkill", 1);
                cfg.set("required-skills-and-levels.1.AnotherSkill", 2);
            }
            this.cfg.saveChanges();


            this.name = StringUT.color(cfg.getString("name", this.getId()));
            this.maxLvl = cfg.getInt("max-level", 1);
            this.desc = cfg.getStringList("description");
            this.icon = cfg.getItem("icon");

            this.spCost = new TreeMap<>();
            for (String sLvl : cfg.getSection("skill-points-cost-by-level")) {
                int lvl = StringUT.getInteger(sLvl, -1);
                if (lvl < 0) continue;

                this.spCost.put(lvl, cfg.getInt("skill-points-cost-by-level." + sLvl));
            }

            this.manaCost = new TreeMap<>();
            for (String sLvl : cfg.getSection("mana-cost-by-level")) {
                int lvl = StringUT.getInteger(sLvl, -1);
                if (lvl < 0) continue;

                this.manaCost.put(lvl, cfg.getDouble("mana-cost-by-level." + sLvl));
            }

            this.cooldown = new TreeMap<>();
            for (String sLvl : cfg.getSection("cooldown-by-level")) {
                int lvl = StringUT.getInteger(sLvl, -1);
                if (lvl < 0) continue;

                this.cooldown.put(lvl, cfg.getInt("cooldown-by-level." + sLvl));
            }

            this.lvlClassReq = new TreeMap<>();
            for (String sLvl : cfg.getSection("required-classes-and-levels")) {
                int lvl = StringUT.getInteger(sLvl, -1);
                if (lvl < 0) continue;

                Map<String, Integer> clazLvl = new HashMap<>();
                for (String sClas : cfg.getSection("required-classes-and-levels." + sLvl)) {
                    int minLvl = cfg.getInt("required-classes-and-levels." + sLvl + "." + sClas);
                    clazLvl.put(sClas.toLowerCase(), minLvl);
                }
                this.lvlClassReq.put(lvl, clazLvl);
            }

            this.lvlSkillReq = new TreeMap<>();
            for (String sLvl : cfg.getSection("required-skills-and-levels")) {
                int lvl = StringUT.getInteger(sLvl, -1);
                if (lvl < 0) continue;

                Map<String, Integer> clazLvl = new HashMap<>();
                for (String sClas : cfg.getSection("required-skills-and-levels." + sLvl)) {
                    int minLvl = cfg.getInt("required-skills-and-levels." + sLvl + "." + sClas);
                    clazLvl.put(sClas.toLowerCase(), minLvl);
                }
                this.lvlSkillReq.put(lvl, clazLvl);
            }

            this.setup();
            this.registerListeners();
        } catch (InvalidConfigurationException e) {
            this.plugin.error("Failed to load skill '" + this.getId() + "': Configuration error");
            e.printStackTrace();
            this.shutdown();
        } catch (Exception ex) {
            throw ex;
        }
    }

    public final void unload() {
        this.shutdown();
        this.unregisterListeners();
    }

    public abstract void setup();

    public abstract void shutdown();

    public final boolean canUse(@NotNull Player caster, int lvl, boolean msg) {
        if (this.isOnCooldown(caster)) {
            if (msg) plugin.lang().Classes_Skill_Cast_Error_Cooldown
                    .replace("%time%", TimeUT.formatTimeLeft(this.getActiveCooldown(caster)))
                    .replace("%skill%", this.getName())
                    .send(caster);
            return false;
        }

        UserClassData cData = this.classManager.getUserData(caster);
        if (cData == null) return false;

        String userClass = cData.getPlayerClass().getId();
        int    req       = this.getMinClassLevelRequirement(lvl, userClass);
        if (req >= 0) {
            int userLvl = cData.getLevel();
            if (userLvl < req) {
                if (msg) plugin.lang().Classes_Skill_Cast_Error_Level
                        .replace("%lvl%", String.valueOf(req))
                        .send(caster);
                return false;
            }
        } else {
            if (msg) plugin.lang().Classes_Skill_Cast_Error_Class
                    .replace("%class%", cData.getPlayerClass().getName())
                    .send(caster);
            return false;
        }

        double manaReq = this.getManaCost(lvl);
        if (manaReq > 0) {
            int manaHas = cData.getMana();
            if (manaHas < manaReq) {
                if (msg) plugin.lang().Classes_Skill_Cast_Error_Mana.send(caster);
                return false;
            }
        }

        Map.Entry<Integer, Map<String, Integer>> eSkills = this.lvlSkillReq.floorEntry(lvl);
        if (eSkills != null) {
            for (Map.Entry<String, Integer> e : eSkills.getValue().entrySet()) {
                String         skillId  = e.getKey();
                IAbstractSkill skillReq = classManager.getSkillById(skillId);
                if (skillReq == null) continue;

                UserSkillData sData = cData.getSkillData(skillId);
                if (sData == null) {
                    if (msg) plugin.lang().Classes_Skill_Cast_Error_Skill_Learn
                            .replace("%skill%", skillReq.getName())
                            .send(caster);
                    return false;
                }

                int userSkillLvl = sData.getLevel();
                int reqSkill     = e.getValue();
                if (userSkillLvl < reqSkill) {
                    if (msg) plugin.lang().Classes_Skill_Cast_Error_Skill_Level
                            .replace("%skill%", skillReq.getName())
                            .replace("%lvl%", String.valueOf(reqSkill))
                            .send(caster);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * @param caster
     * @param weapon
     * @param force  Defines if skill should skip all the requirements
     * @return
     */
    public boolean cast(@NotNull Player caster, @NotNull ItemStack weapon, int lvl, boolean force) {
        if (this.isPassive()) return false;

        UserClassData classData = this.classManager.getUserData(caster);
        if (classData == null) return false;

        if (!force) {
            if (!this.canUse(caster, lvl, !this.isPassive())) {
                return false;
            }
        }

        PlayerPreCastSkillEvent e = new PlayerPreCastSkillEvent(caster, classData, this);
        plugin.getPluginManager().callEvent(e);
        if (e.isCancelled()) return false;

        if (this.onCast(caster, weapon, lvl, force)) {
            double manaReq = this.getManaCost(lvl);
            this.classManager.consumeMana(caster, manaReq, false);
            this.addCooldown(caster, lvl);

            PlayerCastSkillEvent e2 = new PlayerCastSkillEvent(caster, classData, this);
            plugin.getPluginManager().callEvent(e2);

            return true;
        }

        return false;
    }

    protected abstract boolean onCast(@NotNull Player caster, @NotNull ItemStack weapon, int lvl, boolean force);

    public final void addCooldown(@NotNull Player player, int lvl) {
        UserClassData data = classManager.getUserData(player);
        if (data == null) return;

        double reduce = 0D; // TODO Item Stat

        double cd = (this.getCooldown(lvl) * (1D - reduce / 100D)) * 1000D;
        if (cd <= 0) return;

        Map<String, Long> map = COOLDOWNS.get(player.getName());
        if (map == null) map = new HashMap<>();

        map.put(this.getId().toLowerCase(), System.currentTimeMillis() + (int) cd);
        COOLDOWNS.put(player.getName(), map);
    }

    public final boolean isOnCooldown(@NotNull Player player) {
        return System.currentTimeMillis() <= this.getActiveCooldown(player);
    }

    public final long getActiveCooldown(@NotNull Player player) {
        Map<String, Long> map = COOLDOWNS.get(player.getName());
        if (map == null) return 0L;

        return map.getOrDefault(this.getId().toLowerCase(), 0L);
    }

    protected final boolean hasSkill(@NotNull Player player) {
        return this.getSkillLevel(player) > 0;
    }

    protected final int getSkillLevel(@NotNull Player player) {
        int           lvl  = -1;
        UserClassData data = this.classManager.getUserData(player);
        if (data == null) return lvl;

        UserSkillData sData = data.getSkillData(this.getId());
        if (sData == null) return lvl;

        return sData.getLevel();
    }

    // TODO Add Class Stats requirements?

    public abstract String getAuthor();

    public abstract boolean isPassive();

    public abstract boolean canBeStigma();

    public abstract boolean canBeBook();

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public List<String> getDescription(@Nullable Player player, int lvl) {
        List<String> desc = new ArrayList<>();

        String upCost   = String.valueOf(this.getSkillPointsCost(lvl));
        String level    = String.valueOf(lvl);
        String rLevel   = NumberUT.toRoman(lvl);
        String cooldown = TimeUT.formatTime(this.getCooldown(lvl) * 1000L);
        String manaCost = String.valueOf(this.getManaCost(lvl));

        for (String s : new ArrayList<>(this.desc)) {
            if (s.contains("%req-class%")) {
                if (this.lvlClassReq.containsKey(lvl)) {
                    Map<String, Integer> map = this.lvlClassReq.get(lvl);
                    for (Map.Entry<String, Integer> e : map.entrySet()) {
                        String   cId = e.getKey();
                        RPGClass c   = this.classManager.getClassById(cId);
                        if (c == null) continue;

                        int cLvl = e.getValue();
                        String s2 = s.replace("%req-class%", c.getName())
                                .replace("%req-level%", String.valueOf(cLvl));
                        desc.add(s2);
                    }
                }
                continue;
            }
            if (s.contains("%req-skill%")) {
                if (this.lvlSkillReq.containsKey(lvl)) {
                    Map<String, Integer> map = this.lvlSkillReq.get(lvl);
                    for (Map.Entry<String, Integer> e : map.entrySet()) {
                        String         cId   = e.getKey();
                        IAbstractSkill skill = this.classManager.getSkillById(cId);
                        if (skill == null) continue;

                        int cLvl = e.getValue();
                        String s2 = s.replace("%req-skill%", skill.getName())
                                .replace("%req-level%", String.valueOf(cLvl));
                        desc.add(s2);
                    }
                }
                continue;
            }
            if (s.contains("%upgrade-cost%") && lvl >= this.getMaxLevel()) {
                continue;
            }

            desc.add(s
                    .replace("%upgrade-cost%", upCost)
                    .replace("%level%", level)
                    .replace("%rlevel%", rLevel)
                    .replace("%cooldown%", cooldown)
                    .replace("%mana-cost%", manaCost));
        }

        return StringUT.color(desc);
    }

    public final int getMaxLevel() {
        return this.maxLvl;
    }

    @NotNull
    public ItemStack getIcon(@Nullable Player player, int lvl) {
        ItemStack item = new ItemStack(this.icon);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;

        String level  = String.valueOf(lvl);
        String rLevel = NumberUT.toRoman(lvl);

        if (meta.hasDisplayName()) {
            String name = meta.getDisplayName()
                    .replace("%level%", level)
                    .replace("%rlevel%", rLevel)
                    .replace("%name%", this.getName());
            meta.setDisplayName(name);
        }

        List<String> lore  = meta.getLore();
        List<String> lore2 = new ArrayList<>();
        if (lore != null) {
            for (String s : lore) {
                if (s.contains("%description%")) {
                    for (String s2 : this.getDescription(player, lvl)) {
                        lore2.add(s2);
                    }
                    continue;
                }
                lore2.add(s);
            }
            meta.setLore(lore2);
        }
        item.setItemMeta(meta);

        return item;
    }

    public final int getMinSkillLevelRequirement(int lvl, @NotNull String skill) {
        Entry<Integer, Map<String, Integer>> e = this.lvlSkillReq.floorEntry(lvl);
        if (e == null) return -1;

        return e.getValue().getOrDefault(skill.toLowerCase(), -1);
    }

    public final int getMinClassLevelRequirement(int lvl, @NotNull String clazz) {
        Entry<Integer, Map<String, Integer>> e = this.lvlClassReq.floorEntry(lvl);
        if (e == null) return -1;

        return e.getValue().getOrDefault(clazz.toLowerCase(), -1);
    }

    public final int getSkillPointsCost(int lvl) {
        return this.getValueForLevel(lvl, this.spCost);
    }

    public final int getCooldown(int lvl) {
        return this.getValueForLevel(lvl, this.cooldown);
    }

    public final double getManaCost(int lvl) {
        return this.getDoubleValueForLevel(lvl, this.manaCost);
    }

    protected final void fillMapValues(
            @NotNull TreeMap<Integer, Double> map,
            @NotNull JYML cfg,
            @NotNull String path) {

        for (String sLvl : cfg.getSection(path)) {
            int lvl = StringUT.getInteger(sLvl, -1);
            if (lvl < 1) continue;

            double potionLvl = cfg.getDouble(path + "." + sLvl);
            map.put(lvl, potionLvl);
        }
    }

    protected final int getValueForLevel(int lvl, @NotNull TreeMap<Integer, Integer> map) {
        Map.Entry<Integer, Integer> e = map.floorEntry(lvl);
        if (e == null) return 0;

        return e.getValue();
    }

    protected final double getDoubleValueForLevel(int lvl, @NotNull TreeMap<Integer, Double> map) {
        Map.Entry<Integer, Double> e = map.floorEntry(lvl);
        if (e == null) return 0D;

        return e.getValue();
    }
}
