package studio.magemonkey.divinity.modules.list.essences;

import net.citizensnpcs.api.trait.TraitInfo;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.hooks.external.citizens.CitizensHK;
import studio.magemonkey.codex.util.EffectUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.SocketItem;
import studio.magemonkey.divinity.modules.api.socketing.ModuleSocket;
import studio.magemonkey.divinity.modules.list.essences.EssencesManager.Essence;
import studio.magemonkey.divinity.modules.list.essences.merchant.MerchantTrait;
import studio.magemonkey.divinity.utils.ParticleUtils;

public class EssencesManager extends ModuleSocket<Essence> {

    public EssencesManager(@NotNull Divinity plugin) {
        super(plugin, Essence.class);
    }

    @Override
    @NotNull
    public String getId() {
        return EModule.ESSENCES;
    }

    @Override
    @NotNull
    public String version() {
        return "2.0.0";
    }

    @Override
    public void setup() {

    }

    @Override
    public void shutdown() {

    }

    // -------------------------------------------------------------------- //
    // CLASSES

    @Override
    protected void onPostSetup() {
        super.onPostSetup();

        CitizensHK citizensHook = plugin.getCitizens();
        if (citizensHook != null) {
            TraitInfo trait = TraitInfo.create(MerchantTrait.class);
            citizensHook.registerTrait(plugin, trait);
        }
    }

    public static enum EssenceType {
        HELIX,
        AURA,
        FOOT,
        ;
    }

    public class EssenceEffect {

        private EssenceType type;
        private String      particle;
        private float       speed;
        private int         amount;
        private float       offX;
        private float       offY;
        private float       offZ;

        public EssenceEffect(@NotNull JYML cfg) {

            String sType = cfg.getString("effect.type", "null");
            try {
                this.type = EssenceType.valueOf(sType.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid essence effect type: " + sType);
            }

            this.particle = cfg.getString("effect.name", "");
            if (this.particle.isEmpty()) {
                throw new IllegalArgumentException("NULL essence particle name: " + this.particle);
            }

            cfg.addMissing("effect.amount", 15);
            cfg.addMissing("effect.speed", 0.1);
            cfg.addMissing("effect.offset-x", 0);
            cfg.addMissing("effect.offset-y", 0);
            cfg.addMissing("effect.offset-z", 0);
            cfg.save();

            this.amount = cfg.getInt("effect.amount");
            this.speed = (float) cfg.getDouble("effect.speed");
            this.offX = (float) cfg.getDouble("effect.offset-x");
            this.offY = (float) cfg.getDouble("effect.offset-y");
            this.offZ = (float) cfg.getDouble("effect.offset-z");
        }

        @NotNull
        public EssenceType getType() {
            return this.type;
        }

        @NotNull
        public String getParticleName() {
            return this.particle;
        }

        public float getSpeed() {
            return this.speed;
        }

        public int getAmount() {
            return this.amount;
        }

        public float getOffsetX() {
            return this.offX;
        }

        public float getOffsetY() {
            return this.offY;
        }

        public float getOffsetZ() {
            return this.offZ;
        }

        public void play(@NotNull LivingEntity entity, int lvl) {
            if (type == EssenceType.HELIX) {
                ParticleUtils.helix(this, entity, lvl);
            } else if (type == EssenceType.AURA) {
                ParticleUtils.aura(this, entity, lvl);
            } else if (type == EssenceType.FOOT) {
                ParticleUtils.foot(this, entity, lvl);
            }
        }

        public void display(@NotNull Location loc) {
            EffectUT.playEffect(loc, particle, offX, offY, offZ, speed, amount);
        }
    }

    public class Essence extends SocketItem {

        private EssenceEffect effect;

        public Essence(@NotNull Divinity plugin, @NotNull JYML cfg) {
            super(plugin, cfg, EssencesManager.this);

            this.effect = new EssenceEffect(cfg);
        }

        public EssenceEffect getEffect() {
            return this.effect;
        }
    }
}
