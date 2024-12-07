package studio.magemonkey.divinity.nms.packets.versions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.hooks.Hooks;
import studio.magemonkey.codex.nms.packets.IPacketHandler;
import studio.magemonkey.codex.nms.packets.events.EnginePlayerPacketEvent;
import studio.magemonkey.codex.nms.packets.events.EngineServerPacketEvent;
import studio.magemonkey.codex.util.Reflex;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.api.event.EntityEquipmentChangeEvent;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.data.api.DivinityUser;
import studio.magemonkey.divinity.data.api.UserEntityNamesMode;
import studio.magemonkey.divinity.data.api.UserProfile;
import studio.magemonkey.divinity.manager.EntityManager;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class UniversalPacketHandler implements IPacketHandler {

    protected Divinity       plugin;

    public UniversalPacketHandler(@NotNull Divinity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void managePlayerPacket(@NotNull EnginePlayerPacketEvent e) {
        Class playoutParticles        = Reflex.getNMSClass("PacketPlayOutWorldParticles");
        Class playoutUpdateAttributes = Reflex.getNMSClass("PacketPlayOutUpdateAttributes");
        Class playoutEntityMetadata   = Reflex.getNMSClass("PacketPlayOutEntityMetadata");
        Class playOutEntityEquipment  = Reflex.getNMSClass("PacketPlayOutEntityEquipment");

        Object packet = e.getPacket();

        if (EngineCfg.PACKETS_REDUCE_COMBAT_PARTICLES && playoutParticles.isInstance(packet)) {
            this.manageDamageParticle(e, packet);
            return;
        }

        if (playoutUpdateAttributes.isInstance(packet)) {
            this.manageEquipmentChanges(e, packet);
            return;
        }
        if (playoutEntityMetadata.isInstance(packet)) {
            this.manageEntityNames(e, packet);
            return;
        }
        if (playOutEntityEquipment.isInstance(packet)) {
            this.managePlayerHelmet(e, packet);
        }
    }

    @Override
    public void manageServerPacket(@NotNull EngineServerPacketEvent e) {
    }

    public void manageEquipmentChanges(@NotNull EnginePlayerPacketEvent e, @NotNull Object packet) {
        Class playoutUpdateAttributes = Reflex.getNMSClass("PacketPlayOutUpdateAttributes");
        Class craftServerClass        = Reflex.getCraftClass("CraftServer");
        Class nmsEntityClass          = Reflex.getNMSClass("Entity");
        Class worldServerClass        = Reflex.getNMSClass("WorldServer");

        Object equip = playoutUpdateAttributes.cast(packet);

        Integer entityId = (Integer) Reflex.getFieldValue(equip, "a");
        if (entityId == null) return;

        Object server    = craftServerClass.cast(Bukkit.getServer());
        Object nmsEntity = null;

        Object dedicatedServer = Reflex.invokeMethod(
                Reflex.getMethod(craftServerClass, "getServer"),
                server
        );

        Iterable<?> worlds = (Iterable<?>) Reflex.invokeMethod(
                Reflex.getMethod(dedicatedServer.getClass(), "getWorlds"),
                dedicatedServer
        );

        Method getEntity = Reflex.getMethod(worldServerClass, "getEntity", int.class);
        for (Object worldServer : worlds) {
            nmsEntity = Reflex.invokeMethod(getEntity, worldServer, entityId.intValue());
            if (nmsEntity != null) {
                break;
            }
        }

        if (nmsEntity == null) return;


        Method getUniqueId = Reflex.getMethod(nmsEntityClass, "getUniqueID");

        Entity bukkitEntity =
                CodexEngine.get().getServer().getEntity((UUID) Reflex.invokeMethod(getUniqueId, nmsEntity));
        if (!(bukkitEntity instanceof LivingEntity)) return;
        if (EntityManager.isPacketDuplicatorFixed(bukkitEntity)
                || !EntityManager.isEquipmentNew((LivingEntity) bukkitEntity)) return;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            EntityEquipmentChangeEvent event = new EntityEquipmentChangeEvent((LivingEntity) bukkitEntity);
            plugin.getServer().getPluginManager().callEvent(event);
        });
    }

    protected void manageDamageParticle(@NotNull EnginePlayerPacketEvent e, @NotNull Object packet) {
        Class packetParticlesClass = Reflex.getNMSClass("PacketPlayOutWorldParticles");
        Class particleParamClass   = Reflex.getNMSClass("ParticleParam");

        Object p = packetParticlesClass.cast(packet);

        Object j = Reflex.getFieldValue(p, "j");
        if (j == null) return;

        Method a = Reflex.getMethod(particleParamClass, "a");

        String name = (String) Reflex.invokeMethod(a, j);
        if (name.contains("damage_indicator")) {
            Reflex.setFieldValue(p, "h", 20);
        }
    }

    protected void manageEntityNames(@NotNull EnginePlayerPacketEvent e, @NotNull Object packet) {
        DivinityUser user = plugin.getUserManager().getOrLoadUser(e.getReciever());
        if (user == null) return;

        UserProfile         profile   = user.getActiveProfile();
        UserEntityNamesMode namesMode = profile.getNamesMode();
        if (namesMode == UserEntityNamesMode.DEFAULT) return;

        Class pClass = Reflex.getNMSClass("PacketPlayOutEntityMetadata");

        Object p = pClass.cast(packet);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) Reflex.getFieldValue(p, "b");
        if (list == null) return;

        // Hide or show custom entity names
        if (list.size() > 13) {
            Object index3 = list.get(13);

            Method bMethod = Reflex.getMethod(index3.getClass(), "b");

            Object b = Reflex.invokeMethod(bMethod, index3);
            if (b == null || !b.getClass().equals(Boolean.class)) return;
            //Object nameVisible = Reflex.getFieldValue(index3, "b");

            boolean visibility = namesMode == UserEntityNamesMode.ALWAYS_VISIBLE;
            Reflex.setFieldValue(index3, "b", visibility);
        }
    }

    protected void managePlayerHelmet(@NotNull EnginePlayerPacketEvent e, @NotNull Object packet) {
        Class playOutEntityEquipment = Reflex.getNMSClass("PacketPlayOutEntityEquipment");
        Class enumItemSlotClass      = Reflex.getNMSClass("EnumItemSlot");

        Object p = playOutEntityEquipment.cast(packet);

        @SuppressWarnings("unchecked")
        List<Object> slots = (List<Object>) Reflex.getFieldValue(p, "b");
        if (slots == null || !slots.contains(Reflex.getEnum(enumItemSlotClass, "HEAD"))) return;

        Integer entityId = (Integer) Reflex.getFieldValue(p, "a");
        if (entityId == null) return;

        Class craftServerClass = Reflex.getCraftClass("CraftServer");
        Class nmsEntityClass   = Reflex.getNMSClass("Entity");
        Class worldServerClass = Reflex.getNMSClass("WorldServer");

        Object server    = craftServerClass.cast(Bukkit.getServer());
        Object nmsEntity = null;
        Object dedicatedServer = Reflex.invokeMethod(
                Reflex.getMethod(craftServerClass, "getServer"),
                server
        );

        Iterable<?> worlds = (Iterable<?>) Reflex.invokeMethod(
                Reflex.getMethod(dedicatedServer.getClass(), "getWorlds"),
                dedicatedServer
        );

        Method getEntity = Reflex.getMethod(worldServerClass, "getEntity", int.class);
        for (Object worldServer : worlds) {
            nmsEntity = Reflex.invokeMethod(getEntity, worldServer, entityId.intValue());
            if (nmsEntity != null) {
                break;
            }
        }

        if (nmsEntity == null) return;


        Method getUniqueId = Reflex.getMethod(nmsEntityClass, "getUniqueID");

        Entity bukkitEntity =
                CodexEngine.get().getServer().getEntity((UUID) Reflex.invokeMethod(getUniqueId, nmsEntity));
        if (bukkitEntity == null || Hooks.isNPC(bukkitEntity) || !(bukkitEntity instanceof Player)) return;

        Player       player = (Player) bukkitEntity;
        DivinityUser user   = plugin.getUserManager().getOrLoadUser(player);
        if (user == null) return;

        UserProfile profile = user.getActiveProfile();
        if (profile.isHideHelmet()) {
            Reflex.setFieldValue(p, "c", Reflex.getFieldValue(Reflex.getNMSClass("ItemStack"), "a"));
        }
    }
}
