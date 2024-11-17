package studio.magemonkey.divinity.modules.list.runes.merchant;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import studio.magemonkey.divinity.api.DivinityAPI;
import studio.magemonkey.divinity.modules.api.socketing.merchant.MerchantSocket;
import studio.magemonkey.divinity.modules.list.runes.RuneManager;

@TraitName("runes-merchant")
public class MerchantTrait extends Trait {

    public MerchantTrait() {
        super("runes-merchant");
    }

    @EventHandler
    public void click(NPCRightClickEvent e) {
        if (e.getNPC() == this.getNPC()) {
            Player      p           = e.getClicker();
            RuneManager runeManager = DivinityAPI.getModuleManager().getRuneManager();
            if (runeManager == null) return;

            MerchantSocket merchant = runeManager.getMerchant();
            if (merchant != null) {
                merchant.openMerchantGUI(p, false);
            }
        }
    }
}