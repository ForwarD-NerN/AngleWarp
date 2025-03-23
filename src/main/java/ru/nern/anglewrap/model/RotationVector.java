package ru.nern.anglewrap.model;

import net.minecraft.util.math.Vec2f;

public class RotationVector {
    public final float x;
    public final float y;

    public RotationVector(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public RotationVector(Vec2f vec2f) {
        this(vec2f.x, vec2f.y);
    }

    public float distanceSquared(float x, float y) {
        float f = x - this.x;
        float g = y - this.y;
        return f * f + g * g;
    }
}
