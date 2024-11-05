/**
 * Copyright 2024 MageMonkeyStudio
 */
package studio.magemonkey.divinity.modules.list.customitems;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.util.constants.JStrings;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.Config;
import studio.magemonkey.divinity.modules.EModule;
import studio.magemonkey.divinity.modules.ModuleItem;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.list.customitems.CustomItemsManager.CustomItem;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.divinity.stats.tiers.Tier;
import studio.magemonkey.divinity.stats.tiers.Tiered;

/*
name:
lore:
color: -1,-1,-1
unbreakable: true
item-flags:
- '*'
model-data:
 */

public class CustomItemsManager extends QModuleDrop<CustomItem> {

    public CustomItemsManager(@NotNull Divinity plugin) {
        super(plugin, CustomItem.class);
    }

    @NotNull
    @Override
    public String getId() {
        return EModule.CUSTOM_ITEMS;
    }

    @NotNull
    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void setup() {

    }

    @Override
    public void shutdown() {

    }

    @Getter
    @Setter
    public class CustomItem extends ModuleItem implements Tiered {
        private Tier tier;

        public CustomItem(@NotNull Divinity plugin, @NotNull JYML cfg) {
            super(plugin, cfg, CustomItemsManager.this);

            this.updateConfig(cfg);
            validateTier(cfg);
        }

        private void updateConfig(@NotNull JYML cfg) {
            cfg.addMissing("tier", JStrings.DEFAULT);
            cfg.saveChanges();
        }

        private void validateTier(JYML cfg) {
            this.tier = Config.getTier(cfg.getString("tier", JStrings.DEFAULT));
            if (this.tier == null) {
                throw new IllegalArgumentException("Invalid Tier provided! Module Item must have valid Tier!");
            }
        }

        @Override
        protected String applyLoreReplacements(String lore) {
            return this.getTier().format(lore);
        }

        @Override
        protected void processLore(@NotNull JYML cfg, @NotNull QModuleDrop<?> module) {
            validateTier(cfg);
            super.processLore(cfg, module);
        }
    }

}
