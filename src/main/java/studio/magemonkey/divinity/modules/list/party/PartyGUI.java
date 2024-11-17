package studio.magemonkey.divinity.modules.list.party;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.gui.*;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;

import java.util.ArrayList;
import java.util.List;

public class PartyGUI extends NGUI<Divinity> {

    private PartyManager partyManager;

    private String       memberName;
    private List<String> memberLore;
    private int[]        memberSlots;

    public PartyGUI(@NotNull PartyManager partyManager) {
        super(partyManager.plugin, partyManager.getJYML(), "gui.");
        this.partyManager = partyManager;

        JYML   cfg  = partyManager.getJYML();
        String path = "gui.";

        this.memberName = StringUT.color(cfg.getString(path + "member.name", "&eParty Member"));
        this.memberLore = StringUT.color(cfg.getStringList(path + "member.lore"));
        this.memberSlots = cfg.getIntArray(path + "member.slots");

        GuiClick click = (p, type, e) -> {
            if (type == null) return;

            PartyManager.PartyMember member = partyManager.getPartyMember(p);
            if (member == null) {
                p.closeInventory();
                return;
            }

            if (type == ContentType.EXIT) {
                p.closeInventory();
                return;
            }

            if (!type.getClass().equals(GUIPartyType.class)) return;
            GUIPartyType type2 = (GUIPartyType) type;

            switch (type2) {
                case PARTY_DISBAND: {
                    partyManager.disbandParty(p);
                    p.closeInventory();
                    break;
                }
                case PARTY_LEAVE: {
                    member.leaveParty();
                    p.closeInventory();
                    break;
                }
                case PARTY_DROP: {
                    partyManager.togglePartyDrop(p);
                    open(p, 1);
                    break;
                }
                case PARTY_EXP: {
                    partyManager.togglePartyExp(p);
                    open(p, 1);
                    break;
                }
                case PARTY_CHAT: {
                    partyManager.toggleChat(p);
                    break;
                }
                default: {
                    break;
                }
            }
        };

        for (String itemId : cfg.getSection(path + "content")) {
            GuiItem guiItem = cfg.getGuiItem(path + "content." + itemId, GUIPartyType.class);
            if (guiItem == null) continue;

            if (guiItem.getType() != null) {
                guiItem.setClick(click);
            }

            this.addButton(guiItem);
        }
    }

    @Override
    protected void onCreate(@NotNull Player player, @NotNull Inventory inv, int page) {
        PartyManager.Party party = partyManager.getPlayerParty(player);
        if (party == null) {
            plugin.lang().Party_Error_NotInParty.send(player);
            return;
        }

        int j = 0;
        for (PartyManager.PartyMember member : party.getMembers()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta == null) continue;

            String online = plugin.lang().getBool(member.getPlayer() != null);

            meta.setDisplayName(this.memberName
                    .replace("%online%", online)
                    .replace("%name%", member.getName()));

            List<String> lore = new ArrayList<>(this.memberLore);
            lore.replaceAll(str -> str
                    .replace("%online%", online)
                    .replace("%name%", member.getName()));
            meta.setLore(lore);
            meta.setOwningPlayer(plugin.getServer().getOfflinePlayer(member.getUUID()));
            head.setItemMeta(meta);

            JIcon icon = new JIcon(head);
            icon.setClick((p, type, e) -> {
                if (e.isLeftClick()) {
                    partyManager.teleport(p, member.getPlayer());
                } else if (e.isRightClick()) {
                    partyManager.kickFromParty(p, member.getPlayer());
                    open(p, 1);
                }
            });
            this.addButton(player, icon, this.memberSlots[j++]);
        }
    }

    @Override
    protected boolean cancelClick(int slot) {
        return true;
    }

    @Override
    protected boolean cancelPlayerClick() {
        return true;
    }

    @Override
    protected boolean ignoreNullClick() {
        return true;
    }

    @Override
    protected void replaceMeta(@NotNull Player player, @NotNull ItemStack item, @NotNull GuiItem guiItem) {
        super.replaceMeta(player, item, guiItem);

        PartyManager.Party party = partyManager.getPlayerParty(player);
        if (party == null) return;
        PartyManager.PartyMember leader = party.getLeader();

        String dropMode   = plugin.lang().getEnum(party.getDropMode());
        String expMode    = plugin.lang().getEnum(party.getExpMode());
        String leaderName = leader != null ? leader.getName() : "null";
        String memSize    = String.valueOf(party.getMembers().size());
        String name       = party.getId();
        String fullSize   = String.valueOf(party.getSize());

        ItemUT.replace(item, str -> str
                .replace("%party_drop%", dropMode)
                .replace("%party_exp%", expMode)
                .replace("%party_leader%", leaderName)
                .replace("%party_members%", memSize)
                .replace("%party_name%", name)
                .replace("%party_size%", fullSize)
        );
    }

    static enum GUIPartyType {

        PARTY_MEMBER,
        PARTY_DISBAND,
        PARTY_LEAVE,
        PARTY_DROP,
        PARTY_EXP,
        PARTY_CHAT,
        ;
    }
}
