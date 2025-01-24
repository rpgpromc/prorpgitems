package studio.magemonkey.divinity.stats.items;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.api.meta.NBTAttribute;
import studio.magemonkey.codex.compat.VersionManager;
import studio.magemonkey.codex.core.Version;
import studio.magemonkey.codex.modules.IModule;
import studio.magemonkey.codex.util.DataUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.stats.items.api.DuplicableItemLoreStat;
import studio.magemonkey.divinity.stats.items.api.DynamicStat;
import studio.magemonkey.divinity.stats.items.api.ItemLoreStat;
import studio.magemonkey.divinity.stats.items.attributes.*;
import studio.magemonkey.divinity.stats.items.attributes.SocketAttribute.Type;
import studio.magemonkey.divinity.stats.items.attributes.api.SimpleStat;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;
import studio.magemonkey.divinity.stats.items.attributes.stats.DurabilityStat;
import studio.magemonkey.divinity.utils.ItemUtils;

import java.util.*;

public class ItemStats {

    private static final Map<String, DamageAttribute>            DAMAGES          = new LinkedHashMap<>();
    private static final Map<String, DefenseAttribute>           DEFENSES         = new LinkedHashMap<>();
    private static final Map<SimpleStat.Type, TypedStat>         STATS            = new HashMap<>();
    private static final Map<AmmoAttribute.Type, AmmoAttribute>  AMMO             = new HashMap<>();
    private static final Map<HandAttribute.Type, HandAttribute>  HANDS            = new HashMap<>();
    private static final Map<Type, Map<String, SocketAttribute>> SOCKETS          = new HashMap<>();
    private static final Map<String, ItemLoreStat<?>>            ATTRIBUTES       = new HashMap<>();
    private static final Map<String, DuplicableItemLoreStat<?>>  MULTI_ATTRIBUTES = new HashMap<>();
    private static final Set<DynamicStat>                        DYNAMIC_STATS    = new HashSet<>();
    private static final Divinity                                plugin           = Divinity.getInstance();
    private static final List<NamespacedKey>                     KEY_ID           =
            List.of(new NamespacedKey(plugin, ItemTags.TAG_ITEM_ID),
                    Objects.requireNonNull(NamespacedKey.fromString(
                            "prorpgitems:" + ItemTags.TAG_ITEM_ID.toLowerCase())),
                    Objects.requireNonNull(NamespacedKey.fromString(
                            "prorpgitems:qrpg_" + ItemTags.TAG_ITEM_ID.toLowerCase())),
                    Objects.requireNonNull(NamespacedKey.fromString(
                            "quantumrpg:qrpg_" + ItemTags.TAG_ITEM_ID.toLowerCase())));
    private static final List<NamespacedKey>                     KEY_MODULE       =
            List.of(new NamespacedKey(plugin, ItemTags.TAG_ITEM_MODULE),
                    Objects.requireNonNull(NamespacedKey.fromString(
                            "prorpgitems:" + ItemTags.TAG_ITEM_MODULE.toLowerCase())),
                    Objects.requireNonNull(NamespacedKey.fromString(
                            "prorpgitems:qrpg_" + ItemTags.TAG_ITEM_MODULE.toLowerCase())),
                    Objects.requireNonNull(NamespacedKey.fromString(
                            "quantumrpg:qrpg_" + ItemTags.TAG_ITEM_MODULE.toLowerCase())));
    private static final List<NamespacedKey>                     KEY_LEVEL        =
            List.of(new NamespacedKey(plugin, ItemTags.TAG_ITEM_LEVEL),
                    Objects.requireNonNull(NamespacedKey.fromString(
                            "prorpgitems:" + ItemTags.TAG_ITEM_LEVEL.toLowerCase())),
                    Objects.requireNonNull(NamespacedKey.fromString(
                            "prorpgitems:qrpg_" + ItemTags.TAG_ITEM_LEVEL.toLowerCase())),
                    Objects.requireNonNull(NamespacedKey.fromString(
                            "quantumrpg:qrpg_" + ItemTags.TAG_ITEM_LEVEL.toLowerCase())));
    private static final List<NamespacedKey>                     KEY_SOCKET       =
            List.of(new NamespacedKey(plugin, ItemTags.TAG_ITEM_SOCKET_RATE),
                    Objects.requireNonNull(NamespacedKey.fromString(
                            "prorpgitems:" + ItemTags.TAG_ITEM_SOCKET_RATE.toLowerCase())),
                    Objects.requireNonNull(NamespacedKey.fromString(
                            "prorpgitems:qrpg_" + ItemTags.TAG_ITEM_SOCKET_RATE.toLowerCase())),
                    Objects.requireNonNull(NamespacedKey.fromString(
                            "quantumrpg:qrpg_" + ItemTags.TAG_ITEM_SOCKET_RATE.toLowerCase())));
    private static       DamageAttribute                         DAMAGE_DEFAULT;
    private static       DefenseAttribute                        DEFENSE_DEFAULT;

    private static final double DEFAULT_ATTACK_SPEED = 4D;

    // TODO Register logs

    public static void clear() {
        DAMAGES.clear();
        DEFENSES.clear();
        STATS.clear();
        AMMO.clear();
        HANDS.clear();
        SOCKETS.clear();
        ATTRIBUTES.clear();
        MULTI_ATTRIBUTES.clear();
        DAMAGE_DEFAULT = null;
        DEFENSE_DEFAULT = null;
    }

    public static void registerDamage(@NotNull DamageAttribute dmg) {
        DAMAGES.put(dmg.getId(), dmg);
        if (DAMAGE_DEFAULT == null || dmg.getPriority() >= DAMAGE_DEFAULT.getPriority()) {
            DAMAGE_DEFAULT = dmg;
        }
        // Put default damage at the end of list every time when new damage is added.
        else {
            DAMAGES.remove(DAMAGE_DEFAULT.getId());
            DAMAGES.put(DAMAGE_DEFAULT.getId(), DAMAGE_DEFAULT);
        }
        ItemStats.updateDefenseByDefault();
    }

    public static void registerDefense(@NotNull DefenseAttribute def) {
        DEFENSES.put(def.getId(), def);
        ItemStats.updateDefenseByDefault();
    }

    public static void registerStat(@NotNull TypedStat stat) {
        if (stat.getCapability() == 0) return; // TODO Log

        STATS.put(stat.getType(), stat);
    }

    public static void registerAmmo(@NotNull AmmoAttribute ammo) {
        AMMO.put(ammo.getType(), ammo);
    }


    public static void registerHand(@NotNull HandAttribute hand) {
        HANDS.put(hand.getType(), hand);
    }

    public static void registerDynamicStat(@NotNull DynamicStat stat) {
        DYNAMIC_STATS.add(stat);
    }

    public static Collection<DynamicStat> getDynamicStats() {
        return Collections.unmodifiableSet(DYNAMIC_STATS);
    }

    private static void updateDefenseByDefault() {
        if (DAMAGES.isEmpty()) return;

        for (DamageAttribute dmg : ItemStats.getDamages()) {
            Optional<DefenseAttribute> opt = ItemStats.getDefenses()
                    .stream()
                    .filter(def -> def.isBlockable(dmg))
                    .sorted((def1, def2) -> def2.getPriority() - def1.getPriority())
                    .findFirst();

            if (opt.isPresent()) {
                dmg.setAttachedDefense(opt.get());
                //System.out.println("Attached def: " + opt.get().getId() + " to " + dmg.getId() + " dmg");
            }
        }

        DEFENSE_DEFAULT = ItemStats.getDamageByDefault().getAttachedDefense();
    }

    @NotNull
    public static Collection<DamageAttribute> getDamages() {
        return DAMAGES.values();
    }

    @Nullable
    public static DamageAttribute getDamageById(@NotNull String id) {
        return DAMAGES.get(id.toLowerCase());
    }

    @Nullable
    public static DamageAttribute getDamageByCause(@NotNull DamageCause cause) {
        Optional<DamageAttribute> opt =
                ItemStats.getDamages().stream().filter(dmg -> dmg.isAttached(cause)).sorted((dmg1, dmg2) -> {
                    return dmg2.getPriority() - dmg1.getPriority();
                }).findFirst();

        return opt.isPresent() ? opt.get() : null;
    }

    @NotNull
    public static DamageAttribute getDamageByDefault() {
        return DAMAGE_DEFAULT;
    }

    public static boolean hasDamage(@NotNull ItemStack item, @Nullable Player player) {
        return ItemStats.getDamages().stream().anyMatch(dmg -> ItemStats.hasDamage(item, player, dmg));
    }

    public static boolean hasDamage(@NotNull ItemStack item, @Nullable Player player, @NotNull String id) {
        DamageAttribute dmgType = getDamageById(id);
        if (dmgType == null) return false;

        return ItemStats.hasDamage(item, player, dmgType);
    }

    public static boolean hasDamage(@NotNull ItemStack item,
                                    @Nullable Player player,
                                    @NotNull DamageAttribute dmgType) {
        return dmgType.getTotal(item, player)[1] > 0;
    }

    public static double getDamageMinOrMax(@NotNull ItemStack item,
                                           @Nullable Player player,
                                           @NotNull String id,
                                           int index) {
        DamageAttribute dmgType = getDamageById(id);
        if (dmgType == null) return 0D;

        return dmgType.getTotal(item, player)[index];
    }

    @NotNull
    public static Collection<DefenseAttribute> getDefenses() {
        return DEFENSES.values();
    }

    @Nullable
    public static DefenseAttribute getDefenseById(@NotNull String id) {
        return DEFENSES.get(id.toLowerCase());
    }

    public static boolean hasDefense(@NotNull ItemStack item, @Nullable Player player, @NotNull String id) {
        DefenseAttribute defType = getDefenseById(id);
        if (defType == null) return false;

        return ItemStats.hasDefense(item, player, defType);
    }

    public static boolean hasDefense(@NotNull ItemStack item,
                                     @Nullable Player player,
                                     @NotNull DefenseAttribute defType) {
        return defType.getTotal(item, player) != 0;
    }

    @Nullable
    public static DefenseAttribute getDefenseByDefault() {
        return DEFENSE_DEFAULT;
    }

    public static double getDefense(@NotNull ItemStack item, @Nullable Player player, @NotNull String id) {
        DefenseAttribute defType = getDefenseById(id);
        if (defType == null) return 0D;

        return defType.getTotal(item, player);
    }

    @NotNull
    public static Collection<TypedStat> getStats() {
        return STATS.values();
    }

    @Nullable
    public static TypedStat getStat(@NotNull SimpleStat.Type type) {
        return STATS.get(type);
    }

    public static double getStat(@NotNull ItemStack item, @Nullable Player player, @NotNull SimpleStat.Type type) {
        return getStat(item, player, type, 0D);
    }

    public static double getStat(@NotNull ItemStack item,
                                 @Nullable Player player,
                                 @NotNull SimpleStat.Type type,
                                 double def) {
        TypedStat stat = getStat(type);
        if (stat instanceof SimpleStat) {
            return ((SimpleStat) stat).getTotal(item, player, def);
        }

        if (stat instanceof DurabilityStat) {
            double[] arr = ((DurabilityStat) stat).getRaw(item);
            return arr == null ? def : arr[0];
        }

        return def;
    }

    @SuppressWarnings({"unchecked"})
    @Nullable
    public static <T extends TypedStat> T getStat(@NotNull Class<T> clazz) {
        for (TypedStat stat : ItemStats.getStats()) {
            Class<? extends TypedStat> clazz2 = stat.getClass();
            if (clazz.isAssignableFrom(clazz2)) {
                return (T) stat;
            }
        }
        return null;
    }

    public static boolean hasStat(@NotNull ItemStack item, @Nullable Player player, @NotNull SimpleStat.Type type) {
        TypedStat stat = ItemStats.getStat(type);
        if (stat == null) return false;

        if (stat instanceof SimpleStat) {
            SimpleStat rs = (SimpleStat) stat;
            double     d  = rs.getTotal(item, player);
            return d != 0D;
        }
        if (stat instanceof DurabilityStat) {
            DurabilityStat rs  = (DurabilityStat) stat;
            double[]       arr = rs.getRaw(item);
            return arr != null && arr.length == 2;// && arr[0] != 0D;
        }

        return false;
    }

    // ----------------------------------------------------------------- //

    public static void updateVanillaAttributes(@NotNull ItemStack item, @Nullable Player player) {
        addAttribute(item, player, NBTAttribute.MAX_HEALTH, getStat(item, player, TypedStat.Type.MAX_HEALTH));
        addAttribute(item, player, NBTAttribute.MOVEMENT_SPEED, getStat(item, player, TypedStat.Type.MOVEMENT_SPEED));
        addAttribute(item, player, NBTAttribute.ATTACK_SPEED, getStat(item, player, TypedStat.Type.ATTACK_SPEED));

        double vanilla = DamageAttribute.getVanillaDamage(item);
        if (vanilla > 1) addAttribute(item, player, NBTAttribute.ATTACK_DAMAGE, vanilla);
        if (ItemUtils.isArmor(item)) {
            addAttribute(item, player, NBTAttribute.ARMOR, DefenseAttribute.getVanillaArmor(item));
            double toughness = getStat(item, player, TypedStat.Type.ARMOR_TOUGHNESS);
            addAttribute(item,
                    player,
                    NBTAttribute.ARMOR_TOUGHNESS,
                    toughness == 0 ? DefenseAttribute.getVanillaToughness(item) : toughness);
        }
        ItemMeta im = item.getItemMeta();

        // For 1.20.4+, the HIDE_ATTRIBUTES flag doesn't work unless an attribute has been added that's not the default.
        // Note: This only applies to Paper and its forks.
        if (Version.CURRENT.isAtLeast(Version.V1_20_R4)) {
            Attribute moveSpeed = VersionManager.getNms().getAttribute("MOVEMENT_SPEED");
            if (im.getAttributeModifiers(VersionManager.getNms().getAttribute("MOVEMENT_SPEED")) == null) {
                //noinspection RedundantCast
                im.addAttributeModifier(moveSpeed,
                        new AttributeModifier(((Keyed) moveSpeed).getKey().getKey(), 0, Operation.ADD_NUMBER));
            }
        }

        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(im);
    }

    private static void addAttribute(@NotNull ItemStack item,
                                     @Nullable Player player,
                                     @NotNull NBTAttribute att,
                                     double value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Bows not needed damage tag at all.
        if (att == NBTAttribute.ATTACK_DAMAGE && ItemUtils.isBow(item)) {
            return;
        }

        // Do not add attribute with zero value
        // except the weapons, they will get vanilla speed.
        if (value == 0) {
            boolean isWeapon      = ItemUtils.isWeapon(item);
            boolean isAttackSpeed = att == NBTAttribute.ATTACK_SPEED;

            if (!isAttackSpeed && isWeapon) {
                return;
            }

            double defaultAttackSpeed = SimpleStat.getDefaultAttackSpeed(item, DEFAULT_ATTACK_SPEED);
            double baseAttackSpeed    = getStat(item, player, TypedStat.Type.BASE_ATTACK_SPEED, 4);
            if (baseAttackSpeed == defaultAttackSpeed) {
                return;
            }
        }

        // Fine values
        if (att == NBTAttribute.MOVEMENT_SPEED) {
            value = 0.1 * (1D + value / 100D) - 0.1;
        } else if (att == NBTAttribute.ATTACK_SPEED) {
            value /= 100D;

            // Attack speed the number of full-strength attacks per second.
            // A base value of 4.0 represents 4 attacks per second.

            boolean isArmor = ItemUtils.isArmor(item);
            double weaponModifier =
                    SimpleStat.getDefaultAttackSpeed(item, DEFAULT_ATTACK_SPEED) + DEFAULT_ATTACK_SPEED;
            double baseSpeedModifier = getStat(item, player, TypedStat.Type.BASE_ATTACK_SPEED, weaponModifier);
            double baseSpeed         = baseSpeedModifier - DEFAULT_ATTACK_SPEED;
            if (isArmor) {
                baseSpeed = baseSpeedModifier;
            }

            value = baseSpeed * (1 + value);
        }

        for (EquipmentSlot slot : ItemUtils.getItemSlots(item)) {
            if (slot == EquipmentSlot.OFF_HAND && (att == NBTAttribute.ATTACK_DAMAGE
                    || att == NBTAttribute.ATTACK_SPEED)) continue;
            if (slot != EquipmentSlot.HAND && !ItemUtils.isArmor(item) && att == NBTAttribute.ATTACK_SPEED) continue;

            AttributeModifier am =
                    new AttributeModifier(att.getUUID(slot), att.getNmsName(), value, Operation.ADD_NUMBER, slot);

            meta.removeAttributeModifier(att.getAttribute(), am); // Avoid dupe and error
            meta.addAttributeModifier(att.getAttribute(), am);
        }
        item.setItemMeta(meta);
    }

    @NotNull
    public static Collection<AmmoAttribute> getAmmos() {
        return AMMO.values();
    }

    @Nullable
    public static AmmoAttribute getAmmo(@NotNull AmmoAttribute.Type type) {
        return AMMO.get(type);
    }

    @Nullable
    public static AmmoAttribute getAmmo(@NotNull ItemStack item) {
        for (AmmoAttribute ammo : getAmmos()) {
            String value = ammo.getRaw(item);
            if (value != null) {
                return ammo;
            }
        }
        return null;
    }

    // ================================================================================== //


    @NotNull
    public static Collection<HandAttribute> getHands() {
        return HANDS.values();
    }

    @Nullable
    public static HandAttribute getHand(@NotNull HandAttribute.Type type) {
        return HANDS.get(type);
    }

    @Nullable
    public static HandAttribute getHand(@NotNull ItemStack item) {
        for (HandAttribute hand : getHands()) {
            String value = hand.getRaw(item);
            if (value != null) {
                return hand;
            }
        }
        return null;
    }

    public static void registerAttribute(@NotNull ItemLoreStat<?> att) {
        ATTRIBUTES.put(att.getId(), att);
    }

    @NotNull
    public static Collection<ItemLoreStat<?>> getAttributes() {
        return ATTRIBUTES.values();
    }

    @Nullable
    public static ItemLoreStat<?> getAttribute(@NotNull String id) {
        return ATTRIBUTES.get(id.toLowerCase());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    public static <T extends ItemLoreStat<?>> T getAttribute(@NotNull Class<T> clazz) {
        for (ItemLoreStat<?> stat : ItemStats.getAttributes()) {
            Class<? extends ItemLoreStat> clazz2 = stat.getClass();
            if (clazz.isAssignableFrom(clazz2)) {
                return (T) stat;
            }
        }
        return null;
    }

    // ====================================================== //

    public static void registerSocket(@NotNull SocketAttribute socket) {
        Type                         type = socket.getType();
        Map<String, SocketAttribute> map  = SOCKETS.get(type);
        if (map == null) map = new HashMap<>();

        map.put(socket.getId(), socket);
        SOCKETS.put(type, map);
    }

    @Nullable
    public static SocketAttribute getSocket(@NotNull Type type, @NotNull String id) {
        Map<String, SocketAttribute> map = SOCKETS.get(type);
        if (map == null) return null;

        return map.get(id.toLowerCase());
    }

    @NotNull
    public static Collection<SocketAttribute> getSockets(@NotNull Type type) {
        Map<String, SocketAttribute> map = SOCKETS.get(type);
        if (map == null) return Collections.emptySet();

        return map.values();
    }

    public static void setId(@NotNull ItemStack item, @NotNull String id) {
        DataUT.setData(item, KEY_ID.get(0), id);
    }

    @Nullable
    public static String getId(@NotNull ItemStack item) {
        for (NamespacedKey key : KEY_ID) {
            String data = DataUT.getStringData(item, key);
            if (data != null) return data;
        }
        return null;
    }

    public static void setLevel(@NotNull ItemStack item, int lvl) {
        if (lvl < 1) {
            for (NamespacedKey key : KEY_LEVEL) {
                DataUT.removeData(item, key);
            }
            return;
        }
        DataUT.setData(item, KEY_LEVEL.get(0), lvl);
    }

    public static int getLevel(@NotNull ItemStack item) {
        for (NamespacedKey key : KEY_LEVEL) {
            int data = DataUT.getIntData(item, key);
            if (data != 0) return data;
        }
        return 0;
    }

    // ======================================================== //

    public static void setModule(@NotNull ItemStack item, @NotNull String mod) {
        DataUT.setData(item, KEY_MODULE.get(0), mod);
    }

    @Nullable
    public static QModuleDrop<?> getModule(@NotNull ItemStack item) {
        String data = null;
        for (NamespacedKey key : KEY_MODULE) {
            data = DataUT.getStringData(item, key);
            if (data != null) break;
        }
        if (data == null) return null;

        IModule<?> mod = plugin.getModuleManager().getModule(data);
        if (mod instanceof QModuleDrop<?>) {
            return (QModuleDrop<?>) mod;
        }
        return null;
    }

    public static void setSocketRate(@NotNull ItemStack item, int rate) {
        DataUT.setData(item, KEY_SOCKET.get(0), rate);
    }

    public static int getSocketRate(@NotNull ItemStack item) {
        for (NamespacedKey key : KEY_SOCKET) {
            int data = DataUT.getIntData(item, key);
            if (data != 0) return data;
        }
        return 0;
    }
}
