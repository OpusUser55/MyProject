# Ooga-Client

A Fabric utility client for **Minecraft 1.21.11** with its own visual identity: a dark
charcoal UI with a restrained, sophisticated gold accent and a soft golden glow.

## Preview

![ClickGUI panels](docs/previews/panels_expanded.png)
![HUD](docs/previews/hud.png)

![Menu](docs/previews/v04_menu.png)
![Combat settings (mockup)](docs/previews/v05_combat.png)
![Base finding HUD and ESP (mockup)](docs/previews/v02_basefinding.png)
![Slow Mine](docs/previews/v02_slowmine.png)
![Admins, radar, region map and sus chunks (mockup)](docs/previews/v03_admins_radar_regions.png)

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
| Move HUD elements | **Edit HUD** in the menu header; scroll over an element to resize |

Settings are saved automatically to `config/ooga/config.json`.

### Base finding

| Module | What it does |
| --- | --- |
| Storage ESP | Chests, shulkers, barrels, ender chests, hoppers and more through walls, colour-coded by kind, with optional tracers. **Stash Alert** announces any chunk holding lots of containers |
| Spawner Finder | Names each spawner's mob, groups spawners that load together into one alert ("4 spawners: 3x Skeleton, Zombie"), and tells placed spawners (red) from dungeon/mineshaft/fortress ones (brown). **Hide Natural** skips the generated ones |
| Tunnel Finder | Long, straight 1x2 corridors of plain air with solid walls, floor and ceiling: player-dug tunnels. Measured across chunk borders; long ones are announced |
| Hole ESP | 1x1 vertical shafts (air or ladders, walled on all four sides) that players dig straight down to hidden bases |
| New Chunks | Marks chunks the server just generated (flowing-liquid updates right after load), so old, explored land stands out |
| Sus Chunk Finder | Scores chunks for player-placed blocks (hoppers, observers, pistons, shulkers, beacons…) and fully grown kelp, which only grows while a chunk stays loaded; flagged chunks are announced and drawn as a gridded square, a plain square, or a tall beam (yellow, pink or accent) |
| Light Finder | Torches and lanterns below a set height, where caves generate none |
| Finder Alerts | Shared settings: a ping sound for every find, and a log of all finds (time, server, dimension, coordinates) in `config/ooga/finds.log` |
| Finds (HUD) | The last few finds in this dimension, with coordinates, distance and an arrow pointing to each |
| Radar (HUD) | A rotating minimap with N/E/S/W on the rim: players as dots with distance (or name) tags, optionally hostiles, and every find as a coloured marker |
| Region Map (HUD) | A numbered grid of world regions (size, grid and centre are configurable) with your region highlighted, its number in the header, visited regions brighter and regions with finds marked |
| Admin Detector (Misc) | An **Admins** panel listing staff online with face and ping, or "None online". Staff = a staff rank in their tab name or team prefix, spectator mode (vanished staff), or a name in `config/ooga/staff.txt`. Chat + notification + sound when staff join or leave |

Finders scan chunks in the background a few per tick, so enabling them never stalls the game.
When blocks change (mined, placed, liquids flowing) the chunk is rescanned once it settles, so
results stay current.

### Render and utility

| Module | What it does |
| --- | --- |
| ESP | Players, hostiles, passive mobs and items through walls. **Style**: Glow (vanilla-style outline), Box, or Both |
| Nametags | Tags over players (and optionally hostiles) through walls: name, health, distance, held item and armour. Hides the vanilla tags it replaces |
| Block ESP | Diamonds, ancient debris, emeralds, gold, nether portals, end portal frames, beacons and budding amethyst through walls, nearest first |
| Tracers | Lines to nearby players, optionally hostile mobs |
| Slow Mine | Mine at normal speed while your hand swings in slow motion. **Speed** sets how slow (35% by default); **When** picks mining only or every swing; **Full Swing** plays each swing all the way through. Purely visual |
| Free Look | Orbit a third-person camera around yourself while you keep moving straight. With **Hold Key**, it lasts as long as you hold its keybind |
| Freecam | Detached flying camera. Scroll to change speed. **Stay Sneaking** keeps your parked body crouched if you were sneaking; **Steady View** turns off view bobbing and FOV effects while flying; it lands itself if you respawn or change dimension |
| Zoom, Fullbright | Spyglass zoom, see in the dark |
| Auto Tool (World) | Switches to the fastest hotbar tool for the block you're mining, skipping nearly broken ones, and back when you stop |
| Sprint (Movement) | Always sprint |

### Combat

| Module | What it does |
| --- | --- |
| Trigger Bot | Attacks the entity under your crosshair once your attack has recharged (charge threshold and a small random delay are configurable) |
| Aim Assist | Smoothly pulls your aim toward the nearest target within a FOV cone, eased per frame |
| Auto Totem | Refills your offhand with a totem after one pops. **Inventory** mode (default) opens your inventory, swaps the totem in and closes it again, each step after a random delay in the range you set; **Instant** swaps without opening anything. **Hover Refill** also refills while you have your inventory open yourself |
| Auto Crystal | Hold right click with end crystals: places a crystal on the obsidian you aim at and breaks crystals under your crosshair at a random speed between Min and Max CPS. Can put obsidian down first, and pauses after a nearby kill so loot survives |
| Auto Anchor | Look at a respawn anchor while holding right click: charges it with glowstone, switches to your detonate slot, blows it and picks anchors back up. Random delays between steps plus a skip chance; Only Own / Only Charge / Loot Protect options. Anchors only explode outside the Nether |

### Misc

| Module | What it does |
| --- | --- |
| Admin Detector | See the base-finding table above |
| Name Protect | Replaces your username in everything drawn on screen (chat, tab, scoreboard, tags), for recordings |
| Auto Reconnect | Reconnect button on the disconnect screen, plus an automatic countdown while enabled |
| Auto Log | Disconnects on low health, when staff come online, or when a player gets close; then turns itself off |
| Auto Eat | Eats the best hotbar food when hunger drops, skipping bad food |
| Fast Place | Shortens the delay between placements |
| Key Pearl | Bind a key to throw an ender pearl (or wind charge) from anywhere in your hotbar |
| Target HUD (HUD) | Face, name, smooth health bar, distance and gear of whoever you're fighting |

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
│   └── impl/                basefinding · client · movement · render · world modules
├── config/                  debounced JSON persistence
├── camera/                  CameraController, CameraMode, FreeCamera, OrbitCamera,
│                            FirstPersonRenderer, PlayerControlLock
├── world/                   ChunkScanner, BlockEntityTracker, BlockUpdates, Finds
├── render/                  WorldOverlay (see-through boxes and lines), Projector (world → HUD)
├── ui/
│   ├── OogaTheme.java       colour, radius and spacing tokens
│   ├── render/              Render2D (pixel-exact shapes), GlowRenderer, OogaFonts, Icon
│   ├── clickgui/            the menu and its widgets
│   ├── hud/                 watermark, module list, info, keystrokes, finds, radar, HUD editor
│   └── notify/              toast notifications
└── mixin/                   the only code that touches Minecraft internals
```

The UI typeface is [Inter](https://rsms.me/inter/) (SIL Open Font License, see
`assets/ooga/font/INTER_LICENSE.txt`).
