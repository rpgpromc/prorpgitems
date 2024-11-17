//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package studio.magemonkey.divinity.modules;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.util.NumberUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.actions.ActionManipulator;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.api.QModuleUsage;
import studio.magemonkey.divinity.stats.items.ItemTags;
import studio.magemonkey.divinity.stats.items.requirements.ItemRequirements;
import studio.magemonkey.divinity.stats.items.requirements.user.BannedClassRequirement;
import studio.magemonkey.divinity.stats.items.requirements.user.ClassRequirement;
import studio.magemonkey.divinity.stats.items.requirements.user.LevelRequirement;
import studio.magemonkey.divinity.types.QClickType;
import studio.magemonkey.divinity.utils.LoreUT;

import java.util.*;
import java.util.Map.Entry;

/**
 * @deprecated
 */
@Deprecated
public class UsableItem extends LimitedItem {
    protected TreeMap<Integer, String[]>            reqUserLvl;
    protected TreeMap<Integer, String[]>            reqUserClass;
    protected TreeMap<Integer, String[]>            reqBannedUserClass;
    protected TreeMap<Integer, Map<String, Object>> varsLvl;
    protected Map<QClickType, UsableItem.Usage>     usageMap;

    public UsableItem(@NotNull Divinity plugin, @NotNull JYML cfg, @NotNull QModuleUsage<?> module) {
        super(plugin, cfg, module);
        String   sLvl;
        Iterator var5;
        int      lvl;
        String   reqRaw;
        if (ItemRequirements.isRegisteredUser(LevelRequirement.class)) {
            this.reqUserLvl = new TreeMap();
            var5 = cfg.getSection("user-requirements-by-level.level").iterator();

            while (var5.hasNext()) {
                sLvl = (String) var5.next();
                lvl = StringUT.getInteger(sLvl, -1);
                if (lvl > 0) {
                    reqRaw = cfg.getString("user-requirements-by-level.level." + sLvl);
                    if (reqRaw != null && !reqRaw.isEmpty()) {
                        this.reqUserLvl.put(lvl, reqRaw.split(":"));
                    }
                }
            }
        }

        if (ItemRequirements.isRegisteredUser(ClassRequirement.class)) {
            this.reqUserClass = new TreeMap();
            var5 = cfg.getSection("user-requirements-by-level.class").iterator();

            while (var5.hasNext()) {
                sLvl = (String) var5.next();
                lvl = StringUT.getInteger(sLvl, -1);
                if (lvl > 0) {
                    reqRaw = cfg.getString("user-requirements-by-level.class." + sLvl);
                    if (reqRaw != null && !reqRaw.isEmpty()) {
                        this.reqUserClass.put(lvl, reqRaw.split(","));
                    }
                }
            }
        }

        if (ItemRequirements.isRegisteredUser(BannedClassRequirement.class)) {
            this.reqBannedUserClass = new TreeMap();
            var5 = cfg.getSection("user-requirements-by-level.banned-class").iterator();

            while (var5.hasNext()) {
                sLvl = (String) var5.next();
                lvl = StringUT.getInteger(sLvl, -1);
                if (lvl > 0) {
                    reqRaw = cfg.getString("user-requirements-by-level.banned-class." + sLvl);
                    if (reqRaw != null && !reqRaw.isEmpty()) {
                        this.reqBannedUserClass.put(lvl, reqRaw.split(","));
                    }
                }
            }
        }

        this.varsLvl = new TreeMap();
        var5 = cfg.getSection("variables-by-level").iterator();

        while (true) {
            do {
                if (!var5.hasNext()) {
                    this.usageMap = new HashMap();
                    var5 = cfg.getSection("usage").iterator();

                    while (var5.hasNext()) {
                        sLvl = (String) var5.next();
                        QClickType qClick = QClickType.valueOf(sLvl.toUpperCase());
                        reqRaw = "usage." + sLvl + ".";
                        double            usageCd     = cfg.getDouble(reqRaw + "cooldown");
                        ActionManipulator usageEngine = new ActionManipulator(plugin, cfg, reqRaw + "actions");
                        UsableItem.Usage  iu          = new UsableItem.Usage(usageCd, usageEngine);
                        this.usageMap.put(qClick, iu);
                    }

                    return;
                }

                sLvl = (String) var5.next();
                lvl = StringUT.getInteger(sLvl, -1);
            } while (lvl <= 0);

            Map<String, Object> vars = new HashMap();
            Iterator            var9 = cfg.getSection("variables-by-level." + sLvl).iterator();

            while (var9.hasNext()) {
                String var    = (String) var9.next();
                String path   = "variables-by-level." + sLvl + "." + var;
                Object varVal = cfg.get(path);
                vars.put(var.toLowerCase(), varVal);
            }

            this.varsLvl.put(lvl, vars);
        }
    }

    protected final int[] getUserLevelRequirement(int itemLvl) {
        if (this.reqUserLvl != null) return null;
        Entry<Integer, String[]> e = this.reqUserLvl.floorEntry(itemLvl);
        return e == null ? new int[1] : this.doMathExpression(itemLvl, e.getValue());
    }

    @Nullable
    protected final String[] getUserClassRequirement(int itemLvl) {
        if (this.reqBannedUserClass == null) return null;
        Entry<Integer, String[]> e = this.reqUserClass.floorEntry(itemLvl);
        return e == null ? null : e.getValue();
    }

    @Nullable
    protected final String[] getBannedUserClassRequirement(int itemLvl) {
        if (this.reqBannedUserClass == null) return null;
        Entry<Integer, String[]> e = this.reqBannedUserClass.floorEntry(itemLvl);
        return e == null ? null : e.getValue();
    }

    @NotNull
    public Map<Integer, Map<String, Object>> getVariables() {
        return this.varsLvl;
    }

    @NotNull
    public final String replaceVars(@NotNull String str, int lvl) {
        Entry<Integer, Map<String, Object>> e = this.varsLvl.floorEntry(lvl);
        if (e == null) {
            return str;
        } else {
            Map<String, Object> vars = e.getValue();

            Entry  eVar;
            String valueFormat;
            for (Iterator var6 = vars.entrySet().iterator();
                 var6.hasNext();
                 str = str.replace("%var_" + eVar.getKey() + "%", valueFormat)) {
                eVar = (Entry) var6.next();
                Object value = eVar.getValue();
                valueFormat = value.toString();
                if (value instanceof Number) {
                    double valD = NumberUT.round(StringUT.getDouble(valueFormat, -1.0D));
                    if (valD == (double) ((long) valD)) {
                        valueFormat = String.format("%d", (long) valD);
                    } else {
                        valueFormat = String.format("%s", valD);
                    }
                }
            }

            return str;
        }
    }

    @NotNull
    protected ItemStack build(int lvl, int uses) {
        ItemStack item     = super.build(lvl, uses);
        int[]     levelReq = this.getUserLevelRequirement(lvl);
        if (levelReq != null) {
            LevelRequirement reqLevel = ItemRequirements.getUserRequirement(LevelRequirement.class);
            if (reqLevel != null)
                reqLevel.add(item, levelReq, -1);
        }

        String[] userClass = this.getUserClassRequirement(lvl);
        if (userClass != null) {
            ClassRequirement reqClass = ItemRequirements.getUserRequirement(ClassRequirement.class);
            if (reqClass != null)
                reqClass.add(item, userClass, -1);
        }

        String[] bannedUserClass = this.getBannedUserClassRequirement(lvl);
        if (bannedUserClass != null) {
            BannedClassRequirement reqBannedClass = ItemRequirements.getUserRequirement(BannedClassRequirement.class);
            if (reqBannedClass != null)
                reqBannedClass.add(item, bannedUserClass, -1);
        }

        LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_REQ_USER_LEVEL, null);
        LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_REQ_USER_CLASS, null);
        LoreUT.replacePlaceholder(item, ItemTags.PLACEHOLDER_REQ_USER_BANNED_CLASS, null);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        } else {
            List<String> lore = meta.getLore();
            if (lore == null) {
                return item;
            } else {
                for (int i = 0; i < lore.size(); ++i) {
                    lore.set(i, StringUT.color(this.replaceVars(lore.get(i), lvl)));
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
            }
        }
    }

    @NotNull
    public Map<QClickType, UsableItem.Usage> getUsage() {
        return this.usageMap;
    }

    @Nullable
    public UsableItem.Usage getUsage(@NotNull QClickType type) {
        return this.usageMap.get(type);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static class Cooldown {
        private final String     itemId;
        private final QClickType click;
        private final long       time;

        public Cooldown(@NotNull String itemId, @NotNull QClickType click, double cd) {
            this.itemId = itemId;
            this.click = click;
            this.time = System.currentTimeMillis() + (long) (1000.0D * cd);
        }

        @NotNull
        public String getItemId() {
            return this.itemId;
        }

        @NotNull
        public QClickType getClickType() {
            return this.click;
        }

        public long getTimeExpire() {
            return this.time;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > this.time;
        }
    }

    public class Usage {
        private final double            cd;
        private final ActionManipulator actionManipulator;

        public Usage(double cd, @NotNull ActionManipulator actionManipulator) {
            this.cd = cd;
            this.actionManipulator = actionManipulator;
        }

        public double getCooldown() {
            return this.cd;
        }

        @NotNull
        public ActionManipulator getActionEngine() {
            return this.actionManipulator;
        }

        public void use(@NotNull Player p, int lvl) {
            ActionManipulator r1 = this.getActionEngine().replace((str) -> UsableItem.this.replaceVars(str, lvl));
            r1.process(p);
        }
    }
}
