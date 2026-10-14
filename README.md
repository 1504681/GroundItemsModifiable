# Ground Items Modifiable

A fork of RuneLite's built-in Ground Items plugin with three additions:

- **Tile fill and outline you control.** Mark the whole tile or just a circle in its centre. Tiles can use the item's colour or one custom colour for the outline and the fill, with separate opacity and outline width. One polygon per tile; the highlighted item on a tile decides its colour.
- **Rename ground items.** Hold the hotkey (ALT by default), right-click an item and pick **Rename**. The custom name is used on the ground, in the right-click menu and when matching your highlight/hidden lists. **Reset name** or an empty rename restores the real name. Hotkey + right-click also offers **Renames > Export to clipboard** and **Import from clipboard** (or `::exportrenames` / `::importrenames` in chat); import merges into what you have. `*` works as a wildcard (`Grimy *=Herb`). With **Group doses and charges** on, renaming `Prayer potion(4)` to `PPot` covers every dose, and each item keeps its real dose: `PPot(3)`, `PPot(2)`, on the ground and in the menu.
- **Item icons on the ground.** Every ground item label gets its icon drawn next to it, on by default.
- **Ring despawn timer.** Set *Despawn timer* to **Ring** and the timer drains clockwise around the icon (or a small ring left of the name with icons off). Under a configurable number of seconds it pulses in a colour of your choice, faster in the last five seconds.
- **Tile value total.** When two or more items share a tile, one extra line above the stack shows their summed value, coloured by your value tiers.

Everything else (highlight/hidden lists, value tiers, lootbeams, despawn timers, ownership filter, menu recolouring, per-item colours) works as in the original plugin. On first start the plugin copies all of your Ground Items settings, so disable the built-in Ground Items plugin and this one picks up where it left off.

## Settings

Tile highlight

| Setting | Default | What it does |
|---|---|---|
| Highlight tiles | off | Draw tiles under ground items |
| Shape | Whole tile | Outline the whole tile, or draw a circle in its middle |
| Circle size | 40 % | Circle diameter relative to the tile width |
| Highlighted items only | off | Only tiles of highlighted and value-tier items |
| Outline colour | Item colour | Item colour or custom colour |
| Custom outline colour | white | Used when outline colour is custom |
| Outline width | 2 px | 0 removes the outline |
| Fill colour | Item colour | Item colour, custom colour or no fill |
| Item colour fill opacity | 50 | 0–255, applies to item-colour fill |
| Custom fill colour | black, 50 alpha | Used when fill colour is custom |

Timer ring and tile totals

| Setting | Default | What it does |
|---|---|---|
| Ring pulse under | 15 s | Pulse the ring with this much time left, 0 disables |
| Ring pulse colour | red | Colour while pulsing |
| Ring width | 2 px | Ring thickness |
| Tile value total | on | Total line above tiles with 2+ items |
| Total minimum value | 0 | Hide totals below this value |

Icons and names

| Setting | Default | What it does |
|---|---|---|
| Show item icons | on | Icon next to the name on the ground |
| Icon size | 16 px | Icon height |
| Custom names in menu | on | Use renamed items in the right-click menu too |
| Stack size format | x 3 | `Prayer potion(4) x 3` or `Prayer potion(4) (3)` |
| Group doses and charges | on | `(4)`, `(3)`... suffixes share one rename |
| Item renames | empty | Your renames as `Real name=Custom name` lines, written by the Rename menu |

## Changelog

- 1.1.2: grouped renames keep the item's real dose ("Ovl (2)" stays a 2-dose) on the ground and in the right-click menu
- 1.1.1: stack size shown as "x 3" instead of "(3)", with a format setting
- 1.1.0: rename export/import via clipboard, wildcard renames, dose grouping, ring despawn timer with pulse, tile value totals, circle tile marks
- 1.0.0: initial release
