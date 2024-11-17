package studio.magemonkey.divinity.modules.list.consumables;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.event.DivinityPlayerItemUseEvent;
import studio.magemonkey.divinity.modules.LimitedItem;
import studio.magemonkey.divinity.modules.UsableItem;
import studio.magemonkey.divinity.modules.api.QModuleUsage;
import studio.magemonkey.divinity.stats.EntityStats;

public class ConsumablesManager extends QModuleUsage<ConsumablesManager.Consume> {
    private boolean allowConsumeFullHealth;
    private boolean allowConsumeFullFood;

    public ConsumablesManager(@NotNull Divinity plugin) {
        super(plugin, Consume.class);
    }

    @NotNull
    public String getId() {
        return "consumables";
    }

    @NotNull
    public String version() {
        return "1.8.0";
    }

    public void setup() {
        this.allowConsumeFullHealth = this.cfg.getBoolean("consuming.allow-on-full-health");
        this.allowConsumeFullFood = this.cfg.getBoolean("consuming.allow-on-full-food");
    }

    public void shutdown() {
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onConsume(DivinityPlayerItemUseEvent e) {
        LimitedItem uItem = e.getItem();
        if (!(uItem instanceof Consume))
            return;
        Player    p         = e.getPlayer();
        Consume   c         = (Consume) uItem;
        ItemStack i         = e.getItemStack();
        double    maxHealth = EntityStats.getEntityMaxHealth(p);
        if (c.getHealth() > 0.0D && !isConsumingAllowedOnFullHealth() && p.getHealth() >= maxHealth) {
            (this.plugin.lang()).Consumables_Consume_Error_HealthLevel.replace("%item%", ItemUT.getItemName(i))
                    .send(p);
            e.setCancelled(true);
            return;
        }
        if (c.getHunger() > 0.0D && !isConsumingAllowedOnFullFood() && p.getFoodLevel() >= 20) {
            (this.plugin.lang()).Consumables_Consume_Error_FoodLevel.replace("%item%", ItemUT.getItemName(i))
                    .send(p);
            e.setCancelled(true);
            return;
        }
        c.applyEffects(p);
    }

    public boolean isConsumingAllowedOnFullHealth() {
        return this.allowConsumeFullHealth;
    }

    public boolean isConsumingAllowedOnFullFood() {
        return this.allowConsumeFullFood;
    }

    public class Consume extends UsableItem {
        @Getter
        private final double health, hunger, saturation;

        public Consume(@NotNull Divinity plugin, JYML cfg) {
            super(plugin, cfg, ConsumablesManager.this);
            this.health = cfg.getDouble("effects.health");
            this.hunger = cfg.getDouble("effects.hunger");
            this.saturation = cfg.getDouble("effects.saturation", 0);
        }

        public void applyEffects(@NotNull Player p) {
            double max  = EntityStats.getEntityMaxHealth(p);
            int    food = (int) Math.min(20.0D, p.getFoodLevel() + getHunger());
            double heal = Math.min(p.getHealth() + getHealth(), max);
            p.setHealth(heal);
            p.setFoodLevel(food);

            double sat = Math.min(p.getFoodLevel(), p.getSaturation() + getSaturation());
            p.setSaturation((float) sat);
        }
    }
}
