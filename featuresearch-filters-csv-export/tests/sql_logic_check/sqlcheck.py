"""
Optional extra check: runs the REAL queries produced by the panels against a
small in-memory SQLite database and compares the rows with hand-worked answers.

SQLite is only a stand-in (two tiny shims: strpos() and CAST(x AS DATE)), so
this checks the filter LOGIC (boundaries, AND-combinations, wildcards, account
vs. type). It does not prove anything about Supabase itself - use
tests/MANUAL_CHECKLIST.md for that.

From the project folder:
  javac -d test-out -cp lib/postgresql-42.7.14.jar src/*.java tests/sql_logic_check/QueryDump.java
  java -Djava.awt.headless=true -cp test-out QueryDump > tests/sql_logic_check/dump.txt
  python tests/sql_logic_check/sqlcheck.py
"""
import os
os.chdir(os.path.dirname(os.path.abspath(__file__)))
import sqlite3, re, sys

db = sqlite3.connect(":memory:")
db.create_function("strpos", 2, lambda a, b: (a or "").find(b) + 1)
db.executescript("""
CREATE TABLE accounts(id INTEGER PRIMARY KEY, code TEXT, name TEXT, type TEXT);
CREATE TABLE transactions(id INTEGER PRIMARY KEY, transaction_date TEXT, description TEXT);
CREATE TABLE journal_lines(id INTEGER PRIMARY KEY AUTOINCREMENT, transaction_id INT, account_id INT,
                           debit_centavos INT, credit_centavos INT);
INSERT INTO accounts VALUES
 (1,'1000','Cash','ASSET'),(2,'4000','Sales Revenue','REVENUE'),(3,'5000','Rent Expense','EXPENSE'),
 (4,'5100','Supplies Expense','EXPENSE'),(5,'2000','Accounts Payable','LIABILITY'),(6,'3000','Owner''s Capital','EQUITY');
INSERT INTO transactions VALUES
 (1,'2026-03-01','Owner investment'),      -- ON start boundary
 (2,'2026-03-15','March rent'),
 (3,'2026-03-31','Sold goods, cash'),      -- ON end boundary
 (4,'2026-04-01','April rent'),            -- day after
 (5,'2026-02-28','Bought supplies on credit'), -- day before
 (6,'2026-03-10','100% discount_promo');   -- wildcard characters
-- (tx, debit acct, credit acct, centavos)
""")
for tx, dr, cr, amt in [(1,1,6,10000000),(2,3,1,1500000),(3,1,2,3000000),(4,3,1,1500000),(5,4,5,500000),(6,4,1,200000)]:
    db.execute("INSERT INTO journal_lines(transaction_id,account_id,debit_centavos,credit_centavos) VALUES(?,?,?,0)",(tx,dr,amt))
    db.execute("INSERT INTO journal_lines(transaction_id,account_id,debit_centavos,credit_centavos) VALUES(?,?,0,?)",(tx,cr,amt))

def shim(sql):  # dialect shim: PostgreSQL CAST(x AS DATE) -> SQLite date(x)
    return re.sub(r"CAST\(([\w\.]+) AS DATE\)", r"date(\1)", sql)

def params(raw):
    out=[]
    for p in (raw.split("\t") if raw else []):
        k,v=p[0],p[2:]
        out.append(int(v) if k=="I" else v)
    return out

queries={}
for line in open("dump.txt", encoding="utf-8").read().splitlines():
    name,sql,raw=line.split("\u0001"); queries[name]=(sql,raw)

def run(name):
    sql,raw=queries[name]; p=params(raw)
    assert sql.count("?")==len(p), name
    return db.execute(shim(sql),p).fetchall()

def tx_ids(name):  return sorted({r[0] for r in run(name)})   # col0 = transaction id
def ids_of(name):  return sorted(r[0] for r in run(name))

fails=0
def expect(label, got, want):
    global fails
    ok = got==want
    fails += (not ok)
    print(("PASS " if ok else "FAIL ")+label.ljust(58)+f" got={got}" + ("" if ok else f"  expected={want}"))

print("== Transactions list ==")
expect("T-march: dates 03-01..03-31 incl. both boundaries", tx_ids("T-march"), [1,2,3,6])
expect("T-day-end: single day 03-31 (end boundary row)",    tx_ids("T-day-end"), [3])
expect("T-day-start: single day 03-01 (start boundary row)",tx_ids("T-day-start"), [1])
expect("T-kw-rent: keyword 'rent'",                         tx_ids("T-kw-rent"), [2,4])
expect("T-kw-RENT: '  RENT ' (case + spaces)",              tx_ids("T-kw-RENT"), [2,4])
expect("T-kw-pct: '%' is literal, not wildcard",            tx_ids("T-kw-pct"), [6])
expect("T-kw-under: '_' is literal, not wildcard",          tx_ids("T-kw-under"), [6])
expect("T-acct-cash: debit OR credit account = Cash",       tx_ids("T-acct-cash"), [1,2,3,4,6])
expect("T-type-exp: either side is an EXPENSE account",     tx_ids("T-type-exp"), [2,4,5,6])
expect("T-combined: March + Cash + EXPENSE + 'rent'",       tx_ids("T-combined"), [2])
expect("T-empty: keyword matches nothing",                  tx_ids("T-empty"), [])
expect("T-liab-march: LIABILITY only tx is in Feb -> empty",tx_ids("T-liab-march"), [])
expect("T-kw-capital: matches account name",                tx_ids("T-kw-capital"), [1])
expect("T-kw-id: '3' matches ref 3 AND account code 3000", tx_ids("T-kw-id"), [1,3])
expect("T-all: no filters -> all 6",                        tx_ids("T-all"), [1,2,3,4,5,6])
r=run("T-march"); expect("T-march rows newest first (dates)", [x[1] for x in r], ["2026-03-31","2026-03-15","2026-03-10","2026-03-01"])

print("\n== General Journal ==")
def lines(n): return len(run(n))
def balanced(n):
    r=run(n); return sum(x[4] for x in r)==sum(x[5] for x in r)
expect("J-march: 4 entries x 2 lines",           (tx_ids("J-march"), lines("J-march")), ([1,2,3,6],8))
expect("J-acct-rent: entries that use Rent acct",(tx_ids("J-acct-rent"), lines("J-acct-rent")), ([2,4],4))
expect("J-acct-rent stays balanced (both lines)",balanced("J-acct-rent"), True)
expect("J-type-liab",                            (tx_ids("J-type-liab"), lines("J-type-liab")), ([5],2))
expect("J-kw-payable: matches an account on entry",tx_ids("J-kw-payable"), [5])
expect("J-kw-expense: matches account name/type",tx_ids("J-kw-expense"), [2,4,5,6])
expect("J-combined: March + EXPENSE + 'rent'",   (tx_ids("J-combined"), lines("J-combined")), ([2],2))
expect("J-kw-pct: '%' literal",                  tx_ids("J-kw-pct"), [6])
expect("J-acct+type-mismatch: Rent acct but ASSET",tx_ids("J-acct+type-mismatch"), [])
expect("J-all: all 12 lines, balanced",          (lines("J-all"), balanced("J-all")), (12, True))
r=run("J-march"); expect("J-march oldest first, debit before credit", [(x[0],x[4]>0) for x in r][:4], [(1,True),(1,False),(6,True),(6,False)])

print("\n== Chart of Accounts ==")
expect("A-kw-expense: code/name/type search",    ids_of("A-kw-expense"), [3,4])
expect("A-type-asset",                           ids_of("A-type-asset"), [1])
expect("A-cash-expense: AND of both -> empty",   ids_of("A-cash-expense"), [])
expect("A-all sorted by code",                   [x[1] for x in run("A-all")], ["1000","2000","3000","4000","5000","5100"])

print("\n== General Ledger ==")
expect("L-cash-march: Cash entries through 03-31, chronological (04-01 excluded)", [x[0] for x in run("L-cash-march")], [1,6,2,3])
expect("L-cash-all: no end date -> includes 04-01 entry (chronological)", [x[0] for x in run("L-cash-all")], [1,6,2,3,4])
expect("L-list-exp: account dropdown by type",   ids_of("L-list-exp"), [3,4])
expect("L-list-all",                             len(run("L-list-all")), 6)

print(f"\n{'ALL PASSED' if not fails else str(fails)+' FAILED'}")
sys.exit(1 if fails else 0)
