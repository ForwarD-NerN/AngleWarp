package ru.nern.anglewrap;

import com.google.common.collect.Maps;
import net.minecraft.util.Util;

import java.util.Map;
import java.util.Random;

public class WarpUtils {
    public static Map<String, Integer> colors = Util.make(Maps.newHashMap(), map -> {
        map.put("black", 0xFF000000);
        map.put("red", 0xFFFF0000);
        map.put("lime", 0xFF00FF00);
        map.put("blue", 0xFF0000FF);
        map.put("yellow", 0xFFFFFF00);
        map.put("cyan", 0xFF00FFFF);
        map.put("magenta", 0xFFFF00FF);
        map.put("silver", 0xFFC0C0C0);
        map.put("gray", 0xFF808080);
        map.put("maroon", 0xFF800000);
        map.put("olive", 0xFF808000);
        map.put("green", 0xFF008000);
        map.put("purple", 0xFF800080);
        map.put("teal", 0xFF008080);
        map.put("navy", 0xFF000080);
    });

    public static int getRandomColor() {
        Random random = new Random();
        return (int) colors.values().toArray()[random.nextInt(14)];
    }
}
