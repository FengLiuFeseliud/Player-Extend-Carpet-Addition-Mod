package fengliu.peca.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fengliu.peca.util.Page;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PecaCommand {
    private static final LiteralArgumentBuilder<ServerCommandSource> PecaCmd = literal("peca");
    private static final Map<UUID, Page<?>> pages = new HashMap<>();

    public static void addPage(ServerPlayerEntity player, Page<?> page){
        pages.put(player.getUuid(), page);
    }

    public static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandBuildContext){
        PecaCmd.then(makePageCommand("next", (context, page) -> page.next()))
            .then(makePageCommand("prev", (context, page) -> page.prev()))
            .then(makePageCommand("to", (context, page) -> page.to(IntegerArgumentType.getInteger(context, "page"))).then(argument("page", IntegerArgumentType.integer(0))));

        dispatcher.register(PecaCmd);
    }

    interface runPage{
        void run(CommandContext<ServerCommandSource> context, Page<?> page);
    }

    public static LiteralArgumentBuilder<ServerCommandSource> makePageCommand(String name, runPage run){
        return literal(name).executes(context -> {
            PlayerEntity player = context.getSource().getPlayer();
            if (player == null){
                return Command.SINGLE_SUCCESS;
            }

            if (!pages.containsKey(player.getUuid())){
                return Command.SINGLE_SUCCESS;
            }

            run.run(context, pages.get(player.getUuid()));
            return Command.SINGLE_SUCCESS;
        });
    }
}
