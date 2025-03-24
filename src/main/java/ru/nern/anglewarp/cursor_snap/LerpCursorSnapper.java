package ru.nern.anglewarp.cursor_snap;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import ru.nern.anglewarp.AngleWarp;

import static ru.nern.anglewarp.AngleWarp.config;
import static ru.nern.anglewarp.AngleWarp.currentlySnapped;

public class LerpCursorSnapper implements CursorSnapper {
    private float startYaw, startPitch;
    private int lerpingTicks = 0;
    private boolean isLerping = false;


    @Override
    public void snapToPoint(ClientPlayerEntity player, float yaw, float pitch) {
        AngleWarp.enableMouseLock();
        this.startYaw = MathHelper.wrapDegrees(player.getYaw());
        this.startPitch = MathHelper.wrapDegrees(player.getPitch());
        this.lerpingTicks = 0;
        this.isLerping = true;
    }

    @Override
    public void tick(ClientPlayerEntity player) {
        if (isLerping) {
            lerpingTicks++;

            int maxLerpingTicks = config.lerping.maxLerpingTicks;

            float progress = (float) lerpingTicks / maxLerpingTicks;

            float smoothYaw = MathHelper.lerp(progress, startYaw, currentlySnapped.rotation.y);
            float smoothPitch = MathHelper.lerp(progress, startPitch, currentlySnapped.rotation.x);

            player.prevYaw = player.getYaw();
            player.prevPitch = player.getPitch();

            player.setYaw(smoothYaw);
            player.setPitch(smoothPitch);

            if (lerpingTicks >= maxLerpingTicks) {
                stopSnapping();
            }
        }
    }

    @Override
    public void stopSnapping() {
        this.isLerping = false;
        AngleWarp.mouseLocked = false;
    }
}
