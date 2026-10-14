# Ground Items Modifiable

A fork of RuneLite's built-in Ground Items plugin with three additions:

- **Tile fill and outline you control.** Mark the whole tile or just a circle in its centre. Tiles can use the item's colour or one custom colour for the outline and the fill, with separate opacity and outline width. One polygon per tile; the highlighted item on a tile decides its colour.
- **Rename ground items.** Hold the hotkey (ALT by default), right-click an item and pick **Rename**. The custom name is used on the ground, in the right-click menu and when matching your highlight/hidden lists. **Reset name** or an empty rename restores the real name. Hotkey + right-click also offers **Renames > Export to clipboard** and **Import from clipboard** (or `::exportrenames` / `::importrenames` in chat); import merges into what you have. `*` works as a wildcard (`Grimy *=Herb`). With **Group doses and charges** on, renaming `Prayer potion(4)` to `PPot` covers every dose, and each item keeps its real dose: `PPot(3)`, `PPot(2)`, on the ground and in the menu.
- **Item icons on the ground.** Every ground item label gets its icon drawn next to it, on by default. **Icons only until hovered** strips the ground down to icons; hover a tile's icons or hold the hotkey and its names, prices and total appear.
- **Ring and bar despawn timers.** Set *Despawn timer* to **Ring** (drains clockwise around the icon) or **Bar** (a thin bar under the icon and name that shrinks from the right). Under a configurable number of seconds both pulse in a colour of your choice, faster in the last five seconds. **Show timer** can hide any timer until the item is public or pulsing, or until fewer than N ticks remain; holding the hotkey always shows every timer.
- **Tile value total.** When two or more items share a tile, one extra line above the stack shows their summed value, coloured by your value tiers.

Everything else (highlight/hidden lists, value tiers, lootbeams, despawn timers, ownership filter, menu recolouring, per-item colours) works as in the original plugin. On first start the plugin copies all of your Ground Items settings (highlight and hidden lists, colours, value tiers, everything) and turns the built-in Ground Items plugin off, so it picks up where that one left off. Switching the built-in plugin back on turns this one off; **Replace built-in Ground Items** at the top of the settings disables that. `::importgrounditems` in chat copies the built-in settings again, replacing what is set here.

## Settings

The panel is grouped into sections: Item lists, Ground overlay, Colours and value tiers, Tile highlight, Despawn timer, Renames, Right-click menu, Lootbeams, Notifications, Hotkey. Settings carried over from Ground Items behave as they did there. The ones this fork adds:

Ground overlay

| Setting | Default | What it does |
|---|---|---|
| Stack size format | x 3 | `Prayer potion(4) x 3` or `Prayer potion(4) (3)` |
| Show item icons | on | Icon next to the name on the ground |
| Icon size | 16 px | Icon height |
| Icons only until hovered | off | Only icons on the ground; hover or hold the hotkey for text |
| Stack size next to icon | on | Icons-only mode shows `x 3` after the icon in the item colour |
| Tile value total | on | Total line above tiles with 2+ items |
| Total minimum value | 0 | Hide totals below this value |

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

Despawn timer

| Setting | Default | What it does |
|---|---|---|
| Despawn timer | Off | Off, pie, ring, bar, ticks or seconds |
| Show timer | Always | Always, public or pulsing only, or under N ticks left |
| Show timer under | 100 ticks | Threshold for the under-N-ticks mode |
| Pulse under | 15 s | Ring and bar pulse with this much time left, 0 disables |
| Pulse colour | red | Colour while pulsing |
| Ring / bar thickness | 2 px | Thickness of the ring or bar |
| Bar width | Icon and name | Bar spans icon and name, or just the icon |

Renames and menu

| Setting | Default | What it does |
|---|---|---|
| Group doses and charges | on | `(4)`, `(3)`... suffixes share one rename, each keeps its real dose |
| Item renames | empty | Your renames as `Real name=Custom name` lines, written by the Rename menu |
| Custom names in menu | on | Use renamed items in the right-click menu too |

## Changelog

- 1.4.0: turns the built-in Ground Items plugin off when this one is on (and the other way round); `::importgrounditems` re-copies its settings
- 1.3.2: icons-only mode shows the stack size next to the icon
- 1.3.1: settings regrouped into themed sections (overlay, colours, tiles, timer, renames, menu, lootbeams, notifications, hotkey)
- 1.3.0: icons-only mode with hover to expand, bar width setting (icon only)
- 1.2.0: bar despawn timer, "Show timer" visibility rule (always / public or pulsing / under N ticks)
- 1.1.2: grouped renames keep the item's real dose ("Ovl (2)" stays a 2-dose) on the ground and in the right-click menu
- 1.1.1: stack size shown as "x 3" instead of "(3)", with a format setting
- 1.1.0: rename export/import via clipboard, wildcard renames, dose grouping, ring despawn timer with pulse, tile value totals, circle tile marks
- 1.0.0: initial release
