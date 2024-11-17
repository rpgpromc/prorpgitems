package studio.magemonkey.divinity.utils;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.EngineCfg;

import java.util.List;
import java.util.Set;

public class LoreUT {

    private static Divinity plugin;

    static {
        plugin = Divinity.getInstance();
    }

    @Deprecated
    public static final String TAG_SPLITTER = "__x__";

    public static void addOrReplace(
            @NotNull List<String> lore, int orig, int line, @NotNull String format) {

        if (orig < 0) {
            if (line < 0 || line >= lore.size()) {
                lore.add(format);
            } else {
                lore.add(line, format);
            }
        } else {
            if (line < 0 || line >= lore.size()) {
                lore.set(orig, format);
            } else {
                lore.remove(orig);
                lore.add(line, format);
            }
        }
    }

    public static int addToLore(@NotNull List<String> lore, int pos, @NotNull String value) {
        if (pos >= lore.size()) {
            lore.add(value);
        } else {
            lore.add(pos, value);
        }
        return pos + 1;
    }

    @NotNull
    public static String getStrSeparated(
            @NotNull Set<String> values, @NotNull String sep, @NotNull String color, int maxPerLine, String newline) {
        return LoreUT.getStrSeparated(values.toArray(new String[values.size()]), sep, color, maxPerLine, "");
    }


    @NotNull
    public static String getStrSeparated(
            @NotNull Set<String> values, @NotNull String sep, @NotNull String color) {
        return LoreUT.getStrSeparated(values.toArray(new String[values.size()]), sep, color, -1, "");
    }

    @NotNull
    public static String getStrSeparated(
            @NotNull String[] values, @NotNull String sep, @NotNull String color) {
        return getStrSeparated(values, sep, color, -1, "");
    }

    @NotNull
    public static String getStrSeparated(
            @NotNull String[] values, @NotNull String sep, @NotNull String color, int maxPerLine, String newline) {

        StringBuilder builder   = new StringBuilder("");
        int           lineCount = 0;
        for (String clazz : values) {
            if (clazz.isEmpty()) continue;
            if (builder.length() > 0) {
                builder.append(sep);
            }
            if (maxPerLine > 0 && lineCount == maxPerLine) {
                builder.append("\n").append(newline);
            }
            builder.append(color);
            builder.append(clazz);
            lineCount++;
        }

        return builder.toString();
    }

    public static void replacePlaceholder(
            @NotNull ItemStack item, @NotNull String placeholder, @Nullable String r) {

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.getLore();
        if (lore == null) return;

        int pos = lore.indexOf(placeholder);
        if (pos < 0) return;

        if (r != null && !r.isEmpty()) {
            lore.set(pos, r);
        } else {
            lore.remove(pos);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public static void replaceEnchants(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.getLore();
        if (lore == null) return;

        int pos = lore.indexOf("%ENCHANTS%");
        if (pos < 0) return;

        for (Enchantment e : meta.getEnchants().keySet()) {
            int level = meta.getEnchantLevel(e);
            String value = EngineCfg.LORE_STYLE_ENCHANTMENTS_FORMAT_MAIN
                    .replace("%name%", plugin.lang().getEnchantment(e))
                    .replace("%value%",
                            level > EngineCfg.LORE_STYLE_ENCHANTMENTS_FORMAT_MAX_ROMAN ? String.valueOf(level)
                                    : NumberUT.toRoman(level));
            lore.add(pos, value);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);

        replacePlaceholder(item, "%ENCHANTS%", null);
    }
}
