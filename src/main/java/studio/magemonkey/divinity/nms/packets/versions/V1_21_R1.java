package studio.magemonkey.divinity.nms.packets.versions;

import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.api.events.EnginePlayerPacketEvent;
import studio.magemonkey.codex.util.Reflex;
import studio.magemonkey.divinity.Divinity;

import java.lang.reflect.Method;

public class V1_21_R1 extends V1_20_R4 {
    public V1_21_R1(@NotNull Divinity plugin) {super(plugin);}

    @Override
    protected void manageDamageParticle(@NotNull EnginePlayerPacketEvent e, @NotNull Object packet) {
        Class<?> packetParticlesClass = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutWorldParticles");
        Class<?> particleParamClass   = Reflex.getClass("net.minecraft.core.particles", "ParticleParam");

        Class<?> registries       = Reflex.getClass("net.minecraft.core.registries.BuiltInRegistries");
        Object   particleRegistry = Reflex.getFieldValue(registries, "i");

        Object p = packetParticlesClass.cast(packet);

        Object particleParam = Reflex.getFieldValue(p, "k");
        if (particleParam == null) return;

        Method a = Reflex.getMethod(particleParamClass, "a"); //Get the namespace key of the particle being sent

        try {
            Object   particleType = Reflex.invokeMethod(a, particleParam);
            String   mcKey        = "damage_indicator";
            Class<?> keyClass     = Reflex.getClass("net.minecraft.resources.MinecraftKey");
            Object   key          =
                    Reflex.getConstructor(keyClass, String.class, String.class).newInstance("minecraft", mcKey);
            Object damageIndicator = Reflex.invokeMethod(Reflex.getMethod(particleRegistry.getClass(), "a", keyClass),
                    particleRegistry,
                    key);
            if (particleType.equals(damageIndicator)) {
                Reflex.setFieldValue(p, "i", 20); // This is the count
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
