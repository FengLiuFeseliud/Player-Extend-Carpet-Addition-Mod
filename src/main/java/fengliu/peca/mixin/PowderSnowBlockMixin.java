package fengliu.peca.mixin;

import carpet.patches.EntityPlayerMPFake;
import fengliu.peca.PecaSettings;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    @Inject(method = "canWalkOnPowderSnow", at = @At(value = "HEAD"), cancellable = true)
    private static void fakePlayerNotBeCaughtInPowderSnow(Entity entity, CallbackInfoReturnable<Boolean> cir){
        if (entity instanceof EntityPlayerMPFake && PecaSettings.fakePlayerNotBeCaughtInPowderSnow){
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
