package studio.magemonkey.divinity.manager.listener.object;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import studio.magemonkey.divinity.api.event.DivinityDamageEvent;
import studio.magemonkey.divinity.testutil.MockedTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("Methods are not implemented yet")
public class VanillaWrapperListenerTest extends MockedTest {
    private Player damager;
    private Player target;

    @BeforeEach
    public void setup() {
        damager = genPlayer("Travja");
        target = genPlayer("Goflish");
    }

    @Test
    void swordDoesAppropriateDamage() {
        final double expectedDamage = 5;
        damager.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
        ((PlayerMock) target).simulateDamage(5, damager);
        assertEventFired(DivinityDamageEvent.Start.class, event -> {
            double damage = 0;
            for (Double value : event.getDamageMap().values()) {
                damage += value;
            }

            assertEquals(target, event.getVictim(), "Event victim is not the target");
            assertEquals(damager, event.getDamager(), "Event damager is not the damager");
            assertEquals(expectedDamage, damage, 0.001);

            return true;
        });

        assertEquals(20 - expectedDamage, target.getHealth(), 0.001);
    }

    @Test
    void tridentDoesAppropriateDamage() {
        final double expectedDamage = 9;
        Trident      trident        = target.getWorld().spawn(target.getLocation(), Trident.class);
        trident.setShooter(damager);
        ((PlayerMock) target).simulateDamage(9, trident);
        assertEventFired(DivinityDamageEvent.Start.class, event -> {
            double damage = 0;
            for (Double value : event.getDamageMap().values()) {
                damage += value;
            }

            assertEquals(target, event.getVictim(), "Event victim is not the target");
            assertEquals(damager, event.getDamager(), "Event damager is not the damager");
            assertEquals(expectedDamage, damage, 0.001);

            return true;
        });

        assertEquals(20 - expectedDamage, target.getHealth(), 0.001);
    }
}