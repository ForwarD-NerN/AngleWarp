package ru.nern.anglewarp.cursor_snap;

import net.minecraft.client.network.ClientPlayerEntity;
import ru.nern.anglewarp.AngleWarp;

public class InstantCursorSnapper implements CursorSnapper {

    @Override
    public void snapToPoint(ClientPlayerEntity player, float yaw, float pitch) {
        AngleWarp.enableMouseLock();
        player.setYaw(yaw);
        player.setPitch(pitch);
    }

    @Override
    public void stopSnapping() {
        AngleWarp.mouseLocked = false;
    }
}
