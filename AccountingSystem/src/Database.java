import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    public static Connection connect() throws SQLException {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new SQLException(
                "Missing database settings. Set DB_URL, DB_USER, and DB_PASSWORD."
            );
        }
        return DriverManager.getConnection(url, user, password);
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