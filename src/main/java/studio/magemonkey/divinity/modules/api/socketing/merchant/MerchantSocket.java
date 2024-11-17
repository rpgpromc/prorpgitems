package studio.magemonkey.divinity.modules.api.socketing.merchant;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.codex.manager.api.Loadable;
import studio.magemonkey.codex.modules.IModuleExecutor;
import studio.magemonkey.divinity.Divinity;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.api.socketing.ModuleSocket;

public class MerchantSocket implements Loadable {

    private Divinity        plugin;
    private ModuleSocket<?> moduleSocket;
    private JYML            cfg;

    private int     socketChanceBonusAmount;
    private int     socketChanceBonusMax;
    private boolean socketSilentRateEnabled;

    private double socketWorthModifier;
    private double itemWorthModifier;

    private MerchantGUI merchantGUI;

    public MerchantSocket(@NotNull ModuleSocket<?> moduleSocket) {
        this.plugin = moduleSocket.plugin;
        this.moduleSocket = moduleSocket;
    }

    @Override
    public void setup() {
        try {
            this.cfg = JYML.loadOrExtract(plugin, this.moduleSocket.getPath() + "merchant.yml");
        } catch (InvalidConfigurationException e) {
            this.plugin.error("Failed to load merchant config (" + this.moduleSocket.getPath()
                    + "merchant.yml): Configuration error");
            e.printStackTrace();
            shutdown();
            return;
        }

        String path = "socketing.";
        this.socketChanceBonusAmount = cfg.getInt(path + "chance.merchant-bonus.amount", 15);
        this.socketChanceBonusMax = cfg.getInt(path + "chance.merchant-bonus.maximal", 80);
        this.socketSilentRateEnabled = cfg.getBoolean(path + "silent-rate-bonus.enabled", false);

        path = "price.";
        this.socketWorthModifier = cfg.getDouble(path + "socket-worth-modifier", 1.0);
        this.itemWorthModifier = cfg.getDouble(path + "item-worth-modifier", 0.6);

        this.merchantGUI = new MerchantGUI(this.moduleSocket, this);

        IModuleExecutor<Divinity> exec = this.moduleSocket.getExecutor();
        if (exec != null) {
            exec.addSubCommand(new MerchantCmd(this.moduleSocket, this));
        }
    }

    @Override
    public void shutdown() {
        if (this.merchantGUI != null) {
            this.merchantGUI.shutdown();
            this.merchantGUI = null;
        }
        this.cfg = null;
    }

    public void openMerchantGUI(@NotNull Player player, boolean force) {
        if (!force && !player.hasPermission(Perms.getSocketGuiMerchant(this.moduleSocket))) {
            plugin.lang().Error_NoPerm.send(player);
            return;
        }
        this.getMerchantGUI().open(player, 1);
    }

    @NotNull
    public JYML getConfig() {
        return cfg;
    }

    @NotNull
    public MerchantGUI getMerchantGUI() {
        return merchantGUI;
    }

    public int getSocketChanceBonusAmount() {
        return socketChanceBonusAmount;
    }

    public int getSocketChanceBonusMax() {
        return socketChanceBonusMax;
    }

    public boolean isSocketSilentRateEnabled() {
        return socketSilentRateEnabled;
    }

    public double getSocketWorthModifier() {
        return socketWorthModifier;
    }

    public double getItemWorthModifier() {
        return itemWorthModifier;
    }
}
