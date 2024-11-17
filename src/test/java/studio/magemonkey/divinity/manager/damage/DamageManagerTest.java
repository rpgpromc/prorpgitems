package studio.magemonkey.divinity.manager.damage;

import org.mockbukkit.mockbukkit.entity.FishHookMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.entity.ZombieMock;
import org.bukkit.entity.FishHook;
import org.bukkit.event.player.PlayerFishEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.testutil.MockedTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageManagerTest extends MockedTest {
    private PlayerMock player;

    @BeforeEach
    public void setup() {
        player = genPlayer("Travja");
    }

    @Test
    public void onDamageFishHook_enabledDoesDamage() {
        EngineCfg.COMBAT_FISHING_HOOK_DO_DAMAGE = true;
        ZombieMock entity = new ZombieMock(server, UUID.randomUUID());
        entity.setHealth(20);
        FishHook fishHook = new FishHookMock(server, UUID.randomUUID());
        server.getPluginManager().callEvent(new PlayerFishEvent(
                player,
                entity,
                fishHook,
                PlayerFishEvent.State.CAUGHT_ENTITY
        ));

        assertEquals(19, entity.getHealth(), 0.001);
    }

    @Test
    public void onDamageFishHook_disabledDoesNoDamage() {
        EngineCfg.COMBAT_FISHING_HOOK_DO_DAMAGE = false;
        ZombieMock entity = new ZombieMock(server, UUID.randomUUID());
        entity.setHealth(20);
        FishHook fishHook = new FishHookMock(server, UUID.randomUUID());
        server.getPluginManager().callEvent(new PlayerFishEvent(
                player,
                entity,
                fishHook,
                PlayerFishEvent.State.CAUGHT_ENTITY
        ));

        assertEquals(20, entity.getHealth(), 0.001);
    }

    @Test
    public void onDamageRPGStart() {
        assertTrue(true);
    }
}