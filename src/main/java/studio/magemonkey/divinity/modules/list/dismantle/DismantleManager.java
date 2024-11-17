package studio.magemonkey.divinity.modules.list.dismantle;

import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.ILangMsg;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.external.VaultHK;
import studio.magemonkey.codex.hooks.external.citizens.CitizensHK;
import studio.magemonkey.codex.modules.IModule;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.codex.util.PlayerUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.actions.ActionManipulator;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.LimitedItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.list.dismantle.DismantleManager.DismantleItem;
import studio.magemonkey.divinity.modules.list.dismantle.command.DismantleOpenCmd;
import studio.magemonkey.divinity.modules.list.dismantle.event.PlayerDismantleItemEvent;
import studio.magemonkey.divinity.modules.list.dismantle.event.PlayerPreDismantleItemEvent;
import studio.magemonkey.divinity.stats.items.ItemStats;

import java.util.*;

public class DismantleManager extends QModuleDrop<DismantleItem> {

    private Map<String, Set<DismantleTable>> moduleTables;
    private ActionManipulator                actionsComplete;
    ActionManipulator actionsError;

    private DismantleGUI gui;

    public DismantleManager(@NotNull Divinity plugin) {
        super(plugin, DismantleItem.class);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.DISMANTLE;
    }

    @Override
    @NotNull
    public String version() {
        return "1.8.0";
    }

    @Override
    public void setup() {
        this.moduleTables = new HashMap<>();
        this.plugin.getConfigManager().extract(this.getPath() + "source");

        for (JYML cfg : JYML.loadAll(this.getFullPath() + "source", true)) {
            try {
                DismantleTable      table  = new DismantleTable(cfg);
                Set<DismantleTable> tables = this.moduleTables.get(table.getModule());
                if (tables == null) tables = new HashSet<>();

                tables.add(table);
                this.moduleTables.put(table.getModule(), tables);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
        }
        this.actionsComplete = new ActionManipulator(plugin, cfg, "general.actions-complete");
        this.actionsError = new ActionManipulator(plugin, cfg, "general.actions-error");

        this.gui = new DismantleGUI(this);
        this.moduleCommand.addSubCommand(new DismantleOpenCmd(this));

        CitizensHK citizens = plugin.getCitizens();
        if (citizens != null) {
            TraitInfo trait = TraitInfo.create(DismantleTrait.class).withName("dismantle");
            citizens.registerTrait(plugin, trait);
        }
    }

    @Override
    public void shutdown() {
        if (this.gui != null) {
            this.gui.shutdown();
            this.gui = null;
        }
        if (this.moduleTables != null) {
            this.moduleTables.clear();
            this.moduleTables = null;
        }
        this.actionsComplete = null;
        this.actionsError = null;
    }

    // -------------------------------------------------------------------- //
    // METHODS

    public void openDismantleGUI(@NotNull Player player, boolean isForce) {
        if (!isForce && !player.hasPermission(Perms.DISMANTLE_GUI)) {
            plugin.lang().Error_NoPerm.send(player);
            return;
        }
        this.gui.open(player, 1);
    }

    @Nullable
    public OutputContainer getResult(@NotNull ItemStack item) {
        QModuleDrop<?> mod = ItemStats.getModule(item);
        if (mod == null) return null;

        String itemId = ItemStats.getId(item);
        if (itemId == null) return null;

        int itemLvl = ItemStats.getLevel(item);

        for (DismantleTable table : this.moduleTables.getOrDefault(mod.getId(), Collections.emptySet())) {
            OutputContainer result = table.getResult(itemId, itemLvl);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public boolean isDismantleable(@NotNull ItemStack item) {
        return this.getResult(item) != null;
    }

    public boolean dismantle(@NotNull Player player, @NotNull ItemStack item) {
        OutputContainer result = this.getResult(item);
        if (result == null) return false;

        for (OutputItem src : result.getItems()) {
            src.give(player);
        }
        return true;
    }

    // -------------------------------------------------------------------- //
    // EVENTS

    @Override
    protected boolean onDragDrop(
            @NotNull Player player,
            @NotNull ItemStack src,
            @NotNull ItemStack target,
            @NotNull DismantleItem rItem,
            @NotNull InventoryClickEvent e) {

        OutputContainer out = this.getResult(target);
        if (out == null) return false;

        Map<ItemStack, OutputContainer> result = new HashMap<>();
        result.put(target, out);

        PlayerPreDismantleItemEvent event = new PlayerPreDismantleItemEvent(player, 0, result);
        plugin.getPluginManager().callEvent(event);

        double cost = event.getCost();
        if (!this.payForDismantle(player, cost)) {
            event.setCancelled(true);
        }
        if (event.isCancelled()) {
            this.actionsError.process(player);
            return false;
        }

        this.takeChargeClickEvent(player, src, e);

        ItemStack targetLeft = null;
        if (target.getAmount() > 1) {
            targetLeft = new ItemStack(target);
            targetLeft.setAmount(target.getAmount() - 1);
            target.setAmount(1);
        }

        e.setCurrentItem(null);

        if (targetLeft != null) {
            ItemUT.addItem(player, targetLeft);
        }

        for (OutputItem oItem : out.getItems()) {
            oItem.give(player);
        }

        PlayerDismantleItemEvent event2 = new PlayerDismantleItemEvent(player, cost, result);
        plugin.getPluginManager().callEvent(event2);

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDismantleMain(PlayerDismantleItemEvent e) {
        Player                          p      = e.getPlayer();
        Map<ItemStack, OutputContainer> result = e.getResult();
        double                          cost   = e.getCost();
        boolean                         isFree = cost <= 0;
        ILangMsg                        msg    = null;

        if (result.size() == 1) {
            for (ItemStack item : result.keySet()) {
                if (isFree) {
                    msg = plugin.lang().Dismantle_Dismantle_Single_Free
                            .replace("%item%", ItemUT.getItemName(item));
                } else {
                    msg = plugin.lang().Dismantle_Dismantle_Single_Paid
                            .replace("%item%", ItemUT.getItemName(item));
                }
            }
        } else {
            if (isFree) {
                msg = plugin.lang().Dismantle_Dismantle_Many_Free;
            } else {
                msg = plugin.lang().Dismantle_Dismantle_Many_Paid;
            }
        }
        if (msg != null) {
            msg.replace("%cost%", NumberUT.format(cost)).send(p);
        }
        this.actionsComplete.process(p);
    }

    final boolean payForDismantle(@NotNull Player player, double cost) {
        if (cost > 0) {
            VaultHK vault = plugin.getVault();
            if (vault != null && vault.getEconomy() != null) {
                double balance = vault.getBalance(player);
                if (balance < cost) {
                    plugin.lang().Dismantle_Dismantle_Error_TooExpensive
                            .replace("%cost%", NumberUT.format(cost))
                            .replace("%balance%", NumberUT.format(balance))
                            .send(player);

                    return false;
                }
                vault.take(player, cost);
            }
        }
        return true;
    }

    public class DismantleTable {

        private String                                         module;
        private Map<String, TreeMap<Integer, OutputContainer>> items;

        public DismantleTable(@NotNull JYML cfg) {
            this.module = cfg.getString("input-module");
            IModule<?> mod = plugin.getModuleManager().getModule(module);
            if (mod == null || !mod.isLoaded() || !(mod instanceof QModuleDrop<?>)) {
                throw new IllegalArgumentException("Module " + module + " is invalid or not loaded.");
            }

            this.items = new HashMap<>();
            for (String mItemId : cfg.getSection("output-by-item-id-level")) {
                TreeMap<Integer, OutputContainer> mapLvlContainer = new TreeMap<>();

                for (String sItemLvl : cfg.getSection("output-by-item-id-level." + mItemId)) {
                    int mItemLvl = StringUT.getInteger(sItemLvl, -1);
                    if (mItemLvl < 1) continue;
                    String           path   = "output-by-item-id-level." + mItemId + "." + sItemLvl + ".";
                    double           oCost  = cfg.getDouble(path + "cost");
                    List<OutputItem> oItems = new ArrayList<>();

                    for (String oItemId : cfg.getSection(path + "output")) {
                        String path2  = path + "output." + oItemId + ".";
                        double chance = cfg.getDouble(path2 + "chance");
                        if (chance <= 0) continue;

                        ItemStack    preview = cfg.getItem(path2 + "preview");
                        ItemStack    item    = cfg.getItem(path2 + "item");
                        List<String> cmds    = cfg.getStringList(path2 + "commands");

                        OutputItem si = new OutputItem(oItemId, chance, preview, item, cmds);
                        oItems.add(si);
                    }
                    if (!oItems.isEmpty()) {
                        OutputContainer container = new OutputContainer(oCost, oItems);
                        mapLvlContainer.put(mItemLvl, container);
                    }
                }
                if (!mapLvlContainer.isEmpty()) {
                    this.items.put(mItemId.toLowerCase(), mapLvlContainer);
                }
            }
        }

        @NotNull
        public String getModule() {
            return this.module;
        }

        @Nullable
        public OutputContainer getResult(@NotNull String itemId, int itemLvl) {
            TreeMap<Integer, OutputContainer> mapContainer = this.items.get(itemId);
            if (mapContainer == null) {
                mapContainer = this.items.get(JStrings.DEFAULT);
            }
            if (mapContainer == null) return null;

            Map.Entry<Integer, OutputContainer> e = mapContainer.floorEntry(itemLvl);
            if (e == null) return null;

            return e.getValue();
        }
    }

    public class OutputContainer {

        private double           cost;
        private List<OutputItem> items;

        public OutputContainer(
                double cost,
                @NotNull List<OutputItem> items
        ) {
            this.cost = cost;
            this.items = items;
        }

        public double getCost() {
            return this.cost;
        }

        @NotNull
        public List<OutputItem> getItems() {
            return this.items;
        }
    }

    public class OutputItem {

        private String       id;
        private double       chance;
        private ItemStack    preview;
        private ItemStack    item;
        private List<String> cmds;

        public OutputItem(
                @NotNull String id,
                double chance,
                @Nullable ItemStack preview,
                @Nullable ItemStack item,
                @NotNull List<String> cmds
        ) {
            this.id = id.toLowerCase();
            this.chance = chance;
            this.item = item;
            this.cmds = cmds;
            this.preview = preview;
        }

        @NotNull
        public String getId() {
            return this.id;
        }

        public double getChance() {
            return this.chance;
        }

        public void setChance(double chance) {
            this.chance = chance;
        }

        @Nullable
        public ItemStack getPreview() {
            return this.preview;
        }

        @Nullable
        public ItemStack getItem() {
            return this.item;
        }

        @NotNull
        public List<String> getCommands() {
            return this.cmds;
        }

        public void give(@NotNull Player player) {
            if (Rnd.get(true) >= this.getChance()) return;

            if (this.item != null) {
                ItemUT.addItem(player, this.item);
            }
            for (String cmd : this.cmds) {
                PlayerUT.execCmd(player, cmd);
            }
        }
    }

    public class DismantleItem extends LimitedItem {

        public DismantleItem(@NotNull Divinity plugin, @NotNull JYML cfg) {
            super(plugin, cfg, DismantleManager.this);
        }
    }
}
