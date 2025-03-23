package ru.nern.anglewrap.managers;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.nern.anglewrap.AngleWarp;
import ru.nern.anglewrap.model.WarpPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WarpPointManager {
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final List<WarpPoint> points = new ArrayList<>();

    public static WarpPoint getPointById(String id) {
        if(id == null) return null;

        for(WarpPoint point : WarpPointManager.points) {
            if(point.id.equals(id)) return point;
        }

        return null;
    }

    private static File getWarpPointsSaveFile() {
        return new File(FabricLoader.getInstance().getGameDir().toString(), "warp_points.json");
    }


    public static void savePoints() {
        try {
            FileWriter fileWriter = new FileWriter(getWarpPointsSaveFile());
            fileWriter.write(gson.toJson(points));
            fileWriter.close();
            AngleWarp.LOGGER.info("Saved {} warp points", points.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadPoints() {
        File file = getWarpPointsSaveFile();
        if(!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String json = reader.lines().collect(Collectors.joining());
            Type listType = new TypeToken<List<WarpPoint>>() {}.getType();
            List<WarpPoint> loadedPoints = gson.fromJson(json, listType);

            points.addAll(loadedPoints);
            AngleWarp.LOGGER.info("Loaded {} warp points", points.size());
        } catch (Exception e) {
            AngleWarp.LOGGER.info("Exception occurred during loading of warp points. {}", e.getMessage());
            e.printStackTrace();
        }
    }

}
