import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class RecoveryService {

    private static final int CODE_ITERATIONS = 200_000;
    private static final int PASSWORD_ITERATIONS = 600_000;
    private static final int KEY_BITS = 256;
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MS = 60_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static boolean columnsReady = false;

    // username -> {failed attempts, locked-until time}
    private static final Map<String, long[]> attempts = new HashMap<>();

    // Makes a new code, saves its hash for this user, and returns the code.
    public static String issueCode(String username)
            throws SQLException, GeneralSecurityException {

        String code = newCode();
        byte[] salt = randomBytes(16);
        byte[] hash = hash(normalize(code).toCharArray(), salt, CODE_ITERATIONS);

        try (Connection connection = Database.connect()) {
            ensureColumns(connection);

            String sql = """
                UPDATE public.users
                SET recovery_hash = ?, recovery_salt = ?
                WHERE LOWER(username) = LOWER(?)
                """;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setBytes(1, hash);
                ps.setBytes(2, salt);
                ps.setString(3, username.trim());
                ps.executeUpdate();
            }
        }
        return code;
    }

    // Returns the NEW recovery code if it worked, or null if the
    // username/code was wrong.
    public static String resetPassword(
        String username,
        String code,
        char[] newPassword
    ) throws SQLException, GeneralSecurityException {

        String key = username.trim().toLowerCase();
        checkLocked(key);

        if (newPassword.length < 8) {
            throw new IllegalArgumentException(
                "New password needs at least 8 characters."
            );
        }

        try (Connection connection = Database.connect()) {
            ensureColumns(connection);

            byte[] storedHash;
            byte[] storedSalt;

            String selectSql = """
                SELECT recovery_hash, recovery_salt
                FROM public.users
                WHERE LOWER(username) = LOWER(?)
                """;

            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setString(1, username.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        recordFailure(key);
                        return null;
                    }
                    storedHash = rs.getBytes(1);
                    storedSalt = rs.getBytes(2);
                }
            }

            if (storedHash == null || storedSalt == null) {
                recordFailure(key);
                return null;
            }

            byte[] actual = hash(
                normalize(code).toCharArray(), storedSalt, CODE_ITERATIONS
            );

            if (!MessageDigest.isEqual(actual, storedHash)) {
                recordFailure(key);
                return null;
            }

            clearFailures(key);

            byte[] passwordSalt = randomBytes(16);
            byte[] passwordHash = hash(newPassword, passwordSalt, PASSWORD_ITERATIONS);

            String newCode = newCode();
            byte[] codeSalt = randomBytes(16);
            byte[] codeHash = hash(
                normalize(newCode).toCharArray(), codeSalt, CODE_ITERATIONS
            );

            String updateSql = """
                UPDATE public.users
                SET password_hash = ?, password_salt = ?, iterations = ?,
                    recovery_hash = ?, recovery_salt = ?
                WHERE LOWER(username) = LOWER(?)
                """;

            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setBytes(1, passwordHash);
                ps.setBytes(2, passwordSalt);
                ps.setInt(3, PASSWORD_ITERATIONS);
                ps.setBytes(4, codeHash);
                ps.setBytes(5, codeSalt);
                ps.setString(6, username.trim());
                ps.executeUpdate();
            }

            return newCode;
        }
    }

    // ---------- helpers ----------

    private static synchronized void ensureColumns(Connection connection)
            throws SQLException {
        if (columnsReady) return;

        try (Statement st = connection.createStatement()) {
            st.execute("""
                ALTER TABLE public.users
                ADD COLUMN IF NOT EXISTS recovery_hash BYTEA,
                ADD COLUMN IF NOT EXISTS recovery_salt BYTEA
                """);
        }
        columnsReady = true;
    }

    private static String newCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) sb.append('-');
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String normalize(String code) {
        return code.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static byte[] hash(char[] secret, byte[] salt, int iterations)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(secret, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static synchronized void checkLocked(String key) {
        long[] state = attempts.get(key);
        if (state != null && state[1] > System.currentTimeMillis()) {
            long seconds = (state[1] - System.currentTimeMillis()) / 1000 + 1;
            throw new IllegalArgumentException(
                "Too many attempts. Try again in " + seconds + " seconds."
            );
        }
    }

    private static synchronized void recordFailure(String key) {
        long[] state = attempts.computeIfAbsent(key, k -> new long[2]);
        state[0]++;
        if (state[0] >= MAX_ATTEMPTS) {
            state[0] = 0;
            state[1] = System.currentTimeMillis() + LOCK_MS;
        }
    }

    private static synchronized void clearFailures(String key) {
        attempts.remove(key);
    }
}