package studio.magemonkey.divinity.modules.list.itemhints;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.api.QModule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemHintsManager extends QModule {

    private boolean     glowEnabled;
    private boolean     glowIgnoreVanilla;
    private Set<String> glowIgnoredMaterials;
    private Set<String> glowIgnoredNames;
    private Set<String> glowIgnoredLores;
    private Set<String> glowIgnoredModules;

    private boolean     hintEnabled;
    private String      hintFormatSingular;
    private String      hintFormatPlural;
    private boolean     hintIgnoreVanilla;
    private Set<String> hintIgnoredMaterials;
    private Set<String> hintIgnoredNames;
    private Set<String> hintIgnoredLores;
    private Set<String> hintIgnoredModules;

    public ItemHintsManager(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.ITEM_HINTS;
    }

    @Override
    @NotNull
    public String version() {
        return "1.3.0";
    }

    @Override
    public void setup() {
        String path = "glow.";
        if (this.glowEnabled = cfg.getBoolean(path + "enabled")) {
            glowIgnoreVanilla = cfg.getBoolean(path + "ignored-items.vanilla");
            glowIgnoredMaterials = new HashSet<>(cfg.getStringList(path + "ignored-items.by-material"));
            glowIgnoredNames = new HashSet<>(cfg.getStringList(path + "ignored-items.by-name"));
            glowIgnoredLores = new HashSet<>(cfg.getStringList(path + "ignored-items.by-lore"));
            glowIgnoredModules = new HashSet<>(cfg.getStringList(path + "ignored-items.by-module"));
        }

        path = "hint.";
        if (this.hintEnabled = cfg.getBoolean(path + "enabled")) {
            hintFormatSingular = StringUT.color(cfg.getString(path + "format.singular", "%name%"));
            hintFormatPlural = StringUT.color(cfg.getString(path + "format.plural", "%name% &7x%amount%"));
            hintIgnoreVanilla = cfg.getBoolean(path + "ignored-items.vanilla");
            hintIgnoredMaterials = new HashSet<>(cfg.getStringList(path + "ignored-items.by-material"));
            hintIgnoredNames = new HashSet<>(cfg.getStringList(path + "ignored-items.by-name"));
            hintIgnoredLores = new HashSet<>(cfg.getStringList(path + "ignored-items.by-lore"));
            hintIgnoredModules = new HashSet<>(cfg.getStringList(path + "ignored-items.by-module"));
        }
    }

    @Override
    public void shutdown() {
        if (this.glowIgnoredMaterials != null) {
            this.glowIgnoredMaterials.clear();
            this.glowIgnoredMaterials = null;
        }
        if (this.glowIgnoredNames != null) {
            this.glowIgnoredNames.clear();
            this.glowIgnoredNames = null;
        }
        if (this.glowIgnoredLores != null) {
            this.glowIgnoredLores.clear();
            this.glowIgnoredLores = null;
        }
        if (this.glowIgnoredModules != null) {
            this.glowIgnoredModules.clear();
            this.glowIgnoredModules = null;
        }

        if (this.hintIgnoredMaterials != null) {
            this.hintIgnoredMaterials.clear();
            this.hintIgnoredMaterials = null;
        }
        if (this.hintIgnoredNames != null) {
            this.hintIgnoredNames.clear();
            this.hintIgnoredNames = null;
        }
        if (this.hintIgnoredLores != null) {
            this.hintIgnoredLores.clear();
            this.hintIgnoredLores = null;
        }
        if (this.hintIgnoredModules != null) {
            this.hintIgnoredModules.clear();
            this.hintIgnoredModules = null;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        Item item = e.getEntity();
        this.setItemHint(item, 0);
        this.setGlow(item);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent e) {
        Item src    = e.getEntity();
        Item target = e.getTarget();

        this.setItemHint(target, src.getItemStack().getAmount());
        this.removeScoreboardEntry(src);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void itemPickup(PlayerPickupItemEvent e) {
        Item item = e.getItem();
        this.removeScoreboardEntry(item);
    }

    // Glow is added via packets
    public void setItemHint(@NotNull Item i, int addAmount) {
        if (!this.isHint(i)) return;

        ItemStack item   = i.getItemStack();
        String    name   = ItemUT.getItemName(item);
        int       amount = item.getAmount() + addAmount;
        String    format = amount > 1 ? this.hintFormatPlural : this.hintFormatSingular;

        String name2 = format.replace("%name%", name).replace("%amount%", String.valueOf(amount));
        i.setCustomName(name2);
        i.setCustomNameVisible(true);
    }

    private ChatColor getItemColor(ItemStack item) {
        ChatColor cc   = ChatColor.WHITE;
        String    name = ItemUT.getItemName(item);
        if (name.length() > 2) {
            String ss = String.valueOf(cc.getChar());
            if (name.startsWith(String.valueOf(ChatColor.COLOR_CHAR))) {
                ss = name.substring(1, 2);
            }
            ChatColor c2 = ChatColor.getByChar(ss);
            if (c2 != null && c2.isColor()) cc = c2;
        }

        return cc;
    }

    public void setGlow(Item item) {
        if (!this.isGlow(item)) return;

        ChatColor cc     = getItemColor(item.getItemStack());
        String    teamId = "GLOW_" + (cc != ChatColor.LIGHT_PURPLE ? cc.name() : "LP");

        // We'll add the item to every player's scoreboard
        Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            Scoreboard scoreboard = player.getScoreboard() != null
                    ? player.getScoreboard()
                    : Bukkit.getScoreboardManager().getMainScoreboard();
            if (scoreboard == null) {
                scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            }

            Team team = scoreboard.getTeam(teamId) != null ? scoreboard.getTeam(teamId)
                    : scoreboard.registerNewTeam(teamId);

            team.setColor(cc);
            team.addEntry(item.getUniqueId().toString());

            player.setScoreboard(scoreboard);
        });

        item.setGlowing(true);
    }

    public void removeScoreboardEntry(Item item) {
        if (!this.isGlow(item)) return;

        Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            Scoreboard scoreboard = player.getScoreboard() != null
                    ? player.getScoreboard()
                    : Bukkit.getScoreboardManager().getMainScoreboard();
            if (scoreboard == null) {
                return;
            }

            Team team = scoreboard.getTeam("GLOW_" + getItemColor(item.getItemStack()).name());
            if (team != null) {
                team.removeEntry(item.getUniqueId().toString());
                player.setScoreboard(scoreboard);
            }
        });
    }

    public boolean isGlow(@NotNull Item item) {
        return this.glowEnabled && this.isAffected(item, true);
    }

    public boolean isHint(@NotNull Item item) {
        return this.hintEnabled && this.isAffected(item, false);
    }

    private boolean isAffected(@NotNull Item item, boolean checkGlow) {
        String name = item.getCustomName();
        if (name == null) name = ItemUT.getItemName(item.getItemStack());

        if (name != null) {
            for (String blackText : (checkGlow ? this.glowIgnoredNames : this.hintIgnoredNames)) {
                if (name.contains(blackText)) {
                    return false;
                }
            }
        }

        return this.isAffected(item.getItemStack(), checkGlow);
    }

    private boolean isAffected(@NotNull ItemStack item, boolean checkGlow) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || (!meta.hasDisplayName() && !meta.hasLore())) {
            if (checkGlow ? this.glowIgnoreVanilla : this.hintIgnoreVanilla) {
                return false;
            }
        }

        String type = item.getType().name();
        if ((checkGlow ? this.glowIgnoredMaterials : this.hintIgnoredMaterials).contains(type)) {
            return false;
        }

        String name = ItemUT.getItemName(item);
        for (String blackText : (checkGlow ? this.glowIgnoredNames : this.hintIgnoredNames)) {
            if (name.contains(blackText)) {
                return false;
            }
        }

        List<String> lore = meta == null ? null : meta.getLore();
        if (lore == null) return true;

        for (String blackText : (checkGlow ? this.glowIgnoredLores : this.hintIgnoredLores)) {
            for (String loreText : lore) {
                if (loreText.contains(blackText)) {
                    return false;
                }
            }
        }
        return true;
    }
}
