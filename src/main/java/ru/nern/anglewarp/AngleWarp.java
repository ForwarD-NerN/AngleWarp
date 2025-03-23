package ru.nern.anglewarp;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.nern.anglewarp.config.ModConfig;
import ru.nern.anglewarp.managers.CommandsManager;
import ru.nern.anglewarp.managers.KeybindsManager;
import ru.nern.anglewarp.managers.RenderManager;
import ru.nern.anglewarp.managers.WarpPointManager;
import ru.nern.anglewarp.model.WarpPoint;

public class AngleWarp implements ClientModInitializer {
	public static final String MOD_ID = "anglewarp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	//Warping
	public static WarpPoint lastWarped;
	public static int warpProgress = 0;
	public static boolean isWarpingKeyPressed = false;


	//Mouse Blocking
	public static boolean mouseBlocked = false;
	public static int mouseBlockedTicks = 0;

	// Lerping
	private float targetYaw, targetPitch;
	private float startYaw, startPitch;
	private int lerpingTicks = 0;
	private boolean isLerping = false;

	public static ModConfig config;

	@Override
	public void onInitializeClient() {
		CommandsManager.init();
		KeybindsManager.init();
		RenderManager.init();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			tickWarpingProgress(client.player);

			if(KeybindsManager.showWarpsKey.isPressed()) {
				tickLerp(client.player);
				tickSnapping(client.player);
				isWarpingKeyPressed = true;
			}else{
				mouseBlocked = false;
				isWarpingKeyPressed = false;
			}
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> WarpPointManager.savePoints());
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> WarpPointManager.loadPoints());
		// Only choose one of these!
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
	}



	public void tickLerp(ClientPlayerEntity player) {
		if (isLerping) {
			lerpingTicks++;

			int maxLerpingTicks = config.lerping.maxLerpingTicks;

			float progress = (float) lerpingTicks / maxLerpingTicks;

			float smoothYaw = MathHelper.lerp(progress, startYaw, targetYaw);
			float smoothPitch = MathHelper.lerp(progress, startPitch, targetPitch);

			player.prevYaw = player.getYaw();
			player.prevPitch = player.getPitch();

			player.setYaw(smoothYaw);
			player.setPitch(smoothPitch);

			if (lerpingTicks >= maxLerpingTicks) {
				isLerping = false;
			}
		}
	}

	public void lookAtPoint(ClientPlayerEntity player, WarpPoint point) {
		if(!config.lerping.enableLerping) {
			lookAtPointInstantly(player, point);
			return;
		}
		this.startYaw = MathHelper.wrapDegrees(player.getYaw());
		this.startPitch = MathHelper.wrapDegrees(player.getPitch());
		this.targetYaw = point.rotation.y;
		this.targetPitch = point.rotation.x;
		this.lerpingTicks = 0;
		isLerping = true;
	}

	public void lookAtPointInstantly(ClientPlayerEntity player, WarpPoint point) {
		player.setYaw(point.rotation.y);
		player.setPitch(point.rotation.x);
	}



	public void unsnap() {
		LOGGER.info("Unsnapping");
		lastWarped = null;
		warpProgress = 0;
	}


	public void tickWarpingProgress(ClientPlayerEntity player) {
		if(lastWarped == null) return;

		float playerYaw = MathHelper.wrapDegrees(player.getYaw());
		float playerPitch = MathHelper.wrapDegrees(player.getPitch());

		float distanceToLast = lastWarped.rotation.distanceSquared(playerPitch, playerYaw);

		if(distanceToLast > config.snapping.unsnapDistance) {
			unsnap();
			return;
		}

		if(lastWarped.warpTicks != 0 && !isLerping) {
			warpProgress++;

			if(warpProgress == lastWarped.warpTicks) {
				player.sendMessage(Text.literal("Warped to " + lastWarped.getDisplayName()), true);

				WarpPoint postActionPoint = WarpPointManager.getPointById(lastWarped.postActionPointId);

				if(postActionPoint != null) {
					lookAtPointInstantly(player, postActionPoint);
					unsnap();
					lastWarped = postActionPoint;
				}
			}
		}
	}

	public void tickSnapping(ClientPlayerEntity player) {
		if (mouseBlocked) {
			mouseBlockedTicks++;
			if (mouseBlockedTicks >= config.snapping.maxMouseBlockingTicks) {
				mouseBlocked = false;
				mouseBlockedTicks = 0;
			}
			return;
		}

		float playerYaw = MathHelper.wrapDegrees(player.getYaw());
		float playerPitch = MathHelper.wrapDegrees(player.getPitch());

		for (WarpPoint point : WarpPointManager.points) {
			if(!point.canSnap) continue;

			float distance = point.rotation.distanceSquared(playerPitch, playerYaw);

			if (distance < config.snapping.snapDistance && point != lastWarped) {
				lookAtPoint(player, point);

				mouseBlocked = true;
                LOGGER.info("Snapping to {}", point.getDisplayName());
				lastWarped = point;
			}

		}
	}


}

