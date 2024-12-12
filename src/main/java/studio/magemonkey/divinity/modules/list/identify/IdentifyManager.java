package studio.magemonkey.divinity.modules.list.identify;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.items.providers.ICodexItemProvider;
import studio.magemonkey.codex.api.items.providers.VanillaProvider;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.modules.IModule;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.actions.ActionManipulator;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.DivinityAPI;
import studio.magemonkey.divinity.manager.listener.object.DynamicStatListener;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.LimitedItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.list.identify.IdentifyManager.IdentifyItem;
import studio.magemonkey.divinity.modules.list.identify.command.IdentifyCmd;
import studio.magemonkey.divinity.modules.list.identify.event.PlayerIdentifyItemEvent;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager.GeneratorItem;
import studio.magemonkey.divinity.stats.items.ItemStats;

import java.util.*;

public class IdentifyManager extends QModuleDrop<IdentifyItem> {

    private ActionManipulator actionsComplete;
    private ActionManipulator actionsError;

    private static final String PERFIX_TOME = "tome-";
    private static final String PREFIX_ITEM = "item-";

    public IdentifyManager(@NotNull Divinity plugin) {
        super(plugin, IdentifyItem.class);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.IDENTIFY;
    }

    @Override
    @NotNull
    public String version() {
        return "1.2.0";
    }

    @Override
    public void setup() {
        this.actionsComplete = new ActionManipulator(plugin, cfg, "general.actions-complete");
        this.actionsError = new ActionManipulator(plugin, cfg, "general.actions-error");

        this.moduleCommand.addSubCommand(new IdentifyCmd(this));
    }

    @Override
    public void shutdown() {

    }

    @Override
    protected void loadItems() {
        this.items = new HashMap<>();
        this.plugin.getConfigManager().extractFullPath(plugin.getDataFolder() + this.getItemsFolder());
        this.plugin.getConfigManager().extractFullPath(plugin.getDataFolder() + this.getPath() + "tomes");

        for (JYML cfg : JYML.loadAll(this.getFullPath() + "tomes", true)) {
            try {
                IdentifyTome tome = new IdentifyTome(plugin, cfg);
                this.items.put(tome.getId(), tome);
            } catch (IllegalArgumentException iae) {
                this.error("Could not load item '" + cfg.getFile().getName() + "'");
                this.error(" - " + iae.getMessage());
            } catch (Exception e) {
                if (e.getClass().getSimpleName().equals("FabledNotEnabledException")) {
                    this.error("Could not load item '" + cfg.getFile().getName() + "'");
                    this.error(" - It looks like this item uses skills from Fabled, but Fabled is not enabled yet");
                } else {
                    this.error("Could not load item '" + cfg.getFile().getName() + "'");
                    e.printStackTrace();
                }
            }
        }

        for (JYML cfg : JYML.loadAll(plugin.getDataFolder() + this.getItemsFolder(), true)) {
            try {
                UnidentifiedItem item = new UnidentifiedItem(plugin, cfg);
                this.items.put(item.getId(), item);
            } catch (IllegalArgumentException iae) {
                this.error("Could not load item '" + cfg.getFile().getName() + "'");
                this.error(" - " + iae.getMessage());
            } catch (Exception e) {
                if (e.getClass().getSimpleName().equals("FabledNotEnabledException")) {
                    this.error("Could not load item '" + cfg.getFile().getName() + "'");
                    this.error(" - It looks like this item uses skills from Fabled, but Fabled is not enabled yet");
                }
                this.error("Could not load item '" + cfg.getFile().getName() + "'");
                e.printStackTrace();
            }
        }
    }

    /**
     * @param item ItemStack
     * @return true if item is IdentifyTome
     */
    public boolean isUnidentified(@NotNull ItemStack item) {
        String id = this.getItemId(item);
        if (id == null) return false;

        return this.isUnidentified(this.getItemById(id));
    }

    private boolean isUnidentified(@Nullable IdentifyItem item) {
        return item instanceof UnidentifiedItem;
    }

    public boolean isTome(@NotNull ItemStack item) {
        String id = this.getItemId(item);
        if (id == null) return false;

        return this.isTome(this.getItemById(id));
    }

    private boolean isTome(@Nullable IdentifyItem item) {
        return item instanceof IdentifyTome;
    }

    private boolean isValidTome(@NotNull ItemStack target, @NotNull ItemStack tome) {
        if (!this.isUnidentified(target)) return false;
        if (!this.isTome(tome)) return false;

        IdentifyTome iTome = (IdentifyTome) this.getModuleItem(tome);
        if (iTome == null) return false;

        UnidentifiedItem uItem = (UnidentifiedItem) this.getModuleItem(target);
        if (uItem == null) return false;

        return uItem.getApplicableTomeIds().contains(iTome.getId());
    }

    @Nullable
    public ItemStack getIdentifiedOf(@NotNull ItemStack unknown) {
        UnidentifiedItem uItem = (UnidentifiedItem) this.getModuleItem(unknown);
        if (uItem == null) return null;

        ItemGeneratorManager generatorManager = plugin.getModuleCache().getTierManager();

        ItemStack unlock = null;
        int       lvl    = ItemStats.getLevel(unknown);

        if (uItem.getResultModule() instanceof ItemGeneratorManager && generatorManager != null) {
            GeneratorItem result = generatorManager.getItemById(uItem.getResultId());
            if (result != null) {
                unlock = result.create(lvl, -1, CodexEngine.get().getItemManager().getItemTypes(unknown).stream()
                        .filter(itemType -> itemType.getCategory() != ICodexItemProvider.Category.PRO)
                        .max(Comparator.comparing(ItemType::getCategory))
                        .orElseGet(() -> new VanillaProvider.VanillaItemType(unknown.getType())));
            }
        } else {
            unlock = DivinityAPI.getItemByModule(uItem.getResultModule(), uItem.getResultId(), lvl, -1, -1);
        }

        return unlock;
    }

    @Override
    protected boolean onDragDrop(
            @NotNull Player player,
            @NotNull ItemStack src,
            @NotNull ItemStack target,
            @NotNull IdentifyItem mItem,
            @NotNull InventoryClickEvent e) {

        // Check if IdentifyTome can be applied to this item
        if (!this.isValidTome(target, src)) {
            plugin.lang().Identify_Identify_Error_Tome.send(player);
            return false;
        }

        ItemStack unlock = this.getIdentifiedOf(target);
        if (unlock == null || unlock.getType() == Material.AIR) {
            plugin.lang().Error_Internal.send(player);
            return false;
        }

        UnidentifiedItem uItem = (UnidentifiedItem) this.getModuleItem(target);
        IdentifyTome     iTome = (IdentifyTome) this.getModuleItem(src);
        if (uItem == null || iTome == null) return false;

        PlayerIdentifyItemEvent eve = new PlayerIdentifyItemEvent(uItem, iTome, unlock, player);
        plugin.getPluginManager().callEvent(eve);
        if (eve.isCancelled()) {
            this.actionsError.process(player);
            return false;
        }

        this.takeChargeClickEvent(player, src, e);

        // Save other items in stack
        // and then return them back to a player
        ItemStack lost = null;
        if (target.getAmount() > 1) {
            lost = new ItemStack(target);
            lost.setAmount(target.getAmount() - 1);
        }

        DynamicStatListener.updateItem(player, unlock);
        e.setCurrentItem(unlock);

        if (lost != null) {
            ItemUT.addItem(player, lost);
        }

        plugin.lang().Identify_Identify_Success
                .replace("%item%", ItemUT.getItemName(unlock))
                .send(player);

        this.actionsComplete.process(player);
        return true;
    }

    public abstract class IdentifyItem extends LimitedItem {

        public IdentifyItem(@NotNull Divinity plugin, @NotNull JYML cfg, @NotNull QModuleDrop<?> module) {
            super(plugin, cfg, IdentifyManager.this);
        }
    }

    public class IdentifyTome extends IdentifyItem {

        public IdentifyTome(@NotNull Divinity plugin, @NotNull JYML cfg) {
            super(plugin, cfg, IdentifyManager.this);
            this.id = PERFIX_TOME + this.id; // Add tome prefix to item id
        }
    }

    public class UnidentifiedItem extends IdentifyItem {

        private QModuleDrop<?> itemModule;
        private String         itemId;
        private Set<String>    applicableTomes;

        public UnidentifiedItem(@NotNull Divinity plugin, @NotNull JYML cfg) {
            super(plugin, cfg, IdentifyManager.this);
            this.id = PREFIX_ITEM + this.id; // Add item prefix to item id

            String itemModule = cfg.getString("item-module");
            if (itemModule == null) {
                throw new IllegalArgumentException("Invalid 'item-module' setting.");
            }
            IModule<?> mod = plugin.getModuleManager().getModule(itemModule);
            if (mod == null || !mod.isLoaded() || !(mod instanceof QModuleDrop<?>)) {
                throw new IllegalArgumentException("Invalid 'item-module' setting. No such module.");
            }
            this.itemModule = (QModuleDrop<?>) mod;

            String finalId = cfg.getString("item-id");
            if (finalId == null || this.itemModule.getItemById(finalId) == null) {
                throw new IllegalArgumentException(
                        "Invalid 'item-id' provided (" + finalId + ")! No such item.");
            }
            this.setResultId(finalId);

            this.applicableTomes = new HashSet<>();
            for (String s : cfg.getStringList("applicable-tomes")) {
                this.applicableTomes.add(PERFIX_TOME + s.toLowerCase());
            }
        }

        @NotNull
        public QModuleDrop<?> getResultModule() {
            return this.itemModule;
        }

        /**
         * @return Returns the final (original/identified) item id.
         * Returns an empty string if no item id was attached.
         */
        @NotNull
        public String getResultId() {
            return this.itemId;
        }


        public void setResultId(@NotNull String item) {
            this.itemId = item.toLowerCase();
        }

        /**
         * @return Returns list with IdentifyTome ids, which are can
         * be applied to this item to identify it.
         */
        @NotNull
        public Set<String> getApplicableTomeIds() {
            return this.applicableTomes;
        }

        @Override
        @NotNull
        protected ItemStack build(int lvl, int uses) {
            ItemStack orig = DivinityAPI.getItemByModule(this.getResultModule(), getResultId(), lvl, -1, -1);
            if (orig == null) {
                error("Invalid module for unidentified item '" + id + "' !");
                return new ItemStack(Material.AIR);
            }

            ItemStack item = super.build(lvl, uses);
            item.setType(orig.getType());

            ItemMeta metaItem = item.getItemMeta();
            ItemMeta metaOrig = orig.getItemMeta();
            if (metaOrig != null && metaItem != null) {
                if (metaOrig instanceof Damageable && metaItem instanceof Damageable) {
                    Damageable dOrig = (Damageable) metaOrig;
                    Damageable dItem = (Damageable) metaItem;

                    dItem.setDamage(dOrig.getDamage());
                }
                if (metaOrig.hasCustomModelData()) {
                    metaItem.setCustomModelData(metaOrig.getCustomModelData());
                }

                List<String> lore = metaItem.getLore();
                if (lore == null) return item;

                List<String> loreCopy = new ArrayList<>();
                for (String line : lore) {
                    if (line.contains("%tome-name%")) {
                        for (String tomeId : this.getApplicableTomeIds()) {
                            IdentifyItem tome = IdentifyManager.this.getItemById(tomeId);
                            if (tome == null) continue;

                            ItemStack tomeItem = tome.create(1);
                            loreCopy.add(line.replace("%tome-name%", ItemUT.getItemName(tomeItem)));
                        }
                    } else {
                        loreCopy.add(line);
                    }
                }
                metaItem.setLore(loreCopy);
                item.setItemMeta(metaItem);
            }

            return item;
        }
    }
}
