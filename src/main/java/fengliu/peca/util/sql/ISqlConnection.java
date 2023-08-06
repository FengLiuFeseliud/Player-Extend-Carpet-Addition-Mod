package fengliu.peca.util.sql;

import fengliu.peca.PecaMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * sql 连接
 */
public interface ISqlConnection {
    /**
     * 获取数据库地址
     *
     * @return 数据库地址
     */
    String getDBUrl();

    /**
     * 获取使用表名
     *
     * @return 表名
     */
    String getTableName();

    /**
     * 获取创建使用表 sql
     *
     * @return sql
     */
    String getCreateTableSql();

    interface Job {
        Object run(Statement statement) throws Exception;
    }

    /**
     * 以该配置执行 sql 语句
     *
     * @param job sql
     * @return 结果
     */
    default Object runSql(Job job) {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection connection = DriverManager.getConnection(this.getDBUrl());
            Statement statement = connection.createStatement();
            Object data = job.run(statement);
            statement.close();
            connection.close();
            return data;
        } catch (Exception e) {
            PecaMod.LOGGER.error(String.valueOf(e));
        }
        return false;
    }

    /**
     * 创建表
     *
     * @return 成功 true
     */
    default boolean createTable() {
        return (boolean) this.runSql(statement -> {
            try {
                statement.executeQuery(String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%s';", this.getTableName())).getString(1);
                return true;
            } catch (SQLException ignored) {
            }

            statement.execute(this.getCreateTableSql());
            PecaMod.LOGGER.info(Text.translatable(String.format("peca.info.sql.not.exist.table.%s", this.getTableName())).getString());
            return true;
        });
    }

    /**
     * 以该配置执行 sql 语句
     *
     * @param job sql
     * @return 结果
     */
    default Object executeSpl(Job job) {
        if (!createTable()) {
            PecaMod.LOGGER.error(Text.translatable(String.format("peca.info.sql.error.exist.table.%s", this.getTableName())).getString());
            return false;
        }
        return runSql(job);
    }
}
