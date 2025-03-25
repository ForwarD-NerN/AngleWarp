package ru.nern.anglewarp.managers;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.xpple.clientarguments.arguments.CCoordinates;
import dev.xpple.clientarguments.arguments.CResourceLocationArgument;
import dev.xpple.clientarguments.arguments.CRotationArgument;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import ru.nern.anglewarp.AngleWarp;
import ru.nern.anglewarp.WarpUtils;
import ru.nern.anglewarp.model.*;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.IdentityHashMap;

public class CommandsManager {
    private static final SimpleCommandExceptionType POINT_ALREADY_EXISTS = new SimpleCommandExceptionType(Text.literal("Warp point with this name already exists!"));
    private static final SimpleCommandExceptionType POINT_DOESNT_EXIST = new SimpleCommandExceptionType(Text.literal("Warp point with this name doesn't exist"));
    private static final SimpleCommandExceptionType CANT_SET_COLOR = new SimpleCommandExceptionType(Text.literal("Unable to set this color"));
    private static final SimpleCommandExceptionType SHAPE_DOESNT_EXIST = new SimpleCommandExceptionType(Text.literal("This shape doesn't exist"));
    private static final SimpleCommandExceptionType SOUND_TYPE_DOESNT_EXIST = new SimpleCommandExceptionType(Text.literal("This warp sound type doesn't exist"));

    public static final SuggestionProvider<FabricClientCommandSource> POINTS =
            (context, builder) -> CommandSource.suggestMatching(AngleWarp.warpPointManager.points.stream().map(warpPoint -> warpPoint.id), builder);

    public static final SuggestionProvider<FabricClientCommandSource> SHAPES =
            (context, builder) -> {
                for(WarpPointShape shape : WarpPointShape.values()) {
                    builder.suggest(shape.name().toLowerCase());
                }
                return builder.buildFuture();
            };

    public static final SuggestionProvider<FabricClientCommandSource> AVAILABLE_SOUNDS_CLIENT =
            (context, builder) -> CommandSource.suggestIdentifiers(context.getSource().getSoundIds(), builder);

    public static final SuggestionProvider<FabricClientCommandSource> SOUND_TYPES =
            (context, builder) -> {
                for(WarpSoundType type : WarpSoundType.values()) {
                    builder.suggest(type.name().toLowerCase());
                }
                return builder.buildFuture();
            };


    public static final SuggestionProvider<FabricClientCommandSource> ANGLES =
            (context, builder) -> {
                Vec2f rotation = context.getSource().getPlayer().getRotationClient();
                DecimalFormat formatter = new DecimalFormat("00.00");
                return CommandSource.suggestMatching(new String[]{formatter.format(MathHelper.wrapDegrees(rotation.y)) + " " + formatter.format(MathHelper.wrapDegrees(rotation.x))}, builder);
            };

    public static final SuggestionProvider<FabricClientCommandSource> COLOR_NAMES =
            (context, builder) -> CommandSource.suggestMatching(WarpUtils.colors.keySet(), builder);


    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("anglewarp")
                        .then(ClientCommandManager.literal("add_point")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string())
                                        .then(ClientCommandManager.argument("angles", CRotationArgument.rotation()).suggests(ANGLES)
                                                .executes(context -> addPoint(context.getSource(), StringArgumentType.getString(context, "id"), CRotationArgument.getRotation(context, "angles"), 0))
                                                .then(ClientCommandManager.argument("warp_ticks", IntegerArgumentType.integer()).executes(context -> addPoint(context.getSource(), StringArgumentType.getString(context, "id"), CRotationArgument.getRotation(context, "angles"), IntegerArgumentType.getInteger(context, "warp_ticks")))))
                                ))
                        .then(ClientCommandManager.literal("remove_point")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string()).suggests(POINTS).executes(context -> removePoint(context.getSource(), StringArgumentType.getString(context, "id")))))
                        .then(ClientCommandManager.literal("configure")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string()).suggests(POINTS)
                                        .then(ClientCommandManager.literal("display_name").then(ClientCommandManager.argument("display_name", StringArgumentType.string()).executes(context -> setPointName(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "display_name")))))
                                        .then(ClientCommandManager.literal("shape").then(ClientCommandManager.argument("shape_name", StringArgumentType.string()).suggests(SHAPES).executes(context -> setPointShape(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "shape_name")))))
                                        .then(ClientCommandManager.literal("warp_delay").then(ClientCommandManager.argument("warp_delay", IntegerArgumentType.integer()).executes(context -> setPointWarpDelay(context.getSource(), StringArgumentType.getString(context, "id"), IntegerArgumentType.getInteger(context, "warp_delay")))))
                                        .then(ClientCommandManager.literal("snapping").then(ClientCommandManager.argument("snap", BoolArgumentType.bool()).executes(context -> setCanSnap(context.getSource(), StringArgumentType.getString(context, "id"), BoolArgumentType.getBool(context, "snap")))))
                                        .then(ClientCommandManager.literal("hide").then(ClientCommandManager.argument("hide", BoolArgumentType.bool()).executes(context -> setHidden(context.getSource(), StringArgumentType.getString(context, "id"), BoolArgumentType.getBool(context, "hide")))))
                                        .then(ClientCommandManager.literal("2fa_point").then(ClientCommandManager.argument("2fa", StringArgumentType.string()).suggests(POINTS).executes(context -> setPostActionPoint(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "2fa")))))
                                        .then(ClientCommandManager.literal("angles").then(ClientCommandManager.argument("angles", CRotationArgument.rotation()).suggests(ANGLES).executes(context -> setPointAngles(context.getSource(), StringArgumentType.getString(context, "id"), CRotationArgument.getRotation(context, "angles")))))
                                        .then(ClientCommandManager.literal("sounds").then(ClientCommandManager.argument("sound_type", StringArgumentType.string()).suggests(SOUND_TYPES)
                                                .then(ClientCommandManager.argument("sound", CResourceLocationArgument.id()).suggests(AVAILABLE_SOUNDS_CLIENT)
                                                        .executes(context -> setPointSound(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "sound_type"), CResourceLocationArgument.getId(context, "sound"), 50, 0))
                                                        .then(ClientCommandManager.argument("volume", FloatArgumentType.floatArg())
                                                                .executes(context -> setPointSound(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "sound_type"), CResourceLocationArgument.getId(context, "sound"), FloatArgumentType.getFloat(context, "volume"), 0))
                                                                .then(ClientCommandManager.argument("pitch",  FloatArgumentType.floatArg())
                                                                        .executes(context -> setPointSound(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "sound_type"), CResourceLocationArgument.getId(context, "sound"), FloatArgumentType.getFloat(context, "volume"), FloatArgumentType.getFloat(context, "pitch"))))
                                                        ))))
                                        .then(ClientCommandManager.literal("color")
                                                .then(ClientCommandManager.literal("hex").then(ClientCommandManager.argument("hex", StringArgumentType.string()).executes(context -> setPointColor(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "hex")))))
                                                .then(ClientCommandManager.argument("color_name", StringArgumentType.string()).suggests(COLOR_NAMES).executes(context -> setPointColor(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "color_name"))))
                                        )
                                )
                        )
                        .then(ClientCommandManager.literal("look")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string()).suggests(POINTS).executes(context -> lookAtPoint(context.getSource(), StringArgumentType.getString(context, "id")))))
                        .then(ClientCommandManager.literal("list_points").executes(context -> listPoints(context.getSource())))
                ));
    }

    private static int listPoints(FabricClientCommandSource source) {
        StringBuilder builder = new StringBuilder();

        if(AngleWarp.warpPointManager.points.isEmpty()) {
            source.sendFeedback(Text.literal("There are no warp points available."));
            return 1;
        }

        builder.append("Available points("); builder.append(AngleWarp.warpPointManager.points.size()); builder.append("): ");
        for(WarpPoint point : AngleWarp.warpPointManager.points) {
            builder.append(String.format("\n* %s (%s). Rotation: (%.2f, %.2f), ticks: %d%s%s%s", point.getDisplayName(), point.id, point.rotation.x, point.rotation.y, point.warpTicks, point.hidden ? ", hidden" : "", !point.canSnap ? ", snapping disabled" : "", point.postActionPointId != null ? (", 2FA: " + point.postActionPointId) : ""));
        }
        source.sendFeedback(Text.literal(builder.toString()));

        return 1;
    }

    private static int lookAtPoint(FabricClientCommandSource source, String id) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);


        source.getPlayer().setYaw(point.rotation.y);
        source.getPlayer().setPitch(point.rotation.x);
        AngleWarp.currentlySnapped = point;

        source.sendFeedback(Text.literal("Looking at '" + id + "'"));

        return 1;
    }

    private static int setPointSound(FabricClientCommandSource source, String id, String type, Identifier sound, float volume, float pitch) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        point.initializeSoundMap();

        try {
            WarpSoundType soundType = WarpSoundType.valueOf(type.toUpperCase());
            point.setSoundEntry(soundType, new WarpSoundEntry(sound.toString(), volume, pitch));
            source.sendFeedback(Text.literal(id + " " + type + " sound was set to " + sound));
        }catch (IllegalArgumentException e) {
            throw SOUND_TYPE_DOESNT_EXIST.create();
        }

        return 1;
    }


    private static int setPointName(FabricClientCommandSource source, String id, String name) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        point.name = name;
        source.sendFeedback(Text.literal(id + " name was set to " + name));

        return 1;
    }

    private static int setPointShape(FabricClientCommandSource source, String id, String shapeName) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);

        try {
            point.shape = WarpPointShape.valueOf(shapeName.toUpperCase());
            source.sendFeedback(Text.literal(id + " shape was set to " + shapeName));
        }catch (IllegalArgumentException e) {
            throw SHAPE_DOESNT_EXIST.create();
        }

        return 1;
    }

    private static int setPostActionPoint(FabricClientCommandSource source, String id, String pointId) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        point.postActionPointId = pointId;
        source.sendFeedback(Text.literal(id + " 2FA point was set to " + pointId));

        return 1;
    }

    private static int setPointWarpDelay(FabricClientCommandSource source, String id, int warpDelay) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        point.warpTicks = warpDelay;
        source.sendFeedback(Text.literal(id + " warp delay was set to " + warpDelay));

        return 1;
    }


    private static int setPointColor(FabricClientCommandSource source, String id, String colorStr) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);

        int color = getColorByName(colorStr);

        try {
            if(color == -1) color = Color.decode(colorStr).getRGB();
        }catch (Exception e) {
            throw CANT_SET_COLOR.create();
        }
        point.color = color;

        source.sendFeedback(Text.literal(id + " color was set to " + colorStr));

        return 1;
    }

    private static int setCanSnap(FabricClientCommandSource source, String id, boolean canSnap) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        point.canSnap = canSnap;
        source.sendFeedback(Text.literal(id + " snapping mode was set to " + canSnap));

        return 1;
    }

    private static int setPointAngles(FabricClientCommandSource source, String id, CCoordinates location) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        point.rotation = new RotationVector(location.getRotation(source));
        source.sendFeedback(Text.literal(id + " angles were set to (" + point.rotation.y + ", " + point.rotation.x + ") "));

        return 1;
    }


    private static int setHidden(FabricClientCommandSource source, String id, boolean hidden) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);

        point.hidden = hidden;
        if(point.hidden) {
            source.sendFeedback(Text.literal("Point " +id+ "  is now hidden"));
        }else{
            source.sendFeedback(Text.literal("Point " +id+ " is now shown"));
        }

        return 1;
    }


    private static int removePoint(FabricClientCommandSource source, String id) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);

        if(AngleWarp.warpPointManager.points.remove(point)) {
            if(AngleWarp.currentlySnapped == point) {
                AngleWarp.activationProgress = 0;
                AngleWarp.currentlySnapped = null;
            }

            source.sendFeedback(Text.literal("The warp point '" + id + "' was successfully removed"));
            return 1;
        }

        return 0;
    }

    private static int addPoint(FabricClientCommandSource source, String id, CCoordinates location, int warpDelay) throws CommandSyntaxException {
        final WarpPoint point = new WarpPoint(id, new RotationVector(location.getRotation(source)), WarpUtils.getRandomColor(), warpDelay);

        for(WarpPoint existing : AngleWarp.warpPointManager.points) {
            if(existing.id.equals(point.id)) throw POINT_ALREADY_EXISTS.create();
        }

        AngleWarp.warpPointManager.points.add(point);
        source.sendFeedback(Text.literal("The warp point '" + id + "' was successfully created"));
        return 1;
    }

    private static WarpPoint getPointByIdOrThrow(String id) throws CommandSyntaxException {
        for(WarpPoint point : AngleWarp.warpPointManager.points) {
            if(point.id.equals(id)) return point;
        }
        throw POINT_DOESNT_EXIST.create();
    }

    private static int getColorByName(String name) {
        return WarpUtils.colors.getOrDefault(name, -1);
    }
}
