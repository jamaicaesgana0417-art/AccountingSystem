import java.time.LocalDate;
import java.util.*;

public class QueryDump {
    static void out(String name, FilterSupport.Query q) {
        StringBuilder p = new StringBuilder();
        for (Object o : q.params) {
            if (p.length() > 0) p.append('\t');
            if (o instanceof LocalDate) p.append("D:").append(o);
            else if (o instanceof Integer) p.append("I:").append(o);
            else p.append("S:").append(o);
        }
        System.out.println(name + "\u0001" + q.sql.replaceAll("\\s+", " ") + "\u0001" + p);
    }
    static LocalDate d(String s) { return LocalDate.parse(s); }

    public static void main(String[] a) {
        LocalDate m1 = d("2026-03-01"), m31 = d("2026-03-31");
        out("T-march",   TransactionsPanel.buildQuery(m1, m31, "", null, null));
        out("T-day-end", TransactionsPanel.buildQuery(m31, m31, "", null, null));
        out("T-day-start", TransactionsPanel.buildQuery(m1, m1, "", null, null));
        out("T-kw-rent", TransactionsPanel.buildQuery(null, null, "rent", null, null));
        out("T-kw-RENT", TransactionsPanel.buildQuery(null, null, "  RENT ", null, null));
        out("T-kw-pct",  TransactionsPanel.buildQuery(null, null, "%", null, null));
        out("T-kw-under",TransactionsPanel.buildQuery(null, null, "_", null, null));
        out("T-acct-cash", TransactionsPanel.buildQuery(null, null, "", 1, null));
        out("T-type-exp", TransactionsPanel.buildQuery(null, null, "", null, "EXPENSE"));
        out("T-combined", TransactionsPanel.buildQuery(m1, m31, "rent", 1, "EXPENSE"));
        out("T-empty",   TransactionsPanel.buildQuery(null, null, "zzz", null, null));
        out("T-liab-march", TransactionsPanel.buildQuery(m1, m31, "", null, "LIABILITY"));
        out("T-kw-capital", TransactionsPanel.buildQuery(null, null, "capital", null, null));
        out("T-kw-id",   TransactionsPanel.buildQuery(null, null, "3", null, null));
        out("T-all",     TransactionsPanel.buildQuery(null, null, "", null, null));

        out("J-march",   JournalPanel.buildQuery(m1, m31, "", null, null));
        out("J-acct-rent", JournalPanel.buildQuery(null, null, "", 3, null));
        out("J-type-liab", JournalPanel.buildQuery(null, null, "", null, "LIABILITY"));
        out("J-kw-payable", JournalPanel.buildQuery(null, null, "payable", null, null));
        out("J-kw-expense", JournalPanel.buildQuery(null, null, "expense", null, null));
        out("J-combined", JournalPanel.buildQuery(m1, m31, "rent", null, "EXPENSE"));
        out("J-kw-pct",  JournalPanel.buildQuery(null, null, "%", null, null));
        out("J-acct+type-mismatch", JournalPanel.buildQuery(null, null, "", 3, "ASSET"));
        out("J-all",     JournalPanel.buildQuery(null, null, "", null, null));

        out("A-kw-expense", AccountsPanel.buildQuery("expense", null));
        out("A-type-asset", AccountsPanel.buildQuery("", "ASSET"));
        out("A-cash-expense", AccountsPanel.buildQuery("cash", "EXPENSE"));
        out("A-all", AccountsPanel.buildQuery("", null));

        out("L-cash-march", LedgerPanel.buildLedgerQuery(1, m31));
        out("L-cash-all",   LedgerPanel.buildLedgerQuery(1, null));
        out("L-list-exp",   LedgerPanel.buildAccountListQuery("EXPENSE"));
        out("L-list-all",   LedgerPanel.buildAccountListQuery(null));
    }
}
