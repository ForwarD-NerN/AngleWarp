package ru.nern.anglewrap.model;

import net.minecraft.util.math.Vec2f;

public class WarpPoint {
    public String id;
    public String name;
    public RotationVector rotation;
    public int color;
    public int warpTicks;
    public String postActionPointId;
    public boolean canSnap;
    public boolean hidden;

    public WarpPoint(String id, RotationVector rotation, int argb, int warpTicks) {
        this(id, null, rotation, argb, warpTicks, true, false);
    }

    public WarpPoint(String id, String name, RotationVector rotation, int argb, int warpTicks, boolean canSnap, boolean hidden) {
        this.id = id;
        this.name = name;
        this.rotation = rotation;
        this.color = argb;
        this.warpTicks = warpTicks;
        this.canSnap = canSnap;
        this.hidden = hidden;
    }

    public String getDisplayName() {
        return this.name == null ? this.id : this.name;
    }


}


