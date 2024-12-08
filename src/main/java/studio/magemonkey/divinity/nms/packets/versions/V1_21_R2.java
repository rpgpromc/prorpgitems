package studio.magemonkey.divinity.nms.packets.versions;

import com.mojang.datafixers.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.VersionManager;
import studio.magemonkey.codex.core.Version;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.nms.packets.events.EnginePlayerPacketEvent;
import studio.magemonkey.codex.util.Reflex;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.event.EntityEquipmentChangeEvent;
import studio.magemonkey.divinity.data.api.DivinityUser;
import studio.magemonkey.divinity.data.api.UserProfile;
import studio.magemonkey.divinity.manager.EntityManager;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class V1_21_R2 extends V1_21_R1 {
    private final Class<?> playoutUpdateAttributes = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutUpdateAttributes");
    private final Class<?> craftServerClass        = Reflex.getCraftClass("CraftServer");
    private final Class<?> nmsEntityClass          = Reflex.getClass("net.minecraft.world.entity", "Entity");
    private final Class<?> worldServerClass        = Reflex.getClass("net.minecraft.server.level", "WorldServer");
    private final Method   getEntity               = Reflex.getMethod(worldServerClass, "a", int.class);
    private final Method   getServer               = Reflex.getMethod(craftServerClass, "getServer");

    public V1_21_R2(@NotNull Divinity plugin) {super(plugin);}

    @Override
    public void manageEquipmentChanges(@NotNull EnginePlayerPacketEvent e, @NotNull Object packet) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Object equip = playoutUpdateAttributes.cast(packet);

            Integer entityId = (Integer) Reflex.getFieldValue(equip, "b");
            if (entityId == null) return;

            Object server    = craftServerClass.cast(Bukkit.getServer());
            Object nmsEntity = null;

            Object dedicatedServer = Reflex.invokeMethod(
                    getServer,
                    server
            );

            Iterable<?> worlds = (Iterable<?>) Reflex.invokeMethod(
                    Reflex.getMethod(dedicatedServer.getClass(), "L"), // Get worlds (getAllLevels)
                    dedicatedServer
            );

            for (Object worldServer : worlds) {
                nmsEntity = Reflex.invokeMethod(getEntity, worldServer, entityId.intValue());
                if (nmsEntity != null) {
                    break;
                }
            }

            if (nmsEntity == null) return;

            Method getUniqueId = Reflex.getMethod(nmsEntityClass, "cG");
            Entity bukkitEntity =
                    CodexEngine.get().getServer().getEntity((UUID) Reflex.invokeMethod(getUniqueId, nmsEntity));

            if (!(bukkitEntity instanceof LivingEntity)) return;
            if (EntityManager.isPacketDuplicatorFixed(bukkitEntity)
                    || !EntityManager.isEquipmentNew((LivingEntity) bukkitEntity)) return;

            EntityEquipmentChangeEvent event = new EntityEquipmentChangeEvent((LivingEntity) bukkitEntity);
            plugin.getServer().getPluginManager().callEvent(event);
        });
    }

    @Override
    protected void managePlayerHelmet(@NotNull EnginePlayerPacketEvent e, @NotNull Object packet) {
        Bukkit.getScheduler().runTask(Divinity.getInstance(), () -> {
            Class playOutEntityEquipment = Reflex.getClass(PACKET_LOCATION, "PacketPlayOutEntityEquipment");
            Class enumItemSlotClass      = Reflex.getClass("net.minecraft.world.entity", "EnumItemSlot");

            Object p = playOutEntityEquipment.cast(packet);

            @SuppressWarnings("unchecked")
            List<Pair<Object, Object>> slots = (List<Pair<Object, Object>>) Reflex.getFieldValue(p, "d");
            Pair<Object, Object> helmet = null;
            for (Pair<Object, Object> pair : slots) {
                Enum head = (Enum) Reflex.invokeMethod(
                        Reflex.getMethod(enumItemSlotClass, "a", String.class), //fromName
                        null, "head");
                if (pair.getFirst() == head) {
                    helmet = pair;
                    break;
                }
            }
            if (slots == null || helmet == null) return;

            Integer entityId = (Integer) Reflex.getFieldValue(p, "c");
            if (entityId == null) return;
            Class craftServerClass = Reflex.getCraftClass("CraftServer");
            Class nmsEntityClass   = Reflex.getClass("net.minecraft.world.entity", "Entity");
            Class worldServerClass = Reflex.getClass("net.minecraft.server.level", "WorldServer");

            Object server    = craftServerClass.cast(Bukkit.getServer());
            Object nmsEntity = null;
            Object dedicatedServer = Reflex.invokeMethod(
                    Reflex.getMethod(craftServerClass, "getServer"),
                    server
            );

            Iterable<?> worlds = (Iterable<?>) Reflex.invokeMethod(
                    Reflex.getMethod(dedicatedServer.getClass(), "L"), //Get worlds (getAllLevels)
                    dedicatedServer
            );

            Method getEntity = Reflex.getMethod(worldServerClass, "a", int.class);
            for (Object worldServer : worlds) {
                nmsEntity = Reflex.invokeMethod(getEntity, worldServer, entityId.intValue());
                if (nmsEntity != null) {
                    break;
                }
            }

            if (nmsEntity == null) return;


            Method getUniqueId = Reflex.getMethod(nmsEntityClass, "cG");
            Entity bukkitEntity =
                    CodexEngine.get().getServer().getEntity((UUID) Reflex.invokeMethod(getUniqueId, nmsEntity));

            if (bukkitEntity == null || Hooks.isNPC(bukkitEntity) || !(bukkitEntity instanceof Player)) return;

            Player       player = (Player) bukkitEntity;
            DivinityUser user   = plugin.getUserManager().getOrLoadUser(player);
            if (user == null) return;

            UserProfile profile = user.getActiveProfile();
            if (profile.isHideHelmet()) {
                ItemStack air = new ItemStack(Material.AIR);
                slots.remove(helmet);
                slots.add(new Pair<>(helmet.getFirst(), VersionManager.getNms().getNMSCopy(air)));
            }
        });
    }

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
            String   mcKey        = "minecraft:damage_indicator";
            Class<?> keyClass     = Reflex.getClass("net.minecraft.resources.MinecraftKey");
            Object key = Version.CURRENT.isAtLeast(Version.V1_21_R2) ? Reflex.getConstructor(keyClass,
                            String.class,
                            String.class)
                    .newInstance("minecraft", "damage_indicator")
                    : Reflex.getConstructor(keyClass, String.class).newInstance(mcKey);
            Object damageIndicator = Reflex.invokeMethod(
                    Reflex.getMethod(particleRegistry.getClass(), "a", keyClass),
                    particleRegistry,
                    key
            );
            if (particleType.equals(damageIndicator)) {
                Reflex.setFieldValue(p, "i", 20); // This is the count
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
