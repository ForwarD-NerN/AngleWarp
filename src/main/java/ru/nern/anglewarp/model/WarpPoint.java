package ru.nern.anglewarp.model;

import java.util.IdentityHashMap;
import java.util.Map;

public class WarpPoint {
    public String id;
    public String name;
    public RotationVector rotation;
    public int color;
    public int warpTicks;
    public String postActionPointId;
    public boolean canSnap;
    public boolean hidden;
    public WarpPointShape shape;
    private Map<WarpSoundType, WarpSoundEntry> sounds;

    public WarpPoint(String id, RotationVector rotation, int argb, int warpTicks) {
        this(id, null, rotation, argb, warpTicks, true, false, WarpPointShape.DIAMOND);
    }

    public WarpPoint(String id, String name, RotationVector rotation, int argb, int warpTicks, boolean canSnap, boolean hidden, WarpPointShape shape) {
        this.id = id;
        this.name = name;
        this.rotation = rotation;
        this.color = argb;
        this.warpTicks = warpTicks;
        this.canSnap = canSnap;
        this.hidden = hidden;
        this.shape = shape;
        this.sounds = new IdentityHashMap<>();
    }

    public String getDisplayName() {
        return this.name == null ? this.id : this.name;
    }


    public void setSoundEntry(WarpSoundType type, WarpSoundEntry entry) {
        if(sounds != null) sounds.put(type, entry);
    }

    public WarpSoundEntry getSoundEntry(WarpSoundType type) {
        return sounds == null ? null : sounds.get(type);
    }

    public void initializeSoundMap() {
        if(sounds == null) sounds = new IdentityHashMap<>();
    }

}


