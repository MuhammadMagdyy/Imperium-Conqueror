# ⚔️ Imperium-Conqueror: The Age of Empires

**Imperium-Conqueror** is a single-player, turn-based grand strategy game developed in Java[cite: 9]. As a Commander, you start with one historical capital—**Cairo, Rome, or Sparta** [cite: 10]—and must manage resources, build infrastructure, and lead armies to conquer every city on the map within a set turn limit[cite: 11, 12].

---

## 📜 Full Gameplay Guide

### 1. Winning and Losing
* **The Goal:** You win if you manage to conquer all cities available in the game within the determined number of turns[cite: 14].
* **The Defeat:** If these turns pass and you have not achieved this goal, it is considered a loss[cite: 15].

### 2. Economic Infrastructure & Survival
Your empire relies on two major resources[cite: 33]:
* **Gold 💰:** Used to build/upgrade structures and recruit units[cite: 34]. You must pay gold each turn for army upkeep; failure to pay causes soldiers to lose faith and desert[cite: 35, 36, 37].
* **Food 🍞:** Used to keep soldiers alive[cite: 38]. Consumption increases while marching and is highest during a city siege[cite: 39, 40]. Lack of food leads to starvation and death[cite: 41].

### 3. City Development (Buildings)
Each turn, you can perform **one action per building**: build, upgrade, or recruit units[cite: 47, 48].

| Building | Type | Level Effect |
| :--- | :--- | :--- |
| **Market** | Economic | Primary source of gold profit[cite: 51, 52]. |
| **Farm** | Economic | Provides food supplies for the army[cite: 54]. |
| **Archery Range**| Military | Enables recruitment of **Archers**[cite: 58]. |
| **Barracks** | Military | Enables recruitment of **Infantry**[cite: 62]. |
| **Stable** | Military | Enables recruitment of **Cavalry**[cite: 65]. |

### 4. Military Units & Combat Strategy
Combat follows a tactical advantage system:
* **Archers 🏹:** Strong against foot units but vulnerable to mounted units[cite: 59, 61].
* **Infantry ⚔️:** Strong against mounted units but vulnerable to archers[cite: 64].
* **Cavalry 🐴:** Supreme against archers, good against infantry[cite: 67].

### 5. Conquest & Sieges
When an army reaches a target city, you have several tactical choices[cite: 19]:
* **Direct Attack:** Immediately engage the defending army[cite: 19].
* **Besiege:** Surround the city for a maximum of three turns to starve defenders[cite: 20, 21].
* **Auto-Resolve:** A randomized battle where units trade attacks until one army is destroyed[cite: 24, 25].
* **Manual Mode:** You manually choose which units to attack in the defending army[cite: 24, 28, 30].

---

## 📂 Project Structure
```text
Imperium-Conqueror/
├── src/
│   ├── buildings/       # Economic and Military building logic
│   ├── engine/          # Core game engine and turn-state management
│   ├── exceptions/      # Custom game exceptions
│   ├── units/           # Unit-specific logic (Archers, Infantry, Cavalry)
│   ├── views/           # UI implementation (Frames, Dialogs, Panels)
│   └── Main.java        # Application entry point
├── background/          # World map textures and UI backgrounds
├── icons/               # Unit, building, and resource assets
├── audio/               # Thematic music and sound effects
└── README.md            # Comprehensive project documentation
🛠️ Technical Features
Custom Graphics2D UI: Features rounded panels, golden gradients, and medieval-style progress bars.
Dynamic Audio Engine: Procedural music that shifts intensity based on resource levels and active combat status.
MVC Architecture: Strict separation between game mechanics and visual representation.
🚀 How to Run
Prerequisites: Install Java JDK 17 or higher.
Compile: javac -d bin -sourcepath src src/Main.java
Run: java -cp bin Main
Author: Muhammad Magdy
License: MIT EOF


git add README.md
git commit -m "Fix technical features and structure formatting in README"
git push origin main --force
