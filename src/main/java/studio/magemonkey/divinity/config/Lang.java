package studio.magemonkey.divinity.config;

import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.ILangMsg;
import studio.magemonkey.codex.core.config.CoreLang;
import studio.magemonkey.codex.manager.types.ClickType;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.data.api.UserEntityNamesMode;
import studio.magemonkey.divinity.modules.list.classes.object.ClassAttributeType;
import studio.magemonkey.divinity.modules.list.party.PartyManager;


public class Lang extends CoreLang {

    public ILangMsg Command_Modify_List                               = new ILangMsg(
            this,
            "{message: ~prefix: false;}"
                    + "&8&m-----------&8&l[ &aQuantum RPG &7- &aModify &8&l]&8&m-----------"
                    + "\n"
                    + "&2> &a/qrpg modify name <Name> &7- Change display name of the item."
                    + "\n"
                    + "&2> &a/qrpg modify lore add [text] [line] &7- Add lore line."
                    + "\n"
                    + "&2> &a/qrpg modify lore del [line] &7- Remove lore line."
                    + "\n"
                    + "&2> &a/qrpg modify lore clear &7- Clear item lore."
                    + "\n"
                    + "&2> &a/qrpg modify flag add <flag> &7- Add a flag to item."
                    + "\n"
                    + "&2> &a/qrpg modify flag del <flag> &7- Remove a flag from the item."
                    + "\n"
                    + "&2> &a/qrpg modify enchant <enchantment> <level> &7- Enchants/Disenchants the item."
                    + "\n"
                    + "&2> &a/qrpg modify potion <effect> <level> <duration> [ambient(true/false)] [particles(true/false)] [icon(true/false)] &7- Modify potion effects."
                    + "\n"
                    + "&2> &a/qrpg modify color <r,g,b> &7- Change the color of leather armor or potion.");
    public ILangMsg Command_Set_List                                  = new ILangMsg(
            this,
            "{message: ~prefix: false;}"
                    + "&8&m-----------&8&l[ &aQuantum RPG &7- &aSet &8&l]&8&m-----------"
                    + "\n"
                    + "&2> &a/qrpg set stat <type> <value> [position] &7- Set the attribute to item."
                    + "\n"
                    + "&2> &a/qrpg set level <number> [position] &7- Add level requirements to item."
                    + "\n"
                    + "&2> &a/qrpg set class <class1,class2,etc> [position] &7- Add class requirements to item."
                    + "\n"
                    + "&2> &a/qrpg set damage <type> <min> <max> [position] &7- Add the damage type to item."
                    + "\n"
                    + "&2> &a/qrpg set defense <type> <value> [position] &7- Add the defense type to item."
                    + "\n"
                    + "&2> &a/qrpg set ammo <type> [position] &7- Set the ammo type to bow."
                    + "\n"
                    + "&2> &a/qrpg set hand <type> [position] &7- Set the hand type to item."
                    + "\n"
                    + "&2> &a/qrpg set socket <type> [position] &7- Add an empty slot to item.");
    public ILangMsg Command_Modify_Desc                               = new ILangMsg(this, "Modify an item.");
    public ILangMsg Command_Modify_Done                               = new ILangMsg(this, "Done!");
    public ILangMsg Command_Buff_Desc                                 =
            new ILangMsg(this, "Temporary increases specified attribute.");
    public ILangMsg Command_Buff_Usage                                =
            new ILangMsg(this, "<player> <type> <id> <amount> <duration> [-r]");
    public ILangMsg Command_Buff_Done                                 =
            new ILangMsg(this, "Added &a%amount% %stat%&7 buff to &a%player%&7 for &a%time%&7!");
    public ILangMsg Command_Buff_Get                                  =
            new ILangMsg(this, "You just got &a%amount% %stat%&7 buff for &a%time%&7!");
    public ILangMsg Module_Cmd_List_Usage                             = new ILangMsg(this, "[page]");
    public ILangMsg Module_Cmd_List_Desc                              = new ILangMsg(this, "List of module items.");
    public ILangMsg Module_Cmd_Get_Done                               =
            new ILangMsg(this, "You get &ax%amount% &7of &a%item%&7!");
    public ILangMsg Module_Cmd_Get_Usage                              = new ILangMsg(this, "<id> [level] [amount]");
    public ILangMsg Module_Cmd_Get_Desc                               = new ILangMsg(this, "Get an module item.");
    public ILangMsg Module_Cmd_Give_Done                              =
            new ILangMsg(this, "You gave &ax%amount% &7of &a%item%&7 to &a%player%&7!");
    public ILangMsg Module_Cmd_Give_Usage                             =
            new ILangMsg(this, "<player> <id> [level] [amount]");
    public ILangMsg Module_Cmd_Give_Desc                              =
            new ILangMsg(this, "Give an module item to a player.");
    public ILangMsg Module_Cmd_Drop_Usage                             =
            new ILangMsg(this, "<world> <x> <y> <z> <id> [level] [amount]");
    public ILangMsg Module_Cmd_Drop_Desc                              =
            new ILangMsg(this, "Drops the specified item in the world.");
    public ILangMsg Module_Cmd_Drop_Done                              =
            new ILangMsg(this, "You dropped &ax%amount% &7of &a%item%&7 in &a%w%&7, %x%&7, &a%y%&7, &a%z%&7!");
    public ILangMsg Module_Cmd_Help_Format                            =
            new ILangMsg(this, "&2> &a/%cmd% %label% %usage% &7- %desc%");
    public ILangMsg Module_Cmd_Usage                                  =
            new ILangMsg(this, "&cUsage: &f/%cmd% %label% %usage%");
    public ILangMsg Module_Cmd_Reload                                 =
            new ILangMsg(this, "Module &a%module% &7reloaded!");
    public ILangMsg Drop_Module_Cmd_Give_Done                         =
            new ILangMsg(this, "&a%item%&7 given to &a%player%&7, &a%amount%&7 items given!");
    public ILangMsg Drop_Module_Cmd_Drop_Give_Usage                   =
            new ILangMsg(this, "<player> <table_id> [level]");
    public ILangMsg Drop_Module_Cmd_Drop_Give_Desc                    =
            new ILangMsg(this, "Roll a drop table and give it to a player.");
    public ILangMsg Drop_Module_Cmd_Drop_Drop_Usage                   =
            new ILangMsg(this, "<player> <table_id> [level]");
    public ILangMsg Drop_Module_Cmd_Drop_Drop_Desc                    =
            new ILangMsg(this, "Roll a drop table and drop it to a player's feet.");
    public ILangMsg Drop_Module_Cmd_Drop_Invalid_Table                = new ILangMsg(this, "Invalid table '%table%'");
    public ILangMsg Module_Cmd_List_Format_List                       = new ILangMsg(
            this,
            "{message: ~prefix: false;}"
                    + "\n"
                    + "&8&m-------- &e List of %module% &8&m--------"
                    + "\n"
                    + "&6%pos%. &e%item% %button_get%"
                    + "\n"
                    + "&8&m-------- &e Page &7%page% &e of &7 %pages% &8&m--------"
                    + "\n");
    public ILangMsg Module_Cmd_List_Button_Get_Name                   = new ILangMsg(this, "&a&l[Get Item]");
    public ILangMsg Module_Cmd_List_Button_Get_Hint                   =
            new ILangMsg(this, "&7Gives item to your inventory.");
    public ILangMsg Module_Socketing_Cmd_Merchant_Desc                =
            new ILangMsg(this, "Open Merchant Socketing GUI.");
    public ILangMsg Module_Socketing_Cmd_Merchant_Usage               =
            new ILangMsg(this, "[player] [force(true/false)]");
    public ILangMsg Module_Item_Usage_Cooldown                        =
            new ILangMsg(this, "&cYou must wait &e%time%&c to use &f%item% &cagain!");
    public ILangMsg Module_Item_Usage_Broken                          =
            new ILangMsg(this, "Your item is broken! You can not use it!");
    public ILangMsg Module_Item_Usage_NoCharges                       =
            new ILangMsg(this, "&e%item% &cis out of charges! You must recharge it to use!");
    public ILangMsg Module_Item_Apply_Error_Level                     =
            new ILangMsg(this, "&e%source% &ccan only be applied to items with level(s) &e%value%&c!");
    public ILangMsg Module_Item_Apply_Error_Type                      =
            new ILangMsg(this, "&e%source% &ccan only be applied to specified items: &e%value%&c!");
    public ILangMsg Module_Item_Apply_Error_Module                    =
            new ILangMsg(this, "&e%source% &ccan only be applied to specified items: &e%value%&c!");
    public ILangMsg Module_Item_Apply_Error_Socket                    =
            new ILangMsg(this, "&e%target% &cdoes not have free &e%socket%&c!");
    public ILangMsg Module_Item_Apply_Error_Tier                      =
            new ILangMsg(this, "&e%source% &ccan only be applied to &e%tier% &citems!");
    public ILangMsg Module_Item_Interact_Error_Level                  =
            new ILangMsg(this, "&cYour level is not suitable for using %item%&c.");
    public ILangMsg Module_Item_Interact_Error_Class                  =
            new ILangMsg(this, "&cYour class is not suitable for using %item%&c.");
    public ILangMsg Module_Item_Interact_Error_Soulbound              = new ILangMsg(this,
            "&cYou must set &eSoulbound &cto use %item%! Do &eRight-Click &con &e%item%&c in opened inventory.");
    public ILangMsg Module_Item_Interact_Error_Owner                  =
            new ILangMsg(this, "&e%item% &cbounds to &e%owner%&c. You can not use it.");
    public ILangMsg Module_Item_Interact_Error_Hand                   =
            new ILangMsg(this, "Your weapon is two-handed! You can't hold items in off hand!");
    public ILangMsg Module_Item_Interact_Error_Creative               =
            new ILangMsg(this, "&cPlease exit from &eCreative &cgame mode.");
    public ILangMsg Module_Item_Socketing_Merchant_Error_TooExpensive =
            new ILangMsg(this, "&cYou can't afford this operation!");
    public ILangMsg Module_Item_Socketing_Merchant_Notify_Pay         =
            new ILangMsg(this, "You paid &e$%amount%&7 for socketing services.");
    public ILangMsg Module_Item_Socketing_Error_InAction              =
            new ILangMsg(this, "&cYou can not socket items during active actions.");
    public ILangMsg Module_Item_Socketing_Error_AlreadyHave           =
            new ILangMsg(this, "&e%item% &calready have this item socketed.");
    public ILangMsg Module_Item_Socketing_Result_Total                = new ILangMsg(
            this,
            "{message: ~prefix: false;}"
                    + "&6&m                   &e&l[&e Socketing Result &6&l]&6&m                   &r"
                    + "\n"
                    + "&7"
                    + "\n"
                    + "&6Items Used:"
                    + "\n"
                    + "&e▸ %state-target% %item-target%"
                    + "\n"
                    + "&e▸ %state-source% %item-source%"
                    + "\n"
                    + "&7"
                    + "\n");
    public ILangMsg Module_Item_Socketing_Result_State_Success        = new ILangMsg(this, "&a(Success) &f");
    public ILangMsg Module_Item_Socketing_Result_State_Destroyed      = new ILangMsg(this, "&c(Destroyed) &f");
    public ILangMsg Module_Item_Socketing_Result_State_Wiped          = new ILangMsg(this, "&d(Corrupted) &f");
    public ILangMsg Module_Item_Socketing_Result_State_Consumed       = new ILangMsg(this, "&9(Consumed) &f");
    public ILangMsg Module_Item_Socketing_Result_State_Saved          = new ILangMsg(this, "&d(Saved) &f");
    public ILangMsg Classes_Cmd_Select_Desc                           = new ILangMsg(this, "Change or select class.");
    public ILangMsg Classes_Cmd_Skills_Desc                           = new ILangMsg(this, "Manage your skills.");
    public ILangMsg Classes_Cmd_Aspects_Desc                          = new ILangMsg(this, "Manage your aspects.");
    public ILangMsg Classes_Cmd_Stats_Desc                            = new ILangMsg(this, "View class stats.");
    public ILangMsg Classes_Cmd_AddSkill_Desc                         = new ILangMsg(this, "Add skill to player.");
    public ILangMsg Classes_Cmd_AddSkill_Usage                        =
            new ILangMsg(this, "<player> <skill> <level> [force<true/false>]");
    public ILangMsg Classes_Cmd_AddSkill_Done                         =
            new ILangMsg(this, "&aGiven &f%skill% &askill to &f%name%&a!");
    public ILangMsg Classes_Cmd_AddSkill_Error_NoSkill                = new ILangMsg(this, "&cInvalid skill!");
    public ILangMsg Classes_Cmd_AddExp_Desc                           = new ILangMsg(this, "Add class exp to player.");
    public ILangMsg Classes_Cmd_AddExp_Usage                          = new ILangMsg(this, "<player> <amount>");
    public ILangMsg Classes_Cmd_AddExp_Done                           =
            new ILangMsg(this, "&aGiven &f%amount% &aexp to &f%name%&a!");
    public ILangMsg Classes_Cmd_AddExp_Error_NoClass                  =
            new ILangMsg(this, "&cUser don't have a class!");
    public ILangMsg Classes_Cmd_AddAspectPoints_Desc                  =
            new ILangMsg(this, "Add aspect points to player.");
    public ILangMsg Classes_Cmd_AddAspectPoints_Usage                 = new ILangMsg(this, "<player> <amount>");
    public ILangMsg Classes_Cmd_AddAspectPoints_Done                  =
            new ILangMsg(this, "&aGiven &f%amount% &aaspect points to &f%name%&a!");
    public ILangMsg Classes_Cmd_AddAspectPoints_Error_NoClass         =
            new ILangMsg(this, "&cUser don't have a class!");
    public ILangMsg Classes_Cmd_AddSkillPoints_Desc                   =
            new ILangMsg(this, "Add skill points to player.");
    public ILangMsg Classes_Cmd_AddSkillPoints_Usage                  = new ILangMsg(this, "<player> <amount>");
    public ILangMsg Classes_Cmd_AddSkillPoints_Done                   =
            new ILangMsg(this, "&aGiven &f%amount% &askill points to &f%name%&a!");
    public ILangMsg Classes_Cmd_AddSkillPoints_Error_NoClass          =
            new ILangMsg(this, "&cUser don't have a class!");
    public ILangMsg Classes_Cmd_AddLevel_Desc                         =
            new ILangMsg(this, "Add class level(s) to player.");
    public ILangMsg Classes_Cmd_AddLevel_Usage                        = new ILangMsg(this, "<player> <amount>");
    public ILangMsg Classes_Cmd_AddLevel_Done                         =
            new ILangMsg(this, "&aGiven &f%amount% &alevels to &f%name%&a!");
    public ILangMsg Classes_Cmd_AddLevel_Error_NoClass                =
            new ILangMsg(this, "&cUser don't have a class!");
    public ILangMsg Classes_Cmd_Cast_Desc                             = new ILangMsg(this, "Cast specified skill.");
    public ILangMsg Classes_Cmd_Cast_Usage                            =
            new ILangMsg(this, "<skill> [level] [force(true/false)]");
    public ILangMsg Classes_Cmd_Cast_Done                             =
            new ILangMsg(this, "&aSkill casted: &f%skill%&a!");
    public ILangMsg Classes_Cmd_Cast_Error_InvalidSkill               = new ILangMsg(this, "&cNo such skill!");
    public ILangMsg Classes_Cmd_SetClass_Desc                         = new ILangMsg(this, "Set player's class.");
    public ILangMsg Classes_Cmd_SetClass_Usage                        =
            new ILangMsg(this, "<player> <class> [force<true/false>]");
    public ILangMsg Classes_Cmd_SetClass_Done                         =
            new ILangMsg(this, "&aSet &f%class% &aclass to &f%name%&a!");
    public ILangMsg Classes_Cmd_SetClass_Error_NoClass                = new ILangMsg(this, "&cInvalid class!");
    public ILangMsg Classes_Cmd_Reset_Desc                            =
            new ILangMsg(this, "Reset player's class data.");
    public ILangMsg Classes_Cmd_Reset_Usage                           = new ILangMsg(this, "<player>");
    public ILangMsg Classes_Cmd_Reset_Done                            =
            new ILangMsg(this, "&7Reset &a%player% &7class data!");
    public ILangMsg Classes_Cmd_ResetAspectPoints_Desc                =
            new ILangMsg(this, "Reset player's aspect points.");
    public ILangMsg Classes_Cmd_ResetAspectPoints_Usage               = new ILangMsg(this, "<player>");
    public ILangMsg Classes_Cmd_ResetAspectPoints_Done                =
            new ILangMsg(this, "Reset &a%player% &7aspect points!");
    public ILangMsg Classes_Cmd_ResetSkillPoints_Desc                 =
            new ILangMsg(this, "Reset player's skill points.");
    public ILangMsg Classes_Cmd_ResetSkillPoints_Usage                = new ILangMsg(this, "<player>");
    public ILangMsg Classes_Cmd_ResetSkillPoints_Done                 =
            new ILangMsg(this, "Reset &a%player% &7skill points!");
    public ILangMsg Classes_Error_Level_World                         =
            new ILangMsg(this, "&cYour level must be &e%level%+ &cto enter this world!");
    public ILangMsg Classes_Error_NoClass                             =
            new ILangMsg(this, "&cYou must have class to do that!");
    public ILangMsg Classes_Aspect_Inc_Error_NoPoints                 =
            new ILangMsg(this, "You don't have enought aspect points!");
    public ILangMsg Classes_Aspect_Inc_Done                           =
            new ILangMsg(this, "Your &a%aspect% &7has been increased to &a%value%&7!");
    public ILangMsg Classes_Skill_Cast_Error_Skill_Level              =
            new ILangMsg(this, "&cYour &e%skill%&c skill level must be at least &e%lvl% &cto cast this skill!");
    public ILangMsg Classes_Skill_Cast_Error_Skill_Learn              =
            new ILangMsg(this, "&cYou must learn &e%skill%&c skill to cast this skill!");
    public ILangMsg Classes_Skill_Cast_Error_Class                    =
            new ILangMsg(this, "&cYour class &e(%class%&e) &ccan not use this skill!");
    public ILangMsg Classes_Skill_Cast_Error_Level                    =
            new ILangMsg(this, "&cYour level must be at least &e%lvl% &cto cast this skill!");
    public ILangMsg Classes_Skill_Cast_Error_Cooldown                 =
            new ILangMsg(this, "&cYou must wait &e%time% &cbefore cast &e%skill%&c again!");
    public ILangMsg Classes_Skill_Cast_Error_Mana                     =
            new ILangMsg(this, "[ACTION_BAR]&cNot enough mana.");
    public ILangMsg Classes_Skill_Cast_Done                           =
            new ILangMsg(this, "[ACTION_BAR]&eCasting: &f%skill%&e...");
    public ILangMsg Classes_Skill_Cast_Cancel                         = new ILangMsg(this, "[ACTION_BAR]&4&lCancelled");
    public ILangMsg Classes_Skill_Learn_Done                          =
            new ILangMsg(this, "You've learned new skill: &a%skill% %rlvl%");
    public ILangMsg Classes_Skill_Learn_Error_Has                     =
            new ILangMsg(this, "You're already know this skill!");
    public ILangMsg Classes_Skill_Learn_Error_TooExpensive            =
            new ILangMsg(this, "You don't have enought skill points to upgrade this skill!");
    public ILangMsg Classes_Leveling_Points_Aspect_Get                = new ILangMsg(this,
            "{message: ~prefix: false;}&2*** &aYou received &f%amount% Aspect Points&a! Spend them in &f/class aspects &2***");
    public ILangMsg Classes_Leveling_Points_Skill_Get                 = new ILangMsg(this,
            "{message: ~prefix: false;}&2*** &aYou received &f%amount% Skill Points&a! Spend them in &f/class skills &2***");
    public ILangMsg Classes_Leveling_Child_Available                  = new ILangMsg(this,
            "{message: ~prefix: false;}&eNew child class is available now! Check it in &6/class select&e.");
    public ILangMsg Classes_Leveling_Exp_Get                          =
            new ILangMsg(this, "{message: ~prefix: false;}&6*** &eYou got &f%exp% &eexp from &f%src% &6***");
    public ILangMsg Classes_Leveling_Exp_Lost                         =
            new ILangMsg(this, "{message: ~prefix: false;}&4*** &cYou lost &f%exp% &cexp &4***");
    public ILangMsg Classes_Leveling_Level_Up                         = new ILangMsg(this,
            "{message: ~prefix: false;}&6*** &eYour level have been increased to &f&l%lvl%&e! &6***");
    public ILangMsg Classes_Leveling_Level_Down                       = new ILangMsg(this,
            "{message: ~prefix: false;}&4*** &cYour level have been downgraded to &f&l%lvl%&c! &4***");
    public ILangMsg Classes_Select_Error_NoChildYet                   =
            new ILangMsg(this, "&cThere are no sub-classes available at the moment!");
    public ILangMsg Classes_Select_Error_NoChild                      =
            new ILangMsg(this, "&cYour class don't have sub-classes!");
    public ILangMsg Classes_Select_Error_Cooldown                     =
            new ILangMsg(this, "&cYou must wait &e%time% &cto change your class!");
    public ILangMsg Classes_Select_Error_Once                         =
            new ILangMsg(this, "&cYou can select a class only once! But you can create new profile for new character.");
    public ILangMsg Classes_Select_Done                               = new ILangMsg(this, "You're &a%class% &7now!");
    public ILangMsg CombatLog_Cmd_Log_Desc                            = new ILangMsg(this, "View latest combat log.");
    public ILangMsg Consumables_Consume_Error_FoodLevel               =
            new ILangMsg(this, "&cYou can not consume &e%item% &cbecause your food level is full.");
    public ILangMsg Consumables_Consume_Error_HealthLevel             =
            new ILangMsg(this, "&cYou can not consume &e%item% &cbecause your health is full.");
    public ILangMsg Dismantle_Cmd_Open_Desc                           = new ILangMsg(this, "Open Dismantle GUI.");
    public ILangMsg Dismantle_Cmd_Open_Usage                          =
            new ILangMsg(this, "[player] [force<true/false>]");
    public ILangMsg Dismantle_Cmd_Open_Done_Others                    =
            new ILangMsg(this, "Opened Dismantle GUI for &a%player%&7.");
    public ILangMsg Dismantle_Dismantle_Error_TooExpensive            =
            new ILangMsg(this, "&cYou can't arrord this operation! You need: &e$%cost%, &cyou have: &e$%balance%");
    public ILangMsg Dismantle_Dismantle_Single_Free                   =
            new ILangMsg(this, "&a%item%&7 dismantled!");
    public ILangMsg Dismantle_Dismantle_Single_Paid                   =
            new ILangMsg(this, "&a%item%&7 dismantled for &a$%cost%!");
    public ILangMsg Dismantle_Dismantle_Many_Free                     = new ILangMsg(this, "Items dismantled!");
    public ILangMsg Dismantle_Dismantle_Many_Paid                     =
            new ILangMsg(this, "Items dismantled for &a$%cost%&7!");
    public ILangMsg Extractor_Cmd_Open_Desc                           = new ILangMsg(this, "Opens Extractor GUI.");
    public ILangMsg Extractor_Cmd_Open_Usage                          =
            new ILangMsg(this, "[player] [force(true/false)]");
    public ILangMsg Extractor_Cmd_Open_Done_Others                    =
            new ILangMsg(this, "Opened Extractor for &a%player%&7.");
    public ILangMsg Extractor_Extract_Complete                        =
            new ILangMsg(this, "[TITLES] &a&lExtraction complete!");
    public ILangMsg Extractor_Extract_Error_TooExpensive              =
            new ILangMsg(this, "&cYou must have &e$%cost% &cto do the extraction! You only have &e$%balance%&c.");
    public ILangMsg Extractor_Open_Error_NoSockets                    =
            new ILangMsg(this, "&e%item% &chas nothing to extract!");
    public ILangMsg Fortify_Cmd_Fortify_Usage                         = new ILangMsg(this, "<item> <level>");
    public ILangMsg Fortify_Cmd_Fortify_Desc                          =
            new ILangMsg(this, "Fortifies the item with a specified stone.");
    public ILangMsg Fortify_Cmd_Fortify_Error_Stone                   = new ILangMsg(this, "Invalid fortified stone!");
    public ILangMsg Fortify_Cmd_Unfortify_Desc                        =
            new ILangMsg(this, "Removes fortify state from the item.");
    public ILangMsg Fortify_Fortify_Done                              = new ILangMsg(this, "Item fortified!");
    public ILangMsg Fortify_Fortify_Error_Already                     =
            new ILangMsg(this, "Item is already fortified!");
    public ILangMsg Fortify_Enchanting_Failure                        =
            new ILangMsg(this, "Fortified stone could not protect &c%item%&7.");
    public ILangMsg Fortify_Enchanting_Success                        =
            new ILangMsg(this, "Fortified stone saves &a%item%&7!");
    public ILangMsg Identify_Cmd_Identify_Desc                        =
            new ILangMsg(this, "Force identifies item in your hand.");
    public ILangMsg Identify_Cmd_Identify_Error_Item                  =
            new ILangMsg(this, "You must hold an &cUnidentified Item&7.");
    public ILangMsg Identify_Identify_Success                         =
            new ILangMsg(this, "Item identified: &a%item%&7!");
    public ILangMsg Identify_Identify_Error_Tome                      =
            new ILangMsg(this, "&clOops! &7This &cIdentify Tome &7can not identify this item.");
    public ILangMsg Identify_Usage_Error_Unidentified                 =
            new ILangMsg(this, "You can not equip/use an unidentified item!");
    public ILangMsg ItemGenerator_Cmd_Create_Desc                     =
            new ILangMsg(this, "Creates a new item generator.");
    public ILangMsg ItemGenerator_Cmd_Create_Done                     =
            new ILangMsg(this, "&aCreated %id%.yml item generator!");
    public ILangMsg ItemGenerator_Cmd_Create_Error_ExistingId         =
            new ILangMsg(this, "&cAn item generator with that id already exists.");
    public ILangMsg ItemGenerator_Cmd_Create_Error_ExistingFile       =
            new ILangMsg(this, "&cA file with that name already exists.");
    public ILangMsg ItemGenerator_Cmd_Editor_Desc                     =
            new ILangMsg(this, "Opens the ItemGenerator in-game editor.");
    public ILangMsg ItemGenerator_Cmd_Editor_Error_InvalidItem        =
            new ILangMsg(this, "&cNo such item generator!");
    public ILangMsg ItemGenerator_Cmd_Editor_Error_AlreadyOpen        =
            new ILangMsg(this, "&cAn editor for this item generator is already open by %player%");
    public ILangMsg ItemGenerator_Cmd_Editor_Error_InvalidInput       =
            new ILangMsg(this, "%input% &cis not a valid %value%");
    public ILangMsg Loot_Box_Error_NotOwner                           =
            new ILangMsg(this, "You're not obtain this loot.");
    public ILangMsg Loot_Box_Error_Locked                             =
            new ILangMsg(this, "Someone's already looting this...");
    public ILangMsg Loot_Box_Owner_None                               = new ILangMsg(this, "Free");
    public ILangMsg Loot_Party_Roll_Notify_List                       = new ILangMsg(
            this,
            "{message: ~prefix: false;}"
                    + "&8&m-----------&8&l[ &e&lRoll The Dice &8&l]&8&m-----------"
                    + "\n"
                    + "              &7Item: &a%item%&7!"
                    + "\n"
                    + "                      %roll%"
                    + "\n"
                    + "&8&m-----------&8&l[ &8&m-------------&8&l ]&8&m-----------");
    public ILangMsg Loot_Party_Roll_Notify_Roll_Name                  = new ILangMsg(this, "&a&l[Roll!]");
    public ILangMsg Loot_Party_Roll_Notify_Roll_Hint                  =
            new ILangMsg(this, "&aClick me to roll the dice!");
    public ILangMsg Loot_Party_Roll_MemberRoll                        =
            new ILangMsg(this, "%player% rolled the dice: &e%value%&7.");
    public ILangMsg Loot_Party_Roll_AlreadyRoll                       =
            new ILangMsg(this, "You've already rolled the dice. Your value: &e%value%&7.");
    public ILangMsg Loot_Party_Roll_RollIn                            =
            new ILangMsg(this, "Roll-dice time: &e%time% seconds&7.");
    public ILangMsg Loot_Party_Roll_NoRoll                            = new ILangMsg(this, "No one rolled the dice.");
    public ILangMsg Loot_Party_Roll_Winner                            =
            new ILangMsg(this, "&a%player% &7obtained the right to possession &a%item%&7.");
    public ILangMsg Loot_Party_Roll_NotOwner                          =
            new ILangMsg(this, "You're not obtain this item.");
    public ILangMsg Loot_Party_Roll_AlreadyStarted                    =
            new ILangMsg(this, "Your party already rolling the item.");
    public ILangMsg MagicDust_Cmd_Open_Desc                           = new ILangMsg(this, "Open Magic Dust GUI.");
    public ILangMsg MagicDust_Cmd_Open_Usage                          =
            new ILangMsg(this, "[player] [force(true/false)]");
    public ILangMsg MagicDust_Cmd_Open_Done_Others                    =
            new ILangMsg(this, "Opened Dust GUI for &a%player%&7.");
    public ILangMsg MagicDust_Apply_Done                              =
            new ILangMsg(this, "&aSuccess! &7New &f%item% &7success rate: &a%rate-new%%&7!");
    public ILangMsg MagicDust_Apply_Error_MaxRate                     =
            new ILangMsg(this, "&e%source% &ccould not add more than &e%max-rate%%&c!");
    public ILangMsg MagicDust_GUI_Error_TooExpensive                  = new ILangMsg(this,
            "&cYou could not pay for this operation! Required: &e$%cost%&c, you have: &e$%balance%&c.");
    public ILangMsg MagicDust_GUI_Error_InvalidItem                   =
            new ILangMsg(this, "&e%item% &cis not a valid item for this!");
    public ILangMsg Party_Cmd_Chat_Desc                               = new ILangMsg(this, "Toggles party chat mode.");
    public ILangMsg Party_Cmd_Create_Desc                             = new ILangMsg(this, "Create a party.");
    public ILangMsg Party_Cmd_Create_Usage                            = new ILangMsg(this, "[name]");
    public ILangMsg Party_Cmd_Disband_Desc                            = new ILangMsg(this, "Disband the party.");
    public ILangMsg Party_Cmd_Drop_Desc                               = new ILangMsg(this, "Toggle drop mode.");
    public ILangMsg Party_Cmd_Exp_Desc                                = new ILangMsg(this, "Toggle exp mode.");
    public ILangMsg Party_Cmd_Invite_Desc                             =
            new ILangMsg(this, "Invite player to the party.");
    public ILangMsg Party_Cmd_Invite_Usage                            = new ILangMsg(this, "<player>");
    public ILangMsg Party_Cmd_Join_Desc                               = new ILangMsg(this, "Join the party.");
    public ILangMsg Party_Cmd_Join_Usage                              = new ILangMsg(this, "<party>");
    public ILangMsg Party_Cmd_Kick_Desc                               =
            new ILangMsg(this, "Kick player from the party.");
    public ILangMsg Party_Cmd_Kick_Usage                              = new ILangMsg(this, "<player>");
    public ILangMsg Party_Cmd_Leave_Desc                              = new ILangMsg(this, "Leave the party.");
    public ILangMsg Party_Cmd_Menu_Desc                               = new ILangMsg(this, "Open party menu.");
    public ILangMsg Party_Cmd_Teleport_Desc                           =
            new ILangMsg(this, "Teleport to a party member.");
    public ILangMsg Party_Cmd_Teleport_Usage                          = new ILangMsg(this, "<player>");
    public ILangMsg Party_Cmd_Roll_Desc                               = new ILangMsg(this, "Roll the dice.");
    public ILangMsg Party_Cmd_Roll_Error_Nothing                      = new ILangMsg(this, "Nothing to roll.");
    public ILangMsg Party_Create_Done                                 =
            new ILangMsg(this, "Created a new party &a%party%&7. Invite players: &a/party invite <player>&7.");
    public ILangMsg Party_Create_Error_Exist                          =
            new ILangMsg(this, "Party with such name is already created.");
    public ILangMsg Party_Join_Done                                   =
            new ILangMsg(this, "&7You have joined the &a%party% &7party.");
    public ILangMsg Party_Join_New                                    =
            new ILangMsg(this, "&a%player% &7joined the party.");
    public ILangMsg Party_Leader_Transfer                             =
            new ILangMsg(this, "&7You're the new party leader now.");
    public ILangMsg Party_Invite_Already                              =
            new ILangMsg(this, "This player is already invited to your party.");
    public ILangMsg Party_Invite_Send                                 =
            new ILangMsg(this, "Invite sent to &e%player%&7.");
    public ILangMsg Party_Invite_Get                                  =
            new ILangMsg(this, "&e%leader% &7invites you to the &e%party% &7party. Type &e/party join %party%&7.");
    public ILangMsg Party_Invite_Another                              =
            new ILangMsg(this, "You don't have invite to this party.");
    public ILangMsg Party_Leave_Done                                  =
            new ILangMsg(this, "&7You left the &c%party% &7party.");
    public ILangMsg Party_Leave_Member                                =
            new ILangMsg(this, "&c%player% &7left the party.");
    public ILangMsg Party_Leave_QuitGame                              =
            new ILangMsg(this, "&c%player% &7left the game.");
    public ILangMsg Party_Leave_ComeBack                              =
            new ILangMsg(this, "&a%player% &7joined the game.");
    public ILangMsg Party_Disband_Done                                =
            new ILangMsg(this, "Party disbanded: &c%party%&7.");
    public ILangMsg Party_Disband_Leader                              =
            new ILangMsg(this, "&c%leader% &7disbands the party.");
    public ILangMsg Party_Kick_You                                    =
            new ILangMsg(this, "&c%leader% &7kicked you from the party.");
    public ILangMsg Party_Kick_Other                                  =
            new ILangMsg(this, "&c%leader% &7kicked &c%player% &7from the party.");
    public ILangMsg Party_Kick_Error_Self                             = new ILangMsg(this, "Unable to kick yourself.");
    public ILangMsg Party_Error_MaxPlayers                            =
            new ILangMsg(this, "There are maximum players in party.");
    public ILangMsg Party_Error_Player_AlreadyIn                      =
            new ILangMsg(this, "&c%player% &7is already in a party.");
    public ILangMsg Party_Error_Player_NotIn                          = new ILangMsg(this, "Player is not in party.");
    public ILangMsg Party_Error_AlreadyIn                             =
            new ILangMsg(this, "You're already in party! Leave it: &e/party leave&7.");
    public ILangMsg Party_Error_NotInParty                            = new ILangMsg(this, "You're not in the party.");
    public ILangMsg Party_Error_LeaderOnly                            =
            new ILangMsg(this, "Only party leader can do this.");
    public ILangMsg Party_Error_Invalid                               = new ILangMsg(this, "No such party.");
    public ILangMsg Party_Teleport_Error_Cooldown                     =
            new ILangMsg(this, "Teleport cooldown: &c%time%");
    public ILangMsg Party_Teleport_Error_Self                         = new ILangMsg(this, "Unable teleport to self!");
    public ILangMsg Party_Teleport_Done_To                            =
            new ILangMsg(this, "&8*** &7Teleport to &a%player%&7. &8***");
    public ILangMsg Party_Teleport_Done_From                          =
            new ILangMsg(this, "&8*** &a%player% &7teleported to you. &8***");
    public ILangMsg Party_Chat_Toggle                                 = new ILangMsg(this, "Party chat: &e%state%");
    public ILangMsg Party_Drop_Toggle                                 =
            new ILangMsg(this, "&8*** &7Drop mode: &e%mode% &8***");
    public ILangMsg Party_Exp_Toggle                                  =
            new ILangMsg(this, "&8*** &7Exp mode: &e%mode% &8***");
    public ILangMsg Profiles_Command_Profiles_Desc                    = new ILangMsg(this, "Open profile menu.");
    public ILangMsg Profiles_Create_Error_Regex                       =
            new ILangMsg(this, "&cProfile name contains unacceptable symbols!");
    public ILangMsg Profiles_Create_Error_Exists                      =
            new ILangMsg(this, "&cProfile with such name already exists!");
    public ILangMsg Profiles_Create_Error_Maximum                     =
            new ILangMsg(this, "&cYou have reached maximum &e(%amount%) &camount of profiles!");
    public ILangMsg Profiles_Create_Error_Unexpected                  =
            new ILangMsg(this, "&cAn unexpected error while creating profile! Please contact administrator.");
    public ILangMsg Profiles_Create_Tip_Name                          = new ILangMsg(this,
            "{message: ~type: TITLES; ~fadeIn: 20; ~stay: -1; ~fadeOut: 20;}&b&lNew Profile\n&7Enter profile name...");
    public ILangMsg Profiles_Create_Tip_Error                         = new ILangMsg(this,
            "{message: ~type: TITLES; ~fadeIn: 20; ~stay: 60; ~fadeOut: 20;}&c&lError!\n&7See details in chat.");
    public ILangMsg Profiles_Create_Tip_Done                          =
            new ILangMsg(this, "{message: ~type: TITLES; ~fadeIn: 20; ~stay: 60; ~fadeOut: 20;}&a&lProfile Created!");
    public ILangMsg Profiles_Switch_Done                              =
            new ILangMsg(this, "Profile switched: &a%profile%");
    public ILangMsg Refine_Cmd_Refine_Desc                            =
            new ILangMsg(this, "Force refines item in your hand.");
    public ILangMsg Refine_Cmd_Refine_Usage                           = new ILangMsg(this, "[ench. stone]");
    public ILangMsg Refine_Cmd_Refine_Done                            = new ILangMsg(this, "Item refined!");
    public ILangMsg Refine_Cmd_Refine_Error_Stone                     =
            new ILangMsg(this, "Invalid enchantment stone id.");
    public ILangMsg Refine_Cmd_Downgrade_Desc                         =
            new ILangMsg(this, "Force downgrades refined item in your hand.");
    public ILangMsg Refine_Cmd_Downgrade_Done                         = new ILangMsg(this, "Item downgraded!");
    public ILangMsg Refine_Enchanting_Error_MaxLevel                  =
            new ILangMsg(this, "&cCould not refine &e%item%&c: Maximal level.");
    public ILangMsg Refine_Enchanting_Error_WrongStone                =
            new ILangMsg(this,
                    "&cCould not refine &e%item%&c: Wrong Enchantment Stone. You should use: &e%stone%&c.");
    public ILangMsg Refine_Enchanting_Result_Total                    = new ILangMsg(
            this,
            "{message: ~prefix: false;}"
                    + "&6&m                   &e&l[&e Refine Result &6&l]&6&m                   &r"
                    + "\n"
                    + "&7"
                    + "\n"
                    + "&e▸ %state-target% %item-target%"
                    + "\n"
                    + "&e▸ %state-source% %item-source%"
                    + "\n"
                    + "&7"
                    + "\n");
    public ILangMsg Refine_Enchanting_Result_State_Success            = new ILangMsg(this, "&a(Success) &f");
    public ILangMsg Refine_Enchanting_Result_State_Destroyed          = new ILangMsg(this, "&c(Destroyed) &f");
    public ILangMsg Refine_Enchanting_Result_State_Downgraded         = new ILangMsg(this, "&d(Downgraded) &f");
    public ILangMsg Refine_Enchanting_Result_State_Consumed           = new ILangMsg(this, "&9(Consumed) &f");
    public ILangMsg Refine_Enchanting_Result_State_Saved              = new ILangMsg(this, "&d(Saved) &f");
    public ILangMsg Repair_Cmd_Open_Desc                              =
            new ILangMsg(this, "Open custom repair anvil GUI.");
    public ILangMsg Repair_Cmd_Open_Usage                             =
            new ILangMsg(this, "[player] [force(true/false)]");
    public ILangMsg Repair_Cmd_Open_Done_Others                       =
            new ILangMsg(this, "Opened Anvil Repair GUI for &a%player%&7.");
    public ILangMsg Repair_Error_TypeNotSelected                      =
            new ILangMsg(this, "&cPlease, select the repair type!");
    public ILangMsg Repair_Error_NoDurability                         =
            new ILangMsg(this, "&c%item% &7can not be repaired.");
    public ILangMsg Repair_Error_NotDamaged                           =
            new ILangMsg(this, "&c%item% &7is not damaged.");
    public ILangMsg Repair_Error_TooExpensive                         =
            new ILangMsg(this, "&cYou can't afford the repair!");
    public ILangMsg Repair_Done                                       =
            new ILangMsg(this, "&a%item% &7have been successfully repaired.");
    public ILangMsg Sell_Cmd_Open_Desc                                = new ILangMsg(this, "Open Sell GUI.");
    public ILangMsg Sell_Cmd_Open_Usage                               =
            new ILangMsg(this, "[player] [force<true/false>]");
    public ILangMsg Sell_Cmd_Open_Done_Others                         =
            new ILangMsg(this, "Opened Sell GUI for &a%player%&7.");
    public ILangMsg Sell_Sell_Complete                                = new ILangMsg(this, "Sold for &a$%cost%&7!");
    public ILangMsg Soulbound_Cmd_Soul_Desc                           =
            new ILangMsg(this, "Manage item soulbound requirement.");
    public ILangMsg Soulbound_Cmd_Soul_Usage                          = new ILangMsg(this, "<add|remove> [position]");
    public ILangMsg Soulbound_Cmd_Soul_Done                           =
            new ILangMsg(this, "Item soulbound requirement: &e%state%");
    public ILangMsg Soulbound_Cmd_Untradeable_Desc                    =
            new ILangMsg(this, "Manage item trade state.");
    public ILangMsg Soulbound_Cmd_Untradeable_Usage                   = new ILangMsg(this, "<add|remove> [position]");
    public ILangMsg Soulbound_Cmd_Untradeable_Done                    =
            new ILangMsg(this, "Item untradeable: &e%state%");
    public ILangMsg Soulbound_Item_Soulbound_Apply                    =
            new ILangMsg(this, "&cSoulbound &eapplied to &c%item%&e!");
    public ILangMsg Soulbound_Item_Interact_Error_Pickup              =
            new ILangMsg(this, "&cYou can't pickup item you don't own.");
    public ILangMsg Soulbound_Item_Interact_Error_Command             =
            new ILangMsg(this, "&cYou can not use that command while holding untradeable item!");
    public ILangMsg Error_InvalidItem                                 =
            new ILangMsg(this, "This action is not applicable to this item!");
    public ILangMsg Error_InvalidArgument                             = new ILangMsg(this, "Invalid value: &c%arg%&7!");
    public ILangMsg Error_Internal                                    =
            new ILangMsg(this, "&cInternal error! Contact administration.");
    public ILangMsg Other_Get                                         =
            new ILangMsg(this, "You received: &7<%item%&7>");

    public Lang(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    protected void setupEnums() {
        this.setupEnum(PartyManager.PartyDropMode.class);
        this.setupEnum(PartyManager.PartyExpMode.class);
        this.setupEnum(ClickType.class);

        this.setupEnum(ClassAttributeType.class);
        this.setupEnum(UserEntityNamesMode.class);
    }
}
