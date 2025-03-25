package ru.nern.anglewarp.managers;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.WorldSavePath;
import org.apache.commons.lang3.StringUtils;
import ru.nern.anglewarp.AngleWarp;
import ru.nern.anglewarp.model.WarpPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class WarpPointManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public final List<WarpPoint> points = new ArrayList<>();

    public WarpPoint getPointById(String id) {
        if(id == null) return null;

        for(WarpPoint point : points) {
            if(point.id.equals(id)) return point;
        }

        return null;
    }

    public void savePoints() {
        try {
            File file = getWarpPointsSaveFile();

            if(!shouldSave(file)) return;
            if(shouldCreateSaveDirectory()) Files.createDirectories(file.toPath().getParent());

            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(gson.toJson(preprocessJson()));
            fileWriter.close();
            AngleWarp.LOGGER.info("Saved {} warp points", points.size());
        } catch (Exception e) {
            AngleWarp.LOGGER.info("Exception occurred during saving of warp points.", e);
        }
    }

    public JsonObject preprocessJson() {
        JsonObject object = new JsonObject();
        object.add("points", gson.toJsonTree(points));
        return object;
    }

    public void loadPoints() {
        try {
            File file = getWarpPointsSaveFile();
            if(!file.exists()) return;

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String json = reader.lines().collect(Collectors.joining());

            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

            Type listType = new TypeToken<List<WarpPoint>>() {}.getType();

            points.addAll(gson.fromJson(jsonObject.getAsJsonArray("points"), listType));
            AngleWarp.LOGGER.info("Loaded {} warp points", points.size());

            reader.close();
        } catch (Exception e) {
            AngleWarp.LOGGER.info("Exception occurred during loading of warp points.", e);
        }

    }

    protected abstract File getWarpPointsSaveFile();

    protected boolean shouldCreateSaveDirectory() {
        return true;
    }

    // To prevent saving an empty file
    protected boolean shouldSave(File file) {
        return !points.isEmpty() || file.exists();
    }

    public static class Global extends WarpPointManager {

        @Override
        protected File getWarpPointsSaveFile() {
            return new File(FabricLoader.getInstance().getGameDir().toString(), "warp_points.json");
        }
    }

    public static class Singleplayer extends WarpPointManager {
        final File file;

        public Singleplayer() {
            file = new File(MinecraftClient.getInstance().getServer().getSavePath(WorldSavePath.ROOT).getParent().toFile(), "warp_points.json");
        }

        @Override
        protected boolean shouldCreateSaveDirectory() {
            return false;
        }

        @Override
        protected File getWarpPointsSaveFile() {
            return file;
        }
    }

    public static class Multiplayer extends WarpPointManager {
        private static final HashFunction SHA256 = Hashing.sha256();

        private final String serverName;
        private final File file;

        public Multiplayer(ServerInfo serverInfo) {
            String hash = SHA256.hashString(serverInfo.address, Charsets.UTF_8).toString();
            String fileName = StringUtils.substring(hash, 0, 36) + ".json"; // Take 36 characters to prevent making a file with a long name

            this.serverName = serverInfo.name;
            this.file = new File(FabricLoader.getInstance().getConfigDir().resolve("anglewarp_points").toString(), fileName);
        }

        @Override
        public JsonObject preprocessJson() {
            JsonObject object = super.preprocessJson();
            object.addProperty("server_name", serverName);

            return object;
        }

        @Override
        protected File getWarpPointsSaveFile() {
            return file;
        }

    }
}
