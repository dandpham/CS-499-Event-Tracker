# Aircraft Maintenance Tracker

Enhanced from my CS 360 Event Tracking app for ePortfolio Category 1: Software Design/Engineering

## Design Decisions

- Chose MVVM over keeping the original Activity-based design, because each
  Activity mixed screen, validation, and database code, which made the app
  hard to test and change.
- Moved repeated string constants and the -1 "no ID" value into one
  AppConstants class, so each value is defined once and a typo can't cause
  a mismatch between screens.
- Added a SessionManager class to track the logged-in user in one place. Screens no longer pass the
  user ID to each other through Intents, which removes a way for the wrong ID to reach a screen.
