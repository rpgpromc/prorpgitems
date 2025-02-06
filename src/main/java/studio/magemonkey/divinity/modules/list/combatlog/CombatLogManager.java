package studio.magemonkey.divinity.modules.list.combatlog;

import lombok.Getter;
import lombok.Setter;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.manager.api.task.ITask;
import studio.magemonkey.codex.util.*;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.hooks.EHook;
import studio.magemonkey.divinity.hooks.external.FabledHook;
import studio.magemonkey.divinity.manager.damage.DamageMeta;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.api.QModule;
import studio.magemonkey.divinity.modules.list.combatlog.command.LogCommand;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.items.attributes.DamageAttribute;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CombatLogManager extends QModule {

    public  boolean           genLogEnabled;
    private int               genLogMaxAmount;
    private DateTimeFormatter genLogFormatTime;
    private String            genLogFormatText;
    private String            genLogFormatButtonDamageName;
    private List<String>      genLogFormatButtonDamageText;
    private String            genLogFormatButtonDefenseName;
    private List<String>      genLogFormatButtonDefenseText;
    private String            genLogFormatButtonDetailsName;
    private List<String>      genLogFormatButtonDetailsText;
    private String            genLogFormatButtonWeaponName;

    @Getter
    private boolean ignoreZeroDamage;

    private List<String>        indicatorOrder;
    private Map<String, String> indicatorDamageTypes;
    private Map<String, String> indicatorRegenTypes;

    private Map<String, List<ClickText>> logCombat;
    private IndicatorExpansion           indicatorExpansion;

    public CombatLogManager(@NotNull Divinity plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.COMBAT_LOG;
    }

    @Override
    @NotNull
    public String version() {
        return "1.5.0";
    }

    @Override
    public void setup() {
        String path = "general.";
        if (this.genLogEnabled = cfg.getBoolean(path + "logging.enabled")) {
            this.logCombat = new HashMap<>();
            this.genLogMaxAmount = cfg.getInt(path + "logging.max-amount", 10);

            String path2 = "general.logging.format.";
            this.genLogFormatTime = DateTimeFormatter.ofPattern(cfg.getString(path2 + "time", "HH:mm"));
            this.genLogFormatText = StringUT.color(cfg.getString(path2 + "text",
                    "&6[%time%] &r%message% %damage% %defense% %details% %weapon%"));

            this.genLogFormatButtonDamageName =
                    StringUT.color(cfg.getString(path2 + "buttons.damage.name", "&c&l[Damage]"));
            this.genLogFormatButtonDamageText = StringUT.color(cfg.getStringList(path2 + "buttons.damage.text"));

            this.genLogFormatButtonDefenseName =
                    StringUT.color(cfg.getString(path2 + "buttons.defense.name", "&b&l[Defense]"));
            this.genLogFormatButtonDefenseText = StringUT.color(cfg.getStringList(path2 + "buttons.defense.text"));

            this.genLogFormatButtonDetailsName =
                    StringUT.color(cfg.getString(path2 + "buttons.details.name", "&e&l[Details]"));
            this.genLogFormatButtonDetailsText = StringUT.color(cfg.getStringList(path2 + "buttons.details.text"));

            this.genLogFormatButtonWeaponName =
                    StringUT.color(cfg.getString(path2 + "buttons.weapon.name", "&d&l[Weapon]"));

            this.moduleCommand.addSubCommand(new LogCommand(this));
        }

        path = "messages.";
        this.ignoreZeroDamage = cfg.getBoolean(path + "ignore-zero-damage");

        for (MessageType msgType : MessageType.values()) {
            String path2 = path + "types." + msgType.name() + ".";
            if (!cfg.getBoolean(path2 + "enabled")) continue;

            String msgDamager   = cfg.getString(path2 + "messages.damager");
            String msgVictim    = cfg.getString(path2 + "messages.victim");
            String msgIndicator = cfg.getString(path2 + "messages.indicator");
            Keyed  sound        = null;
            String soundStr     = cfg.getString(path2 + "sound");
            if (soundStr != null && !soundStr.equalsIgnoreCase(JStrings.NONE)) {
                try {
                    sound = SoundUT.getSound(soundStr.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }

            msgType.setEnabled(true);
            msgType.setMessageDamager(msgDamager);
            msgType.setMessageVictim(msgVictim);
            msgType.setMessageIndicator(msgIndicator);
            msgType.setSound(sound);
        }

        path = "indicators.";
        if (cfg.getBoolean(path + "enabled") && Hooks.hasPlugin(EHook.HOLOGRAPHIC_DISPLAYS)) {
            this.indicatorOrder = cfg.getStringList(path + "format.order");

            this.indicatorDamageTypes = new HashMap<>();
            for (String dmgId : cfg.getSection(path + "format.damage-types")) {
                String holoText = StringUT.color(cfg.getString(path + "format.damage-types." + dmgId, "&c-%damage%"));
                this.indicatorDamageTypes.put(dmgId.toLowerCase(), holoText);
            }

            this.indicatorRegenTypes = new HashMap<>();
            for (String regenId : cfg.getSection(path + "format.regen-sources")) {
                String holoText = StringUT.color(cfg.getString(path + "format.regen-sources." + regenId, "&a+%hp%"));
                this.indicatorRegenTypes.put(regenId.toLowerCase(), holoText);
            }

            this.indicatorExpansion = new IndicatorExpansion();
            this.indicatorExpansion.setup();
        }
    }

    @Override
    public void shutdown() {
        if (this.indicatorExpansion != null) {
            this.indicatorExpansion.shutdown();
            this.indicatorExpansion = null;
        }
        if (this.indicatorOrder != null) {
            this.indicatorOrder.clear();
            this.indicatorOrder = null;
        }
        if (this.indicatorDamageTypes != null) {
            this.indicatorDamageTypes.clear();
            this.indicatorDamageTypes = null;
        }
        if (this.indicatorRegenTypes != null) {
            this.indicatorRegenTypes.clear();
            this.indicatorRegenTypes = null;
        }
        if (this.logCombat != null) {
            this.logCombat.clear();
            this.logCombat = null;
        }
    }

    // -------------------------------------------------------------------- //
    // METHODS

    @NotNull
    public List<ClickText> getCombatLog(@NotNull Player player) {
        return this.logCombat.getOrDefault(player.getName(), new ArrayList<>());
    }

    private void sendCombatLog(@NotNull DamageMeta meta, double damageTotal) {
        if (damageTotal <= 0 && this.ignoreZeroDamage) return;

        LivingEntity damager = meta.getDamager();
        if (damager == null) return;

        MessageType type = MessageType.NORMAL;
        if (meta.isDodged()) {
            type = MessageType.DODGE;
        } else if (meta.isCritical()) {
            type = MessageType.CRITICAL;
        } else if (meta.isBlocked()) {
            type = MessageType.BLOCK;
        }
        if (!type.isEnabled()) return;


        String strDamage = NumberUT.format(damageTotal);
        String strBlock  = NumberUT.format((1D - meta.getBlockModifier()) * 100D);

        LivingEntity victim = meta.getVictim();

        Player pVictim  = null;
        Player pDamager = null;

        if (victim instanceof Player) pVictim = (Player) victim;
        if (damager instanceof Player) pDamager = (Player) damager;

        class LogFormatter {

            public void format(@NotNull LivingEntity damager, @NotNull Player sender, @NotNull String dMsg) {
                String time     = genLogFormatTime.format(LocalTime.now());
                String main     = genLogFormatText.replace("%message%", dMsg).replace("%time%", time);
                String atkPower = NumberUT.format(100D * EntityStats.get(damager).getAttackPowerModifier());

                double defBlockedTotal    = meta.getDefendedDamage();
                double damageRawTotal     = damageTotal + defBlockedTotal;
                double damagePercentTotal = (damageTotal / damageRawTotal) * 100D;

                List<String> damageHint = new ArrayList<>();
                for (String line : genLogFormatButtonDamageText) {
                    if (line.contains("%damage_type%")) {
                        for (Map.Entry<DamageAttribute, Double> e : meta.getDamages().entrySet()) {
                            double damageAmount  = e.getValue();
                            double damageRaw     = damageAmount + meta.getDefendedDamage(e.getKey());
                            double damagePercent = (damageAmount / damageRaw) * 100D;
                            String dmgName       = e.getKey().getName();

                            damageHint.add(line
                                    .replace("%percent%", NumberUT.format(damagePercent))
                                    .replace("%raw%", NumberUT.format(damageRaw))
                                    .replace("%amount%", NumberUT.format(damageAmount))
                                    .replace("%damage_type%", dmgName));
                        }
                        continue;
                    }
                    damageHint.add(line
                            .replace("%percent_total%", NumberUT.format(damagePercentTotal))
                            .replace("%raw_total%", NumberUT.format(damageRawTotal))
                            .replace("%amount_total%", NumberUT.format(damageTotal))
                            .replace("%attack_power%", atkPower));
                }

                double defPercentTotal = (defBlockedTotal / damageRawTotal) * 100D;

                List<String> defenseHint = new ArrayList<>();
                for (String line : genLogFormatButtonDefenseText) {
                    if (line.contains("%defense_type%")) {
                        for (Map.Entry<DamageAttribute, Double> e : meta.getDamages().entrySet()) {
                            double defBlocked = meta.getDefendedDamage(e.getKey());
                            double damageType = e.getValue() + defBlocked;
                            double defPercent = (defBlocked / damageType) * 100D;

                            defenseHint.add(line
                                    .replace("%damage%", NumberUT.format(damageType))
                                    .replace("%percent%", NumberUT.format(defPercent))
                                    .replace("%amount%", NumberUT.format(defBlocked))
                                    .replace("%defense_type%", e.getKey().getName()));
                        }
                        continue;
                    }
                    defenseHint.add(line
                            .replace("%percent_total%", NumberUT.format(defPercentTotal))
                            .replace("%amount_total%", NumberUT.format(defBlockedTotal))
                            .replace("%damage_total%", NumberUT.format(damageRawTotal)));
                }

                List<String> damageDetails = new ArrayList<>(genLogFormatButtonDetailsText);
                damageDetails.replaceAll(s -> s
                        .replace("%enchantment_protection_factor%",
                                NumberUT.format(meta.getEnchantProtectionModifier()))
                        .replace("%pvpe_modifier%", NumberUT.format(meta.getPvEDamageModifier()))
                        .replace("%penetrate_modifier%", NumberUT.format(meta.getPenetrateModifier()))
                        .replace("%block_modifier%", NumberUT.format(meta.getBlockModifier()))
                        .replace("%critical_modifier%", NumberUT.format(meta.getCriticalModifier()))
                        .replace("%direct_modifier%", NumberUT.format(meta.getDirectModifier())));

                ItemStack weapon = meta.getWeapon();
                ClickText text   = new ClickText(main);
                if (meta.isDodged()) {
                    text.createPlaceholder("%damage%", "");
                    text.createPlaceholder("%defense%", "");
                    text.createPlaceholder("%details%", "");
                } else {
                    text.createPlaceholder("%damage%", genLogFormatButtonDamageName).hint(damageHint);
                    text.createPlaceholder("%defense%", genLogFormatButtonDefenseName).hint(defenseHint);
                    text.createPlaceholder("%details%", genLogFormatButtonDetailsName).hint(damageDetails);
                }
                if (weapon != null && weapon.getType() != Material.AIR) {
                    text.createPlaceholder("%weapon%", genLogFormatButtonWeaponName).showItem(weapon);
                } else {
                    text.createPlaceholder("%weapon%", "");
                }

                List<ClickText> dLog = getCombatLog(sender);
                if (dLog.size() >= genLogMaxAmount) {
                    dLog.remove(0);
                }

                dLog.add(text);
                logCombat.put(sender.getName(), dLog);
            }
        }

        LogFormatter logger = this.genLogEnabled && (pVictim != null || pDamager != null) ? new LogFormatter() : null;

        if (pVictim != null) {
            String zMsg = type.getMessageVictim();
            if (zMsg != null) {
                zMsg = zMsg
                        .replace("%damage%", strDamage)
                        .replace("%entity%", meta.getDamagerName())
                        .replace("%block%", strBlock);

                MsgUT.sendActionBar(pVictim, zMsg);
                type.playSound(pVictim);


                if (logger != null) {
                    logger.format(damager, pVictim, zMsg);
                }
            }
        }
        if (pDamager != null) {
            String dMsg = type.getMessageDamager();
            if (dMsg != null) {
                dMsg = dMsg
                        .replace("%damage%", strDamage)
                        .replace("%entity%", meta.getVictimName())
                        .replace("%block%", strBlock);

                MsgUT.sendActionBar(pDamager, dMsg);
                type.playSound(pDamager);

                if (logger != null) {
                    logger.format(pDamager, pDamager, dMsg);
                }
            }
        }
    }

    @NotNull
    private String getDamageTypeFormat(@NotNull String type) {
        type = type.toLowerCase();
        if (indicatorDamageTypes.containsKey(type)) {
            return this.indicatorDamageTypes.get(type);
        } else {
            return "%damage%";
        }
    }

    @Nullable
    private String getRegenTypeFormat(@NotNull String type) {
        type = type.toLowerCase();
        if (indicatorRegenTypes.containsKey(type)) {
            return this.indicatorRegenTypes.get(type);
        } else if (indicatorRegenTypes.containsKey(JStrings.DEFAULT)) {
            return this.indicatorRegenTypes.get(JStrings.DEFAULT);
        } else {
            return null;
        }
    }

    // -------------------------------------------------------------------- //
    // EVENTS

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onIndicatorRegen(EntityRegainHealthEvent e) {
        if (this.indicatorExpansion == null || this.indicatorRegenTypes.isEmpty()) return;

        Entity e1 = e.getEntity();
        if (!(e1 instanceof LivingEntity)) return;

        String regenSource = e.getRegainReason().name();
        double health      = e.getAmount();
        if (health <= 0) return;

        String line = this.getRegenTypeFormat(regenSource);
        if (line == null) return;

        List<String> list = List.of(line.replace("%hp%", NumberUT.format(health)));

        LivingEntity li  = (LivingEntity) e1;
        Location     loc = li.getEyeLocation().clone().add(0, 0.9D, 0);
        this.indicatorExpansion.create(loc, list);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageIndicator(EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent && plugin.getPluginManager().isPluginEnabled("Fabled")) {
            EntityDamageByEntityEvent ede        = (EntityDamageByEntityEvent) e;
            FabledHook                fabledHook = (FabledHook) this.plugin.getHook(EHook.SKILL_API);
            if (Objects.requireNonNull(fabledHook).isFakeDamage(ede)) return;
        }

        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity zertva = (LivingEntity) e.getEntity();

        // Quick fix for stupid plugins
        if (zertva instanceof ArmorStand || zertva.isInvulnerable()) return;

        DamageMeta meta = EntityStats.get(zertva).getLastDamageMeta();
        if (meta == null || (e.isCancelled() && !meta.isDodged())) return;
        meta.addMissingDmg(e.getDamage());

        double dmgTotal = meta.getTotalDamage();
        if (dmgTotal <= 0 && this.ignoreZeroDamage && !meta.isDodged()) return;

        this.sendCombatLog(meta, dmgTotal);

        if (this.indicatorExpansion != null) {
            this.indicatorExpansion.create(meta);
        }
    }

    // -------------------------------------------------------------------- //
    // CLASSES

    enum MessageType {

        NORMAL,
        CRITICAL,
        DODGE,
        BLOCK,
        ;

        @Getter
        @Setter
        private boolean enabled      = false;
        private String  msgDamager   = null;
        private String  msgVictim    = null;
        private String  msgIndicator = null;
        private Keyed   sound        = null;

        @Nullable
        public String getMessageDamager() {
            return this.msgDamager;
        }

        public void setMessageDamager(@Nullable String msgDamager) {
            this.msgDamager = msgDamager != null ? StringUT.color(msgDamager) : msgDamager;
        }

        @Nullable
        public String getMessageVictim() {
            return this.msgVictim;
        }

        public void setMessageVictim(@Nullable String msgVictim) {
            this.msgVictim = msgVictim != null ? StringUT.color(msgVictim) : msgVictim;
        }

        @Nullable
        public String getMessageIndicator() {
            return this.msgIndicator;
        }

        public void setMessageIndicator(@Nullable String msgIndicator) {
            this.msgIndicator = msgIndicator != null ? StringUT.color(msgIndicator) : msgIndicator;
        }

        public void setSound(@Nullable Keyed sound) {
            this.sound = sound;
        }

        public void playSound(@NotNull Player player) {
            if (this.sound != null) {
                player.playSound(player.getLocation(), this.sound.getKey().toString(), 0.8f, 0.8f);
            }
        }
    }

    class IndicatorExpansion {

        private Map<Hologram, Integer> map;
        private UpTask                 taskUp;

        public void setup() {
            this.map = new HashMap<>();
            this.taskUp = new UpTask();
            this.taskUp.start();
        }

        public void shutdown() {
            if (this.taskUp != null) {
                this.taskUp.stop();
                this.taskUp = null;
            }
            if (this.map != null) {
                for (Hologram hologram : this.map.keySet()) {
                    hologram.delete();
                }
                this.map.clear();
                this.map = null;
            }
        }

        public void create(@NotNull DamageMeta meta) {
            List<String> list = new ArrayList<>();
            for (String holoText : indicatorOrder) {
                if (holoText.equalsIgnoreCase("%dodge%") && meta.isDodged()) {
                    list.add(MessageType.DODGE.getMessageIndicator());
                    break;
                }
                if (holoText.equalsIgnoreCase("%critical%") && meta.isCritical()) {
                    list.add(MessageType.CRITICAL.getMessageIndicator());
                    continue;
                }
                if (holoText.equalsIgnoreCase("%block%") && meta.isBlocked()) {
                    list.add(MessageType.BLOCK.getMessageIndicator());
                    continue;
                }
                if (holoText.equalsIgnoreCase("%damage%")) {
                    for (Map.Entry<DamageAttribute, Double> e : meta.getDamages().entrySet()) {
                        double dmgType = e.getValue();
                        if (dmgType > 0) {
                            String line = getDamageTypeFormat(e.getKey().getId());
                            list.add(line.replace("%damage%", NumberUT.format(dmgType)));
                        }
                    }
                }
            }
            Location loc = meta.getVictim().getEyeLocation().clone().add(0, 0.9D, 0);
            this.create(loc, list);
        }

        public void create(@NotNull Location loc, @NotNull List<String> list) {
            if (list.isEmpty()) return;

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Hologram holo = HolographicDisplaysAPI.get(plugin).createHologram(loc);
                for (String line : list) {
                    holo.getLines().appendText(line);
                }
                map.put(holo, 1);
            });
        }

        class UpTask extends ITask<Divinity> {

            public UpTask() {
                super(CombatLogManager.this.plugin, 1L, false);
            }

            @Override
            public void action() {
                for (Map.Entry<Hologram, Integer> e : new HashMap<>(map).entrySet()) {
                    Hologram holo   = e.getKey();
                    int      yStack = e.getValue();

                    holo.setPosition(holo.getPosition().add(0, 0.11, 0));
                    if (yStack++ >= 20) {
                        map.remove(holo);
                        holo.delete();
                    } else {
                        map.put(holo, yStack);
                    }
                }
            }
        }
    }
}
