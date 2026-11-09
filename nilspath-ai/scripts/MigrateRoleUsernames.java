import java.sql.*;
import java.util.*;

/** One-time, idempotent MySQL migration. Credentials are read from the environment. */
class MigrateRoleUsernames {
    static Map<String,List<String>> uniqueIndexes(Connection connection) throws SQLException {
        Map<String,SortedMap<Integer,String>> indexes=new LinkedHashMap<>();
        try(var rows=connection.getMetaData().getIndexInfo(connection.getCatalog(),null,"accounts",true,false)) {
            while(rows.next()) {
                String index=rows.getString("INDEX_NAME"),column=rows.getString("COLUMN_NAME");
                if(index!=null && column!=null && !"PRIMARY".equalsIgnoreCase(index))
                    indexes.computeIfAbsent(index,key->new TreeMap<>()).put(rows.getInt("ORDINAL_POSITION"),column.toLowerCase(Locale.ROOT));
            }
        }
        Map<String,List<String>> result=new LinkedHashMap<>();
        indexes.forEach((name,columns)->result.put(name,new ArrayList<>(columns.values())));
        return result;
    }
    static long count(Connection connection) throws SQLException {
        try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT COUNT(*) FROM accounts")) {rows.next();return rows.getLong(1);}
    }
    public static void main(String[] args) throws Exception {
        String url=System.getenv("DB_URL"),user=System.getenv("DB_USER"),password=System.getenv("DB_PASSWORD");
        if(url==null||url.isBlank())url="jdbc:mysql://localhost:3306/nilspath_ai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        if(user==null||user.isBlank())user="nilspath_app";
        if(password==null||password.isBlank())throw new IllegalArgumentException("DB_PASSWORD is required.");
        try(var connection=DriverManager.getConnection(url,user,password)) {
            if(!connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql"))
                throw new IllegalStateException("This migration is for MySQL only.");
            long before=count(connection);
            Map<String,List<String>> indexes=uniqueIndexes(connection);
            try(var statement=connection.createStatement()) {
                boolean scoped=indexes.values().stream().anyMatch(columns->columns.size()==2 && new HashSet<>(columns).equals(Set.of("username","role")));
                if(!scoped)statement.execute("ALTER TABLE accounts ADD CONSTRAINT uk_accounts_username_role UNIQUE (username, role)");
                // Add the replacement constraint first; preserve every account,
                // password hash, approval flag and foreign-key ID throughout.
                for(var index:indexes.entrySet()) {
                    if(index.getValue().equals(List.of("username")))
                        statement.execute("ALTER TABLE accounts DROP INDEX `"+index.getKey().replace("`","``")+"`");
                }
            }
            indexes=uniqueIndexes(connection);
            boolean global=indexes.values().stream().anyMatch(columns->columns.equals(List.of("username")));
            boolean scoped=indexes.values().stream().anyMatch(columns->columns.size()==2 && new HashSet<>(columns).equals(Set.of("username","role")));
            if(global||!scoped||count(connection)!=before)throw new IllegalStateException("Migration verification failed.");
            System.out.println("Role-scoped username uniqueness enabled; all "+before+" existing accounts preserved.");
        }
    }
}
