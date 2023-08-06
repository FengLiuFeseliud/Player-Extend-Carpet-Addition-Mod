package fengliu.peca;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.utils.Translations;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import fengliu.peca.command.PecaCommand;
import fengliu.peca.command.PlayerAutoCommand;
import fengliu.peca.command.PlayerGroupCommand;
import fengliu.peca.command.PlayerManageCommand;
import fengliu.peca.player.sql.PlayerGroupSql;
import fengliu.peca.player.sql.PlayerSql;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.WorldSavePath;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public class PecaMod implements ModInitializer, CarpetExtension {
    public static final String MOD_ID = "peca";
    public static Path MC_PATH;
    public static String MOD_VERSION;
    public static String dbPath;
    public static String dbUrl;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    static {
        CarpetServer.manageExtension(new PecaMod());
        MC_PATH = FabricLoader.getInstance().getGameDir();
        FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(modContainer -> MOD_VERSION = modContainer.getMetadata().getVersion().getFriendlyString());
    }

    @Override
    public String version() {
        return MOD_ID;
    }

    @Override
    public void onGameStarted() {
        CarpetServer.settingsManager.parseSettingsClass(PecaSettings.class);
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        String dataJSON;
        try {
            dataJSON = IOUtils.toString(
                Objects.requireNonNull(Translations.class.getClassLoader().getResourceAsStream(
                    String.format("assets/" + MOD_ID + "/lang/%s.json", lang))),
                StandardCharsets.UTF_8);
        } catch (NullPointerException | IOException e) {
            return null;
        }
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        return gson.fromJson(dataJSON, new TypeToken<Map<String, String>>() {
        }.getType());
    }

    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandBuildContext) {
        PlayerGroupCommand.registerAll(dispatcher);
        PlayerAutoCommand.registerAll(dispatcher, commandBuildContext);
        PlayerManageCommand.registerAll(dispatcher, commandBuildContext);
        PecaCommand.registerAll(dispatcher, commandBuildContext);
    }

    @Override
    public void onInitialize() {
    }

    @Override
    public void onServerLoaded(MinecraftServer server) {
        dbPath = server.getSavePath(WorldSavePath.ROOT).getParent() + "/pecaPlayer.db";
        dbUrl = "jdbc:sqlite:" + dbPath;

        PlayerSql.createTable();
        PlayerGroupSql.createTable();
    }
}