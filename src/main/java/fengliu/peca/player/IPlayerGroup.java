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

    default void stop(){
        this.manipulation(EntityPlayerActionPack::stopAll);
    }
}
