package ru.nern.anglewrap.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "anglewrap")
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    public SnappingOptions snapping = new SnappingOptions();

    @ConfigEntry.Gui.CollapsibleObject
    public LerpingOptions lerping = new LerpingOptions();

    @ConfigEntry.Gui.CollapsibleObject
    public Rendering rendering = new Rendering();

    public static class SnappingOptions {
        public float snapDistance = 12f;
        public float unsnapDistance = 15;
        public int maxMouseBlockingTicks = 10;
    }

    public static class Rendering {
        public float warpPointDistance = 600f;
        public boolean renderProgressBar = true;
        public boolean useMarkerColorForProgressBar = false;
    }

    public static class LerpingOptions {
        public boolean enableLerping = true;
        public int maxLerpingTicks = 8;
    }

    @Override
    public void validatePostLoad() {
        snapping.snapDistance = Math.max(snapping.snapDistance, 0);
        snapping.unsnapDistance = Math.max(snapping.unsnapDistance, 0);
        snapping.maxMouseBlockingTicks = Math.max(snapping.maxMouseBlockingTicks, 0);

        rendering.warpPointDistance = Math.max(rendering.warpPointDistance, 0);

        lerping.maxLerpingTicks = Math.max(lerping.maxLerpingTicks, 0);
    }
}