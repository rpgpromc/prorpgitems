package studio.magemonkey.divinity.modules.list.extractor;

import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.compat.VersionManager;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.external.VaultHK;
import studio.magemonkey.codex.hooks.external.citizens.CitizensHK;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.actions.ActionManipulator;
import studio.magemonkey.codex.util.eval.Evaluator;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.LimitedItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.list.extractor.ExtractorManager.ExtractorTool;
import studio.magemonkey.divinity.modules.list.extractor.command.ExtractorOpenCmd;
import studio.magemonkey.divinity.modules.list.extractor.event.PlayerExtractSocketEvent;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.SocketAttribute;

import java.util.HashMap;
import java.util.Map;

public class ExtractorManager extends QModuleDrop<ExtractorTool> {

    private Map<SocketAttribute.Type, Map<String, String>> extractPrice;
    private ActionManipulator                              extractActionsComplete;
    private ActionManipulator                              extractActionsError;

    private ExtractGUI gui;

    public ExtractorManager(@NotNull Divinity plugin) {
        super(plugin, ExtractorTool.class);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.EXTRACTOR;
    }

    @Override
    @NotNull
    public String version() {
        return "1.3.0";
    }

    @Override
    public void setup() {
        this.moduleCommand.addSubCommand(new ExtractorOpenCmd(this));

        VaultHK vh = plugin.getVault();
        if (vh != null && vh.getEconomy() != null) {
            this.extractPrice = new HashMap<>();
            for (SocketAttribute.Type type : SocketAttribute.Type.values()) {
                Map<String, String> socketPrices = new HashMap<>();
                for (String socketCategory : cfg.getSection("extraction.price." + type.name())) {
                    String path         = "extraction.price." + type.name() + "." + socketCategory;
                    String priceFormula = cfg.getString(path, "75 * %socket_level%");

                    socketPrices.put(socketCategory.toLowerCase(), priceFormula);
                }
                this.extractPrice.put(type, socketPrices);
            }
        } else {
            this.warn("No economy found. Extraction will be free.");
        }

        this.extractActionsComplete = new ActionManipulator(plugin, cfg, "extraction.actions-complete");
        this.extractActionsError = new ActionManipulator(plugin, cfg, "extraction.actions-error");

        CitizensHK citizens = plugin.getCitizens();
        if (citizens != null) {
            TraitInfo trait = TraitInfo.create(ExtractorTrait.class).withName("extractor");
            citizens.registerTrait(plugin, trait);
        }

        this.gui = new ExtractGUI(this);
    }

    @Override
    public void shutdown() {
        if (this.gui != null) {
            this.gui.shutdown();
            this.gui = null;
        }
        if (this.extractPrice != null) {
            this.extractPrice.clear();
            this.extractPrice = null;
        }
        this.extractActionsComplete = null;
        this.extractActionsError = null;
    }

    public final boolean openExtraction(
            @NotNull Player player,
            @Nullable ItemStack target,
            @Nullable ItemStack src,
            @Nullable SocketAttribute.Type type,
            boolean force
    ) {

        if (!force && !player.hasPermission(Perms.EXTRACTOR_GUI)) {
            plugin.lang().Error_NoPerm.send(player);
            return false;
        }

        if (target != null) {
            if (!this.canExtract(target)) {
                plugin.lang().Extractor_Open_Error_NoSockets
                        .replace("%item%", ItemUT.getItemName(target))
                        .send(player);
                return false;
            }

            this.splitDragItem(player, src, target);
        }
        this.gui.open(player, target, src, type);
        return true;
    }

    public double getExtractionPrice(
            @NotNull SocketAttribute.Type type, @NotNull String socketCat, int level) {

        if (this.extractPrice == null) return 0D;

        Map<String, String> map = this.extractPrice.get(type);
        if (map == null) return 0D;

        // TODO Add default?

        String formula = map.get(socketCat);
        if (formula == null) return 0D;

        formula = formula.replace("%socket_level%", String.valueOf(level));

        return Evaluator.eval(formula, 1);
    }

    public boolean canExtract(@NotNull ItemStack item) {
        // Allow to open 'empty' extractor.
        if (item.getType() == Material.AIR) return true;

        for (SocketAttribute.Type m : SocketAttribute.Type.values()) {
            for (SocketAttribute socketAtt : ItemStats.getSockets(m)) {
                if (socketAtt.getFilledAmount(item) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------------- //
    // EVENTS

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onExtractComplete(PlayerExtractSocketEvent e) {
        Player player = e.getPlayer();
        if (e.isFailed()) {
            this.extractActionsError.process(player);
            return;
        }

        plugin.lang().Extractor_Extract_Complete.send(player);
        this.extractActionsComplete.process(player);
    }

    @Override
    protected boolean onDragDrop(
            @NotNull Player player,
            @NotNull ItemStack src,
            @NotNull ItemStack target,
            @NotNull ExtractorTool mItem,
            @NotNull InventoryClickEvent e) {
        VersionManager.getCompat().setCursor(e, null);
        boolean open = this.openExtraction(player, target, src, null, true);
        if (open) src.setAmount(0);

        Bukkit.getScheduler().runTaskLater(Divinity.getInstance(), player::updateInventory, 1L);
        return open;
    }

    // -------------------------------------------------------------------- //
    // CLASSES

    public class ExtractorTool extends LimitedItem {

        public ExtractorTool(@NotNull Divinity plugin, @NotNull JYML cfg) {
            super(plugin, cfg, ExtractorManager.this);
        }
    }
}
