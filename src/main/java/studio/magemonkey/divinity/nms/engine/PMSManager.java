package studio.magemonkey.divinity.nms.engine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.config.EngineCfg;
import studio.magemonkey.divinity.nms.packets.PacketManager;

public class PMSManager {

    private final Divinity      plugin;
    private       PMS           nmsEngine;
    private       PacketManager packetManager;

    public PMSManager(@NotNull Divinity plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        nmsEngine = new PMS();

        if (EngineCfg.PACKETS_ENABLED) {
            this.plugin.info("Packets are enabled. Setup packet manager...");
            this.packetManager = new PacketManager(this.plugin);
            this.packetManager.setup();
        }
    }

    public void shutdown() {
        this.nmsEngine = null;
        if (this.packetManager != null) {
            this.packetManager.shutdown();
            this.packetManager = null;
        }
    }

    public PMS get() {
        return this.nmsEngine;
    }

    @Nullable
    public PacketManager getPacketManager() {
        return this.packetManager;
    }
}
