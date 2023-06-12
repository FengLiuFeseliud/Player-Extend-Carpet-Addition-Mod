package fengliu.peca.mixin;

import carpet.patches.EntityPlayerMPFake;
import fengliu.peca.PecaSettings;
import fengliu.peca.player.IPlayerAuto;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Shadow public abstract boolean damage(DamageSource source, float amount);
    @Shadow public int totalExperience;
    @Shadow public int experienceLevel;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void fakePlayerImmuneDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
        if ((PlayerEntity) (Object) this instanceof EntityPlayerMPFake){
            if (PecaSettings.fakePlayerImmuneDamage){
                cir.cancel();
            } else if(this.getHealth() <= amount && PecaSettings.fakePlayerNotDie){
                this.setHealth(0.1f);
                this.damage(source, 0);
                cir.cancel();
            } else if (source.isOf(DamageTypes.ON_FIRE) && PecaSettings.fakePlayerImmuneOnFireDamage){
                cir.cancel();
            } else if (source.isOf(DamageTypes.IN_FIRE) && PecaSettings.fakePlayerImmuneInFireDamage){
                cir.cancel();
            } else if (source.isOf(DamageTypes.LAVA) && PecaSettings.fakePlayerImmuneLavaDamage){
                cir.cancel();
            } else if (source.isOf(DamageTypes.LAVA) && PecaSettings.fakePlayerImmuneLavaDamage){
                cir.cancel();
            } else if ((source.isOf(DamageTypes.PLAYER_ATTACK) || source.isOf(DamageTypes.PLAYER_EXPLOSION)) && PecaSettings.fakePlayerImmunePlayerDamage){
                cir.cancel();
            } else if ((source.isOf(DamageTypes.EXPLOSION) || source.isOf(DamageTypes.PLAYER_EXPLOSION)) && PecaSettings.fakePlayerImmuneExplosionDamage){
                cir.cancel();
            } else if (source.isOf(DamageTypes.CRAMMING) && PecaSettings.fakePlayerImmuneCrammingDamage){
                cir.cancel();
            }
        }
    }

    @Inject(method = "getXpToDrop", at = @At("RETURN"), cancellable = true)
    public void fakePlayerDropAllExp(CallbackInfoReturnable<Integer> cir){
        if ((PlayerEntity) (Object) this instanceof EntityPlayerMPFake){
            if (PecaSettings.fakePlayerDropAllExp){
                cir.setReturnValue(this.totalExperience);
            }

            if (PecaSettings.fakePlayerDropExpNoUpperLimit){
                cir.setReturnValue(this.experienceLevel * 7);
            }

            cir.cancel();
        }
    }

    @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
    public void fakePlayerKeepInventory(CallbackInfo ci){
        if ((PlayerEntity) (Object) this instanceof EntityPlayerMPFake && PecaSettings.fakePlayerKeepInventory){
            super.dropInventory();
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void fakePlayerRunAutoTask(CallbackInfo ci){
        if ((PlayerEntity) (Object) this instanceof EntityPlayerMPFake){
            ((IPlayerAuto) this).runAutoTask();
        }
    }
}
