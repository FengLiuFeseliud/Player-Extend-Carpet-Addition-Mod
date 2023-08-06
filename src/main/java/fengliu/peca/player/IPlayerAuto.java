package fengliu.peca.player;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;

/**
 * 假人自动任务
 */
public interface IPlayerAuto {

    /**
     * 获取假人自动任务类型
     *
     * @return 假人自动任务类型
     */
    PlayerAutoType getAutoType();

    /**
     * 设置假人自动任务
     *
     * @param context 指令上下文
     * @param type    自动任务类型
     */
    void setAutoType(CommandContext<ServerCommandSource> context, PlayerAutoType type);

    /**
     * 假人执行自动任务
     */
    void runAutoTask();

    /**
     * 假人停止自动任务
     */
    void stopAutoTask();

    /**
     * 设置假人自动任务
     *
     * @param context 指令上下文
     * @param type    自动任务类型
     * @return Command.SINGLE_SUCCESS;
     */
    static int setPlayerAutoType(CommandContext<ServerCommandSource> context, PlayerAutoType type) {
        if (!(context.getSource().getServer().getPlayerManager().getPlayer(StringArgumentType.getString(context, "player")) instanceof EntityPlayerMPFake fakePlay)) {
            return Command.SINGLE_SUCCESS;
        }

        ((IPlayerAuto) fakePlay).setAutoType(context, type);
        return Command.SINGLE_SUCCESS;
    }
}
