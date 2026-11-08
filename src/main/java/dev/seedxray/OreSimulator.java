package dev.seedxray;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/** Re-implements vanilla ore decoration (placement modifiers + OreFeature / ScatteredOreFeature). */
public final class OreSimulator {
    public record Hit(BlockPos pos, OreType type) {}

    private static final int STEP_UNDERGROUND_ORES = 6;

    public static List<Hit> scanChunk(ClientWorld w, int cx, int cz, long worldSeed, XrayConfig cfg) {
        int dim = w.getRegistryKey().equals(World.OVERWORLD) ? 0 : w.getRegistryKey().equals(World.NETHER) ? 1 : -1;
        List<Hit> out = new ArrayList<>();
        if (dim < 0) return out;

        int ox = cx << 4, oz = cz << 4;
        McRandom r = new McRandom(worldSeed);
        long l = r.nextLong() | 1L;
        long m = r.nextLong() | 1L;
        long popSeed = ((long) ox * l + (long) oz * m) ^ worldSeed;

        int minY = w.getBottomY(), h = w.getHeight();
        for (OreTable.OreDef d : OreTable.ALL) {
            if (d.dim() != dim) continue;
            int idx = cfg.indexOverrides.getOrDefault(d.id(), d.index());
            r.setSeed(popSeed + idx + 10000L * STEP_UNDERGROUND_ORES);

            int n;
            if (d.rarity() > 0) n = r.nextFloat() < 1.0f / d.rarity() ? 1 : 0;
            else if (d.countMax() > d.countMin()) n = r.nextInt(d.countMax() - d.countMin() + 1) + d.countMin();
            else n = d.countMin();

            for (int i = 0; i < n; i++) {
                int x = ox + r.nextInt(16);
                int z = oz + r.nextInt(16);
                int y = height(r, d, minY, h);
                BlockPos p = new BlockPos(x, y, z);
                if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
                if (d.biomes() != null) {
                    String b = w.getBiome(p).getKey().map(k -> k.getValue().getPath()).orElse("");
                    if (!d.biomes().contains(b)) continue;
                }
                if (d.scattered()) scattered(w, r, x, y, z, d, dim, out);
                else vein(w, r, x, y, z, d, dim, out);
            }
        }
        return out;
    }

    private static int height(McRandom r, OreTable.OreDef d, int minY, int h) {
        int lo = d.min().get(minY, h), hi = d.max().get(minY, h);
        if (lo > hi) return (lo + hi) / 2;
        if (!d.trapezoid()) return r.nextInt(hi - lo + 1) + lo;
        int l = hi - lo;
        if (l == 0) return lo + r.nextInt(1);
        int m = l / 2;
        int n = l - m;
        int a = r.nextInt(n + 1);
        int b = r.nextInt(m + 1);
        return lo + a + b;
    }

    // ---------- block tests ----------
    private static boolean isOreBlock(BlockState s) {
        String p = Registries.BLOCK.getId(s.getBlock()).getPath();
        return p.endsWith("_ore") || p.equals("ancient_debris");
    }

    private static boolean isTarget(BlockState s, int dim) {
        if (dim == 1) return s.isIn(BlockTags.BASE_STONE_NETHER) || isOreBlock(s);
        return s.isIn(BlockTags.STONE_ORE_REPLACEABLES) || s.isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || isOreBlock(s);
    }

    private static boolean shouldNotDiscard(McRandom r, float chance) {
        if (chance <= 0.0f) return true;
        if (chance >= 1.0f) return false;
        return r.nextFloat() >= chance;
    }

    private static boolean adjacentToAir(ClientWorld w, int x, int y, int z) {
        BlockPos.Mutable p = new BlockPos.Mutable();
        int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        for (int[] d : dirs) {
            p.set(x + d[0], y + d[1], z + d[2]);
            if (w.getBlockState(p).isAir()) return true;
        }
        return false;
    }

    private static boolean shouldPlace(ClientWorld w, McRandom r, OreTable.OreDef d, int dim, int x, int y, int z,
                                       BlockState s) {
        if (!isTarget(s, dim)) return false;
        if (shouldNotDiscard(r, d.discard())) return true;
        return !adjacentToAir(w, x, y, z);
    }

    // ---------- ScatteredOreFeature (ancient debris) ----------
    private static int spread(McRandom r, int spread) {
        return Math.round((r.nextFloat() - r.nextFloat()) * (float) spread);
    }

    private static void scattered(ClientWorld w, McRandom r, int ox, int oy, int oz, OreTable.OreDef d, int dim,
                                  List<Hit> out) {
        int count = r.nextInt(d.size() + 1);
        BlockPos.Mutable p = new BlockPos.Mutable();
        for (int j = 0; j < count; j++) {
            int s = Math.min(j, 7);
            int dx = spread(r, s), dy = spread(r, s), dz = spread(r, s);
            int x = ox + dx, y = oy + dy, z = oz + dz;
            if (y < w.getBottomY() || y >= w.getBottomY() + w.getHeight()) continue;
            p.set(x, y, z);
            BlockState st = w.getBlockState(p);
            if (shouldPlace(w, r, d, dim, x, y, z, st)) out.add(new Hit(p.toImmutable(), d.type()));
        }
    }

    // ---------- OreFeature ----------
    private static void vein(ClientWorld w, McRandom r, int px, int py, int pz, OreTable.OreDef d, int dim,
                             List<Hit> out) {
        int size = d.size();
        float f = r.nextFloat() * (float) Math.PI;
        float g = size / 8.0f;
        int i = MathHelper.ceil((size / 16.0f * 2.0f + 1.0f) / 2.0f);
        double sx = px + Math.sin(f) * g;
        double ex = px - Math.sin(f) * g;
        double sz = pz + Math.cos(f) * g;
        double ez = pz - Math.cos(f) * g;
        double sy = py + r.nextInt(3) - 2;
        double ey = py + r.nextInt(3) - 2;
        int n = px - MathHelper.ceil(g) - i;
        int o = py - 2 - i;
        int p = pz - MathHelper.ceil(g) - i;
        int q = 2 * (MathHelper.ceil(g) + i);
        int rr = 2 * (2 + i);
        part(w, r, d, dim, sx, ex, sz, ez, sy, ey, n, o, p, q, rr, out);
    }

    private static void part(ClientWorld w, McRandom r, OreTable.OreDef d, int dim, double sx, double ex, double sz,
                             double ez, double sy, double ey, int x0, int y0, int z0, int hs, int vs, List<Hit> out) {
        int size = d.size();
        BitSet bits = new BitSet(hs * vs * hs);
        double[] ds = new double[size * 4];
        for (int k = 0; k < size; k++) {
            float f = (float) k / (float) size;
            double a = MathHelper.lerp((double) f, sx, ex);
            double b = MathHelper.lerp((double) f, sy, ey);
            double c = MathHelper.lerp((double) f, sz, ez);
            double hh = r.nextDouble() * (double) size / 16.0;
            double l = ((double) (MathHelper.sin((float) Math.PI * f) + 1.0f) * hh + 1.0) / 2.0;
            ds[k * 4] = a; ds[k * 4 + 1] = b; ds[k * 4 + 2] = c; ds[k * 4 + 3] = l;
        }
        for (int k = 0; k < size - 1; k++) {
            double a = ds[k * 4 + 3];
            if (a <= 0.0) continue;
            for (int m = k + 1; m < size; m++) {
                double b = ds[m * 4 + 3];
                if (b <= 0.0) continue;
                double dx = ds[k * 4] - ds[m * 4];
                double dy = ds[k * 4 + 1] - ds[m * 4 + 1];
                double dz = ds[k * 4 + 2] - ds[m * 4 + 2];
                double dr = a - b;
                if (dr * dr > dx * dx + dy * dy + dz * dz) {
                    if (dr > 0.0) ds[m * 4 + 3] = -1.0; else ds[k * 4 + 3] = -1.0;
                }
            }
        }
        int minY = w.getBottomY(), maxY = minY + w.getHeight();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int k = 0; k < size; k++) {
            double l = ds[k * 4 + 3];
            if (l < 0.0) continue;
            double a = ds[k * 4], b = ds[k * 4 + 1], c = ds[k * 4 + 2];
            int p = Math.max(MathHelper.floor(a - l), x0);
            int q = Math.max(MathHelper.floor(b - l), y0);
            int s = Math.max(MathHelper.floor(c - l), z0);
            int t = Math.max(MathHelper.floor(a + l), p);
            int u = Math.max(MathHelper.floor(b + l), q);
            int v = Math.max(MathHelper.floor(c + l), s);
            for (int x = p; x <= t; x++) {
                double dx = ((double) x + 0.5 - a) / l;
                if (dx * dx >= 1.0) continue;
                for (int y = q; y <= u; y++) {
                    double dy = ((double) y + 0.5 - b) / l;
                    if (dx * dx + dy * dy >= 2.0) continue;
                    for (int z = s; z <= v; z++) {
                        double dz = ((double) z + 0.5 - c) / l;
                        if (dx * dx + dy * dy + dz * dz >= 1.0) continue;
                        if (y < minY || y >= maxY) continue;
                        int index = x - x0 + (y - y0) * hs + (z - z0) * hs * vs;
                        if (bits.get(index)) continue;
                        bits.set(index);
                        pos.set(x, y, z);
                        BlockState st = w.getBlockState(pos);
                        if (shouldPlace(w, r, d, dim, x, y, z, st)) out.add(new Hit(pos.toImmutable(), d.type()));
                    }
                }
            }
        }
    }

    private OreSimulator() {}
}
