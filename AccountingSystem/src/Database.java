import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String URL = "jdbc:sqlite:accounting.db";

    public static Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }

        return connection;
    }

    public static void initialize() throws SQLException {
        String accounts = """
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY,
                code TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                type TEXT NOT NULL CHECK (
                    type IN (
                        'ASSET', 'LIABILITY', 'EQUITY',
                        'REVENUE', 'EXPENSE'
                    )
                ),
                active INTEGER NOT NULL DEFAULT 1
                    CHECK (active IN (0, 1))
            )
            """;

        String transactions = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY,
                transaction_date TEXT NOT NULL,
                description TEXT NOT NULL
            )
            """;

        // Amounts are stored as whole centavos.
        String journalLines = """
            CREATE TABLE IF NOT EXISTS journal_lines (
                id INTEGER PRIMARY KEY,
                transaction_id INTEGER NOT NULL,
                account_id INTEGER NOT NULL,
                debit_centavos INTEGER NOT NULL DEFAULT 0,
                credit_centavos INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (transaction_id) REFERENCES transactions(id),
                FOREIGN KEY (account_id) REFERENCES accounts(id),
                CHECK (
                    (debit_centavos > 0 AND credit_centavos = 0)
                    OR
                    (credit_centavos > 0 AND debit_centavos = 0)
                )
            )
            """;

        try (
            Connection connection = connect();
            Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate(accounts);
            statement.executeUpdate(transactions);
            statement.executeUpdate(journalLines);
        }
    }

    public static void main(String[] args) {
        try {
            initialize();
            System.out.println("Database and tables are ready.");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
}
