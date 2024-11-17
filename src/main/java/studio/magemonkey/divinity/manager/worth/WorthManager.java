package studio.magemonkey.divinity.manager.worth;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.Loadable;
import studio.magemonkey.codex.modules.IModule;
import studio.magemonkey.codex.util.CollectionsUT;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.ModuleItem;
import studio.magemonkey.divinity.modules.SocketItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.api.socketing.ModuleSocket;
import studio.magemonkey.divinity.modules.list.refine.RefineManager;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.DamageAttribute;
import studio.magemonkey.divinity.stats.items.attributes.DefenseAttribute;
import studio.magemonkey.divinity.stats.items.attributes.SocketAttribute;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class WorthManager implements Loadable {

    private final Divinity                                           plugin;
    private final Map<ItemStack, Double>                             worthCache = new HashMap<>();
    private       Map<String, Double>                                priceItemMaterial;
    private       Map<SimpleStat.Type, Double>                       priceItemStats;
    private       TreeMap<Integer, Double>                           priceRefineLvl;
    private       Map<String, Double>                                priceDefenseTypes;
    private       Map<String, Double>                                priceDamageTypes;
    private       Map<SocketAttribute.Type, Map<String, Double>>     priceSocketTypes;
    private       Map<String, TreeMap<Integer, Double>>              priceEnchants;
    private       Map<String, Map<String, TreeMap<Integer, Double>>> priceItemModule;

    public WorthManager(@NotNull Divinity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        JYML cfg;
        try {
            cfg = JYML.loadOrExtract(plugin, "worth.yml");
        } catch (InvalidConfigurationException e) {
            this.plugin.error("Failed to load worth config (worth.yml): Configuration error");
            e.printStackTrace();
            shutdown();
            return;
        }

        this.priceItemMaterial = new HashMap<>();
        this.priceItemStats = new HashMap<>();
        this.priceRefineLvl = new TreeMap<>();
        this.priceDefenseTypes = new HashMap<>();
        this.priceDamageTypes = new HashMap<>();
        this.priceSocketTypes = new HashMap<>();
        this.priceEnchants = new HashMap<>();
        this.priceItemModule = new HashMap<>();

        String path = "worth-calculator.";

        //double defPrice = 0D;
        for (SimpleStat.Type type : TypedStat.Type.values()) {
            cfg.addMissing(path + "by-item-stats." + type.name(), Rnd.get(1, 11));
        }
        for (DamageAttribute dmgAtt : ItemStats.getDamages()) {
            cfg.addMissing(path + "by-damage-types." + dmgAtt.getId(), Rnd.get(1, 11));
        }
        for (DefenseAttribute defAtt : ItemStats.getDefenses()) {
            cfg.addMissing(path + "by-defense-types." + defAtt.getId(), Rnd.get(1, 11));
        }
        for (SocketAttribute.Type type : SocketAttribute.Type.values()) {
            for (SocketAttribute socket : ItemStats.getSockets(type)) {
                cfg.addMissing(path + "by-socket-types." + type.name() + "." + socket.getId(), Rnd.get(100, 150));
            }
        }
        for (Enchantment en : Enchantment.values()) {
            for (int min = en.getStartLevel(); min < en.getMaxLevel(); min++) {
                cfg.addMissing(path + "by-enchant-level." + en.getKey().getKey() + "." + min, Rnd.get(30, 60) * min);
            }
        }
        for (IModule<?> mod : plugin.getModuleManager().getModules()) {
            if (!(mod instanceof QModuleDrop<?>)) continue;
            QModuleDrop<?> md = (QModuleDrop<?>) mod;

            for (ModuleItem item : md.getItems()) {
                if (item == null) continue;
                cfg.addMissing(path + "by-item-id-level." + md.getId() + "." + item.getId() + ".1",
                        Rnd.get(100, 200));
            }
        }


        for (String sMat : cfg.getSection(path + "by-item-material")) {
            Material material = Material.getMaterial(sMat.toUpperCase());
            if (material == null) {
                this.plugin.error("[Worth] Invalid material '" + sMat + "' !");
                continue;
            }
            double price = cfg.getDouble(path + "by-item-material." + sMat);
            if (price == 0) continue;
            this.priceItemMaterial.put(sMat.toUpperCase(), price);
        }

        for (String sId : cfg.getSection(path + "by-item-stats")) {
            SimpleStat.Type type = CollectionsUT.getEnum(sId, SimpleStat.Type.class);
            if (type == null) {
                this.plugin.error("[Worth] Invalid stat type: '" + sId + "' !");
                continue;
            }
            double price = cfg.getDouble(path + "by-item-stats." + sId);
            if (price == 0) continue;
            this.priceItemStats.put(type, price);
        }

        for (String sLvl : cfg.getSection(path + "by-refine-level")) {
            int lvl = StringUT.getInteger(sLvl, -1);
            if (lvl < 1) {
                continue;
            }
            double price = cfg.getDouble(path + "by-refine-level." + sLvl);
            if (price == 0) continue;
            this.priceRefineLvl.put(lvl, price);
        }

        for (String sId : cfg.getSection(path + "by-defense-types")) {
            DefenseAttribute defAtt = ItemStats.getDefenseById(sId);
            if (defAtt == null) {
                this.plugin.error("[Worth] Invalid Defense Attribute '" + sId + "' !");
                continue;
            }
            double price = cfg.getDouble(path + "by-defense-types." + sId);
            if (price == 0) continue;
            this.priceDefenseTypes.put(defAtt.getId(), price);
        }

        for (String sId : cfg.getSection(path + "by-damage-types")) {
            DamageAttribute defAtt = ItemStats.getDamageById(sId);
            if (defAtt == null) {
                this.plugin.error("[Worth] Invalid Damage Attribute '" + sId + "' !");
                continue;
            }
            double price = cfg.getDouble(path + "by-damage-types." + sId);
            if (price == 0) continue;
            this.priceDamageTypes.put(defAtt.getId(), price);
        }

        for (String sType : cfg.getSection(path + "by-socket-types")) {
            SocketAttribute.Type type = SocketAttribute.Type.getByName(sType);
            if (type == null) continue;

            Map<String, Double> itemMap = new HashMap<>();
            for (String sId : cfg.getSection(path + "by-socket-types." + sType)) {
                SocketAttribute socket = ItemStats.getSocket(type, sId);
                if (socket == null) {
                    this.plugin.error("[Worth] Invalid Socket Attribute: '" + sId + "' !");
                    continue;
                }
                double price = cfg.getDouble(path + "by-socket-types." + sType + "." + sId);
                if (price == 0) continue;
                itemMap.put(socket.getId(), price);
            }
            if (!itemMap.isEmpty()) {
                this.priceSocketTypes.put(type, itemMap);
            }
        }

        for (String sId : cfg.getSection(path + "by-enchant-level")) {
            try {
                Enchantment e = Enchantment.getByKey(NamespacedKey.minecraft(sId.toLowerCase()));
                if (e == null) {
                    this.plugin.error("[Worth] Invalid Enchantment '" + sId + "' !");
                    continue;
                }
            } catch (IllegalArgumentException ex) {
                this.plugin.error("[Worth] Invalid Enchantment '" + sId + "' !");
                continue;
            }

            TreeMap<Integer, Double> lvlMap = new TreeMap<>();
            for (String sLvl : cfg.getSection(path + "by-enchant-level." + sId)) {
                int lvl = StringUT.getInteger(sLvl, -1);
                if (lvl < 1) continue;

                double price = cfg.getDouble(path + "by-enchant-level." + sId + "." + sLvl);
                lvlMap.put(lvl, price);
            }
            if (!lvlMap.isEmpty()) {
                this.priceEnchants.put(sId.toLowerCase(), lvlMap);
            }
        }

        for (String mId : cfg.getSection(path + "by-item-id-level")) {
            IModule<?> mod = plugin.getModuleManager().getModule(mId);
            if (mod == null || !mod.isLoaded() || !(mod instanceof QModuleDrop<?>)) {
                this.plugin.error("[Worth] Invalid module provided: '" + mId + "' !");
                continue;
            }

            Map<String, TreeMap<Integer, Double>> itemMap = new HashMap<>();
            for (String itemId : cfg.getSection(path + "by-item-id-level." + mId)) {

                TreeMap<Integer, Double> lvlMap = new TreeMap<>();
                for (String sLvl : cfg.getSection(path + "by-item-id-level." + mId + "." + itemId)) {
                    int lvl = StringUT.getInteger(sLvl, -1);
                    if (lvl < 1) continue;

                    String path2 = path + "by-item-id-level." + mId + "." + itemId + "." + sLvl;
                    double price = cfg.getDouble(path2);
                    if (price == 0D) continue;

                    lvlMap.put(lvl, price);
                }
                if (!lvlMap.isEmpty()) {
                    itemMap.put(itemId.toLowerCase(), lvlMap);
                }
            }
            if (!itemMap.isEmpty()) {
                this.priceItemModule.put(mod.getId(), itemMap);
            }
        }

        cfg.saveChanges();
    }

    @Override
    public void shutdown() {
        if (this.priceItemMaterial != null) {
            this.priceItemMaterial.clear();
            this.priceItemMaterial = null;
        }
        if (this.priceItemStats != null) {
            this.priceItemStats.clear();
            this.priceItemStats = null;
        }
        if (this.priceRefineLvl != null) {
            this.priceRefineLvl.clear();
            this.priceRefineLvl = null;
        }
        if (this.priceDefenseTypes != null) {
            this.priceDefenseTypes.clear();
            this.priceDefenseTypes = null;
        }
        if (this.priceDamageTypes != null) {
            this.priceDamageTypes.clear();
            this.priceDamageTypes = null;
        }
        if (this.priceSocketTypes != null) {
            this.priceSocketTypes.clear();
            this.priceSocketTypes = null;
        }
        if (this.priceEnchants != null) {
            this.priceEnchants.clear();
            this.priceEnchants = null;
        }
        if (this.priceItemModule != null) {
            this.priceItemModule.clear();
            this.priceItemModule = null;
        }
    }

    private double getItemMaterialPrice(@NotNull ItemStack item) {
        return this.priceItemMaterial.getOrDefault(item.getType().name(), 0D);
    }

    private double getItemEnchantCost(@NotNull ItemStack item) {
        double cost = 0D;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasEnchants()) return cost;

        for (Entry<Enchantment, Integer> e : meta.getEnchants().entrySet()) {
            String enchKey = e.getKey().getKey().getKey();

            TreeMap<Integer, Double> itemMap = this.priceEnchants.get(enchKey);
            if (itemMap == null) continue;

            Map.Entry<Integer, Double> eItem = itemMap.floorEntry(e.getValue());
            if (eItem == null) continue;

            cost += eItem.getValue();
        }
        return cost;
    }

    private double getItemModulePrice(@NotNull ItemStack item) {
        double cost = 0D;

        QModuleDrop<?> md = ItemStats.getModule(item);
        if (md == null) return cost;

        // Get price for inserted sockets
        if (!md.isSocketable()) {
            for (IModule<?> iMod : plugin.getModuleManager().getModules()) {
                if (iMod instanceof ModuleSocket<?>) {
                    ModuleSocket<?> ms = (ModuleSocket<?>) iMod;
                    for (Map.Entry<? extends SocketItem, Integer> socket : ms.getItemSockets(item)) {
                        SocketItem sItem = socket.getKey();
                        if (sItem == null) continue;

                        int sLvl = socket.getValue();

                        ItemStack sStack = sItem.create(sLvl);
                        cost += this.getItemModulePrice(sStack);
                    }
                }
            }
        }

        String modId = md.getId();

        Map<String, TreeMap<Integer, Double>> itemMap = this.priceItemModule.get(modId);
        if (itemMap == null) return 0D;

        String itemId = ItemStats.getId(item);
        if (itemId == null) return 0D;

        TreeMap<Integer, Double> lvlMap = itemMap.get(itemId);
        if (lvlMap == null) return 0D;

        int itemLvl = ItemStats.getLevel(item);

        Map.Entry<Integer, Double> ePrice = lvlMap.floorEntry(itemLvl);
        if (ePrice == null) return 0D;

        cost += ePrice.getValue();

        return Math.max(0, cost);
    }

    private double getItemAttributesPrice(@NotNull ItemStack item) {
        double cost = 0D;

        for (Map.Entry<SimpleStat.Type, Double> e : this.priceItemStats.entrySet()) {
            cost += (e.getValue() * ItemStats.getStat(item, null, e.getKey()));
        }
        for (Map.Entry<String, Double> e : this.priceDamageTypes.entrySet()) {
            cost += (e.getValue() * ItemStats.getDamageMinOrMax(item, null, e.getKey(), 1));
        }
        for (Map.Entry<String, Double> e : this.priceDefenseTypes.entrySet()) {
            cost += (e.getValue() * ItemStats.getDefense(item, null, e.getKey()));
        }

        return cost;
    }

    private double getItemRefinePrice(@NotNull ItemStack item) {
        if (this.priceRefineLvl.isEmpty()) return 0D;

        RefineManager refine = plugin.getModuleCache().getRefineManager();
        if (refine == null) return 0D;

        int refLvl = refine.getRefineLevel(item);
        if (refLvl < 1) return 0D;

        Map.Entry<Integer, Double> e = this.priceRefineLvl.floorEntry(refLvl);
        if (e == null) return 0D;

        return e.getValue();
    }

    private double getItemSocketPrice(@NotNull ItemStack item) {
        double cost = 0D;

        for (Map.Entry<SocketAttribute.Type, Map<String, Double>> e : this.priceSocketTypes.entrySet()) {
            Map<String, Double> itemMap = e.getValue();

            for (Map.Entry<String, Double> eSocket : itemMap.entrySet()) {
                SocketAttribute socket = ItemStats.getSocket(e.getKey(), eSocket.getKey());
                if (socket == null) continue;

                int emptyCount = socket.getEmptyAmount(item);
                if (emptyCount == 0) continue;

                double socketCost = 0D;
                String socketId   = socket.getId();

                if (itemMap.containsKey(socketId)) {
                    socketCost = itemMap.get(socketId);
                }
                //else if (itemMap.containsKey(JStrings.DEFAULT)) {
                //	socketCost = itemMap.get(JStrings.DEFAULT);
                //}
                cost += (socketCost * emptyCount);
            }
        }

        return cost;
    }

    public double getItemWorth(@NotNull ItemStack item) {
        double cost = 0;

        if (worthCache.containsKey(item))
            return worthCache.get(item);

        if (!ItemUT.isAir(item)) {
            cost += this.getItemMaterialPrice(item);
            cost += this.getItemModulePrice(item);
            cost += this.getItemAttributesPrice(item);
            cost += this.getItemEnchantCost(item);
            cost += this.getItemRefinePrice(item);
            cost += this.getItemSocketPrice(item);


            cost *= (1D + ItemStats.getStat(item, null, TypedStat.Type.SALE_PRICE) / 100D);
            cost *= item.getAmount();
        }

        cost = Math.max(0, NumberUT.round(cost));
        //Let's cache so we don't have to run these calculations all the time.
        worthCache.put(item, cost);
        Bukkit.getScheduler().runTaskLater(Divinity.getInstance(), () -> worthCache.remove(item), 20 * 20L);

        return cost;
    }
}
