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

/**
 * 假人数据
 *
 * @param id                     数据库索引, 只有在数据库单独保存才会存在, 不存在为 -1
 * @param name                   假人名
 * @param dimension              维度 id
 * @param pos                    坐标
 * @param yaw                    视角
 * @param pitch                  视角
 * @param gamemode               游戏模式
 * @param flying                 是否飞行
 * @param execute                执行操作
 * @param purpose                保存理由, 只有在数据库单独保存才会存在, 不存在为 null
 * @param createTime             创建时间, 只有在数据库单独保存才会存在, 不存在为 null
 * @param createPlayerUuid       创建用户游戏 uuid, 只有在数据库单独保存才会存在, 不存在为 null
 * @param lastModifiedTime       最后修改时间, 只有在数据库单独保存才会存在, 不存在为 null
 * @param lastModifiedPlayerUuid 最后修改用户游戏 uuid, 只有在数据库单独保存才会存在, 不存在为 null
 */
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
) {

    /**
     * 从查询结果表转假人数据列表
     *
     * @param result 查询结果
     * @return 假人数据列表
     * @throws SQLException sql 错误
     */
    public static List<PlayerData> fromResultSet(ResultSet result) throws SQLException {
        List<PlayerData> playerDates = new ArrayList<>();
        while (result.next()) {
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

    /**
     * 从玩家转假人数据
     *
     * @param player 玩家
     * @return 假人数据
     */
    public static PlayerData fromPlayer(ServerPlayerEntity player) {
        return new PlayerData(
                -1,
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

    /**
     * 从 JSON 转假人数据
     *
     * @param playerJson 假人数据 JSON
     * @return 假人数据
     */
    public static PlayerData fromJson(JsonObject playerJson) {
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

    /**
     * 从假人数据转 JSON
     *
     * @return JSON
     */
    public JsonObject toJson() {
        JsonObject playerJson = new JsonObject();
        playerJson.addProperty("id", this.id);
        playerJson.addProperty("name", this.name);
        playerJson.addProperty("dimension", this.dimension.toString());

        JsonObject pos = new JsonObject();
        pos.addProperty("x", this.pos.x);
        pos.addProperty("y", this.pos.y);
        pos.addProperty("z", this.pos.z);
        playerJson.add("pos", pos);
        playerJson.addProperty("yaw", this.yaw);
        playerJson.addProperty("pitch", this.pitch);
        playerJson.addProperty("gamemode", this.gamemode.getId());
        playerJson.addProperty("flying", this.flying);
        playerJson.add("execute", this.execute);
        playerJson.addProperty("purpose", this.purpose);
        if (this.createTime != null){
            playerJson.addProperty("createTime", this.createTime.toString());
        } else {
            playerJson.add("createTime", null);
        }

        if (this.createPlayerUuid != null){
            playerJson.addProperty("createPlayerUuid", this.createPlayerUuid.toString());
        } else {
            playerJson.add("createPlayerUuid", null);
        }

        if (this.lastModifiedTime != null) {
            playerJson.addProperty("lastModifiedTime", this.lastModifiedTime.toString());
        } else {
            playerJson.add("lastModifiedTime", null);
        }
        if (this.lastModifiedPlayerUuid != null) {
            playerJson.addProperty("lastModifiedPlayerUuid", this.lastModifiedPlayerUuid.toString());
        } else {
            playerJson.add("lastModifiedPlayerUuid", null);
        }
        return playerJson;
    }

    /**
     * 以该假人数据创建假人
     *
     * @param server 服务器实例
     * @return 假人
     */
    public EntityPlayerMPFake spawn(MinecraftServer server) {
        if (!PlayerUtil.canSpawn(this.name, server.getPlayerManager())){
            return null;
        }
        return EntityPlayerMPFake.createFake(this.name, server, this.pos.getX(), this.pos.getY(), this.pos.getY(), this.yaw, this.pitch, RegistryKey.of(RegistryKeys.WORLD, this.dimension), this.gamemode, this.flying);
    }
}
