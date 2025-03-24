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
import ru.nern.anglewarp.cursor_snap.CursorSnapper;
import ru.nern.anglewarp.cursor_snap.InstantCursorSnapper;
import ru.nern.anglewarp.cursor_snap.LerpCursorSnapper;
import ru.nern.anglewarp.cursor_snap.NoCursorSnapper;
import ru.nern.anglewarp.managers.CommandsManager;
import ru.nern.anglewarp.managers.KeybindsManager;
import ru.nern.anglewarp.managers.RenderManager;
import ru.nern.anglewarp.managers.WarpPointManager;
import ru.nern.anglewarp.model.WarpPoint;

public class AngleWarp implements ClientModInitializer {
	public static final String MOD_ID = "anglewarp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	//Activation
	public static WarpPoint currentlySnapped;
	public static int activationProgress = 0;

	public static boolean isOverlayEnabled = false;

	//Mouse Blocking
	public static boolean mouseLocked = false;
	public static int mouseBlockedTicks = 0;

	public static ModConfig config;

	private static final InstantCursorSnapper instantSnapper = new InstantCursorSnapper();
	private static final LerpCursorSnapper lerpSnapper = new LerpCursorSnapper();
	private static final NoCursorSnapper noSnapper = new NoCursorSnapper();

	@Override
	public void onInitializeClient() {
		CommandsManager.init();
		KeybindsManager.init();
		RenderManager.init();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if(client.isPaused()) return;

			tickActivationProgress(client.player);

			if(KeybindsManager.showWarpsKey.isPressed()) {
				if(currentlySnapped != null) getSnapper().tick(client.player);
				tickAiming(client.player);
				isOverlayEnabled = true;
			}else if(isOverlayEnabled) {
				mouseLocked = false;
				isOverlayEnabled = false;
				getSnapper().stopSnapping();
			}
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> WarpPointManager.savePoints());
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> WarpPointManager.loadPoints());

		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
	}

	public static void enableMouseLock() {
		if(isOverlayEnabled) {
			mouseLocked = true;
		}
	}


	public void tickActivationProgress(ClientPlayerEntity player) {
		if(currentlySnapped == null) return;

		float playerYaw = MathHelper.wrapDegrees(player.getYaw());
		float playerPitch = MathHelper.wrapDegrees(player.getPitch());

		float distanceToSnapped = currentlySnapped.rotation.distanceSquared(playerPitch, playerYaw);

		if(distanceToSnapped > config.snapping.snapDistance) {
			currentlySnapped = null;
			activationProgress = 0;
			return;
		}

		if(distanceToSnapped <= config.activation.activationDistance) {
			activationProgress++;

			if(activationProgress == currentlySnapped.warpTicks) {
				player.sendMessage(Text.literal("Activated " + currentlySnapped.getDisplayName()), true);

				WarpPoint postActionPoint = WarpPointManager.getPointById(currentlySnapped.postActionPointId);

				if(postActionPoint != null) {
					snapToPointInstantly(player, postActionPoint);
					activationProgress = 0;
					currentlySnapped = postActionPoint;
				}
			}
		}else{
			activationProgress = 0;
		}
	}

	public void tickAiming(ClientPlayerEntity player) {
		if (mouseLocked) {
			mouseBlockedTicks++;
			if (mouseBlockedTicks >= config.snapping.maxMouseBlockingTicks) {
				mouseLocked = false;
				mouseBlockedTicks = 0;
			}
			return;
		}

		float playerYaw = MathHelper.wrapDegrees(player.getYaw());
		float playerPitch = MathHelper.wrapDegrees(player.getPitch());

		for (WarpPoint point : WarpPointManager.points) {
			if(point != currentlySnapped && point.canSnap) {
				float distance = point.rotation.distanceSquared(playerPitch, playerYaw);

				if(distance <= config.snapping.snapDistance) {
					snapToPoint(player, point);
					currentlySnapped = point;
					LOGGER.info("Snapping to {}", point.getDisplayName());
				}

			}
		}
	}


	public CursorSnapper getSnapper() {
		if(!config.snapping.enableSnapping) return noSnapper;
		return config.lerping.enableLerping ? lerpSnapper : instantSnapper;
	}

	public void snapToPoint(ClientPlayerEntity player, WarpPoint point) {
		getSnapper().snapToPoint(player, point.rotation.y, point.rotation.x);
	}

	public void snapToPointInstantly(ClientPlayerEntity player, WarpPoint point) {
		instantSnapper.snapToPoint(player, point.rotation.y, point.rotation.x);
	}


}

/*
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
	public static WarpPoint currentlyActive;
	public static WarpPoint currentlySnapped;
	public static int activationProgress = 0;
	public static boolean isWarpingKeyPressed = false;


	//Mouse Blocking
	public static boolean mouseBlocked = false;
	public static int mouseBlockedTicks = 0;

	// Lerping
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
			tickActivationProgress(client.player);

			if(KeybindsManager.showWarpsKey.isPressed()) {
				tickLerp(client.player);
				tickAiming(client.player);
				isWarpingKeyPressed = true;
			}else{
				mouseBlocked = false;
				isWarpingKeyPressed = false;
				isLerping = false;
				currentlySnapped = null;
			}
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> WarpPointManager.savePoints());
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> WarpPointManager.loadPoints());
		// Only choose one of these!
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
	}



	public void tickLerp(ClientPlayerEntity player) {
		if (currentlySnapped != null && isLerping) {
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
				isLerping = false;
			}
		}
	}

	public void lookAtPoint(ClientPlayerEntity player, WarpPoint point) {
		if(!config.snapping.enableSnapping) {
			return;
		}

		currentlySnapped = point;

		if(!config.lerping.enableLerping) {
			lookAtPointInstantly(player, point);
			return;
		}

		this.startYaw = MathHelper.wrapDegrees(player.getYaw());
		this.startPitch = MathHelper.wrapDegrees(player.getPitch());

		this.lerpingTicks = 0;
		isLerping = true;
	}

	public void lookAtPointInstantly(ClientPlayerEntity player, WarpPoint point) {
		player.setYaw(point.rotation.y);
		player.setPitch(point.rotation.x);
	}

	public void deactivate() {
		currentlyActive = null;
		activationProgress = 0;
	}


	public void tickActivationProgress(ClientPlayerEntity player) {
		if(currentlyActive == null) return;

		float playerYaw = MathHelper.wrapDegrees(player.getYaw());
		float playerPitch = MathHelper.wrapDegrees(player.getPitch());

		float distanceToActive = currentlyActive.rotation.distanceSquared(playerPitch, playerYaw);

		if(distanceToActive > config.activation.activationDistance) {
			deactivate();
			return;
		}

		if(currentlyActive.warpTicks != 0 && !isLerping) {
			activationProgress++;

			if(activationProgress == currentlyActive.warpTicks) {
				player.sendMessage(Text.literal("Activated " + currentlyActive.getDisplayName()), true);

				WarpPoint postActionPoint = WarpPointManager.getPointById(currentlyActive.postActionPointId);

				if(postActionPoint != null) {
					lookAtPointInstantly(player, postActionPoint);
					deactivate();
					currentlyActive = postActionPoint;
				}
			}
		}
	}

	public void tickAiming(ClientPlayerEntity player) {
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


		if(currentlySnapped != null) {
			float distanceToSnapped = currentlySnapped.rotation.distanceSquared(playerPitch, playerYaw);

			if(distanceToSnapped > config.activation.activationDistance) {
				currentlySnapped = null;
				return;
			}
		}


		for (WarpPoint point : WarpPointManager.points) {
			float distance = point.rotation.distanceSquared(playerPitch, playerYaw);

			if(point != currentlySnapped && distance < config.snapping.snapDistance && point.canSnap) {
				mouseBlocked = true;
				lookAtPoint(player, point);
				LOGGER.info("Snapping to {}", point.getDisplayName());
				continue;
			}
			if(point != currentlyActive && distance < config.activation.activationDistance) {
				currentlyActive = point;
			}
		}
	}


}


 */