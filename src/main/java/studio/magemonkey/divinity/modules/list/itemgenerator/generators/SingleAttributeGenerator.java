package studio.magemonkey.divinity.modules.list.itemgenerator.generators;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager.GeneratorItem;
import studio.magemonkey.divinity.modules.list.itemgenerator.api.AbstractAttributeGenerator;
import studio.magemonkey.divinity.stats.bonus.BonusCalculator;
import studio.magemonkey.divinity.stats.items.api.ItemLoreStat;
import studio.magemonkey.divinity.utils.LoreUT;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SingleAttributeGenerator<A extends ItemLoreStat<String>> extends AbstractAttributeGenerator {

    private Map<A, Double> attributes;

    public SingleAttributeGenerator(
            @NotNull Divinity plugin,
            @NotNull GeneratorItem generatorItem,
            @NotNull String path,
            @NotNull Collection<A> attributesAll,
            @NotNull String placeholder) {
        super(plugin, generatorItem, placeholder);
        JYML cfg = this.generatorItem.getConfig();

        this.minAmount = 1;
        this.maxAmount = 1;

        this.attributes = new HashMap<>();
        attributesAll.forEach(att -> {
            double chance = cfg.getDouble(path + att.getId().toUpperCase());
            //if (chance <= 0) return; Removed so that Bonuses can be applied

            this.attributes.put(att, chance);
        });
    }

    @Override
    public void generate(@NotNull ItemStack item, int itemLevel) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.getLore();
        if (lore == null) return;

        if (this.attributes.isEmpty()) {
            LoreUT.replacePlaceholder(item, this.placeholder, null);
            return;
        }
        Map<A, Double> copy = new HashMap<>();
        for (Map.Entry<A, Double> entry : this.attributes.entrySet()) {
            double weight = BonusCalculator.SIMPLE_FULL.apply(entry.getValue(),
                    List.of(generatorItem.getMaterialModifiers(item, entry.getKey())));
            if (weight > 0) copy.put(entry.getKey(), weight);
        }
        @Nullable A att = Rnd.getRandomItem(copy, true);
        if (att != null) {
            att.add(item, att.getName(), -1);
        }
        LoreUT.replacePlaceholder(item, this.placeholder, null);
    }
}
