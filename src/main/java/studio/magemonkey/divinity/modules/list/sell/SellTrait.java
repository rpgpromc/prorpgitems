package studio.magemonkey.divinity.modules.list.sell;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import studio.magemonkey.divinity.api.DivinityAPI;

@TraitName("sell")
public class SellTrait extends Trait {

    public SellTrait() {
        super("sell");
    }

    @EventHandler
    public void click(NPCRightClickEvent e) {
        if (e.getNPC() == this.getNPC()) {
            Player      player      = e.getClicker();
            SellManager sellManager = DivinityAPI.getModuleManager().getSellManager();
            if (sellManager == null) return;

            sellManager.openSellGUI(player, false);
        }
    }
}