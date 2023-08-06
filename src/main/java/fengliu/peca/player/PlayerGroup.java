package fengliu.peca.player;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fengliu.peca.player.sql.PlayerData;
import fengliu.peca.player.sql.PlayerGroupData;
import fengliu.peca.player.sql.PlayerGroupSql;
import fengliu.peca.util.CommandUtil;
import fengliu.peca.util.PlayerUtil;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;

import static fengliu.peca.util.CommandUtil.getArgOrDefault;

/**
 * 假人组
 */
public class PlayerGroup implements IPlayerGroup {
    private long id = -1;
    public static final List<PlayerGroup> groups = new ArrayList<>();
    public final String groupName;
    protected int groupAmount = 0;
    private final List<EntityPlayerMPFake> bots = new ArrayList<>();

    public PlayerGroup(CommandContext<ServerCommandSource> context, Vec3d[] formationPos){
        this.groupName = StringArgumentType.getString(context, "name");
        if (!PlayerUtil.canSpawnGroup(this.groupName, context)){
            context.getSource().sendError(Text.translatable("peca.info.command.error.create.player.group"));
            return;
        }

        PlayerGroup.groups.add(this);
        int botAmount = CommandUtil.getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "amount"), -1);
        if (botAmount == -1){
            return;
        }

        GameMode mode = getArgOrDefault(() -> GameModeArgumentType.getGameMode(context, "gamemode"), null);
        if (formationPos == null){
            Vec3d pos = getArgOrDefault(() -> Vec3ArgumentType.getVec3(context, ""), context.getSource().getPosition());
            for (int index = 1; index < IntegerArgumentType.getInteger(context, "amount") + 1; index++){
                EntityPlayerMPFake player = PlayerUtil.spawn(this.groupName + "_" + index, pos, mode, context);
                if (player == null){
                    continue;
                }
                this.add(player);
            }
            return;
        }

        for (int index = 1; index < botAmount + 1; index++){
            EntityPlayerMPFake player = PlayerUtil.spawn(this.groupName + "_" + index, formationPos[index-1], mode, context);
            if (player == null){
                continue;
            }
            this.add(player);
        }
    }

    public PlayerGroup(PlayerGroupData playerGroupData, MinecraftServer server) {
        this.groupName = playerGroupData.name();
        this.id = playerGroupData.id();
        groups.add(this);

        playerGroupData.players().forEach(playerData -> {
            this.bots.add(playerData.spawn(server));
        });
    }

    /**
     * 创建假人组
     *
     * @param playerGroupData 假人组数据
     * @param server          服务器实例
     */
    public static void createGroup(PlayerGroupData playerGroupData, MinecraftServer server) {
        new PlayerGroup(playerGroupData, server);
    }

    /**
     * 指令创建假人组
     *
     * @param context      指令上下文
     * @param formationPos 假人成员坐标
     * @return Command.SINGLE_SUCCESS
     */
    public static int createGroup(CommandContext<ServerCommandSource> context, Vec3d[] formationPos) {
        new PlayerGroup(context, formationPos);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 指令创建空假人组
     *
     * @param context 指令上下文
     * @return Command.SINGLE_SUCCESS
     */
    public static int createGroup(CommandContext<ServerCommandSource> context) {
        new PlayerGroup(context, null);
        return Command.SINGLE_SUCCESS;
    }

    public static PlayerGroup getGroup(String groupName) {
        for (PlayerGroup group : groups) {
            if (!group.getName().equals(groupName)) {
                continue;
            }
            return group;
        }
        return null;
    }

    @Override
    public void add(EntityPlayerMPFake bot) {
        if (bot == null){
            return;
        }

        if (id != -1){
            PlayerGroupSql.addPlayer(id, bot);
        }

        this.groupAmount++;
        IPlayerGroup.super.add(bot);
    }

    @Override
    public EntityPlayerMPFake del(EntityPlayerMPFake player) {
        EntityPlayerMPFake fakePlayer = IPlayerGroup.super.del(player);
        if (fakePlayer == null){
            return null;
        }

        if (this.id == -1){
            return fakePlayer;
        }

        PlayerGroupSql.delPlayer(this.id, player);
        return fakePlayer;
    }

    @Override
    public List<EntityPlayerMPFake> getBots() {
        return this.bots;
    }

    @Override
    public void kill() {
        IPlayerGroup.super.kill();
        groups.remove(this);
    }

    @Override
    public String getName() {
        return this.groupName;
    }

    @Override
    public int getAmount() {
        return this.groupAmount;
    }

    /**
     * 假人组队形
     */
    public enum FormationType {

        /**
         * 列
         */
        COLUMN("column", (amount, pos, direction, row, interstice, formationPos) -> {
            for (int index = 0; index < amount; index++) {
                formationPos[index] = pos.offset(direction, addInterstice(index, interstice));
            }
            return formationPos;
        }),

        /**
         * 可叠加列
         */
        COLUMN_FOLD("columnFold", (amount, pos, direction, row, interstice, formationPos) -> {
            int rowIndex = 0;
            int rowSize = (int) Math.ceil((double) (amount / row));
            for (int index = 0; index < amount; index++) {
                if (index % rowSize == 0) {
                    rowIndex = 0;
                }
                formationPos[index] = pos.offset(direction, addInterstice(rowIndex, interstice));
                rowIndex++;
            }
            return formationPos;
        }),

        /**
         * 行
         */
        ROW("row", ((amount, pos, direction, row, interstice, formationPos) -> {
            Direction rowDirection = getRowDirection(direction);
            for (int index = 0; index < amount; index++) {
                formationPos[index] = pos.offset(rowDirection, addInterstice(index, interstice));
            }
            return formationPos;
        })),

        /**
         * 可叠加行
         */
        ROW_FOLD("rowFold", ((amount, pos, direction, row, interstice, formationPos) -> {
            int rowIndex = 0;
            int rowSize = (int) Math.ceil((double) (amount / row));
            Direction rowDirection = getRowDirection(direction);
            for (int index = 0; index < amount; index++) {
                if (index % rowSize == 0) {
                    rowIndex = 0;
                }
                formationPos[index] = pos.offset(rowDirection, addInterstice(rowIndex, interstice));
                rowIndex++;
            }
            return formationPos;
        })),

        /**
         * 四边形
         */
        QUADRANGLE("quadrangle", (amount, pos, direction, row, interstice, formationPos) -> {
            int rowIn = 0;
            int rowIndex = 0;
            int rowSize = (int) Math.ceil((double) (amount / row));
            Direction rowDirection = getRowDirection(direction);
            for (int index = 0; index < amount; index++) {
                if (index % rowSize == 0) {
                    rowIn++;
                    rowIndex = 0;
                }

                formationPos[index] = pos.offset(rowDirection, addInterstice(rowIndex, interstice)).offset(direction, addInterstice(rowIn, interstice));
                rowIndex++;
            }
            return formationPos;
        });

        private static Direction getRowDirection(Direction direction){
            if (direction == Direction.NORTH){
                return Direction.EAST;
            } else if (direction == Direction.WEST){
                return Direction.NORTH;
            } else if (direction == Direction.EAST) {
                return Direction.SOUTH;
            } else {
                return Direction.WEST;
            }
        }

        private static int addInterstice(int index, int interstice) {
            return index + (interstice * index);
        }

        /**
         * 队形计算
         */
        private interface Formation {

            /**
             * 计算队形坐标, 返回队形坐标数组
             *
             * @param amount       队形成员数
             * @param pos          起始坐标
             * @param direction    方向
             * @param row          行数
             * @param interstice   间隔
             * @param formationPos 队形坐标数组
             * @return 队形坐标数组
             */
            Vec3d[] get(int amount, Vec3d pos, Direction direction, int row, int interstice, Vec3d[] formationPos);
        }

        public final String name;
        private final Formation formation;

        /**
         * 假人组队形
         *
         * @param name      队形名
         * @param formation 队形计算
         */
        FormationType(String name, Formation formation) {
            this.name = name;
            this.formation = formation;
        }

        /**
         * 获取队形坐标数组
         *
         * @param context   指令上下文
         * @param direction 队形方向
         * @return 队形坐标数组
         */
        public Vec3d[] getFormationPos(CommandContext<ServerCommandSource> context, Direction direction) {
            int amount = IntegerArgumentType.getInteger(context, "amount");
            return this.formation.get(amount,
                    getArgOrDefault(() -> Vec3ArgumentType.getVec3(context, "position"), context.getSource().getPosition()),
                    direction,
                    getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "row"), 0),
                    getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "length"), 0),
                    new Vec3d[amount]);
        }

        /**
         * 获取当前用户方向队形坐标数组
         *
         * @param context 指令上下文
         * @return 队形方向
         */
        public Vec3d[] getFormationPos(CommandContext<ServerCommandSource> context) {
            PlayerEntity player = context.getSource().getPlayer();
            if (player == null) {
                return null;
            }

            return this.getFormationPos(context, Direction.getLookDirectionForAxis(player, Direction.Axis.Z));
        }
    }
}
