# Aircraft Maintenance Tracker

Enhanced from my CS 360 Event Tracking app for ePortfolio Category 1: Software Design/Engineering

## Design Decisions

- Chose MVVM over keeping the original Activity-based design, because each Activity mixed screen, validation, and database code, which made the app hard to test and change.
- Moved repeated string constants and the -1 "no ID" value into one AppConstants class, so each value is defined once and a typo can't cause a mismatch between screens.
- Added a SessionManager class to track the logged-in user in one place. Screens no longer pass the user ID to each other through Intents, which removes a way for the wrong ID to reach a screen.
- Moved all input rules into a Validator class with no Android code, so the rules live in one place and can be unit tested. The current time is passed in rather than read inside the method, so tests always give the same result.
- The past-date rule only applies to new tasks. Existing tasks can have past due dates, since overdue tasks still need to be edited.
- Renamed Event to MaintenanceTask and added task type and status fields. Used Android Studio's Refactor > Rename so every reference updated safely.
- Rewrote onUpgrade() to add new columns with ALTER TABLE instead of dropping all tables, so existing data survives the upgrade. Old events with no location are marked UNASSIGNED for review instead of being deleted.
- Combined the repeated row-reading code in DatabaseHelper into one cursorToTask() method.
- Found that every DatabaseHelper method closes the database after each query, which is inefficient. Planned for the Databases enhancement.
- Updated the dashboard card and detail screen to show tail number, task type, and status, so the app reads as a maintenance tracker throughout.
- Added a TaskRepository interface and a SqliteTaskRepository class. The repository is the only class that talks to DatabaseHelper, and it runs all database work on one background thread (ExecutorService) so the screen never freezes. Results return to the main thread through callbacks.
- ViewModels will depend on the TaskRepository interface rather than the SQLite class, so tests can use a fake repository without a database.
- Moved all reminder code into one ReminderScheduler class. Reminders now fire 60 minutes before the due time instead of at a hard-coded 9:00 AM, and the repository always cancels a task's reminder when it deletes the task, which fixes the bug where deleting from the detail screen left the reminder active.
- Replaced the Calendar date parsing with java.time and one shared date format in AppConstants.
- Added ViewModels with LiveData. TaskListViewModel handles the dashboard (loading, sorting by real due date, flagging overdue tasks, deleting). TaskViewModel handles a single task and is shared by the add/edit and detail screens, so that logic isn't duplicated.
- The dashboard now sorts by real dates in the ViewModel, which fixes tasks appearing out of order across years, even though the database still stores dates as text (to be fixed in the Databases enhancement).
- ViewModels receive the current time through a Supplier, so overdue checks can be tested with a fixed time.
- Login and registration still query the database on the main thread. Moving them to a UserRepository is planned for the Databases enhancement.
- The Activities now only display data from the ViewModels and pass user actions to them. They no longer use DatabaseHelper or AlarmManager directly. AddEditEventActivity went from about 440 lines to about 230.
- Delete logic, which was written three times, now exists once in the repository. This fixed the bug where deleting from the detail screen left the reminder active.
- Replaced the deprecated startActivityForResult with reloading in onResume(), so every screen shows the latest data when it reappears.
- The detail screen receives a task ID and loads the task through the ViewModel, instead of receiving a copy that could be out of date.
- Overdue tasks show in red and completed tasks are faded. The ViewModel decides what's overdue, and the adapter only displays it.
- If exact alarm permission is off, saving asks the user to open Settings or save without a reminder, instead of showing a dialog and closing the screen behind it.
- Added keyword search to the dashboard. It filters the tasks the ViewModel already loaded, matching name, tail number, notes, type, or status, so no database change was needed. This is a linear scan, which is fine for one user's tasks. A database-level search with an index is planned for the Databases enhancement.
- The search text lives in the ViewModel, so an active search survives a screen rotation.
- Tested both ViewModels with a FakeTaskRepository that stores tasks in a list, so the 26 ViewModel tests run in seconds without a device or database. This works because the ViewModels depend on the TaskRepository interface. Tests use a fixed time so overdue checks are repeatable.
- Two tests use the exact dates that sorted wrong in the original app(January 2027 before December 2026, and 1:00 PM before 11:00 AM) to prove the sorting bug is fixed on screen.
- Total test count went from 1 placeholder test to 48 real tests.
- Final cleanup from the code review checklist: removed the duplicate SEND_SMS permission, the default test stubs, an unused DatabaseHelper in SettingsActivity, the unused Parcelable code in MaintenanceTask, and unused imports. Replaced the hard-coded phone number with a named constant and renamed showSystemNotification() to showReminderToast() so the name matches what it does.
- Left for the Databases enhancement: password hashing, ownership checks on updates and deletes, storing dates as timestamps, indexes, backup exclusion, keeping the database open instead of closing it after every query, and moving login queries off the main thread.
## Enhancement Three: Databases

Enhanced the same app's SQLite database for ePortfolio Category 3: Databases.

### Design Decisions

- Redesigned the schema into four tables: users, aircraft, tasks, and audit_log. Aircraft now have their own table, and each task belongs to one aircraft (one-to-many), instead of the tail number being a text field repeated on every task.
- Kept SQLite instead of moving to a server database, because the app stores one user's data on the device and works offline.
- Stored due dates as timestamps (milliseconds since 1970) instead of text, so the database sorts and compares them correctly. This fixes the bug where January 2027 sorted before December 2026 at the database level, not only in the ViewModel.
- Replaced plain text passwords with salted PBKDF2-HMAC-SHA256 hashes at 600,000 iterations, OWASP's recommended setting. Each user gets a random 16-byte salt, and passwords are compared in constant time. The User class no longer holds a password at all.
- Turned on foreign keys for every connection. SQLite ignores them by default, so version 2 declared a foreign key that was never enforced.
- Added CHECK constraints so the database itself rejects bad data, such as an unknown status, a repeat interval of 0, or sign-off information on an open task.
- Added ownership checks to every task query. Reads, updates, deletes, and sign-offs only work if the task's aircraft belongs to the logged-in user. Updates check ownership twice: once when reading the task, and again in the UPDATE's WHERE clause.
- Added sign-offs. A task can only be completed by signing it off, which records who did it and when. The "Mark as complete" checkbox was removed, so there's no way to complete a task without a sign-off. Signed-off tasks can't be edited, like a signed maintenance record.
- Added recurring inspections. Signing off a task with a repeat interval creates the next task, due that many days after the sign-off. Days are added on the calendar, so the due time stays the same across daylight saving time changes.
- Every change that touches more than one table runs in a transaction. For example, a sign-off updates the task, writes an audit entry, creates the next recurring task, and writes a second audit entry. If any step fails, all of it is rolled back.
- Added an append-only audit log. Every create, update, sign-off, and delete is logged in the same transaction as the change. Updates record exactly which fields changed. Deleting a task keeps its history.
- Protected the audit log two ways. Triggers block any UPDATE or DELETE on the log, which stops mistakes in the app's own code. A SHA-256 hash chain links each entry to the one before it, which detects changes made outside the app, such as with a database browser. The hash input writes each field's length first, so shifting text between fields can't produce the same hash.
- Limitation: someone with full access to the database file could drop the triggers and rebuild the whole chain, because the hash has no secret key. A keyed HMAC with the key stored in the Android Keystore would close that gap, and is a possible next step.
- Added an index on tasks (aircraft_id, status, due_at), which matches how tasks are searched. Confirmed with EXPLAIN QUERY PLAN that the overdue query searches the tasks table with this index and the aircraft table with the index SQLite builds for UNIQUE (owner_id, tail_number), with no full table scans. The trade-off is a little more storage and slightly slower writes.
- Added maintenance reports with GROUP BY: overdue tasks per aircraft, the most frequent repairs (matching names regardless of capitalization), and open versus completed tasks per aircraft. A Reports screen shows them, along with a button that verifies the audit log's hash chain.
- Rewrote onUpgrade to move version 2 data into the new design without losing anything. Passwords are hashed, tail numbers become aircraft, text dates become timestamps, and every task gets a first audit entry. User and task IDs are kept, so logged-in sessions and reminders still work. Tasks marked UNASSIGNED in Enhancement One, and any task whose old date can't be read, are flagged "Needs review." An unreadable date is kept in the notes instead of being thrown away. Events whose user doesn't exist are skipped and logged, since nobody could see them.
- Completed tasks from version 2 stay complete with no sign-off, instead of the upgrade inventing a signer. The detail screen says they were completed before sign-offs were recorded.
- Made DatabaseHelper a single shared instance that stays open, instead of opening and closing the database for every query.
- Moved login and registration off the main thread into a UserRepository and LoginViewModel. This was required now, since password hashing is slow on purpose. Login shows the same message for a wrong username or a wrong password, so it doesn't reveal which usernames exist.
- Added the change password feature that was left as a placeholder in Settings. It checks the current password and saves a new salt and hash.
- Turned off backups, and excluded the database and login session from both cloud backups and device-to-device transfers. On Android 12 and higher, turning off backups alone doesn't stop transfers.
- Added an aircraft model field (optional), saved on the aircraft row.
- Moved TaskListItem.java into the app's package folder. It was in the wrong folder, though it still compiled.
- Kept the dashboard's keyword search in memory. A LIKE '%text%' search can't use an index, so moving it into the database wouldn't make it faster.
- Added 37 instrumented tests that run on an emulator against a real SQLite database. They cover the schema, foreign keys, CHECK constraints, triggers, ownership checks, the transaction rollback, the query plan, the reports, the hash chain, and the full upgrade from a real version 2 database. Added unit tests for password hashing, the hash chain, date conversion, audit details, and the new ViewModels. Total tests went from 48 to 147.
