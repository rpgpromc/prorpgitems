package studio.magemonkey.divinity.modules.list.drops;

import com.google.common.collect.Sets;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.manager.api.MobGroup;
import studio.magemonkey.codex.util.actions.ActionManipulator;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.DivinityAPI;
import studio.magemonkey.divinity.hooks.external.MyPetHK;
import studio.magemonkey.divinity.hooks.external.mythicmobs.AbstractMythicMobsHK;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.api.QModule;
import studio.magemonkey.divinity.modules.list.drops.commands.DropsDropCmd;
import studio.magemonkey.divinity.modules.list.drops.commands.DropsGiveCmd;
import studio.magemonkey.divinity.modules.list.drops.object.*;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;

import java.util.*;
import java.util.stream.Collectors;

public class DropManager extends QModule {

    private static final String                 META_DROP_MOB = "QRPG_NO_MOB_DROP";
    private              Map<String, Float>     dropModifier;
    private              Map<String, DropMob>   dropNpc;
    private              Map<String, DropTable> dropTables;
    private              AbstractMythicMobsHK   mmHook;
    private              MyPetHK                myPetHook;

    public DropManager(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.DROPS;
    }

    @Override
    @NotNull
    public String version() {
        return "2.1.0";
    }

    @Override
    public void setup() {
        this.moduleCommand.addSubCommand(new DropsDropCmd(this));
        this.moduleCommand.addSubCommand(new DropsGiveCmd(this));

        this.plugin.getConfigManager().extract(this.getPath() + "mobs");
        this.plugin.getConfigManager().extract(this.getPath() + "tables");

        this.dropModifier = new HashMap<>();
        this.dropNpc = new HashMap<>();
        this.dropTables = new HashMap<>();

        JYML cfg_main = this.cfg;
        for (String s : cfg_main.getSection("multipliers")) {
            double d = cfg_main.getDouble("multipliers." + s);
            this.dropModifier.put(s.toLowerCase(), (float) d);
        }

        for (JYML cfg : JYML.loadAll(this.getFullPath() + "/tables/", true)) {
            try {
                DropTable dropTable = new DropTable(plugin, cfg);
                this.dropTables.put(dropTable.getId(), dropTable);
            } catch (Exception ex) {
                error("Could not load Drop Table: " + cfg.getFile().getName());
                ex.printStackTrace();
            }
        }

        for (JYML cfg : JYML.loadAll(this.getFullPath() + "/mobs/", true)) {
            try {
                DropMob npc = new DropMob(plugin, cfg, this);
                this.dropNpc.put(npc.getId(), npc);
            } catch (Exception ex) {
                error("Could not load Mob Table: " + cfg.getFile().getName());
                ex.printStackTrace();
            }
        }

        this.mmHook = plugin.getHook(AbstractMythicMobsHK.class);
        this.myPetHook = plugin.getHook(MyPetHK.class);
    }

    @Override
    public void shutdown() {
        if (this.dropModifier != null) {
            this.dropModifier.clear();
            this.dropModifier = null;
        }
        if (this.dropNpc != null) {
            this.dropNpc.clear();
            this.dropNpc = null;
        }
        if (this.dropTables != null) {
            this.dropTables.clear();
            this.dropTables = null;
        }
    }

    public List<DropTable> getTables() {
        return dropTables.values().stream().collect(Collectors.toList());
    }

    @Nullable
    public DropTable getTableById(@NotNull String id) {
        return this.dropTables.get(id.toLowerCase());
    }

    private String getMobType(Entity entity) {
        if (this.mmHook != null && this.mmHook.isMythicMob(entity))
            return this.mmHook.getMythicNameByEntity(entity);
        else
            return entity.getType().name();
    }

    @NotNull
    private boolean isVanillaCancelled(@NotNull Entity entity) {
        boolean cancelled = false;
        if (!(entity instanceof LivingEntity)) return cancelled;

        String  mobType  = getMobType(entity);
        boolean isMythic = this.mmHook != null && this.mmHook.isMythicMob(entity);

        for (DropMob dropNpc : this.dropNpc.values()) {
            Set<String> mobList;
            if (isMythic) mobList = dropNpc.getMythic();
            else mobList = dropNpc.getEntities();

            boolean contains = mobList.contains(JStrings.MASK_ANY) || mobList.contains(mobType);

            if (!isMythic) {
                MobGroup group = MobGroup.getMobGroup(entity);
                if (mobList.contains(group.name())) {
                    contains = true;
                }
            }

            if (contains && !dropNpc.isVanillaDrops()) {
                cancelled = true;
                break;
            }
        }

        return cancelled;
    }

    @NotNull
    private Set<DropMob> getDropsForEntity(@NotNull Entity entity) {
        if (!(entity instanceof LivingEntity)) return Collections.emptySet();

        String  mobType  = getMobType(entity);
        boolean isMythic = this.mmHook != null && this.mmHook.isMythicMob(entity);

        Set<DropMob> tables = new HashSet<>();

        for (DropMob dropNpc : this.dropNpc.values()) {
            Set<String> mobList;
            if (isMythic) mobList = dropNpc.getMythic();
            else mobList = dropNpc.getEntities();

            if (mobList.contains(JStrings.MASK_ANY)) {
                tables.add(dropNpc);
                continue;
            }

            if (!isMythic) {
                MobGroup group = MobGroup.getMobGroup(entity);
                if (mobList.contains(group.name())) {
                    tables.add(dropNpc);
                    continue;
                }
            }

            if (mobList.contains(mobType)) {
                tables.add(dropNpc);
            }
        }
        List<String> tableNames = new ArrayList<>();
        tables.forEach(table -> table.getDropTables().forEach(t -> tableNames.add(t.getGroupName())));

        return tables;
    }

    private float getMultiplier(@NotNull Player player, @NotNull LivingEntity dead) {
        float mult      = 0.0f;
        float multGroup = 0.0f;
        float multStat  = 0.0f;

        String group = Hooks.getPermGroup(player);
        if (this.dropModifier.containsKey(group)) {
            multGroup += ((this.dropModifier.get(group) - 1f) * 100f);
        }

        multStat += EntityStats.get(player).getItemStat(TypedStat.Type.LOOT_RATE, false);
        mult = 1f + ((multGroup + multStat) / 100f);

        return mult;
    }

    @NotNull
    private List<ItemStack> methodRoll(@NotNull Player killer, @NotNull LivingEntity dead) {
        List<ItemStack> loot     = new ArrayList<>();
        Set<DropMob>    mobs     = this.getDropsForEntity(dead);
        float           modifier = this.getMultiplier(killer, dead);

        // TODO
        // Fast implementation for Drop Conditions.
        // Must be remaked a lot
        Map<String, Set<Entity>> mapTarget = new HashMap<>();
        mapTarget.put("player", Sets.newHashSet(killer));
        mapTarget.put("entity", Sets.newHashSet(dead));

        for (DropMob dropNpc : mobs) {
            Set<Drop> drop = dropNpc.dropCalculator(killer, dead, modifier);

            for (Drop dropItem : drop) {
                DropItem     dropConfig     = dropItem.getDropConfig();
                List<String> dropConditions = dropConfig.getConditions();
                if (!ActionManipulator.processConditions(plugin, killer, dropConditions, mapTarget)) continue;

                String itemId = dropConfig.getItemId();
                String tierId = dropConfig.getTierId();
                for (int i = 0; i < dropItem.getCount(); i++) {
                    int itemLvl = dropConfig.getLevel(killer, dead);

                    ItemStack dropStack =
                            DivinityAPI.getItemByModule(dropConfig.getModuleId(), itemId, itemLvl, -1, -1, tierId);
                    if (dropStack == null || dropStack.getType() == Material.AIR) continue;

                    dropConfig.executeActions(killer, mapTarget);
                    loot.add(dropStack);
                }
            }

            for (DropTable table : dropNpc.getDropTables()) {

                for (DropNonItem nonItemDrop : table.getNonItemDrops()) {

                    nonItemDrop.execute(killer);
                }
            }
        }
        return loot;
    }

    public List<ItemStack> rollTable(Player target, DropTable table, int itemLvl) {
        float           modifier = 1.0f;
        List<ItemStack> loot     = new ArrayList<>();

        LivingEntity dead = (LivingEntity) target.getWorld().spawnEntity(target.getLocation(), EntityType.BAT);

        Map<String, Set<Entity>> mapTarget = new HashMap<>();
        mapTarget.put("player", Sets.newHashSet(target));
        mapTarget.put("entity", Sets.newHashSet(dead));

        Set<Drop> drop = table.dropCalculator(target, dead, modifier);
        for (Drop dropItem : drop) {
            DropItem     dropConfig     = dropItem.getDropConfig();
            List<String> dropConditions = dropConfig.getConditions();
            if (!ActionManipulator.processConditions(plugin, target, dropConditions, mapTarget)) continue;

            String itemId = dropConfig.getItemId();
            String tierId = dropConfig.getTierId();

            for (int i = 0; i < dropItem.getCount(); i++) {
                ItemStack dropStack =
                        DivinityAPI.getItemByModule(dropConfig.getModuleId(), itemId, itemLvl, -1, -1, tierId);
                if (dropStack == null || dropStack.getType() == Material.AIR) continue;

                dropConfig.executeActions(target, mapTarget);
                loot.add(dropStack);
            }
        }

        for (DropNonItem nonItemDrops : table.getNonItemDrops()) {

            nonItemDrops.execute(target);
        }

        dead.remove();
        return loot;
    }

    public List<ItemStack> rollTable(Location loc, DropTable table, int itemLvl) {
        float           modifier = 1.0f;
        List<ItemStack> loot     = new ArrayList<>();

        LivingEntity dead = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.BAT);

//        Map<String, Set<Entity>> mapTarget = new HashMap<>();
//        mapTarget.put("player", Sets.newHashSet(target));
//        mapTarget.put("entity", Sets.newHashSet(dead));

        Set<Drop> drop = table.dropCalculator(null, dead, modifier);
        for (Drop dropItem : drop) {
            DropItem dropConfig = dropItem.getDropConfig();
//            List<String> dropConditions = dropConfig.getConditions();
//            if (!ActionManipulator.processConditions(plugin, target, dropConditions, mapTarget)) continue;

            String itemId = dropConfig.getItemId();
            String tierId = dropConfig.getTierId();

            for (int i = 0; i < dropItem.getCount(); i++) {
                ItemStack dropStack =
                        DivinityAPI.getItemByModule(dropConfig.getModuleId(), itemId, itemLvl, -1, -1, tierId);
                if (dropStack == null || dropStack.getType() == Material.AIR) continue;

//              dropConfig.executeActions(target, mapTarget);
                loot.add(dropStack);
            }
        }

        dead.remove();
        return loot;
    }

    // ---------------------------------------------------- //

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDropDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead.hasMetadata(META_DROP_MOB)) return;

        Player killer = dead.getKiller();
        if (killer == null) {
            // Support for MyPet kills.
            if (this.myPetHook != null) {
                EntityDamageEvent e2 = dead.getLastDamageCause();
                if (e2 instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent ede     = (EntityDamageByEntityEvent) e2;
                    Entity                    damager = ede.getDamager();
                    if (this.myPetHook.isPet(damager)) {
                        killer = this.myPetHook.getPetOwner(damager);
                    }
                }
            }
            if (killer == null) return;
        }

        if (isVanillaCancelled(dead))
            e.getDrops().clear();
        e.getDrops().addAll(this.methodRoll(killer, dead));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDropSpawn(CreatureSpawnEvent e) {
        // Run after event call, so that MythicMobs are properly detected
        new BukkitRunnable() {
            @Override
            public void run() {
                LivingEntity entity = e.getEntity();
                Set<DropMob> mobs   = getDropsForEntity(entity);
                if (mobs.isEmpty()) return;

                String reason = e.getSpawnReason().name();
                for (DropMob dropNpc : mobs) {
                    if (dropNpc.getReasons().contains(reason)) {
                        entity.setMetadata(META_DROP_MOB, new FixedMetadataValue(plugin, "yes"));
                        break;
                    }
                }
            }
        }.runTask(plugin);
    }
}
