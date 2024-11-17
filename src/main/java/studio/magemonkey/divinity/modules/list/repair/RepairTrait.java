package studio.magemonkey.divinity.modules.list.repair;

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

@TraitName("repair")
public class RepairTrait extends Trait {

    public RepairTrait() {
        super("repair");
    }

    @EventHandler
    public void click(NPCRightClickEvent e) {
        if (e.getNPC() == this.getNPC()) {
            Player player = e.getClicker();

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

            RepairManager repairManager = DivinityAPI.getModuleManager().getRepairManager();
            if (repairManager == null) return;

            repairManager.openAnvilGUI(player, player.getInventory().getItemInMainHand(), skull, null, false);
        }
    }
}