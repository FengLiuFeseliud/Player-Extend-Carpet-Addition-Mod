package fengliu.peca.player;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;

import java.util.List;
import java.util.function.Consumer;

/**
 * 假人组
 */
public interface IPlayerGroup {

    /**
     * 获取假人组所有假人
     *
     * @return 假人列表
     */
    List<EntityPlayerMPFake> getBots();

    /**
     * 获取假人组名
     *
     * @return 假人组名
     */
    String getName();

    /**
     * 获取假人组假人数
     *
     * @return 假人数
     */
    int getAmount();

    default List<EntityPlayerMPFake> subBot(int start, int end) {
        int playerGroupSize = this.getBots().size();
        if (end <= -1) {
            end = playerGroupSize;
        } else if (end > playerGroupSize) {
            end = playerGroupSize;
        } else if (start - 1 > playerGroupSize) {
            start = playerGroupSize;
        } else if (start - 1 < 0) {
            start = 1;
        }

        return this.getBots().subList(start - 1, end);
    }

    /**
     * 假人组添加假人
     *
     * @param bot 假人
     */
    default void add(EntityPlayerMPFake bot) {
        this.getBots().add(bot);
    }

    /**
     * 假人组删除假人
     *
     * @param player 假人
     * @return 被删除假人, 没有删除 null
     */
    default EntityPlayerMPFake del(EntityPlayerMPFake player) {
        for (EntityPlayerMPFake fakePlayer : this.getBots()) {
            if (fakePlayer.getUuid().equals(player.getUuid())) {
                this.getBots().remove(fakePlayer);
                fakePlayer.kill();
                return fakePlayer;
            }
        }
        return null;
    }

    /**
     * 杀死组所有假人
     */
    default void kill() {
        this.getBots().forEach(EntityPlayerMPFake::kill);

    }

    /**
     * 假人组假人执行操作
     *
     * @param action 执行操作
     */
    default void manipulation(Consumer<EntityPlayerActionPack> action) {
        this.getBots().forEach(bot -> {
            action.accept(((ServerPlayerInterface) bot).getActionPack());
        });
    }

    /**
     * 假人组假人执行操作, 可指定范围
     *
     * @param action 执行操作
     */
    default void manipulation(Consumer<EntityPlayerActionPack> action, int start, int end) {
        this.subBot(start, end).forEach(bot -> {
            action.accept(((ServerPlayerInterface) bot).getActionPack());
        });
    }

    /**
     * 假人组所有假人停止操作
     */
    default void stop() {
        this.manipulation(EntityPlayerActionPack::stopAll);
    }

    /**
     * 假人组所有假人停止操作, 可指定范围
     */
    default void stop(int start, int end) {
        this.manipulation(EntityPlayerActionPack::stopAll, start, end);
    }
}
