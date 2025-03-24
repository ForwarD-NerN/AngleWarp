package ru.nern.anglewarp.cursor_snap;

import net.minecraft.client.network.ClientPlayerEntity;

public interface CursorSnapper {
    void snapToPoint(ClientPlayerEntity player, float yaw, float pitch);
    default void tick(ClientPlayerEntity player) {};
    default void stopSnapping() {};
}
