import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class CsvExporter {

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

        try (
            BufferedWriter writer = Files.newBufferedWriter(
                path, StandardCharsets.UTF_8
            )
        ) {
            writer.write('\uFEFF');

            for (int column = 0; column < table.getColumnCount(); column++) {
                if (column > 0) {
                    writer.write(",");
                }

                writer.write(escape(table.getColumnName(column)));
            }

            writer.write("\r\n");

            for (int row = 0; row < table.getRowCount(); row++) {
                for (
                    int column = 0;
                    column < table.getColumnCount();
                    column++
                ) {
                    if (column > 0) {
                        writer.write(",");
                    }

                    Object value = table.getValueAt(row, column);
                    String text;

                    if (value instanceof BigDecimal) {
                        text = ((BigDecimal) value)
                            .setScale(2).toPlainString();
                    } else {
                        text = value == null ? "" : value.toString();

                        // Keep user-entered text from becoming a formula.
                        String trimmed = text.stripLeading();

                        if (!trimmed.isEmpty()
                                && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
                            text = "'" + text;
                        }
                    }

                    writer.write(escape(text));
                }

                writer.write("\r\n");
            }

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

    private static String escape(String text) {
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}