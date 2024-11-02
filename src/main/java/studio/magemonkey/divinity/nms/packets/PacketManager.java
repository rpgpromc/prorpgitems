package studio.magemonkey.divinity.nms.packets;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.core.Version;
import studio.magemonkey.codex.nms.packets.IPacketHandler;
import studio.magemonkey.codex.util.Reflex;
import studio.magemonkey.divinity.Divinity;

public class PacketManager {
    private final Divinity       plugin;
    private       IPacketHandler packetHandler;

    public PacketManager(@NotNull Divinity plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        if (Version.TEST.isCurrent()) return;
        String cur = Version.CURRENT.name().toUpperCase();
        try {
            String   pack  = this.getClass().getPackage().getName() + ".versions";
            Class<?> clazz = Reflex.getClass(pack, cur);
            if (clazz == null) return;

            packetHandler = (IPacketHandler) clazz.getConstructor(Divinity.class).newInstance(plugin);
            this.plugin.getPacketManager().registerHandler(this.packetHandler);
            this.plugin.info("Registered packet manager for version " + ChatColor.GREEN + cur);
        } catch (Exception e) {
            this.plugin.error("Could not register PacketHandler!");
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (this.packetHandler != null) {
            this.plugin.getPacketManager().unregisterHandler(this.packetHandler);
            this.packetHandler = null;
        }
    }
}
