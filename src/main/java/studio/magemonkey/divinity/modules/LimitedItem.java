package studio.magemonkey.divinity.modules;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.list.identify.IdentifyManager;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.ItemTags;
import studio.magemonkey.divinity.stats.items.attributes.ChargesAttribute;
import studio.magemonkey.divinity.utils.LoreUT;

import java.util.Map;
import java.util.TreeMap;

public class LimitedItem extends LeveledItem {
    protected TreeMap<Integer, Integer> chargesByLvl;

    // Creating new config
    @Deprecated
    public LimitedItem(Divinity plugin, String path, QModuleDrop<?> module) throws InvalidConfigurationException {
        super(plugin, path, module);
    }

    // Load from config
    public LimitedItem(@NotNull Divinity plugin, @NotNull JYML cfg, @NotNull QModuleDrop<?> module) {
        super(plugin, cfg, module);

        this.chargesByLvl = new TreeMap<>();
        for (String sLvl : cfg.getSection("uses-by-level")) {
            int itemLvl = StringUT.getInteger(sLvl, -1);
            if (itemLvl <= 0) continue;

            int uses = cfg.getInt("uses-by-level." + sLvl);
            this.chargesByLvl.put(itemLvl, uses);
        }
    }

    @NotNull
    public TreeMap<Integer, Integer> getChargesMap() {
        return this.chargesByLvl;
    }

    public final int getCharges(int lvl) {
        Map.Entry<Integer, Integer> e = this.chargesByLvl.floorEntry(lvl);
        if (e == null) return -1;

        return e.getValue();
    }

    @Override
    @NotNull
    public final ItemStack create(int lvl) {
        return this.create(lvl, -1);
    }

    @Override
    @NotNull
    protected final ItemStack build(int lvl) {
        return this.build(lvl, -1);
    }

    @NotNull
    public ItemStack create(int lvl, int uses) {
        lvl = this.validateLevel(lvl); // Normalize level.
        if (uses < 1) uses = this.getCharges(lvl);

        return this.build(lvl, uses);
    }

    @NotNull
    protected ItemStack build(int lvl, int uses) {
        return build(null, lvl, uses);
    }

    @NotNull
    protected ItemStack build(@Nullable ItemStack item, int lvl, int uses) {
        item = item == null ? super.build(lvl) : super.build(item, lvl);

        ChargesAttribute charges = ItemStats.getAttribute(ChargesAttribute.class);
        if (charges != null) {
            if (uses == 1 && (this instanceof SocketItem || this instanceof IdentifyManager.IdentifyItem))
                LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_ITEM_CHARGES, null);
            else
                //if (charges.hasPlaceholder(item)) { // In order for unlimited charged items to work
                // properly, this needs to be disabled. Not sure if this will produce any other harmful side effects.
                charges.add(item, new int[]{uses, uses}, -1);
            //}
        } else
            LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_ITEM_CHARGES, null);

        return item;
    }
}
