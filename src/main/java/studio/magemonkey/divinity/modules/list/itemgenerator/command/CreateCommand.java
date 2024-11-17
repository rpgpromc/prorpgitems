package studio.magemonkey.divinity.modules.list.itemgenerator.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.config.api.JYML;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.modules.command.MCmd;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Objects;

public class CreateCommand extends MCmd<ItemGeneratorManager> {

    public CreateCommand(@NotNull ItemGeneratorManager module) {
        super(module, new String[]{"create"}, Perms.ADMIN);
    }

    @NotNull
    @Override
    public String usage() {return "<id>";}

    @NotNull
    @Override
    public String description() {return plugin.lang().ItemGenerator_Cmd_Create_Desc.getMsg();}

    @Override
    public boolean playersOnly() {return false;}

    @Override
    protected void perform(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
        if (strings.length != 2) {
            this.printUsage(commandSender);
            return;
        }
        if (module.getItemById(strings[1]) != null) {
            plugin.lang().ItemGenerator_Cmd_Create_Error_ExistingId.send(commandSender);
            return;
        }
        File file = new File(plugin.getDataFolder() + module.getPath() + "items/" + strings[1] + ".yml");
        if (file.exists()) {
            plugin.lang().ItemGenerator_Cmd_Create_Error_ExistingFile.send(commandSender);
            return;
        }
        try (InputStreamReader in = new InputStreamReader(Objects.requireNonNull(plugin.getClass()
                .getResourceAsStream(module.getPath() + "items/common.yml")))) {
            JYML cfg = new JYML(file);
            cfg.load(in);
            cfg.save();
            module.load(strings[1], cfg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        plugin.lang().ItemGenerator_Cmd_Create_Done.replace("%id%", strings[1]).send(commandSender);
    }
}
