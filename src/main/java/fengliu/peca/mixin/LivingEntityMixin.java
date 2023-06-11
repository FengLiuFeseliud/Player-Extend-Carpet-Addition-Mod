package fengliu.peca.mixin;

import carpet.patches.EntityPlayerMPFake;
import fengliu.peca.PecaSettings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "canBreatheInWater", at = @At("RETURN"), cancellable = true)
    public void fakePlayerNotHypoxic(CallbackInfoReturnable<Boolean> cir){
        if ((LivingEntity)(Object) this instanceof EntityPlayerMPFake && PecaSettings.fakePlayerNotHypoxic){
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "canWalkOnFluid", at = @At("RETURN"), cancellable = true)
    public void fakePlayerCanWalkOnFluid(FluidState state, CallbackInfoReturnable<Boolean> cir){
        if ((LivingEntity)(Object) this instanceof EntityPlayerMPFake && PecaSettings.fakePlayerCanWalkOnFluid){
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
