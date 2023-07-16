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
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
            return """
                       CREATE TABLE "PLAYER" (
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
            """;
        }
    }

    public static void createTable(){
        connection.createTable();
    }

    public static boolean savePlayer(PlayerData data, ServerPlayerEntity createPlayer, String purpose){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        UUID uuid = createPlayer.getUuid();

        return (boolean) connection.executeSpl(statement -> {
            statement.execute(String.format("""
                        INSERT INTO PLAYER (NAME, DIMENSION, X, Y, Z, FACING_X, FACING_Y, GAME_MODE, FLYING, PURPOSE, CREATE_TIME, CREATE_PLAYER_UUID, LAST_MODIFIED_TIME, LAST_MODIFIED_PLAYER_UUID)
                        VALUES ('%s', '%s', %g, %g, %g, %g, %g, '%s', '%s', '%s', '%s', '%s', '%s', '%s');
            """, data.name(), data.dimension(), data.pos().x, data.pos().y, data.pos().z, data.pitch(), data.yaw(), data.gamemode().getId(), data.flying(), purpose, timestamp, uuid, timestamp, uuid));
            return true;
        });
    }

    public static boolean savePlayer(ServerPlayerEntity player, ServerPlayerEntity createPlayer, String purpose){
        return savePlayer(PlayerData.fromPlayer(player), createPlayer, purpose);
    }

    public static boolean deletePlayer(long id){
        return (boolean) connection.executeSpl(statement -> {
            statement.execute("DELETE FROM PLAYER WHERE ID=" + id);
            return true;
        });
    }

    private static List<PlayerData> readPlayer(@Nullable String name, @Nullable GameMode mode, @Nullable Vec3d pos, @Nullable Integer offset, @Nullable RegistryKey<DimensionType> dimensionKey){
        SqlUtil.BuildSqlHelper sqlHelper = new SqlUtil.BuildSqlHelper("SELECT * FROM PLAYER");
        if (name != null){
            sqlHelper.like("NAME", "'%" + name +"%'");
        }

        if (mode != null){
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

        Object sqlData = connection.executeSpl(statement -> PlayerData.fromResultSet(statement.executeQuery(sqlHelper.build())));
        if (!(sqlData instanceof List<?> dataList)){
            return new ArrayList<>();
        }

        List<PlayerData> PlayerDataList = new ArrayList<>();
        dataList.forEach(data -> {
            if (data instanceof PlayerData){
                PlayerDataList.add((PlayerData) data);
            }
        });
        return PlayerDataList;
    }

    public static List<PlayerData> readPlayer(CommandContext<ServerCommandSource> context){
        ServerWorld world = CommandUtil.getArgOrDefault(() -> DimensionArgumentType.getDimensionArgument(context, "dimension"), null);
        RegistryKey<DimensionType> dimensionKey = null;
        if (world != null){
            dimensionKey = world.getDimensionKey();
        }

        return readPlayer(
                CommandUtil.getArgOrDefault(() -> StringArgumentType.getString(context, "name"), null),
                CommandUtil.getArgOrDefault(() -> GameModeArgumentType.getGameMode(context, "gamemode"), null),
                CommandUtil.getArgOrDefault(() -> Vec3ArgumentType.getVec3(context, "pos"), null),
                CommandUtil.getArgOrDefault(() -> IntegerArgumentType.getInteger(context, "offset"), null),
                dimensionKey
        );
    }

    public static PlayerData readPlayer(long id){
        return (PlayerData) connection.executeSpl(statement -> PlayerData.fromResultSet(statement.executeQuery("SELECT * FROM PLAYER WHERE ID=" + id)).get(0));
    }

    public static boolean executeUpdate(long id, JsonArray execute){
        return (boolean) connection.executeSpl(statement -> {
            statement.execute(String.format("UPDATE PLAYER SET EXECUTE='%s' WHERE ID=%s", execute, id));
            return true;
        });
    }
}
