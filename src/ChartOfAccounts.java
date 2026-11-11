import java.sql.*;
import java.util.*;

/** The fixed chart of accounts. Every user gets their own copy of these. */
public final class ChartOfAccounts {

    public static final class Entry {
        public final String code;
        public final String name;
        public final String type;

        Entry(String code, String name, String type) {
            this.code = code;
            this.name = name;
            this.type = type;
        }
    }

    public static final List<Entry> ACCOUNTS = List.of(
        new Entry("1", "Cash", "ASSET"),
        new Entry("2", "Accounts Receivable", "ASSET"),
        new Entry("3", "Supplies", "ASSET"),
        new Entry("4", "Prepaid Insurance", "ASSET"),
        new Entry("5", "Prepaid Rent", "ASSET"),
        new Entry("6", "Equipment", "ASSET"),
        new Entry("7", "Accumulated Depreciation - Equipment", "CONTRA-ASSET"),
        new Entry("8", "Furniture", "ASSET"),
        new Entry("9", "Vehicles", "ASSET"),
        new Entry("10", "Accounts Payable", "LIABILITY"),
        new Entry("11", "Unearned Revenue", "LIABILITY"),
        new Entry("12", "Notes Payable", "LIABILITY"),
        new Entry("13", "Salaries Payable", "LIABILITY"),
        new Entry("14", "Owner's Capital", "EQUITY"),
        new Entry("15", "Owner's Drawings", "CONTRA-EQUITY"),
        new Entry("16", "Service Revenue", "REVENUE"),
        new Entry("17", "Expenses", "EXPENSE"),
        new Entry("18", "Advertising Expense", "EXPENSE"),
        new Entry("19", "Utilities Expense", "EXPENSE"),
        new Entry("20", "Salaries Expense", "EXPENSE"),
        new Entry("21", "Maintenance Expense", "EXPENSE"),
        new Entry("22", "Professional Fees Expense", "EXPENSE"),
        new Entry("23", "Repairs Expense", "EXPENSE"),
        new Entry("24", "Rent Expense", "EXPENSE"),
        new Entry("25", "Interest Expense", "EXPENSE"),
        new Entry("26", "Insurance Expense", "EXPENSE"),
        new Entry("27", "Supplies Expense", "EXPENSE"),
        new Entry("28", "Depreciation Expense", "EXPENSE")
    );

    private ChartOfAccounts() {}

    /** True for accounts whose normal balance is a debit. */
    public static boolean isDebitNormal(String type) {
        return type.equals("ASSET")
            || type.equals("EXPENSE")
            || type.equals("CONTRA-EQUITY");
    }

    private static String key(String name) {
        return name.trim().replace('\u2019', '\'').toLowerCase();
    }

    /**
     * Makes sure the user has every predefined account (existing ones are
     * kept, with their type corrected and switched to active).
     */
    public static void ensureForUser(String username) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try {
                Map<String, int[]> idByName = new HashMap<>();      // id, active
                Map<String, String> typeByName = new HashMap<>();
                Set<String> usedCodes = new HashSet<>();

                String selectSql = """
                    SELECT id, code, name, type, active
                    FROM accounts
                    WHERE owner_username = ?
                    """;

                try (PreparedStatement select =
                         connection.prepareStatement(selectSql)) {
                    select.setString(1, username);

                    try (ResultSet rs = select.executeQuery()) {
                        while (rs.next()) {
                            String k = key(rs.getString("name"));
                            idByName.putIfAbsent(
                                k, new int[] {rs.getInt("id"), rs.getInt("active")});
                            typeByName.putIfAbsent(k, rs.getString("type"));
                            usedCodes.add(rs.getString("code"));
                        }
                    }
                }

                String insertSql = """
                    INSERT INTO accounts (code, name, type, active, owner_username)
                    VALUES (?, ?, ?, 1, ?)
                    """;
                String updateSql = """
                    UPDATE accounts SET type = ?, active = 1 WHERE id = ?
                    """;

                try (PreparedStatement insert =
                         connection.prepareStatement(insertSql);
                     PreparedStatement update =
                         connection.prepareStatement(updateSql)) {

                    for (Entry entry : ACCOUNTS) {
                        String k = key(entry.name);

                        if (idByName.containsKey(k)) {
                            int[] found = idByName.get(k);
                            if (!entry.type.equals(typeByName.get(k))
                                    || found[1] != 1) {
                                update.setString(1, entry.type);
                                update.setInt(2, found[0]);
                                update.executeUpdate();
                            }
                            continue;
                        }

                        // Pick a free code if the preferred one is taken.
                        int code = Integer.parseInt(entry.code);
                        while (usedCodes.contains(String.valueOf(code))) {
                            code++;
                        }
                        usedCodes.add(String.valueOf(code));

                        insert.setString(1, String.valueOf(code));
                        insert.setString(2, entry.name);
                        insert.setString(3, entry.type);
                        insert.setString(4, username);
                        insert.executeUpdate();
                    }
                }

                connection.commit();

            } catch (SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }
}
