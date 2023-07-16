package fengliu.peca.util.sql;

public class SqlUtil {

    public static class BuildSqlHelper{
        protected String sql;
        protected boolean whereIn = false;
        protected boolean firstIn = true;

        public BuildSqlHelper(String baseSql){
            this.sql = baseSql + " ";
        }

        public BuildSqlHelper where(){
            if (whereIn){
                return this;
            }

            this.whereIn = true;
            this.sql += "WHERE ";
            return this;
        }

        interface Add{
            void add();
        }

        private BuildSqlHelper add(Add add, String sql){
            if (!this.whereIn){
                this.where();
                this.sql += sql + " ";
                this.whereIn = true;
            } else {
                add.add();
            }

            return this;
        }

        public BuildSqlHelper like(String t, String sql){
            return this.and(t + " LIKE " + sql);
        }

        public BuildSqlHelper and(String sql){
            return this.add(() -> this.sql += "AND " + sql + " ", sql);
        }

        public BuildSqlHelper or(String sql){
            return this.add(() -> this.sql += "OR " + sql + " ", sql);
        }

        public String build(){
            return this.sql;
        }
    }
}
