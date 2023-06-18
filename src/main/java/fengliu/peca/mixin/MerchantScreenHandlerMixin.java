package fengliu.peca.mixin;

import fengliu.peca.util.IMerchantScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MerchantScreenHandler.class)
public class MerchantScreenHandlerMixin implements IMerchantScreenHandler {

    @Shadow @Final private Merchant merchant;

    public Merchant getMerchant(){
        return this.merchant;
    }
}
