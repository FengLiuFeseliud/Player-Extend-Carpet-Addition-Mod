package fengliu.peca.util.sql;

import fengliu.peca.PecaMod;
import net.minecraft.text.Text;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public interface ISqlConnection {
    String getDBUrl();
    String getTableName();
    String getCreateTableSql();

    interface Job{
        Object run(Statement statement) throws Exception;
    }

    default Object runSql(Job job){
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

    default boolean createTable(){
        return (boolean) this.runSql(statement -> {
            try {
                statement.executeQuery(String.format("SELECT name FROM sqlite_master WHERE type='table' AND name='%s';", this.getTableName())).getString(1);
                return true;
            } catch (SQLException ignored){}

            statement.execute(this.getCreateTableSql());
            PecaMod.LOGGER.info(Text.translatable(String.format("peca.info.sql.not.exist.table.%s", this.getTableName())).getString());
            return true;
        });
    }

    default Object executeSpl(Job job){
        if (!createTable()){
            PecaMod.LOGGER.error(Text.translatable(String.format("peca.info.sql.error.exist.table.%s", this.getTableName())).getString());
            return false;
        }
        return runSql(job);
    }
}
