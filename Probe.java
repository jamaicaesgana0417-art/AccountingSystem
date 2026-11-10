import java.math.BigDecimal;
import java.sql.*;
public class Probe {
  public static void main(String[] args) throws Exception {
    Database.initialize();
    String username = "probe_tx_" + System.currentTimeMillis();
    char[] pass = "P@ssw0rd123".toCharArray();
    AuthService.register(username, pass);
    Session.login(username);
    ChartOfAccounts.ensureForUser(username);
    System.out.println("USER=" + username);
    try (Connection conn = Database.connect()) {
      PreparedStatement s = conn.prepareStatement("SELECT id, name, code FROM accounts WHERE owner_username = ? ORDER BY code");
      s.setString(1, username);
      ResultSet rs = s.executeQuery();
      while (rs.next()) {
        System.out.println(rs.getString("code") + " | " + rs.getString("name") + " | " + rs.getInt("id"));
      }
    }
    String sql = "SELECT MIN(id) AS debit_id, MAX(id) AS credit_id FROM accounts WHERE owner_username = ? AND active = 1";
    int debitId = -1, creditId = -1;
    try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        debitId = rs.getInt("debit_id");
        creditId = rs.getInt("credit_id");
      }
    }
    System.out.println("debit=" + debitId + " credit=" + creditId);
    if (debitId > 0 && creditId > 0 && debitId != creditId) {
      try (Connection conn = Database.connect()) {
        conn.setAutoCommit(false);
        long txId;
        try (PreparedStatement tx = conn.prepareStatement("INSERT INTO transactions (transaction_date, description, owner_username) VALUES (?, ?, ?) RETURNING id")) {
          tx.setString(1, java.time.LocalDate.now().toString());
          tx.setString(2, "Probe transaction");
          tx.setString(3, username);
          ResultSet rs = tx.executeQuery();
          rs.next();
          txId = rs.getLong("id");
        }
        try (PreparedStatement line = conn.prepareStatement("INSERT INTO journal_lines (transaction_id, account_id, debit_centavos, credit_centavos) VALUES (?, ?, ?, ?)")) {
          line.setLong(1, txId);
          line.setInt(2, debitId);
          line.setLong(3, 12345);
          line.setLong(4, 0);
          line.executeUpdate();
          line.setLong(1, txId);
          line.setInt(2, creditId);
          line.setLong(3, 0);
          line.setLong(4, 12345);
          line.executeUpdate();
        }
        conn.commit();
        System.out.println("posted tx=" + txId);
      }
    }
  }
}