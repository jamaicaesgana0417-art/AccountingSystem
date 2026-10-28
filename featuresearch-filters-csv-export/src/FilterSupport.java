import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared search / filter helpers (Project 10: Search, Filters, and CSV Export).
 *
 * Every panel builds its WHERE clause with these helpers so that keyword
 * search, inclusive date ranges, and account / account-type filters behave
 * the same everywhere. All user input is passed as bound parameters (never
 * concatenated into the SQL text).
 *
 * The SQL is written for PostgreSQL (Supabase).
 */
public final class FilterSupport {

    public static final String ALL_TYPES = "All Types";

    public static final String[] ACCOUNT_TYPES = {
        "ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE"
    };

    private FilterSupport() {
    }

    /** SQL text plus the values to bind to its ? placeholders, in order. */
    public static final class Query {
        public final String sql;
        public final List<Object> params;

        public Query(String sql, List<Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }

    // ---------------------------------------------------------------
    // Dates
    // ---------------------------------------------------------------

    /**
     * Blank text means "no limit" and returns null.
     * Anything else must be a real calendar date written YYYY-MM-DD.
     *
     * @throws DateTimeParseException for bad text such as 2026-02-30
     */
    public static LocalDate parseOptionalDate(String text) {
        String value = text == null ? "" : text.trim();

        return value.isEmpty() ? null : LocalDate.parse(value);
    }

    /** Returns an error message, or null when the range is usable. */
    public static String validateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            return "Start date must not be after end date.";
        }

        return null;
    }

    /**
     * Inclusive date range: rows ON the start date and ON the end date are
     * both kept. CAST makes this work whether the column is DATE or text
     * holding YYYY-MM-DD.
     */
    public static void appendDateRange(
        StringBuilder sql,
        List<Object> params,
        String column,
        LocalDate start,
        LocalDate end
    ) {
        if (start != null) {
            sql.append(" AND CAST(").append(column)
                .append(" AS DATE) >= ? ");
            params.add(start);
        }

        if (end != null) {
            sql.append(" AND CAST(").append(column)
                .append(" AS DATE) <= ? ");
            params.add(end);
        }
    }

    // ---------------------------------------------------------------
    // Keyword search
    // ---------------------------------------------------------------

    /**
     * One case-insensitive "contains" test with a single ? placeholder.
     * strpos() is a plain substring test, so a typed % or _ is treated as
     * ordinary text and not as a wildcard.
     */
    public static String keywordTest(String expression) {
        return "strpos(lower(COALESCE(CAST(" + expression
            + " AS TEXT), '')), lower(?)) > 0";
    }

    /**
     * Adds " AND (test1 OR test2 ...)" for the keyword, one placeholder per
     * expression. A blank keyword adds nothing.
     */
    public static void appendKeyword(
        StringBuilder sql,
        List<Object> params,
        String keyword,
        String... expressions
    ) {
        String trimmed = normalizeKeyword(keyword);

        if (trimmed.isEmpty() || expressions.length == 0) {
            return;
        }

        sql.append(" AND (");

        for (int i = 0; i < expressions.length; i++) {
            if (i > 0) {
                sql.append(" OR ");
            }

            sql.append(keywordTest(expressions[i]));
            params.add(trimmed);
        }

        sql.append(") ");
    }

    public static String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    /**
     * Java-side version of the same rule, for lists where the row must stay
     * in the calculation (e.g. ledger running balance) but only matching
     * rows are shown.
     */
    public static boolean matchesKeyword(String keyword, String... texts) {
        String needle = normalizeKeyword(keyword).toLowerCase(Locale.ROOT);

        if (needle.isEmpty()) {
            return true;
        }

        for (String text : texts) {
            if (text != null
                    && text.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }

        return false;
    }

    // ---------------------------------------------------------------
    // Account / account-type filters
    // ---------------------------------------------------------------

    /** Adds " AND column = ?" when value is not null. */
    public static void appendEquals(
        StringBuilder sql,
        List<Object> params,
        String column,
        Object value
    ) {
        if (value == null) {
            return;
        }

        sql.append(" AND ").append(column).append(" = ? ");
        params.add(value);
    }

    /**
     * Adds " AND (a = ? OR b = ?)" when value is not null. Used for the
     * debit-account / credit-account pair on the Transactions list.
     */
    public static void appendEither(
        StringBuilder sql,
        List<Object> params,
        String columnA,
        String columnB,
        Object value
    ) {
        if (value == null) {
            return;
        }

        sql.append(" AND (").append(columnA).append(" = ? OR ")
            .append(columnB).append(" = ?) ");
        params.add(value);
        params.add(value);
    }

    /** "All Types", blank, or null mean no type filter (returns null). */
    public static String typeOrNull(Object selected) {
        if (selected == null) {
            return null;
        }

        String text = selected.toString().trim();

        if (text.isEmpty() || text.equals(ALL_TYPES)) {
            return null;
        }

        return text;
    }

    public static String[] typeChoices() {
        String[] choices = new String[ACCOUNT_TYPES.length + 1];
        choices[0] = ALL_TYPES;
        System.arraycopy(
            ACCOUNT_TYPES, 0, choices, 1, ACCOUNT_TYPES.length
        );
        return choices;
    }

    // ---------------------------------------------------------------
    // Binding
    // ---------------------------------------------------------------

    public static int countPlaceholders(String sql) {
        int count = 0;

        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') {
                count++;
            }
        }

        return count;
    }

    public static void bind(PreparedStatement statement, List<Object> params)
            throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);
            int index = i + 1;

            if (value instanceof LocalDate) {
                statement.setDate(index, Date.valueOf((LocalDate) value));
            } else if (value instanceof Integer) {
                statement.setInt(index, (Integer) value);
            } else {
                statement.setString(index, String.valueOf(value));
            }
        }
    }

    public static List<Object> newParams() {
        return new ArrayList<>();
    }
}
