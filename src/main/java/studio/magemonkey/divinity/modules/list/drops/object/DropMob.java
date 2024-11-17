package studio.magemonkey.divinity.modules.list.drops.object;

import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.LoadableItem;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.drops.DropManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DropMob extends LoadableItem implements DropCalculator {

    protected float   chance;
    protected boolean rollOnce;

    protected Set<String> entityGood;
    protected Set<String> mythicGood;
    protected Set<String> reasonsBad;

    @Getter
    protected boolean vanillaDrops;

    protected List<DropTable> dropTables;

    public DropMob(@NotNull Divinity plugin, @NotNull JYML cfg, DropManager dropManager) {
        super(plugin, cfg);

        this.chance = (float) cfg.getDouble("chance");
        this.rollOnce = cfg.getBoolean("roll-once");

        this.entityGood = new HashSet<>(cfg.getStringList("vanilla-mobs"));
        this.mythicGood = new HashSet<>(cfg.getStringList("mythic-mobs"));
        this.reasonsBad = new HashSet<>(cfg.getStringList("prevent-from"));

        this.vanillaDrops = cfg.getBoolean("vanilla-drops", true);

        this.dropTables = new ArrayList<>();
//        DropManager dropManager = plugin.getModuleCache().getDropManager();
        if (dropManager != null) {
            for (String tableId : cfg.getStringList("drop-tables")) {
                DropTable dropTable = dropManager.getTableById(tableId);
                if (dropTable == null) {
                    dropManager.error("Invalid drop table " + tableId + " in " + cfg.getFile().getName());
                    continue;
                }
                this.dropTables.add(dropTable);
            }
        }

        if (this.dropTables.isEmpty()) {
            throw new IllegalStateException("Empty drop tables for " + cfg.getFile().getName());
        }
    }

    public boolean isRollOnce() {
        return this.rollOnce;
    }

    public float getChance() {
        return this.chance;
    }

    @NotNull
    public Set<String> getEntities() {
        return this.entityGood;
    }

    @NotNull
    public Set<String> getMythic() {
        return this.mythicGood;
    }

    @NotNull
    public Set<String> getReasons() {
        return this.reasonsBad;
    }

    @NotNull
    public List<DropTable> getDropTables() {
        return this.dropTables;
    }

    @Override
    protected void save(@NotNull JYML cfg) {

    }

    @Override
    public Set<Drop> dropCalculator(
            @Nullable Player killer,
            @NotNull LivingEntity npc,
            float dropModifier) {
        Set<Drop> drops = new HashSet<>();

        float percent = this.chance;
        percent *= dropModifier;

        if (Rnd.get(true) >= percent) {
            return drops;
        }

        if (this.rollOnce) {
            DropTable dg = Rnd.get(this.dropTables);
            if (dg != null) {
                return dg.dropCalculator(killer, npc, dropModifier);
            }
        }
        for (DropTable dg : this.dropTables) {
            drops.addAll(dg.dropCalculator(killer, npc, dropModifier));
        }
        return drops;
    }
}
