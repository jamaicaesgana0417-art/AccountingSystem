# Manual checklist - run against your real Supabase database

The automated tests cannot connect to Supabase, so this short checklist covers
what only the real app + database can prove. It uses test data dated in the
year **2099** and descriptions starting with **P10TEST**, so it cannot mix with
real records and is easy to delete afterwards.

## 1. Load the test data (Supabase SQL editor)

```sql
INSERT INTO accounts (code, name, type) VALUES
  ('P10-1000', 'P10 Cash',             'ASSET'),
  ('P10-2000', 'P10 Accounts Payable', 'LIABILITY'),
  ('P10-3000', 'P10 Owner Capital',    'EQUITY'),
  ('P10-4000', 'P10 Sales Revenue',    'REVENUE'),
  ('P10-5000', 'P10 Rent Expense',     'EXPENSE'),
  ('P10-5100', 'P10 Supplies Expense', 'EXPENSE');

INSERT INTO transactions (transaction_date, description) VALUES
  ('2099-03-01', 'P10TEST Owner investment'),       -- ON the start boundary
  ('2099-03-15', 'P10TEST March rent'),
  ('2099-03-31', 'P10TEST Sold goods, cash'),       -- ON the end boundary
  ('2099-04-01', 'P10TEST April rent'),             -- day after
  ('2099-02-28', 'P10TEST Supplies on credit'),     -- day before
  ('2099-03-10', 'P10TEST 100% discount_promo');    -- % and _ in the text

INSERT INTO journal_lines (transaction_id, account_id, debit_centavos, credit_centavos)
SELECT t.id, a.id, v.dr, v.cr
FROM (VALUES
  ('P10TEST Owner investment',      'P10-1000', 10000000,       0),
  ('P10TEST Owner investment',      'P10-3000',        0, 10000000),
  ('P10TEST March rent',            'P10-5000',  1500000,       0),
  ('P10TEST March rent',            'P10-1000',        0,  1500000),
  ('P10TEST Sold goods, cash',      'P10-1000',  3000000,       0),
  ('P10TEST Sold goods, cash',      'P10-4000',        0,  3000000),
  ('P10TEST April rent',            'P10-5000',  1500000,       0),
  ('P10TEST April rent',            'P10-1000',        0,  1500000),
  ('P10TEST Supplies on credit',    'P10-5100',   500000,       0),
  ('P10TEST Supplies on credit',    'P10-2000',        0,   500000),
  ('P10TEST 100% discount_promo',   'P10-5100',   200000,       0),
  ('P10TEST 100% discount_promo',   'P10-1000',        0,   200000)
) AS v(descr, code, dr, cr)
JOIN transactions t ON t.description = v.descr
JOIN accounts a     ON a.code = v.code;
```

If an INSERT complains about a missing column, your tables have extra required
columns (for example `accounts.active`) - add them to the column list.

## 2. Run these checks in the app

Tick the box and write what you actually saw. "TX n" = the transaction number
from the list above (1 = Owner investment ... 6 = 100% discount_promo).

| # | Panel | Do this | Expected | OK |
|---|-------|---------|----------|----|
| 1 | Transactions | Search `P10TEST` | all 6 rows | [ ] |
| 2 | Transactions | From `2099-03-01` To `2099-03-31` | TX 1, 2, 3, 6 (**both boundary days included**; TX 4 and 5 not) | [ ] |
| 3 | Transactions | From and To both `2099-03-31` | only TX 3 | [ ] |
| 4 | Transactions | From and To both `2099-03-01` | only TX 1 | [ ] |
| 5 | Transactions | Search `rent`, dates 2099-02-28..2099-04-01 | TX 2 and 4 | [ ] |
| 6 | Transactions | Search `MARCH Rent` (mixed case), dates 2099-02-28..2099-04-01 | only TX 2 (search ignores upper/lower case) | [ ] |
| 7 | Transactions | Search `%` plus dates 2099-02-28..2099-04-01 | only TX 6 (`%` is plain text) | [ ] |
| 8 | Transactions | Account = `P10-1000 - P10 Cash`, dates 2099-02-28..2099-04-01 | TX 1, 2, 3, 4, 6 | [ ] |
| 9 | Transactions | Type = EXPENSE, dates 2099-02-28..2099-04-01 | TX 2, 4, 5, 6 | [ ] |
| 10 | Transactions | Dates 2099-03-01..2099-03-31, Account = P10 Cash, Type = EXPENSE, search `rent` | only TX 2 (all filters combined) | [ ] |
| 11 | Transactions | Search `zzzzqqq` | empty table, count shows 0 | [ ] |
| 12 | Transactions | From `2099-04-01` To `2099-03-01` | warning "Start date must not be after end date" | [ ] |
| 13 | Transactions | From `2099-02-30` | warning "Enter valid dates" | [ ] |
| 14 | Journal | Dates 2099-03-01..2099-03-31 | TX 1, 2, 3, 6 = 8 lines, debits = credits | [ ] |
| 15 | Journal | Account = `P10-5000 - P10 Rent Expense` | TX 2 and 4, **both lines of each**, debits = credits | [ ] |
| 16 | Journal | Type = LIABILITY, dates 2099-02-28..2099-04-01 | TX 5 (2 lines) | [ ] |
| 17 | Journal | Search `payable`, dates 2099-02-28..2099-04-01 | TX 5 (matches an account on the entry) | [ ] |
| 18 | Ledger | Type = EXPENSE | account list shows only P10-5000 and P10-5100 (and your other expense accounts) | [ ] |
| 19 | Ledger | Account = P10 Cash, From 2099-03-01 To 2099-03-31 | opening 0.00; entries TX 1, 6, 2, 3 in date order with running balance 100,000.00 -> 98,000.00 -> 83,000.00 -> 113,000.00 Dr; closing **113,000.00 Dr** (TX 4 is after the end date) | [ ] |
| 20 | Ledger | Same as 19 + search `rent` | only TX 2 listed (its balance column still reads 83,000.00 Dr); **closing still 113,000.00 Dr**; summary says "Entries shown: 1 of 4" | [ ] |
| 21 | Ledger | Account = P10 Cash, From `2099-03-15`, no end date | opening **98,000.00 Dr** (TX 1 and 6 are before 03-15); entries TX 2, 3, 4; closing **98,000.00 Dr** | [ ] |
| 22 | Accounts | Search `P10` | the 6 P10 accounts | [ ] |
| 23 | Accounts | Search `expense` + Type = EXPENSE | P10-5000 and P10-5100 (plus any real expense accounts) | [ ] |
| 24 | Accounts | Search `p10` + Type = ASSET | only P10-1000 | [ ] |

How the Ledger numbers work (PHP): P10 Cash has TX 1 = +100,000.00 (debit),
TX 6 = -2,000.00, TX 2 = -15,000.00, TX 3 = +30,000.00 and TX 4 = -15,000.00
(credits). Date order is TX 1 (03-01), TX 6 (03-10), TX 2 (03-15), TX 3 (03-31),
TX 4 (04-01).

## 3. CSV export checks (do on at least Transactions and Ledger)

| # | Do this | Expected | OK |
|---|---------|----------|----|
| 25 | Apply check #2, click **Export CSV**, open the file in Excel **and** Notepad | first line is the headers; exactly 4 data rows; same order as the screen | [ ] |
| 26 | Look at the `P10TEST Sold goods, cash` row | the comma stays inside **one** cell (no extra column) | [ ] |
| 27 | Look at the `100% discount_promo` row | text unchanged | [ ] |
| 28 | Apply check #11 (no results), click Export CSV | message "There are no displayed rows to export", no file created | [ ] |
| 29 | Sort the table by Amount (click the header), export again | file order matches the sorted screen | [ ] |
| 30 | Export Ledger (#19) | file contains the Opening balance row, entries, and Closing balance row; Debit/Credit/Balance columns have 2 decimals | [ ] |

## 4. Clean up

```sql
DELETE FROM journal_lines
 WHERE transaction_id IN (SELECT id FROM transactions WHERE description LIKE 'P10TEST%');
DELETE FROM transactions WHERE description LIKE 'P10TEST%';
DELETE FROM accounts     WHERE code LIKE 'P10-%';
```
