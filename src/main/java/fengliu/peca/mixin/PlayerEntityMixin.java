package fengliu.peca.mixin;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import fengliu.peca.PecaSettings;
import fengliu.peca.player.IPlayerAuto;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Shadow
    public abstract boolean damage(DamageSource source, float amount);

    @Shadow
    public int totalExperience;
    @Shadow
    public int experienceLevel;

    @Shadow
    @Final
    protected static TrackedData<Byte> PLAYER_MODEL_PARTS;

    @Shadow
    public abstract PlayerInventory getInventory();

    @Shadow
    @Final
    private PlayerInventory inventory;

    @Shadow
    public abstract void onDeath(DamageSource damageSource);

    @Shadow
    @Nullable
    public abstract ItemEntity dropItem(ItemStack stack, boolean retainOwnership);

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void fakePlayerImmuneDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((PlayerEntity) (Object) this instanceof EntityPlayerMPFake) {
            if (PecaSettings.fakePlayerImmuneDamage) {
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
    public void fakePlayerKeepInventory(CallbackInfo ci) {
        if ((PlayerEntity) (Object) this instanceof EntityPlayerMPFake && PecaSettings.fakePlayerKeepInventory) {
            super.dropInventory();
            ci.cancel();
        }
    }

    private void playerReplaceLowTool() {
        ItemStack mainStack = this.getMainHandStack();
        if (!mainStack.isDamageable()) {
            return;
        }

        if (mainStack.getDamage() + 10 < mainStack.getMaxDamage()) {
            return;
        }

        PlayerInventory inventory = this.getInventory();
        for (int index = 0; index < 54; index++) {
            ItemStack itemStack = inventory.getStack(index);
            if (!mainStack.isOf(itemStack.getItem())) {
                continue;
            }

            int slot = inventory.getSlotWithStack(itemStack);
            if (slot == inventory.selectedSlot) {
                continue;
            }

            ItemStack copyItem = itemStack.copy();
            if (PecaSettings.playerDropLowTool) {
                this.dropItem(mainStack.copy(), false);
                mainStack.decrement(1);
                itemStack.decrement(1);
            } else {
                inventory.setStack(slot, mainStack);
            }
            inventory.setStack(inventory.selectedSlot, copyItem);
            return;
        }
        ((ServerPlayerInterface) this).getActionPack().stopAll();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void fakePlayerRunAutoTask(CallbackInfo ci) {
        if (!((PlayerEntity) (Object) this instanceof EntityPlayerMPFake)) {
            return;
        }

        ((IPlayerAuto) this).runAutoTask();
        if (PecaSettings.playerReplaceLowTool) {
            playerReplaceLowTool();
        }
    }
}
