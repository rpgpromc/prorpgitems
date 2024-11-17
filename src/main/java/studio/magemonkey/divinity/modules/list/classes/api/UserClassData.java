package studio.magemonkey.divinity.modules.list.classes.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.divinity.modules.list.classes.ComboManager.ComboKey;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAspect;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAspectBonus;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAttribute;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAttributeType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UserClassData {

    private String                          id;
    private int                             mana;
    private int                             lvl;
    private int                             exp;
    private int                             expToLvl;
    private int                             skillPoints;
    private int                             aspectPoints;
    private Map<String, UserSkillData>      skills;
    private Map<ClassAttributeType, Double> attributes;
    private Map<String, Integer>            aspects;

    private transient RPGClass clazz;

    public UserClassData(@NotNull RPGClass rpgClass) {
        this.clazz = rpgClass;
        this.id = rpgClass.getId();
        this.lvl = Math.max(1, rpgClass.getStartLevel());
        this.mana = rpgClass.getManaMax(this.getLevel());
        this.exp = 0;
        this.expToLvl = rpgClass.getStartExp();
        this.skillPoints = 0;
        this.aspectPoints = 0;
        this.skills = new HashMap<>();
        this.attributes = new HashMap<>();
        this.aspects = new HashMap<>();

        this.updateData();
    }

    public UserClassData(
            @NotNull String id,
            int lvl,
            int mana,
            int exp,
            int expToLvl,
            int skillPoints,
            int aspectPoints,
            @NotNull Map<String, UserSkillData> skills,
            @NotNull Map<ClassAttributeType, Double> attributes,
            @NotNull Map<String, Integer> aspects
    ) {
        this.id = id.toLowerCase();
        this.mana = mana;
        this.lvl = lvl;
        this.exp = exp;
        this.expToLvl = expToLvl;
        this.skillPoints = skillPoints;
        this.aspectPoints = aspectPoints;
        this.skills = skills;
        this.attributes = attributes;
        this.aspects = aspects;
    }

    public void inheritData(@NotNull UserClassData from) {
        this.skillPoints = from.getSkillPoints();
        this.aspectPoints = from.getAspectPoints();
        this.skills = new HashMap<>(from.skills);
        this.aspects = new HashMap<>(from.aspects);
    }

    public void updateData() {
        // Fix exp to level in case if it was changed
        this.expToLvl = this.clazz.getNeedExpForLevel(this.lvl);
        if (this.exp >= this.expToLvl && this.getLevel() < this.clazz.getMaxLevel()) {
            this.upLevel((this.exp - this.expToLvl));
        }

        // Restore Vanilla Attribute values.
        for (ClassAttributeType type : ClassAttributeType.values()) {
            ClassAttribute att = this.clazz.getAttribute(type);
            if (att == null) continue;

            double attDefault = this.clazz.getAttributeValue(type, this.lvl);

            // Add from Aspect Bonuses.
            for (Map.Entry<ClassAspect, ClassAspectBonus> ass : clazz.getAspectBonuses().entrySet()) {
                double has = this.getAspect(ass.getKey());
                attDefault += ass.getValue().getPerPointAttribute(type) * has;
            }

            // Fix max. value
            if (att.getMaxValue() >= 0) {
                attDefault = Math.min(att.getMaxValue(), attDefault);
            }
            this.attributes.put(type, attDefault);
        }

        if (this.getMana() > this.clazz.getManaMax(this.getLevel())) {
            this.setMana(this.clazz.getManaMax(this.getLevel()));
        }
    }

    public RPGClass getPlayerClass() {
        return this.clazz;
    }

    public void setPlayerClass(@NotNull RPGClass clazz) {
        this.clazz = clazz;
    }

    @NotNull
    public String getClassId() {
        return id;
    }

    public int getMana() {
        return mana;
    }

    public int getManaMax() {
        return this.getPlayerClass().getManaMax(this.getLevel());
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getLevel() {
        return this.lvl;
    }

    public void setLevel(int lvl) {
        this.lvl = Math.max(1, lvl);
    }

    public int getExp() {
        return this.exp;
    }

    public void setExp(int exp) {
        this.exp = Math.max(-this.expToLvl, exp);
    }

    public int getExpToUp(boolean total) {
        return total ? this.expToLvl : Math.max(0, this.expToLvl - this.exp);
    }

    public int getSkillPoints() {
        return this.skillPoints;
    }

    public void setSkillPoints(int points) {
        this.skillPoints = points;
    }

    public int getAspectPoints() {
        return this.aspectPoints;
    }

    public void setAspectPoints(int points) {
        this.aspectPoints = points;
    }

    @Nullable
    public UserSkillData getSkillData(@NotNull String id) {
        return this.skills.get(id.toLowerCase());
    }

    @Nullable
    public UserSkillData getSkillData(@NotNull ComboKey[] combo) {
        for (UserSkillData data : this.skills.values()) {
            ComboKey[] c2 = data.getCombo();
            if (Arrays.equals(combo, c2)) {
                return data;
            }
        }
        return null;
    }

    @NotNull
    public Collection<UserSkillData> getSkills() {
        return this.skills.values();
    }

    @NotNull
    public Map<String, UserSkillData> getSkillsMap() {
        return this.skills;
    }

    public boolean addSkill(@NotNull IAbstractSkill skill, int lvl) {
        // Upgrade skill
        UserSkillData has = this.getSkillData(skill.getId());
        if (has != null) {
            if (has.getLevel() < lvl) {
                has.setLevel(lvl);
                return true;
            }
            return false;
        }

        UserSkillData data = new UserSkillData(skill, lvl);
        this.skills.put(skill.getId().toLowerCase(), data);
        return true;
    }

    public boolean takeSkill(@NotNull String id) {
        id = id.toLowerCase();
        if (this.skills.containsKey(id)) {
            this.skills.remove(id);
            return true;
        }
        return false;
    }

    public double getAttribute(@NotNull ClassAttributeType type) {
        return this.attributes.getOrDefault(type, 0D);
    }

    public void addAspect(@NotNull String aspectId, int amount) {
        int has = this.getAspect(aspectId) + amount;
        this.aspects.put(aspectId.toLowerCase(), has);
    }

    @NotNull
    public Map<String, Integer> getAspects() {
        return this.aspects;
    }

    public int getAspect(@NotNull ClassAspect aspect) {
        return this.getAspect(aspect.getId());
    }

    public int getAspect(@NotNull String aspectId) {
        return this.aspects.getOrDefault(aspectId.toLowerCase(), 0);
    }

    public boolean isTimeToChildClass() {
        if (this.clazz.hasChildClass()) {
            int specLvl = this.clazz.getLevelToChild();
            if (specLvl > 0 && this.getLevel() >= specLvl) {
                return true;
            }
        }
        return false;
    }

    public void upLevel(int expLeft) {
        this.lvl += 1;

        int expToNext = this.clazz.getNeedExpForLevel(this.lvl);
        this.exp = expLeft;
        this.expToLvl = expToNext;
        this.skillPoints += this.clazz.getSkillPointsPerLevel();
        this.aspectPoints += this.clazz.getAspectPointsPerLevel();

        if (expLeft >= expToNext) {
            if (this.getLevel() >= this.clazz.getMaxLevel()) {
                this.addExp(1);
            } else {
                this.upLevel(expLeft - expToNext);
            }
        }
    }

    public void downLevel(int exp_left) {
        if (this.getLevel() == this.clazz.getStartLevel()) return;

        int exp_req = this.clazz.getNeedExpForLevel(this.lvl - 1);

        this.exp = exp_req - exp_left;
        this.expToLvl = exp_req;
        this.lvl -= 1;
        this.skillPoints -= this.clazz.getSkillPointsPerLevel();
        this.aspectPoints -= this.clazz.getAspectPointsPerLevel();

        if (this.exp >= exp_req) {
            if (this.lvl >= this.clazz.getMaxLevel()) {
                this.addExp(1);
            } else {
                this.upLevel(this.exp - exp_req);
            }
        } else {
            int mod = Math.abs(this.exp);
            if (mod >= exp_req) {
                if (this.lvl == this.clazz.getStartLevel()) {
                    this.setExp(0);
                } else {
                    this.downLevel((mod - exp_req));
                }
            }
        }
    }

    public void addExp(int amount) {
        if (amount == 0) return;

        int expHas    = this.getExp();
        int expToNext = this.getExpToUp(true);

        if (amount < 0) {
            int mod = Math.abs(amount);
            if (mod + Math.abs(expHas) >= expToNext) {
                if (this.getLevel() == this.clazz.getStartLevel()) {
                    this.setExp(-expToNext);
                } else {
                    int left = mod + Math.abs(expHas) - expToNext;
                    this.downLevel(left);
                }
                return;
            }
        }

        if (expHas + amount < expToNext) {
            this.setExp(expHas + amount);
        } else {
            if (this.getLevel() >= this.clazz.getMaxLevel() || this.isTimeToChildClass()) {
                this.setExp(this.getExpToUp(true));
                return;
            }
            this.upLevel((expHas + amount) - expToNext);
        }
    }
}
