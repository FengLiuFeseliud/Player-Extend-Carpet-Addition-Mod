package fengliu.peca.player;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.context.CommandContext;
import fengliu.peca.PecaMod;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.World;

import java.util.Optional;

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
    }),

    CRAFT((context, player) -> {
        if(!(player.currentScreenHandler instanceof CraftingScreenHandler crafting) || player.getServer() == null){
            return;
        }

        PlayerInventory inventory = player.getInventory();
        FakeCraftingInventory fakeCraftingInventory = new FakeCraftingInventory();

        for (int index = 0; index < 9; index++){
            ItemStack stack = ItemStackArgumentType.getItemStackArgument(context, "slot" + index).getItem().getDefaultStack();
            if (stack.isEmpty()){
                continue;
            }

            int slot = -1;
            for (int slotIndex = 0; slotIndex < inventory.size(); slotIndex++){
                if (!inventory.getStack(slotIndex).isOf(stack.getItem())){
                    continue;
                }
                slot = slotIndex;
                break;
            }

            if (slot == -1){
                return;
            }

            fakeCraftingInventory.setStack(index, inventory.getStack(slot));
        }

        World world = player.getWorld();
        ItemStack stack = ItemStack.EMPTY;

        while (true){
            Optional<CraftingRecipe> optional = player.getServer().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, fakeCraftingInventory, world);
            if (!optional.isPresent()){
                return;
            }

            ItemStack craftStack = optional.get().craft(fakeCraftingInventory, world.getRegistryManager());
            if (!craftStack.isOf(stack.getItem()) && !stack.isEmpty()){
                return;
            }
            stack = craftStack;
            player.dropItem(craftStack, false, false);
            for(int craftingIndex = 0; craftingIndex < fakeCraftingInventory.size(); craftingIndex++){
                fakeCraftingInventory.getStack(craftingIndex).decrement(1);
            }
        }

    }, (context, player) -> {
        return;
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
