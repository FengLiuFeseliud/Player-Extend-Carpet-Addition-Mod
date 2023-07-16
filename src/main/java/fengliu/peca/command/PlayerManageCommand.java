package fengliu.peca.command;

import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fengliu.peca.PecaSettings;
import fengliu.peca.player.sql.PlayerData;
import fengliu.peca.player.sql.PlayerSql;
import fengliu.peca.util.Page;
import fengliu.peca.util.TextClickUtil;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PlayerManageCommand {
    private static final LiteralArgumentBuilder<ServerCommandSource> PlayerManageCmd = literal("playerManage")
            .requires((player) -> CommandHelper.canUseCommand(player, PecaSettings.commandPlayerManage));

    public static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandBuildContext) {
        PlayerManageCmd
                .then(literal("list").executes(c -> find(c, PlayerSql::readPlayer)))
                .then(literal("find")

                        .then(argument("name", StringArgumentType.string())
                                .executes(c -> find(c, PlayerSql::readPlayer))
                                .then(makeFindAtCommand())
                                .then(makeFindInCommand())
                                .then(makeFindInDimensionCommand()))

                        .then(literal("gamemode").then(argument("gamemode", GameModeArgumentType.gameMode())
                                .executes(c -> find(c, PlayerSql::readPlayer))
                                .then(makeFindIsCommand())
                                .then(makeFindAtCommand())
                                .then(makeFindInDimensionCommand())))

                        .then(literal("pos").then(argument("pos", Vec3ArgumentType.vec3())
                                .executes(c -> find(c, PlayerSql::readPlayer))
                                .then(literal("inside")
                                        .then(argument("offset", IntegerArgumentType.integer(0))
                                                .executes(c -> find(c, PlayerSql::readPlayer))
                                                .then(makeFindIsCommand())
                                                .then(makeFindInCommand())
                                                .then(makeFindInDimensionCommand())))))

                        .then(literal("dimension").then(argument("dimension", DimensionArgumentType.dimension())
                                .executes(c -> find(c, PlayerSql::readPlayer))
                                .then(makeFindIsCommand())
                                .then(makeFindAtCommand())
                                .then(makeFindInCommand()))))

                .then(argument("player", EntityArgumentType.players())
                        .then(literal("save").then(argument("purpose", StringArgumentType.string()).executes(PlayerManageCommand::save)))
                        .then(literal("info").executes(PlayerManageCommand::info)))
                .then(literal("id").then(argument("id", LongArgumentType.longArg(0))
                        .then(literal("info").executes(PlayerManageCommand::infoId))
                        .then(literal("delete").executes(PlayerManageCommand::delete))));

        dispatcher.register(PlayerManageCmd);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFindIsCommand(){
        return literal("is").then(argument("name", StringArgumentType.string())
                .executes(c -> find(c, PlayerSql::readPlayer)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFindAtCommand(){
        return literal("at").then(argument("pos", Vec3ArgumentType.vec3())
                .executes(c -> find(c, PlayerSql::readPlayer))
                .then(literal("inside")
                        .then(argument("offset", IntegerArgumentType.integer(0))
                                .executes(c -> find(c, PlayerSql::readPlayer)))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFindInCommand(){
        return literal("in").then(argument("gamemode", GameModeArgumentType.gameMode())
                .executes(c -> find(c, PlayerSql::readPlayer)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFindInDimensionCommand(){
        return literal("in").then(argument("dimension", DimensionArgumentType.dimension())
                .executes(c -> find(c, PlayerSql::readPlayer)));
    }

    private static boolean booleanPrintMsg(boolean bool, MutableText text, MutableText errorText, CommandContext<ServerCommandSource> context){
        if (bool){
            context.getSource().sendMessage(text);
        } else {
            context.getSource().sendError(errorText);
        }
        return bool;
    }

    private static String getLoggedText(ServerCommandSource context, PlayerData playerData){
        String loggedText;
        ServerPlayerEntity player = context.getServer().getPlayerManager().getPlayer(playerData.name());
        if (player == null){
            loggedText = "peca.info.command.player.not.logged";
        } else if (!(player instanceof EntityPlayerMPFake)){
            loggedText = "peca.info.command.player.not.fake";
        } else {
            loggedText = "peca.info.command.player.logged";
        }
        return loggedText;
    }

    public static class PlayerPage extends Page<PlayerData> {

        public PlayerPage(ServerCommandSource context, List<PlayerData> data) {
            super(context, data);
        }

        @Override
        public List<MutableText> putPageData(PlayerData pageData, int index) {


            List<MutableText> texts = new ArrayList<>();
            texts.add(Text.literal(String.format("[%s] ", index))
                    .append(pageData.name())
                    .append(" - ")
                    .append(Text.translatable(getLoggedText(this.context, pageData)))
                    .append(" ")
                    .append(Text.literal(pageData.dimension().getPath() + " ").setStyle(Style.EMPTY.withColor(0xAAAAAA)))
                    .append(Text.literal(String.format("x: %d y: %d z:%d", (int) pageData.pos().x, (int) pageData.pos().y, (int) pageData.pos().z)).setStyle(Style.EMPTY.withColor(0x00AAAA)))
            );
            texts.add(Text.translatable("peca.info.command.player.purpose", "§6" + pageData.purpose())
                    .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.spawn"), String.format(
                            "/player %s spawn at %g %g %g facing %g %g in %s in %s",
                            pageData.name(),
                            pageData.pos().x,
                            pageData.pos().y,
                            pageData.pos().z,
                            pageData.yaw(),
                            pageData.pitch(),
                            pageData.dimension().getPath(),
                            pageData.gamemode().getName()
                    )))
                    .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.kill"), String.format("/player %s kill", pageData.name())))
                    .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.info"), String.format("/playerManage id %s info", pageData.id())))
                    .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.delete"), String.format("/playerManage id %s delete", pageData.id())))
            );
            return texts;
        }
    }

    interface Find{
        List<PlayerData> run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException;
    }

    private static int find(CommandContext<ServerCommandSource> context, Find find){
        List<PlayerData> lists = null;
        try {
            lists = find.run(context);
        } catch (CommandSyntaxException e) {
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.find.empty"));
            return -1;
        }

        if (lists.isEmpty()) {
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.find.empty"));
            return -1;
        }

        Page<?> page = new PlayerPage(context.getSource(), lists);
        PecaCommand.addPage(Objects.requireNonNull(context.getSource().getPlayer()), page);
        page.look();
        return Command.SINGLE_SUCCESS;
    }

    private static int save(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String purpose = StringArgumentType.getString(context, "purpose");
        if (purpose.isEmpty()){
            context.getSource().sendError(Text.translatable("peca.info.command.error.not.bot.purpose"));
            return -1;
        }

        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        booleanPrintMsg(PlayerSql.savePlayer(player, context.getSource().getPlayer(), purpose),
                Text.translatable("peca.info.command.player.save", player.getName()),
                Text.translatable("peca.info.command.player.save", player.getName()),
                context
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int delete(CommandContext<ServerCommandSource> context){
        long id = LongArgumentType.getLong(context, "id");
        booleanPrintMsg(PlayerSql.deletePlayer(id),
                Text.translatable("peca.info.command.player.delete.info", id),
                Text.translatable("peca.info.command.error.player.delete", id),
                context
        );
        return Command.SINGLE_SUCCESS;
    }

    private static void printInfo(CommandContext<ServerCommandSource> context, PlayerData playerData){
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.name", playerData.name() + " - " + Text.translatable(getLoggedText(context.getSource(), playerData)).getString()));
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.dimension", playerData.dimension().getPath()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.pos", playerData.pos().x, playerData.pos().y, playerData.pos().z).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.pos.overworld", playerData.pos().x * 8, playerData.pos().y * 8, playerData.pos().z * 8).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.yaw", playerData.yaw()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.pitch", playerData.pitch()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.gamemode", playerData.gamemode().getName()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        if (playerData.id() != -1){
            ServerPlayerEntity createPlayer = context.getSource().getServer().getPlayerManager().getPlayer(playerData.createPlayerUuid());
            if (createPlayer != null){
                context.getSource().sendMessage(Text.translatable("peca.info.command.player.create.player", createPlayer.getName()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
            }

            context.getSource().sendMessage(Text.translatable("peca.info.command.player.create.player.uuid", playerData.createPlayerUuid()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.create.time", playerData.createTime()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.purpose", playerData.purpose()));
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.id", playerData.id()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.data.info").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
    }

    private static int info(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        if (!(player instanceof EntityPlayerMPFake)){
            context.getSource().sendError(Text.translatable(""));
            return -1;
        }

        printInfo(context, PlayerData.fromPlayer(EntityArgumentType.getPlayer(context, "player")));
        return Command.SINGLE_SUCCESS;
    }

    private static int infoId(CommandContext<ServerCommandSource> context){
        printInfo(context, PlayerSql.readPlayer(LongArgumentType.getLong(context, "id")));
        return Command.SINGLE_SUCCESS;
    }
}
