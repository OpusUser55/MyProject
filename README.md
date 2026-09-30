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
| Stash Finder | Chunks packed with chests, barrels and shulkers (threshold adjustable, shulkers count triple), outlined with tracers, announced once, optionally saved as waypoints |
| Tracers (Render) | Lines to nearby players, optionally hostile mobs |

Finders scan chunks in the background a few per tick, so enabling them never stalls the game.

### Camera

| Module | What it does |
| --- | --- |
| Freecam | Detach the camera and fly around while your body stays put |
| Freelook | Hold `Left Alt` to orbit the camera around you while you keep walking the way you face (third or first person) |
| Zoom | Hold `Z` to zoom; scroll to adjust |
| Waypoints | Saved places per server, drawn as a box + beam with a floating name/distance label, optional tracers and a nearest-first HUD list. Overworld waypoints show in the Nether at /8 (purple) and vice versa |
| Block ESP | Highlights any blocks you list by ID (`diamond_ore, spawner, beacon`…), from chunk data and live block updates, with optional tracers |
| Trajectories | Predicts where ender pearls, snowballs, eggs, potions, XP bottles, tridents and drawn bows / charged crossbows will land |

### Misc

| Module | What it does |
| --- | --- |
| Fake Pay | Your own `/pay <player> <amount>` is caught before sending and shows the "You paid" message locally instead (amounts like `1.5k`, `2m`, `$2,500` work) |
| Fake Scoreboard | Replaces the sidebar with your own money / shards / kills / deaths / playtime lines and an optional footer; the server's board returns when it's off. With Fake Pay's **Remove From Scoreboard** on, fake payments come off the money line |

| Name Protect | Replaces your username everywhere it's drawn (chat, tab, scoreboard, name tags) with a name you choose, for screenshots and streams |
| Death Coords | Tells you where you died and marks the spot with a waypoint until you're back (on by default) |
| Visual Range | Alerts when a player comes within a set distance (or render distance) and when they leave; ignores `.friend`s; optional loud sound and red screen-edge flash |
| Auto Reconnect | Rejoins the last server after a kick or connection loss, with a countdown on the disconnect screen |
| Auto Respawn | Respawns automatically after a short delay |
| Durability Alert | Warns once when your armor or held tool drops below a threshold (on by default) |
| Better Chat | Timestamps, hides repeated spam lines, a hide-words filter, and a gold marker + ping when your name (or extra words) is mentioned |
| Armor HUD (HUD) | Armor and held items with colour-coded durability percentages |
| Item Count HUD (HUD) | Totems, crystals, XP bottles, golden apples, pearls, obsidian and food you're carrying |
| Inventory HUD (HUD) | Your 27 inventory slots (optionally the hotbar too) in a panel |
| Region Map (HUD) | DonutSMP's 9×9 numbered region grid with your region in gold, a dot at your exact spot, and your coordinates + region number. Region size (default 50,000) and centre are adjustable |

All of these are client-side only: nothing extra is sent to the server.

### Movement

| Module | What it does |
| --- | --- |
| Sprint | Sprints whenever you move forward |
| Auto Walk | Holds forward for you; pressing back turns it off |
| Safe Walk | Sneaks for you near the edge of a drop so you don't fall off (set the minimum drop) |
| Parkour | Jumps at the last moment before you run off an edge |
| Auto Jump | Sprint-jumps continuously while you move |
| Auto Swim | Holds jump in water and lava so you float; hold sneak to dive |
| Elytra Swap | Puts a fresh elytra from your inventory on before the worn one breaks |

These press the normal game keys for you and hand them back when they stop, so your own key presses always win.

### World

| Module | What it does |
| --- | --- |
| Auto Tool | Switches to the fastest hotbar tool while you mine and back afterwards; skips tools about to break |
| Auto Eat | Eats the most filling hotbar food when hunger drops to a threshold, then switches back; skips golden apples, bad food and chorus fruit unless allowed |
| Auto Fish | Reels in when the bobber dips and recasts after a delay |

ESP and Tracers colour `.friend`s blue (Tracers can skip them entirely).

### Chat commands

Type these in chat; they're handled by the client and never sent. Start a message with `..` to send a literal `.`.

| Command | What it does |
| --- | --- |
| `.help` | List commands |
| `.t <module>` | Toggle a module (`.t freecam`, `.t debris finder`) |
| `.b <module> <key\|none>` | Bind a key (`.b freelook left.alt`) |
| `.wp add <name> [x y z]` | Save a waypoint here (or at coordinates) |
| `.wp remove <name>` / `.wp list` / `.wp clear` | Manage this server's waypoints |
| `.friend <add\|remove\|list> [name]` | Manage friends (alias `.f`) |
| `.settings <module>` | List a module's settings |
| `.set <module> <setting> <value>` | Change a setting (`.set debris finder range 200`, `.set better chat timestamps off`) |
| `.reset <module> [setting]` | Reset one setting or all of a module's settings |
| `.profile <save\|load\|list\|delete> [name]` | Save whole setups (modules, binds, settings, HUD layout) and switch between them |
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
