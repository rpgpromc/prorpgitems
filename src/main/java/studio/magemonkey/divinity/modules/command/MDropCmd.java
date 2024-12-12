package studio.magemonkey.divinity.modules.command;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.api.items.ItemType;
import studio.magemonkey.codex.api.items.exception.MissingItemException;
import studio.magemonkey.codex.api.items.exception.MissingProviderException;
import studio.magemonkey.codex.util.ItemUT;
import studio.magemonkey.codex.util.LocUT;
import studio.magemonkey.codex.util.random.Rnd;
import studio.magemonkey.divinity.Perms;
import studio.magemonkey.divinity.api.DivinityAPI;
import studio.magemonkey.divinity.modules.api.QModuleDrop;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager;
import studio.magemonkey.divinity.modules.list.itemgenerator.ItemGeneratorManager.GeneratorItem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MDropCmd extends MCmd<QModuleDrop<?>> {

    public MDropCmd(@NotNull QModuleDrop<?> m) {
        super(m, new String[]{"drop"}, Perms.ADMIN);
    }

    @Override
    @NotNull
    public String usage() {
        return plugin.lang().Module_Cmd_Drop_Usage.getMsg();
    }

    @Override
    @NotNull
    public String description() {
        return plugin.lang().Module_Cmd_Drop_Desc.getMsg();
    }

    @Override
    public boolean playersOnly() {
        return false;
    }

    @Override
    @NotNull
    public List<String> getTab(@NotNull Player player, int i, @NotNull String[] args) {
        if (i == 1) {
            return LocUT.getWorldNames();
        }
        if (i == 2) {
            return Arrays.asList("<x>");
        }
        if (i == 3) {
            return Arrays.asList("<y>");
        }
        if (i == 4) {
            return Arrays.asList("<z>");
        }
        if (i == 5) {
            return module.getItemIds();
        }
        if (i == 6) {
            return Arrays.asList("[level]", "-1", "1:5", "1");
        }
        if (i == 7) {
            return Arrays.asList("1", "10"); // Amount
        }

        // Support for material argument for ItemGenerator
        if (i == 8 && this.module instanceof ItemGeneratorManager) {
            ItemGeneratorManager itemGeneratorManager = (ItemGeneratorManager) this.module;
            GeneratorItem        generatorItem        = itemGeneratorManager.getItemById(args[5]);
            if (generatorItem != null) {
                List<String> list = generatorItem.getMaterialsList().stream()
                        .map(ItemType::getNamespacedID).collect(Collectors.toList());
                return list;
            }
        }
        return super.getTab(player, i, args);
    }

    @Override
    public void perform(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length < 7) {
            this.printUsage(sender);
            return;
        }

        World world = plugin.getServer().getWorld(args[1]);
        if (world == null) {
            plugin.lang().Error_NoWorld.replace("%world%", args[1]).send(sender);
            return;
        }

        double x = this.getNumD(sender, args[2], 0, true);
        double y = this.getNumD(sender, args[3], 0, true);
        double z = this.getNumD(sender, args[4], 0, true);

        int lMin   = -1;
        int lMax   = -1;
        int amount = 1;

        if (args.length >= 7) {
            if (args[6].contains(":")) {
                String[] split = args[6].split(":");
                lMin = this.getNumI(sender, split[0], -1, true);
                lMax = this.getNumI(sender, split[1], -1, true);
            } else {
                lMin = lMax = this.getNumI(sender, args[6], -1, true);
            }
        }

        if (args.length >= 8) {
            amount = this.getNumI(sender, args[7], 1);
        }

        String    id   = args[5];
        ItemStack item = null;
        Location  loc  = new Location(world, x, y, z);

        ItemType material;
        try {
            material = args.length >= 9 ? CodexEngine.get().getItemManager().getItemType(args[8]) : null;
        } catch (MissingProviderException | MissingItemException e) {
            material = null;
        }
        ItemGeneratorManager itemGenerator =
                this.module instanceof ItemGeneratorManager ? (ItemGeneratorManager) this.module : null;
        GeneratorItem generatorItem = itemGenerator != null ? itemGenerator.getItemById(id) : null;

        Map<String, Integer> addedItems = new HashMap<>();

        for (int i = 0; i < amount; i++) {
            int iLevel = Rnd.get(lMin, lMax);

            if (material != null && generatorItem != null) {
                item = generatorItem.create(iLevel, -1, material);
            } else {
                item = DivinityAPI.getItemByModule(this.module, id, iLevel, -1, -1);
            }
            if (item == null) continue;

            world.dropItemNaturally(loc, item);

            String name = ItemUT.getItemName(item);
            addedItems.put(name,
                    addedItems.containsKey(name) ? addedItems.get(name) + item.getAmount() : item.getAmount());
        }

        for (String name : addedItems.keySet()) {
            plugin.lang().Module_Cmd_Drop_Done
                    .replace("%w%", world.getName())
                    .replace("%x%", String.valueOf(x))
                    .replace("%y%", String.valueOf(y))
                    .replace("%z%", String.valueOf(z))
                    .replace("%item%", name)
                    .replace("%amount%", String.valueOf(addedItems.get(name)))
                    .send(sender);
        }
    }
}
