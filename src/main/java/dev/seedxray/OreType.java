package dev.seedxray;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;

public enum OreType {
    COAL("Coal", Items.COAL_ORE, Formatting.DARK_GRAY),
    IRON("Iron", Items.IRON_ORE, Formatting.GOLD),
    COPPER("Copper", Items.COPPER_ORE, Formatting.RED),
    GOLD("Gold", Items.GOLD_ORE, Formatting.YELLOW),
    REDSTONE("Redstone", Items.REDSTONE_ORE, Formatting.DARK_RED),
    LAPIS("Lapis", Items.LAPIS_ORE, Formatting.BLUE),
    DIAMOND("Diamond", Items.DIAMOND_ORE, Formatting.AQUA),
    EMERALD("Emerald", Items.EMERALD_ORE, Formatting.GREEN),
    NETHER_GOLD("Nether Gold", Items.NETHER_GOLD_ORE, Formatting.YELLOW),
    QUARTZ("Nether Quartz", Items.NETHER_QUARTZ_ORE, Formatting.WHITE),
    ANCIENT_DEBRIS("Ancient Debris", Items.ANCIENT_DEBRIS, Formatting.LIGHT_PURPLE);

    public final String label;
    public final Item item;
    public final Formatting color;

    OreType(String label, Item item, Formatting color) {
        this.label = label;
        this.item = item;
        this.color = color;
    }
}
