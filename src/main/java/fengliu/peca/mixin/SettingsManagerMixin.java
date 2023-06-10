package fengliu.peca.mixin;

import carpet.api.settings.SettingsManager;
import carpet.utils.Messenger;
import carpet.utils.Translations;
import fengliu.peca.PecaMod;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SettingsManager.class)
public class SettingsManagerMixin {
    @Inject(
        method = "listAllSettings",
        at = @At(
            value = "INVOKE",
            target = "Lcarpet/utils/Messenger;m(Lnet/minecraft/server/command/ServerCommandSource;[Ljava/lang/Object;)V",
            shift = At.Shift.AFTER
        ),
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lcarpet/api/settings/SettingsManager;listSettings(Lnet/minecraft/server/command/ServerCommandSource;Ljava/lang/String;Ljava/util/Collection;)I"),
            to = @At(value = "INVOKE", target = "Ljava/util/ArrayList;<init>()V")
        )
    )
    private void printModVersion(ServerCommandSource source, CallbackInfoReturnable<Integer> cir) {
        Messenger.m(source, String.format("g %s", Translations.tr("peca.info.version").formatted(PecaMod.MOD_VERSION)));
    }
}
