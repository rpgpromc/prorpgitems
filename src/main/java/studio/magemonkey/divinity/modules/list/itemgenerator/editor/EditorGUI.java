package studio.magemonkey.divinity.modules.list.itemgenerator.editor;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import studio.magemonkey.codex.core.Version;
import studio.magemonkey.codex.manager.api.menu.Slot;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.bonuses.MainBonusesGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.enchantments.EnchantmentsGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.materials.MainMaterialsGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.requirements.MainRequirementsGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.skills.MainSkillsGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.sockets.MainSocketsGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.stats.MainStatsGUI;
import studio.magemonkey.divinity.modules.list.itemgenerator.editor.trimmings.TrimmingListGUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EditorGUI extends AbstractEditorGUI {

    public EditorGUI(Player player, ItemGeneratorReference itemGenerator) {
        super(player, 5, "Editor", itemGenerator);
    }

    @Override
    public void setContents() {
        setSlot(0, new Slot(createItem(Material.NAME_TAG,
                "&eName format", StringUT.replace(CURRENT_PLACEHOLDER,
                        StringUT.wrap(itemGenerator.getHandle().getName().substring("§r§f".length()), 30),
                        "&bCurrent: &a%current%",
                        "&6Left-Click: &eSet",
                        "&6Right-Click: &eSet to default value"))) {
            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.NAME.getTitle(),
                        itemGenerator.getHandle().getName().substring("§r§f".length()).replace('§', '&'),
                        s -> {
                            itemGenerator.getConfig().set(ItemType.NAME.getPath(), s);
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.NAME.getPath());
                saveAndReopen();
            }
        });
        setSlot(1, new Slot(createItem(Material.IRON_INGOT,
                "&eMaterials",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new MainMaterialsGUI(player, itemGenerator));
            }
        });
        setSlot(2, new Slot(createItem(Material.WRITABLE_BOOK,
                "&eLore format",
                StringUT.replace(CURRENT_PLACEHOLDER, itemGenerator.getConfig().getStringList(ItemType.LORE.getPath()),
                        "&bCurrent:",
                        "&a----------",
                        "&f%current%",
                        "&a----------",
                        "&6Left-Click: &eModify",
                        "&6Right-Click: &eSet to default value"))) {
            @Override
            public void onLeftClick() {
                openSubMenu(new LoreGUI(player,
                        "[&d" + itemGenerator.getId() + "&r] editor/" + ItemType.LORE.getTitle(),
                        itemGenerator,
                        ItemType.LORE.getPath()));
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.LORE.getPath());
                saveAndReopen();
            }
        });
        setSlot(3, new Slot(createItem(Material.END_CRYSTAL,
                "&eCustom Model Data",
                "&bCurrent: &a" + this.itemGenerator.getConfig().getInt(ItemType.MODEL_DATA.getPath(), 0),
                "&6Left-Click: &eSet",
                "&6Shift-Left-Click: &eDecrease",
                "&6Shift-Right-Click: &eIncrease",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onShiftLeftClick() {
                itemGenerator.getConfig()
                        .set(ItemType.MODEL_DATA.getPath(),
                                Math.max(0, itemGenerator.getConfig().getInt(ItemType.MODEL_DATA.getPath(), 0) - 1));
                saveAndReopen();
            }

            @Override
            public void onShiftRightClick() {
                itemGenerator.getConfig()
                        .set(ItemType.MODEL_DATA.getPath(),
                                Math.max(0, itemGenerator.getConfig().getInt(ItemType.MODEL_DATA.getPath(), 0) + 1));
                saveAndReopen();
            }

            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.MODEL_DATA.getTitle(),
                        String.valueOf(itemGenerator.getConfig().getInt(ItemType.MODEL_DATA.getPath(), 0)),
                        s -> {
                            int cmd = Integer.parseInt(s);
                            if (cmd >= 0) {
                                itemGenerator.getConfig().set(ItemType.MODEL_DATA.getPath(), cmd);
                            } else {
                                throw new IllegalArgumentException();
                            }
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                itemGenerator.getConfig().remove(ItemType.MODEL_DATA.getPath());
                saveAndReopen();
            }
        });
        int[]     color = itemGenerator.getHandle().getColor();
        ItemStack itemStack;
        if (color == null) {
            itemStack = createItem(Material.GLASS_BOTTLE,
                    "&eColor",
                    "&bCurrent: &anull",
                    "&6Left-Click: &eSet",
                    "&6Drop: &eRemove");
        } else {
            itemStack = createItem(Material.POTION,
                    "&eColor",
                    "&bCurrent: &a" + color[0] + "," + color[1] + "," + color[2],
                    "&6Left-Click: &eSet",
                    "&6Right-Click: &eRemove");
            ItemMeta meta = itemStack.getItemMeta();
            if (meta instanceof PotionMeta) {
                int r = color[0] >= 0 ? color[0] : Rnd.get(255);
                int g = color[1] >= 0 ? color[1] : Rnd.get(255);
                int b = color[2] >= 0 ? color[2] : Rnd.get(255);
                ((PotionMeta) meta).setColor(Color.fromRGB(r, g, b));
                itemStack.setItemMeta(meta);
            }
        }
        setSlot(4, new Slot(itemStack) {
            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.COLOR.getTitle(),
                        color == null ? "null" : color[0] + "," + color[1] + "," + color[2],
                        s -> {
                            String[] splitString = s.split(",");
                            if (splitString.length != 3) {
                                throw new IllegalArgumentException();
                            }
                            int[] rgb = new int[3];
                            rgb[0] = Integer.parseInt(splitString[0].strip());
                            rgb[1] = Integer.parseInt(splitString[1].strip());
                            rgb[2] = Integer.parseInt(splitString[2].strip());
                            itemGenerator.getConfig()
                                    .set(ItemType.COLOR.getPath(), rgb[0] + "," + rgb[1] + "," + rgb[2]);
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                itemGenerator.getConfig().remove(ItemType.COLOR.getPath());
                saveAndReopen();
            }
        });
        setSlot(5, new Slot(createItem(Material.ANVIL,
                "&eDurability",
                "&bCurrent: &a" + this.itemGenerator.getConfig().getInt(ItemType.DURABILITY.getPath(), 0),
                "&6Left-Click: &eSet",
                "&6Shift-Left-Click: &eDecrease",
                "&6Shift-Right-Click: &eIncrease",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onShiftLeftClick() {
                itemGenerator.getConfig()
                        .set(ItemType.DURABILITY.getPath(),
                                Math.max(0, itemGenerator.getConfig().getInt(ItemType.DURABILITY.getPath(), 0) - 1));
                saveAndReopen();
            }

            @Override
            public void onShiftRightClick() {
                itemGenerator.getConfig()
                        .set(ItemType.DURABILITY.getPath(),
                                Math.max(0, itemGenerator.getConfig().getInt(ItemType.DURABILITY.getPath(), 0) + 1));
                saveAndReopen();
            }

            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.DURABILITY.getTitle(),
                        String.valueOf(itemGenerator.getConfig().getInt(ItemType.DURABILITY.getPath(), 0)),
                        s -> {
                            int durability = Integer.parseInt(s);
                            if (durability >= 0) {
                                itemGenerator.getConfig().set(ItemType.DURABILITY.getPath(), durability);
                            } else {
                                throw new IllegalArgumentException();
                            }
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                itemGenerator.getConfig().remove(ItemType.DURABILITY.getPath());
                saveAndReopen();
            }
        });
        itemStack = createItem(Material.ELYTRA,
                "&eUnbreakable",
                "&bCurrent: &a" + itemGenerator.getHandle().isUnbreakable(),
                "&6Left-Click: &eToggle",
                "&6Right-Click: &eSet to default value");
        if (!itemGenerator.getHandle().isUnbreakable()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta instanceof Damageable) {
                ((Damageable) itemMeta).setDamage(432);
                itemStack.setItemMeta(itemMeta);
            }
        }
        setSlot(6, new Slot(itemStack) {
            @Override
            public void onLeftClick() {
                itemGenerator.getConfig()
                        .set(ItemType.UNBREAKABLE.getPath(), !itemGenerator.getHandle().isUnbreakable());
                saveAndReopen();
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.UNBREAKABLE.getPath());
                saveAndReopen();
            }
        });
        setSlot(7, new Slot(createItem(Material.BROWN_MUSHROOM,
                "&ePrefix Chance", List.of(
                        "&bCurrent: &a" + itemGenerator.getHandle().getPrefixChance(),
                        "&6Left-Click: &eSet",
                        "&6Right-Click: &eSet to default value"))) {
            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.PREFIX_CHANCE.getTitle(),
                        String.valueOf(itemGenerator.getHandle().getPrefixChance()),
                        s -> {
                            itemGenerator.getConfig().set(ItemType.PREFIX_CHANCE.getPath(), Double.parseDouble(s));
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.PREFIX_CHANCE.getPath());
                saveAndReopen();
            }
        });
        setSlot(8, new Slot(createItem(Material.RED_MUSHROOM,
                "&eSuffix Chance",
                "&bCurrent: &a" + itemGenerator.getHandle().getSuffixChance(),
                "&6Left-Click: &eSet",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.SUFFIX_CHANCE.getTitle(),
                        String.valueOf(itemGenerator.getHandle().getSuffixChance()),
                        s -> {
                            itemGenerator.getConfig().set(ItemType.SUFFIX_CHANCE.getPath(), Double.parseDouble(s));
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.SUFFIX_CHANCE.getPath());
                saveAndReopen();
            }
        });
        if (Version.CURRENT.isHigher(Version.V1_19_R3)) {
            List<String> lore = new ArrayList<>();
            setSlot(11, new Slot(createItem(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
                    "&eArmor Trimmings", StringUT.replace(CURRENT_PLACEHOLDER, lore,
                            "&bCurrent:",
                            "&a%current%",
                            "&6Left-Click: &eModify"))) {
                @Override
                public void onLeftClick() {
                    openSubMenu(new TrimmingListGUI(player, itemGenerator));
                }
            });
        }
        List<String> lore = new ArrayList<>();
        for (ItemFlag flag : itemGenerator.getHandle().getFlags()) {
            lore.add("- " + flag.name().toLowerCase());
        }
        setSlot(12, new Slot(createItem(Material.OAK_SIGN,
                "&eItemFlags", StringUT.replace(CURRENT_PLACEHOLDER, lore,
                        "&bCurrent:",
                        "&a%current%",
                        "&6Left-Click: &eModify",
                        "&6Right-Click: &eSet to default value"))) {
            @Override
            public void onLeftClick() {
                openSubMenu(new ItemFlagsGUI(player, itemGenerator));
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.ITEM_FLAGS.getPath());
                saveAndReopen();
            }
        });
        boolean enchanted = this.itemGenerator.getConfig().getBoolean(ItemType.ENCHANTED.getPath(), false);
        setSlot(13, new Slot(createItem(enchanted ? Material.ENCHANTED_BOOK : Material.BOOK,
                "&eEnchanted",
                "&bCurrent: &a" + enchanted,
                "&6Left-Click: &eToggle",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onLeftClick() {
                itemGenerator.getConfig()
                        .set(ItemType.ENCHANTED.getPath(),
                                !itemGenerator.getConfig().getBoolean(ItemType.ENCHANTED.getPath()));
                saveAndReopen();
            }

            @Override
            public void onRightClick() {
                itemGenerator.getConfig().remove(ItemType.ENCHANTED.getPath());
                saveAndReopen();
            }
        });
        String hash = this.itemGenerator.getConfig().getString(ItemType.SKULL_HASH.getPath());
        itemStack = createItem(Material.PLAYER_HEAD,
                "&eSkull Hash", StringUT.replace(CURRENT_PLACEHOLDER, StringUT.wrap(hash == null ? "\"\"" : hash, 30),
                        "&bCurrent: &a%current%",
                        "&6Left-Click: &eSet",
                        "&6Right-Click: &eSet to default value"));
        if (hash != null) {
            ItemUT.addSkullTexture(itemStack, hash, this.itemGenerator.getId());
        }
        setSlot(14, new Slot(itemStack) {
            @Override
            public void onLeftClick() {
                String current = itemGenerator.getConfig().getString(ItemType.SKULL_HASH.getPath());
                sendSetMessage(ItemType.SKULL_HASH.getTitle(),
                        current == null ? "" : current,
                        s -> {
                            itemGenerator.getConfig().set(ItemType.SKULL_HASH.getPath(), s);
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                itemGenerator.getConfig().remove(ItemType.SKULL_HASH.getPath());
                saveAndReopen();
            }
        });
        setSlot(15, new Slot(createItem(Material.GOLD_INGOT,
                "&eStat Bonuses",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new MainBonusesGUI(player, itemGenerator));
            }
        });
        setSlot(20, new Slot(createItem(Material.EXPERIENCE_BOTTLE,
                "&eMinimum Level",
                "&bCurrent: &a" + itemGenerator.getConfig().getInt(ItemType.MIN_LEVEL.getPath()),
                "&6Left-Click: &eSet",
                "&6Shift-Left-Click: &eDecrease",
                "&6Shift-Right-Click: &eIncrease",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onShiftLeftClick() {
                itemGenerator.getConfig()
                        .set(ItemType.MIN_LEVEL.getPath(), Math.max(0, itemGenerator.getHandle().getMinLevel() - 1));
                saveAndReopen();
            }

            @Override
            public void onShiftRightClick() {
                itemGenerator.getConfig()
                        .set(ItemType.MIN_LEVEL.getPath(), Math.max(0, itemGenerator.getHandle().getMinLevel() + 1));
                saveAndReopen();
            }

            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.MIN_LEVEL.getTitle(),
                        String.valueOf(itemGenerator.getHandle().getMinLevel()),
                        s -> {
                            int level = Integer.parseInt(s);
                            if (level >= 0) {
                                itemGenerator.getConfig().set(ItemType.MIN_LEVEL.getPath(), level);
                            } else {
                                throw new IllegalArgumentException();
                            }
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.MIN_LEVEL.getPath());
                saveAndReopen();
            }
        });
        setSlot(21, new Slot(createItem(Material.EXPERIENCE_BOTTLE,
                "&eMaximum Level",
                "&bCurrent: &a" + itemGenerator.getConfig().getInt(ItemType.MAX_LEVEL.getPath()),
                "&6Left-Click: &eSet",
                "&6Shift-Left-Click: &eDecrease",
                "&6Shift-Right-Click: &eIncrease",
                "&6Right-Click: &eSet to default value")) {
            @Override
            public void onShiftLeftClick() {
                itemGenerator.getConfig()
                        .set(ItemType.MAX_LEVEL.getPath(), itemGenerator.getHandle().getMaxLevel() - 1);
                saveAndReopen();
            }

            @Override
            public void onShiftRightClick() {
                itemGenerator.getConfig()
                        .set(ItemType.MAX_LEVEL.getPath(), itemGenerator.getHandle().getMaxLevel() + 1);
                saveAndReopen();
            }

            @Override
            public void onLeftClick() {
                sendSetMessage(ItemType.MAX_LEVEL.getTitle(),
                        String.valueOf(itemGenerator.getHandle().getMaxLevel()),
                        s -> {
                            int level = Integer.parseInt(s);
                            if (level >= 0) {
                                itemGenerator.getConfig().set(ItemType.MAX_LEVEL.getPath(), level);
                            } else {
                                throw new IllegalArgumentException();
                            }
                            saveAndReopen();
                        });
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.MAX_LEVEL.getPath());
                saveAndReopen();
            }
        });
        setSlot(22, new Slot(createItem(Material.DIAMOND,
                "&eTier",
                StringUT.replace(CURRENT_PLACEHOLDER, StringUT.wrap(itemGenerator.getHandle().getTier().getName(), 12),
                        "&bCurrent: &a%current%",
                        "&6Left-Click: &eSet",
                        "&6Right-Click: &eSet to default value"))) {
            @Override
            public void onLeftClick() {
                openSubMenu(new TierGUI(player, itemGenerator));
            }

            @Override
            public void onRightClick() {
                setDefault(ItemType.TIER.getPath());
                saveAndReopen();
            }
        });
        lore = new ArrayList<>();
        ConfigurationSection ammoSection =
                this.itemGenerator.getConfig().getConfigurationSection(ItemType.AMMO_TYPES.getPath());
        if (ammoSection != null) {
            for (String ammoType : ammoSection.getKeys(false)) {
                lore.add("&a " + ammoType + ": &f" + ammoSection.getDouble(ammoType));
            }
        }
        setSlot(23, new Slot(createItem(Material.ARROW,
                "&eAmmo Types", StringUT.replace(CURRENT_PLACEHOLDER, lore,
                        "&bCurrent:",
                        "%current%",
                        "&6Left-Click: &eModify"))) {
            @Override
            public void onLeftClick() {
                openSubMenu(new AmmoTypesGUI(player, itemGenerator));
            }
        });
        lore = new ArrayList<>();
        ConfigurationSection handSection =
                this.itemGenerator.getConfig().getConfigurationSection(ItemType.HAND_TYPES.getPath());
        if (handSection != null) {
            for (String handType : handSection.getKeys(false)) {
                lore.add("&a " + handType + ": &f" + handSection.getDouble(handType));
            }
        }
        setSlot(24, new Slot(createItem(Material.STICK,
                "&eHand Types", StringUT.replace(CURRENT_PLACEHOLDER, lore,
                        "&bCurrent:",
                        "%current%",
                        "&6Left-Click: &eModify"))) {
            @Override
            public void onLeftClick() {
                openSubMenu(new HandTypesGUI(player, itemGenerator));
            }
        });
        setSlot(27, new Slot(createItem(Material.IRON_SWORD,
                "&eDamage Types",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new MainStatsGUI(player, itemGenerator, ItemType.DAMAGE_TYPES));
            }
        });
        setSlot(28, new Slot(createItem(Material.IRON_CHESTPLATE,
                "&eDefense Types",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new MainStatsGUI(player, itemGenerator, ItemType.DEFENSE_TYPES));
            }
        });
        setSlot(29, new Slot(createItem(Material.PAPER,
                "&eItem Stats",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new MainStatsGUI(player, itemGenerator, ItemType.ITEM_STATS));
            }
        });
        setSlot(30, new Slot(createItem(Material.BOOK,
                "&eFabled Attributes",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new MainStatsGUI(player, itemGenerator, ItemType.FABLED_ATTRIBUTES));
            }
        });
        setSlot(31, new Slot(createItem(Material.EMERALD,
                "&eSockets",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new MainSocketsGUI(player, itemGenerator));
            }
        });
        setSlot(32, new Slot(createItem(Material.REDSTONE,
                "&eRequirements",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new MainRequirementsGUI(player, itemGenerator));
            }
        });
        setSlot(33, new Slot(createItem(Material.ENCHANTED_BOOK,
                "&eEnchantments",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new EnchantmentsGUI(player, itemGenerator));
            }
        });
        setSlot(34, new Slot(createItem(Material.FIRE_CHARGE,
                "&eSkills",
                "&6Left-Click: &eModify")) {
            @Override
            public void onLeftClick() {
                openSubMenu(new MainSkillsGUI(player, itemGenerator));
            }
        });
        lore = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : UsesByLevelGUI.getUsesByLevel(this.itemGenerator.getConfig())
                .entrySet()) {
            lore.add("&a " + entry.getKey() + ": &f" + entry.getValue());
        }
        setSlot(35, new Slot(createItem(Material.CAULDRON,
                "&eUses by level", StringUT.replace(CURRENT_PLACEHOLDER, lore,
                        "&bCurrent:",
                        "&a%current%",
                        "&6Left-Click: &eModify"))) {
            @Override
            public void onLeftClick() {
                openSubMenu(new UsesByLevelGUI(player, itemGenerator));
            }
        });
        setSlot(40, new Slot(this.itemGenerator.getHandle().create(-1, -1, null)) {
            @Override
            public void onLeftClick() {
                saveAndReopen();
            }
        });
    }

    public enum ItemType {
        NAME("name"),
        MATERIALS("generator.materials"),
        LORE("lore"),
        MODEL_DATA("model-data"),
        COLOR("color"),
        DURABILITY("durability"),
        UNBREAKABLE("unbreakable"),
        PREFIX_CHANCE("generator.prefix-chance"),
        SUFFIX_CHANCE("generator.suffix-chance"),
        ARMOR_TRIMINGS("generator.armor-trimmings"),
        ITEM_FLAGS("item-flags"),
        ENCHANTED("enchanted"),
        SKULL_HASH("skull-hash"),
        BONUSES("generator.bonuses"),
        MIN_LEVEL("level.min"),
        MAX_LEVEL("level.max"),
        TIER("tier"),
        AMMO_TYPES("generator.ammo-types"),
        HAND_TYPES("generator.hand-types"),
        DAMAGE_TYPES("generator.damage-types"),
        DEFENSE_TYPES("generator.defense-types"),
        ITEM_STATS("generator.item-stats"),
        FABLED_ATTRIBUTES("generator.fabled-attributes"),
        SOCKETS("generator.sockets"),
        REQUIREMENTS("user-requirements-by-level"),
        ENCHANTMENTS("generator.enchantments"),
        SKILLS("generator.skills"),
        USES_BY_LEVEL("uses-by-level"),
        SAMPLE(null),
        ;

        private final String path;

        ItemType(String path) {this.path = path;}

        public String getPath() {return path;}

        public String getTitle() {return this.name().replace('_', ' ').toLowerCase();}
    }
}
