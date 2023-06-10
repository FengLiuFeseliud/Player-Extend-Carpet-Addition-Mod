package fengliu.peca.mixin;

import carpet.patches.EntityPlayerMPFake;
import fengliu.peca.PecaSettings;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow @Final protected ServerPlayerEntity player;

    @Shadow public abstract boolean changeGameMode(GameMode gameMode);

    @Inject(method = "changeGameMode", at = @At("HEAD"), cancellable = true)
    public void fakePlayerGameModeLockSurvive(GameMode gameMode, CallbackInfoReturnable<Boolean> cir){
        if (this.player instanceof EntityPlayerMPFake && PecaSettings.fakePlayerGameModeLockSurvive) {
            this.player.getAbilities().flying = false;
            this.changeGameMode(GameMode.SURVIVAL);
            cir.cancel();
        }
    }
}
