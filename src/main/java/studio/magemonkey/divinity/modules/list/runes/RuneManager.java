package studio.magemonkey.divinity.modules.list.runes;

import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.external.citizens.CitizensHK;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.SocketItem;
import studio.magemonkey.divinity.modules.api.socketing.ModuleSocket;
import studio.magemonkey.divinity.modules.list.runes.RuneManager.Rune;
import studio.magemonkey.divinity.modules.list.runes.merchant.MerchantTrait;
import studio.magemonkey.divinity.stats.EntityStats;
import studio.magemonkey.divinity.stats.EntityStatsTask;

import java.util.Map;
import java.util.TreeMap;

public class RuneManager extends ModuleSocket<Rune> {

    public RuneManager(@NotNull Divinity plugin) {
        super(plugin, Rune.class);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.RUNES;
    }

    @Override
    @NotNull
    public String version() {
        return "1.21";
    }

    @Override
    public void setup() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    protected void onPostSetup() {
        super.onPostSetup();

        CitizensHK citizensHook = plugin.getCitizens();
        if (citizensHook != null) {
            TraitInfo trait = TraitInfo.create(MerchantTrait.class);
            citizensHook.registerTrait(plugin, trait);
        }
    }

    // -------------------------------------------------------------------- //
    // METHODS

    public void addRuneEffects(@NotNull LivingEntity entity) {
        EntityStats stats = EntityStats.get(entity);
        for (ItemStack item : stats.getEquipment()) {
            for (Map.Entry<Rune, Integer> e : this.getItemSockets(item)) {
                Rune rune = e.getKey();
                int  lvl  = e.getValue();

                PotionEffect effect = rune.getEffect(lvl);
                if (effect == null) continue;

                stats.addPermaPotionEffect(effect);
            }
        }
    }

    // -------------------------------------------------------------------- //
    // CLASSES

    public class Rune extends SocketItem {

        private TreeMap<Integer, PotionEffect> effects;

        public Rune(@NotNull Divinity plugin, @NotNull JYML cfg) {
            super(plugin, cfg, RuneManager.this);

            PotionEffectType type = PotionEffectType.getByName(cfg.getString("effect", "").toUpperCase());
            if (type == null) {
                throw new IllegalArgumentException(
                        "Invalid potion effect for rune: '" + cfg.getFile().getName() + "'.");
            }

            int dur = EntityStatsTask.POTION_DURATION;
            if (type == PotionEffectType.NIGHT_VISION) {
                dur *= 5;
            }

            this.effects = new TreeMap<>();
            for (int lvl = this.getMinLevel() - 1; lvl < this.getMaxLevel(); lvl++) {
                PotionEffect pe = new PotionEffect(type, dur, Math.max(0, lvl));
                this.effects.put(lvl + 1, pe);
            }
        }

        @Nullable
        public PotionEffect getEffect(int lvl) {
            Map.Entry<Integer, PotionEffect> e = this.effects.floorEntry(lvl);
            return e != null ? e.getValue() : null;
        }
    }
}
