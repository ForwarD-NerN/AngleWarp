package ru.nern.anglewarp;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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
import ru.nern.anglewarp.model.WarpSoundEntry;
import ru.nern.anglewarp.model.WarpSoundType;

import java.util.Optional;

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

	public static WarpPointManager warpPointManager;

	@Override
	public void onInitializeClient() {
		CommandsManager.init();
		KeybindsManager.init();
		RenderManager.init();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if(client.isPaused() || client.player == null) return;

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

		ClientPlayConnectionEvents.JOIN.register((clientPlayNetworkHandler, packetSender, client) -> {
			if(config.saving.useGlobalStorage) {
				warpPointManager = new WarpPointManager.Global();
			}else {
				warpPointManager = client.isIntegratedServerRunning() ?
						new WarpPointManager.Singleplayer() :
						new WarpPointManager.Multiplayer(client.getNetworkHandler().getServerInfo());
			}
			warpPointManager.loadPoints();
		});

		ClientPlayConnectionEvents.DISCONNECT.register((clientPlayNetworkHandler, minecraftClient) -> {
			warpPointManager.savePoints();
			warpPointManager = null;
		});

		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
	}

	public static void enableMouseLock() {
		if(isOverlayEnabled) mouseLocked = true;
	}

	private static void playPointSound(ClientPlayerEntity player, WarpPoint point, WarpSoundType type) {
		WarpSoundEntry soundEntry = point.getSoundEntry(type);
		if(soundEntry == null) return;

		Optional<SoundEvent> sound = Registries.SOUND_EVENT.getOptionalValue(Identifier.of(soundEntry.soundId()));
		sound.ifPresent(soundEvent -> player.playSound(soundEvent, soundEntry.volume(), soundEntry.pitch()));
	}


	public void tickActivationProgress(ClientPlayerEntity player) {
		if(currentlySnapped == null) return;

		float playerYaw = MathHelper.wrapDegrees(player.getYaw());
		float playerPitch = MathHelper.wrapDegrees(player.getPitch());

		float distanceToSnapped = currentlySnapped.rotation.distanceSquared(playerPitch, playerYaw);

		if(distanceToSnapped > config.snapping.snapDistance) {
			playPointSound(player, currentlySnapped, WarpSoundType.UNSNAP);
			currentlySnapped = null;
			activationProgress = 0;
			return;
		}

		if(distanceToSnapped <= config.activation.activationDistance) {
			activationProgress++;

			if(activationProgress == 1) {
				playPointSound(player, currentlySnapped, WarpSoundType.ACTIVATION_START);
			}

			if(activationProgress == currentlySnapped.warpTicks) {
				player.sendMessage(Text.literal("Activated " + currentlySnapped.getDisplayName()), true);
				playPointSound(player, currentlySnapped, WarpSoundType.ACTIVATION_FINISH);

				WarpPoint postActionPoint = warpPointManager.getPointById(currentlySnapped.postActionPointId);

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

		for (WarpPoint point : warpPointManager.points) {
			if(point != currentlySnapped) {
				float distance = point.rotation.distanceSquared(playerPitch, playerYaw);

				if(distance <= config.snapping.snapDistance) {
					if(point.canSnap) snapToPoint(player, point);
					currentlySnapped = point;
					LOGGER.debug("Snapping to {}", point.getDisplayName());
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
		playPointSound(player, point, WarpSoundType.SNAP);
	}

	public void snapToPointInstantly(ClientPlayerEntity player, WarpPoint point) {
		instantSnapper.snapToPoint(player, point.rotation.y, point.rotation.x);
		playPointSound(player, point, WarpSoundType.SNAP);
	}


}