package fengliu.peca.util;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.context.CommandContext;
import fengliu.peca.PecaSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerUtil {

    public static boolean canSpawn(String name, CommandContext<ServerCommandSource> context){
        if (PecaSettings.playerReset){
            return true;
        }

        AtomicBoolean canSpawn = new AtomicBoolean(true);
        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
            if (player.getName().getString().contains(name)){
                canSpawn.set(false);
            }
        });
        return canSpawn.get();
    }

    public static EntityPlayerMPFake spawn(String name, Vec3d pos, GameMode mode, CommandContext<ServerCommandSource> context){
        if (!canSpawn(name, context)){
            return null;
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

        return EntityPlayerMPFake.createFake(name, context.getSource().getServer(), pos, facing.y, facing.x, context.getSource().getPlayer().getSpawnPointDimension(), mode, flying);
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
}
