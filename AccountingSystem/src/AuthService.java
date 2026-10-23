import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class AuthService {

    private static final int ITERATIONS = 600_000;
    private static final int KEY_BITS = 256;

    public static void initialize() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY,
                username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_hash BLOB NOT NULL,
                password_salt BLOB NOT NULL,
                iterations INTEGER NOT NULL
            )
            """;

        try (
            Connection connection = Database.connect();
            Statement statement = connection.createStatement()
        ) {
            statement.executeUpdate(sql);
        }
    }

    public static void register(
        String username,
        char[] password
    ) throws SQLException, GeneralSecurityException {

        username = username.trim();

        if (!username.matches("[A-Za-z0-9_]{3,30}")) {
            throw new IllegalArgumentException(
                "Username must contain 3–30 letters, numbers, or underscores."
            );
        }

        if (password.length < 8) {
            throw new IllegalArgumentException(
                "Password must contain at least 8 characters."
            );
        }

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        byte[] hash = hash(password, salt, ITERATIONS);

        String sql = """
            INSERT INTO users (
                username, password_hash, password_salt, iterations
            ) VALUES (?, ?, ?, ?)
            """;

        try (Connection connection = Database.connect()) {
            String checkSql =
                "SELECT 1 FROM users WHERE username = ?";

            try (
                PreparedStatement check =
                    connection.prepareStatement(checkSql)
            ) {
                check.setString(1, username);

                try (ResultSet result = check.executeQuery()) {
                    if (result.next()) {
                        throw new IllegalArgumentException(
                            "That username is already taken."
                        );
                    }
                }
            }

            try (
                PreparedStatement statement =
                    connection.prepareStatement(sql)
            ) {
                statement.setString(1, username);
                statement.setBytes(2, hash);
                statement.setBytes(3, salt);
                statement.setInt(4, ITERATIONS);
                statement.executeUpdate();
            }
        }
    }

    // Returns the saved username on success; otherwise returns null.
    public static String login(
        String username,
        char[] password
    ) throws SQLException, GeneralSecurityException {

        String sql = """
            SELECT username, password_hash, password_salt, iterations
            FROM users
            WHERE username = ?
            """;

        try (
            Connection connection = Database.connect();
            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setString(1, username.trim());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                byte[] actual = hash(
                    password,
                    result.getBytes("password_salt"),
                    result.getInt("iterations")
                );

                byte[] expected = result.getBytes("password_hash");

                return MessageDigest.isEqual(actual, expected)
                    ? result.getString("username")
                    : null;
            }
        }
    }

    private static byte[] hash(
        char[] password,
        byte[] salt,
        int iterations
    ) throws GeneralSecurityException {

        PBEKeySpec specification =
            new PBEKeySpec(password, salt, iterations, KEY_BITS);

        try {
            return SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(specification)
                .getEncoded();
        } finally {
            specification.clearPassword();
        }
    }
}
