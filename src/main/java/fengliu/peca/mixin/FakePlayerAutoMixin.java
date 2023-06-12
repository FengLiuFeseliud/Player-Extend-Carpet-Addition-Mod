package fengliu.peca.mixin;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import fengliu.peca.player.IPlayerAuto;
import fengliu.peca.player.PlayerAutoType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityPlayerMPFake.class)
public abstract class FakePlayerAutoMixin extends ServerPlayerEntity implements IPlayerAuto {
    private PlayerAutoType autoType = PlayerAutoType.STOP;
    private CommandContext<ServerCommandSource> autoContext;

    public FakePlayerAutoMixin(MinecraftServer server, ServerWorld world, GameProfile profile) {
        super(server, world, profile);
    }

    @Override
    public PlayerAutoType getAutoType() {
        return this.autoType;
    }

    @Override
    public void setAutoType(CommandContext<ServerCommandSource> context, PlayerAutoType type) {
        this.stopAutoTask();
        this.autoType = type;
        this.autoContext = context;
    }

    @Override
    public void runAutoTask() {
        this.autoType.runTask(this.autoContext, (EntityPlayerMPFake) (Object) this);
    }

    @Override
    public void stopAutoTask() {
        this.autoType.stopTask(this.autoContext, (EntityPlayerMPFake) (Object) this);
    }
}
