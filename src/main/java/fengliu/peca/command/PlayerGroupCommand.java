package fengliu.peca.command;

import carpet.helpers.EntityPlayerActionPack;
import carpet.helpers.EntityPlayerActionPack.Action;
import carpet.helpers.EntityPlayerActionPack.ActionType;
import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fengliu.peca.PecaSettings;
import fengliu.peca.player.IPlayerGroup;
import fengliu.peca.player.PlayerGroup;
import fengliu.peca.player.PlayerGroup.FormationType;
import fengliu.peca.player.sql.PlayerData;
import fengliu.peca.player.sql.PlayerGroupData;
import fengliu.peca.player.sql.PlayerGroupSql;
import fengliu.peca.util.CommandUtil;
import fengliu.peca.util.Page;
import fengliu.peca.util.TextClickUtil;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static fengliu.peca.util.CommandUtil.booleanPrintMsg;
import static fengliu.peca.util.CommandUtil.getArgOrDefault;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PlayerGroupCommand {
    private static final LiteralArgumentBuilder<ServerCommandSource> PlayerGroupCmd = literal("playerGroup")
            .requires((player) -> CommandHelper.canUseCommand(player, PecaSettings.commandPlayerGroup));

    public static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher) {
        PlayerGroupCmd.then(argument("name", StringArgumentType.string())
                .then(literal("spawn").executes(PlayerGroup::createGroup)
                        .then(argument("amount", IntegerArgumentType.integer()).executes(PlayerGroup::createGroup)
                        .then(makeFormationCommands())
                        .then(literal("in").then(argument("gamemode", GameModeArgumentType.gameMode())
                                .requires(source -> source.hasPermissionLevel(2)).executes(PlayerGroup::createGroup)
                                .then(makeFormationCommands())))
                        .then(literal("at").then(argument("position", Vec3ArgumentType.vec3()).executes(PlayerGroup::createGroup)
                                .then(makeFormationCommands())
                                .then(literal("in").then(argument("gamemode", GameModeArgumentType.gameMode()).executes(PlayerGroup::createGroup)
                                        .then(makeFormationCommands())))))))

                .then(literal("kill").executes(context -> {
                    PlayerGroup group = PlayerGroup.getGroup(StringArgumentType.getString(context, "name"));
                    if (group != null) {
                        group.kill();
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("stop").executes(PlayerGroupCommand::stop)
                        .then(literal("from").then(argument("start", IntegerArgumentType.integer(1)).executes(PlayerGroupCommand::stop)
                                .then(literal("to").then(argument("end", IntegerArgumentType.integer(-1)).executes(PlayerGroupCommand::stop))))))
                .then(makeActionCommand("attack", ActionType.ATTACK))
                .then(makeActionCommand("use", ActionType.USE))
                .then(makeActionCommand("jump", ActionType.JUMP))
                .then(makeActionCommand("swapHands", ActionType.SWAP_HANDS))
                .then(makeActionCommand("dropStack", ActionType.DROP_STACK))
                .then(makeDropCommand("drop", false))
                .then(makeDropCommand("dropStack", true))
                .then(makeManipulationCommand("mount", ap -> ap.mount(true))
                        .then(makeManipulationCommand("anything", ap -> ap.mount(false))))
                .then(makeManipulationCommand("dismount", EntityPlayerActionPack::dismount))
                .then(makeManipulationCommand("sneak", ap -> ap.setSneaking(true)))
                .then(makeManipulationCommand("unsneak", ap -> ap.setSneaking(false)))
                .then(makeManipulationCommand("sprint", ap -> ap.setSprinting(true)))
                .then(makeManipulationCommand("unsprint", ap -> ap.setSprinting(false)))
                .then(literal("hotbar").then(makeManipulationArgumentCommand("slot", IntegerArgumentType.integer(1, 9),
                        context -> playerGroupManipulation(context, ap -> ap.setSlot(IntegerArgumentType.getInteger(context, "slot"))))))
                .then(literal("look")
                        .then(makeManipulationCommand("north", ap -> ap.look(Direction.NORTH)))
                        .then(makeManipulationCommand("south", ap -> ap.look(Direction.SOUTH)))
                        .then(makeManipulationCommand("east", ap -> ap.look(Direction.EAST)))
                        .then(makeManipulationCommand("west", ap -> ap.look(Direction.WEST)))
                        .then(makeManipulationCommand("up", ap -> ap.look(Direction.UP)))
                        .then(makeManipulationCommand("down", ap -> ap.look(Direction.DOWN)))
                        .then(literal("at").then(makeManipulationArgumentCommand("position", Vec3ArgumentType.vec3(),
                                context -> playerGroupManipulation(context, ap -> ap.lookAt(Vec3ArgumentType.getVec3(context, "position"))))))
                        .then(literal("direction").then(makeManipulationArgumentCommand("direction", RotationArgumentType.rotation(),
                                context -> playerGroupManipulation(context, ap -> ap.look(RotationArgumentType.getRotation(context, "direction").toAbsoluteRotation(context.getSource())))))))
                .then(literal("turn")
                        .then(makeManipulationCommand("left", ap -> ap.turn(-90, 0)))
                        .then(makeManipulationCommand("right", ap -> ap.turn(90, 0)))
                        .then(makeManipulationCommand("back", ap -> ap.turn(180, 0)))
                        .then(makeManipulationArgumentCommand("rotation", RotationArgumentType.rotation(),
                                context -> playerGroupManipulation(context, ap -> ap.turn(RotationArgumentType.getRotation(context, "rotation").toAbsoluteRotation(context.getSource()))))))
                .then(literal("move")
                        .executes(context -> playerGroupManipulation(context, EntityPlayerActionPack::stopMovement))
                        .then(makeManipulationCommand("forward", ap -> ap.setForward(1)))
                        .then(makeManipulationCommand("backward", ap -> ap.setForward(-1)))
                        .then(makeManipulationCommand("left", ap -> ap.setStrafing(1)))
                        .then(makeManipulationCommand("right", ap -> ap.setStrafing(-1))))
                .then(literal("save").then(argument("purpose", StringArgumentType.string()).executes(PlayerGroupCommand::save)))
                .then(literal("add").then(argument("player", EntityArgumentType.player()).executes(PlayerGroupCommand::addPlayer)))
                .then(literal("del").then(argument("player", EntityArgumentType.player()).executes(PlayerGroupCommand::delPlayer)))
        );

        PlayerGroupCmd
                .then(literal("id").then(argument("id", LongArgumentType.longArg())
                        .then(literal("delete").executes(PlayerGroupCommand::delete))
                        .then(literal("spawn").executes(PlayerGroupCommand::spawn))
                        .then(literal("info").executes(PlayerGroupCommand::infoId))
                        .then(makeExecuteDelCommands("execute", root -> root.executes(PlayerGroupCommand::execute), PlayerGroupCommand::execute)
                                .then(makeExecuteCommands("add", root -> root.executes(PlayerGroupCommand::executeAdd), PlayerGroupCommand::executeAdd))
                                .then(makeExecuteCommands("set", root -> root.then(argument("commandIndex", IntegerArgumentType.integer(1)).executes(PlayerGroupCommand::executeSet)), PlayerGroupCommand::executeSet))
                                .then(makeExecuteDelCommands("del", root -> root.then(argument("commandIndex", IntegerArgumentType.integer(1)).executes(PlayerGroupCommand::executeDel)), PlayerGroupCommand::executeDel))
                                .then(makeExecuteDelCommands("clear", root -> root.executes(PlayerGroupCommand::executeClear), PlayerGroupCommand::executeClear)))));

        PlayerGroupCmd
                .then(literal("list").executes(c -> find(c, PlayerGroupSql::readPlayerGroup)));

        dispatcher.register(PlayerGroupCmd);
    }

    private static int stop(CommandContext<ServerCommandSource> context) {
        PlayerGroup group = PlayerGroup.getGroup(StringArgumentType.getString(context, "name"));
        if (group == null) {
            context.getSource().sendError(Text.translatable("peca.info.command.error.not.player.group", StringArgumentType.getString(context, "name")));
            return -1;
        }

        int start = getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "start"), 0);
        int end = getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "end"), -1);
        if (start != 0 || end != -1) {
            group.stop(start, end);
        } else {
            group.stop();
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int playerGroupManipulation(CommandContext<ServerCommandSource> context, Consumer<EntityPlayerActionPack> action) {
        PlayerGroup group = PlayerGroup.getGroup(StringArgumentType.getString(context, "name"));
        if (group == null) {
            context.getSource().sendError(Text.translatable("peca.info.command.error.not.player.group", StringArgumentType.getString(context, "name")));
            return -1;
        }

        int start = getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "start"), 0);
        int end = getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "end"), -1);
        if (start != 0 || end != -1) {
            group.manipulation(action, start, end);
        } else {
            group.manipulation(action);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeManipulationCommand(String actionName, Consumer<EntityPlayerActionPack> action) {
        return literal(actionName)
                .executes(context -> playerGroupManipulation(context, action))
                .then(literal("from").then(argument("start", IntegerArgumentType.integer(1))
                        .executes(context -> playerGroupManipulation(context, action))
                        .then(literal("to").then(argument("end", IntegerArgumentType.integer(-1))
                                .executes(context -> playerGroupManipulation(context, action))))));
    }

    interface ArgumentExecutes {
        int run(CommandContext<ServerCommandSource> context);
    }

    private static <T> RequiredArgumentBuilder<ServerCommandSource, T> makeManipulationArgumentCommand(String name, ArgumentType<T> type, ArgumentExecutes executes) {
        return argument(name, type).executes(executes::run)
                .then(literal("from").then(argument("start", IntegerArgumentType.integer())
                        .executes(executes::run)
                        .then(literal("to").then(argument("end", IntegerArgumentType.integer())
                                .executes(executes::run)))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeActionCommand(String actionName, ActionType type) {
        return makeManipulationCommand(actionName, actionPack -> actionPack.start(type, Action.once()))
                .then(makeManipulationCommand("once", actionPack -> actionPack.start(type, Action.once())))
                .then(makeManipulationCommand("continuous", actionPack -> actionPack.start(type, Action.continuous())))
                .then(literal("interval").then(makeManipulationArgumentCommand("ticks", IntegerArgumentType.integer(1),
                        context -> playerGroupManipulation(context, actionPack -> actionPack.start(type, Action.interval(IntegerArgumentType.getInteger(context, "ticks")))))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeDropCommand(String actionName, boolean dropAll) {
        return literal(actionName)
                .then(makeManipulationCommand("all", ap -> ap.drop(-2, dropAll)))
                .then(makeManipulationCommand("mainhand", ap -> ap.drop(-1, dropAll)))
                .then(makeManipulationCommand("offhand", ap -> ap.drop(40, dropAll)))
                .then(makeManipulationArgumentCommand("slot", IntegerArgumentType.integer(0, 40),
                        context -> playerGroupManipulation(context, ap -> ap.drop(IntegerArgumentType.getInteger(context, "slot"), dropAll))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFormationDirectionCommand(FormationType type, Direction direction) {
        return literal(direction.getName()).executes(context -> PlayerGroup.createGroup(context, type.getFormationPos(context, direction)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFormationIntersticeCommand(FormationType type) {
        return literal("interstice").then(argument("length", IntegerArgumentType.integer())
                .executes(context -> PlayerGroup.createGroup(context, type.getFormationPos(context)))
                .then(makeFormationDirectionCommand(type, Direction.NORTH))
                .then(makeFormationDirectionCommand(type, Direction.SOUTH))
                .then(makeFormationDirectionCommand(type, Direction.EAST))
                .then(makeFormationDirectionCommand(type, Direction.WEST)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFormationCommand(FormationType type) {
        return literal(type.name).executes(context -> PlayerGroup.createGroup(context, type.getFormationPos(context)))
                .then(makeFormationIntersticeCommand(type))
                .then(makeFormationDirectionCommand(type, Direction.NORTH))
                .then(makeFormationDirectionCommand(type, Direction.SOUTH))
                .then(makeFormationDirectionCommand(type, Direction.EAST))
                .then(makeFormationDirectionCommand(type, Direction.WEST));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFormationRowCommand(FormationType type) {
        return literal(type.name).then(argument("row", IntegerArgumentType.integer())
                .executes(context -> PlayerGroup.createGroup(context, type.getFormationPos(context)))
                .then(makeFormationIntersticeCommand(type))
                .then(makeFormationDirectionCommand(type, Direction.NORTH))
                .then(makeFormationDirectionCommand(type, Direction.SOUTH))
                .then(makeFormationDirectionCommand(type, Direction.EAST))
                .then(makeFormationDirectionCommand(type, Direction.WEST)));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeFormationCommands() {
        return literal("formation")
                .then(makeFormationCommand(FormationType.COLUMN))
                .then(makeFormationRowCommand(FormationType.COLUMN_FOLD))
                .then(makeFormationCommand(FormationType.ROW))
                .then(makeFormationRowCommand(FormationType.ROW_FOLD))
                .then(makeFormationRowCommand(FormationType.QUADRANGLE));
    }

    interface ExecutesLiteral {
        RequiredArgumentBuilder<ServerCommandSource, ?> literal(RequiredArgumentBuilder<ServerCommandSource, ?> root);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeExecuteCommands(String name, ExecutesLiteral executesLiteral, Command<ServerCommandSource> command) {
        return literal(name).then(argument("command", StringArgumentType.string())
                .executes(command)
                .then(literal("to")
                        .then(executesLiteral.literal(argument("name", StringArgumentType.string())))
                        .then(literal("index").then(executesLiteral.literal(argument("index", IntegerArgumentType.integer(0)))))));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> makeExecuteDelCommands(String name, ExecutesLiteral executesLiteral, Command<ServerCommandSource> command) {
        return literal(name)
                .executes(command)
                .then(literal("to")
                        .then(executesLiteral.literal(argument("name", StringArgumentType.string())))
                        .then(literal("index").then(executesLiteral.literal(argument("index", IntegerArgumentType.integer(0))))));
    }

    private static String getCreateText(PlayerGroupData playerGroupData) {
        String createText;
        if (PlayerGroup.getGroup(playerGroupData.name()) != null) {
            createText = "peca.info.command.player.group.create";
        } else {
            createText = "peca.info.command.player.group.not.create";
        }
        return createText;
    }

    public static class PlayerGroupPage extends Page<PlayerGroupData>{

        public PlayerGroupPage(ServerCommandSource context, List<PlayerGroupData> data) {
            super(context, data);
        }

        @Override
        public List<MutableText> putPageData(PlayerGroupData pageData, int index) {
            List<MutableText> texts = new ArrayList<>();
            texts.add(Text.literal(String.format("[%s] ", index))
                    .append(pageData.name())
                    .append(" §7- ")
                    .append(Text.translatable("peca.info.command.player.group.count", pageData.botCount()).setStyle(Style.EMPTY.withColor(0x00AAAA)))
                    .append(" §7- ")
                    .append(Text.translatable(getCreateText(pageData))));
            texts.add(Text.translatable("peca.info.command.player.group.purpose", "§6" + pageData.purpose())
                    .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.group.spawn"), String.format("/playerGroup id %s spawn", pageData.id())))
                    .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.group.kill"), String.format("/playerGroup %s kill", pageData.name())))
                    .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.group.info"), String.format("/playerGroup id %s info", pageData.id())))
                    .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.group.delete"), String.format("/playerGroup id %s delete", pageData.id())))
            );
            return texts;
        }
    }

    private static int save(CommandContext<ServerCommandSource> context){
        String purpose = StringArgumentType.getString(context, "purpose");
        if (purpose.isEmpty()) {
            context.getSource().sendError(Text.translatable("peca.info.command.error.not.bot.purpose"));
            return -1;
        }

        String name = StringArgumentType.getString(context, "name");
        IPlayerGroup playerGroup = PlayerGroup.getGroup(name);
        if (playerGroup == null){
            context.getSource().sendError(Text.translatable("peca.info.command.error.not.player.group", name));
            return -1;
        }

        CommandUtil.booleanPrintMsg(
                PlayerGroupSql.saveGroup(playerGroup, context.getSource().getPlayer(), purpose),
                Text.translatable("peca.info.command.save.player.group", name),
                Text.translatable("peca.info.command.error.save.player.group"),
                context
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int delete(CommandContext<ServerCommandSource> context){
        long id = LongArgumentType.getLong(context, "id");
        CommandUtil.booleanPrintMsg(
                PlayerGroupSql.deleteGroup(id),
                Text.translatable("peca.info.command.delete.player.group.info", id),
                Text.translatable("peca.info.command.error.delete.player.group", id),
                context
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int spawn(CommandContext<ServerCommandSource> context){
        long id = LongArgumentType.getLong(context, "id");
        CommandUtil.booleanPrintMsg(
                PlayerGroupSql.spawnGroup(id, context.getSource().getServer()),
                Text.translatable("peca.info.command.spawn.player.group", id),
                Text.translatable("peca.info.command.error.spawn.player.group", id),
                context
        );
        return Command.SINGLE_SUCCESS;
    }

    interface Find {
        List<PlayerGroupData> run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException;
    }

    private static int find(CommandContext<ServerCommandSource> context, Find find){
        List<PlayerGroupData> lists = null;
        try {
            lists = find.run(context);
        } catch (CommandSyntaxException e) {
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.group.find.empty"));
            return -1;
        }

        if (lists.isEmpty()) {
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.group.find.empty"));
            return -1;
        }

        Page<?> page = new PlayerGroupPage(context.getSource(), lists);
        PecaCommand.addPage(Objects.requireNonNull(context.getSource().getPlayer()), page);
        page.look();
        return Command.SINGLE_SUCCESS;
    }

    private static void printInfo(CommandContext<ServerCommandSource> context, PlayerGroupData playerGroupData) {
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.group.name", String.format("%s - %s ", playerGroupData.name(), Text.translatable(getCreateText(playerGroupData)).getString())));
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.group.count", playerGroupData.botCount()).setStyle(Style.EMPTY.withColor(0x00AAAA)));

        context.getSource().sendMessage(Text.translatable("peca.info.command.player.group.player.info.1"));
        for(int index = 0; index < playerGroupData.players().size(); index++) {
            PlayerData playerData = playerGroupData.players().get(index);
            context.getSource().sendMessage(Text.literal(String.format("[%s] %s §3- §6x: %s y: %s z: %s §3- §7%s", index + 1, playerData.name(), (int) playerData.pos().x, (int) playerData.pos().y, (int) playerData.pos().z, playerData.dimension())));
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.execute.info",
                    playerData.execute().toString().replace("[", "§3[").replace("]", "§3]").replace(",", "§3,").replace("\"", "§6\"")));
            context.getSource().sendMessage(TextClickUtil.suggestText(Text.translatable("peca.info.command.player.spawn.suggest"), String.format(
                            "/player %s spawn at %g %g %g facing %g %g in %s in %s",
                            playerData.name(),
                            playerData.pos().x,
                            playerData.pos().y,
                            playerData.pos().z,
                            playerData.yaw(),
                            playerData.pitch(),
                            playerData.dimension().getPath(),
                            playerData.gamemode().getName()
                    ))
                    .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.stop"), String.format("/player %s stop", playerData.name())))
                    .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.kill"), String.format("/player %s kill", playerData.name())))
                    .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.group.delete.player"), String.format("/playerGroup %s del %s", playerGroupData.name(), playerData.name()))));
        }
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.group.player.info.2"));

        if (playerGroupData.id() == -1){
            return;
        }

        ServerPlayerEntity createPlayer = context.getSource().getServer().getPlayerManager().getPlayer(playerGroupData.createPlayerUuid());
        if (createPlayer != null) {
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.create.player", createPlayer.getName()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        }
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.create.player.uuid", playerGroupData.createPlayerUuid()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.create.time", playerGroupData.createTime()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        ServerPlayerEntity lastModifiedPlayer = context.getSource().getServer().getPlayerManager().getPlayer(playerGroupData.lastModifiedPlayerUuid());
        if (lastModifiedPlayer != null) {
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.last.modified.player", lastModifiedPlayer.getName()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        }
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.last.modified.player.uuid", playerGroupData.lastModifiedPlayerUuid()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.last.modified.time", playerGroupData.lastModifiedTime()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.group.purpose", playerGroupData.purpose()));
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.group.id", playerGroupData.id()).setStyle(Style.EMPTY.withColor(0x00AAAA)));
        context.getSource().sendMessage(Text.translatable("peca.info.command.player.group.data.info").setStyle(Style.EMPTY.withColor(0xFF5555)));
        context.getSource().sendMessage(TextClickUtil.runText(Text.translatable("peca.info.command.player.group.spawn"), String.format("/playerGroup id %s spawn", playerGroupData.id()))
                .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.group.execute"), String.format("/playerGroup id %s execute", playerGroupData.id())))
                .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.group.stop"), String.format("/playerGroup %s stop", playerGroupData.name())))
                .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.group.kill"), String.format("/playerGroup %s kill", playerGroupData.name())))
                .append(TextClickUtil.runText(Text.translatable("peca.info.command.player.group.delete"), String.format("/playerGroup id %s delete", playerGroupData.id()))));
    }

    private static int infoId(CommandContext<ServerCommandSource> context) {
        printInfo(context, PlayerGroupSql.readPlayerGroup(LongArgumentType.getLong(context, "id")));
        return Command.SINGLE_SUCCESS;
    }

    private static int addPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerGroup playerGroup = PlayerGroup.getGroup(StringArgumentType.getString(context, "name"));
        if (playerGroup == null){
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.group.find.empty"));
            return -1;
        }

        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        if (!(player instanceof EntityPlayerMPFake fakePlayer)){
            context.getSource().sendMessage(Text.translatable("peca.info.command.error.player.group.add.not.fake.player"));
            return -1;
        }

        playerGroup.add(fakePlayer);
        return Command.SINGLE_SUCCESS;
    }

    private static int delPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerGroup playerGroup = PlayerGroup.getGroup(StringArgumentType.getString(context, "name"));
        if (playerGroup == null) {
            context.getSource().sendMessage(Text.translatable("peca.info.command.player.group.find.empty"));
            return -1;
        }

        playerGroup.del((EntityPlayerMPFake) EntityArgumentType.getPlayer(context, "player"));
        return Command.SINGLE_SUCCESS;
    }


    private static void setExecute(CommandContext<ServerCommandSource> context, PlayerGroupSql.Execute execute) {
        long id = LongArgumentType.getLong(context, "id");
        booleanPrintMsg(
                PlayerGroupSql.updatePlayerExecute(
                        id,
                        execute,
                        CommandUtil.getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "index") - 1, -1),
                        CommandUtil.getArgOrDefault(() -> StringArgumentType.getString(context, "name"), null)
                ),
                Text.translatable("peca.info.command.player.group.execute.update"),
                Text.translatable("peca.info.command.error.player.group.execute.update"),
                context
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        setExecute(context, (executeArray, playerName) -> {
            executeArray.forEach(command -> {
                context.getSource().getServer().getCommandManager().executeWithPrefix(context.getSource(), command.getAsString());
            });
            return executeArray;
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int executeAdd(CommandContext<ServerCommandSource> context) {
        String command = StringArgumentType.getString(context, "command");
        if (!command.startsWith("/player")) {
            context.getSource().sendError(Text.translatable("peca.info.command.error.execute.add.starts"));
            return -1;
        }

        setExecute(context, (executeArray, playerName) -> {
            executeArray.add(command.replace("%s", playerName));
            return executeArray;
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int executeDel(CommandContext<ServerCommandSource> context) {
        int index = IntegerArgumentType.getInteger(context, "commandIndex") - 1;
        setExecute(context, (executeArray, playerName) -> {
            if (index > executeArray.size()) {
                return executeArray;
            }
            executeArray.remove(index);
            return executeArray;
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int executeSet(CommandContext<ServerCommandSource> context) {
        int index = IntegerArgumentType.getInteger(context, "commandIndex") - 1;
        String command = StringArgumentType.getString(context, "command");
        if (!command.startsWith("/player")) {
            context.getSource().sendError(Text.translatable("peca.info.command.error.execute.add.starts"));
            return -1;
        }

        setExecute(context, (executeArray, playerName) -> {
            if (index > executeArray.size()) {
                return executeArray;
            }
            executeArray.set(index, new JsonPrimitive(command.replace("%s", playerName)));
            return executeArray;
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int executeClear(CommandContext<ServerCommandSource> context) {
        setExecute(context, (executeArray, playerName) -> new JsonArray());
        return Command.SINGLE_SUCCESS;
    }

}
