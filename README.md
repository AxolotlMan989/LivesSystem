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

### Plugin
1. Drop `livessystem-1.0.0.jar` into your server's `plugins/` folder
2. Restart the server
3. A `plugins/LivesSystem/config.yml` will be generated automatically

### Datapack (for crafting recipes)
The crafting recipes for the **Life Token** and **Revive Book** are provided as a separate datapack.

1. Place `livessystem-datapack.zip` into your world's `datapacks/` folder:
   ```
   world/
     datapacks/
       livessystem-datapack.zip
   ```
2. Run `/reload` in-game or restart the server
3. Run `/datapack list` to confirm it shows as enabled

> ⚠️ Without the datapack, items can only be obtained via the `/lifetoken` and `/revivebook` admin commands.

---

## ✨ Features

- **Lives are separate from hearts** — normal HP is completely untouched
- **Configurable lives count** — set starting lives, max lives, and revive lives in `config.yml`
- **Spectator mode on elimination** — players with 0 lives are automatically moved to Spectator
- **Revive Book** — right-click to open a GUI menu showing all eliminated players; click a head to revive them
- **Life Token** — right-click in hand to instantly gain extra lives
- **Paginated revive GUI** — supports unlimited eliminated players across multiple pages
- **TAB list display** — shows each player's lives count next to their name, color-coded by how many they have left
- **Action bar messages** — death, elimination, and revive messages shown on screen
- **Craftable items via datapack** — both items have shapeless crafting recipes
- **Fully configurable** — messages, sounds, TAB format, item names and lore all in `config.yml`

---

## 🎒 Items

### 📖 Revive Book
Used to bring eliminated players back into the game.

**How to use:**
1. Hold the Revive Book in your main hand
2. Right-click — a chest GUI will open showing all eliminated players as their own heads
3. Click a player's head to revive them
4. The book is consumed on use (configurable)

**How to obtain:**
- Admin command: `/revivebook [player]`
- Crafting (requires datapack): Book + Totem of Undying + Nether Star

---

### ⭐ Life Token
Used to grant yourself an extra life.

**How to use:**
1. Hold the Life Token in your main hand
2. Right-click — you instantly gain lives up to the configured amount
3. The token is consumed on use (configurable)

**How to obtain:**
- Admin command: `/lifetoken [player]`
- Crafting (requires datapack): Nether Star + Diamond + Gold Ingot

---

### ⚠️ Item Identification Note
Items are identified by their **display name**. This means:
- Any item renamed to match (e.g. via anvil) will be treated as that item
- If you change an item's `name` in `config.yml`, update the datapack recipe result name to match, or players who craft it will get an unrecognised item
- The admin commands (`/revivebook`, `/lifetoken`) always give correctly named items

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
| `/livesreload` | Reload `config.yml` without restarting | `livessystem.admin` |
| `/livesreset` | Wipe all lives data and start fresh | `livessystem.admin` |
| `/withdrawlife [amount]` | Convert one or more of your lives into Life Tokens | `livessystem.withdraw` |

---

## 🔑 Permissions

| Permission | Description | Default |
|---|---|---|
| `livessystem.lives` | Check lives with `/lives` | Everyone |
| `livessystem.admin` | All admin commands | OP only |
| `livessystem.withdraw` | Use /withdrawlife | Everyone |
| `livessystem.bypass` | Never lose lives on death | Nobody |

---

## ⚙️ Configuration (`config.yml`)

Found at `plugins/LivesSystem/config.yml` after first launch.

```yaml
# Lives settings
starting-lives: 5       # Lives each player starts with on first join
max-lives: 10           # Maximum lives a player can hold
revive-lives: 1         # Lives given to a player when revived
life-token-lives: 1     # Lives granted by one Life Token

# TAB list
tab-display: true
tab-format: "&c❤ %lives% lives"
tab-colors:
  high: "&a"            # 4+ lives
  medium: "&e"          # 2-3 lives
  low: "&c"             # 1 life
  dead: "&8"            # eliminated

# Action bar
action-bar: true

# Item names (must match datapack result names if using crafting)
revive-book:
  name: "&6&lRevive Book"
  material: BOOK
  consume-on-use: true

life-token:
  name: "&b&lLife Token"
  material: NETHER_STAR
  consume-on-use: true
```

All messages and sounds are also configurable in `config.yml`. Color codes use the `&` prefix (e.g. `&c` = red, `&a` = green, `&l` = bold).

---

## 📦 Datapack Recipe Reference

Both recipes are **shapeless** (ingredients can go in any order, any slot).

| Item | Ingredients |
|---|---|
| Life Token | Nether Star + Diamond + Gold Ingot |
| Revive Book | Book + Totem of Undying + Nether Star |

To add **modded item ingredients**, edit the `.json` files inside the datapack zip under `data/livessystem/recipes/` and replace an ingredient's `"item"` value with the modded item's namespaced ID, e.g.:
```json
{ "item": "create:copper_casing" }
```

---

## 🔄 Revive GUI

When a player right-clicks with a Revive Book, a **54-slot chest GUI** opens:

```
[ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ]
[ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ]
[ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ]
[ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ]
[ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ][ Head ]
[« Prev][Glass][Glass][  Page X/Y  ][Glass][Glass][Next »]
```

- Each head shows the eliminated player's name and their lives count
- Supports **unlimited players** across as many pages as needed (45 players per page)
- Clicking a head revives that player and closes the menu
- The Revive Book is consumed after a successful revive

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
    config.yml       ← all configuration
    data.yml         ← player lives data (auto-generated, do not edit manually)

world/
  datapacks/
    livessystem-datapack.zip   ← crafting recipes
```
