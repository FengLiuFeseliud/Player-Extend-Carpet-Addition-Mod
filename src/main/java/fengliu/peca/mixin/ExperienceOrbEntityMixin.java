package fengliu.peca.mixin;

import carpet.patches.EntityPlayerMPFake;
import fengliu.peca.PecaSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin extends Entity {

    @Shadow private PlayerEntity target;

    @Shadow public abstract boolean isAttackable();

    @Shadow protected abstract boolean isMergeable(ExperienceOrbEntity other);

    public ExperienceOrbEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z",
            shift = At.Shift.BEFORE
        ),
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ExperienceOrbEntity;expensiveUpdate()V"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z")
        ),
        cancellable = true
    )
    public void fakePlayerCanNotSurroundExp(CallbackInfo ci){
        if (this.target instanceof EntityPlayerMPFake && PecaSettings.fakePlayerCanNotSurroundExp){
            this.target = null;
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    public void fakePlayerCanNotAssimilateExp(PlayerEntity player, CallbackInfo ci){
        if (player instanceof EntityPlayerMPFake && PecaSettings.fakePlayerCanNotAssimilateExp){
            ci.cancel();
        }

    }
}
