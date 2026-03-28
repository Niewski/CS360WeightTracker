# CS360 Weight Tracker

A weight-tracking Android application built with **Java 11**, **Android SDK 36**, **SQLite**, and **Material Components**. Users register, log daily weights, view history, edit/delete entries, manage their profile, and receive a one-time SMS notification when they hit their goal weight.

## Build & Test

```bash
./gradlew assembleDebug          # Debug APK
./gradlew test                   # Local unit tests (JUnit 4)
./gradlew connectedAndroidTest   # Instrumented tests (AndroidJUnit4 + Espresso)
./gradlew build                  # Full build + all tests
```

- **Min SDK 34 / Target SDK 36 / Compile SDK 36**
- **Java 11**, AGP 8.12.0
- App ID: `com.example.cs360weighttracker`

---

## Architecture

The application follows the **Model-View-ViewModel (MVVM)** pattern using Android's `ViewModel` and `LiveData` components.

```
┌─────────────────────────────────────────────────────┐
│                    View (Activities)                 │
│  LoginActivity · CreateAccountActivity              │
│  WeightHistoryActivity · AddWeightActivity          │
│  EditWeightActivity · ProfileActivity               │
│                                                     │
│  Observes LiveData, handles UI events, delegates    │
│  business logic to ViewModels.                      │
├─────────────────────────────────────────────────────┤
│                  ViewModel Layer                     │
│  LoginViewModel · CreateAccountViewModel            │
│  WeightHistoryViewModel · AddWeightViewModel        │
│  EditWeightViewModel · ProfileViewModel             │
│                                                     │
│  Holds UI state as LiveData, validates input,       │
│  calls Repository methods.                          │
├─────────────────────────────────────────────────────┤
│                 Repository Layer                     │
│  UserRepository · WeightRepository                  │
│                                                     │
│  Mediates between ViewModels and DatabaseHelper.    │
│  Converts Cursors to typed lists. Decouples         │
│  data source from consumers.                        │
├─────────────────────────────────────────────────────┤
│                   Data Layer                        │
│  DatabaseHelper (SQLiteOpenHelper)                  │
│  UserProfile · WeightEntry (POJOs)                  │
│                                                     │
│  Direct SQLite operations using parameterized       │
│  queries and ContentValues.                         │
└─────────────────────────────────────────────────────┘
```

### Package Structure

```
com.example.cs360weighttracker
├── data/                    # Database helper, repositories, POJOs
│   ├── DatabaseHelper.java  # SQLiteOpenHelper — all DB operations
│   ├── UserRepository.java  # Wraps user-related DB calls
│   ├── WeightRepository.java# Wraps weight-related DB calls, cursor→list
│   ├── UserProfile.java     # Immutable POJO for user profile data
│   └── WeightEntry.java     # POJO for a single weight log entry
└── features/
    ├── login/               # Authentication screens
    │   ├── LoginActivity.java
    │   ├── LoginViewModel.java
    │   ├── CreateAccountActivity.java
    │   └── CreateAccountViewModel.java
    ├── weight/              # Weight tracking screens
    │   ├── WeightHistoryActivity.java
    │   ├── WeightHistoryViewModel.java
    │   ├── AddWeightActivity.java
    │   ├── AddWeightViewModel.java
    │   ├── EditWeightActivity.java
    │   ├── EditWeightViewModel.java
    │   └── WeightAdapter.java
    └── profile/             # User profile screen
        ├── ProfileActivity.java
        └── ProfileViewModel.java
```

---

## Database Schema

**SQLite** — `weight_tracker.db` (version 3)

### `users` table

| Column             | Type    | Constraints                  | Description                          |
|--------------------|---------|------------------------------|--------------------------------------|
| `id`               | INTEGER | PRIMARY KEY AUTOINCREMENT    | User's unique identifier             |
| `username`         | TEXT    | UNIQUE                       | Login name                           |
| `password`         | TEXT    |                              | Password (plain text)                |
| `goal_weight`      | REAL    |                              | Target weight in pounds              |
| `phone_number`     | TEXT    | nullable                     | Optional phone for SMS notifications |
| `goal_reached_sent`| INTEGER | DEFAULT 0                    | Boolean flag (0/1) for SMS sent      |

### `weights` table

| Column   | Type    | Constraints               | Description                       |
|----------|---------|---------------------------|-----------------------------------|
| `id`     | INTEGER | PRIMARY KEY AUTOINCREMENT | Entry's unique identifier         |
| `userId` | INTEGER |                           | Foreign key to `users.id`         |
| `date`   | TEXT    |                           | Entry date in `YYYY-MM-DD` format |
| `weight` | REAL    |                           | Weight value in pounds            |

---

## Key Flows

### Authentication

1. `LoginActivity` → user enters credentials → `LoginViewModel.login()` → `UserRepository.loginUser()` → `DatabaseHelper.loginUser()`.
2. On success, `userId` is passed via Intent extra to `WeightHistoryActivity`.
3. Every downstream Activity receives and validates `userId` from its Intent.

### Adding a Weight Entry

1. `AddWeightActivity` defaults the date to today.
2. User enters weight → `AddWeightViewModel.saveWeight()` → `WeightRepository.addWeight()`.
3. After insert, the ViewModel checks: `weight <= goalWeight && !isGoalSmsSent`.
4. If goal reached, a `GoalReachedEvent` is emitted → Activity sends SMS → on success, `markGoalSmsSent()` persists the flag.

### Goal-Reached SMS

- The SMS is sent **once** per goal setting. The `goal_reached_sent` flag prevents re-sending.
- If the user changes their goal weight in the Profile screen, the flag resets, allowing a new SMS when the updated goal is reached.
- The flag is only set after **successful** SMS dispatch — if sending fails, it will retry on the next qualifying weight entry.

### Data Refresh

- `WeightHistoryActivity.onResume()` calls `viewModel.loadWeights()` to pick up changes made in Add, Edit, or Profile screens.

---

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| **MVVM with manual init** | ViewModels use a `void init(...)` method with a guard instead of a `ViewModelFactory`. This keeps the code simple while still separating concerns. |
| **Repository layer** | Thin wrappers over `DatabaseHelper` that decouple ViewModels from the concrete data source. `WeightRepository` also converts raw `Cursor` results to `List<WeightEntry>`. |
| **Toast-only feedback** | Consistent, simple UX pattern across all screens. No Snackbars or dialogs. |
| **Explicit Intents with extras** | Simple navigation model — `userId` is passed as an int extra between all Activities. |
| **GoalReachedEvent with handled flag** | Prevents LiveData from re-delivering the SMS trigger on configuration changes. Acts as a simple single-use event wrapper. |

---

## Known Limitations

- **Passwords are stored in plain text.** A production app should use salted hashing (e.g., bcrypt).
- **No foreign key constraint** between `weights.userId` and `users.id`. Referential integrity is enforced only at the application layer.
- **Destructive `onUpgrade`** — bumping the database version drops all data. Incremental migration should be implemented before release.
- **No input sanitization** beyond parameterized queries. Phone number format, date format, and password strength are not validated.
