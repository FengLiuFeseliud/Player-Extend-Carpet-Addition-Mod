package fengliu.peca.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fengliu.peca.PecaSettings;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.literal;

public class PlayerGroupManageCommand {
    private static final LiteralArgumentBuilder<ServerCommandSource> PlayerGroupCmd = literal("playerGroupManage")
            .requires((player) -> CommandHelper.canUseCommand(player, PecaSettings.commandPlayerGroupManage));
}
