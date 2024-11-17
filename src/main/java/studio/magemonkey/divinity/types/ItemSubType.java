package studio.magemonkey.divinity.types;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.CodexEngine;
import studio.magemonkey.codex.items.ItemType;
import studio.magemonkey.codex.items.exception.CodexItemException;
import studio.magemonkey.codex.items.providers.VanillaProvider;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.Divinity;

import java.util.HashSet;
import java.util.Set;

public class ItemSubType {

    private String        id;
    private String        name;
    private Set<ItemType> mats;

    public ItemSubType(@NotNull String id, @NotNull String name, @NotNull Set<String> mats) {
        this.id = id.toLowerCase();
        this.setName(name);

        this.mats = new HashSet<>();
        for (String mat : mats) {
            try {
                this.mats.add(CodexEngine.get().getItemManager().getItemType(mat));
            } catch (CodexItemException e) {
                Divinity.getInstance().warn("Unknown item sub type: \"" + mat + '\"');
            }
        }
    }

    @NotNull
    public String getId() {
        return this.id;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull String name) {
        this.name = StringUT.color(name);
    }

    @NotNull
    public Set<ItemType> getMaterials() {
        return this.mats;
    }

    public boolean isItemOfThis(@NotNull ItemStack item) {
        return this.mats.stream().anyMatch(itemType -> itemType.isInstance(item));
    }

    @Deprecated
    public boolean isItemOfThis(@NotNull Material mat) {
        return this.mats.contains(new VanillaProvider.VanillaItemType(mat));
    }

    public boolean isItemOfThis(@NotNull String mat) {
        return this.mats.stream().anyMatch(itemType -> itemType.getNamespacedID().equalsIgnoreCase(mat));
    }
}
