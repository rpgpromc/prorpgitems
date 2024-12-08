package studio.magemonkey.divinity.modules.list.loot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.compat.VersionManager;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.hooks.external.WorldGuardHK;
import studio.magemonkey.codex.manager.api.task.ITask;
import studio.magemonkey.codex.util.EffectUT;
import studio.magemonkey.codex.util.LocUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.hooks.EHook;
import studio.magemonkey.divinity.hooks.external.mythicmobs.AbstractMythicMobsHK;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.api.QModule;
import studio.magemonkey.divinity.modules.list.party.PartyManager;
import studio.magemonkey.divinity.modules.list.party.PartyManager.Party;
import studio.magemonkey.divinity.modules.list.party.PartyManager.PartyMember;
import studio.magemonkey.divinity.modules.list.party.event.PlayerLeavePartyEvent;

import java.util.*;

public class LootManager extends QModule {

    private boolean     generalProtectDrop;
    private boolean     airOnly;
    private int         generalTimeToLoot;
    private Set<String> generalEntityBlack;
    private Set<String> generalMythicBlack;
    private Set<String> generalWorldBlack;
    private Set<String> generalRegionBlack;
    private Set<String> generalSpawnReasonBlack;
    private Set<String> generalDeathCauseBlack;

    private String boxSkullHash;
    private String boxParticleStatic;
    private String boxParticleDespawn;
    boolean boxHoloEnabled;
    private List<String> boxHoloText;

    int partyDropRollTime;

    private Map<Location, LootHolder> loots;

    private LootTask taskLoot;

    private AbstractMythicMobsHK mmHook;

    private static final String META_SPAWN_REASON = "QRPG_META_SPAWN_REASON";

    public LootManager(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.LOOT;
    }

    @Override
    @NotNull
    public String version() {
        return "1.04";
    }

    @Override
    public void setup() {
        this.cfg.addMissing("general.death-cause-blacklist", Arrays.asList("LAVA"));

        String path = "general.";
        this.generalProtectDrop = cfg.getBoolean(path + "protect-drop");
        this.airOnly = cfg.getBoolean(path + "air-only");
        this.generalTimeToLoot = cfg.getInt(path + "time-to-loot");
        this.generalEntityBlack = cfg.getStringSet(path + "entity-blacklist");
        this.generalMythicBlack = cfg.getStringSet(path + "mythic-blacklist");
        this.generalWorldBlack = cfg.getStringSet(path + "world-blacklist");
        this.generalRegionBlack = cfg.getStringSet(path + "region-blacklist");
        this.generalSpawnReasonBlack = cfg.getStringSet(path + "spawn-reason-blacklist");
        this.generalDeathCauseBlack = cfg.getStringSet(path + "death-cause-blacklist");

        path = "lootbox.";
        this.boxSkullHash = cfg.getString(path + "skull-hash");
        this.boxParticleStatic = cfg.getString(path + "particles.static");
        this.boxParticleDespawn = cfg.getString(path + "particles.despawn");
        this.boxHoloEnabled = cfg.getBoolean(path + "holograms.enabled") && Hooks.hasPlugin(EHook.HOLOGRAPHIC_DISPLAYS);
        this.boxHoloText = StringUT.color(cfg.getStringList(path + "holograms.text"));

        path = "party.drop-modes.";
        this.partyDropRollTime = cfg.getInt(path + "roll-time");

        this.loots = new HashMap<>();


        this.taskLoot = new LootTask(plugin);
        this.taskLoot.start();

        this.mmHook = plugin.getHook(AbstractMythicMobsHK.class);

        this.cfg.saveChanges();
    }

    @Override
    public void shutdown() {
        if (this.taskLoot != null) {
            this.taskLoot.stop();
            this.taskLoot = null;
        }

        for (Location l : new HashMap<>(this.loots).keySet()) {
            this.despawnLoot(l);
        }
        this.loots.clear();
        this.loots = null;
    }

    public boolean isDropProtect() {
        return this.generalProtectDrop;
    }

    public int getLootTime() {
        return this.generalTimeToLoot;
    }

    @NotNull
    public List<String> getHoloText() {
        return this.boxHoloText;
    }

    public boolean spawnLoot(@Nullable LivingEntity killer, @NotNull LivingEntity dead, @NotNull List<ItemStack> loot) {
        if (!this.isApplicable(dead)) return false;

        Location boxLoc = this.findLocation(dead.getLocation());
        if (boxLoc == null) return false;

        Block block = boxLoc.getBlock();
        block.setType(Material.PLAYER_HEAD);

        List<BlockFace> faces = new ArrayList<>(Arrays.asList(BlockFace.values()));
        faces.remove(BlockFace.DOWN);
        faces.remove(BlockFace.UP);
        faces.remove(BlockFace.SELF);

        BlockData data = block.getBlockData();
        if (data instanceof Rotatable) {
            Rotatable directional = (Rotatable) data;
            BlockFace face        = Rnd.get(faces);
            directional.setRotation(face != null ? face : BlockFace.EAST);
            block.setBlockData(directional);
        }
        VersionManager.getNms().changeSkull(block, this.boxSkullHash);

        LootHolder lootHolder = new LootHolder(this, boxLoc, killer, dead, loot);
        this.loots.putIfAbsent(boxLoc, lootHolder);
        return true;
    }

    @Nullable
    private Location findLocation(@NotNull Location from) {
        World world = from.getWorld();
        if (world == null) return null;

        Location loc   = LocUT.getFirstGroundBlock(from.clone(), airOnly);
        Block    block = loc.getBlock();

        while ((!block.isEmpty() && ((airOnly && !block.getType().isAir()) || (!airOnly && block.getType().isSolid())))
                || this.isLootBox(block.getLocation())) {
            loc = Rnd.nextBoolean() ? loc.add(1, 0, 0) : loc.add(0, 0, 1);

            block = loc.getBlock();
            if (!block.isEmpty() && block.getType().isSolid()) {
                block = world.getHighestBlockAt(loc);
            }
        }

        return block.getLocation();
    }

    public void despawnLoot(@NotNull Location loc) {
        LootHolder holder = this.loots.remove(loc);
        if (holder == null) return;

        holder.shutdown();
        loc.getBlock().setType(Material.AIR);
        EffectUT.playEffect(LocUT.getCenter(loc, false), this.boxParticleDespawn, 0.1f, 0.1f, 0.1f, 0.1f, 30);
    }

    public boolean isApplicable(@NotNull LivingEntity entity) {
        if (entity.hasMetadata(META_SPAWN_REASON)) {
            return false;
        }

        if (!this.generalWorldBlack.isEmpty()) {
            String      worldName = entity.getWorld().getName();
            Set<String> black     = this.generalWorldBlack;
            if (black.contains(JStrings.MASK_ANY) || black.contains(worldName)) {
                return false;
            }
        }

        if (!this.generalDeathCauseBlack.isEmpty()) {
            EntityDamageEvent e2 = entity.getLastDamageCause();
            if (e2 != null) {
                String      cause = e2.getCause().name();
                Set<String> black = this.generalDeathCauseBlack;
                if (black.contains(JStrings.MASK_ANY) || black.contains(cause)) {
                    return false;
                }
            }
        }

        boolean mythic = false;
        if (!this.generalMythicBlack.isEmpty() && this.mmHook != null) {
            if (this.mmHook.isMythicMob(entity)) {
                String      mobId = this.mmHook.getMythicNameByEntity(entity);
                Set<String> black = this.generalMythicBlack;
                if (black.contains(JStrings.MASK_ANY) || black.contains(mobId)) {
                    return false;
                }
                mythic = true;
            }
        }
        if (!mythic && !this.generalEntityBlack.isEmpty()) {
            Set<String> black = this.generalEntityBlack;
            if (black.contains(JStrings.MASK_ANY) || black.contains(entity.getType().name())) {
                return false;
            }
        }

        WorldGuardHK wg = plugin.getWorldGuard();
        if (!this.generalRegionBlack.isEmpty() && wg != null) {
            String      regionId = wg.getRegion(entity);
            Set<String> black    = this.generalRegionBlack;
            if (!regionId.isEmpty() && black.contains(JStrings.MASK_ANY) || black.contains(regionId)) {
                return false;
            }
        }


        return true;
    }

    @Nullable
    public LootHolder.RollTask getPartyRollTask(@NotNull Player player) {
        PartyManager manager = plugin.getModuleCache().getPartyManager();
        if (manager == null) return null;

        PartyMember member = manager.getPartyMember(player);
        if (member == null) return null;

        Party party = member.getParty();

        for (LootHolder loot : loots.values()) {
            LootHolder.RollTask rollTask = loot.getRollTask();
            if (rollTask == null) continue;

            PartyMember roller = manager.getPartyMember(rollTask.roller);
            if (roller == null) continue;

            if (party.isMember(roller)) {
                return rollTask;
            }
        }

        return null;
    }

    public boolean isLootBox(@NotNull Location loc) {
        return this.loots.containsKey(loc)
                && loc.getBlock().getType() == Material.PLAYER_HEAD;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLootBreak(BlockBreakEvent e) {
        Block    block = e.getBlock();
        Location loc   = block.getLocation();
        if (this.isLootBox(loc)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLootExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> this.isLootBox(b.getLocation()));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLootExplode2(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> this.isLootBox(b.getLocation()));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLootLiquid(BlockFromToEvent e) {
        Block block = e.getToBlock();
        if (this.isLootBox(block.getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLootExtend(BlockPistonExtendEvent e) {
        if (e.getBlocks().stream().anyMatch(block -> this.isLootBox(block.getLocation()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLootRetract(BlockPistonRetractEvent e) {
        if (e.getBlocks().stream().anyMatch(block -> this.isLootBox(block.getLocation()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLootCreatureSpawn(CreatureSpawnEvent e) {
        String      reason = e.getSpawnReason().name();
        Set<String> black  = this.generalSpawnReasonBlack;
        if (black.contains(JStrings.MASK_ANY) || black.contains(reason)) {
            e.getEntity().setMetadata(META_SPAWN_REASON, new FixedMetadataValue(plugin, "LOOT_NO"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLootDeath(EntityDeathEvent e) {
        List<ItemStack> loot = e.getDrops();
        if (loot.isEmpty()) return;

        LivingEntity dead   = e.getEntity();
        Player       killer = dead.getKiller();
        if (this.spawnLoot(killer, dead, new ArrayList<>(loot))) {
            loot.clear();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onLootClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.useInteractedBlock() == Result.DENY) return;

        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = e.getPlayer();
        Block  block  = e.getClickedBlock();
        if (block == null) return;

        Location loot = block.getLocation();
        if (this.loots.containsKey(loot)) {
            e.setUseItemInHand(Result.DENY);
            this.loots.get(loot).open(player);
        }
    }

    /**
     * Transfer the owner of party loot box to another party member,
     * when the 'killer/owner' lefts the party. Avoid scum for party members.
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPartyLeave(PlayerLeavePartyEvent e) {
        Player player = e.getPlayer();
        if (player == null) return;

        PartyMember mem   = e.getPartyMember();
        Party       party = mem.getParty();

        if (party.getOnline() == 1 || this.loots.isEmpty()) return;

        PartyMember transfer = null;
        for (PartyMember pm : party.getMembers()) {
            if (pm.equals(mem)) continue;
            transfer = pm;
            break;
        }
        if (transfer == null) return;

        for (LootHolder loot : this.loots.values()) {
            if (loot.ownerId != null && loot.ownerId.equals(player.getUniqueId())) {
                loot.ownerId = transfer.getUUID();
                loot.ownerName = transfer.getName();
            }
        }
    }

    class LootTask extends ITask<Divinity> {

        public LootTask(@NotNull Divinity plugin) {
            super(plugin, 1, false);
        }

        @Override
        public void action() {
            for (Map.Entry<Location, LootHolder> e : new HashMap<>(loots).entrySet()) {
                LootHolder loot = e.getValue();
                Location   loc  = e.getKey();

                if (!isLootBox(loc) || loot.isExpired()) {
                    despawnLoot(loc);
                    continue;
                }

                loot.updateHolo();
                EffectUT.playEffect(LocUT.getCenter(loc, false), boxParticleStatic, 0.1f, 0.1f, 0.1f, 0.1f, 30);
            }
        }
    }
}
