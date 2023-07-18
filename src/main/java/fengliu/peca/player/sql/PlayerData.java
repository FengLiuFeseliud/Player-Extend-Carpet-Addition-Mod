package fengliu.peca.player.sql;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fengliu.peca.util.CommandUtil;
import fengliu.peca.util.PlayerUtil;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PlayerData(
        long id,
        String name,
        Identifier dimension,
        Vec3d pos,
        double yaw,
        double pitch,
        GameMode gamemode,
        boolean flying,
        JsonArray execute,
        String purpose,
        Timestamp createTime,
        UUID createPlayerUuid,
        Timestamp lastModifiedTime,
        UUID lastModifiedPlayerUuid
)
{
    public static List<PlayerData> fromResultSet(ResultSet result) throws SQLException {
        List<PlayerData> playerDates = new ArrayList<>();
        while (result.next()){
            playerDates.add(new PlayerData(
                    result.getLong("ID"),
                    result.getString("NAME"),
                    Identifier.tryParse(result.getString("DIMENSION")),
                    new Vec3d(result.getDouble("X"), result.getDouble("Y"), result.getDouble("Z")),
                    result.getDouble("FACING_Y"),
                    result.getDouble("FACING_X"),
                    GameMode.getOrNull(result.getInt("GAME_MODE")),
                    result.getBoolean("FLYING"),
                    JsonParser.parseString(result.getString("EXECUTE")).getAsJsonArray(),
                    result.getString("PURPOSE"),
                    Timestamp.valueOf(result.getString("CREATE_TIME")),
                    UUID.fromString(result.getString("CREATE_PLAYER_UUID")),
                    Timestamp.valueOf(result.getString("LAST_MODIFIED_TIME")),
                    UUID.fromString(result.getString("LAST_MODIFIED_PLAYER_UUID"))
            ));
        }
        return playerDates;
    }

    public static PlayerData fromPlayer(ServerPlayerEntity player){
        return new PlayerData (
                -1 ,
                player.getEntityName(),
                player.getWorld().getDimensionKey().getValue(),
                player.getPos(),
                player.getYaw(),
                player.getPitch(),
                player.interactionManager.getGameMode(),
                player.getAbilities().flying,
                JsonParser.parseString("[]").getAsJsonArray(),
                null,
                null,
                null,
                null,
                null
        );
    }

    public static PlayerData fromJson(JsonObject playerJson){
        JsonObject pos = playerJson.getAsJsonObject("pos");
        return new PlayerData(
                playerJson.get("id").getAsLong(),
                playerJson.get("name").getAsString(),
                Identifier.tryParse(playerJson.get("dimension").getAsString()),
                new Vec3d(pos.get("x").getAsDouble(), pos.get("y").getAsDouble(), pos.get("z").getAsDouble()),
                playerJson.get("yaw").getAsDouble(),
                playerJson.get("pitch").getAsDouble(),
                GameMode.getOrNull(playerJson.get("gamemode").getAsInt()),
                playerJson.get("flying").getAsBoolean(),
                playerJson.getAsJsonArray("execute"),
                CommandUtil.getArgOrDefault(() -> playerJson.get("purpose").getAsString(), null),
                CommandUtil.getArgOrDefault(() -> Timestamp.valueOf(playerJson.get("createTime").getAsString()), null),
                CommandUtil.getArgOrDefault(() -> UUID.fromString(playerJson.get("createPlayerUuid").getAsString()), null),
                CommandUtil.getArgOrDefault(() -> Timestamp.valueOf(playerJson.get("lastModifiedTime").getAsString()), null),
                CommandUtil.getArgOrDefault(() -> UUID.fromString(playerJson.get("lastModifiedPlayerUuid").getAsString()), null)
        );
    }

    public static JsonObject toJson(PlayerData playerData){
        JsonObject playerJson = new JsonObject();
        playerJson.addProperty("id", playerData.id);
        playerJson.addProperty("name", playerData.name);
        playerJson.addProperty("dimension", playerData.dimension.toString());

        JsonObject pos = new JsonObject();
        pos.addProperty("x", playerData.pos.x);
        pos.addProperty("y", playerData.pos.y);
        pos.addProperty("z", playerData.pos.z);
        playerJson.add("pos", pos);
        playerJson.addProperty("yaw", playerData.yaw);
        playerJson.addProperty("pitch", playerData.pitch);
        playerJson.addProperty("gamemode", playerData.gamemode.getId());
        playerJson.addProperty("flying", playerData.flying);
        playerJson.add("execute", playerData.execute);
        playerJson.addProperty("purpose", playerData.purpose);
        if (playerData.createTime != null){
            playerJson.addProperty("createTime", playerData.createTime.toString());
        } else {
            playerJson.add("createTime", null);
        }

        if (playerData.createPlayerUuid != null){
            playerJson.addProperty("createPlayerUuid", playerData.createPlayerUuid.toString());
        } else {
            playerJson.add("createPlayerUuid", null);
        }

        if (playerData.lastModifiedTime != null){
            playerJson.addProperty("lastModifiedTime", playerData.lastModifiedTime.toString());
        } else {
            playerJson.add("lastModifiedTime", null);
        }
        if (playerData.lastModifiedPlayerUuid != null){
            playerJson.addProperty("lastModifiedPlayerUuid", playerData.lastModifiedPlayerUuid.toString());
        } else {
            playerJson.add("lastModifiedPlayerUuid", null);
        }
        return playerJson;
    }

    public EntityPlayerMPFake spawn(MinecraftServer server){
        if (!PlayerUtil.canSpawn(this.name, server.getPlayerManager())){
            return null;
        }
       return EntityPlayerMPFake.createFake(this.name, server, this.pos, this.yaw, this.pitch, RegistryKey.of(RegistryKeys.WORLD, this.dimension), this.gamemode, this.flying);
    }
}
