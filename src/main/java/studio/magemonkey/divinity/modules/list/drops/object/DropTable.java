package studio.magemonkey.divinity.modules.list.drops.object;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.external.WorldGuardHK;
import studio.magemonkey.codex.manager.LoadableItem;
import studio.magemonkey.codex.modules.IModule;
import studio.magemonkey.codex.util.EnumUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.actions.ActionManipulator;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.modules.api.QModuleDrop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DropTable extends LoadableItem implements DropCalculator {

    private final boolean          penaltyLevelEnabled;
    private final int              penaltyLevelVariance;
    protected     String           name;
    protected     boolean          rollOnce = true;
    protected     Set<String>      worldsGood;
    protected     Set<String>      biomesGood;
    protected     Set<String>      regionsBad;
    protected     List<DropItem>   dropList;
    protected     Set<DropNonItem> nonItemDrops;

    public DropTable(@NotNull Divinity plugin, @NotNull JYML cfg) {
        super(plugin, cfg);

        this.name = StringUT.color(cfg.getString("name", this.getId()));
        this.rollOnce = cfg.getBoolean("roll-once");

        this.penaltyLevelEnabled = cfg.getBoolean("level-penalty.enabled");
        this.penaltyLevelVariance = cfg.getInt("level-penalty.variance");

        this.worldsGood = new HashSet<>(cfg.getStringList("world-whitelist"));
        this.biomesGood = new HashSet<>(cfg.getStringList("biome-whitelist"));
        this.regionsBad = new HashSet<>(cfg.getStringList("region-blacklist"));

        this.dropList = new ArrayList<>();
        for (String id : cfg.getSection("items")) {
            String path = "items." + id + ".";

            float  itemChance = (float) cfg.getDouble(path + "chance");
            String moduleId   = cfg.getString(path + "module-id", "");

            IModule<?> mod = plugin.getModuleManager().getModule(moduleId);
            if (mod == null || !mod.isLoaded() || !(mod instanceof QModuleDrop<?>)) {
                plugin.error("Invalid module for item " + id + " in drop table " + cfg.getFile().getName());
                continue;
            }

            String itemId = cfg.getString(path + "item-id");
            if (itemId == null) {
                plugin.error("Invalid id for item " + id + " in drop table " + cfg.getFile().getName());
                continue;
            }
            String tier = cfg.getString(path + "tier-id", null);

            QModuleDrop<?> itemModule = (QModuleDrop<?>) mod;
            int            amountMin  = cfg.getInt(path + "min-amount", 1);
            int            amountMax  = cfg.getInt(path + "max-amount", 1);

            String            levelMin       = cfg.getString(path + "min-level", "-1");
            String            levelMax       = cfg.getString(path + "max-level", "-1");
            List<String>      dropConditions = cfg.getStringList(path + "additional-conditions");
            ActionManipulator dropActions    = new ActionManipulator(plugin, cfg, path + "actions-on-drop");

            DropItem di = new DropItem(
                    itemChance,
                    itemModule,
                    itemId,
                    tier,
                    amountMin,
                    amountMax,
                    levelMin,
                    levelMax,
                    dropConditions,
                    dropActions
            );
            this.dropList.add(di);
        }

        this.nonItemDrops = new HashSet<>();

        for (String key : cfg.getSection("non-items")) {

            String path = "non-items." + key;

            if (key.equalsIgnoreCase("money")) {
                nonItemDrops.add(new DropMoney(cfg.getConfigurationSection(path)));
            } else if (key.equalsIgnoreCase("experience")) {
                nonItemDrops.add(new DropExperience(cfg.getConfigurationSection(path)));
            }

        }
    }

    @Override
    protected void save(@NotNull JYML cfg) {

    }

    @NotNull
    public String getGroupName() {
        return name;
    }

    public boolean isRollOnce() {
        return rollOnce;
    }

    public boolean isLevelPenalty() {
        return this.penaltyLevelEnabled && this.penaltyLevelVariance > 0;
    }

    public int getPenaltyVariance() {
        return this.penaltyLevelVariance;
    }

    @NotNull
    public Set<String> getAllowedWorlds() {
        return this.worldsGood;
    }

    @NotNull
    public Set<String> getAllowedBiomes() {
        return this.biomesGood;
    }

    @NotNull
    public Set<String> getDisallowedRegions() {
        return this.regionsBad;
    }

    @NotNull
    public List<DropItem> getDrop() {
        return this.dropList;
    }

    @NotNull
    public Set<DropNonItem> getNonItemDrops() {
        return this.nonItemDrops;
    }

    /**
     * @param npc Instance of a valid entity
     * @return True if drop is possible at this location.
     */
    protected boolean checkForLocation(@NotNull LivingEntity npc) {
        String world = npc.getWorld().getName();
        if (!this.worldsGood.contains(JStrings.MASK_ANY) &&
                !this.worldsGood.contains(world)) return false;


        String biome = EnumUT.getName(npc.getLocation().getBlock().getBiome());
        if (!this.biomesGood.contains(JStrings.MASK_ANY) &&
                !this.biomesGood.contains(biome)) return false;


        WorldGuardHK wg = plugin.getWorldGuard();
        if (wg != null) {
            String region = wg.getRegion(npc);
            return !this.regionsBad.contains(JStrings.MASK_ANY) &&
                    !this.regionsBad.contains(region);
        }

        return true;
    }

    protected boolean canDrop(@NotNull LivingEntity npc) {
        if (!this.checkForLocation(npc)) return false;

        if (this.isLevelPenalty()) {
            Player p = npc.getKiller();
            if (p != null) {
                int    maxDiff   = this.getPenaltyVariance();
                double lvlMob    = EngineCfg.HOOK_MOB_LEVEL_PLUGIN.getMobLevel(npc);
                double lvlPlayer = EngineCfg.HOOK_PLAYER_LEVEL_PLUGIN.getLevel(p);
                return !(lvlPlayer > lvlMob) || !((lvlPlayer - lvlMob) >= maxDiff);
            }
        }
        return true;
    }

    @Override
    public Set<Drop> dropCalculator(
            @Nullable Player killer,
            @NotNull LivingEntity npc,
            float dropModifier) {
        Set<Drop> drops = new HashSet<>();

        if (this.dropList.isEmpty() || !this.canDrop(npc)) {
            return drops;
        }

        if (this.rollOnce) {
            DropItem dropItem = Rnd.get(this.dropList);
            if (dropItem != null) {
                return dropItem.dropCalculator(killer, npc, dropModifier);
            }
        }
        for (DropItem dropItem : this.dropList) {
            drops.addAll(dropItem.dropCalculator(killer, npc, dropModifier));
        }

        return drops;
    }
}
