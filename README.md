# BreathKOTH

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21-green)
![License](https://img.shields.io/badge/License-MIT-blue)
![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen)

**BreathKOTH** is a King of the Hill (KOTH)-style plugin for Minecraft servers running Spigot/Paper 1.21. This plugin introduces dynamic zones where players can trigger devastating dragon breath effects, compete for leaderboard standings, and experience custom events like item drops and potion effects. Built with performance and scalability in mind, it’s perfect for PvP servers looking to add a thrilling twist to their gameplay.

---

## Features

- **Dynamic KOTH Zones**: Define circular zones with configurable centers, radii, and intervals for dragon breath attacks.
- **Dragon Breath Effects**: Triggers area-wide damage with lingering clouds and customizable particles (e.g., flame, smoke).
- **Player Statistics**: Tracks time spent in zones and damage taken, with a detailed leaderboard GUI.
- **Zone Visualization**: Displays zone boundaries with scalable, height-varying particle outlines.
- **Custom Events**: Supports broadcasts, entity spawns, item giveaways, and potion effects on flame start/end, with global caps and random selection options.
- **GUI Management**: Intuitive interface for adding, editing, and removing zones, plus teleportation and leaderboard viewing.
- **Performance Optimized**: Lightweight particle spawning, efficient stat tracking, and debug mode for monitoring.

---

## Installation

1. **Requirements**:
   - Minecraft server running Spigot or Paper 1.21.1.
   - Java 17 or higher.
   - Maven (for building from source).

2. **Download**:
   - Grab the latest `BreathKOTH.jar` from the [Releases](https://github.com/4K1D3V/breath-koth/releases) page.

3. **Install**:
   - Place `BreathKOTH.jar` in your server’s `plugins` folder.
   - Restart the server to generate default configuration files (`config.yml`, `zones.yml`, `stats.yml`).

4. **Build from Source** (Optional):
   - Clone the repository: `git clone https://github.com/4K1D3V/breath-koth.git`
   - Navigate to the project directory: `cd BreathKOTH`
   - Build with Maven: `mvn clean package`
   - Copy `target/BreathKOTH-1.0.jar` to your `plugins` folder.

---

## Usage

### Commands
- **`/breathkoth start`**: Triggers dragon flames in all zones immediately.
- **`/breathkoth stop`**: Halts all active flames.
- **`/breathkoth addzone <name> <x> <y> <z> <radius> <interval>`**: Adds a new zone (e.g., `/breathkoth addzone zone3 10 64 10 5 300`).
- **`/breathkoth removezone <name>`**: Removes a specified zone.
- **`/breathkoth reload`**: Reloads configuration files.
- **`/breathkoth gui`**: Opens the zone management GUI (players only).

### Permissions
- `breathkoth.admin`: Grants access to all commands (default: op).

### GUI Features
- **Zone Management**: Add, edit, or remove zones; adjust coordinates, radius, interval, and permissions.
- **Teleportation**: Teleport to zone centers (30s cooldown by default).
- **Leaderboard**: View top players by time spent or damage taken, with zone-specific sub-menus and stat breakdowns.

---

## Configuration

### `config.yml`
Default configuration with two example zones:
```yaml
koth-zones:
  zone1:
    world: "world"
    x: 0
    y: 64
    z: 0
    radius: 5
    interval: 300
    permission: "breathkoth.zone.zone1"
    visualize: true
    events:
      - "start: broadcast: &cDragon flames begin at zone1!"
      - "end: broadcast: &aThe dragon rests at zone1."
      - "start: spawn: zombie"
      - "start: give: DIAMOND 1 2 random"
      - "end: effect: SPEED 30 1 1"
      - "start: give: GOLD_INGOT 2 3 5"
  zone2:
    world: "world"
    x: 100
    y: 64
    z: 100
    radius: 10
    interval: 600
    permission: null
    visualize: true
    events:
      - "start: broadcast: &cBeware the flames at zone2!"
flame-settings:
  interval: 300
  duration: 10
  damage-per-tick: 4
  warning-time: 5
  cloud-duration: 20
  player-activation-time: 30
effects:
  sound-enabled: true
  particle-density: 0.5
  particle-type: DRAGON_BREATH
  spawn-dragon: true
custom-effects:
  "zone1":
    damage-per-tick: 6
    cloud-duration: 30
    particle-type: FLAME
    cloud-effects:
      - type: HARM
        duration: 20
        amplifier: 0
      - type: SLOWNESS
        duration: 60
        amplifier: 1
  "zone2":
    damage-per-tick: 3
    cloud-duration: 15
    particle-type: SMOKE
    cloud-effects:
      - type: POISON
        duration: 40
        amplifier: 0
performance:
  max-particles: 100
  debug-mode: false
teleport-cooldown: 30
visualize-zones: true
```

- **Key Sections**:
  - `koth-zones`: Define zones with coordinates, radius, interval, permissions, visualization, and events.
  - `flame-settings`: Control flame timing, damage, and player activation triggers.
  - `effects`: Customize sounds, particle density, and dragon visuals.
  - `custom-effects`: Override defaults per zone (e.g., particle type, cloud effects).
  - `performance`: Set particle limits and enable debug logging.
  - `teleport-cooldown`: GUI teleport cooldown in seconds.

### Event Syntax
- Format: `<phase>: <type>: <value> [args] [limit] [extra]`
  - Examples:
    - `"start: give: DIAMOND 1 2 random"`: Gives 1 diamond to 2 random players on start.
    - `"start: give: GOLD_INGOT 2 3 5"`: Gives 2 gold ingots to 3 players, max 5 total per cycle.
    - `"end: effect: SPEED 30 1 1"`: Grants Speed II (30s) to 1 player on end.

---

## Files
- **`config.yml`**: Main configuration file.
- **`zones.yml`**: Persistent zone data (auto-generated).
- **`stats.yml`**: Player statistics (auto-generated).

---

## Contributing

1. **Fork the Repository**: Click "Fork" on GitHub.
2. **Clone Your Fork**: `git clone https://github.com/4K1D3V/breath-koth.git`
3. **Make Changes**: Implement features or fixes in a new branch (`git checkout -b feature-name`).
4. **Test**: Ensure compatibility with Minecraft 1.21.1 and no performance regressions.
5. **Commit**: `git commit -m "Add feature X"`
6. **Push**: `git push origin feature-name`
7. **Pull Request**: Submit a PR with a clear description of changes.

### Guidelines
- Follow Java coding standards (e.g., camelCase, clear comments).
- Test thoroughly with Spigot/Paper 1.21.1.
- Document new features in this README.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Credits

- Developed by [Kit](https://github.com/4K1D3V).
- Built with love for the Minecraft community.