package fengliu.peca.player.sql;

import com.google.gson.JsonArray;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fengliu.peca.util.CommandUtil;
import fengliu.peca.util.sql.ISqlConnection;
import fengliu.peca.util.sql.SqlUtil;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.GameModeArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static fengliu.peca.PecaMod.dbUrl;

public class PlayerSql {
    public static final PlayerSqlConnection connection = new PlayerSqlConnection();

    /**
     * 保存在表 PLAYER 内
     */
    static class PlayerSqlConnection implements ISqlConnection {

        @Override
        public String getDBUrl() {
            return dbUrl;
        }

        @Override
        public String getTableName() {
            return "PLAYER";
        }

        @Override
        public String getCreateTableSql() {
            return String.format("""
                                       CREATE TABLE "%s" (
                                            "ID"	INTEGER NOT NULL UNIQUE,
                                            "NAME"	TEXT NOT NULL,
                                            "DIMENSION"	TEXT NOT NULL,
                                            "X"	REAL NOT NULL,
                                            "Y"	REAL NOT NULL,
                                            "Z"	REAL NOT NULL,
                                            "FACING_X"	REAL NOT NULL,
                                            "FACING_Y"	REAL NOT NULL,
                                            "GAME_MODE"	INTEGER NOT NULL,
                                            "FLYING"	BLOB NOT NULL,
                                            "EXECUTE"	TEXT NOT NULL DEFAULT '[]',
                                            "PURPOSE"	TEXT NOT NULL,
                                            "CREATE_TIME"	INTEGER NOT NULL UNIQUE,
                                            "CREATE_PLAYER_UUID"	TEXT NOT NULL,
                                            "LAST_MODIFIED_TIME"	INTEGER NOT NULL,
                                            "LAST_MODIFIED_PLAYER_UUID"	TEXT NOT NULL,
                                            PRIMARY KEY("ID" AUTOINCREMENT)
                                      );
                    """, this.getTableName());
        }
    }

    public static void createTable() {
        connection.createTable();
    }

    /**
     * 从表保存假人
     *
     * @param data         假人数据
     * @param createPlayer 保存用户玩家
     * @param purpose      保存理由
     * @return 成功 true
     */
    public static boolean savePlayer(PlayerData data, ServerPlayerEntity createPlayer, String purpose) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        UUID uuid = createPlayer.getUuid();

        return (boolean) connection.executeSpl(statement -> {
            statement.execute(String.format("""
                                INSERT INTO %s (NAME, DIMENSION, X, Y, Z, FACING_X, FACING_Y, GAME_MODE, FLYING, PURPOSE, CREATE_TIME, CREATE_PLAYER_UUID, LAST_MODIFIED_TIME, LAST_MODIFIED_PLAYER_UUID)
                                VALUES ('%s', '%s', %g, %g, %g, %g, %g, '%s', '%s', '%s', '%s', '%s', '%s', '%s');
                    """, connection.getTableName(), data.name(), data.dimension(), data.pos().x, data.pos().y, data.pos().z, data.pitch(), data.yaw(), data.gamemode().getId(), data.flying(), purpose, timestamp, uuid, timestamp, uuid));
            return true;
        });
    }

    /**
     * 从表保存假人
     *
     * @param player       假人
     * @param createPlayer 保存用户玩家
     * @param purpose      保存理由
     * @return 成功 true
     */
    public static boolean savePlayer(ServerPlayerEntity player, ServerPlayerEntity createPlayer, String purpose) {
        return savePlayer(PlayerData.fromPlayer(player), createPlayer, purpose);
    }

    /**
     * 从表删除假人
     *
     * @param id 假人自增 id
     * @return 成功 true
     */
    public static boolean deletePlayer(long id) {
        return (boolean) connection.executeSpl(statement -> {
            statement.execute(String.format("DELETE FROM %s WHERE ID=%s", connection.getTableName(), id));
            return true;
        });
    }

    /**
     * 从表查询假人
     *
     * @param id           假人自增 id, -1 不查询假人自增 id
     * @param name         假人名, null 不查询假人名
     * @param mode         假人游戏模式, null 不查询游戏模式
     * @param pos          假人坐标, null 不查询坐标
     * @param offset       假人坐标范围偏移量
     * @param dimensionKey 假人维度, null 不查询维度
     * @return 假人列表
     */
    private static List<PlayerData> readPlayer(long id, @Nullable String name, @Nullable GameMode mode, @Nullable Vec3d pos, @Nullable Integer offset, @Nullable RegistryKey<DimensionType> dimensionKey) {
        SqlUtil.BuildSqlHelper sqlHelper = new SqlUtil.BuildSqlHelper(String.format("SELECT * FROM %s", connection.getTableName()));
        if (name != null) {
            sqlHelper.like("NAME", "'%" + name + "%'");
        }

        if (mode != null) {
            sqlHelper.and("GAME_MODE=" + mode.getId());
        }

        if (pos != null){
            if (offset == null){
                offset = 1;
            }
            sqlHelper.and(String.format("%s-%s<=X", pos.x, offset))
                    .and(String.format("X<=%s+%s", pos.x, offset))
                    .and(String.format("%s-%s<=Y", pos.y, offset))
                    .and(String.format("Y<=%s+%s", pos.y, offset))
                    .and(String.format("%s-%s<=Z", pos.z, offset))
                    .and(String.format("Z<=%s+%s", pos.z, offset));
        }

        if (dimensionKey != null){
            sqlHelper.and(String.format("DIMENSION='%s'", dimensionKey.getValue()));
        }

        if (id != -1){
            sqlHelper.and(String.format("ID='%s'", id));
        }

        Object sqlData = connection.executeSpl(statement -> PlayerData.fromResultSet(statement.executeQuery(sqlHelper.build())));
        if (!(sqlData instanceof List<?> dataList)) {
            return new ArrayList<>();
        }

        List<PlayerData> PlayerDataList = new ArrayList<>();
        dataList.forEach(data -> {
            if (data instanceof PlayerData) {
                PlayerDataList.add((PlayerData) data);
            }
        });
        return PlayerDataList;
    }

    /**
     * 指令查询假人
     *
     * @param context 指令上下文
     * @return 假人列表
     */
    public static List<PlayerData> readPlayer(CommandContext<ServerCommandSource> context) {
        ServerWorld world = CommandUtil.getArgOrDefault(() -> DimensionArgumentType.getDimensionArgument(context, "dimension"), null);
        RegistryKey<DimensionType> dimensionKey = null;
        if (world != null) {
            dimensionKey = world.getDimensionKey();
        }

        return readPlayer(
                -1,
                CommandUtil.getArgOrDefault(() -> StringArgumentType.getString(context, "name"), null),
                CommandUtil.getArgOrDefault(() -> GameModeArgumentType.getGameMode(context, "gamemode"), null),
                CommandUtil.getArgOrDefault(() -> Vec3ArgumentType.getVec3(context, "pos"), null),
                CommandUtil.getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "offset"), null),
                dimensionKey
        );
    }

    /**
     * 假人自增 id 查询假人
     *
     * @param id 假人自增 id
     * @return 假人
     */
    public static PlayerData readPlayer(long id) {
        return readPlayer(id, null, null, null, null, null).get(0);
    }

    /**
     * 从表向已保存假人更新假人操作
     *
     * @param id      假人自增 id
     * @param execute 假人操作
     * @return 成功 true
     */
    public static boolean executeUpdate(long id, JsonArray execute) {
        return (boolean) connection.executeSpl(statement -> {
            statement.execute(String.format("UPDATE %s SET EXECUTE='%s' WHERE ID=%s", connection.getTableName(), execute, id));
            return true;
        });
    }
}
