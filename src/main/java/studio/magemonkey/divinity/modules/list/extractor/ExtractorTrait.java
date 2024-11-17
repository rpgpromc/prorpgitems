package studio.magemonkey.divinity.modules.list.extractor;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import studio.magemonkey.divinity.api.DivinityAPI;

@TraitName("extractor")
public class ExtractorTrait extends Trait {

    public ExtractorTrait() {
        super("extractor");
    }

    @EventHandler
    public void click(NPCRightClickEvent e) {
        if (e.getNPC() == this.getNPC()) {
            Player p = e.getClicker();

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta  = (SkullMeta) skull.getItemMeta();
            if (meta == null) return;

            NPC    npc    = this.getNPC();
            Entity entity = npc.getEntity();
            meta.setDisplayName(npc.getName());
            if (entity instanceof Player) {
                meta.setOwningPlayer((OfflinePlayer) entity);
            }
            skull.setItemMeta(meta);

            ExtractorManager extractorManager = DivinityAPI.getModuleManager().getExtractManager();
            if (extractorManager == null) return;

            extractorManager.openExtraction(p, p.getInventory().getItemInMainHand(), skull, null, false);
        }
    }
}
