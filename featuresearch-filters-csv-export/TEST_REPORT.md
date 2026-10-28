# Test Report - Project 10: Search, Filters, and CSV Export

Task 10.5: *Test combined filters, empty results, boundary dates, and
CSV-to-screen accuracy.*

## What was tested, and how

| Layer | What it covers | Needs | Result |
|-------|----------------|-------|--------|
| **A. Automated unit tests** (`tests/FilterExportTests.java`) | date parsing/validation, inclusive range SQL, keyword search, account/type filters, **all 32 filter combinations** of the Transactions and Journal queries, CSV format, CSV escaping, formula guard, CSV vs. on-screen table, CSV file on disk | JDK only | **112 passed, 0 failed** |
| **B. SQL logic check** (`tests/sql_logic_check/`) | the *real* queries from the panels run against a small seeded in-memory SQLite database (two shims: `strpos`, `CAST AS DATE`) and compared with hand-worked answers | JDK + Python | **35 passed, 0 failed** |
| **C. Manual check on Supabase** (`tests/MANUAL_CHECKLIST.md`) | the same scenarios through the real app + PostgreSQL, plus opening the exported CSV in Excel | your database | **not run yet - fill in the checklist** |

A and B ran on the final code. A and B do **not** talk to Supabase; layer C is
the only one that does, so please run it before submitting.

## Coverage by requirement

| Requirement | Evidence |
|-------------|----------|
| **10.1** keyword search on transactions, journal, ledger, accounts | A: keyword rules (blank, trim, case, `%`/`_`/quote text, SQL-injection text stays a bound parameter). B: `rent`, `RENT`, `%`, `_`, account-name and reference matches on Transactions / Journal / Accounts. Ledger search is Java-side and covered by manual checks #20. |
| **10.2** inclusive date range | A: SQL uses `>=` and `<=`, never `>` / `<`; rejects `2026-02-30`, `2026-13-01`, `31/01/2026`, etc.; start after end is an error; same-day range allowed. B: a row **on** the start date and a row **on** the end date are both returned; the day before and the day after are not; single-day ranges work. |
| **10.3** account and account-type filters | A: account/type parameter order, "All Types" = no filter. B: Cash (either side), type EXPENSE (either side), type LIABILITY, account+type mismatch -> empty, Journal account filter keeps both lines (stays balanced), Ledger account list by type. |
| **10.4** CSV with headers and escaping | A: header row first; commas, quotes, embedded newlines, leading/trailing spaces, empty text, accents and the peso sign survive a write -> read round trip with no column shift; 2-decimal amounts; `null` is blank; UTF-8 BOM; CRLF line ends. |
| **10.5** combined filters | A: 32 combinations build consistent SQL. B: dates + account + type + keyword together -> exactly the expected row. |
| **10.5** empty results | A: empty table -> header-only CSV text. B: no-match keyword and contradictory filters -> 0 rows. In the app, Export on an empty table shows a message and writes no file. |
| **10.5** boundary dates | A + B (see 10.2 above), including both boundary days with filtering and sorting active. |
| **10.5** CSV-to-screen accuracy | A: a table is filtered (March only, both boundary days) and sorted (amount high-to-low); every cell of the CSV is compared with the cell on screen, in the same row order; hidden rows are absent. |

## Problems found by testing (and fixed)

1. **`instr()` does not exist in PostgreSQL.** The original search in
   `TransactionsPanel` and `JournalPanel` used SQLite's `instr()`; it would
   have raised an error on Supabase. Replaced with `strpos()`.
2. **Dates were bound as text.** Comparing a `DATE` column with a text
   parameter fails on PostgreSQL. Dates are now bound as real dates and the
   column is cast with `CAST(... AS DATE)`, so it works for DATE, timestamp
   or `YYYY-MM-DD` text columns (a timestamp on the end date is still
   included).
3. **Glued SQL keyword (`0OR EXISTS`)** in my first version of the Journal
   keyword query, caused by a Java text block trimming a leading space. Found
   by test layer B, fixed, and a regression test was added to layer A.
4. `%` and `_` typed in the search box are treated as ordinary text (not
   wildcards).
5. Transactions had its own copy of the CSV code; it now uses the shared
   `CsvExporter`, so every panel exports the same way.

## Test-quality check

The unit tests were "mutation tested": bugs were put into the code on purpose
and the suite was confirmed to fail for each - start date made exclusive, end
date made exclusive, `instr` instead of `strpos`, quotes not doubled, no
header row, keyword not trimmed, BOM removed, glued `0OR`. All were caught.

## Known limitations / design decisions

- **Formula guard:** text cells that start with `=`, `+`, `-` or `@` get a
  leading `'` so Excel cannot run them as formulas. Numbers are never changed.
  To turn this off set `NEUTRALIZE_FORMULAS = false` in `CsvExporter`.
- **Account filter meaning:** on Transactions, "account" and "type" match the
  debit **or** credit side. On the Journal they keep **whole entries** that use
  a matching account, so debits and credits still balance on screen.
- **Ledger keyword:** hides non-matching rows but the running balance, period
  totals and closing balance still include every entry (the summary says
  "Entries shown: x of y").
- **Export = what is on screen.** If you type new filter text but do not press
  Apply, Export still exports the rows currently displayed.
- Not covered by automated tests: the Swing windows themselves and the
  Supabase connection (see layer C).

## How to re-run

```
tests\run_tests.bat            (Windows)      sh tests/run_tests.sh   (Mac/Linux)
```
Expected last line: `Passed: 112   Failed: 0`

Optional SQL logic check: see the header of `tests/sql_logic_check/sqlcheck.py`.
