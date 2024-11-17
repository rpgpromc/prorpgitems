package studio.magemonkey.divinity.modules.list.sets;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.api.armor.ArmorEquipEvent;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.LoadableItem;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.api.QModule;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.list.fortify.FortifyManager;
import studio.magemonkey.divinity.modules.list.refine.RefineManager;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.EntityStatsTask;
import studio.magemonkey.divinity.stats.bonus.BonusMap;
import studio.magemonkey.divinity.stats.items.ItemTags;
import studio.magemonkey.divinity.utils.LoreUT;

import java.util.*;
import java.util.stream.Collectors;

public class SetManager extends QModule {

    private static final List<String>         SET_LORE_TAG = List.of("set_cache", "qrpg_set_cache");
    private              String               formatElementActive;
    private              String               formatElementInactive;
    private              List<String>         formatLore;
    private              Map<String, ItemSet> sets;

    public SetManager(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.SETS;
    }

    @Override
    @NotNull
    public String version() {
        return "1.62";
    }

    @Override
    public void setup() {
        String path = "format.";
        this.formatElementActive = StringUT.color(cfg.getString(path + "element.active", "%c%• %name% &a✓"));
        this.formatElementInactive = StringUT.color(cfg.getString(path + "element.inactive", "%c%• %name% &c✗"));
        this.formatLore = StringUT.color(cfg.getStringList(path + "lore"));

        this.sets = new HashMap<>();
        this.plugin.getConfigManager().extractFullPath(this.getFullPath() + "items");

        for (JYML cfg : JYML.loadAll(this.getFullPath() + "items", true)) {
            try {
                ItemSet set = new ItemSet(plugin, cfg);
                this.sets.put(set.getId(), set);
            } catch (Exception ex) {
                error("Could not load Set: " + cfg.getFile().getName());
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void shutdown() {
        if (this.sets != null) {
            this.sets.clear();
            this.sets = null;
        }
    }

    public void addSetPotionEffects(@NotNull LivingEntity entity) {
        EntityStats stats = EntityStats.get(entity);

        for (Map.Entry<ItemSet, Integer> e : this.getEquippedSets(entity).entrySet()) {
            int have = e.getValue();
            if (have <= 0) continue;

            ItemSet set = e.getKey();
            set.getElementBonuses().stream()
                    .filter(setBonus -> have >= setBonus.getMinElementsAmount())
                    .forEach(setBonus -> setBonus.getPotionEffects().forEach(eff -> stats.addPermaPotionEffect(eff)));
        }
    }

    @NotNull
    public Map<ItemSet, Integer> getEquippedSets(@NotNull LivingEntity entity) {
        Map<ItemSet, Integer> sets = new HashMap<>();

        Collection<ItemSet> sets2 = this.getSets();
        for (ItemStack item : EntityStats.get(entity).getEquipment()) {
            sets2.stream().filter(set -> set.isValidElement(item))
                    .forEach(set -> sets.merge(set, 1, Integer::sum));
        }

        return sets;
    }

    @NotNull
    public List<BonusMap> getActiveSetBonuses(@NotNull LivingEntity entity) {
        List<BonusMap> bonuses = new ArrayList<>();

        for (Map.Entry<ItemSet, Integer> e : this.getEquippedSets(entity).entrySet()) {
            int have = e.getValue();
            if (have <= 0) continue;

            ItemSet set = e.getKey();
            set.getElementBonuses().stream()
                    .filter(elm -> have >= elm.getMinElementsAmount())
                    .forEach(elm -> bonuses.add(elm.getBonusMap()));
        }
        return bonuses;
    }

    @NotNull
    private String getClearName(@NotNull ItemStack item) {
        String name = ItemUT.getItemName(item);

        RefineManager refineManager = plugin.getModuleCache().getRefineManager();
        if (refineManager != null) name = refineManager.getNameWithoutLevel(item, name);

        FortifyManager fortifyManager = plugin.getModuleCache().getFortifyManager();
        if (fortifyManager != null) name = fortifyManager.getNameDeformatted(item, name);

        return name.trim();
    }

    public boolean hasSet(@NotNull ItemStack item) {
        return this.getItemSet(item) != null;
    }

    @Nullable
    public ItemSet getItemSet(@NotNull ItemStack item) {
        return this.getSets().stream().filter(set -> set.isValidElement(item)).findFirst().orElse(null);
    }

    private void updateSets(@NotNull Player player) {
        EntityStats entityStats = EntityStats.get(player);
        entityStats.updateAll();

        entityStats.getEquipment().forEach(item -> this.updateItemSet(item, player));
    }

    public void updateItemSet(@NotNull ItemStack item, @Nullable Player player) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.getLore();
        if (lore == null) return;

        StringBuilder loreTag   = new StringBuilder();
        String        storedTag = null;
        for (String key : SET_LORE_TAG) {
            storedTag = ItemUT.getLoreTag(item, key);
            if (storedTag != null) break;
        }
        String[] storedLines = storedTag != null ? storedTag.split(LoreUT.TAG_SPLITTER) : new String[]{};

        int pos = lore.indexOf(ItemTags.PLACEHOLDER_ITEM_SET);
        if (pos < 0) {

            // Delete old set lines
            if (storedLines.length > 0) {
                int firstText = -1;
                for (String stored : storedLines) {
                    firstText++;
                    if (!StringUT.colorOff(stored).isEmpty()) break;
                }

                int index = lore.indexOf(storedLines[firstText]) - firstText;

                if (index >= 0) {
                    if (pos < 0) pos = index;
                    for (int count = 0; count < storedLines.length; count++) {
                        lore.remove(index);
                    }
                }
            }
            if (pos < 0) return; // If no set info then exit
        } else lore.remove(pos);

        ItemSet set = this.getItemSet(item);
        if (set != null) {
            for (String lineFormat : this.formatLore) {
                if (lineFormat.contains("%elements%")) {
                    for (SetElement setPart : set.getElements()) {
                        String name   = StringUT.colorOff(setPart.getName());
                        String color  = set.getColorInactive();
                        String format = this.formatElementInactive;

                        if (player != null && setPart.isEquipped(player)) {
                            format = this.formatElementActive;
                            color = set.getColorActive();
                        }

                        String name3 = format.replace("%c%", color).replace("%name%", name);
                        pos = LoreUT.addToLore(lore, pos, name3);

                        if (loreTag.length() > 0) loreTag.append(LoreUT.TAG_SPLITTER);
                        loreTag.append(name3);
                    }
                } else if (lineFormat.contains("%effects%")) {
                    int partHave = (player != null) ? set.getEquippedElements(player) : 0;

                    for (SetElementBonus setPartBonus : set.getElementBonuses()) {
                        int partNeed = setPartBonus.getMinElementsAmount();

                        String color = set.getColorInactive();
                        if (partHave >= partNeed) {
                            color = set.getColorActive();
                        }

                        List<String> loreBonus = setPartBonus.getLore();
                        for (String lineBonus : loreBonus) {
                            String lineX = lineBonus.replace("%c%", color);
                            if (lineX.isEmpty()) lineX = ChatColor.GRAY + lineX;
                            pos = LoreUT.addToLore(lore, pos, lineX);

                            if (loreTag.length() > 0) loreTag.append(LoreUT.TAG_SPLITTER);
                            loreTag.append(lineX);
                        }
                    }
                } else {
                    String lineX = lineFormat.replace("%set%", set.getName());
                    if (lineX.isEmpty()) lineX = ChatColor.GRAY + lineX;

                    pos = LoreUT.addToLore(lore, pos, lineX);

                    if (loreTag.length() > 0) loreTag.append(LoreUT.TAG_SPLITTER);
                    loreTag.append(lineX);
                }
            }
        }
        meta.setLore(lore);
        item.setItemMeta(meta);

        ItemUT.addLoreTag(item, SET_LORE_TAG.get(0), loreTag.toString());
    }

    @NotNull
    public Collection<ItemSet> getSets() {
        return this.sets.values();
    }

    @Nullable
    public ItemSet getSetById(@NotNull String id) {
        return id.equalsIgnoreCase(QModuleDrop.RANDOM_ID)
                ? Rnd.get(new ArrayList<>(this.getSets()))
                : this.sets.get(id.toLowerCase());
    }

    @NotNull
    public List<String> getSetNames() {
        return new ArrayList<>(sets.keySet());
    }

    // -------------------------------------------------------------------- //
    // EVENTS

    @EventHandler(ignoreCancelled = true)
    public void onSetUpdateEquip(ArmorEquipEvent e) {
        Player p = e.getPlayer();

        if (p.getGameMode() == GameMode.CREATIVE) return;

        boolean update = false;

        ItemStack oldPiece = e.getOldArmorPiece();
        if (oldPiece != null && this.hasSet(oldPiece)) {
            this.updateItemSet(oldPiece, null);
            update = true;
        }

        ItemStack newPiece = e.getNewArmorPiece();
        if (newPiece != null && this.hasSet(newPiece)) update = true;

        if (update) this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.updateSets(p));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSetUpdateSpawn(ItemSpawnEvent e) {
        Item      item  = e.getEntity();
        ItemStack stack = item.getItemStack();

        this.updateItemSet(stack, null);
        item.setItemStack(stack);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSetNameHackAnvil(PrepareAnvilEvent e) {
        ItemStack result = e.getResult();
        if (result == null) return;

        ItemStack before = e.getInventory().getItem(0);
        if (before == null) return;

        ItemMeta metaBefore = before.getItemMeta();
        ItemMeta metaResult = result.getItemMeta();

        String nameBefore = metaBefore == null ? "" : StringUT.colorOff(metaBefore.getDisplayName());
        String nameResult = metaResult == null ? "" : StringUT.colorOff(metaResult.getDisplayName());

        if (nameBefore.equalsIgnoreCase(nameResult)) return;

        if (this.hasSet(result) || this.hasSet(before)) e.setResult(null);
    }

    // -------------------------------------------------------------------- //
    // CLASSES

    public class ItemSet extends LoadableItem {

        private final String                            name;
        private final String                            prefix;
        private final String                            suffix;
        private final String                            colorHave;
        private final String                            colorMiss;
        private final Map<String, SetElement>           elements;
        private final TreeMap<Integer, SetElementBonus> elementBonus;

        public ItemSet(@NotNull Divinity plugin, @NotNull JYML cfg) {
            super(plugin, cfg);

            this.name = StringUT.color(cfg.getString("name", id));
            this.prefix = StringUT.color(cfg.getString("prefix", ""));
            this.suffix = StringUT.color(cfg.getString("suffix", ""));
            this.colorHave = StringUT.color(cfg.getString("color.active", "&a"));
            this.colorMiss = StringUT.color(cfg.getString("color.inactive", "&8"));

            this.elements = new LinkedHashMap<>();
            for (String eId : cfg.getSection("elements")) {
                String path2 = "elements." + eId + ".";

                Set<Material> eMaterials = new HashSet<>();
                for (String matName : cfg.getStringList(path2 + "materials")) {
                    Material eMat = Material.getMaterial(matName.toUpperCase());
                    if (eMat != null) eMaterials.add(eMat);
                }
                if (eMaterials.isEmpty()) {
                    error("Invalid material for '" + eId + "' element of '" + id + "' set!");
                    continue;
                }

                String eRawName = cfg.getString(path2 + "name");
                if (eRawName == null) {
                    error("No Set Element name provided in: " + cfg.getFile().getName());
                    continue;
                }

                String eName = StringUT.oneSpace(StringUT.color(eRawName)
                        .replace("%suffix%", suffix)
                        .replace("%prefix%", prefix));

                SetElement setElement = new SetElement(eId, eMaterials, eName);
                this.elements.put(setElement.getId(), setElement);
            }

            this.elementBonus = new TreeMap<>();
            for (String sAmount : cfg.getSection("bonuses.by-elements-amount")) {
                int amount = StringUT.getInteger(sAmount, -1);
                if (amount < 1) continue;

                String path2 = "bonuses.by-elements-amount." + sAmount + ".";

                List<String> bonusLore = cfg.getStringList(path2 + "lore").stream()
                        .map(StringUT::color)
                        .collect(Collectors.toList());

                // Setup potion effects.
                // With good map = better performance.
                Map<PotionEffectType, Integer> potions = new HashMap<>();
                for (String potionName : cfg.getSection(path2 + "potion-effects")) {
                    PotionEffectType pet = PotionEffectType.getByName(potionName.toUpperCase());
                    if (pet == null) {
                        error("Invalid potion effect " + potionName + " for set " + id);
                        continue;
                    }
                    int level = cfg.getInt(path2 + "potion-effects." + potionName);
                    if (level > 0) potions.put(pet, level);
                }

                BonusMap bonusMap = new BonusMap();
                bonusMap.loadStats(cfg, path2 + "item-stats");
                bonusMap.loadDamages(cfg, path2 + "damage-types");
                bonusMap.loadDefenses(cfg, path2 + "defense-types");

                SetElementBonus spe = new SetElementBonus(amount, bonusLore, potions, bonusMap);
                this.elementBonus.put(amount, spe);
            }
        }

        @NotNull
        public String getName() {
            return this.name;
        }

        @NotNull
        public String getPrefix() {
            return this.prefix;
        }

        @NotNull
        public String getSuffix() {
            return this.suffix;
        }

        @NotNull
        public String getColorActive() {
            return this.colorHave;
        }

        @NotNull
        public String getColorInactive() {
            return this.colorMiss;
        }

        @NotNull
        public Collection<SetElement> getElements() {
            return this.elements.values();
        }

        @Nullable
        public SetElement getElement(@NotNull String id) {
            return this.elements.get(id.toLowerCase());
        }

        @NotNull
        public Collection<SetElementBonus> getElementBonuses() {
            return this.elementBonus.values();
        }

        public boolean isValidElement(@NotNull ItemStack item) {
            return this.getElements().stream().anyMatch(element -> element.isValidElement(item));
        }

        public int getEquippedElements(@NotNull LivingEntity entity) {
            return (int) this.getElements().stream()
                    .filter(element -> element.isEquipped(entity)).count();
        }

        @Override
        protected void save(@NotNull JYML cfg) {

        }
    }

    public class SetElement {

        private final String        id;
        private final Set<Material> materials;
        private       String        name;

        public SetElement(
                @NotNull String id,
                @NotNull Set<Material> materials,
                @NotNull String name
        ) {
            this.id = id.toLowerCase();
            this.materials = materials;
            this.setName(name);
        }

        @NotNull
        public String getId() {
            return this.id;
        }

        @NotNull
        public Set<Material> getMaterials() {
            return this.materials;
        }

        @NotNull
        public String getName() {
            return this.name;
        }

        public void setName(@NotNull String name) {
            this.name = name.trim(); // Remove spaces from name
        }

        public boolean isEquipped(@NotNull LivingEntity p) {
            return EntityStats.get(p).getEquipment().stream().anyMatch(this::isValidElement);
        }

        public boolean isValidElement(@NotNull ItemStack item) {
            if (!this.getMaterials().contains(item.getType())) return false;

            String itemName = StringUT.colorOff(getClearName(item));
            String elemName = StringUT.colorOff(this.getName());
            return itemName.toLowerCase().contains(elemName.toLowerCase());
        }
    }

    public class SetElementBonus {

        private final int               minAmount;
        private final List<String>      lore;
        private final Set<PotionEffect> potions;
        private final BonusMap          bonusMap;

        public SetElementBonus(
                int amount,
                @NotNull List<String> lore,
                @NotNull Map<PotionEffectType, Integer> potions,
                @NotNull BonusMap bonusMap
        ) {
            this.minAmount = amount;
            this.lore = lore;
            this.potions = new HashSet<>();
            for (Map.Entry<PotionEffectType, Integer> e : potions.entrySet()) {
                PotionEffectType type = e.getKey();
                int              dur  = EntityStatsTask.POTION_DURATION;
                if (type.equals(PotionEffectType.NIGHT_VISION)
                        || type.getName().equalsIgnoreCase(PotionEffectType.NIGHT_VISION.getName())) {
                    dur *= 5;
                }

                PotionEffect pe = new PotionEffect(type, dur, Math.max(0, e.getValue() - 1));
                this.potions.add(pe);
            }

            this.bonusMap = bonusMap;
        }

        public int getMinElementsAmount() {
            return this.minAmount;
        }

        @NotNull
        public List<String> getLore() {
            return this.lore;
        }

        @NotNull
        public Set<PotionEffect> getPotionEffects() {
            return this.potions;
        }

        @NotNull
        public BonusMap getBonusMap() {
            return this.bonusMap;
        }
    }
}
