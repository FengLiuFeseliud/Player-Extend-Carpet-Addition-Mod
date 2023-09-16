package fengliu.peca.util;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.context.CommandContext;
import fengliu.peca.PecaSettings;
import fengliu.peca.player.PlayerGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerUtil {

    public static boolean canSpawn(String name, PlayerManager playerManager){
        AtomicBoolean canSpawn = new AtomicBoolean(true);
        playerManager.getPlayerList().forEach(player -> {
            if (player.getName().getString().equals(name)){
                canSpawn.set(false);
            }
        });
        return canSpawn.get();
    }

    public static boolean canSpawn(String name, CommandContext<ServerCommandSource> context){
        return canSpawn(name, context.getSource().getServer().getPlayerManager());
    }

    public static boolean canSpawnGroup(String nameHand, CommandContext<ServerCommandSource> context){
        if (PecaSettings.groupCanBePlayerLogInSpawn){
            return true;
        }

        if (PlayerGroup.getGroup(nameHand) != null){
            return false;
        }

        AtomicBoolean canSpawn = new AtomicBoolean(true);
        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
            if (player.getName().getString().contains(nameHand)){
                canSpawn.set(false);
            }
        });
        return canSpawn.get();
    }

    public static EntityPlayerMPFake spawn(String name, Vec3d pos, GameMode mode, CommandContext<ServerCommandSource> context){
        if (!canSpawn(name, context)){
            return null;
        }

        if (PecaSettings.fakePlayerGameModeLockSurvive){
            mode = GameMode.SURVIVAL;
        }

        if (mode == null){
            return spawn(name, pos, context);
        }

        PlayerEntity sourcePlayer = context.getSource().getPlayer();
        Vec2f facing = context.getSource().getRotation();

        boolean flying = false;
        if (sourcePlayer instanceof ServerPlayerEntity player && mode != GameMode.SURVIVAL){
            flying = player.getAbilities().flying;
        }

        if (mode == GameMode.SPECTATOR){
            flying = true;
        }

        return EntityPlayerMPFake.createFake(name, context.getSource().getServer(), pos.getX(), pos.getY(), pos.getY(), facing.y, facing.x, context.getSource().getPlayer().getSpawnPointDimension(), mode, flying);
    }

    public static EntityPlayerMPFake spawn(String name, Vec3d pos, CommandContext<ServerCommandSource> context){
        GameMode mode = GameMode.CREATIVE;
        PlayerEntity sourcePlayer = context.getSource().getPlayer();

        if (sourcePlayer instanceof ServerPlayerEntity player){
            if (!player.isCreative() && !player.isSpectator()){
                mode = GameMode.SURVIVAL;
            } else if (player.isSpectator()) {
                mode = GameMode.SPECTATOR;
            }
        }

        return spawn(name, pos, mode, context);
    }

    public static int getItemSlot(ItemStack stack, PlayerInventory inventory){
        for (int slotIndex = 0; slotIndex < inventory.size(); slotIndex++){
            if (!inventory.getStack(slotIndex).isOf(stack.getItem())){
                continue;
            }
            return slotIndex;
        }
        return -1;
    }

    public static int getItemSlotAndCount(ItemStack stack, PlayerInventory inventory){
        for (int slotIndex = 0; slotIndex < inventory.size(); slotIndex++){
            ItemStack slotStack = inventory.getStack(slotIndex);
            if (!slotStack.isOf(stack.getItem()) || slotStack.getCount() < stack.getCount()){
                continue;
            }
            return slotIndex;
        }
        return -1;
    }
}
