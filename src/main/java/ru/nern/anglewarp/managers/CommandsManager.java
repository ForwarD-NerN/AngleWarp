package ru.nern.anglewarp.managers;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.xpple.clientarguments.arguments.CCoordinates;
import dev.xpple.clientarguments.arguments.CRotationArgument;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import ru.nern.anglewarp.AngleWarp;
import ru.nern.anglewarp.WarpUtils;
import ru.nern.anglewarp.model.RotationVector;
import ru.nern.anglewarp.model.WarpPoint;
import ru.nern.anglewarp.model.WarpPointShape;

import java.awt.*;
import java.text.DecimalFormat;

public class CommandsManager {
    private static final SimpleCommandExceptionType POINT_ALREADY_EXISTS = new SimpleCommandExceptionType(Text.literal("Warp point with this name already exists!"));
    private static final SimpleCommandExceptionType POINT_DOESNT_EXIST = new SimpleCommandExceptionType(Text.literal("Warp point with this name doesn't exist"));
    private static final SimpleCommandExceptionType CANT_SET_COLOR = new SimpleCommandExceptionType(Text.literal("Unable to set this color"));

    public static final SuggestionProvider<FabricClientCommandSource> POINTS_SUGGESTION_PROVIDER =
            (context, builder) -> CommandSource.suggestMatching(AngleWarp.warpPointManager.points.stream().map(warpPoint -> warpPoint.id), builder);

    public static final SuggestionProvider<FabricClientCommandSource> POINT_SHAPE_SUGGESTION_PROVIDER =
            (context, builder) -> {
                for(WarpPointShape shape : WarpPointShape.values()) {
                    builder.suggest(shape.name().toLowerCase());
                }
                return builder.buildFuture();
            };


    public static final SuggestionProvider<FabricClientCommandSource> ANGLE_SUGGESTION_PROVIDER =
            (context, builder) -> {
                Vec2f rotation = context.getSource().getPlayer().getRotationClient();
                DecimalFormat formatter = new DecimalFormat("00.00");
                return CommandSource.suggestMatching(new String[]{formatter.format(MathHelper.wrapDegrees(rotation.y)) + " " + formatter.format(MathHelper.wrapDegrees(rotation.x))}, builder);
            };

    public static final SuggestionProvider<FabricClientCommandSource> COLORS_SUGGESTION_PROVIDER =
            (context, builder) -> CommandSource.suggestMatching(WarpUtils.colors.keySet(), builder);


    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("anglewarp")
                        .then(ClientCommandManager.literal("add_point")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string())
                                        .then(ClientCommandManager.argument("angles", CRotationArgument.rotation()).suggests(ANGLE_SUGGESTION_PROVIDER)
                                                .executes(context -> addPoint(context.getSource(), StringArgumentType.getString(context, "id"), CRotationArgument.getRotation(context, "angles"), 0))
                                                .then(ClientCommandManager.argument("warp_ticks", IntegerArgumentType.integer()).executes(context -> addPoint(context.getSource(), StringArgumentType.getString(context, "id"), CRotationArgument.getRotation(context, "angles"), IntegerArgumentType.getInteger(context, "warp_ticks")))))
                                ))
                        .then(ClientCommandManager.literal("remove_point")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string()).suggests(POINTS_SUGGESTION_PROVIDER).executes(context -> removePoint(context.getSource(), StringArgumentType.getString(context, "id")))))
                        .then(ClientCommandManager.literal("configure")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string()).suggests(POINTS_SUGGESTION_PROVIDER)
                                        .then(ClientCommandManager.literal("display_name").then(ClientCommandManager.argument("display_name", StringArgumentType.string()).executes(context -> setPointName(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "display_name")))))
                                        .then(ClientCommandManager.literal("shape").then(ClientCommandManager.argument("shape_name", StringArgumentType.string()).suggests(POINT_SHAPE_SUGGESTION_PROVIDER).executes(context -> setPointShape(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "shape_name")))))
                                        .then(ClientCommandManager.literal("warp_delay").then(ClientCommandManager.argument("warp_delay", IntegerArgumentType.integer()).executes(context -> setPointWarpDelay(context.getSource(), StringArgumentType.getString(context, "id"), IntegerArgumentType.getInteger(context, "warp_delay")))))
                                        .then(ClientCommandManager.literal("snapping").then(ClientCommandManager.argument("snap", BoolArgumentType.bool()).executes(context -> setCanSnap(context.getSource(), StringArgumentType.getString(context, "id"), BoolArgumentType.getBool(context, "snap")))))
                                        .then(ClientCommandManager.literal("hide").then(ClientCommandManager.argument("hide", BoolArgumentType.bool()).executes(context -> setHidden(context.getSource(), StringArgumentType.getString(context, "id"), BoolArgumentType.getBool(context, "hide")))))
                                        .then(ClientCommandManager.literal("2fa_point").then(ClientCommandManager.argument("2fa", StringArgumentType.string()).suggests(POINTS_SUGGESTION_PROVIDER).executes(context -> setPostActionPoint(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "2fa")))))
                                        .then(ClientCommandManager.literal("angles").then(ClientCommandManager.argument("angles", CRotationArgument.rotation()).suggests(ANGLE_SUGGESTION_PROVIDER).executes(context -> setPointAngles(context.getSource(), StringArgumentType.getString(context, "id"), CRotationArgument.getRotation(context, "angles")))))
                                        .then(ClientCommandManager.literal("color")
                                                .then(ClientCommandManager.literal("hex").then(ClientCommandManager.argument("hex", StringArgumentType.string()).executes(context -> setPointColor(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "hex")))))
                                                .then(ClientCommandManager.argument("color_name", StringArgumentType.string()).suggests(COLORS_SUGGESTION_PROVIDER).executes(context -> setPointColor(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "color_name"))))
                                        )
                                )
                        )
                        .then(ClientCommandManager.literal("look")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string()).suggests(POINTS_SUGGESTION_PROVIDER).executes(context -> lookAtPoint(context.getSource(), StringArgumentType.getString(context, "id")))))
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


    private static int setPointName(FabricClientCommandSource source, String id, String name) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        point.name = name;
        source.sendFeedback(Text.literal(id + " name was set to " + name));

        return 1;
    }

    private static int setPointShape(FabricClientCommandSource source, String id, String shapeName) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        WarpPointShape shape = WarpPointShape.valueOf(shapeName.toUpperCase());

        if(shape != null) {
            point.shape = shape;
            source.sendFeedback(Text.literal(id + " shape was set to " + shapeName));
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
