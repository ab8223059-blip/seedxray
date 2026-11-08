package dev.seedxray;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Scans chunks around the player, predicts ore positions from the seed and marks them with
 * client-side glowing item entities (the glow outline is visible through walls).
 */
public final class XrayManager {
    public static final long SEED = 6608149111735331168L;
    private static final int MAX_MARKERS = 1500;

    private static ClientWorld lastWorld;
    private static final Set<Long> scanned = new HashSet<>();
    private static final Map<Long, List<OreSimulator.Hit>> hits = new HashMap<>();
    private static final Map<BlockPos, Entity> markers = new HashMap<>();
    private static final Map<BlockPos, OreType> markerType = new HashMap<>();
    private static int nextId = -1_000_000;
    private static int tickCounter = 0;
    private static boolean dirty = true;

    public static void markDirty() { dirty = true; }

    public static void tick(MinecraftClient mc) {
        ClientWorld w = mc.world;
        if (w == null || mc.player == null) { lastWorld = null; forget(); return; }
        if (w != lastWorld) { forget(); lastWorld = w; }

        XrayConfig cfg = XrayConfig.INSTANCE;
        if (!cfg.enabled) { removeAllMarkers(w); return; }

        int pcx = mc.player.getChunkPos().x, pcz = mc.player.getChunkPos().z;
        int R = Math.max(1, Math.min(12, cfg.radius));

        // scan up to 2 new chunks per tick, nearest first
        int budget = 2;
        for (int ring = 0; ring <= R && budget > 0; ring++) {
            for (int dx = -ring; dx <= ring && budget > 0; dx++) {
                for (int dz = -ring; dz <= ring && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int cx = pcx + dx, cz = pcz + dz;
                    long key = (((long) cx) << 32) ^ (cz & 0xFFFFFFFFL);
                    if (scanned.contains(key)) continue;
                    if (!neighboursLoaded(w, cx, cz)) continue;
                    scanned.add(key);
                    hits.put(key, OreSimulator.scanChunk(w, cx, cz, SEED, cfg));
                    budget--;
                    dirty = true;
                }
            }
        }

        if (dirty || ++tickCounter % 20 == 0) {
            dirty = false;
            syncMarkers(mc, w, pcx, pcz, R, cfg);
        }
    }

    private static boolean neighboursLoaded(ClientWorld w, int cx, int cz) {
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                if (!w.getChunkManager().isChunkLoaded(cx + x, cz + z)) return false;
        return true;
    }

    private static void syncMarkers(MinecraftClient mc, ClientWorld w, int pcx, int pcz, int R, XrayConfig cfg) {
        Vec3d pp = mc.player.getEntityPos();
        List<OreSimulator.Hit> wanted = new ArrayList<>();
        for (Map.Entry<Long, List<OreSimulator.Hit>> e : hits.entrySet()) {
            int cx = (int) (e.getKey() >> 32), cz = (int) (long) e.getKey();
            if (Math.abs(cx - pcx) > R || Math.abs(cz - pcz) > R) continue;
            for (OreSimulator.Hit h : e.getValue()) {
                if (!cfg.isOn(h.type())) continue;
                if (!w.isChunkLoaded(h.pos().getX() >> 4, h.pos().getZ() >> 4)) continue;
                if (w.getBlockState(h.pos()).isAir()) continue;   // already mined / cave
                wanted.add(h);
            }
        }
        wanted.sort(Comparator.comparingDouble(h -> h.pos().getSquaredDistance(pp)));
        if (wanted.size() > MAX_MARKERS) wanted = wanted.subList(0, MAX_MARKERS);

        Set<BlockPos> keep = new HashSet<>();
        for (OreSimulator.Hit h : wanted) {
            keep.add(h.pos());
            if (!markers.containsKey(h.pos())) addMarker(w, h);
        }
        for (Iterator<Map.Entry<BlockPos, Entity>> it = markers.entrySet().iterator(); it.hasNext();) {
            Map.Entry<BlockPos, Entity> e = it.next();
            if (!keep.contains(e.getKey())) {
                dropEntity(w, e.getValue(), markerType.remove(e.getKey()));
                it.remove();
            }
        }
    }

    private static void addMarker(ClientWorld w, OreSimulator.Hit h) {
        ItemEntity ie = new ItemEntity(w, h.pos().getX() + 0.5, h.pos().getY() + 0.3, h.pos().getZ() + 0.5,
                new ItemStack(h.type().item));
        ie.setNoGravity(true);
        ie.setVelocity(Vec3d.ZERO);
        ie.setPickupDelay(32767);
        ie.setGlowing(true);
        ie.setId(nextId--);          // negative ids never collide with server entity ids
        w.addEntity(ie);
        try {
            Scoreboard sb = w.getScoreboard();
            String name = "sxr_" + h.type().name().toLowerCase(Locale.ROOT);
            Team t = sb.getTeam(name);
            if (t == null) { t = sb.addTeam(name); t.setColor(h.type().color); }
            sb.addScoreHolderToTeam(ie.getUuidAsString(), t);
        } catch (Throwable ignored) {}
        markers.put(h.pos(), ie);
        markerType.put(h.pos(), h.type());
    }

    private static void dropEntity(ClientWorld w, Entity e, OreType type) {
        try {
            Scoreboard sb = w.getScoreboard();
            Team t = sb.getScoreHolderTeam(e.getUuidAsString());
            if (t != null) sb.removeScoreHolderFromTeam(e.getUuidAsString(), t);
        } catch (Throwable ignored) {}
        w.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
    }

    private static void removeAllMarkers(ClientWorld w) {
        if (markers.isEmpty()) return;
        for (Map.Entry<BlockPos, Entity> e : markers.entrySet()) dropEntity(w, e.getValue(), null);
        markers.clear();
        markerType.clear();
        dirty = true;
    }

    private static void forget() {
        scanned.clear();
        hits.clear();
        markers.clear();   // entities vanish together with the old world
        markerType.clear();
        dirty = true;
    }

    private XrayManager() {}
}
