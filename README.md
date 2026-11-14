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