package ru.nern.anglewrap.managers;

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
import ru.nern.anglewrap.AngleWarp;
import ru.nern.anglewrap.WarpUtils;
import ru.nern.anglewrap.model.RotationVector;
import ru.nern.anglewrap.model.WarpPoint;

import java.text.DecimalFormat;

public class CommandsManager {
    private static final SimpleCommandExceptionType POINT_ALREADY_EXISTS = new SimpleCommandExceptionType(Text.literal("Warp point with this name already exists!"));
    private static final SimpleCommandExceptionType POINT_DOESNT_EXIST = new SimpleCommandExceptionType(Text.literal("Warp point with this name doesn't exist"));
    private static final SimpleCommandExceptionType COLOR_NOT_FOUND = new SimpleCommandExceptionType(Text.literal("Can't find color with this name"));

    public static final SuggestionProvider<FabricClientCommandSource> POINTS_SUGGESTION_PROVIDER =
            (context, builder) -> CommandSource.suggestMatching(WarpPointManager.points.stream().map(warpPoint -> warpPoint.id), builder);

    public static final SuggestionProvider<FabricClientCommandSource> ROTATION_SUGGESTION_PROVIDER =
            (context, builder) -> {
                Vec2f rotation = context.getSource().getPlayer().getRotationClient();
                DecimalFormat formatter = new DecimalFormat("##.00");
                return CommandSource.suggestMatching(new String[]{formatter.format(MathHelper.wrapDegrees(rotation.y)) + " " + formatter.format(MathHelper.wrapDegrees(rotation.x))}, builder);
            };

    public static final SuggestionProvider<FabricClientCommandSource> COLORS_SUGGESTION_PROVIDER =
            (context, builder) -> CommandSource.suggestMatching(WarpUtils.colors.keySet(), builder);


    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("anglewarp")
                        .then(ClientCommandManager.literal("add_point")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string())
                                        .then(ClientCommandManager.argument("rotation", CRotationArgument.rotation()).suggests(ROTATION_SUGGESTION_PROVIDER)
                                                .executes(context -> addPoint(context.getSource(), StringArgumentType.getString(context, "id"), CRotationArgument.getRotation(context, "rotation"), 0))
                                                .then(ClientCommandManager.argument("warp_ticks", IntegerArgumentType.integer()).executes(context -> addPoint(context.getSource(), StringArgumentType.getString(context, "id"), CRotationArgument.getRotation(context, "rotation"), IntegerArgumentType.getInteger(context, "warp_ticks")))))
                                ))
                        .then(ClientCommandManager.literal("remove_point")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string()).suggests(POINTS_SUGGESTION_PROVIDER).executes(context -> removePoint(context.getSource(), StringArgumentType.getString(context, "id")))))
                        .then(ClientCommandManager.literal("configure")
                                .then(ClientCommandManager.argument("id", StringArgumentType.string()).suggests(POINTS_SUGGESTION_PROVIDER)
                                        .then(ClientCommandManager.literal("display_name").then(ClientCommandManager.argument("display_name", StringArgumentType.string()).executes(context -> setPointName(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "display_name")))))
                                        .then(ClientCommandManager.literal("warp_delay").then(ClientCommandManager.argument("warp_delay", IntegerArgumentType.integer()).executes(context -> setPointWarpDelay(context.getSource(), StringArgumentType.getString(context, "id"), IntegerArgumentType.getInteger(context, "warp_delay")))))
                                        .then(ClientCommandManager.literal("snapping").then(ClientCommandManager.argument("snap", BoolArgumentType.bool()).executes(context -> setCanSnap(context.getSource(), StringArgumentType.getString(context, "id"), BoolArgumentType.getBool(context, "snap")))))
                                        .then(ClientCommandManager.literal("hide").then(ClientCommandManager.argument("hide", BoolArgumentType.bool()).executes(context -> setHidden(context.getSource(), StringArgumentType.getString(context, "id"), BoolArgumentType.getBool(context, "hide")))))
                                        .then(ClientCommandManager.literal("2fa").then(ClientCommandManager.argument("2fa", StringArgumentType.string()).suggests(POINTS_SUGGESTION_PROVIDER).executes(context -> setPostActionPoint(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "2fa")))))
                                        .then(ClientCommandManager.literal("color")
                                                .then(ClientCommandManager.literal("value")
                                                        .then(ClientCommandManager.argument("color", IntegerArgumentType.integer()).executes(context -> setPointColorByInt(context.getSource(), StringArgumentType.getString(context, "id"), IntegerArgumentType.getInteger(context, "color")))))
                                                .then(ClientCommandManager.literal("name")
                                                        .then(ClientCommandManager.argument("color_name", StringArgumentType.string()).suggests(COLORS_SUGGESTION_PROVIDER).executes(context -> setPointColorByName(context.getSource(), StringArgumentType.getString(context, "id"), StringArgumentType.getString(context, "color_name")))))
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

        if(WarpPointManager.points.isEmpty()) {
            source.sendFeedback(Text.literal("There are no warp points available."));
            return 1;
        }

        builder.append("Available points("); builder.append(WarpPointManager.points.size()); builder.append("): ");
        for(WarpPoint point : WarpPointManager.points) {
            builder.append(String.format("\n* %s (%s). Rotation: (%.2f, %.2f), ticks: %d%s%s%s", point.getDisplayName(), point.id, point.rotation.x, point.rotation.y, point.warpTicks, point.hidden ? ", hidden" : "", !point.canSnap ? ", snapping disabled" : "", point.postActionPointId != null ? (", 2fa: " + point.postActionPointId) : ""));
        }
        source.sendFeedback(Text.literal(builder.toString()));

        return 1;
    }

    private static int lookAtPoint(FabricClientCommandSource source, String id) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);

        source.getPlayer().setYaw(point.rotation.y);
        source.getPlayer().setPitch(point.rotation.x);
        AngleWarp.lastWarped = point;

        source.sendFeedback(Text.literal("Looking at '" + id + "'"));

        return 1;
    }


    private static int setPointName(FabricClientCommandSource source, String id, String name) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        point.name = name;
        source.sendFeedback(Text.literal("Point's name was set to " + name));

        return 1;
    }

    private static int setPostActionPoint(FabricClientCommandSource source, String id, String pointId) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        point.postActionPointId = pointId;
        source.sendFeedback(Text.literal("Point's 2fa was set to " + pointId));

        return 1;
    }

    private static int setPointWarpDelay(FabricClientCommandSource source, String id, int warpDelay) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        point.warpTicks = warpDelay;
        source.sendFeedback(Text.literal("Point's warp delay was set to " + warpDelay));

        return 1;
    }

    private static int setPointColorByName(FabricClientCommandSource source, String id, String colorName) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        if(WarpUtils.colors.containsKey(colorName)) {
            point.color = WarpUtils.colors.get(colorName);
            source.sendFeedback(Text.literal("Point's color was set to " + colorName));
            return 1;
        }
        throw COLOR_NOT_FOUND.create();
    }

    private static int setCanSnap(FabricClientCommandSource source, String id, boolean canSnap) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        point.canSnap = canSnap;
        source.sendFeedback(Text.literal("Point's snapping mode was set to " + canSnap));

        return 1;
    }

    private static int setHidden(FabricClientCommandSource source, String id, boolean hidden) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);

        point.hidden = hidden;
        if(point.hidden) {
            source.sendFeedback(Text.literal("Point is now hidden"));
        }else{
            source.sendFeedback(Text.literal("Point is now shown"));
        }


        return 1;
    }

    private static int setPointColorByInt(FabricClientCommandSource source, String id, int color) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);
        point.color = color;
        source.sendFeedback(Text.literal("Point's color was set to " + Integer.toHexString(color)));

        return 1;
    }


    private static int removePoint(FabricClientCommandSource source, String id) throws CommandSyntaxException {
        WarpPoint point = getPointByIdOrThrow(id);

        WarpPointManager.points.remove(point);
        source.sendFeedback(Text.literal("The warp point '" + id + "' was successfully removed"));

        return 1;
    }

    private static int addPoint(FabricClientCommandSource source, String name, CCoordinates location, int warpDelay) throws CommandSyntaxException {
        final WarpPoint point = new WarpPoint(name, new RotationVector(location.getRotation(source)), WarpUtils.getRandomColor(), warpDelay);

        for(WarpPoint existing : WarpPointManager.points) {
            if(existing.id.equals(point.id)) throw POINT_ALREADY_EXISTS.create();
        }

        WarpPointManager.points.add(point);
        source.sendFeedback(Text.literal("The warp point '" + name + "' was successfully added"));

        return 1;
    }

    private static WarpPoint getPointByIdOrThrow(String id) throws CommandSyntaxException {
        for(WarpPoint point : WarpPointManager.points) {
            if(point.id.equals(id)) return point;
        }
        throw POINT_DOESNT_EXIST.create();
    }
}
