# 🩸 LivesSystem

A Minecraft plugin for **Mohist 1.20.1** (Forge + Bukkit hybrid) that adds a fully configurable lives system to your server. Perfect for SMPs, hardcore events, and high-stakes challenges.

Lives are tracked **completely separately from normal hearts** — player HP is never touched.

---

## 📋 Requirements

| Requirement | Version |
|---|---|
| Minecraft | 1.20.1 |
| Server software | Mohist, Spigot, Paper, Purpur |
| Java (to build) | JDK 17 or newer |
| Maven (to build) | 3.6+ |

---

## 🔨 Building the Plugin

You cannot drop the source code directly into your server — you need to compile it first.

### Option A — Build locally
1. Install [JDK 17+](https://adoptium.net) and [Maven](https://maven.apache.org/download.cgi)
2. Open a terminal inside the `livessystem/` folder (the one with `pom.xml`)
3. Run:
   ```
   mvn package
   ```
4. Your compiled plugin will be at:
   ```
   target/livessystem-1.0.0.jar
   ```

### Option B — Build via GitHub Actions (no local install needed)
1. Create a free [GitHub](https://github.com) account
2. Create a new repository and upload all files from this folder
3. Go to the **Actions** tab in your repository
4. Click **"set up a workflow yourself"**, paste in the contents of `.github/workflows/build.yml`, and commit
5. The build will run automatically — wait for the green checkmark ✅
6. Go to **Actions → latest run → Artifacts** and download **LivesSystem-Plugin**
7. Extract the zip — inside is your `.jar`

---

## 🚀 Installation

1. Drop `livessystem-1.0.0.jar` into your server's `plugins/` folder
2. Restart the server
3. A `plugins/LivesSystem/config.yml` will be generated automatically

> No datapack is required. Crafting recipes are handled entirely by the plugin and configured in-game.

---

## ✨ Features

- **Lives are separate from hearts** — normal HP is completely untouched
- **Configurable lives count** — set starting lives, max lives, and revive lives in `config.yml`
- **Spectator mode on elimination** — players with 0 lives are automatically moved to Spectator and restored on revive
- **Revive Book** — right-click to open a paginated GUI showing all eliminated players as their own heads; click a head to revive them
- **Life Token** — right-click in hand to instantly gain extra lives
- **Life withdrawing** — players can convert their own lives into Life Tokens to share with others
- **In-game recipe editor** — change crafting recipes for both items without ever editing a file or restarting
- **Shaped and shapeless recipes** — toggle between recipe types in the editor GUI
- **TAB list display** — shows each player's lives next to their name, color-coded by how many they have left
- **Action bar messages** — death, elimination, and revive notifications shown on screen
- **Fully configurable** — messages, sounds, TAB format, item names, lore, and recipes all configurable

---

## 🎒 Items

### 📖 Revive Book
Used to bring eliminated players back into the game.

**How to use:**
1. Hold the Revive Book in your main hand
2. Right-click — a chest GUI opens showing all eliminated players as their own heads
3. Click a player's head to revive them
4. The book is consumed on use (configurable in `config.yml`)

**How to obtain:**
- Admin command: `/revivebook [player]`
- Crafting table (default recipe below, changeable in-game via `/editrevivebookrecipe`)

**Default recipe (shaped):**
```
N S N
S B S    N = Nether Star  |  S = Soul Sand  |  B = Enchanted Book
N S N
```

---

### ⭐ Life Token
Used to grant yourself an extra life.

**How to use:**
1. Hold the Life Token in your main hand
2. Right-click — you instantly gain lives (amount set by `life-token-lives` in config)
3. The token is consumed on use (configurable)

**How to obtain:**
- Admin command: `/lifetoken [player]`
- Crafting table (default recipe below, changeable in-game via `/editlifetokenrecipe`)
- Another player using `/withdrawlife` to convert their own life into a token

**Default recipe (shaped):**
```
G I G
I N I    G = Gold Nugget  |  I = Iron Nugget  |  N = Nether Star
G I G
```

---

### ⚠️ Item Identification Note
Items are identified by their **display name**. This means:
- Any item renamed to match (e.g. via anvil) will be treated as that item
- If you change an item's `name` in `config.yml`, make sure to also update the recipe via `/editrevivebookrecipe` or `/editlifetokenrecipe` so the crafted result still gets recognised
- The admin commands (`/revivebook`, `/lifetoken`) always give correctly named items

---

## 🔄 Revive GUI

When a player right-clicks with a Revive Book, a **54-slot chest GUI** opens:

```
[ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ]
[ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ]
[ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ]
[ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ]
[ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ]
[« Prev][ . ][ . ][  Page X / Y  ][ . ][ . ][Next »]
```

- Each head shows the eliminated player's name and their current lives count
- Supports **unlimited players** — 45 per page, as many pages as needed
- Clicking a head revives that player and closes the menu
- The Revive Book is consumed after a successful revive

---

## 🧰 Recipe Editor

Run `/editrevivebookrecipe` or `/editlifetokenrecipe` to open the in-game recipe editor. No restart or file editing required.

```
[ . ][ . ][ . ][ . ][ . ][ . ][ . ][ . ][ . ]
[ . ][ 1 ][ 2 ][ 3 ][ . ][ . ][OUT][ . ][ . ]
[ . ][ 4 ][ 5 ][ 6 ][ . ][ ▶ ][ . ][ . ][ . ]
[ . ][ 7 ][ 8 ][ 9 ][ . ][ . ][ . ][ . ][ . ]
[ . ][ . ][ . ][ . ][ . ][ . ][ . ][ . ][ . ]
[SAV][ . ][ . ][ . ][MOD][ . ][ . ][ . ][CLO]
```

| Button | Description |
|---|---|
| Slots 1–9 | The crafting grid — drag items from your inventory into these slots |
| OUT | Preview of the resulting named item (display only) |
| MOD | Toggle between **Shaped** (position matters) and **Shapeless** (any order) |
| SAV | Save the recipe and activate it immediately |
| CLO | Close without saving — items are returned to your inventory |

**Tips:**
- Shift-click an item from your inventory to send it straight to the first empty grid slot
- Any items left in the grid when you close or save are automatically returned to you
- The recipe activates instantly on save — no reload or restart needed
- Recipes are saved to `config.yml` and persist across restarts

---

## ⌨️ Commands

| Command | Description | Permission |
|---|---|---|
| `/lives [player]` | Check your own or another player's lives | `livessystem.lives` |
| `/setlives <player> <amount>` | Set a player's lives to a specific amount | `livessystem.admin` |
| `/addlives <player> <amount>` | Add lives to a player | `livessystem.admin` |
| `/removelives <player> <amount>` | Remove lives from a player | `livessystem.admin` |
| `/revivebook [player]` | Give a Revive Book to yourself or a player | `livessystem.admin` |
| `/lifetoken [player]` | Give a Life Token to yourself or a player | `livessystem.admin` |
| `/withdrawlife [amount]` | Convert one or more of your lives into Life Tokens | `livessystem.withdraw` |
| `/editrevivebookrecipe` | Open the recipe editor for the Revive Book | `livessystem.admin` |
| `/editlifetokenrecipe` | Open the recipe editor for the Life Token | `livessystem.admin` |
| `/livesreload` | Reload `config.yml` without restarting | `livessystem.admin` |
| `/livesreset` | Wipe all lives data and start fresh | `livessystem.admin` |

---

## 🔑 Permissions

| Permission | Description | Default |
|---|---|---|
| `livessystem.lives` | Check lives with `/lives` | Everyone |
| `livessystem.admin` | All admin commands | OP only |
| `livessystem.withdraw` | Use `/withdrawlife` | Everyone |
| `livessystem.bypass` | Never lose lives on death | Nobody |

---

## ⚙️ Configuration (`config.yml`)

Found at `plugins/LivesSystem/config.yml` after first launch. Use `/livesreload` to apply changes without restarting.

```yaml
# Lives settings
starting-lives: 5         # Lives each player starts with on first join
max-lives: 10             # Maximum lives a player can hold at once
revive-lives: 1           # Lives given to a player when revived
life-token-lives: 1       # Lives granted by one Life Token
withdraw-min-lives: 2     # Minimum lives a player must keep when withdrawing

# TAB list
tab-display: true
tab-format: "&c❤ %lives% lives"   # %lives% = current lives count
tab-colors:
  high: "&a"              # 4+ lives (green)
  medium: "&e"            # 2-3 lives (yellow)
  low: "&c"               # 1 life (red)
  dead: "&8"              # eliminated (grey)

# Action bar notifications
action-bar: true

# Revive Book item
revive-book:
  material: BOOK
  name: "&6&lRevive Book"
  lore:
    - "&7Right-click to open the revive menu."
    - ""
    - "&eGrants: &c%revive-lives% life/lives on revive"
  consume-on-use: true

# Life Token item
life-token:
  material: NETHER_STAR
  name: "&b&lLife Token"
  lore:
    - "&7Right-click to gain an extra life."
    - ""
    - "&eGrants: &a+%token-lives% life/lives"
  consume-on-use: true

# Sounds (use Bukkit sound names)
sounds:
  death: ENTITY_WITHER_DEATH
  eliminated: ENTITY_WITHER_SPAWN
  revived: ENTITY_PLAYER_LEVELUP
  life-token: ENTITY_EXPERIENCE_ORB_PICKUP
```

All messages are also configurable under the `messages:` section. Color codes use the `&` prefix — e.g. `&c` = red, `&a` = green, `&e` = yellow, `&l` = bold.

> **Note:** Recipe grids are stored under `revive-book.recipe` and `life-token.recipe` in `config.yml`. These are managed automatically by the in-game editor — you generally don't need to edit them manually.

---

## 🔧 Compatibility

| Software | Compatible |
|---|---|
| Mohist 1.20.1 (Forge + Bukkit) | ✅ Yes |
| Paper 1.20.1 | ✅ Yes |
| Spigot 1.20.1 | ✅ Yes |
| Purpur 1.20.1 | ✅ Yes |
| Vanilla / Fabric / pure Forge | ❌ No — requires Bukkit API |
| Versions other than 1.20.x | ⚠️ Untested |

---

## 📁 File Structure

```
plugins/
  LivesSystem/
    config.yml    ← all configuration including saved recipes
    data.yml      ← player lives data (auto-generated, do not edit manually)
```
