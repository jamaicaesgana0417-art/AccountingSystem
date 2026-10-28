import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Project 10 tests: search, filters, and CSV export.
 *
 * Plain Java (no JUnit needed). Needs NO database and NO window.
 *
 * Compile + run from the project folder (see tests/README.md):
 *   javac -d out -cp lib/postgresql-42.7.14.jar src/*.java tests/*.java
 *   java -Djava.awt.headless=true -cp out FilterExportTests
 */
public class FilterExportTests {

    private static int passed = 0;
    private static final List<String> failures = new ArrayList<>();
    private static String section = "";

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        dates();
        dateRangeSql();
        keywordSearch();
        typeFilter();
        queryBuilders();
        csvFormat();
        csvEscaping();
        csvFormulaGuard();
        csvMatchesScreen();
        csvFile();

        System.out.println();
        System.out.println("Passed: " + passed + "   Failed: " + failures.size());

        for (String failure : failures) {
            System.out.println("  FAIL " + failure);
        }

        System.exit(failures.isEmpty() ? 0 : 1);
    }

    // =================================================================
    // 10.2  Date parsing and validation
    // =================================================================
    static void dates() {
        section = "10.2 dates";

        check("blank = no limit", FilterSupport.parseOptionalDate("") == null);
        check("spaces = no limit", FilterSupport.parseOptionalDate("   ") == null);
        check("null = no limit", FilterSupport.parseOptionalDate(null) == null);
        check("valid date",
            LocalDate.of(2026, 1, 31).equals(
                FilterSupport.parseOptionalDate(" 2026-01-31 ")));
        check("leap day ok",
            LocalDate.of(2024, 2, 29).equals(
                FilterSupport.parseOptionalDate("2024-02-29")));

        for (String bad : new String[] {
            "2026-02-30", "2026-02-29", "2026-13-01", "2026-00-10",
            "31/01/2026", "2026-1-5", "20260131", "abc", "2026-01-31x"
        }) {
            boolean threw = false;

            try {
                FilterSupport.parseOptionalDate(bad);
            } catch (DateTimeParseException expected) {
                threw = true;
            }

            check("rejects \"" + bad + "\"", threw);
        }

        LocalDate a = LocalDate.of(2026, 3, 1);
        LocalDate b = LocalDate.of(2026, 3, 31);

        check("start after end is an error",
            FilterSupport.validateRange(b, a) != null);
        check("start before end ok", FilterSupport.validateRange(a, b) == null);
        check("single day (start = end) ok",
            FilterSupport.validateRange(a, a) == null);
        check("only start ok", FilterSupport.validateRange(a, null) == null);
        check("only end ok", FilterSupport.validateRange(null, b) == null);
        check("neither ok", FilterSupport.validateRange(null, null) == null);
    }

    // =================================================================
    // 10.2  Inclusive range in SQL
    // =================================================================
    static void dateRangeSql() {
        section = "10.2 inclusive range";

        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        FilterSupport.appendDateRange(sql, params, "t.d", start, end);

        String text = sql.toString();

        check("start uses >= (includes start date)", text.contains(">= ?"));
        check("end uses <= (includes end date)", text.contains("<= ?"));
        check("never uses strict > or <",
            !text.contains("> ?") && !text.replace(">= ?", "").contains("> ?")
                && !text.replace("<= ?", "").contains("< ?"));
        check("params are [start, end] in order",
            params.equals(Arrays.asList(start, end)));
        check("placeholders match params",
            FilterSupport.countPlaceholders(text) == params.size());

        sql = new StringBuilder();
        params = new ArrayList<>();
        FilterSupport.appendDateRange(sql, params, "t.d", start, null);
        check("start only -> one param", params.equals(List.of(start))
            && !sql.toString().contains("<="));

        sql = new StringBuilder();
        params = new ArrayList<>();
        FilterSupport.appendDateRange(sql, params, "t.d", null, end);
        check("end only -> one param", params.equals(List.of(end))
            && !sql.toString().contains(">="));

        sql = new StringBuilder();
        params = new ArrayList<>();
        FilterSupport.appendDateRange(sql, params, "t.d", null, null);
        check("no dates -> no SQL, no params",
            sql.length() == 0 && params.isEmpty());

        sql = new StringBuilder();
        params = new ArrayList<>();
        FilterSupport.appendDateRange(sql, params, "t.d", start, start);
        check("single day -> same date twice",
            params.equals(Arrays.asList(start, start)));
    }

    // =================================================================
    // 10.1  Keyword search
    // =================================================================
    static void keywordSearch() {
        section = "10.1 keyword";

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        FilterSupport.appendKeyword(sql, params, "   ", "a", "b");
        check("blank keyword adds nothing", sql.length() == 0 && params.isEmpty());

        sql = new StringBuilder();
        params = new ArrayList<>();
        FilterSupport.appendKeyword(sql, params, null, "a");
        check("null keyword adds nothing", sql.length() == 0 && params.isEmpty());

        sql = new StringBuilder();
        params = new ArrayList<>();
        FilterSupport.appendKeyword(sql, params, "  Rent ", "x", "y", "z");
        check("one placeholder per column", FilterSupport.countPlaceholders(
            sql.toString()) == 3 && params.size() == 3);
        check("keyword is trimmed", params.equals(List.of("Rent", "Rent", "Rent")));
        check("columns are OR-ed", sql.toString().split(" OR ").length == 3);
        check("case-insensitive (lower on both sides)",
            sql.toString().contains("lower(?)")
                && sql.toString().contains("lower(COALESCE"));
        check("uses PostgreSQL strpos, not SQLite instr",
            sql.toString().contains("strpos(")
                && !sql.toString().contains("instr("));

        for (String hostile : new String[] {
            "'; DROP TABLE accounts; --", "100%", "a_b", "\" OR 1=1"
        }) {
            sql = new StringBuilder();
            params = new ArrayList<>();
            FilterSupport.appendKeyword(sql, params, hostile, "a");
            check("\"" + hostile + "\" is bound, never in SQL text",
                !sql.toString().contains(hostile.trim())
                    && params.contains(hostile.trim()));
        }

        // Java-side rule used by the ledger.
        check("matches ignoring case",
            FilterSupport.matchesKeyword("RENT", "Office rent for March"));
        check("matches any column",
            FilterSupport.matchesKeyword("42", "x", "42", "y"));
        check("no match", !FilterSupport.matchesKeyword("zzz", "a", "b"));
        check("blank matches everything",
            FilterSupport.matchesKeyword("  ", "anything"));
        check("null text is safe",
            !FilterSupport.matchesKeyword("a", (String) null));
        check("% is literal text, not a wildcard",
            !FilterSupport.matchesKeyword("%", "no percent sign here")
                && FilterSupport.matchesKeyword("%", "50% off"));
    }

    // =================================================================
    // 10.3  Account-type helpers
    // =================================================================
    static void typeFilter() {
        section = "10.3 type filter";

        check("All Types -> no filter", FilterSupport.typeOrNull("All Types") == null);
        check("null -> no filter", FilterSupport.typeOrNull(null) == null);
        check("blank -> no filter", FilterSupport.typeOrNull("  ") == null);
        check("ASSET kept", "ASSET".equals(FilterSupport.typeOrNull("ASSET")));

        String[] choices = FilterSupport.typeChoices();
        check("dropdown = All Types + 5 types",
            choices.length == 6 && choices[0].equals("All Types")
                && choices[5].equals("EXPENSE"));

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        FilterSupport.appendEither(sql, params, "da.id", "ca.id", 7);
        check("either-side filter binds value twice",
            params.equals(List.of(7, 7))
                && FilterSupport.countPlaceholders(sql.toString()) == 2);

        sql = new StringBuilder();
        params = new ArrayList<>();
        FilterSupport.appendEither(sql, params, "da.id", "ca.id", null);
        check("either-side filter skipped when null",
            sql.length() == 0 && params.isEmpty());
    }

    // =================================================================
    // 10.1-10.3  Real query builders, every combination of filters
    // =================================================================
    static void queryBuilders() {
        section = "queries";

        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        // 5 optional filters -> 32 combinations, all must be consistent.
        int combos = 0;

        for (int mask = 0; mask < 32; mask++) {
            LocalDate s = (mask & 1) != 0 ? start : null;
            LocalDate e = (mask & 2) != 0 ? end : null;
            String kw = (mask & 4) != 0 ? "rent" : "";
            Integer acct = (mask & 8) != 0 ? 7 : null;
            String type = (mask & 16) != 0 ? "EXPENSE" : null;

            FilterSupport.Query t =
                TransactionsPanel.buildQuery(s, e, kw, acct, type);
            FilterSupport.Query j =
                JournalPanel.buildQuery(s, e, kw, acct, type);

            if (FilterSupport.countPlaceholders(t.sql) != t.params.size()
                    || FilterSupport.countPlaceholders(j.sql) != j.params.size()) {
                check("placeholders == params for combination " + mask, false);
            }

            if (glued(t.sql) || glued(j.sql)) {
                check("no glued SQL words (e.g. 0OR) in combination " + mask,
                    false);
            }

            if (t.sql.contains("instr(") || j.sql.contains("instr(")) {
                check("no SQLite instr() in combination " + mask, false);
            }

            combos++;
        }

        check("all " + combos + " filter combinations are consistent", true);

        FilterSupport.Query t = TransactionsPanel.buildQuery(
            start, end, "rent", 7, "EXPENSE");
        List<Object> expected = new ArrayList<>(Arrays.asList(
            start, end, 7, 7, "EXPENSE", "EXPENSE"));
        expected.addAll(Arrays.asList("rent", "rent", "rent", "rent"));
        check("Transactions: combined param order", t.params.equals(expected));
        check("Transactions: newest first", t.sql.contains("ORDER BY t.transaction_date DESC"));

        t = TransactionsPanel.buildQuery(null, null, "", null, null);
        check("Transactions: no filters -> no params", t.params.isEmpty());

        FilterSupport.Query j = JournalPanel.buildQuery(
            start, end, "rent", 7, "EXPENSE");
        expected = new ArrayList<>(Arrays.asList(
            start, end, 7, "EXPENSE", "rent", "rent", "rent"));
        check("Journal: combined param order", j.params.equals(expected));
        check("Journal: account filter keeps whole entry (EXISTS)",
            j.sql.contains("EXISTS"));

        j = JournalPanel.buildQuery(null, null, "", null, "ASSET");
        check("Journal: type only -> just the type", j.params.equals(List.of("ASSET")));

        j = JournalPanel.buildQuery(null, null, "", null, null);
        check("Journal: no filters -> no params, no EXISTS",
            j.params.isEmpty() && !j.sql.contains("EXISTS"));

        FilterSupport.Query a = AccountsPanel.buildQuery("cash", "ASSET");
        check("Accounts: type then keyword params",
            a.params.equals(List.of("ASSET", "cash", "cash", "cash")));
        check("Accounts: ordered by code", a.sql.contains("ORDER BY code"));
        a = AccountsPanel.buildQuery("", null);
        check("Accounts: no filters -> no params", a.params.isEmpty());

        FilterSupport.Query l = LedgerPanel.buildLedgerQuery(5, end);
        check("Ledger: account then inclusive end date",
            l.params.equals(Arrays.asList(5, end))
                && l.sql.contains("<= ?"));
        l = LedgerPanel.buildLedgerQuery(5, null);
        check("Ledger: no end date -> account only", l.params.equals(List.of(5)));

        FilterSupport.Query list = LedgerPanel.buildAccountListQuery("EXPENSE");
        check("Ledger: account list filtered by type",
            list.params.equals(List.of("EXPENSE")));
        list = LedgerPanel.buildAccountListQuery(null);
        check("Ledger: account list unfiltered", list.params.isEmpty());
    }

    /** True when a SQL keyword is glued to the token before it ("0OR"). */
    static boolean glued(String sql) {
        return java.util.regex.Pattern
            .compile("[0-9?)'](OR|AND|ORDER)\\b")
            .matcher(sql).find();
    }

    // =================================================================
    // 10.4  CSV format
    // =================================================================
    static void csvFormat() {
        section = "10.4 csv format";

        DefaultTableModel model = new DefaultTableModel(
            new String[] {"Reference", "Description", "Amount (PHP)"}, 0);
        model.addRow(new Object[] {1L, "Rent", new BigDecimal("1500.5").setScale(2)});
        model.addRow(new Object[] {2L, null, BigDecimal.ZERO.setScale(2)});
        model.addRow(new Object[] {3L, "Refund", new BigDecimal("-5.00")});

        JTable table = new JTable(model);
        String csv = CsvExporter.buildCsv(table);
        String[] lines = csv.split("\r\n", -1);

        check("header row first", lines[0].equals(
            "\"Reference\",\"Description\",\"Amount (PHP)\""));
        check("one line per row + header + trailing break", lines.length == 5);
        check("amount keeps 2 decimals", lines[1].equals("\"1\",\"Rent\",\"1500.50\""));
        check("null cell is empty, not 'null'", lines[2].equals("\"2\",\"\",\"0.00\""));
        check("negative amount is not altered", lines[3].equals("\"3\",\"Refund\",\"-5.00\""));
        check("lines end with CRLF", csv.endsWith("\r\n") && !csv.contains("\n\n"));

        // Empty results -> header only (no stray rows)
        JTable empty = new JTable(new DefaultTableModel(
            new String[] {"A", "B"}, 0));
        check("empty table exports header only",
            CsvExporter.buildCsv(empty).equals("\"A\",\"B\"\r\n"));
    }

    // =================================================================
    // 10.4  Escaping
    // =================================================================
    static void csvEscaping() {
        section = "10.4 escaping";

        String[] tricky = {
            "plain",
            "has,comma",
            "has \"quotes\" inside",
            "\"starts and ends with quotes\"",
            "line one\nline two",
            "windows\r\nbreak",
            "  leading and trailing  ",
            "",
            "\u20B1 peso and \u00F1 enye",
            "comma, \"quote\" and\nnewline together",
            ",",
            "\"",
        };

        DefaultTableModel model = new DefaultTableModel(
            new String[] {"Text", "Second"}, 0);

        for (String text : tricky) {
            model.addRow(new Object[] {text, "x"});
        }

        JTable table = new JTable(model);
        List<List<String>> parsed = parseCsv(CsvExporter.buildCsv(table));

        check("every row keeps exactly 2 columns (no column shift)",
            parsed.stream().allMatch(row -> row.size() == 2));
        check("row count = header + data", parsed.size() == tricky.length + 1);

        for (int i = 0; i < tricky.length; i++) {
            check("round-trips: " + printable(tricky[i]),
                parsed.get(i + 1).get(0).equals(tricky[i]));
        }

        check("quote is doubled", CsvExporter.escape("a\"b").equals("\"a\"\"b\""));
    }

    // =================================================================
    // 10.4  Formula guard
    // =================================================================
    static void csvFormulaGuard() {
        section = "10.4 formula guard";

        for (String text : new String[] {
            "=SUM(A1:A9)", "+1+1", "-2+3", "@cmd", "  =spaced"
        }) {
            check("text \"" + text + "\" is neutralized",
                CsvExporter.cellText(text).startsWith("'"));
        }

        check("normal text untouched",
            CsvExporter.cellText("Office rent").equals("Office rent"));
        check("text with = in the middle untouched",
            CsvExporter.cellText("a=b").equals("a=b"));
        check("negative number is never prefixed",
            CsvExporter.cellText(new BigDecimal("-12.30"))
                .equals("-12.30"));
        check("negative Long is never prefixed",
            CsvExporter.cellText(-5L).equals("-5"));
        check("null -> empty", CsvExporter.cellText(null).equals(""));
    }

    // =================================================================
    // 10.5  CSV matches what is on screen (filtered + sorted view)
    // =================================================================
    static void csvMatchesScreen() {
        section = "10.5 csv == screen";

        DefaultTableModel model = new DefaultTableModel(
            new String[] {"Reference", "Date", "Description", "Amount (PHP)"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Long.class
                    : column == 3 ? BigDecimal.class : String.class;
            }
        };

        model.addRow(new Object[] {1L, "2026-03-01", "Rent, March", bd("1500.00")});
        model.addRow(new Object[] {2L, "2026-03-15", "Said \"paid\"", bd("20.05")});
        model.addRow(new Object[] {3L, "2026-03-31", "Multi\nline", bd("0.00")});
        model.addRow(new Object[] {4L, "2026-04-01", "Outside the range", bd("99.99")});
        model.addRow(new Object[] {5L, "2026-02-28", "Before the range", bd("7.00")});
        model.addRow(new Object[] {6L, "2026-03-10", "=HYPERLINK()", bd("3.30")});

        JTable table = new JTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Show only March (boundary dates 03-01 and 03-31 included)
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                String d = e.getStringValue(1);
                return d.compareTo("2026-03-01") >= 0 && d.compareTo("2026-03-31") <= 0;
            }
        });

        // ...and the user sorted by amount, high to low.
        sorter.setSortKeys(List.of(new RowSorter.SortKey(3, SortOrder.DESCENDING)));

        List<List<String>> parsed = parseCsv(CsvExporter.buildCsv(table));

        check("CSV has header + exactly the displayed rows",
            parsed.size() == table.getRowCount() + 1 && table.getRowCount() == 4);
        check("boundary dates are included",
            columnValues(parsed, 1).containsAll(
                List.of("2026-03-01", "2026-03-31")));
        check("rows outside the range are NOT exported",
            !columnValues(parsed, 1).contains("2026-04-01")
                && !columnValues(parsed, 1).contains("2026-02-28"));

        for (int row = 0; row < table.getRowCount(); row++) {
            for (int col = 0; col < table.getColumnCount(); col++) {
                String screen = screenText(table.getValueAt(row, col));
                String csv = parsed.get(row + 1).get(col);

                // The only intended difference: leading ' on formula text.
                String normalized = csv.startsWith("'") && csv.length() > 1
                    && "=+-@".indexOf(csv.charAt(1)) >= 0
                    ? csv.substring(1) : csv;

                if (!screen.equals(normalized)) {
                    check("cell [" + row + "," + col + "] screen=\"" + screen
                        + "\" csv=\"" + csv + "\"", false);
                }
            }
        }

        check("every cell matches the screen (same order as sorted view)", true);
        check("sorted high-to-low on screen is the same in the file",
            columnValues(parsed, 3).equals(
                List.of("1500.00", "20.05", "3.30", "0.00")));

        // Nothing matches -> empty results
        sorter.setRowFilter(RowFilter.regexFilter("^no-such-row$", 2));
        check("empty result -> 0 displayed rows", table.getRowCount() == 0);
        check("empty result -> CSV is header only",
            parseCsv(CsvExporter.buildCsv(table)).size() == 1);
    }

    // =================================================================
    // 10.4  Actual file on disk
    // =================================================================
    static void csvFile() throws IOException {
        section = "10.4 file";

        DefaultTableModel model = new DefaultTableModel(
            new String[] {"Description", "Amount"}, 0);
        model.addRow(new Object[] {"Caf\u00E9 \u20B1100, \"big\"", bd("100.00")});

        Path file = Files.createTempFile("export-test", ".csv");

        try {
            CsvExporter.writeCsv(file, new JTable(model));
            byte[] bytes = Files.readAllBytes(file);

            check("file starts with UTF-8 BOM (Excel)",
                bytes.length > 3 && (bytes[0] & 0xFF) == 0xEF
                    && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF);

            String text = new String(bytes, StandardCharsets.UTF_8);
            check("BOM is the first character only", text.charAt(0) == '\uFEFF'
                && text.indexOf('\uFEFF', 1) < 0);

            List<List<String>> parsed = parseCsv(
                text.startsWith("\uFEFF") ? text.substring(1) : text);
            boolean shapeOk = parsed.size() == 2 && parsed.get(1).size() == 2;
            check("header + 1 data row", shapeOk);
            check("accent, peso sign, comma and quotes survive",
                shapeOk && parsed.get(1).get(0)
                    .equals("Caf\u00E9 \u20B1100, \"big\""));
            check("amount survives",
                shapeOk && parsed.get(1).get(1).equals("100.00"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    // =================================================================
    // helpers
    // =================================================================
    static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    static String screenText(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).setScale(2).toPlainString();
        }

        return value.toString();
    }

    static List<String> columnValues(List<List<String>> rows, int column) {
        List<String> values = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            values.add(rows.get(i).get(column));
        }

        return values;
    }

    static String printable(String text) {
        return "\"" + text.replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }

    /** Minimal RFC 4180 reader used to read our own output back. */
    static List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        boolean cellStarted = false;

        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
                cellStarted = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
                cellStarted = true;
            } else if (c == '\r' && i + 1 < csv.length()
                    && csv.charAt(i + 1) == '\n') {
                row.add(cell.toString());
                rows.add(row);
                row = new ArrayList<>();
                cell.setLength(0);
                cellStarted = false;
                i++;
            } else {
                cell.append(c);
                cellStarted = true;
            }
        }

        if (cellStarted || cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }

        return rows;
    }

    static void check(String name, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failures.add(section + ": " + name);
        }
    }
}
