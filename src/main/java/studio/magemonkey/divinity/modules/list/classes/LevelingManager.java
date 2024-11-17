package studio.magemonkey.divinity.modules.list.classes;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.hooks.external.WorldGuardHK;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.codex.manager.api.Loadable;
import studio.magemonkey.codex.util.CollectionsUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.hooks.external.mythicmobs.AbstractMythicMobsHK;
import studio.magemonkey.divinity.modules.list.classes.api.RPGClass;
import studio.magemonkey.divinity.modules.list.classes.api.UserClassData;
import studio.magemonkey.divinity.modules.list.classes.event.PlayerClassExpGainEvent;
import studio.magemonkey.divinity.modules.list.classes.event.PlayerClassLevelChangeEvent;
import studio.magemonkey.divinity.modules.list.classes.object.ExpObject;
import studio.magemonkey.divinity.modules.list.classes.object.ExpSource;
import studio.magemonkey.divinity.stats.EntityStats;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class LevelingManager extends IListener<Divinity> implements Loadable {

    private final ClassManager classManager;
    private       JYML         cfg;

    private Map<String, Integer> lvlWorlds;
    private Set<String>          expWorldList;
    private boolean              lvlIsWorldBlack;
    private Set<String>          lvlRegionBlack;
    private boolean              lvlIsRegionWhite;

    private boolean                expUseTables;
    private Set<String>            expReasonBlack;
    private Map<String, ExpObject> expSrcVanilla;
    private Map<String, ExpObject> expSrcMythic;

    private boolean              expBossBarEnabled;
    private String               expBossBarTitle;
    private BarColor             expBossBarColor;
    private BarStyle             expBossBarStyle;
    private Map<Player, BossBar> expBossBarCache;

    private int     expLossDeathPercent;
    private boolean expLossAllowNegative;

    private boolean expBalDecEnabled;
    private int     expBalDecInterval;
    private int     expBalDecMaxTimes;
    private int     expBalDecPercent;

    private boolean expBalIncEnabled;
    private int     expBalIncInterval;
    private int     expBalIncMaxTimes;
    private int     expBalIncPercent;

    private AbstractMythicMobsHK mmHook;

    private static final String ENTITY_NO_EXP_SPAWN = "QRPG_NO_DROP_EXP";

    LevelingManager(@NotNull ClassManager classManager) {
        super(classManager.plugin);
        this.classManager = classManager;
    }

    @Override
    public void setup() {
        try {
            this.cfg = JYML.loadOrExtract(this.plugin, this.classManager.getPath() + "leveling.yml");
        } catch (InvalidConfigurationException e) {
            this.plugin.error("Failed to load leveling config (" + this.classManager.getPath()
                    + "leveling.yml): Configuration error");
            e.printStackTrace();
            shutdown();
            return;
        }
        this.mmHook = plugin.getHook(AbstractMythicMobsHK.class);

        this.lvlWorlds = new HashMap<>();
        for (String wName : cfg.getSection("world-levels")) {
            int lvl = cfg.getInt("world-levels." + wName);
            if (lvl <= 0) continue;
            this.lvlWorlds.put(wName, lvl);
        }
        this.expWorldList = cfg.getStringSet("world-whitelist.list");
        this.lvlIsWorldBlack = cfg.getBoolean("world-whitelist.reverse");

        this.lvlRegionBlack = cfg.getStringSet("region-blacklist.list");
        this.lvlIsRegionWhite = cfg.getBoolean("region-blacklist.reverse");

        String path = "exp.";
        this.expUseTables = cfg.getBoolean(path + "use-tables");

        path = "exp.boss-bar.";
        if (this.expBossBarEnabled = cfg.getBoolean(path + "enabled")) {
            this.expBossBarTitle = StringUT.color(cfg.getString(path + "title", ""));
            this.expBossBarColor = CollectionsUT.getEnum(cfg.getString(path + "color", "YELLOW"), BarColor.class);
            this.expBossBarStyle = CollectionsUT.getEnum(cfg.getString(path + "style", "SOLID"), BarStyle.class);
            this.expBossBarCache = new WeakHashMap<>();
        }

        path = "exp.sources.";
        this.expReasonBlack = cfg.getStringSet(path + "prevent-from");
        if (this.expUseTables) {
            this.expSrcVanilla = new HashMap<>();
            for (String sId : cfg.getSection(path + "vanilla-mobs")) {
                String path2  = path + "vanilla-mobs." + sId + ".";
                int    min    = cfg.getInt(path2 + "min");
                int    max    = cfg.getInt(path2 + "max");
                double chance = cfg.getDouble(path2 + "chance");
                this.expSrcVanilla.put(sId.toLowerCase(), new ExpObject(min, max, chance));
            }
            if (this.mmHook != null) {
                this.expSrcMythic = new HashMap<>();
                for (String sId : cfg.getSection(path + "mythic-mobs")) {
                    String path2  = path + "mythic-mobs." + sId + ".";
                    int    min    = cfg.getInt(path2 + "min");
                    int    max    = cfg.getInt(path2 + "max");
                    double chance = cfg.getDouble(path2 + "chance");
                    this.expSrcMythic.put(sId.toLowerCase(), new ExpObject(min, max, chance));
                }
            }
        }

        path = "exp.loss.";
        if (cfg.getBoolean(path + "on-death.enabled")) {
            this.expLossDeathPercent = cfg.getInt(path + "on-death.percent");
            this.expLossAllowNegative = cfg.getBoolean(path + "allow-negative");
        }

        path = "exp.balance.decrease-for-low-lvl-mobs.";
        this.expBalDecEnabled = cfg.getBoolean(path + "enabled");
        this.expBalDecInterval = cfg.getInt(path + "every");
        this.expBalDecMaxTimes = cfg.getInt(path + "max-times");
        this.expBalDecPercent = cfg.getInt(path + "percent");

        path = "exp.balance.increase-for-high-lvl-mobs.";
        this.expBalIncEnabled = cfg.getBoolean(path + "enabled");
        this.expBalIncInterval = cfg.getInt(path + "every");
        this.expBalIncMaxTimes = cfg.getInt(path + "max-times");
        this.expBalIncPercent = cfg.getInt(path + "percent");

        this.registerListeners();
    }

    @Override
    public void shutdown() {
        this.unregisterListeners();

        if (this.expSrcVanilla != null) {
            this.expSrcVanilla.clear();
            this.expSrcVanilla = null;
        }
        if (this.expSrcMythic != null) {
            this.expSrcMythic.clear();
            this.expSrcMythic = null;
        }

        if (this.expBossBarCache != null) {
            this.expBossBarCache.forEach((p, bar) -> bar.removeAll());
            this.expBossBarCache.clear();
            this.expBossBarCache = null;
        }

        this.cfg = null;
    }

    public void updateExpBossBar(@NotNull Player player) {
        if (!this.expBossBarEnabled) return;

        BossBar bar = this.expBossBarCache.computeIfAbsent(player,
                b -> plugin.getServer().createBossBar("", expBossBarColor, expBossBarStyle));

        UserClassData data   = this.classManager.getUserData(player);
        int           expHas = data == null ? 0 : data.getExp();
        int           expMax = data == null ? 0 : data.getExpToUp(true);
        int           level  = data == null ? 0 : data.getLevel();

        String title = this.expBossBarTitle
                .replace("%exp%", String.valueOf(expHas))
                .replace("%exp-max%", String.valueOf(expMax))
                .replace("%level%", String.valueOf(level));

        if (Hooks.hasPlugin(Hooks.PLACEHOLDER_API)) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }

        double progress = expMax > 0 ? (double) expHas / (double) expMax : 0D;

        bar.setTitle(title);
        bar.setProgress(progress);
        bar.setVisible(true);
        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
    }

    public boolean canDropExp(@NotNull Entity entity) {
        boolean isListedWorld = this.expWorldList.contains(entity.getWorld().getName());
        if (isListedWorld == this.lvlIsWorldBlack) {
            return false;
        }

        WorldGuardHK worldGuard = plugin.getWorldGuard();
        if (worldGuard != null) {
            boolean isListedReg = this.lvlRegionBlack.contains(worldGuard.getRegion(entity));
            return isListedReg == this.lvlIsRegionWhite;
        }

        return true;
    }

    public int getExpFromTable(@NotNull Entity from) {
        // TODO EXP_RATE stat for player - more exp chance %
        boolean mythic = this.mmHook != null && mmHook.isMythicMob(from);
        String  type   = mythic ? this.mmHook.getMythicNameByEntity(from) : from.getType().name();

        Map<String, ExpObject> expMap = mythic ? this.expSrcMythic : this.expSrcVanilla;
        ExpObject expObject =
                expMap.getOrDefault(type.toLowerCase(), expMap.getOrDefault(JStrings.DEFAULT.toLowerCase(), null));

        return expObject == null ? 0 : expObject.getExp();
    }

    private double balanceExp(@NotNull Player player, @NotNull Entity from, double exp) {
        return exp * this.getExpPercentByLevel(player, from);
    }

    public void addExp(@NotNull Player player, @NotNull Entity from) {
        double amount = this.balanceExp(player, from, this.getExpFromTable(from));

        String src = EntityStats.getEntityName(from);
        this.addExp(player, (int) amount, src, ExpSource.MOB_KILL);
    }

    public void addExp(Player player,
                       int amount,
                       String src,
                       ExpSource srcExp) {
        if (!this.expLossAllowNegative && amount < 0D) return;
        if (amount == 0) return;

        UserClassData cData = this.classManager.getUserData(player);
        if (cData == null) return;

        // We don't need to increase amount of loss exp xD.
        if (amount > 0) {
            double expBonus = 0D; // TODO Item Stat
            amount = (int) ((double) amount * (1D + expBonus / 100D));
        }

        PlayerClassExpGainEvent e = new PlayerClassExpGainEvent(player, cData, amount, src, srcExp);
        plugin.getPluginManager().callEvent(e);
        if (e.isCancelled()) return;

        int lvlHas = cData.getLevel();
        amount = e.getExp();
        cData.addExp(amount);

        if (amount >= 0) {
            plugin.lang().Classes_Leveling_Exp_Get
                    .replace("%exp%", amount)
                    .replace("%src%", src)
                    .send(player);
        } else {
            plugin.lang().Classes_Leveling_Exp_Lost
                    .replace("%exp%", Math.abs(amount))
                    .replace("%src%", src)
                    .send(player);
        }

        int lvlNew = cData.getLevel();
        if (lvlHas != lvlNew) {
            PlayerClassLevelChangeEvent e2 = new PlayerClassLevelChangeEvent(player, cData, amount);
            plugin.getPluginManager().callEvent(e2);

            if (lvlNew > lvlHas) {
                plugin.lang().Classes_Leveling_Level_Up
                        .replace("%lvl%", String.valueOf(lvlNew))
                        .send(player);

                // Points Message
                RPGClass rpgClass = cData.getPlayerClass();
                int      aPoints  = rpgClass.getAspectPointsPerLevel() * (lvlNew - lvlHas);
                int      sPoints  = rpgClass.getSkillPointsPerLevel() * (lvlNew - lvlHas);
                plugin.lang().Classes_Leveling_Points_Aspect_Get
                        .replace("%amount%", String.valueOf(aPoints)).send(player);
                plugin.lang().Classes_Leveling_Points_Skill_Get
                        .replace("%amount%", String.valueOf(sPoints)).send(player);

                for (int lvlAct = lvlHas + 1; lvlAct < (lvlNew + 1); lvlAct++) {
                    rpgClass.executeLevelActions(player, lvlAct);
                }
            } else {
                plugin.lang().Classes_Leveling_Level_Down
                        .replace("%lvl%", String.valueOf(lvlNew))
                        .send(player);
            }

            this.classManager.updateClassData(player);
        }

        this.updateExpBossBar(player);
    }


    private double getEntityLevel(@NotNull Entity entity) {
        return Math.max(1, EngineCfg.HOOK_MOB_LEVEL_PLUGIN.getMobLevel(entity));
    }

    private double getExpPercentByLevel(@NotNull Player player, @NotNull Entity entity) {
        UserClassData cData = this.classManager.getUserData(player);
        if (cData == null) return 1D;

        double mobLvl  = this.getEntityLevel(entity);
        double userLvl = cData.getLevel();

        if (mobLvl < userLvl && this.expBalDecEnabled) {
            double max       = this.expBalDecMaxTimes * this.expBalDecPercent;
            double reduceExp = Math.min(max, ((userLvl - mobLvl) / this.expBalDecInterval) * this.expBalDecPercent);
            return Math.max(0, 1D - (reduceExp / 100D));
        }

        if (mobLvl > userLvl && this.expBalIncEnabled) {
            double max    = this.expBalIncMaxTimes * this.expBalIncPercent;
            double incExp = Math.min(max, ((mobLvl - userLvl) / this.expBalIncInterval) * this.expBalIncPercent);
            return 1D + (incExp / 100D);
        }

        return 1D;
    }

    // ------------------------------------------- //

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExpLevelWorldChange(PlayerTeleportEvent e) {
        if (this.lvlWorlds.isEmpty()) return;

        Player player = e.getPlayer();
        if (Hooks.isNPC(player)) return;

        Location to = e.getTo();
        if (to == null) return;

        World world = to.getWorld();
        if (world == null || world.equals(player.getWorld())) return;

        int minLvl = this.lvlWorlds.getOrDefault(world.getName(), 0);
        if (minLvl <= 0) return;

        UserClassData cData = this.classManager.getUserData(player);
        if (cData == null) {
            e.setCancelled(true);
            plugin.lang().Classes_Error_NoClass.send(player);
            return;
        }

        if (cData.getLevel() < minLvl) {
            e.setCancelled(true);

            plugin.lang().Classes_Error_Level_World
                    .replace("%level%", minLvl)
                    .send(player);
            return;
        }
    }

    @EventHandler
    public void onExpLevelUpDown(PlayerClassLevelChangeEvent e) {
        Player player = e.getPlayer();

        UserClassData cData = e.getClassData();
        if (cData.isTimeToChildClass()) {
            plugin.lang().Classes_Leveling_Child_Available.send(player);

            if (!this.classManager.selectPopChildTime || this.classManager.isRemindDisabled(player)) return;
            this.classManager.openSelectionGUI(player, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExpMobSpawn(CreatureSpawnEvent e) {
        String res = e.getSpawnReason().name();
        if (this.expReasonBlack.contains(res)) {
            LivingEntity entity = e.getEntity();
            entity.setMetadata(ENTITY_NO_EXP_SPAWN, new FixedMetadataValue(plugin, "yes"));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onExpPlayerDeath(PlayerDeathEvent e) {
        if (this.expLossDeathPercent <= 0D) return;

        Player player = e.getEntity();
        if (!this.canDropExp(player)) return;

        UserClassData cData = this.classManager.getUserData(player);
        if (cData == null) return;

        double expHas  = Math.abs(cData.getExp());
        double expLoss = expHas * (this.expLossDeathPercent / 100D);
        if (expLoss <= 0D) return;
        if (expLoss > expHas) expLoss = expHas;

        this.addExp(player, (int) -expLoss, player.getName(), ExpSource.DEATH);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExpMobDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();

        if (dead.hasMetadata(ENTITY_NO_EXP_SPAWN)) return;
        if (!this.canDropExp(dead)) return;

        Player killer = dead.getKiller();
        if (killer == null) return;

        if (this.expUseTables) {
            this.addExp(killer, dead);
        } else {
            double exp = this.balanceExp(killer, dead, e.getDroppedExp());
            this.addExp(killer, (int) exp, EntityStats.getEntityName(dead), ExpSource.MOB_KILL);
        }
    }
}
