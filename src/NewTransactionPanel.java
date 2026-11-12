import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class NewTransactionPanel extends JPanel {

    private static final Color NAVY = new Color(25, 47, 96);
    private static final Color PAGE_BG = new Color(244, 246, 255);
    private static final Color INPUT_BG = new Color(243, 246, 255);
    private static final Color BORDER = new Color(221, 228, 245);
    private static final Color TEXT = new Color(31, 50, 91);
    private static final Color MUTED = new Color(104, 120, 153);
    private static final Color BLUE = new Color(55, 91, 255);

    private final JTextField dateField =
        new JTextField(LocalDate.now().toString());

    private final JButton calendarButton = new JButton(new CalendarIcon());

    private final JTextField descriptionField = new JTextField();

    private final JComboBox<AccountOption> debitBox =
        new JComboBox<>();

    private final JComboBox<AccountOption> creditBox =
        new JComboBox<>();

    private final JTextField amountField = new JTextField();

    private final JButton postButton =
        new JButton("Confirm & Post");

    private final JLabel debitTotalLabel =
        new JLabel("₱ 0.00");

    private final JLabel creditTotalLabel =
        new JLabel("₱ 0.00");

    private final JLabel differenceLabel =
        new JLabel("₱ 0.00");


    public NewTransactionPanel() {
        setLayout(new BorderLayout(0, 24));
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 28, 30));

        styleInput(dateField);
        styleInput(descriptionField);
        styleInput(amountField);
        styleInput(debitBox);
        styleInput(creditBox);

        stylePostButton();

        JLabel heading = new JLabel("NEW TRANSACTION");
        heading.setFont(new Font("Inter", Font.BOLD, 24));
        heading.setForeground(NAVY);

        JLabel subtitle = new JLabel(
            "Record a transaction and keep your books up to date."
        );
        subtitle.setFont(new Font("Inter", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(
            new BoxLayout(titleBlock, BoxLayout.Y_AXIS)
        );
        titleBlock.add(heading);
        titleBlock.add(Box.createVerticalStrut(7));
        titleBlock.add(subtitle);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        JPanel detailsCard = buildDetailsCard();
        JPanel previewCard = buildPreviewCard();

        GridBagConstraints cardConstraints = new GridBagConstraints();
        cardConstraints.gridy = 0;
        cardConstraints.anchor = GridBagConstraints.NORTH;
        cardConstraints.weighty = 1;

        cardConstraints.gridx = 0;