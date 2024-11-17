package studio.magemonkey.divinity;

import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.modules.api.socketing.ModuleSocket;

public class Perms {

    private static final String PREFIX = "quantumrpg.";

    public static final String USER  = PREFIX + "user";
    public static final String ADMIN = PREFIX + "admin";

    public static final String CLASS = PREFIX + "class";

    public static final String BYPASS_REQ_USER_CLASS       = PREFIX + "bypass.requirement.class";
    public static final String BYPASS_REQ_USER_LEVEL       = PREFIX + "bypass.requirement.level";
    public static final String BYPASS_REQ_USER_SOULBOUND   = PREFIX + "bypass.requirement.soulbound";
    public static final String BYPASS_REQ_USER_UNTRADEABLE = PREFIX + "bypass.requirement.untradeable";

    public static final String EXTRACTOR_CMD_OPEN = PREFIX + "extractor.cmd.open";
    public static final String EXTRACTOR_GUI      = PREFIX + "extractor.gui";

    public static final String CLASS_CMD_SELECT              = PREFIX + "classes.cmd.select";
    public static final String CLASS_CMD_SKILLS              = PREFIX + "classes.cmd.skills";
    public static final String CLASS_CMD_ASPECTS             = PREFIX + "classes.cmd.aspects";
    public static final String CLASS_CMD_STATS               = PREFIX + "classes.cmd.stats";
    public static final String CLASS_CMD_CAST                = PREFIX + "classes.cmd.cast";
    public static final String CLASS_CMD_ADDSKILL            = PREFIX + "classes.cmd.addskill";
    public static final String CLASS_CMD_ADDEXP              = PREFIX + "classes.cmd.addexp";
    public static final String CLASS_CMD_ADDLEVEL            = PREFIX + "classes.cmd.addlevel";
    public static final String CLASS_CMD_ADD_ASPECT_POINTS   = PREFIX + "classes.cmd.addaspectpoints";
    public static final String CLASS_CMD_ADD_SKILL_POINTS    = PREFIX + "classes.cmd.addskillpoints";
    public static final String CLASS_CMD_SETCLASS            = PREFIX + "classes.cmd.setclass";
    public static final String CLASS_CMD_RESET               = PREFIX + "classes.cmd.reset";
    public static final String CLASS_CMD_RESET_ASPECT_POINTS = PREFIX + "classes.cmd.resetaspectpoints";
    public static final String CLASS_CMD_RESET_SKILL_POINTS  = PREFIX + "classes.cmd.resetskillpoints";
    public static final String CLASS_CLASS                   = PREFIX + "classes.class";

    public static final String COMBAT_LOG_CMD_LOG = PREFIX + "combatlog.cmd.log";

    public static final String IDENTIFY_CMD_IDENTIFY = PREFIX + "identify.cmd.identify";

    public static final String FORTIFY_CMD_FORTIFY   = PREFIX + "fortify.cmd.fortify";
    public static final String FORTIFY_CMD_UNFORTIFY = PREFIX + "fortify.cmd.unfortify";

    private static final String SOCKET_CMD_MERCHANT        = PREFIX + "%module%.cmd.merchant";
    private static final String SOCKET_CMD_MERCHANT_OTHERS = PREFIX + "%module%.cmd.merchant.others";
    private static final String SOCKET_GUI_USER            = PREFIX + "%module%.gui.user";
    private static final String SOCKET_GUI_MERCHANT        = PREFIX + "%module%.gui.merchant";

    public static final String MAGIC_DUST_CMD_OPEN = PREFIX + "magicdust.cmd.open";
    public static final String MAGIC_DUST_GUI      = PREFIX + "magicdust.gui";

    public static final String PARTY_CMD_CHAT    = PREFIX + "party.cmd.chat";
    public static final String PARTY_CMD_CREATE  = PREFIX + "party.cmd.create";
    public static final String PARTY_CMD_DISBAND = PREFIX + "party.cmd.disband";
    public static final String PARTY_CMD_DROP    = PREFIX + "party.cmd.drop";
    public static final String PARTY_CMD_EXP     = PREFIX + "party.cmd.exp";
    public static final String PARTY_CMD_MENU    = PREFIX + "party.cmd.menu";
    public static final String PARTY_CMD_INVITE  = PREFIX + "party.cmd.invite";
    public static final String PARTY_CMD_JOIN    = PREFIX + "party.cmd.join";
    public static final String PARTY_CMD_KICK    = PREFIX + "party.cmd.kick";
    public static final String PARTY_CMD_LEAVE   = PREFIX + "party.cmd.leave";
    public static final String PARTY_CMD_TP      = PREFIX + "party.cmd.tp";
    public static final String PARTY_CMD_ROLL    = PREFIX + "party.cmd.roll";

    public static final String REFINE_CMD_REFINE    = PREFIX + "refine.cmd.refine";
    public static final String REFINE_CMD_DOWNGRADE = PREFIX + "refine.cmd.downgrade";

    public static final String REPAIR_CMD_OPEN = PREFIX + "repair.cmd.open";
    public static final String REPAIR_GUI      = PREFIX + "repair.gui";

    public static final String DISMANTLE_CMD_OPEN = PREFIX + "dismantle.cmd.open";
    public static final String DISMANTLE_GUI      = PREFIX + "dismantle.gui";

    public static final String SELL_CMD_OPEN = PREFIX + "sell.cmd.open";
    public static final String SELL_GUI      = PREFIX + "sell.gui";

    public static final String SOULBOUND_CMD_SOUL    = PREFIX + "soulbound.cmd.soul";
    public static final String SOULBOUND_CMD_UNTRADE = PREFIX + "soulbound.cmd.untradeable";

    @NotNull
    public static String getSocketCmdMerchant(@NotNull ModuleSocket<?> module) {
        return SOCKET_CMD_MERCHANT.replace("%module%", module.getId());
    }

    @NotNull
    public static String getSocketCmdMerchantOthers(@NotNull ModuleSocket<?> module) {
        return SOCKET_CMD_MERCHANT_OTHERS.replace("%module%", module.getId());
    }

    @NotNull
    public static String getSocketGuiUser(@NotNull ModuleSocket<?> module) {
        return SOCKET_GUI_USER.replace("%module%", module.getId());
    }

    @NotNull
    public static String getSocketGuiMerchant(@NotNull ModuleSocket<?> module) {
        return SOCKET_GUI_MERCHANT.replace("%module%", module.getId());
    }
}
