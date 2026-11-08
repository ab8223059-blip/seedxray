package dev.seedxray;

import java.util.*;

/**
 * Vanilla ore placement data (1.21.x), step 6 = UNDERGROUND_ORES.
 * "index" = position of the feature in the dimension's sorted feature list.
 * The indices are the part most likely to need calibration - they can be overridden
 * without recompiling in config/seedxray.json ("indexOverrides").
 */
public final class OreTable {
    public record A(int kind, int v) {
        static A abs(int v) { return new A(0, v); }
        static A bottom(int v) { return new A(1, v); }   // minY + v
        static A top(int v) { return new A(2, v); }      // topY - 1 - v
        int get(int minY, int h) {
            return switch (kind) { case 0 -> v; case 1 -> minY + v; default -> minY + h - 1 - v; };
        }
    }

    public record OreDef(String id, OreType type, int dim, int index, int countMin, int countMax, int rarity,
                         boolean trapezoid, A min, A max, int size, float discard, boolean scattered,
                         Set<String> biomes) {}

    public static final List<OreDef> ALL = new ArrayList<>();

    private static void d(String id, OreType t, int dim, int idx, int cMin, int cMax, int rarity, boolean trap,
                          A min, A max, int size, float disc, boolean scat, String... biomes) {
        ALL.add(new OreDef(id, t, dim, idx, cMin, cMax, rarity, trap, min, max, size, disc, scat,
                biomes.length == 0 ? null : Set.of(biomes)));
    }

    static {
        // ---------- Overworld (dim 0) ----------
        d("ore_coal_upper",   OreType.COAL, 0, 9, 30, 30, 0, false, A.abs(136), A.top(0), 17, 0.5f, false);
        d("ore_coal_lower",   OreType.COAL, 0, 10, 20, 20, 0, true,  A.abs(0), A.abs(192), 17, 0.5f, false);
        d("ore_iron_upper",   OreType.IRON, 0, 11, 90, 90, 0, true,  A.abs(80), A.abs(384), 9, 0f, false);
        d("ore_iron_middle",  OreType.IRON, 0, 12, 10, 10, 0, true,  A.abs(-24), A.abs(56), 9, 0f, false);
        d("ore_iron_small",   OreType.IRON, 0, 13, 10, 10, 0, false, A.bottom(0), A.abs(72), 4, 0f, false);
        d("ore_gold",         OreType.GOLD, 0, 14, 4, 4, 0, true,    A.bottom(-64), A.abs(32), 9, 0.5f, false);
        d("ore_gold_lower",   OreType.GOLD, 0, 15, 0, 1, 0, false,   A.bottom(-64), A.bottom(-48), 9, 0.5f, false);
        d("ore_redstone",     OreType.REDSTONE, 0, 16, 4, 4, 0, false, A.bottom(0), A.abs(15), 8, 0f, false);
        d("ore_redstone_lower", OreType.REDSTONE, 0, 17, 8, 8, 0, true, A.bottom(-32), A.bottom(32), 8, 0f, false);
        d("ore_diamond",        OreType.DIAMOND, 0, 18, 7, 7, 0, true, A.bottom(-80), A.bottom(80), 4, 0.5f, false);
        d("ore_diamond_medium", OreType.DIAMOND, 0, 19, 2, 2, 0, false, A.bottom(-4), A.abs(-4), 8, 0.5f, false);
        d("ore_diamond_large",  OreType.DIAMOND, 0, 20, 1, 1, 9, true, A.bottom(-80), A.bottom(80), 12, 0.7f, false);
        d("ore_diamond_buried", OreType.DIAMOND, 0, 21, 4, 4, 0, true, A.bottom(-80), A.bottom(80), 8, 1.0f, false);
        d("ore_lapis",          OreType.LAPIS, 0, 22, 2, 2, 0, true, A.bottom(-32), A.bottom(32), 7, 0.5f, false);
        d("ore_lapis_buried",   OreType.LAPIS, 0, 23, 4, 4, 0, false, A.bottom(0), A.abs(64), 7, 1.0f, false);
        d("ore_copper",         OreType.COPPER, 0, 24, 16, 16, 0, true, A.abs(-16), A.abs(112), 10, 0f, false);
        d("ore_copper_large",   OreType.COPPER, 0, 25, 16, 16, 0, true, A.abs(-16), A.abs(112), 20, 0f, false,
                "dripstone_caves");
        d("ore_emerald",        OreType.EMERALD, 0, 26, 100, 100, 0, true, A.abs(-16), A.abs(480), 3, 0.5f, false,
                "windswept_hills", "windswept_gravelly_hills", "windswept_forest", "meadow", "grove",
                "snowy_slopes", "frozen_peaks", "jagged_peaks", "stony_peaks", "cherry_grove");
        d("ore_gold_extra",     OreType.GOLD, 0, 27, 50, 50, 0, false, A.abs(32), A.abs(256), 9, 0.5f, false,
                "badlands", "eroded_badlands", "wooded_badlands");

        // ---------- Nether (dim 1) ----------
        d("ore_gold_nether",    OreType.NETHER_GOLD, 1, 2, 10, 10, 0, false, A.bottom(10), A.top(10), 10, 0.5f, false,
                "nether_wastes", "crimson_forest", "warped_forest", "soul_sand_valley");
        d("ore_quartz_nether",  OreType.QUARTZ, 1, 3, 16, 16, 0, false, A.bottom(10), A.top(10), 14, 0.5f, false,
                "nether_wastes", "crimson_forest", "warped_forest", "soul_sand_valley");
        d("ore_ancient_debris_large", OreType.ANCIENT_DEBRIS, 1, 4, 1, 1, 0, true, A.abs(8), A.abs(24), 3, 1.0f, true);
        d("ore_debris_small",   OreType.ANCIENT_DEBRIS, 1, 5, 1, 1, 0, false, A.bottom(8), A.top(16), 2, 1.0f, true);
    }

    private OreTable() {}
}
