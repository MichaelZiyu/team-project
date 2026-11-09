Notes for collaboration methods and class designs:
- https://docs.google.com/document/d/1hk6UIoUFeQurILgY8SrJHeUEdcINJ-Gnw2gLwtYSpO0/edit?tab=t.0

# Team Project

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
- 
## Project Domain

**Games and entertainment.** We are building a 2D sandbox adventure in Java with a Swing interface. 
Players explore a procedurally generated overworld, fight monsters, and gather resources. 
The initial playable loop takes place in the top-down view and uses local saves 
so players can return to their world between sessions. 
A front-view side-scrolling mode is in development.


## User Stories

| ID | User story                                                                                           | Proposed lead |
| - |------------------------------------------------------------------------------------------------------|---------------|
| 1 | As a player, I want to configurate setting and save my world so I can resume progress between plays. | Henry Xu      |
| 2 | As a player, I want to walk and run through a diverse overworld so I can discover new areas.         | Jimmy Zeng    |
| 3 | As a player, I want to fight a monster so I can get past threats while exploring.                    | Michael Zhou  |
| 4 | As a player, I want to interact with the structures in the world so I can enter and explore them.    | Yueheng Guan  |

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

## Minimum Viable Product (MVP)
| Team member | Proposed MVP lead assignment |
| --- | -- |
| Jimmy Zeng | UC |
| Henry Xu | UC |
| Michael Zhou | UC |
| Yueheng Guan | UC |

## Entities
| Domain model | Key instance variables | Relevant behavior |
| --- | --- | --- |
| [`World`](src/main/java/game/World.java) | `seed`, `view`, `player`, `entities`, `map` | `initialize()` rebuilds runtime state. `tick()` advances the map and entities. `getPlayer()` returns the controlled character. |
| [`Player`](src/main/java/game/gameModel/Player.java) | Inherits `name`, `hp`, `maxHp`, `attack`, `speed`, `body`, and `inventory` from `Entity`. | `handleCommand()` applies player input through the state manager. Inherited `tick()` updates movement and state. |
| [`Chunk`](src/main/java/terrain/Chunk.java) | `chunkX`, `chunkY`, `blocks`, `heights` | `blockAt(x, y)` and `heightAt(x, y)` read the block type and height at a local tile coordinate. |

## API Usage
APIs in Project:
1. Multiplayer or trading system or leaderboard
2. Weather

## Design Diagrams

## How to Run

## Notes