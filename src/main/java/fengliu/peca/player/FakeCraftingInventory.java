package fengliu.peca.player;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

public class FakeCraftingInventory extends CraftingInventory {
    private final DefaultedList<ItemStack> stacks;

    public FakeCraftingInventory() {
        super(null, 3, 3);
        this.stacks = DefaultedList.ofSize(9, ItemStack.EMPTY);
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.stacks) {
            if (itemStack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot >= this.size()) {
            return ItemStack.EMPTY;
        }
        return this.stacks.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.stacks, slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.stacks, slot, amount);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.stacks.set(slot, stack);
    }

    @Override
    public void provideRecipeInputs(RecipeMatcher finder) {
        for (ItemStack itemStack : this.stacks) {
            finder.addUnenchantedInput(itemStack);
        }
    }

    public void copy(List<ItemStack> stacks){
        for (int index = 0; index < this.stacks.size(); index++){
            this.stacks.set(index, stacks.get(index));
        }
    }
}
