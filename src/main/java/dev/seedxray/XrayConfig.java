package dev.seedxray;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class XrayConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("seedxray.json");
    public static XrayConfig INSTANCE = load();

    public boolean enabled = true;
    public int radius = 6;                                   // in chunks
    public Map<String, Boolean> ores = new HashMap<>();      // OreType name -> enabled
    public Map<String, Integer> indexOverrides = new HashMap<>(); // feature id -> index

    public boolean isOn(OreType t) { return ores.getOrDefault(t.name(), true); }
    public void set(OreType t, boolean v) { ores.put(t.name(), v); }

    private static XrayConfig load() {
        try (Reader rd = Files.newBufferedReader(FILE)) {
            XrayConfig c = GSON.fromJson(rd, XrayConfig.class);
            if (c != null) return c;
        } catch (Exception ignored) {}
        return new XrayConfig();
    }

    public void save() {
        try (Writer wr = Files.newBufferedWriter(FILE)) {
            GSON.toJson(this, wr);
        } catch (Exception ignored) {}
    }
}
