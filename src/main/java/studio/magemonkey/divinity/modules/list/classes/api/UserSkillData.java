package studio.magemonkey.divinity.modules.list.classes.api;

import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.modules.list.classes.ComboManager.ComboKey;

public class UserSkillData {

    private String     id;
    private int        lvl;
    private ComboKey[] combo;

    public UserSkillData(@NotNull IAbstractSkill skill, int lvl) {
        this.id = skill.getId().toLowerCase();
        this.lvl = lvl;
        this.combo = new ComboKey[ComboKey.values().length];
    }

    public UserSkillData(@NotNull String id, int lvl, @NotNull ComboKey[] combo) {
        this.id = id.toLowerCase();
        this.lvl = lvl;
        this.combo = combo;
    }

    @NotNull
    public String getId() {
        return this.id;
    }

    public int getLevel() {
        return this.lvl;
    }

    public void setLevel(int lvl) {
        this.lvl = lvl;
    }

    @NotNull
    public ComboKey[] getCombo() {
        return this.combo;
    }

    public void setCombo(ComboKey[] combo) {
        this.combo = combo;
    }
}
