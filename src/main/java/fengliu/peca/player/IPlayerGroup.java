package fengliu.peca.player;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;

import java.util.List;
import java.util.function.Consumer;

public interface IPlayerGroup {

    List<EntityPlayerMPFake> getBots();
    String getName();
    int getAmount();

    default List<EntityPlayerMPFake> subBot(int start, int end){
        int playerGroupSize = this.getBots().size();
        if (end <= -1){
            end = playerGroupSize;
        } else if (end > playerGroupSize){
            end = playerGroupSize;
        } else if (start - 1 > playerGroupSize){
            start = playerGroupSize;
        } else if (start - 1 < 0){
            start = 1;
        }

        return this.getBots().subList(start - 1, end);
    }

    default void add(EntityPlayerMPFake bot){
        this.getBots().add(bot);
    }

    default void kill(){
        this.getBots().forEach(EntityPlayerMPFake::kill);
    }

    default void manipulation(Consumer<EntityPlayerActionPack> action){
        this.getBots().forEach(bot -> {
            action.accept(((ServerPlayerInterface) bot).getActionPack());
        });
    }

    default void manipulation(Consumer<EntityPlayerActionPack> action, int start, int end){
        this.subBot(start, end).forEach(bot -> {
            action.accept(((ServerPlayerInterface) bot).getActionPack());
        });
    }

    default void stop(){
        this.manipulation(EntityPlayerActionPack::stopAll);
    }

    default void stop(int start, int end){
        this.manipulation(EntityPlayerActionPack::stopAll, start, end);
    }
}
