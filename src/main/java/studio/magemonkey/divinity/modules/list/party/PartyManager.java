package studio.magemonkey.divinity.modules.list.party;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Keyed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.ILangMsg;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.manager.api.task.ITask;
import studio.magemonkey.codex.util.CollectionsUT;
import studio.magemonkey.codex.util.SoundUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.codex.util.TimeUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.hooks.EHook;
import studio.magemonkey.divinity.hooks.HookLevel;
import studio.magemonkey.divinity.hooks.external.FabledHook;
import studio.magemonkey.divinity.hooks.internal.DivinityHook;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.api.QModule;
import studio.magemonkey.divinity.modules.list.party.command.*;
import studio.magemonkey.divinity.modules.list.party.compat.level.IPEFabled;
import studio.magemonkey.divinity.modules.list.party.compat.level.IPEInternal;
import studio.magemonkey.divinity.modules.list.party.compat.level.IPartyLevelManager;
import studio.magemonkey.divinity.modules.list.party.compat.quest.IPOMangoQuest;
import studio.magemonkey.divinity.modules.list.party.compat.quest.IPartyObjective;
import studio.magemonkey.divinity.modules.list.party.event.PlayerLeavePartyEvent;

import java.util.*;
import java.util.Map.Entry;

public class PartyManager extends QModule {

    private Map<String, Party>             parties;
    private Map<String, Map<String, Long>> invites;
    private Map<String, Long>              tpCooldown;

    private PartyGUI      gui;
    private PartySettings settings;

    private IPartyLevelManager iLevelManager;
    private IPartyObjective    iQuestObjective;

    private QuitTask taskQuit;

    public PartyManager(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.PARTY;
    }

    @Override
    @NotNull
    public String version() {
        return "2.0.0";
    }

    @Override
    public void setup() {
        this.parties = new HashMap<>();
        this.invites = new HashMap<>();
        this.tpCooldown = new HashMap<>();

        this.moduleCommand.addSubCommand(new PartyChatCmd(this));
        this.moduleCommand.addSubCommand(new PartyCreateCmd(this));
        this.moduleCommand.addSubCommand(new PartyDisbandCmd(this));
        this.moduleCommand.addSubCommand(new PartyExpCmd(this));
        this.moduleCommand.addSubCommand(new PartyInviteCmd(this));
        this.moduleCommand.addSubCommand(new PartyJoinCmd(this));
        this.moduleCommand.addSubCommand(new PartyKickCmd(this));
        this.moduleCommand.addSubCommand(new PartyLeaveCmd(this));
        this.moduleCommand.addSubCommand(new PartyMenuCmd(this));

        this.moduleCommand.addSubCommand(new PartyTpCmd(this));

        if (!plugin.cfg().isModuleEnabled(EModule.LOOT)) {
            this.info("Loot module is disabled. Party drop mode will disabled.");
        } else {
            this.moduleCommand.addSubCommand(new PartyDropCmd(this));
            this.moduleCommand.addSubCommand(new PartyRollCmd(this));
        }

        this.settings = new PartySettings(cfg);
        this.gui = new PartyGUI(this);

        // Level plugin compatibility
        if (this.settings.isLevelHooksEnabled()) {
            if (this.settings.isLevelExpBalance()) {
                HookLevel hLvl = EngineCfg.HOOK_PLAYER_LEVEL_PLUGIN;

                if (hLvl instanceof FabledHook) {
                    this.iLevelManager = new IPEFabled(plugin, this);
                } else if (hLvl instanceof DivinityHook) {
                    this.iLevelManager = new IPEInternal(plugin, this);
                }

                if (this.iLevelManager != null) {
                    this.iLevelManager.setup();
                }
            }
        }

        // Quest plugin compatibility
        if (this.settings.isQuestHooksEnabled()) {
//			if (Hooks.hasPlugin(EHook.QUEST_CREATOR)) {
//				this.iQuestObjective = new IPOQuestCreator(plugin, this);
//			}
            /*else */
            if (Hooks.hasPlugin(EHook.MANGO_QUEST)) {
                this.iQuestObjective = new IPOMangoQuest(plugin, this);
            }

            if (this.iQuestObjective != null) {
                this.iQuestObjective.setup();
            }
        }

        this.taskQuit = new QuitTask(this.plugin);
        this.taskQuit.start();
    }

    @Override
    public void shutdown() {
        if (this.taskQuit != null) {
            this.taskQuit.stop();
            this.taskQuit = null;
        }

        if (this.gui != null) {
            this.gui.shutdown();
            this.gui = null;
        }

        if (this.iLevelManager != null) {
            this.iLevelManager.shutdown();
            this.iLevelManager = null;
        }
        if (this.iQuestObjective != null) {
            this.iQuestObjective.shutdown();
            this.iQuestObjective = null;
        }

        for (Party p : this.getParties()) {
            p.disband();
        }

        if (this.parties != null) {
            this.parties.clear();
            this.parties = null;
        }
        if (this.invites != null) {
            this.invites.clear();
            this.invites = null;
        }
        if (this.tpCooldown != null) {
            this.tpCooldown.clear();
            this.tpCooldown = null;
        }

        this.settings = null;
    }

    // -------------------------------------------------------

    public void toggleChat(@NotNull Player player) {
        PartyMember member = this.getPartyMember(player);
        if (member == null) {
            plugin.lang().Party_Error_NotInParty.send(player);
            return;
        }

        member.toggleChat();
    }

    public boolean isInParty(@NotNull Player player) {
        return this.getPartyMember(player) != null;
    }

    @Nullable
    public Party getPlayerParty(@NotNull Player player) {
        for (Party party : this.getParties()) {
            if (party.isMember(player)) {
                return party;
            }
        }
        return null;
    }

    @Nullable
    public PartyMember getPartyMember(@NotNull Player player) {
        Party party = this.getPlayerParty(player);
        if (party != null) {
            return party.getMember(player);
        }
        return null;
    }

    public void createParty(@NotNull Player creator, String name) {
        if (this.isInParty(creator)) {
            plugin.lang().Party_Error_AlreadyIn.send(creator);
            return;
        }

        if (name == null) {
            name = creator.getName();
        }
        name = StringUT.oneSpace(name).replace(" ", "_").trim().toLowerCase();

        if (this.getPartyById(name) != null) {
            plugin.lang().Party_Create_Error_Exist.send(creator);
            return;
        }

        int size = this.settings.getPartyPermSize(creator);

        Party party = new Party(name, creator, size);
        this.parties.put(party.getId(), party);

        plugin.lang().Party_Create_Done.replace("%party%", name).send(creator);
        settings.playSound(creator, PartyAction.CREATE);
    }

    public void joinParty(@NotNull Player player, @NotNull String id) {
        this.updateInvites(player);

        if (this.isInParty(player)) {
            plugin.lang().Party_Error_AlreadyIn.send(player);
            return;
        }

        Party party = this.getPartyById(id);
        if (party == null) {
            plugin.lang().Party_Error_Invalid.send(player);
            return;
        }

        if (!this.hasInvite(player, party)) {
            plugin.lang().Party_Invite_Another.send(player);
            return;
        }

        if (party.getMembers().size() >= party.getSize()) {
            plugin.lang().Party_Error_MaxPlayers.send(player);
            return;
        }

        party.addMember(player);
    }

    public void invitePlayer(@NotNull Player from, @Nullable Player to) {
        PartyMember leader = this.getPartyMember(from);
        if (leader == null) {
            plugin.lang().Party_Error_NotInParty.send(from);
            return;
        }
        if (!leader.isLeader()) {
            plugin.lang().Party_Error_LeaderOnly.send(from);
            return;
        }

        if (to == null) {
            plugin.lang().Error_NoPlayer.send(from);
            return;
        }

        if (this.isInParty(to)) {
            plugin.lang().Party_Error_Player_AlreadyIn.replace("%player%", to.getName()).send(from);
            return;
        }

        Party party = leader.getParty();

        if (party.getMembers().size() >= party.getSize()) {
            plugin.lang().Party_Error_MaxPlayers.send(from);
            return;
        }

        if (this.hasInvite(to, party)) {
            plugin.lang().Party_Invite_Already.send(from);
            return;
        }

        this.addInvite(to, party);

        plugin.lang().Party_Invite_Send.replace("%player%", to.getName()).send(from);
        plugin.lang().Party_Invite_Get
                .replace("%leader%", from.getName())
                .replace("%party%", party.getId())
                .send(to);

        settings.playSound(to, PartyAction.INVITE);
    }

    private void addInvite(@NotNull Player player, @NotNull Party party) {
        String            key = player.getName();
        Map<String, Long> map;

        if (this.invites.containsKey(key)) {
            map = this.invites.get(key);
        } else {
            map = new HashMap<>();
        }

        map.put(party.getId(), System.currentTimeMillis() + settings.partyInviteTime * 1000L);
        this.invites.put(key, map);
    }

    public boolean hasInvite(@NotNull Player p, @NotNull Party party) {
        this.updateInvites(p); // Remove expired and invalid

        String key = p.getName();
        if (this.invites.containsKey(key)) {
            Map<String, Long> map = this.invites.get(key);
            return map.containsKey(party.getId());
        }
        return false;
    }

    private void updateInvites(@NotNull Player player) {
        String key = player.getName();

        if (this.invites.containsKey(key)) {
            Map<String, Long> map = this.invites.get(key);

            // Remove expired or invalid parties
            for (Entry<String, Long> e : new HashSet<>(map.entrySet())) {
                if (System.currentTimeMillis() > e.getValue()
                        || this.getPartyById(e.getKey()) == null) {
                    map.remove(e.getKey());
                }
            }

            // Update values
            if (map.isEmpty()) {
                this.invites.remove(key);
            } else {
                this.invites.put(key, map);
            }
        }
    }

    public void kickFromParty(@NotNull Player lead, @Nullable Player who) {
        PartyMember leader = this.getPartyMember(lead);
        if (leader == null) {
            plugin.lang().Party_Error_NotInParty.send(lead);
            return;
        }
        if (!leader.isLeader()) {
            plugin.lang().Party_Error_LeaderOnly.send(lead);
            return;
        }
        if (who == null) {
            plugin.lang().Error_NoPlayer.send(lead);
            return;
        }

        Party       party = leader.getParty();
        PartyMember liver = this.getPartyMember(who);

        if (liver == null || !party.isMember(liver)) {
            plugin.lang().Party_Error_Player_NotIn.replace("%player%", who.getName()).send(lead);
            return;
        }

        if (leader.equals(liver)) {
            plugin.lang().Party_Kick_Error_Self.send(lead);
            return;
        }

        party.delMember(liver);

        for (PartyMember pm : party.getMembers()) {
            Player p1 = pm.getPlayer();
            if (p1 == null) continue;

            plugin.lang().Party_Kick_Other
                    .replace("%leader%", lead.getName())
                    .replace("%player%", who.getName())
                    .send(p1);

            settings.playSound(p1, PartyAction.KICK);
        }

        plugin.lang().Party_Kick_You
                .replace("%leader%", lead.getName())
                .replace("%party%", party.getId())
                .send(who);

        settings.playSound(who, PartyAction.KICK);
    }

    public void disbandParty(@NotNull Player leader) {
        PartyMember member = this.getPartyMember(leader);
        if (member == null) {
            plugin.lang().Party_Error_NotInParty.send(leader);
            return;
        }
        if (!member.isLeader()) {
            plugin.lang().Party_Error_LeaderOnly.send(leader);
            return;
        }

        Party party = member.getParty();
        party.disband();
        party = null;
    }

    public void teleport(@NotNull Player from, @Nullable Player to) {
        PartyMember member = this.getPartyMember(from);
        if (member == null) {
            plugin.lang().Party_Error_NotInParty.send(from);
            return;
        }

        if (to == null) {
            plugin.lang().Error_NoPlayer.send(from);
            return;
        }

        if (from.equals(to)) {
            plugin.lang().Party_Teleport_Error_Self.send(from);
            return;
        }

        long time = System.currentTimeMillis();
        if (tpCooldown.containsKey(from.getName())) {
            long time_cd = tpCooldown.get(from.getName());
            if (time < time_cd) {
                String lost = TimeUT.formatTimeLeft(time_cd, time);
                plugin.lang().Party_Teleport_Error_Cooldown.replace("%time%", lost).send(from);
                return;
            }
        }

        Party party = member.getParty();
        if (!party.isMember(to)) {
            plugin.lang().Party_Error_Player_NotIn.replace("%player%", to.getName()).send(from);
            return;
        }

        party.teleport(from, to);
    }

    public void togglePartyDrop(@NotNull Player player) {
        PartyMember member = getMembership(player);
        if (member != null) member.getParty().toggleDrop();
    }

    public void togglePartyExp(@NotNull Player player) {
        PartyMember member = getMembership(player);
        if (member != null) member.getParty().toggleExp();
    }

    public PartyMember getMembership(Player player) {
        PartyMember member = this.getPartyMember(player);
        if (member == null) {
            plugin.lang().Party_Error_NotInParty.send(player);
        } else if (!member.isLeader()) {
            plugin.lang().Party_Error_LeaderOnly.send(player);
            return null;
        }

        return member;
    }

    @Nullable
    public Party getPartyById(String id) {
        return this.parties.get(id.toLowerCase());
    }

    @NotNull
    public Collection<String> getPartyIds() {
        return parties.keySet();
    }

    @NotNull
    public Collection<Party> getParties() {
        return parties.values();
    }

    public void openPartyGUI(@NotNull Player player) {
        this.gui.open(player, 1);
    }

    @NotNull
    public PartySettings getSettings() {
        return this.settings;
    }

    // ---------------------------------------------------------------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPartyChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();

        PartyMember member = this.getPartyMember(player);
        if (member == null) return;
        if (!member.isInChat()) return;

        e.getRecipients().clear();

        String format = settings.getChatFormat()
                .replace("{player}", "%1$s")
                .replace("{message}", "%2$s");

        if (Hooks.hasPlaceholderAPI()) {
            format = PlaceholderAPI.setPlaceholders(player, format);
        }
        if (settings.cancelMainChatEvent()) {
            format = format.replace("%1$s", player.getDisplayName())
                    .replace("%2$s", e.getMessage());
            e.setCancelled(true);
            for (PartyMember mem : member.getParty().getMembers()) {
                Player friend = mem.getPlayer();
                if (friend == null) continue;

                friend.sendMessage(format);
            }
        } else {
            e.setFormat(format);

            for (PartyMember friend : member.getParty().getMembers()) {
                Player pFriend = friend.getPlayer();
                if (pFriend == null) continue;

                e.getRecipients().add(pFriend);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPartyMemberDamage(EntityDamageByEntityEvent e) {
        Entity e1 = e.getEntity();
        if (!(e1 instanceof Player)) return;

        Player victim  = (Player) e1;
        Player damager = null;

        Entity e2 = e.getDamager();
        if (e2 instanceof Player) {
            damager = (Player) e2;
        } else if (e2 instanceof Projectile) {
            Projectile       pj = (Projectile) e2;
            ProjectileSource ps = pj.getShooter();
            if (ps != null && ps instanceof Player) {
                damager = (Player) ps;
            }
        }
        if (damager == null) return;

        Party partyVictim  = this.getPlayerParty(victim);
        Party partyDamager = this.getPlayerParty(damager);
        if (partyVictim != null && partyDamager != null && partyVictim.equals(partyDamager)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPartyQuitGame(PlayerQuitEvent e) {
        Player      player = e.getPlayer();
        PartyMember member = this.getPartyMember(player);
        if (member == null) return;

        if (settings.autoLeaveOnQuit()) {
            member.leaveParty();
        } else {
            Party party = member.getParty();

            // Send message when player quit the game,
            // but not quit the party
            for (PartyMember friend : party.getMembers()) {
                Player pFriend = friend.getPlayer();
                if (pFriend == null) continue;

                plugin.lang().Party_Leave_QuitGame
                        .replace("%player%", member.getName())
                        .send(pFriend);

                settings.playSound(pFriend, PartyAction.QUIT);
            }

            if (member.isLeader()) {
                party.transferLeader();
            }
            member.setQuitTime();
        }
    }

    @EventHandler
    public void onPartyJoinBack(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        PartyMember memberBack = this.getPartyMember(player);
        if (memberBack == null) {
            return;
        }

        // Stop auto-kick
        memberBack.resetQuitTime();

        // Send member back message
        for (PartyMember member : memberBack.getParty().getMembers()) {
            Player pFriend = member.getPlayer();
            if (pFriend == null || member.equals(memberBack)) continue;

            plugin.lang().Party_Leave_ComeBack
                    .replace("%player%", memberBack.getName())
                    .send(pFriend);

            settings.playSound(pFriend, PartyAction.BACK);
        }
    }

    // ---------------------------------------------------------------

    public class PartyMember {

        private final UUID    uuid;
        private final String  name;
        private       boolean chat;
        private       long    quitTime;
        private       boolean isLeader;
        private final Party   party;

        public PartyMember(@NotNull Player player, @NotNull Party party) {
            this.uuid = player.getUniqueId();
            this.name = player.getName();
            this.chat = false;
            this.quitTime = 0L;
            this.isLeader = false;
            this.party = party;
        }

        @NotNull
        public UUID getUUID() {
            return this.uuid;
        }

        @NotNull
        public String getName() {
            return this.name;
        }

        public boolean isInChat() {
            return this.chat;
        }

        public boolean isLeader() {
            return this.isLeader;
        }

        public void setLeader(boolean isLeader) {
            this.isLeader = isLeader;
        }

        public void toggleChat() {
            this.chat = !this.chat;

            Player p = this.getPlayer();
            if (p == null) return;

            plugin.lang().Party_Chat_Toggle
                    .replace("%state%", plugin.lang().getBool(this.isInChat()))
                    .send(p);
        }

        public void setQuitTime() {
            this.quitTime = System.currentTimeMillis() + settings.getQuitTimeout() * 60L * 1000L;
        }

        public void resetQuitTime() {
            this.quitTime = 0L;
        }

        public boolean isQuitTime() {
            return this.quitTime > 0 && System.currentTimeMillis() >= this.quitTime;
        }

        @Nullable
        public Player getPlayer() {
            return plugin.getServer().getPlayer(this.uuid);
        }

        @NotNull
        public Party getParty() {
            return this.party;
        }

        public void leaveParty() {
            PlayerLeavePartyEvent event = new PlayerLeavePartyEvent(this.getPlayer(), this);
            plugin.getPluginManager().callEvent(event);

            this.party.delMember(this);

            Player player = this.getPlayer();
            if (player != null) {
                plugin.lang().Party_Leave_Done
                        .replace("%party%", this.party.getId())
                        .send(player);
            }

            if (this.party.getMembers().isEmpty() || (settings.disbandOnLeaderQuit() && this.isLeader())) {
                this.party.disband();
                return;
            } else {
                for (PartyMember member : this.party.getMembers()) {
                    Player pFriend = member.getPlayer();
                    if (pFriend == null) continue;

                    plugin.lang().Party_Leave_Member
                            .replace("%player%", this.getName())
                            .send(pFriend);

                    settings.playSound(pFriend, PartyAction.LEAVE);
                }

                if (this.isLeader()) {
                    this.party.transferLeader();
                }
            }
        }
    }

    public class Party {

        private final String                 id;
        private final Map<UUID, PartyMember> members;
        private       int                    size;
        private       PartyDropMode          drop;
        private       PartyExpMode           expMode;

        public Party(@NotNull String id, @NotNull Player leader, int size) {
            this.id = id.toLowerCase();
            this.members = new LinkedHashMap<>();
            this.setSize(size);
            this.drop = PartyDropMode.FREE;
            this.expMode = PartyExpMode.SHARED;

            this.addMember(leader).setLeader(true);
        }

        @NotNull
        public String getId() {
            return this.id;
        }

        @Nullable
        public PartyMember getLeader() {
            for (PartyMember member : this.getMembers()) {
                if (member.isLeader) return member;
            }
            return null;
        }

        public void setLeader(@NotNull PartyMember leader) {
            for (PartyMember member : this.getMembers()) {
                if (member.isLeader) {
                    member.setLeader(false);
                }
            }
            leader.setLeader(true);
        }

        public void transferLeader() {
            for (PartyMember member : this.getMembers()) {
                if (member.isLeader()) continue;

                Player player = member.getPlayer();
                if (player == null) continue;

                this.setLeader(member);
                plugin.lang().Party_Leader_Transfer.send(player);
            }
        }

        public int getOnline() {
            int online = 0;
            for (PartyMember member : this.getMembers()) {
                Player player = member.getPlayer();
                if (player == null) continue;

                online++;
            }
            return online;
        }

        @NotNull
        public Collection<PartyMember> getMembers() {
            return this.members.values();
        }

        @Nullable
        public PartyMember getMember(@NotNull Player player) {
            return this.getMember(player.getUniqueId());
        }

        @Nullable
        public PartyMember getMember(UUID uuid) {
            return this.members.get(uuid);
        }

        public int getSize() {
            return this.size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        @NotNull
        public PartyDropMode getDropMode() {
            return this.drop;
        }

        public void setDropMode(@NotNull PartyDropMode drop) {
            this.drop = drop;
        }

        @NotNull
        public PartyExpMode getExpMode() {
            return this.expMode;
        }

        public boolean isMember(@NotNull Player player) {
            return this.members.containsKey(player.getUniqueId());
        }

        public boolean isMember(@NotNull PartyMember member) {
            return this.members.containsKey(member.getUUID());
        }

        public PartyMember addMember(@NotNull Player player) {
            plugin.lang().Party_Join_Done
                    .replace("%party%", this.getId())
                    .send(player);

            for (PartyMember member : this.getMembers()) {
                Player pFriend = member.getPlayer();
                if (pFriend == null) continue;

                plugin.lang().Party_Join_New.replace("%player%", player.getName()).send(pFriend);
                settings.playSound(pFriend, PartyAction.JOIN);
            }

            PartyMember member = new PartyMember(player, this);
            this.members.put(member.getUUID(), member);
            return member;
        }

        public void delMember(@NotNull PartyMember member) {
            this.members.remove(member.getUUID());
        }

        public void disband() {
            // Del players
            PartyMember leader = this.getLeader();
            if (leader != null) {
                Player lead = leader.getPlayer();
                if (lead != null) {
                    plugin.lang().Party_Disband_Done.replace("%party%", getId()).send(lead);
                }
            }

            for (PartyMember member : this.getMembers()) {
                Player player = member.getPlayer();
                if (player == null) continue;

                if (!member.isLeader()) {
                    plugin.lang().Party_Disband_Leader
                            .replace("%leader%", leader != null ? leader.getName() : "???")
                            .send(player);
                }
                //toggleChat(p, false);
                settings.playSound(player, PartyAction.DISBAND);
            }
            this.members.clear();
            parties.remove(this.id);
        }

        public void teleport(@NotNull Player from, @NotNull Player to) {
            from.teleport(to);

            plugin.lang().Party_Teleport_Done_To.replace("%player%", to.getName()).send(from);
            plugin.lang().Party_Teleport_Done_From.replace("%player%", from.getName()).send(to);

            settings.playSound(from, PartyAction.TP);
            settings.playSound(to, PartyAction.TP);

            tpCooldown.put(from.getName(), System.currentTimeMillis() + 1000L * settings.getTeleportCooldown());
        }

        public void sendMessage(ILangMsg msg) {
            for (PartyMember member : this.getMembers()) {
                Player player = member.getPlayer();
                if (player == null) continue;

                msg.send(player);
            }
        }

        public void sendMessage(@NotNull PartyMember from, ILangMsg msg, int dist) {
            for (PartyMember member : this.getMembersByDistance(from, dist)) {
                Player player = member.getPlayer();
                if (player == null) continue;

                msg.send(player);
            }
        }

        public void toggleDrop() {
            this.drop = CollectionsUT.toggleEnum(this.drop);

            for (PartyMember pm : getMembers()) {
                Player p = pm.getPlayer();
                if (p == null) continue;

                String dropMode = plugin.lang().getEnum(this.drop);
                plugin.lang().Party_Drop_Toggle.replace("%mode%", dropMode).send(p);
            }
        }

        public void toggleExp() {
            this.expMode = CollectionsUT.toggleEnum(this.expMode);

            for (PartyMember pm : getMembers()) {
                Player p = pm.getPlayer();
                if (p == null) continue;

                String dropMode = plugin.lang().getEnum(this.expMode);
                plugin.lang().Party_Exp_Toggle.replace("%mode%", dropMode).send(p);
            }
        }

        public Set<PartyMember> getMembersByDistance(@NotNull PartyMember from, int dist) {
            Player killer = from.getPlayer();
            if (killer == null) return Collections.emptySet();

            Set<PartyMember> objGeters = new HashSet<>();

            // Fix geters
            for (PartyMember member : this.getMembers()) {
                if (member.equals(from)) continue; // Skip event executor
                Player player = member.getPlayer();
                if (player == null) continue; // Skip offline
                if (dist > 0) {
                    if (!player.getWorld().equals(killer.getWorld())) continue;
                    if (player.getLocation().distance(killer.getLocation()) > dist) continue;
                }

                objGeters.add(member);
            }

            return objGeters;
        }
    }

    public class PartySettings {

        private final int     partyMaxSize;
        private final boolean partyLeadQuitDisband;
        private final int     partyInviteTime;
        private final boolean partyQuitInstLeave;
        private final int     partyQuitWaitTime;
        private final int     partyTeleportCd;

        private final boolean partyChatEnabled;
        private final String  partyChatFormat;
        private final boolean partyChatCancel;

        private final Map<String, Integer> partySizeByRank;

        private final boolean hookLevelEnabled;
        private final boolean hookLevelBalanceExp;
        private final int     hookLevelBalanceExpDistance;

        private final boolean hookQuestsEnabled;
        private final boolean hookQuestObjMobKill;
        private final int     hookQuestObjMobKillpDistance;

        private final Map<PartyAction, Keyed> sounds;

        public PartySettings(JYML cfg) {
            String path = "party.";
            this.partyMaxSize = cfg.getInt(path + "max-size");
            this.partyLeadQuitDisband = cfg.getBoolean(path + "leader-leave-disband");
            this.partyInviteTime = cfg.getInt(path + "invite-timeout");
            this.partyQuitInstLeave = cfg.getBoolean(path + "quit-auto-leave");
            this.partyQuitWaitTime = cfg.getInt(path + "quit-timeout");
            this.partyTeleportCd = cfg.getInt(path + "tp-cooldown");

            path = "chat.";
            this.partyChatEnabled = cfg.getBoolean(path + "enabled");
            this.partyChatFormat = cfg.getString(path + "format");
            this.partyChatCancel = cfg.getBoolean(path + "cancel-main-event", true);

            this.partySizeByRank = new LinkedHashMap<>();
            for (String rank : cfg.getSection("size-permissions")) {
                int size = cfg.getInt("size-permissions." + rank);
                partySizeByRank.put(rank, size);
            }

            path = "hooks.level-plugins.";
            this.hookLevelEnabled = cfg.getBoolean(path + "enabled");
            this.hookLevelBalanceExp = cfg.getBoolean(path + "balance-exp.enabled");
            this.hookLevelBalanceExpDistance = cfg.getInt(path + "balance-exp.max-distance", 25);

            path = "hooks.quest-plugins.";
            this.hookQuestsEnabled = cfg.getBoolean(path + "enabled");
            this.hookQuestObjMobKill = cfg.getBoolean(path + "objectives.mob-kill.enabled");
            this.hookQuestObjMobKillpDistance = cfg.getInt(path + "objectives.mob-kill.max-distance", 25);

            this.sounds = new HashMap<>();
            for (PartyAction action : PartyAction.values()) {
                if (!cfg.contains("sounds." + action.name())) continue;

                Keyed  sound = null;
                String sName = cfg.getString("sounds." + action.name(), "none").toUpperCase();
                try {
                    sound = SoundUT.getSound(sName);
                } catch (IllegalArgumentException ex) {
                    continue;
                }

                this.sounds.put(action, sound);
            }
        }

        public int getPartyMaxSize() {
            return this.partyMaxSize;
        }

        public boolean disbandOnLeaderQuit() {
            return this.partyLeadQuitDisband;
        }

        public int getPartyInviteTime() {
            return this.partyInviteTime;
        }

        public boolean autoLeaveOnQuit() {
            return this.partyQuitInstLeave;
        }

        public int getQuitTimeout() {
            return this.partyQuitWaitTime;
        }

        public int getTeleportCooldown() {
            return this.partyTeleportCd;
        }

        //

        public boolean isChatEnabled() {
            return this.partyChatEnabled;
        }

        public String getChatFormat() {
            return this.partyChatFormat;
        }

        public boolean cancelMainChatEvent() {
            return this.partyChatCancel;
        }

        //

        public Map<String, Integer> getSizePerms() {
            return this.partySizeByRank;
        }

        //

        public boolean isLevelHooksEnabled() {
            return this.hookLevelEnabled;
        }

        public boolean isLevelExpBalance() {
            return this.hookLevelBalanceExp;
        }

        public int getMaxLevelExpBalanceDistance() {
            return this.hookLevelBalanceExpDistance;
        }

        public boolean isQuestHooksEnabled() {
            return this.hookQuestsEnabled;
        }

        public boolean isQuestMobKillEnabled() {
            return this.hookQuestObjMobKill;
        }

        public int getMaxQuestMobKillDistance() {
            return this.hookQuestObjMobKillpDistance;
        }

        //

        public void playSound(@NotNull Player player, @NotNull PartyAction action) {
            Keyed sound = sounds.get(action);
            if (sound != null) {
                player.playSound(player.getLocation(), sound.getKey().toString(), 0.8f, 0.8f);
            }
        }

        public int getPartyPermSize(@NotNull Player player) {
            int    maxSize = this.getPartyMaxSize();
            String group   = Hooks.getPermGroup(player);
            for (String rank : partySizeByRank.keySet()) {
                if (rank.equalsIgnoreCase(group)) {
                    maxSize = partySizeByRank.get(rank);
                }
            }
            return maxSize;
        }
    }

    class QuitTask extends ITask<Divinity> {

        public QuitTask(@NotNull Divinity plugin) {
            super(plugin, 60, false);
        }

        @Override
        public void action() {
            for (Party party : getParties()) {
                for (PartyMember member : new HashSet<>(party.getMembers())) {
                    if (member.isQuitTime()) {
                        member.leaveParty();
                    }
                }
            }
        }
    }

    enum PartyAction {

        CREATE,
        INVITE,
        JOIN,
        LEAVE,
        KICK,
        DISBAND,
        QUIT,
        BACK,
        TP,
    }

    public enum PartyDropMode {
        FREE,
        LEADER,
        AUTO,
        ROLL,
    }

    public enum PartyExpMode {
        PRIVATE,
        SHARED,
    }
}
