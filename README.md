# Ooga-Client

A Fabric utility client for **Minecraft 1.21.11** with its own visual identity: a dark
charcoal UI with a restrained, sophisticated gold accent and a soft golden glow.

## Preview

![ClickGUI panels](docs/previews/panels_expanded.png)
![HUD](docs/previews/hud.png)

These are rendered offline from the real UI code by a stand-in renderer (Inter via Java2D),
so text anti-aliasing differs slightly from in-game.

## Building

Requires Java 21.

```
./gradlew build
```

The mod jar lands in `build/libs/`. Drop it, together with
[Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11, into `.minecraft/mods`.

## Using it

| Action | Default |
| --- | --- |
| Open the menu | `Right Shift` |
| Toggle a module | Left-click its row |
| Show a module's settings | Right-click the row |
| Bind a key | Middle-click the row, then press a key; `Esc`/`Backspace` unbinds |
| Move / collapse a panel | Drag its header / click the chevron (or right-click the header) |
| Switch layout | ClickGUI settings → Layout: Panels (default) or Window |
| Search | Just start typing (or `Ctrl+F`); `Enter` toggles the top result |
| Reset a slider | Right-click its label |
| Edit a text setting | Click the field, type, `Enter`/`Esc` or click away to finish; right-click resets |
| Move HUD elements | **Edit HUD** in the menu header; scroll over an element to resize |

Settings are saved automatically to `config/ooga/config.json`.

### Base finding

| Module | What it does |
| --- | --- |
| Storage ESP | Chests, shulkers, barrels, ender chests, hoppers and more through walls, colour-coded by kind, with optional tracers |
| Spawner Finder | Announces each spawner once (chat + notification with coordinates) and highlights it |
| Sus Chunk Finder | Scores chunks for player-placed blocks (hoppers, observers, pistons, shulkers, beacons…) and fully grown kelp, which only grows while a chunk stays loaded; flagged chunks are marked and announced |
| Light Finder | Torches and lanterns below a set height, where caves generate none |
| Debris Finder | Ancient debris in the Nether, one box per vein (nearest first) with tracers and one chat line per vein. Also catches debris the server reveals later via block updates (mining, TNT), which is what works on anti-xray servers; **Revealed Only** ignores fake ores in chunk data |
| Tracers (Render) | Lines to nearby players, optionally hostile mobs |

Finders scan chunks in the background a few per tick, so enabling them never stalls the game.

### Camera

| Module | What it does |
| --- | --- |
| Freecam | Detach the camera and fly around while your body stays put |
| Freelook | Hold `Left Alt` to orbit the camera around you while you keep walking the way you face (third or first person) |
| Zoom | Hold `Z` to zoom; scroll to adjust |
| Waypoints | Saved places per server, drawn as a box + beam (optional tracers) with a nearest-first HUD list. Overworld waypoints show in the Nether at /8 (purple) and vice versa |

### Misc

| Module | What it does |
| --- | --- |
| Fake Pay | Your own `/pay <player> <amount>` is caught before sending and shows the "You paid" message locally instead (amounts like `1.5k`, `2m`, `$2,500` work) |
| Fake Scoreboard | Replaces the sidebar with your own money / shards / kills / deaths / playtime lines and an optional footer; the server's board returns when it's off. With Fake Pay's **Remove From Scoreboard** on, fake payments come off the money line |

| Name Protect | Replaces your username everywhere it's drawn (chat, tab, scoreboard, name tags) with a name you choose, for screenshots and streams |
| Death Coords | Tells you where you died and marks the spot with a waypoint until you're back (on by default) |
| Visual Range | Notifies you when players enter or leave render distance |
| Auto Reconnect | Rejoins the last server after a kick or connection loss, with a countdown on the disconnect screen |
| Auto Respawn | Respawns automatically after a short delay |
| Durability Alert | Warns once when your armor or held tool drops below a threshold (on by default) |
| Armor HUD (HUD) | Armor and held items with colour-coded durability percentages |

All of these are client-side only: nothing extra is sent to the server.

### Chat commands

Type these in chat; they're handled by the client and never sent. Start a message with `..` to send a literal `.`.

| Command | What it does |
| --- | --- |
| `.help` | List commands |
| `.t <module>` | Toggle a module (`.t freecam`, `.t debris finder`) |
| `.b <module> <key\|none>` | Bind a key (`.b freelook left.alt`) |
| `.wp add <name> [x y z]` | Save a waypoint here (or at coordinates) |
| `.wp remove <name>` / `.wp list` / `.wp clear` | Manage this server's waypoints |
| `.coords` | Copy your coordinates to the clipboard |
| `.modules` | List modules and whether they're on |

### Music widget

The **Music** HUD module shows what's playing in any system media player: Spotify,
browsers, Apple Music and so on. Open chat to click its previous, play/pause and next buttons.

| OS | Source |
| --- | --- |
| Windows 10/11 | System media session (the one the volume flyout shows), via PowerShell |
| macOS | Spotify, then Apple Music, via AppleScript |
| Linux | Any MPRIS player via `playerctl` (install it from your package manager) |

## Layout

```
src/main/java/dev/ooga/client
├── OogaClient.java          entrypoint; wires modules → notifications, HUD, config
├── module/                  Module, Category, ModuleManager, settings
│   └── impl/                client · movement · render modules
├── config/                  debounced JSON persistence
├── camera/                  CameraController, CameraMode, FreeCamera,
│                            FirstPersonRenderer, PlayerControlLock
├── ui/
│   ├── OogaTheme.java       colour, radius and spacing tokens
│   ├── render/              Render2D (pixel-exact shapes), GlowRenderer, OogaFonts, Icon
│   ├── clickgui/            the menu and its widgets
│   ├── hud/                 watermark, module list, info, keystrokes, HUD editor
│   └── notify/              toast notifications
└── mixin/                   the only code that touches Minecraft internals
```

The UI typeface is [Inter](https://rsms.me/inter/) (SIL Open Font License, see
`assets/ooga/font/INTER_LICENSE.txt`).
