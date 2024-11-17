package studio.magemonkey.divinity.stats.tiers;

import org.jetbrains.annotations.NotNull;
import studio.magemonkey.codex.util.StringUT;
import studio.magemonkey.divinity.stats.items.ItemTags;

public class Tier {

    private String id;
    private String name;
    private String color;

    public Tier(
            @NotNull String id,
            @NotNull String name,
            @NotNull String color
    ) {
        this.id = id.toLowerCase();
        this.name = StringUT.color(name);
        this.color = StringUT.color(color);
        this.name = this.format(this.name);
    }

    @NotNull
    public String getId() {
        return this.id;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public String getColor() {
        return this.color;
    }

    @NotNull
    public String format(@NotNull String str) {
        return str
                .replace(ItemTags.PLACEHOLDER_TIER_COLOR, this.getColor())
                .replace(ItemTags.PLACEHOLDER_TIER_NAME, this.getName());
    }
}
