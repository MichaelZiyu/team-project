Notes for collaboration methods and class designs:
- https://docs.google.com/document/d/1hk6UIoUFeQurILgY8SrJHeUEdcINJ-Gnw2gLwtYSpO0/edit?tab=t.0

# Team Project
## Milestone 2 MVP – Testing Guidelines

### 1. Start the Game
- Run `main` directly to launch the game.
- If everything works correctly, the start menu should appear.

### 2. Load the Sample World
- Click **Start** in the start menu.
- World generation has not been implemented yet. For testing purposes, a sample world is created in `main`, and clicking Start will take you directly to the Load Game menu.
- Click **Load** to load the sample world and enter the game.

### 3. Basic Gameplay
- Use **W, A, S, D** to move the character in different directions and explore the map.
- Hold **Shift** while moving to increase the character's movement speed.
- Press **Esc** to exit the game.

### 4. Cloud Save Configuration
- The **Save** button is located at the bottom of the start menu.
- The game supports saving world data (`.dat` files) to a Supabase database.
- To use this feature, click **Save** and provide your Supabase API key and database URL when prompted.

Please keep this up-to-date with information about your project throughout the term.

The readme should include information such as:
- a summary of what your application is all about
- a list of the user stories, along with who is responsible for each one
- information about the API(s) that your project uses
- screenshots or animations demonstrating current functionality

By keeping this README up-to-date,
your team will find it easier to prepare for the final presentation
at the end of the term.

## Team Information

- Michael Zhou
- Yueheng Guan
- Jimmy Zeng
- Henry Xu
## Project Domain

**Games and entertainment.** We are building a 2D sandbox adventure in Java with a Swing interface.
Players explore a procedurally generated overworld, fight monsters, and gather resources.
The initial playable loop takes place in the top-down view and uses local saves
so players can return to their world between sessions.
A front-view side-scrolling mode is in development.


## User Stories

| ID | User story                                                                                           |
| - |------------------------------------------------------------------------------------------------------|
| 1 | As a player, I want to configurate setting and save my world so I can resume progress between plays. |
| 2 | As a player, I want to walk and run through a diverse overworld so I can discover new areas.         |
| 3 | As a player, I want to fight a monster so I can get past threats while exploring.                    |
| 4 | As a player, I want to interact with the structures in the world so I can enter and explore them.    |

## Use Cases

### UC1 · Explore the Overworld (US1)

- **Primary entity** — Player
- **Preconditions** — A world is open in the top-down view and gameplay is active.
- **Trigger** — The player gives a movement command.
- **Success outcome** — The character moves in the requested direction and newly reached terrain comes into view.

**Main success scenario**

1. The player chooses a direction and can hold the run control to move faster.
2. The game updates the character's position and follows the character with the camera.
3. As the player travels, the game brings new terrain into view.

**Extensions**

- **2a.** An obstacle blocks the path. The game stops movement into the obstacle and the player can choose another direction.

### UC2 · Browse and Load a Local World

- **Primary entity** — World
- **Preconditions** — The start menu is open.
- **Trigger** — The player chooses **Start Game**.
- **Success outcome** — The selected world opens for play.

**Main success scenario**

1. The game lists local `.dat` saves by name.
2. The player selects a save and clicks **Load**.
3. The game loads the selected world and starts gameplay.

### UC3 · Adjust the Window Size (US1)

- **Primary entity** — `GameSettings`
- **Preconditions** — The start menu is open.
- **Trigger** — The player chooses **Settings**.
- **Success outcome** — The game window uses the selected `frame_size` for the current session.

**Main success scenario**

1. The Settings menu displays the current window size.
2. The player selects **800 × 600** or **1600 × 900** under **Window size** and clicks **Apply**.
3. The window resizes and the menu displays **Window size applied.**

**Extensions**

- **2a.** **Back** returns to the start menu and discards any unapplied size selection.

## Minimum Viable Product (MVP)
| Team member | Proposed MVP lead assignment |
| --- | -- |
| Jimmy Zeng | UC |
| Henry Xu | UC |
| Michael Zhou | UC |
| Yueheng Guan | UC |

## Entities
| Domain model                                         | Key instance variables | Relevant behavior |
|------------------------------------------------------| --- | --- |
| [`World`](src/main/java/game/World.java)             | `seed`, `view`, `player`, `entities`, `map` | `initialize()` rebuilds runtime state. `tick()` advances the map and entities. `getPlayer()` returns the controlled character. |
| [`Player`](src/main/java/game/gameModel/Player.java) | Inherits `name`, `hp`, `maxHp`, `attack`, `speed`, `body`, and `inventory` from `Entity`. | `handleCommand()` applies player input through the state manager. Inherited `tick()` updates movement and state. |
| [`Chunk`](src/main/java/terrain/Chunk.java)          | `chunkX`, `chunkY`, `blocks`, `heights` | `blockAt(x, y)` and `heightAt(x, y)` read the block type and height at a local tile coordinate. |
| [`GameSettings`]                                     | `frame_size`, a `GameSettings.FrameSize` with `width` and `height` | `frameSize()` returns the configured dimensions. `withFrameSize()` returns a new settings snapshot. |
APIs in Project:
1. Multiplayer or trading system or leaderboard
2. Weather

## Design Diagrams

## How to Run

## Notes