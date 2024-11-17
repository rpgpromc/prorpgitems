package studio.magemonkey.divinity.modules.list.loot;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.manager.IListener;
import studio.magemonkey.codex.manager.api.task.ITask;
import studio.magemonkey.codex.modules.IModuleExecutor;
import studio.magemonkey.codex.util.*;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.party.PartyManager;
import studio.magemonkey.divinity.modules.list.party.PartyManager.Party;
import studio.magemonkey.divinity.modules.list.party.PartyManager.PartyDropMode;
import studio.magemonkey.divinity.modules.list.party.PartyManager.PartyMember;

import java.util.*;
import java.util.Map.Entry;

public class LootHolder extends IListener<Divinity> implements InventoryHolder {

    protected       LootManager manager;
    protected final Location    boxLoc;

    protected final String title;
    protected       int    size;

    protected long   despawnTime;
    protected UUID   ownerId   = null;
    protected UUID   invId;
    protected String ownerName = "";

    protected Inventory   inv;
    protected Set<Player> viewers;

    private RollTask          rollTask = null;
    private HologramExpansion holoEx   = null;

    private static final NamespacedKey KEY_META_OWNER =
            new NamespacedKey(Divinity.getInstance(), "QRPG_LOOT_ITEM_OWNER");

    public LootHolder(
            LootManager manager,
            @NotNull Location boxLoc,
            @Nullable LivingEntity killer,
            @NotNull LivingEntity dead,
            @NotNull List<ItemStack> loot) {

        super(manager.plugin);
        this.manager = manager;
        this.boxLoc = boxLoc;

        this.title = dead.getCustomName() != null ? dead.getCustomName() : plugin.lang().getEnum(dead.getType());
        int lootSize = loot.size();

        this.size = 54;
        if (lootSize < 55) this.size = 54;
        if (lootSize < 46) this.size = 45;
        if (lootSize < 37) this.size = 36;
        if (lootSize < 28) this.size = 27;
        if (lootSize < 19) this.size = 18;
        if (lootSize < 10) this.size = 9;

        this.despawnTime = System.currentTimeMillis() + manager.getLootTime() * 1000L;

        this.ownerName = plugin.lang().Loot_Box_Owner_None.getMsg();

        if (manager.isDropProtect() && killer != null) {
            this.ownerId = killer.getUniqueId();
            this.ownerName = killer.getName();

            PartyManager partyManager = plugin.getModuleCache().getPartyManager();
            if (partyManager != null && killer instanceof Player) {
                Player      kill   = (Player) killer;
                PartyMember member = partyManager.getPartyMember(kill);
                if (member != null) {
                    Party  party = member.getParty();
                    UUID   uuid  = null;
                    String name  = "";

                    PartyMember   leader   = party.getLeader();
                    PartyDropMode dropMode = party.getDropMode();

                    if (dropMode == PartyDropMode.LEADER && leader != null) {
                        uuid = leader.getUUID();
                        name = leader.getName();
                    } else if (dropMode == PartyDropMode.AUTO) {
                        List<PartyMember> memList = new ArrayList<>(party.getMembers());
                        PartyMember       rMem    = Rnd.get(memList);
                        if (rMem == null) rMem = memList.get(0);
                        uuid = rMem.getUUID();
                        name = rMem.getName();
                    } else {
                        uuid = member.getUUID();
                        name = member.getName();
                    }

                    this.ownerId = uuid;
                    this.ownerName = name;
                }
            }
        }

        this.invId = UUID.randomUUID();
        this.viewers = new HashSet<>();

        this.getInventory();
        for (int slot = 0; slot < lootSize; slot++) {
            if (slot >= this.size) break;

            ItemStack item = loot.get(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            this.inv.setItem(slot, item);
        }

        if (this.manager.boxHoloEnabled) {
            this.holoEx = new HologramExpansion();
        }

        this.registerListeners();
    }

    public final void shutdown() {
        if (this.rollTask != null) {
            this.rollTask.cancel();
            this.rollTask = null;
        }

        for (Player p : this.viewers) {
            p.closeInventory();
        }
        this.removeHolo();

        this.unregisterListeners();
        this.inv = null;
        //this.invId = null;
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        if (this.inv == null) {
            this.inv = plugin.getServer().createInventory(this, this.size, this.title);
        }
        return this.inv;
    }

    // ------------------------------------------------------- //

    public void open(@NotNull Player player) {
        if (!this.isOwner(player)) {
            plugin.lang().Loot_Box_Error_NotOwner.send(player);
            return;
        }
        if (!this.viewers.isEmpty()) {
            plugin.lang().Loot_Box_Error_Locked.send(player);
            return;
        }

        player.openInventory(this.getInventory());
        this.viewers.add(player);
    }

    public boolean isOwner(@NotNull Player player) {
        if (this.ownerId == null) return true;

        PartyManager partyManager = plugin.getModuleCache().getPartyManager();
        if (partyManager != null) {
            Party party = partyManager.getPlayerParty(player);
            if (party != null
                    && party.getDropMode() != PartyDropMode.LEADER
                    && party.getDropMode() != PartyDropMode.AUTO) {

                if (party.getMember(this.ownerId) != null) {
                    return true;
                }
            }
        }

        return player.getUniqueId().equals(this.ownerId);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= this.despawnTime;
    }

    public void updateHolo() {
        if (this.holoEx == null) return;
        this.holoEx.update();
    }

    public void removeHolo() {
        if (this.holoEx == null) return;
        this.holoEx.remove();
    }

    private boolean hasItemOwner(@NotNull ItemStack item) {
        return this.getItemOwner(item) != null;
    }

    @Nullable
    private String getItemOwner(@NotNull ItemStack item) {
        return DataUT.getStringData(item, KEY_META_OWNER);
    }

    private boolean isItemOwner(@NotNull Player player, @NotNull ItemStack item) {
        String uuid  = player.getUniqueId().toString();
        String owner = this.getItemOwner(item);
        return owner != null && owner.equalsIgnoreCase(uuid);
    }

    // ------------------------------------------------------- //

    @EventHandler(ignoreCancelled = true)
    public void onLootDrag(InventoryDragEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (holder == null || !(holder.getClass().isInstance(this))) return;

        LootHolder lootHolder = (LootHolder) holder;
        if (!lootHolder.invId.equals(this.invId)) return;

        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLootClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (holder == null || !(holder.getClass().isInstance(this))) return;

        LootHolder lootHolder = (LootHolder) holder;
        if (!lootHolder.invId.equals(this.invId)) return;

        int slot = e.getRawSlot();


        // Prevent to put items in loot box
        ItemStack       cursor = e.getCursor();
        InventoryAction ea     = e.getAction();

        if ((ea == InventoryAction.HOTBAR_MOVE_AND_READD
                || ea == InventoryAction.SWAP_WITH_CURSOR
                || ea == InventoryAction.HOTBAR_SWAP)
                ||
                (cursor != null && cursor.getType() != Material.AIR && slot < this.size)) {
            e.setCancelled(true);
        }

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        // Prevent to take rolling items, even if drop mode was changed
        if (this.rollTask != null && this.rollTask.slot == slot) {
            e.setCancelled(true);
            return;
        }

        Player clicker = (Player) e.getWhoClicked();

        // If item already has owner from the roll
        // then let it pick by owner or deny if not owner.
        if (this.hasItemOwner(item)) {
            if (!this.isItemOwner(clicker, item)) {
                e.setCancelled(true);
                plugin.lang().Loot_Party_Roll_NotOwner.send(clicker);
                return;
            }
            // Clear Owner Tag
            DataUT.removeData(item, KEY_META_OWNER);
            e.setCurrentItem(item);
            return;
        }

        // Manage Party Drop Mode
        PartyManager partyManager = plugin.getModuleCache().getPartyManager();
        if (partyManager == null) return;

        PartyMember member = partyManager.getPartyMember(clicker);
        if (member == null) return;

        Party party = member.getParty();
        if (party.getDropMode() == PartyDropMode.ROLL && party.getOnline() > 1 && slot < size) {
            // Cancel event and start roll task
            e.setCancelled(true);

            if (this.rollTask != null || manager.getPartyRollTask(clicker) != null) {
                plugin.lang().Loot_Party_Roll_AlreadyStarted.send(clicker);
                return;
            }

            this.rollTask = new RollTask(clicker, slot);
            this.rollTask.start();

            clicker.closeInventory();

            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLootClose(InventoryCloseEvent e) {
        InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (inventoryHolder == null || !(inventoryHolder.getClass().isInstance(this))) return;

        LootHolder lootHolder = (LootHolder) inventoryHolder;
        if (!lootHolder.invId.equals(this.invId)) return;

        this.viewers.remove(e.getPlayer());

        for (ItemStack item : this.inv.getContents()) {
            if (!ItemUT.isAir(item)) {
                return;
            }
        }

        this.manager.despawnLoot(this.boxLoc);
    }

    @Nullable
    public RollTask getRollTask() {
        return this.rollTask;
    }

    public class RollTask extends ITask<Divinity> {

        Party     party;
        Player    roller;
        int       slot;
        ItemStack item;

        Map<UUID, Integer> rollMap;
        int                timeEnd;

        public RollTask(@NotNull Player clicker, int slot) {
            super(LootHolder.this.plugin, 1, false);

            this.roller = clicker;
            this.slot = slot;
            this.item = inv.getItem(this.slot);
            this.rollMap = new HashMap<>();
            this.timeEnd = manager.partyDropRollTime;

            PartyManager partyManager = plugin.getModuleCache().getPartyManager();
            PartyMember  member       = partyManager != null ? partyManager.getPartyMember(clicker) : null;
            if (member == null) {
                throw new IllegalArgumentException("Null Party for Roll task!");
            }

            this.party = member.getParty();
        }

        public void addRoll(@NotNull Player player, int roll) {
            UUID uuid = player.getUniqueId();

            // Check if already dropped
            if (this.rollMap.containsKey(uuid)) {
                int value = this.rollMap.get(uuid);

                plugin.lang().Loot_Party_Roll_AlreadyRoll
                        .replace("%value%", String.valueOf(value))
                        .send(player);
                return;
            }

            this.rollMap.put(uuid, roll);

            this.party.sendMessage(plugin.lang().Loot_Party_Roll_MemberRoll
                    .replace("%player%", player.getName())
                    .replace("%value%", String.valueOf(roll)));
        }

        public void rollNotify() {
            PartyManager partyManager = plugin.getModuleCache().getPartyManager();
            if (partyManager == null) return;

            IModuleExecutor<?> exec = partyManager.getExecutor();
            if (exec == null) return;

            String label = exec.labels()[0];

            for (PartyMember mem : party.getMembers()) {
                Player p = mem.getPlayer();
                if (p == null) continue;

                ClickText ct = new ClickText(plugin.lang().Loot_Party_Roll_Notify_List.getMsg());

                ct.createPlaceholder("%roll%", plugin.lang().Loot_Party_Roll_Notify_Roll_Name.getMsg())
                        .hint(plugin.lang().Loot_Party_Roll_Notify_Roll_Hint.getMsg())
                        .execCmd("/" + label + " roll");

                ct.createPlaceholder("%item%", ItemUT.getItemName(item))
                        .showItem(item);

                ct.send(p);
            }
        }

        @Override
        public void action() {
            if (this.party == null || this.party.getOnline() <= 0) {
                this.cancel();
                return;
            }

            if (this.timeEnd <= 0 || this.rollMap.size() >= this.party.getOnline()) {
                UUID winner = null;
                int  best   = 0;
                for (Entry<UUID, Integer> e : this.rollMap.entrySet()) {
                    if (e.getValue() > best) {
                        winner = e.getKey();
                        best = e.getValue();
                    }
                }

                PartyMember winnerMember = null;
                if (winner == null || (winnerMember = this.party.getMember(winner)) == null) {
                    this.party.sendMessage(plugin.lang().Loot_Party_Roll_NoRoll);
                    this.cancel();
                    return;
                }

                DataUT.setData(item, KEY_META_OWNER, winner.toString());
                inv.setItem(slot, item);

                this.party.sendMessage(plugin.lang().Loot_Party_Roll_Winner
                        .replace("%item%", ItemUT.getItemName(item))
                        .replace("%player%", winnerMember.getName()));

                this.cancel();
                return;
            }

            // Notify the item and roll buttons
            double div = manager.partyDropRollTime * 1D / this.timeEnd * 1D;
            if (div == 1 || div == 2) {
                this.rollNotify();
            }

            // Notify the time
            if (this.timeEnd % 5 == 0 || this.timeEnd <= 5) {
                this.party.sendMessage(plugin.lang().Loot_Party_Roll_RollIn
                        .replace("%time%", String.valueOf(this.timeEnd)));
            }

            this.timeEnd--;
        }

        public void cancel() {
            this.stop();
            rollTask = null;
        }

    }

    class HologramExpansion {

        private Hologram holo;

        public HologramExpansion() {
            this.holo =
                    HolographicDisplaysAPI.get(plugin).createHologram(LocUT.getCenter(boxLoc.clone()).add(0, 1.25, 0));
            this.update();
        }

        public void update() {
            if (this.holo == null) return;

            this.holo.getLines().clear();
            for (String s : manager.getHoloText()) {
                this.holo.getLines().appendText(s
                        .replace("%entity%", title)
                        .replace("%owner%", ownerName)
                        .replace("%time%", TimeUT.formatTimeLeft(LootHolder.this.despawnTime)));
            }
        }

        public void remove() {
            if (this.holo == null) return;

            this.holo.delete();
            this.holo = null;
        }
    }
}
