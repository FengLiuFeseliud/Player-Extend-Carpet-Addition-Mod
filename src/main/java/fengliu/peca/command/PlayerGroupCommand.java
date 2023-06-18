package fengliu.peca.command;

import carpet.helpers.EntityPlayerActionPack;
import carpet.helpers.EntityPlayerActionPack.Action;
import carpet.helpers.EntityPlayerActionPack.ActionType;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fengliu.peca.PecaSettings;
import fengliu.peca.player.PlayerGroup;
import fengliu.peca.player.PlayerGroup.FormationType;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Direction;

import java.util.function.Consumer;

import static fengliu.peca.util.CommandUtil.getArgOrDefault;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PlayerGroupCommand {
    private static final LiteralArgumentBuilder<ServerCommandSource> PlayerGroupCmd = literal("playerGroup")
        .requires((player) -> CommandHelper.canUseCommand(player, PecaSettings.commandPlayerGroup));

    public static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher){
        PlayerGroupCmd.then(argument("name", StringArgumentType.string())
            .then(literal("spawn").then(argument("amount", IntegerArgumentType.integer()).executes(PlayerGroup::createGroup)
                .then(makeFormationCommands())
                .then(literal("in").then(argument("gamemode", GameModeArgumentType.gameMode())
                    .requires(source -> source.hasPermissionLevel(2)).executes(PlayerGroup::createGroup)
                    .then(makeFormationCommands())))
                .then(literal("at").then(argument("position", Vec3ArgumentType.vec3()).executes(PlayerGroup::createGroup)
                    .then(makeFormationCommands())
                    .then(literal("in").then(argument("gamemode", GameModeArgumentType.gameMode()).executes(PlayerGroup::createGroup)
                        .then(makeFormationCommands())))))))

            .then(literal("kill").executes(context -> {
                PlayerGroup group = PlayerGroup.getGroup(StringArgumentType.getString(context, "name"));
                if (group != null){
                    group.kill();
                }
                return Command.SINGLE_SUCCESS;
            }))
            .then(literal("stop").executes(PlayerGroupCommand::stop)
                .then(literal("from").then(argument("start", IntegerArgumentType.integer(1)).executes(PlayerGroupCommand::stop)
                    .then(literal("to").then(argument("end", IntegerArgumentType.integer(-1)).executes(PlayerGroupCommand::stop))))))
            .then(makeActionCommand("attack", ActionType.ATTACK))
            .then(makeActionCommand("use", ActionType.USE))
            .then(makeActionCommand("jump", ActionType.JUMP))
            .then(makeActionCommand("swapHands", ActionType.SWAP_HANDS))
            .then(makeActionCommand("dropStack", ActionType.DROP_STACK))
            .then(makeDropCommand("drop", false))
            .then(makeDropCommand("dropStack", true))
            .then(makeManipulationCommand("mount", ap -> ap.mount(true))
                .then(makeManipulationCommand("anything", ap -> ap.mount(false))))
            .then(makeManipulationCommand("dismount", EntityPlayerActionPack::dismount))
            .then(makeManipulationCommand("sneak", ap -> ap.setSneaking(true)))
            .then(makeManipulationCommand("unsneak", ap -> ap.setSneaking(false)))
            .then(makeManipulationCommand("sprint", ap -> ap.setSprinting(true)))
            .then(makeManipulationCommand("unsprint", ap -> ap.setSprinting(false)))
            .then(literal("hotbar").then(makeManipulationArgumentCommand("slot", IntegerArgumentType.integer(1, 9),
                context -> playerGroupManipulation(context, ap -> ap.setSlot(IntegerArgumentType.getInteger(context, "slot"))))))
            .then(literal("look")
                .then(makeManipulationCommand("north", ap -> ap.look(Direction.NORTH)))
                .then(makeManipulationCommand("south", ap -> ap.look(Direction.SOUTH)))
                .then(makeManipulationCommand("east", ap -> ap.look(Direction.EAST)))
                .then(makeManipulationCommand("west", ap -> ap.look(Direction.WEST)))
                .then(makeManipulationCommand("up", ap -> ap.look(Direction.UP)))
                .then(makeManipulationCommand("down", ap -> ap.look(Direction.DOWN)))
                .then(literal("at").then(makeManipulationArgumentCommand("position", Vec3ArgumentType.vec3(),
                    context -> playerGroupManipulation(context, ap -> ap.lookAt(Vec3ArgumentType.getVec3(context, "position"))))))
                .then(literal("direction").then(makeManipulationArgumentCommand("direction", RotationArgumentType.rotation(),
                    context -> playerGroupManipulation(context, ap -> ap.look(RotationArgumentType.getRotation(context, "direction").toAbsoluteRotation(context.getSource())))))))
            .then(literal("turn")
                .then(makeManipulationCommand("left", ap -> ap.turn(-90, 0)))
                .then(makeManipulationCommand("right", ap -> ap.turn(90, 0)))
                .then(makeManipulationCommand("back", ap -> ap.turn(180, 0)))
                .then(makeManipulationArgumentCommand("rotation", RotationArgumentType.rotation(),
                    context -> playerGroupManipulation(context, ap -> ap.turn(RotationArgumentType.getRotation(context, "rotation").toAbsoluteRotation(context.getSource()))))))
            .then(literal("move")
                .executes(context -> playerGroupManipulation(context, EntityPlayerActionPack::stopMovement))
                .then(makeManipulationCommand("forward", ap -> ap.setForward(1)))
                .then(makeManipulationCommand("backward", ap -> ap.setForward(-1)))
                .then(makeManipulationCommand("left", ap -> ap.setStrafing(1)))
                .then(makeManipulationCommand("right", ap -> ap.setStrafing(-1))))
        );
        dispatcher.register(PlayerGroupCmd);
    }

    private static int stop(CommandContext<ServerCommandSource> context){
        PlayerGroup group = PlayerGroup.getGroup(StringArgumentType.getString(context, "name"));
        if (group == null){
            return Command.SINGLE_SUCCESS;
        }

        int start = getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "start"), 0);
        int end = getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "end"), -1);
        if (start != 0 || end != -1){
            group.stop(start, end);
        } else {
            group.stop();
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int playerGroupManipulation(CommandContext<ServerCommandSource> context,  Consumer<EntityPlayerActionPack> action){
        PlayerGroup group = PlayerGroup.getGroup(StringArgumentType.getString(context, "name"));
        if (group == null){
            return 0;
        }

        int start = getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "start"), 0);
        int end = getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "end"), -1);
        if (start != 0 || end != -1){
            group.manipulation(action, start, end);
        } else {
            group.manipulation(action);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeManipulationCommand(String actionName, Consumer<EntityPlayerActionPack> action){
        return literal(actionName)
            .executes(context -> playerGroupManipulation(context, action))
            .then(literal("from").then(argument("start", IntegerArgumentType.integer(1))
                .executes(context -> playerGroupManipulation(context, action))
                .then(literal("to").then(argument("end", IntegerArgumentType.integer(-1))
                    .executes(context -> playerGroupManipulation(context, action))))));
    }

    interface ArgumentExecutes{
        int run(CommandContext<ServerCommandSource> context);
    }

    private static <T> RequiredArgumentBuilder<ServerCommandSource, T> makeManipulationArgumentCommand(String name, ArgumentType<T> type, ArgumentExecutes executes){
        return argument(name, type).executes(executes::run)
            .then(literal("from").then(argument("start", IntegerArgumentType.integer())
                .executes(executes::run)
                .then(literal("to").then(argument("end", IntegerArgumentType.integer())
                    .executes(executes::run)))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeActionCommand(String actionName, ActionType type){
        return makeManipulationCommand(actionName, actionPack -> actionPack.start(type, Action.once()))
            .then(makeManipulationCommand("once", actionPack -> actionPack.start(type, Action.once())))
            .then(makeManipulationCommand("continuous", actionPack -> actionPack.start(type, Action.continuous())))
            .then(literal("interval").then(makeManipulationArgumentCommand("ticks", IntegerArgumentType.integer(1),
                context -> playerGroupManipulation(context, actionPack -> actionPack.start(type, Action.interval(IntegerArgumentType.getInteger(context, "ticks")))))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeDropCommand(String actionName, boolean dropAll){
        return literal(actionName)
            .then(makeManipulationCommand("all", ap -> ap.drop(-2, dropAll)))
            .then(makeManipulationCommand("mainhand", ap -> ap.drop(-1, dropAll)))
            .then(makeManipulationCommand("offhand", ap -> ap.drop(40, dropAll)))
            .then(makeManipulationArgumentCommand("slot", IntegerArgumentType.integer(0, 40),
                context -> playerGroupManipulation(context, ap -> ap.drop(IntegerArgumentType.getInteger(context, "slot"), dropAll))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFormationDirectionCommand(FormationType type, Direction direction){
        return literal(direction.getName()).executes(context -> PlayerGroup.createGroup(context, type.getFormationPos(context, direction)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFormationIntersticeCommand(FormationType type){
        return literal("interstice").then(argument("length", IntegerArgumentType.integer())
            .executes(context -> PlayerGroup.createGroup(context, type.getFormationPos(context)))
            .then(makeFormationDirectionCommand(type, Direction.NORTH))
            .then(makeFormationDirectionCommand(type, Direction.SOUTH))
            .then(makeFormationDirectionCommand(type, Direction.EAST))
            .then(makeFormationDirectionCommand(type, Direction.WEST)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFormationCommand(FormationType type){
        return literal(type.name).executes(context -> PlayerGroup.createGroup(context, type.getFormationPos(context)))
            .then(makeFormationIntersticeCommand(type))
            .then(makeFormationDirectionCommand(type, Direction.NORTH))
            .then(makeFormationDirectionCommand(type, Direction.SOUTH))
            .then(makeFormationDirectionCommand(type, Direction.EAST))
            .then(makeFormationDirectionCommand(type, Direction.WEST));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFormationRowCommand(FormationType type){
        return literal(type.name).then(argument("row", IntegerArgumentType.integer())
            .executes(context -> PlayerGroup.createGroup(context, type.getFormationPos(context)))
            .then(makeFormationIntersticeCommand(type))
            .then(makeFormationDirectionCommand(type, Direction.NORTH))
            .then(makeFormationDirectionCommand(type, Direction.SOUTH))
            .then(makeFormationDirectionCommand(type, Direction.EAST))
            .then(makeFormationDirectionCommand(type, Direction.WEST)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFormationCommands(){
        return literal("formation")
            .then(makeFormationCommand(FormationType.COLUMN))
            .then(makeFormationRowCommand(FormationType.COLUMN_FOLD))
            .then(makeFormationCommand(FormationType.ROW))
            .then(makeFormationRowCommand(FormationType.ROW_FOLD))
            .then(makeFormationRowCommand(FormationType.QUADRANGLE));
    }
}
