import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Exports exactly what a JTable currently shows (the filtered rows, in the
 * order the user sees them) to a CSV file.
 *
 * Format (RFC 4180 style):
 *  - first line is the header row (the visible column names)
 *  - every field is wrapped in double quotes
 *  - a double quote inside a field is doubled ("")
 *  - commas and line breaks inside a field are safe because of the quotes
 *  - lines end with CRLF
 *  - file starts with a UTF-8 BOM so Excel reads symbols correctly
 */
public class CsvExporter {

    /**
     * Text that starts with = + - @ can run as a formula when the CSV is
     * opened in Excel, so a leading ' is added to TEXT cells. Numbers are
     * never changed. Set to false for a byte-for-byte export.
     */
    static final boolean NEUTRALIZE_FORMULAS = true;

    static final String LINE_END = "\r\n";

    // ---------------------------------------------------------------
    // GUI entry point
    // ---------------------------------------------------------------

    public static void export(
        Component parent,
        JTable table,
        String filename
    ) {
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(
                parent, "There are no displayed rows to export."
            );
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export CSV");
        chooser.setFileFilter(
            new FileNameExtensionFilter("CSV files", "csv")
        );
        chooser.setSelectedFile(new java.io.File(filename));

        if (chooser.showSaveDialog(parent)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = chooser.getSelectedFile().toPath();

        if (!path.getFileName().toString()
                .toLowerCase(Locale.ROOT).endsWith(".csv")) {
            path = path.resolveSibling(path.getFileName() + ".csv");
        }

        if (Files.exists(path)) {
            int answer = JOptionPane.showConfirmDialog(
                parent,
                "Replace the existing file?",
                "Confirm Replacement",
                JOptionPane.YES_NO_OPTION
            );

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try {
            writeCsv(path, table);

        } catch (IOException exception) {
            exception.printStackTrace();

            JOptionPane.showMessageDialog(
                parent,
                "Export failed:\n" + exception.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JOptionPane.showMessageDialog(
            parent,
            "Exported " + table.getRowCount()
                + " rows to:\n" + path.toAbsolutePath()
        );
    }

    // ---------------------------------------------------------------
    // File + text building (no dialogs, so these can be unit tested)
    // ---------------------------------------------------------------

    static void writeCsv(Path path, JTable table) throws IOException {
        try (
            BufferedWriter writer = Files.newBufferedWriter(
                path, StandardCharsets.UTF_8
            )
        ) {
            writer.write('\uFEFF');
            writer.write(buildCsv(table));
        }
    }

    /**
     * Header line plus one line per VISIBLE row (the view order, so a
     * filtered or sorted table exports exactly as it appears on screen).
     * An empty table produces just the header line.
     */
    static String buildCsv(JTable table) {
        StringBuilder out = new StringBuilder();

        int columns = table.getColumnCount();

        for (int column = 0; column < columns; column++) {
            if (column > 0) {
                out.append(',');
            }

            out.append(escape(table.getColumnName(column)));
        }

        out.append(LINE_END);

        for (int row = 0; row < table.getRowCount(); row++) {
            for (int column = 0; column < columns; column++) {
                if (column > 0) {
                    out.append(',');
                }

                out.append(escape(cellText(table.getValueAt(row, column))));
            }

            out.append(LINE_END);
        }

        return out.toString();
    }

    static String cellText(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof BigDecimal) {
            return ((BigDecimal) value)
                .setScale(2, RoundingMode.HALF_UP).toPlainString();
        }

        String text = value.toString();

        if (NEUTRALIZE_FORMULAS && value instanceof String) {
            text = neutralizeFormula(text);
        }

        return text;
    }

    private static String neutralizeFormula(String text) {
        String trimmed = text.stripLeading();

        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
            return "'" + text;
        }

        // Tab / carriage return can also start a formula in some programs.
        if (!text.isEmpty()
                && (text.charAt(0) == '\t' || text.charAt(0) == '\r')) {
            return "'" + text;
        }

        return text;
    }

    static String escape(String text) {
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
