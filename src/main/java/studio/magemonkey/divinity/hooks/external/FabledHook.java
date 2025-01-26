package studio.magemonkey.divinity.hooks.external;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.hooks.HookState;
import studio.magemonkey.codex.hooks.NHook;
import studio.magemonkey.codex.util.DataUT;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.hooks.HookClass;
import studio.magemonkey.divinity.hooks.HookLevel;
import studio.magemonkey.divinity.modules.list.itemgenerator.generators.AbilityGenerator;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.items.ItemStats;
import studio.magemonkey.divinity.stats.items.attributes.FabledAttribute;
import studio.magemonkey.divinity.stats.items.attributes.api.TypedStat;
import studio.magemonkey.divinity.stats.items.attributes.stats.DurabilityStat;
import studio.magemonkey.fabled.Fabled;
import studio.magemonkey.fabled.api.DefaultCombatProtection;
import studio.magemonkey.fabled.api.enums.ExpSource;
import studio.magemonkey.fabled.api.event.DynamicTriggerEvent;
import studio.magemonkey.fabled.api.event.PlayerManaGainEvent;
import studio.magemonkey.fabled.api.event.SkillDamageEvent;
import studio.magemonkey.fabled.api.player.PlayerData;
import studio.magemonkey.fabled.api.player.PlayerSkill;
import studio.magemonkey.fabled.api.skills.Skill;

import java.util.*;

public class FabledHook extends NHook<Divinity> implements HookLevel, HookClass {

    public FabledHook(@NotNull Divinity plugin) {
        super(plugin);
    }

    public boolean isFabledLoaded() {
        return Fabled.isLoaded();
    }

    @Override
    @NotNull
    public HookState setup() {
        this.registerListeners();
        return HookState.SUCCESS;
    }

    @Override
    public void shutdown() {
        this.unregisterListeners();
    }

    @Override
    public int getLevel(@NotNull Player player) {
        PlayerData playerData = Fabled.getData(player);
        return playerData.hasClass() ? playerData.getMainClass().getLevel() : 0;
    }

    @Override
    public void giveExp(@NotNull Player player, int amount) {
        PlayerData playerData = Fabled.getData(player);
        playerData.giveExp(amount, ExpSource.SPECIAL);
    }

    @Override
    @NotNull
    public String getClass(@NotNull Player player) {
        PlayerData data = Fabled.getData(player);
        if (data.hasClass()) {
            return StringUT.colorOff(data.getMainClass().getData().getName());
        } else {
            return "";
        }
    }

    @EventHandler
    public void onSkillCast(DynamicTriggerEvent e) {
        if (!EngineCfg.ATTRIBUTES_DURABILITY_REDUCE_FOR_SKILL_API.contains(e.getTrigger().toLowerCase())) return;

        LivingEntity caster = e.getCaster();
        if (!(caster instanceof Player)) return;
        Player     p          = (Player) caster;
        PlayerData playerData = Fabled.getData(p);
        if (playerData == null) return;

        String      skillKey    = e.getSkill().getKey();
        PlayerSkill playerSkill = playerData.getSkill(skillKey);
        if (playerSkill == null || !playerSkill.isExternal()) return;

        ItemStack item = p.getInventory().getItemInMainHand();

        if (getAbilities(item).keySet().stream().anyMatch(s -> s.equalsIgnoreCase(skillKey))) {
            DurabilityStat duraStat = ItemStats.getStat(DurabilityStat.class);
            if (duraStat != null) {
                duraStat.reduceDurability(p, item, 1);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegen(PlayerManaGainEvent e) {
        if (e.getPlayerData() == null) return;
        Player player = e.getPlayerData().getPlayer();
        if (player == null) return;

        double regen = 1D + EntityStats.get(player).getItemStat(TypedStat.Type.MANA_REGEN, false) / 100D;
        if (regen > 0) {
            e.setAmount(e.getAmount() * regen);
        }
    }

    @Override
    public void takeMana(@NotNull Player player, double amount, boolean ofMax) {
        PlayerData data = Fabled.getData(player);
        if (data == null) return;

        double cur = data.getMana();

        if (ofMax) {
            double max = data.getMaxMana();
            amount = max / 100D * amount;
        }

        data.setMana(Math.max(0, cur - amount));
    }

    private final List<UUID> divinityIgnored = new ArrayList<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void skillDamage(SkillDamageEvent event) {
        LivingEntity player = event.getDamager();

        if (event.isIgnoreDivinity())
            divinityIgnored.add(player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void damage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity))
            return;

        divinityIgnored.remove(event.getDamager().getUniqueId());
    }

    public void ignoreDivinity(LivingEntity player, boolean ignore) {
        if (ignore) {
            divinityIgnored.add(player.getUniqueId());
        } else {
            divinityIgnored.remove(player.getUniqueId());
        }
    }

    public boolean ignoreDivinity(LivingEntity player) {
        return divinityIgnored.contains(player.getUniqueId());
    }

    public boolean isSkillDamage() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Fabled")) {
            return false;
        }

        return Skill.isSkillDamage();
    }

    public void addSkill(Player player, String skillId, int level) {
        PlayerData playerData = Fabled.getData(player);
        Skill      skill      = Fabled.getSkill(skillId);
        if (skill == null) {
            plugin.warn("Could not find skill \"" + skillId + "\" to add to the item");
            return;
        }
        playerData.addSkillExternally(skill, playerData.getMainClass(), AbilityGenerator.ABILITY_KEY, level);
    }

    public void removeSkill(Player player, String skillId) {
        PlayerData playerData = Fabled.getData(player);
        Skill      skill      = Fabled.getSkill(skillId);
        if (skill == null) {
            return;
        }
        playerData.removeSkillExternally(skill, AbilityGenerator.ABILITY_KEY);
    }

    public Set<String> getSkills() {
        Set<String> skills = new HashSet<>();
        for (Skill skill : Fabled.getSkills().values()) {
            skills.add(skill.getName());
        }
        return skills;
    }

    public ItemStack getSkillIndicator(String skillId) {
        Skill skill = Fabled.getSkill(skillId);
        if (skill == null) {
            return new ItemStack(Material.JACK_O_LANTERN);
        }
        return skill.getIndicator();
    }

    public Collection<FabledAttribute> getAttributes() {
        List<FabledAttribute> list = new ArrayList<>();
        String                format;
        {
            String baseFormat = Fabled.getSettings().getAttrGiveText("{attr}");
            int    index      = baseFormat.indexOf('{');
            String attrPre    = baseFormat.substring(0, index);
            String attrPost   = baseFormat.substring(index + "{attr}".length());
            format = EngineCfg.LORE_STYLE_FABLED_ATTRIBUTE_FORMAT
                    .replace("%attrPre%", attrPre)
                    .replace("%attrPost%", attrPost)
                    + "%value%";
        }
        for (Map.Entry<String, studio.magemonkey.fabled.manager.FabledAttribute> entry : Fabled.getAttributesManager()
                .getAttributes()
                .entrySet()) {
            list.add(new FabledAttribute(entry.getKey(), entry.getValue().getName(), format));
        }
        return list;
    }

    public ItemStack getAttributeIndicator(String attributeId) {
        studio.magemonkey.fabled.manager.FabledAttribute proAttribute =
                Fabled.getAttributesManager().getAttribute(attributeId);
        if (proAttribute != null) return proAttribute.getToolIcon();

        ItemStack itemStack = new ItemStack(Material.DIRT);
        ItemMeta  meta      = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(attributeId);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private Map<String, Integer> getAbilities(ItemStack item) {
        Map<String, Integer> map = new HashMap<>();
        if (item == null) {
            return map;
        }
        String[] stringAbilities = DataUT.getStringArrayData(item, AbilityGenerator.ABILITY_KEY);
        if (stringAbilities == null) {
            return map;
        }
        for (String stringAbility : stringAbilities) {
            int i = stringAbility.lastIndexOf(':');
            int level;
            try {
                level = Integer.parseInt(stringAbility.substring(i + 1));
            } catch (NumberFormatException e) {
                continue;
            }
            map.put(stringAbility.substring(0, i), level);
        }
        return map;
    }

    public void updateSkills(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Map<String, Integer> skills    = new HashMap<>();
                PlayerInventory      inventory = player.getInventory();
                for (int i : new int[]{inventory.getHeldItemSlot(), 36, 37, 38, 39, 40}) {
                    ItemStack item = inventory.getItem(i);
                    if (item == null) continue;

                    AbilityGenerator.updateNamespace(item);
                    for (Map.Entry<String, Integer> entry : getAbilities(item).entrySet()) {
                        String id    = entry.getKey();
                        int    level = entry.getValue();
                        if (!skills.containsKey(id) || level > skills.get(id)) {
                            skills.put(id, level);
                        }
                    }
                }
                Set<PlayerData.ExternallyAddedSkill> prevSkills =
                        new HashSet<>(Fabled.getData(player).getExternallyAddedSkills());
                for (PlayerData.ExternallyAddedSkill prevSkill : prevSkills) {
                    if (!prevSkill.getKey().equals(AbilityGenerator.ABILITY_KEY)) {
                        continue;
                    }
                    String  id    = prevSkill.getId();
                    Integer level = skills.get(id);
                    if (level == null) { // Removed skill
                        removeSkill(player, id);
                    } else if (level != prevSkill.getLevel()) { // Update changed level
                        addSkill(player, id, level);
                    }
                }
                for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                    String id = entry.getKey();
                    if (prevSkills.stream()
                            .noneMatch(extSkill -> extSkill.getKey().equals(AbilityGenerator.ABILITY_KEY)
                                    && extSkill.getId().equals(id))) {
                        addSkill(player, id, entry.getValue());
                    }
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    public boolean isFakeDamage(EntityDamageByEntityEvent event) {
        return DefaultCombatProtection.isFakeDamageEvent(event);
    }
}
