package ru.nern.anglewarp.cursor_snap;

import net.minecraft.client.network.ClientPlayerEntity;

public class NoCursorSnapper implements CursorSnapper {
    @Override
    public void snapToPoint(ClientPlayerEntity player, float yaw, float pitch) {}
}
