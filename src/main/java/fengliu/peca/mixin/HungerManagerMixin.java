package fengliu.peca.mixin;


import carpet.patches.EntityPlayerMPFake;
import fengliu.peca.PecaSettings;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public class HungerManagerMixin {

    @Shadow private int foodTickTimer;

    @Inject(method = "update", at = @At("HEAD"))
    public void fakePlayerNotHunger(PlayerEntity player, CallbackInfo ci){
        if (player instanceof EntityPlayerMPFake && PecaSettings.fakePlayerNotHunger){
            this.foodTickTimer = 0;
        }
    }
}
