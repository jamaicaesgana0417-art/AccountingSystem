import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL =
        "jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require";
    private static final String USER = "postgres.jinrxgmbxxgbggkdwkfd";
    private static final String PASSWORD = "M3oEulFY01eXBh7J";

    public static Connection connect() throws SQLException {
        if (PASSWORD.startsWith("PASTE_YOUR")) {
            throw new SQLException(
                "Database password not set. Open src/Database.java and paste it into PASSWORD."
            );
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initialize() throws SQLException {
        try (Connection connection = connect()) {
            if (!connection.isValid(5)) {
                throw new SQLException("Database connection failed.");
            }

            System.out.println("Connected to Supabase successfully.");
        }
    }

    public static void main(String[] args) {
        try {
            initialize();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
}
