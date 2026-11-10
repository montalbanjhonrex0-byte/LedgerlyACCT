LEDGERLY - MERGED PROJECT (Journal back-end + Columnar + Accounting)
====================================================================

HOW TO RUN
  Easy way (Windows): double-click  run.bat
  VS Code: File > Open Folder > choose this folder (ACCT-F-B_MERGED),
           open Front-end\LedgerlyAccounting.java, press Run.
  To run the back-end tests: double-click  run-tests.bat

FOLDERS
  Front-end            the screens (LedgerlyAccounting is the MAIN program, LedgerlyColumnar is the interactive sheet)
  Back-end For Journal journal rules, ID numbers (JE-001...), account catalog, and the web routes
  Back-end For Columnar the 12-column worksheet math (WorksheetBackend)

WHAT WAS MERGED (all new lines are marked)
  Look for:  MERGED CODE (NEW ADDITION)
  No old line was changed or deleted. Only new lines were added.

  1. Journal tab > "+ Add Entry"
     - The entry is checked by the back-end (Debits must equal Credits, account type must match the catalog).
     - If it is wrong, you see a clear message and nothing is saved.
     - Every saved entry gets a number: JE-001, JE-002, JE-003 ...
  2. 12-Column Worksheet tab
     - It is READ-ONLY. It is rebuilt from the journal entries of the open workbook every time you open it
       (Account Name, Type, Trial Balance), so nothing can be typed or changed there.
     - The Reload, View Summary, Finalize, Clear and Delete Workbook buttons were removed.
  3. Journal web routes start together with the app (localhost only), at
     http://localhost:8765/api/journal   (the port is printed in the console)
     Send the header  X-User-Email: your@email.com  (an account you signed up with).
       GET  /api/journal      GET /api/journal/JE-001      POST /api/journal      PUT /api/journal/JE-001
       GET  /api/accounts

NOTES
  - Entries sent by POST/PUT through the web routes are kept in memory only (they are lost when the app closes).
    Entries made on the Journal screen are saved in the workbook as before.
  - JournalService.java got one new method (importSaved). It is marked too.

LATEST CHANGES
  - Account menu (top right): the extra "Log out" was removed (the sidebar "Log out" stays).
  - Add Entry: Date, Description, then two Account Titles side by side (left = DEBIT, right = CREDIT),
    the Account Types under them, and one Amount (used for both sides).
  - Journal list: one date for the two accounts and one description under the credit line.
    Clicking a line highlights the whole entry. Sorting by column header is off in the Journal so entries stay together.
  - Journal: "Delete Workbook" was replaced by "Delete Entry" (deletes the selected entry; Delete key works too).
  - Ledger and 12-Column Worksheet: "Delete Workbook" removed. Deleting a workbook is now only on the Dashboard.
  - JournalService.java got one more new method (delete), marked.
