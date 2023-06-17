package fengliu.peca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fengliu.peca.PecaSettings;
import fengliu.peca.player.IPlayerAuto;
import fengliu.peca.player.PlayerAutoType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PlayerAutoCommand {
    private static final LiteralArgumentBuilder<ServerCommandSource> PlayerAutoCmd = literal("playerAuto")
        .requires((player) -> CommandHelper.canUseCommand(player, PecaSettings.commandPlayerAuto));

    public static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandBuildContext){
        PlayerAutoCmd.then(argument("player", StringArgumentType.word())
            .then(literal("stop").executes(context -> IPlayerAuto.setPlayerAutoType(context, PlayerAutoType.STOP)))
            .then(literal("sort").then(argument("item", ItemStackArgumentType.itemStack(commandBuildContext))
                .executes(context -> IPlayerAuto.setPlayerAutoType(context, PlayerAutoType.SORT))))
            .then(literal("craft")
                    .then(argument("slot0", ItemStackArgumentType.itemStack(commandBuildContext))
                            .then(argument("slot1", ItemStackArgumentType.itemStack(commandBuildContext))
                                    .then(argument("slot2", ItemStackArgumentType.itemStack(commandBuildContext))
                                        .then(argument("slot3", ItemStackArgumentType.itemStack(commandBuildContext))
                                                .then(argument("slot4", ItemStackArgumentType.itemStack(commandBuildContext))
                                                        .then(argument("slot5", ItemStackArgumentType.itemStack(commandBuildContext))
                                                                .then(argument("slot6", ItemStackArgumentType.itemStack(commandBuildContext))
                                                                        .then(argument("slot7", ItemStackArgumentType.itemStack(commandBuildContext))
                                                                                .then(argument("slot8", ItemStackArgumentType.itemStack(commandBuildContext))
                                                                                        .executes(context -> IPlayerAuto.setPlayerAutoType(context, PlayerAutoType.CRAFT)))))))))))));

        dispatcher.register(PlayerAutoCmd);
    }
}
