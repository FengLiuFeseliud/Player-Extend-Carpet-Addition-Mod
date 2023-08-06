package fengliu.peca.player.sql;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

    /**
     * 保存在表 PLAYER_GROUP 内
     */
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

    public static void createTable() {
        connection.createTable();
    }

    /**
     * 从表保存假人组
     *
     * @param playerGroup  假人组
     * @param createPlayer 创建保存的用户
     * @param purpose      保存理由
     * @return 成功 true
     */
    public static boolean saveGroup(IPlayerGroup playerGroup, ServerPlayerEntity createPlayer, String purpose) {
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

    /**
     * 从表删除假人组
     *
     * @param id 假人组自增 id
     * @return 成功 true
     */
    public static boolean deleteGroup(long id) {
        return (boolean) connection.executeSpl(statement -> {
            statement.execute(String.format("DELETE FROM %s WHERE ID=%s", connection.getTableName(), id));
            return true;
        });
    }

    /**
     * 生成假人组
     *
     * @param id     假人组自增 id
     * @param server 服务器实例
     * @return 成功 true
     */
    public static boolean spawnGroup(long id, MinecraftServer server) {
        return (boolean) connection.executeSpl(statement -> {
            PlayerGroupData playerGroupData = PlayerGroupData.fromResultSet(statement.executeQuery(String.format("SELECT * FROM %s WHERE ID=%s", connection.getTableName(), id))).get(0);
            if (PlayerGroup.getGroup(playerGroupData.name()) != null) {
                return false;
            }
            playerGroupData.spawn(server);
            return true;
        });
    }

    /**
     * 从表向已保存假人组添加假人
     *
     * @param id     假人组自增 id
     * @param player 假人
     * @return 成功 true
     */
    public static boolean addPlayer(long id, EntityPlayerMPFake player) {
        PlayerGroupData playerGroupData = readPlayerGroup(id);
        if (playerGroupData == null) {
            return false;
        }

        if (playerGroupData.isInPlayer(player)) {
            return false;
        }

        JsonArray playersData = playerGroupData.playersToJsonArray();
        playersData.add(PlayerData.fromPlayer(player).toJson());

        return (boolean) connection.executeSpl(statement -> {
            statement.execute(String.format("UPDATE %s SET PLAYERS='%s', BOT_COUNT=%s WHERE ID=%S", connection.getTableName(), playersData, playersData.size(), id));
            return true;
        });
    }

    /**
     * 从表向已保存假人组删除假人
     *
     * @param id     假人组自增 id
     * @param player 假人
     * @return 成功 true
     */
    public static boolean delPlayer(long id, EntityPlayerMPFake player) {
        PlayerGroupData playerGroupData = readPlayerGroup(id);
        if (playerGroupData == null) {
            return false;
        }

        JsonArray playersData = playerGroupData.playersToJsonArray();
        for (int index = 0; index < playersData.size(); index++) {
            if (!playersData.get(index).getAsJsonObject().get("name").getAsString().equals(player.getName().getString())) {
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

    public interface Execute {
        JsonArray run(JsonArray executeArray, String playerName);
    }

    /**
     * 从表向已保存假人组更新假人操作
     *
     * @param id      假人组自增 id
     * @param execute 更新假人操作
     * @param index   假人组成员索引, 全部更新使用 -1
     * @param name    假人组成员名, 全部更新使用 null
     * @return 成功 true
     */
    public static boolean updatePlayerExecute(long id, Execute execute, int index, @Nullable String name) {
        PlayerGroupData playerGroupData = readPlayerGroup(id);
        if (playerGroupData == null) {
            return false;
        }

        JsonArray players = playerGroupData.playersToJsonArray();
        if (index >= 0) {
            if (index >= players.size()) {
                return false;
            }
            players.get(index).getAsJsonObject().add("execute", execute.run(players.get(index).getAsJsonObject().get("execute").getAsJsonArray(), players.get(index).getAsJsonObject().get("name").getAsString()));
        } else if (name == null) {
            for (JsonElement data : players) {
                data.getAsJsonObject().add("execute", execute.run(data.getAsJsonObject().get("execute").getAsJsonArray(), data.getAsJsonObject().get("name").getAsString()));
            }
        } else {
            for (JsonElement data : players) {
                if (!data.getAsJsonObject().get("name").getAsString().equals(name)) {
                    continue;
                }
                data.getAsJsonObject().add("execute", execute.run(data.getAsJsonObject().get("execute").getAsJsonArray(), data.getAsJsonObject().get("name").getAsString()));
            }
        }

        return (boolean) connection.executeSpl(statement -> {
            statement.execute(String.format("UPDATE %s SET PLAYERS='%s' WHERE ID=%S", connection.getTableName(), players, id));
            return true;
        });
    }

    /**
     * 查询假人组
     *
     * @param id        假人组自增 id, -1 不查询假人组自增 id
     * @param groupName 假人组名, null 不查询假人组名
     * @return 假人组列表
     */
    private static List<PlayerGroupData> readPlayerGroup(long id, @Nullable String groupName) {
        SqlUtil.BuildSqlHelper sqlHelper = new SqlUtil.BuildSqlHelper(String.format("SELECT * FROM %s", connection.getTableName()));
        if (groupName != null) {
            sqlHelper.like("GROUP_NAME", "'%" + groupName + "%'");
        }

        if (id != -1) {
            sqlHelper.and(String.format("ID=%s", id));
        }

        Object sqlData = connection.executeSpl(statement -> PlayerGroupData.fromResultSet(statement.executeQuery(sqlHelper.build())));
        if (!(sqlData instanceof List<?> dataList)) {
            return new ArrayList<>();
        }

        List<PlayerGroupData> PlayerDataList = new ArrayList<>();
        dataList.forEach(data -> {
            if (data instanceof PlayerGroupData) {
                PlayerDataList.add((PlayerGroupData) data);
            }
        });
        return PlayerDataList;
    }

    /**
     * 指令查询假人组
     *
     * @param context 指令上下文
     * @return 假人组列表
     */
    public static List<PlayerGroupData> readPlayerGroup(CommandContext<ServerCommandSource> context) {
        return readPlayerGroup(
                -1,
                CommandUtil.getArgOrDefault(() -> StringArgumentType.getString(context, "name"), null)
        );
    }

    /**
     * 假人组自增 id 查询假人组
     *
     * @param id 假人组自增 id
     * @return 假人组
     */
    public static PlayerGroupData readPlayerGroup(long id){
        return readPlayerGroup(id, null).get(0);
    }
}
