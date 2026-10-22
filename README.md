# Multiplayer Connect Four 

A real-time multiplayer Connect Four game built with **Kotlin**, **Jetpack Compose** and **Firebase Firestore**. The game gets played across two devices using a turnbased gameplay. Game is not deployed yet on google play and was created for educational purposes.

## 🎯 Features
- **Follows basic Connect Four gamerules**
- **Multiplayer gameplay** using Firebase
- **Lobby system** where players can register & challenge each other with accept/decline invitations
- **Turn-based gameplay**
- **Real-time synchronization**

## 🛠️ Technical Stack & Testing

- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose & Material 3
- **Navigation:** Navigation Compose
- **Backend & Database:** Firebase Firestore (Real-time DB snapshots)
- **State Management:** Kotlin Coroutines, StateFlow, MutableState
- **Testing Frameworks:** 
  - **Unit Testing:** JUnit 4 (Game logic, win algorithms, gravity stacking, data models)
  - **UI Testing:** Jetpack Compose UI Testing (`androidx.compose.ui.test.junit4`) & AndroidX Test
- **CI/CD DevOps:** Automated GitHub Actions Workflow (`android.yml`) running unit & UI tests and building debug APKs on every push or Pull Request.

## Testing & CI/CD

The project features a robust test suite:
1. **Unit Tests (`GameLogicTest.kt`)**: Verifies win conditions across all directions, piece gravity/dropping physics, and data models.
2. **UI Instrumented Tests (`ConnectFourUiTest.kt`)**: Validates Jetpack Compose screen rendering, text input interactions, and navigation.
3. **Automated Pipeline (`.github/workflows/android.yml`)**: Automatically runs test suites and builds the project on GitHub Actions.

## Installation

**Clone the repository**

```bash
git clone https://github.com/OscarAssaf/ConnectFour
```
