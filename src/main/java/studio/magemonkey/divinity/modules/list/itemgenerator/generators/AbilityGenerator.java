package studio.magemonkey.divinity.modules.list.itemgenerator.generators;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.ClickType;
import studio.magemonkey.codex.util.DataUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager.GeneratorItem;
import studio.magemonkey.divinity.modules.list.itemgenerator.api.AbstractAttributeGenerator;
import studio.magemonkey.divinity.utils.LoreUT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AbilityGenerator extends AbstractAttributeGenerator {
    public static NamespacedKey                         LEGACY_KEY;
    public static NamespacedKey                         ABILITY_KEY;
    private final Map<AbilityGenerator.Ability, Double> abilities;

    public AbilityGenerator(@NotNull Divinity plugin,
                            @NotNull GeneratorItem generatorItem,
                            @NotNull String placeholder) {
        super(plugin, generatorItem, placeholder);
        AbilityGenerator.LEGACY_KEY = NamespacedKey.fromString("prorpgitems:skills");
        AbilityGenerator.ABILITY_KEY = NamespacedKey.fromString("skills", plugin);

        JYML   cfg  = this.generatorItem.getConfig();
        String path = "generator.skills.";

        this.minAmount = cfg.getInt(path + "minimum");
        this.maxAmount = Math.min(cfg.getInt(path + "maximum"), ClickType.values().length);

        this.abilities = new HashMap<>();
        for (String abilityId : cfg.getSection(path + "list")) {
            String path2 = path + "list." + abilityId + ".";

            double chance = cfg.getDouble(path2 + "chance");
            if (chance <= 0) {
                continue;
            }

            AbilityGenerator.Ability ability = new Ability(abilityId,
                    cfg.getInt(path2 + "min-level", 1),
                    cfg.getInt(path2 + "max-level", 1),
                    cfg.getStringList(path2 + "lore-format"));
            this.abilities.put(ability, chance);
        }
    }

    public static void updateNamespace(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String[] abilityArray = DataUT.getStringArrayData(item, LEGACY_KEY);
        if (abilityArray == null) return;

        // Replace the legacy keyed ability with the new one
        DataUT.setData(item, ABILITY_KEY, abilityArray);
    }

    @Override
    public void generate(@NotNull ItemStack item, int itemLevel) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> lore = meta.getLore();
        if (lore == null) {
            return;
        }

        int pos = lore.indexOf(this.placeholder);
        int min = this.getMinAmount();
        int max = this.getMaxAmount();

        if (pos < 0 || max == 0 || this.abilities.isEmpty()) {
            LoreUT.replacePlaceholder(item, placeholder, null);
            return;
        }

        Map<AbilityGenerator.Ability, Double> abilityMap = new HashMap<>(this.abilities);

        // Min: -1, Max: 5
        // Roll: <=5
        //
        // Min: -1, Max: -1
        // Roll: Unlimited
        //
        // Min: 3, Max: -1
        // Roll: >3
        //
        // Min: 2, Max: 5
        // Roll: (2, 5);
        boolean isMaxUnlimited = (max < 0);
        boolean isMinUnlimited = (min < 0);
        int     rollMax        = isMaxUnlimited ? Integer.MAX_VALUE - 1 : max;
        int     rollMin        = isMinUnlimited ? Rnd.get(rollMax + 1) : min;
        int     roll           = Rnd.get((isMaxUnlimited ? rollMax : rollMin), rollMax);

        // If get stats number is 0
        // Remove all stat placeholders
        if (roll <= 0) {
            LoreUT.replacePlaceholder(item, this.placeholder, null);
            return;
        }

        Map<AbilityGenerator.Ability, Integer> abilityAdd = new HashMap<>();

        for (int count = 0; count < roll; count++) {
            if (abilityMap.isEmpty()) {
                break;
            }

            AbilityGenerator.Ability ability = Rnd.getRandomItem(abilityMap, true);
            if (ability == null) {
                break;
            }

            // Minimal stats are added, so we can process chances
            if (count >= rollMin) {
                // If stats are not limited, then we will check
                // for a chance to apply on item manually.
                double chance = abilityMap.get(ability);
                if (Rnd.get(true) > chance) {
                    abilityMap.remove(ability);
                    continue;
                }
            }

            // Add ability lore with variables.
            int level = ability.getRndLevel();
            for (String format : ability.getLoreFormat()) {
                pos = LoreUT.addToLore(lore, pos, format.replace("%level%", String.valueOf(level)));
            }
            lore.remove(this.placeholder);

            abilityAdd.put(ability, level);
            abilityMap.remove(ability);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        int      i            = 0;
        String[] abilityArray = new String[abilityAdd.size()];
        for (Map.Entry<AbilityGenerator.Ability, Integer> entry : abilityAdd.entrySet()) {
            abilityArray[i] = entry.getKey().getId() + ':' + entry.getValue();
            i++;
        }
        DataUT.setData(item, ABILITY_KEY, abilityArray);
    }

    public static class Ability {

        private final String       id;
        private final int          minLevel;
        private final int          maxLevel;
        private final List<String> loreFormat;

        public Ability(
                @NotNull String id,
                int minLevel,
                int maxLevel,
                @NotNull List<String> loreFormat
        ) {
            this.id = id.toLowerCase();
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.loreFormat = StringUT.color(loreFormat);
        }

        @NotNull
        public String getId() {
            return id;
        }

        public int getRndLevel() {return Rnd.get(minLevel, maxLevel);}

        @NotNull
        public List<String> getLoreFormat() {
            return loreFormat;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Ability ability = (Ability) o;
            return id.equals(ability.id);
        }

        @Override
        public int hashCode() {return Objects.hash(id);}
    }
}
