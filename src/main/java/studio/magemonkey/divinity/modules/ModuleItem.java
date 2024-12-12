package studio.magemonkey.divinity.modules;

import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.items.exception.MissingItemException;
import studio.magemonkey.codex.api.items.exception.MissingProviderException;
import studio.magemonkey.codex.api.items.providers.VanillaProvider;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.LoadableItem;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.NamespaceResolver;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.list.identify.IdentifyManager.UnidentifiedItem;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager.GeneratorItem;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.ItemTags;
import studio.magemonkey.divinity.stats.items.requirements.ItemRequirements;
import studio.magemonkey.divinity.stats.items.requirements.user.UntradeableRequirement;
import studio.magemonkey.divinity.utils.LoreUT;

import java.util.*;

public abstract class ModuleItem extends LoadableItem {
    protected final QModuleDrop<?>            module;
    protected       Divinity                  plugin;
    protected       String                    name;
    protected       ItemType                  material;
    protected       List<String>              lore;
    protected       int                       modelData;
    protected       int                       durability;
    protected       int[]                     color;
    protected       boolean                   enchanted;
    protected       String                    hash;
    protected       Set<ItemFlag>             flags;
    protected       boolean                   isUnbreakable;
    protected       Map<Enchantment, Integer> enchants;

    // Creating new config
    @Deprecated
    public ModuleItem(@NotNull Divinity plugin, String path, QModuleDrop<?> module) throws
            InvalidConfigurationException {
        super(plugin, path);
        this.module = module;
        this.plugin = plugin;
    }

    // Load from existent config
    public ModuleItem(@NotNull Divinity plugin, @NotNull JYML cfg, @NotNull QModuleDrop<?> module) {
        super(plugin, cfg);
        this.plugin = plugin;
        this.module = module;
        this.updateConfig(cfg);

        // Fix for loading Generator Items
        if (this instanceof GeneratorItem || this instanceof UnidentifiedItem) {
            this.material = new VanillaProvider.VanillaItemType(Material.LEATHER_HELMET);
        } else {
            String[] matSplit = cfg.getString("material", "STONE").split(":");
            try {
                this.material = CodexEngine.get().getItemManager().getItemType(matSplit[0].toUpperCase());
            } catch (MissingItemException | MissingProviderException e) {
                throw new IllegalArgumentException("Invalid item material!", e);
            }
        }

        // Make item name and lore with complete format
        this.name = StringUT.color(cfg.getString("name", this.getId()));
        this.name = module.getItemNameFormat().replace(ItemTags.PLACEHOLDER_ITEM_NAME, this.name);
        processLore(cfg, module);

        this.modelData = cfg.getInt("model-data", -1);
        this.durability = cfg.getInt("durability", -1);

        String color = cfg.getString("color");
        if (color != null) {
            String[] rgb = color.split(",");
            int      r   = StringUT.getInteger(rgb[0], -1);
            int      g   = rgb.length >= 2 ? StringUT.getInteger(rgb[1], -1) : 0;
            int      b   = rgb.length >= 3 ? StringUT.getInteger(rgb[2], -1) : 0;
            this.color = new int[]{r, g, b};
        }

        this.enchanted = cfg.getBoolean("enchanted");
        this.hash = cfg.getString("skull-hash");

        this.flags = new HashSet<>();
        for (String flag : cfg.getStringList("item-flags")) {
            if (flag.equals(JStrings.MASK_ANY)) {
                this.flags.addAll(Arrays.asList(ItemFlag.values()));
                break;
            }
            try {
                this.flags.add(ItemFlag.valueOf(flag.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                continue;
            }
        }

        this.isUnbreakable = cfg.getBoolean("unbreakable");

        this.enchants = new HashMap<>();
        for (String sId : cfg.getSection("enchantments")) {
            Enchantment en = Enchantment.getByKey(NamespacedKey.minecraft(sId.toLowerCase()));
            if (en == null) {
                plugin.error("Invalid enchantment provided: " + sId + " (" + cfg.getFile().getName() + ")");
                continue;
            }

            int level = cfg.getInt("enchantments." + sId, 1);

            this.enchants.put(en, level);
        }

        cfg.saveChanges();
    }

    /**
     * This method just returns the same string by default, but can be overridden to apply custom replacements.
     * For example, {@link LeveledItem} uses this to replace the item tier placeholders.
     *
     * @param lore The lore line to apply replacements to.
     * @return The lore line with replacements applied.
     */
    protected String applyLoreReplacements(String lore) {
        return lore;
    }

    protected void processLore(@NotNull JYML cfg, @NotNull QModuleDrop<?> module) {
        this.lore = new ArrayList<>();
        for (String mLore : module.getItemLoreFormat()) {
            if (mLore.equalsIgnoreCase(ItemTags.PLACEHOLDER_ITEM_LORE)) {
                for (String itemLore : StringUT.color(cfg.getStringList("lore"))) {
                    this.lore.add(applyLoreReplacements(itemLore));
                }
                continue;
            }
            this.lore.add(applyLoreReplacements(mLore));
        }
    }

    private void updateConfig(@NotNull JYML cfg) {
        cfg.saveChanges();
    }

    @Override
    protected void save(@NotNull JYML cfg) {
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public ItemType getMaterial() {
        return this.material;
    }

    @NotNull
    public List<String> getLore() {
        return this.lore;
    }

    public int[] getColor() {return this.color == null ? null : Arrays.copyOf(color, 3);}

    public Set<ItemFlag> getFlags() {return new HashSet<>(this.flags);}

    public boolean isUnbreakable() {return isUnbreakable;}

    @NotNull
    public QModuleDrop<?> getModule() {
        return this.module;
    }

    @NotNull
    public ItemStack create() {
        return this.build();
    }

    @NotNull
    protected ItemStack build() {
        return build(this.getMaterial().create());
    }

    @NotNull
    protected ItemStack build(@NotNull ItemStack item) {
        ItemUT.addSkullTexture(item, this.hash, this.getId());

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(StringUT.colorSensitiveStrip(this.name.replace(ItemTags.PLACEHOLDER_BASE_NAME,
                meta.getDisplayName())));
        List<String> baseLore = meta.getLore();
        List<String> modifiedLore;
        if (baseLore == null || baseLore.isEmpty()) {
            modifiedLore = new ArrayList<>(this.lore);
            modifiedLore.remove(ItemTags.PLACEHOLDER_BASE_LORE);
        } else {
            modifiedLore = StringUT.replace(this.lore, ItemTags.PLACEHOLDER_BASE_LORE, baseLore);
        }

        meta.setLore(modifiedLore);

        if (this.modelData > 0) {
            meta.setCustomModelData(this.modelData);
        }
        if (this.durability >= 0 && meta instanceof Damageable) {
            Damageable dMeta = (Damageable) meta;
            dMeta.setDamage(this.durability);
        }

        if (!ArrayUtils.isEmpty(this.color)) {
            int r = this.color[0] >= 0 ? color[0] : Rnd.get(255);
            int g = this.color[1] >= 0 ? color[1] : Rnd.get(255);
            int b = this.color[2] >= 0 ? color[2] : Rnd.get(255);
            if (meta instanceof LeatherArmorMeta) {
                LeatherArmorMeta lm = (LeatherArmorMeta) meta;
                lm.setColor(Color.fromRGB(r, g, b));
            } else if (meta instanceof PotionMeta) {
                PotionMeta pm = (PotionMeta) meta;
                pm.setColor(Color.fromRGB(r, g, b));
            }
        }

        meta.addItemFlags(this.flags.toArray(new ItemFlag[this.flags.size()]));
        meta.setUnbreakable(this.isUnbreakable);
        if (this.enchanted) {
            meta.addEnchant(NamespaceResolver.getEnchantment("POWER", "ARROW_DAMAGE"), 1, true); // ARROW_DAMAGE/POWER
        }

        item.setItemMeta(meta);

        for (Map.Entry<Enchantment, Integer> e : this.enchants.entrySet()) {
            Enchantment enchant   = e.getKey();
            int         enchLevel = e.getValue();
            if (enchLevel < 1) continue;

            item.addUnsafeEnchantment(enchant, enchLevel);
        }

        ItemStats.setId(item, this.getId());
        ItemStats.setModule(item, this.getModule().getId());

        this.replacePlaceholders(item);

        return item;
    }

    private void replacePlaceholders(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String name = meta.getDisplayName();
        //Potential replacers here
        meta.setDisplayName(name);

        List<String> metaLore = meta.getLore();
        List<String> lore     = metaLore != null ? metaLore : new ArrayList<>();
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            //Potential replacers here
            lore.set(i, StringUT.color(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);

        // Replace %UNTRADEABLE% placeholder
        UntradeableRequirement reqUntrade = ItemRequirements.getUserRequirement(UntradeableRequirement.class);
        if (reqUntrade != null && reqUntrade.hasPlaceholder(item)) {
            reqUntrade.add(item, -1);
        }

        // Delete placeholders if requirements were not added or been disabled.
        LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_REQ_ITEM_UNTRADEABLE, null);
    }
}
