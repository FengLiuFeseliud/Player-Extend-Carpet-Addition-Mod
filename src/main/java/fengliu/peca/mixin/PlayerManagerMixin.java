package fengliu.peca.mixin;

import fengliu.peca.PecaMod;
import fengliu.peca.PecaSettings;
import fengliu.peca.player.PlayerGroup;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Z)V", at = @At("HEAD"), cancellable = true)
    public void hiddenFakePlayerGroupJoinMessage(Text message, Function<ServerPlayerEntity, Text> playerMessageFactory, boolean overlay, CallbackInfo ci) {
        if (!PecaSettings.hiddenFakePlayerGroupJoinMessage) {
            return;
        }

        for (PlayerGroup playerGroup : PlayerGroup.groups) {
            PecaMod.LOGGER.info(PlayerGroup.groups.toString());
            if (!message.getString().toLowerCase().contains(playerGroup.getName().toLowerCase())) {
                continue;
            }
            ci.cancel();
            return;
        }
    }
}
