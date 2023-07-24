package fengliu.peca.player.sql;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonArray;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fengliu.peca.PecaMod;
import fengliu.peca.player.IPlayerGroup;
import fengliu.peca.player.PlayerGroup;
import fengliu.peca.util.CommandUtil;
import fengliu.peca.util.sql.ISqlConnection;
import fengliu.peca.util.sql.SqlUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerGroupSql {
    private static final ISqlConnection connection = new PlayerGroupSqlConnection();

    private static class PlayerGroupSqlConnection implements ISqlConnection {

        @Override
        public String getDBUrl() {
            return PecaMod.dbUrl;
        }

        @Override
        public String getTableName() {
            return "PLAYER_GROUP";
        }

        @Override
        public String getCreateTableSql() {
            return String.format("""
                            CREATE TABLE "%s" (
                            	"ID"	INTEGER NOT NULL UNIQUE,
                            	"GROUP_NAME"	TEXT NOT NULL,
                            	"PLAYERS"	TEXT NOT NULL,
                            	"BOT_COUNT"	INTEGER NOT NULL,
                            	"PURPOSE"	TEXT NOT NULL,
                            	"CREATE_TIME"	TEXT NOT NULL,
                            	"CREATE_PLAYER_UUID"	TEXT NOT NULL,
                            	"LAST_MODIFIED_TIME"	TEXT NOT NULL,
                            	"LAST_MODIFIED_PLAYER_UUID"	TEXT NOT NULL,
                            	PRIMARY KEY("ID" AUTOINCREMENT)
                            );
            """, this.getTableName());
        }
    }

    public static void createTable(){
        connection.createTable();
    }

    public static boolean saveGroup(IPlayerGroup playerGroup, ServerPlayerEntity createPlayer, String purpose){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        UUID uuid = createPlayer.getUuid();
        JsonArray players = new JsonArray();

        playerGroup.getBots().forEach(bot -> {
            players.add(PlayerData.fromPlayer(bot).toJson());
        });
        return (boolean) connection.executeSpl(statement -> {
            statement.execute(String.format("""
                    INSERT INTO %s (GROUP_NAME, PLAYERS, BOT_COUNT, PURPOSE, CREATE_TIME, CREATE_PLAYER_UUID, LAST_MODIFIED_TIME, LAST_MODIFIED_PLAYER_UUID)
                    VALUES ('%s', '%s', %s, '%s', '%s', '%s', '%s', '%s');
            """, connection.getTableName(), playerGroup.getName(), players, playerGroup.getAmount(), purpose, timestamp, uuid, timestamp, uuid));
            return true;
        });
    }

    public static boolean deleteGroup(long id){
        return (boolean) connection.executeSpl(statement -> {
            statement.execute(String.format("DELETE FROM %s WHERE ID=%s", connection.getTableName(), id));
            return true;
        });
    }

    public static boolean spawnGroup(long id, MinecraftServer server){
        return (boolean) connection.executeSpl(statement -> {
            PlayerGroupData playerGroupData = PlayerGroupData.fromResultSet(statement.executeQuery(String.format("SELECT * FROM %s WHERE ID=%s", connection.getTableName(), id))).get(0);
            if (PlayerGroup.getGroup(playerGroupData.name()) != null){
                return false;
            }
            playerGroupData.spawn(server);
            return true;
        });
    }

    public static boolean addPlayer(long id, EntityPlayerMPFake player){
        PlayerGroupData playerGroupData = readPlayerGroup(id);
        if (playerGroupData == null){
            return false;
        }

        if (playerGroupData.isInPlayer(player)){
            return false;
        }

        JsonArray playersData = playerGroupData.playersToJsonArray();
        playersData.add(PlayerData.fromPlayer(player).toJson());

        return (boolean) connection.executeSpl(statement -> {
            statement.execute(String.format("UPDATE %s SET PLAYERS='%s', BOT_COUNT=%s WHERE ID=%S", connection.getTableName(), playersData, playersData.size(), id));
            return true;
        });
    }

    public static boolean delPlayer(long id, EntityPlayerMPFake player){
        PlayerGroupData playerGroupData = readPlayerGroup(id);
        if (playerGroupData == null){
            return false;
        }

        JsonArray playersData = playerGroupData.playersToJsonArray();
        for (int index = 0; index < playersData.size(); index++){
            if(!playersData.get(index).getAsJsonObject().get("name").getAsString().equals(player.getName().getString())){
                continue;
            }

            playersData.remove(index);
            return (boolean) connection.executeSpl(statement -> {
                statement.execute(String.format("UPDATE %s SET PLAYERS='%s', BOT_COUNT=%s WHERE ID=%S", connection.getTableName(), playersData, playersData.size(), id));
                return true;
            });
        }
        return false;
    }

    private static List<PlayerGroupData> readPlayerGroup(long id, @Nullable String groupName){
        SqlUtil.BuildSqlHelper sqlHelper = new SqlUtil.BuildSqlHelper(String.format("SELECT * FROM %s", connection.getTableName()));
        if (groupName != null){
            sqlHelper.like("GROUP_NAME", "'%" + groupName +"%'");
        }

        if (id != -1){
            sqlHelper.and(String.format("ID=%s", id));
        }

        Object sqlData = connection.executeSpl(statement -> PlayerGroupData.fromResultSet(statement.executeQuery(sqlHelper.build())));
        if (!(sqlData instanceof List<?> dataList)){
            return new ArrayList<>();
        }

        List<PlayerGroupData> PlayerDataList = new ArrayList<>();
        dataList.forEach(data -> {
            if (data instanceof PlayerGroupData){
                PlayerDataList.add((PlayerGroupData) data);
            }
        });
        return PlayerDataList;
    }

    public static List<PlayerGroupData> readPlayerGroup(CommandContext<ServerCommandSource> context){
        return readPlayerGroup(
                -1,
                CommandUtil.getArgOrDefault(() -> StringArgumentType.getString(context, "name"), null)
        );
    }

    public static PlayerGroupData readPlayerGroup(long id){
        return readPlayerGroup(id, null).get(0);
    }
}
