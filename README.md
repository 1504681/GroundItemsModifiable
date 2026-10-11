# Ground Items Modifiable

A fork of RuneLite's built-in Ground Items plugin with three additions:

- **Tile fill and outline you control.** Highlighted tiles can use the item's colour or one custom colour for the outline and the fill, with separate opacity and outline width. One polygon per tile; the highlighted item on a tile decides its colour.
- **Rename ground items.** Hold the hotkey (ALT by default), right-click an item and pick **Rename**. The custom name is used on the ground, in the right-click menu and when matching your highlight/hidden lists. **Reset name** or an empty rename restores the real name.
- **Item icons on the ground.** Every ground item label gets its icon drawn next to it, on by default.

Everything else (highlight/hidden lists, value tiers, lootbeams, despawn timers, ownership filter, menu recolouring, per-item colours) works as in the original plugin. On first start the plugin copies all of your Ground Items settings, so disable the built-in Ground Items plugin and this one picks up where it left off.

## Settings

Tile highlight

| Setting | Default | What it does |
|---|---|---|
| Highlight tiles | off | Draw tiles under ground items |
| Highlighted items only | off | Only tiles of highlighted and value-tier items |
| Outline colour | Item colour | Item colour or custom colour |
| Custom outline colour | white | Used when outline colour is custom |
| Outline width | 2 px | 0 removes the outline |
| Fill colour | Item colour | Item colour, custom colour or no fill |
| Item colour fill opacity | 50 | 0–255, applies to item-colour fill |
| Custom fill colour | black, 50 alpha | Used when fill colour is custom |

Icons and names

| Setting | Default | What it does |
|---|---|---|
| Show item icons | on | Icon next to the name on the ground |
| Icon size | 16 px | Icon height |
| Custom names in menu | on | Use renamed items in the right-click menu too |

## Changelog

- 1.0.0: initial release
