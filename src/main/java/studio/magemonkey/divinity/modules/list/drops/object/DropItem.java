package studio.magemonkey.divinity.modules.list.drops.object;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.util.actions.ActionManipulator;
import studio.magemonkey.codex.util.eval.Evaluator;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.modules.api.QModuleDrop;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DropItem implements DropCalculator {

    private final float             chance;
    private final QModuleDrop<?>    module;
    private final String            itemId;
    private final String            tierId;
    private final int               amountMin;
    private final int               amountMax;
    private final String            levelMin;
    private final String            levelMax;
    private final List<String>      dropCondidions;
    private final ActionManipulator dropActions;
    protected     boolean           noModifier = false;

    public DropItem(
            float chance,
            @NotNull QModuleDrop<?> module,
            @NotNull String itemId,
            @Nullable String tierId,
            int amountMin,
            int amountMax,
            @NotNull String levelMin,
            @NotNull String levelMax,
            @NotNull List<String> dropConditions,
            @NotNull ActionManipulator dropActions
    ) {
        this.chance = chance;
        this.module = module;
        this.itemId = itemId.toLowerCase();
        this.tierId = tierId;
        this.amountMin = amountMin;
        this.amountMax = amountMax;
        this.levelMin = levelMin;
        this.levelMax = levelMax;
        this.dropCondidions = dropConditions;
        this.dropActions = dropActions;
        //this.noReduce = noReduce; TODO Add Config Option
    }

    @NotNull
    public QModuleDrop<?> getModuleId() {
        return this.module;
    }

    @NotNull
    public String getItemId() {
        return itemId;
    }

    @Nullable
    public String getTierId() {
        return this.tierId;
    }

    public int getMinAmount() {
        return amountMin;
    }

    public int getMaxAmount() {
        return amountMax;
    }

    public float getChance() {
        return chance;
    }

    public boolean isNoModifier() {
        return noModifier;
    }

    public int getLvlMin(@NotNull Player player, @NotNull LivingEntity src) {
        double p_lvl = EngineCfg.HOOK_PLAYER_LEVEL_PLUGIN.getLevel(player);
        double e_lvl = EngineCfg.HOOK_MOB_LEVEL_PLUGIN.getMobLevel(src);

        String ex = this.levelMin.replace("%mob_lvl%", String.valueOf(e_lvl))
                .replace("%player_lvl%", String.valueOf(p_lvl));
        return (int) Evaluator.eval(ex, 1);
    }

    public int getLvlMax(@NotNull Player player, @NotNull LivingEntity src) {
        double p_lvl = EngineCfg.HOOK_PLAYER_LEVEL_PLUGIN.getLevel(player);
        double e_lvl = EngineCfg.HOOK_MOB_LEVEL_PLUGIN.getMobLevel(src);

        String ex = this.levelMax.replace("%mob_lvl%", String.valueOf(e_lvl))
                .replace("%player_lvl%", String.valueOf(p_lvl));
        return (int) Evaluator.eval(ex, 1);
    }

    public int getLevel(@NotNull Player player, @NotNull LivingEntity src) {
        int min = this.getLvlMin(player, src);
        int max = this.getLvlMax(player, src);
        if (min <= 0) min = 1;
        if (max <= 0) return -1;

        return Rnd.get(min, max);
    }

    @NotNull
    public List<String> getConditions() {
        return this.dropCondidions;
    }

    public void executeActions(@NotNull Player player, @NotNull Map<String, Set<Entity>> targetMap) {
        this.dropActions.process(player, targetMap);
    }

    @Override
    public Set<Drop> dropCalculator(
            @Nullable Player killer,
            @NotNull LivingEntity npc,
            float dropModifier) {
        Set<Drop> drops = new HashSet<>();

        float percent = this.noModifier ? this.chance : this.chance * dropModifier;
        if (Rnd.get(true) < percent) {
            Drop dropitem = new Drop(this);
            dropitem.calculateCount();
            drops.add(dropitem);
        }
        return drops;
    }

    @Override
    public String toString() {
        return "Drop [itemId=" + itemId + ", minAmount=" + amountMin + ", maxAmount=" + amountMax + ", chance=" + chance
                + ", noReduce=" + noModifier + "]";
    }
}