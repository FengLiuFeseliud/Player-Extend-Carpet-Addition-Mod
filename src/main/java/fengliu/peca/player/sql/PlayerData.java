package fengliu.peca.player.sql;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PlayerData(long id, String name, Identifier dimension, Vec3d pos, double yaw, double pitch, GameMode gamemode, boolean flying, @Nullable String purpose, Timestamp createTime, UUID createPlayerUuid){
    public static List<PlayerData> fromResultSet(ResultSet result) throws SQLException {
        List<PlayerData> playerDates = new ArrayList<>();
        while (result.next()){
            playerDates.add(new PlayerData(
                    result.getInt("ID"),
                    result.getString("NAME"),
                    Identifier.tryParse(result.getString("DIMENSION")),
                    new Vec3d(result.getDouble("X"), result.getDouble("Y"), result.getDouble("Z")),
                    result.getDouble("FACING_Y"),
                    result.getDouble("FACING_X"),
                    GameMode.getOrNull(result.getInt("GAME_MODE")),
                    result.getBoolean("FLYING"),
                    result.getString("PURPOSE"),
                    Timestamp.valueOf(result.getString("CREATE_TIME")),
                    UUID.fromString(result.getString("CREATE_PLAYER_UUID"))
            ));
        }
        return playerDates;
    }

    public static PlayerData fromPlayer(ServerPlayerEntity player){
        return new PlayerData (-1 , player.getEntityName(), player.getWorld().getDimensionKey().getValue(), player.getPos(), player.getYaw(), player.getPitch(), player.interactionManager.getGameMode(), player.getAbilities().flying, null, null, null);
    }
}
