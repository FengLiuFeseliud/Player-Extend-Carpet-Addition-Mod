package fengliu.peca.player.sql;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import fengliu.peca.player.PlayerGroup;
import net.minecraft.server.MinecraftServer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 假人组数据
 *
 * @param id                     数据库索引, 只有在数据库单独保存才会存在, 不存在为 -1
 * @param name                   假人组名
 * @param players                假人组成员
 * @param botCount               假人组成员数
 * @param purpose                保存理由, 只有在数据库单独保存才会存在, 不存在为 null
 * @param createTime             创建时间, 只有在数据库单独保存才会存在, 不存在为 null
 * @param createPlayerUuid       创建用户游戏 uuid, 只有在数据库单独保存才会存在, 不存在为 null
 * @param lastModifiedTime       最后修改时间, 只有在数据库单独保存才会存在, 不存在为 null
 * @param lastModifiedPlayerUuid 最后修改用户游戏 uuid, 只有在数据库单独保存才会存在, 不存在为 null
 */
public record PlayerGroupData(
        long id,
        String name,
        List<PlayerData> players,
        int botCount,
        String purpose,
        Timestamp createTime,
        UUID createPlayerUuid,
        Timestamp lastModifiedTime,
        UUID lastModifiedPlayerUuid
) {

    /**
     * 从查询结果表转假人组数据列表
     *
     * @param result 查询结果
     * @return 假人组数据列表
     * @throws SQLException sql 错误
     */
    public static List<PlayerGroupData> fromResultSet(ResultSet result) throws SQLException {
        List<PlayerGroupData> playerGroupDataList = new ArrayList<>();
        while (result.next()) {
            List<PlayerData> playerDates = new ArrayList<>();
            JsonParser.parseString(result.getString("PLAYERS")).getAsJsonArray().forEach(playerDate -> {
                playerDates.add(PlayerData.fromJson(playerDate.getAsJsonObject()));
            });
            playerGroupDataList.add(
                    new PlayerGroupData(
                            result.getLong("ID"),
                            result.getString("GROUP_NAME"),
                            playerDates,
                            result.getInt("BOT_COUNT"),
                            result.getString("PURPOSE"),
                            Timestamp.valueOf(result.getString("CREATE_TIME")),
                            UUID.fromString(result.getString("CREATE_PLAYER_UUID")),
                            Timestamp.valueOf(result.getString("LAST_MODIFIED_TIME")),
                            UUID.fromString(result.getString("LAST_MODIFIED_PLAYER_UUID"))
                    )
            );
        }
        return playerGroupDataList;
    }

    /**
     * 检查假人是否在假人组中存在
     *
     * @param fakePlayer 假人
     * @return 存在返回 true
     */
    public boolean isInPlayer(EntityPlayerMPFake fakePlayer) {
        for (PlayerData data : this.players) {
            if (data.name().equals(fakePlayer.getEntityName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 假人组成员转 JSON 数组
     *
     * @return
     */
    public JsonArray playersToJsonArray() {
        JsonArray players = new JsonArray();
        this.players.forEach(playerData -> {
            players.add(playerData.toJson());
        });
        return players;
    }

    /**
     * 以该假人组数据创建假人组
     * @param server 服务器实例
     */
    public void spawn(MinecraftServer server){
        PlayerGroup.createGroup(this, server);
    }
}
