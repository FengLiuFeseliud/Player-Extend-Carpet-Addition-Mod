package fengliu.peca.player;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;

public interface IPlayerAuto {

    PlayerAutoType getAutoType();
    void setAutoType(CommandContext<ServerCommandSource> context, PlayerAutoType type);
    void runAutoTask();
    void stopAutoTask();

    static int setPlayerAutoType(CommandContext<ServerCommandSource> context, PlayerAutoType type){
        if (!(context.getSource().getServer().getPlayerManager().getPlayer(StringArgumentType.getString(context, "player")) instanceof EntityPlayerMPFake fakePlay)) {
            return Command.SINGLE_SUCCESS;
        }

        ((IPlayerAuto) fakePlay).setAutoType(context, type);
        return Command.SINGLE_SUCCESS;
    }
}
