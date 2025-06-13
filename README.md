# AR Scavenger Hunt - Team Adventure Game

An Android-based augmented reality scavenger hunt game where players team up to solve clues, complete quests, and explore their surroundings.

## Core Features

*   **Augmented Reality (AR) Interaction**: Uses ARCore to display AR objects (clues/treasures) in the real world. Players interact with these objects by tapping them.
*   **GPS-Based Gameplay**: Quests and clues are tied to real-world GPS coordinates. Players navigate to these locations to find AR elements or solve puzzles.
*   **Quest System**: Structured quests with multiple clues. Clues can be location-based or involve solving puzzles.
*   **Puzzle Integration**: Basic text riddles and math puzzles are integrated as clue types.
*   **Team Gameplay**: Players create or join teams. Quest progression and some rewards are team-based.
*   **Real-time Team Chat**: In-built chat system for team members using Firebase Realtime Database.
*   **Player Authentication**: Secure login and signup using Firebase Authentication (email/password).
*   **Player Dashboard**: Screen for players to manage their profile, team (create, join, leave), and view some stats (XP displayed as Points).
*   **Admin Dashboard**: Basic interface for administrators to view teams, members, assign players to teams, and disband teams.
*   **Mini-Map**: osmdroid integration in the main gameplay screen (`ScavengerHuntFragment`) to show player location, clue markers, and allow interaction.
*   **Player Stats**: Tracks individual XP, stamina, and coins. Stamina and coins are affected by actions like "Searching".

## Technologies Used

*   **Language**: Java
*   **IDE**: Android Studio
*   **Core Android**: Android SDK, Jetpack (ViewModels, LiveData, Navigation, Room)
*   **Augmented Reality**: ARCore
*   **Mapping**: osmdroid (for mini-map and location display)
*   **Authentication**: Firebase Authentication
*   **Real-time Database (Chat, Teams)**: Firebase Realtime Database
*   **Local Database**: Room Persistence Library (for player profiles, quest/clue progress, etc.)
*   **Dependency Injection**: Hilt

## Setup Instructions

1.  **Clone the Repository**:
    ```bash
    git clone <repository_url>
    ```
2.  **Open in Android Studio**: Open the cloned project in Android Studio (latest stable version recommended).
3.  **Firebase Setup**:
    *   Create a new Firebase project at [https://console.firebase.google.com/](https://console.firebase.google.com/).
    *   Add an Android app to your Firebase project:
        *   Use `com.adventure.solo` as the package name (or verify the `applicationId` in `app/build.gradle.kts`).
        *   Download the `google-services.json` file provided by Firebase.
    *   Place the downloaded `google-services.json` file into the `app/` directory of your Android Studio project.
    *   **Enable Authentication**: In the Firebase console, go to "Authentication" -> "Sign-in method" and enable "Email/Password".
    *   **Enable Realtime Database**: In the Firebase console, go to "Realtime Database", create a database, and start in "test mode" for initial setup (you can refine security rules later: `".read": "auth != null"`, `".write": "auth != null"` are good starting points for authenticated users). Note your Realtime Database URL.
4.  **Configure Firebase Database URL**:
    *   Open `app/src/main/java/com/adventure/solo/di/FirebaseModule.java`.
    *   Update the `provideFirebaseDatabase()` method:
        ```java
        // return FirebaseDatabase.getInstance("YOUR_FIREBASE_REALTIME_DB_URL");
        // Replace YOUR_FIREBASE_REALTIME_DB_URL with your actual Firebase Realtime Database URL.
        // e.g., return FirebaseDatabase.getInstance("https://your-project-id-default-rtdb.firebaseio.com/");
        ```
5.  **Admin UIDs**:
    *   To access the Admin Dashboard, you need to configure administrator Firebase User IDs.
    *   Open `app/src/main/java/com/adventure/solo/ui/admin/AdminDashboardActivity.java`.
    *   Modify the `ADMIN_UIDS` list with the actual Firebase UIDs of your admin users:
        ```java
        // private static final List<String> ADMIN_UIDS = Arrays.asList("YOUR_ADMIN_FIREBASE_UID_1", "YOUR_ADMIN_FIREBASE_UID_2");
        ```
6.  **Build and Run**:
    *   Sync Gradle files.
    *   Build and run the application on an ARCore-compatible Android device or emulator.
7.  **Permissions**:
    *   The app will request Camera and Location permissions. These are necessary for AR and GPS functionalities. Please grant them.
    *   Ensure Location Services (GPS) are enabled on your device.

## How to Play

1.  **Signup/Login**:
    *   On first launch, you'll be directed to the Login screen.
    *   If you don't have an account, tap "Sign Up", provide a username (optional, uses email if blank during signup, but profile can be updated), email, and password.
    *   Log in with your credentials.
2.  **Player Dashboard (Profile Screen)**:
    *   After login, you'll land on the main app screen (likely the Map tab).
    *   Navigate to the "Profile" tab (Player Dashboard) using the bottom navigation.
3.  **Team Management (in Profile Screen)**:
    *   **Create a Team**: If you're not in a team, enter a desired team name and tap "Create Team". Your unique Team ID will be displayed. Share this ID with friends.
    *   **Join a Team**: If you have a Team ID, enter it and tap "Join Team".
    *   **Leave Team**: If you are in a team, a "Leave Team" button will be visible.
4.  **Team Chat**:
    *   Once in a team, a "Chat" option should become accessible (e.g., via a button on the Player Dashboard or another tab - *Note: Direct navigation to chat from dashboard is not yet implemented in this version but the chat activity exists.*).
    *   `TeamChatActivity` allows real-time messaging with team members.
5.  **Gameplay (Scavenger Hunt Screen)**:
    *   Navigate to the "Map" or "Quests" tab (likely the main game screen, `ScavengerHuntFragment`).
    *   If your team has an active quest (or one is auto-started), the mission details and clue markers will appear on the map.
    *   **Navigate to Clues**: Use the map to go to the physical location of clue markers.
    *   **Interact with Clues**:
        *   Tap a clue marker on the map for details.
        *   **Puzzle Clues**: If it's a puzzle (riddle/math), a dialog will appear. Solve it to "collect" the clue.
        *   **Location Clues**: If it's a location clue and you are physically nearby (within ~20 meters), the dialog will offer a "View in AR" button.
    *   **AR Interaction**: Tapping "View in AR" launches `ARSceneFragment`. A gold cube (placeholder treasure) will appear. Tap the cube to collect the clue.
    *   **Progression**: Collecting clues (solving puzzles or tapping AR objects) grants individual XP. Completing all clues in a quest awards a coin bonus to all team members.
    *   The HUD displays your XP (as "Points"), Stamina, Coins, and current mission text.
6.  **"Search" Button**:
    *   On the main game screen, use the "Search Area" button. This costs 10 Stamina and grants 5 Coins.
7.  **Inventory**:
    *   Navigate to the "Inventory" tab to see a list of quests your team has completed.

## Admin Dashboard Guide

1.  **Access**:
    *   Ensure your Firebase User ID is in the `ADMIN_UIDS` list in `AdminDashboardActivity.java`.
    *   Currently, there's no direct UI button to launch the Admin Dashboard. You might need to trigger it via an ADB command or a temporary button for testing:
        ```bash
        adb shell am start -n com.adventure.solo/com.adventure.solo.ui.admin.AdminDashboardActivity
        ```
2.  **Features**:
    *   **View Teams**: See a list of all created teams, their IDs, leaders, and member UIDs/Usernames.
    *   **Assign Player to Team**: Enter a Player's Firebase UID and a target Team ID to manually add them to a team.
    *   **Disband Team**: Enter a Team ID to remove the team and clear the `teamId` from its members' profiles.

## Known Issues & Limitations

*   **Quest Generation**: The `QuestGenerator.java` could not be updated due to tool limitations. Therefore, the automatic generation of diverse quests with new puzzle types and randomized location offsets is **not functional**. The system can *handle* these clue types if quests are manually created in the database, but the game currently relies on simpler or placeholder quest generation.
*   **Player Dashboard UI**: Due to tool issues with modifying `dialog_profile.xml`, the display of detailed player stats (Stamina, Coins, specific Username field) in the Profile/Player Dashboard screen is limited. XP is shown as "Points". Team management functions are operational.
*   **Team Leader Migration**: If a team leader leaves a team that still has members, leader migration is not handled (the team might become leaderless).
*   **Chat Navigation**: Direct UI navigation to `TeamChatActivity` from the Player Dashboard or other main screens is not yet implemented.
*   **Error Handling**: Robust error handling for all Firebase/Room operations (e.g., network issues, data inconsistencies during distributed updates) can be further improved.
*   **AR Visualization**: The AR "treasure" is currently a simple gold cube. Integration of more complex 3D models via a third-party library was out of scope due to tool constraints.

## Future Improvements (Optional)

*   Fully implement `QuestGenerator` for diverse, auto-generated quests.
*   Resolve XML issues and enhance Player Dashboard UI for all stats.
*   Implement robust team leader migration.
*   Add UI for easy access to Team Chat.
*   Integrate a third-party AR library for richer AR experiences and models.
*   Add more puzzle types and mini-games.
*   Implement a more sophisticated reward system (items, collectibles).
*   Refine admin dashboard with more features (e.g., editing quests/clues).
