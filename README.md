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