package fengliu.peca.player;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.context.CommandContext;
import fengliu.peca.PecaMod;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.server.command.ServerCommandSource;

public enum PlayerAutoType {
    STOP((context, player) -> {
        return;
    }, (context, player) -> {
        return;
    }),

    SORT((context, player) -> {
        PecaMod.LOGGER.info(player.currentScreenHandler.toString());
        if (!(player.currentScreenHandler instanceof ShulkerBoxScreenHandler)
            && !(player.currentScreenHandler instanceof GenericContainerScreenHandler)
            && !(player.currentScreenHandler instanceof Generic3x3ContainerScreenHandler)
            && !(player.currentScreenHandler instanceof HopperScreenHandler)){
            return;
        }

        Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        for (ItemStack itemStack: player.getInventory().main){
            if (itemStack.isEmpty()){
                continue;
            }

            if (!itemStack.isOf(item)){
                player.dropItem(itemStack.copy(), false, false);
                itemStack.setCount(0);
                continue;
            }

            for (int index = 0; index < player.currentScreenHandler.slots.size(); index++){
                ItemStack slotStack = player.currentScreenHandler.getSlot(index).getStack();
                if (!slotStack.isEmpty()){
                    continue;
                }

                player.currentScreenHandler.setStackInSlot(index, 0, itemStack.copy());
                itemStack.setCount(0);
            }
        }
    }, (context, player) -> {
        if (!(player.currentScreenHandler instanceof PlayerScreenHandler)){
            player.currentScreenHandler.onClosed(player);
        }
    });

    interface AutoTask{
        void run(CommandContext<ServerCommandSource> context, EntityPlayerMPFake player);
    }

    private final AutoTask task;
    private final AutoTask stopTask;

    PlayerAutoType(AutoTask task, AutoTask stopTask){
        this.task = task;
        this.stopTask = stopTask;
    }

    public void runTask(CommandContext<ServerCommandSource> context, EntityPlayerMPFake player){
        this.task.run(context, player);
    }

    public void stopTask(CommandContext<ServerCommandSource> context, EntityPlayerMPFake player){
        this.stopTask.run(context, player);
    }
}
