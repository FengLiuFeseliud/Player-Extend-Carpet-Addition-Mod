package fengliu.peca.command;

import carpet.helpers.EntityPlayerActionPack;
import carpet.helpers.EntityPlayerActionPack.Action;
import carpet.helpers.EntityPlayerActionPack.ActionType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fengliu.peca.player.PlayerGroup;
import fengliu.peca.player.PlayerGroup.FormationType;

import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Direction;


import java.util.function.Consumer;

import static net.minecraft.server.command.CommandManager.*;
import static net.minecraft.server.command.CommandManager.literal;

public class PlayerGroupCommand {
    private static final LiteralArgumentBuilder<ServerCommandSource> PlayerGroupCommand = literal("playerGroup");

    public static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher){
        PlayerGroupCommand.then(argument("name", StringArgumentType.string())
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
            .then(literal("stop").executes(context -> {
                PlayerGroup group = PlayerGroup.getGroup(StringArgumentType.getString(context, "name"));
                if (group != null){
                    group.stop();
                }
                return Command.SINGLE_SUCCESS;
            }))
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
            .then(literal("hotbar").then(argument("slot", IntegerArgumentType.integer(1, 9))
                    .executes(context -> playerGroupManipulation(context, ap -> ap.setSlot(IntegerArgumentType.getInteger(context, "slot"))))))
            .then(literal("look")
                .then(makeManipulationCommand("north", ap -> ap.look(Direction.NORTH)))
                .then(makeManipulationCommand("south", ap -> ap.look(Direction.SOUTH)))
                .then(makeManipulationCommand("east", ap -> ap.look(Direction.EAST)))
                .then(makeManipulationCommand("west", ap -> ap.look(Direction.WEST)))
                .then(makeManipulationCommand("up", ap -> ap.look(Direction.UP)))
                .then(makeManipulationCommand("down", ap -> ap.look(Direction.DOWN)))
                .then(literal("at").then(argument("position", Vec3ArgumentType.vec3())
                    .executes(context -> playerGroupManipulation(context, ap -> ap.lookAt(Vec3ArgumentType.getVec3(context, "position"))))))
                .then(argument("direction", RotationArgumentType.rotation())
                    .executes(context -> playerGroupManipulation(context, ap -> ap.look(RotationArgumentType.getRotation(context, "direction").toAbsoluteRotation(context.getSource()))))))
            .then(literal("turn")
                .then(makeManipulationCommand("left", ap -> ap.turn(-90, 0)))
                .then(makeManipulationCommand("right", ap -> ap.turn(90, 0)))
                .then(makeManipulationCommand("back", ap -> ap.turn(180, 0)))
                .then(argument("rotation", RotationArgumentType.rotation())
                    .executes(context -> playerGroupManipulation(context, ap -> ap.turn(RotationArgumentType.getRotation(context, "rotation").toAbsoluteRotation(context.getSource()))))))
            .then(literal("move")
                .executes(context -> playerGroupManipulation(context, EntityPlayerActionPack::stopMovement))
                .then(makeManipulationCommand("forward", ap -> ap.setForward(1)))
                .then(makeManipulationCommand("backward", ap -> ap.setForward(-1)))
                .then(makeManipulationCommand("left", ap -> ap.setStrafing(1)))
                .then(makeManipulationCommand("right", ap -> ap.setStrafing(-1))))
        );
        dispatcher.register(PlayerGroupCommand);
    }

    private static int playerGroupManipulation(CommandContext<ServerCommandSource> context,  Consumer<EntityPlayerActionPack> action){
        PlayerGroup group = PlayerGroup.getGroup(StringArgumentType.getString(context, "name"));
        if (group == null){
            return 0;
        }

        group.manipulation(action);
        return Command.SINGLE_SUCCESS;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeManipulationCommand(String actionName, Consumer<EntityPlayerActionPack> action){
        return literal(actionName).executes(context -> playerGroupManipulation(context, action));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeActionCommand(String actionName, ActionType type){
        return literal(actionName)
            .executes(context -> playerGroupManipulation(context, actionPack -> actionPack.start(type, Action.once())))
            .then(literal("once").executes(context -> playerGroupManipulation(context, actionPack -> actionPack.start(type, Action.once()))))
            .then(literal("continuous").executes(context -> playerGroupManipulation(context, actionPack -> actionPack.start(type, Action.continuous()))))
            .then(literal("interval").then(argument("ticks", IntegerArgumentType.integer(1))
                .executes(context -> playerGroupManipulation(context, actionPack -> actionPack.start(type, Action.interval(IntegerArgumentType.getInteger(context, "ticks")))))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeDropCommand(String actionName, boolean dropAll){
        return literal(actionName)
            .then(literal("all").executes(context -> playerGroupManipulation(context, ap -> ap.drop(-2, dropAll))))
            .then(literal("mainhand").executes(context -> playerGroupManipulation(context, ap -> ap.drop(-1, dropAll))))
            .then(literal("offhand").executes(context -> playerGroupManipulation(context, ap -> ap.drop(40, dropAll))))
            .then(argument("slot", IntegerArgumentType.integer(0, 40))
                .executes(context -> playerGroupManipulation(context, ap -> ap.drop(IntegerArgumentType.getInteger(context, "slot"), dropAll))));
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
