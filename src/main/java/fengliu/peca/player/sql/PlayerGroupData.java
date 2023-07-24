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

    public static List<PlayerGroupData> fromResultSet(ResultSet result) throws SQLException {
        List<PlayerGroupData> playerGroupDataList = new ArrayList<>();
        while (result.next()){
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

    public boolean isInPlayer(EntityPlayerMPFake fakePlayer){
        for (PlayerData data: this.players){
            if (data.name().equals(fakePlayer.getEntityName())){
                return true;
            }
        }
        return false;
    }

    public JsonArray playersToJsonArray(){
        JsonArray players = new JsonArray();
        this.players.forEach(playerData -> {
            players.add(playerData.toJson());
        });
        return players;
    }

    public void spawn(MinecraftServer server){
        PlayerGroup.createGroup(this, server);
    }
}
