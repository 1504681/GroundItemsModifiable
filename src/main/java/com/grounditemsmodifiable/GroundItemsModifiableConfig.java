/*
 * Copyright (c) 2017, Aria <aria@ar1as.space>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.grounditemsmodifiable;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;
import com.grounditemsmodifiable.config.BarWidth;
import com.grounditemsmodifiable.config.DespawnTimerMode;
import com.grounditemsmodifiable.config.HighlightTier;
import com.grounditemsmodifiable.config.ItemHighlightMode;
import com.grounditemsmodifiable.config.MenuHighlightMode;
import com.grounditemsmodifiable.config.PriceDisplayMode;
import com.grounditemsmodifiable.config.QuantityFormat;
import com.grounditemsmodifiable.config.TileColorMode;
import com.grounditemsmodifiable.config.TileFillMode;
import com.grounditemsmodifiable.config.TileShape;
import com.grounditemsmodifiable.config.TimerVisibility;
import com.grounditemsmodifiable.config.ValueCalculationMode;
import com.grounditemsmodifiable.config.OwnershipFilterMode;

@ConfigGroup(GroundItemsModifiableConfig.GROUP)
public interface GroundItemsModifiableConfig extends Config
{
	String GROUP = "grounditemsmodifiable";
	String OWNERSHIP_FILTER_MODE = "ownershipFilterMode";

	@ConfigItem(
		keyName = "exclusiveWithCore",
		name = "Replace built-in Ground Items",
		description = "Turn the built-in Ground Items plugin off while this one is on, and turn this one off if you switch the built-in plugin back on.",
		position = -1
	)
	default boolean exclusiveWithCore()
	{
		return true;
	}

	@ConfigSection(
		name = "Item lists",
		description = "Which items are highlighted, hidden or shown at all.",
		position = 0
	)
	String itemLists = "itemLists";

	@ConfigItem(
		keyName = "highlightedItems",
		name = "Highlighted items",
		description = "Configures specifically highlighted ground items. Format: item1, item2, item3",
		position = 0,
		section = itemLists
	)
	default String getHighlightItems()
	{
		return "";
	}

	@ConfigItem(
		keyName = "highlightedItems",
		name = "",
		description = ""
	)
	void setHighlightedItem(String key);

	@ConfigItem(
		keyName = "hiddenItems",
		name = "Hidden items",
		description = "Configures hidden ground items. Format: item1, item2, item3",
		position = 1,
		section = itemLists
	)
	default String getHiddenItems()
	{
		return "Vial, Ashes, Coins, Bones, Bucket, Jug, Seaweed";
	}

	@ConfigItem(
		keyName = "hiddenItems",
		name = "",
		description = ""
	)
	void setHiddenItems(String key);

	@ConfigItem(
		keyName = "showHighlightedOnly",
		name = "Show highlighted items only",
		description = "Configures whether or not to draw items only on your highlighted list.",
		position = 2,
		section = itemLists
	)
	default boolean showHighlightedOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dontHideUntradeables",
		name = "Do not hide untradeables",
		description = "Configures whether or not untradeable items ignore hiding under settings.",
		position = 3,
		section = itemLists
	)
	default boolean dontHideUntradeables()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideUnderValue",
		name = "Hide under value",
		description = "Configures hidden ground items under both GE and HA value.",
		position = 4,
		section = itemLists
	)
	default int getHideUnderValue()
	{
		return 0;
	}

	@ConfigItem(
		keyName = OWNERSHIP_FILTER_MODE,
		name = "Ownership filter",
		description = "Show all items, takeable items, or only your or your group's drops.",
		position = 5,
		section = itemLists
	)
	default OwnershipFilterMode ownershipFilterMode()
	{
		return OwnershipFilterMode.ALL;
	}

	@ConfigSection(
		name = "Ground overlay",
		description = "What is drawn on the ground for each item.",
		position = 1
	)
	String overlaySection = "overlaySection";

	@ConfigItem(
		keyName = "itemHighlightMode",
		name = "Item highlight mode",
		description = "Configures how ground items will be highlighted.",
		position = 0,
		section = overlaySection
	)
	default ItemHighlightMode itemHighlightMode()
	{
		return ItemHighlightMode.BOTH;
	}

	@ConfigItem(
		keyName = "priceDisplayMode",
		name = "Price display mode",
		description = "Configures which price types are shown alongside ground item name.",
		position = 1,
		section = overlaySection
	)
	default PriceDisplayMode priceDisplayMode()
	{
		return PriceDisplayMode.BOTH;
	}

	@ConfigItem(
		keyName = "quantityFormat",
		name = "Stack size format",
		description = "How the stack size is written after the name: 'Prayer potion(4) x 3' or 'Prayer potion(4) (3)'.",
		position = 2,
		section = overlaySection
	)
	default QuantityFormat quantityFormat()
	{
		return QuantityFormat.TIMES;
	}

	@ConfigItem(
		keyName = "textOutline",
		name = "Text outline",
		description = "Use an outline around text instead of a text shadow.",
		position = 3,
		section = overlaySection
	)
	default boolean textOutline()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showItemIcons",
		name = "Show item icons",
		description = "Draw the item's icon next to its name on the ground.",
		position = 4,
		section = overlaySection
	)
	default boolean showItemIcons()
	{
		return true;
	}

	@Range(min = 8, max = 48)
	@ConfigItem(
		keyName = "iconSize",
		name = "Icon size",
		description = "Height of the ground icons in pixels.",
		position = 5,
		section = overlaySection
	)
	@Units(Units.PIXELS)
	default int iconSize()
	{
		return 16;
	}

	@ConfigItem(
		keyName = "compactMode",
		name = "Icons only until hovered",
		description = "Draw only the icon on the ground. Hover a tile's icons or hold the hotkey to see names, prices and totals. Needs item icons on.",
		position = 6,
		section = overlaySection
	)
	default boolean compactMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "compactQuantity",
		name = "Stack size next to icon",
		description = "In icons-only mode, draw the stack size after the icon ('x 3' or '(3)' per the stack size format) in the item's colour.",
		position = 7,
		section = overlaySection
	)
	default boolean compactQuantity()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTileTotal",
		name = "Tile value total",
		description = "When two or more items share a tile, draw a total value line above them, coloured by value tier.",
		position = 8,
		section = overlaySection
	)
	default boolean showTileTotal()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tileTotalMinimum",
		name = "Total minimum value",
		description = "Only show the tile total when it is at least this much.",
		position = 9,
		section = overlaySection
	)
	default int tileTotalMinimum()
	{
		return 0;
	}

	@ConfigSection(
		name = "Colours and value tiers",
		description = "Item colours and the price thresholds behind them.",
		position = 2
	)
	String colorSection = "colorSection";

	@Alpha
	@ConfigItem(
		keyName = "defaultColor",
		name = "Default items",
		description = "Configures the color for default, non-highlighted items.",
		position = 0,
		section = colorSection
	)
	default Color defaultColor()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightedColor",
		name = "Highlighted items",
		description = "Configures the color for highlighted items.",
		position = 1,
		section = colorSection
	)
	default Color highlightedColor()
	{
		return Color.decode("#AA00FF");
	}

	@Alpha
	@ConfigItem(
		keyName = "hiddenColor",
		name = "Hidden items",
		description = "Configures the color for hidden items in right-click menu and when holding ALT.",
		position = 2,
		section = colorSection
	)
	default Color hiddenColor()
	{
		return Color.GRAY;
	}

	@ConfigItem(
		keyName = "highlightValueCalculation",
		name = "Highlight value calculation",
		description = "Configures which coin value is used to determine highlight color.",
		position = 3,
		section = colorSection
	)
	default ValueCalculationMode valueCalculationMode()
	{
		return ValueCalculationMode.HIGHEST;
	}

	@Alpha
	@ConfigItem(
		keyName = "lowValueColor",
		name = "Low value items",
		description = "Configures the color for low value items.",
		position = 4,
		section = colorSection
	)
	default Color lowValueColor()
	{
		return Color.decode("#66B2FF");
	}

	@ConfigItem(
		keyName = "lowValuePrice",
		name = "Low value price",
		description = "Configures the start price for low value items.",
		position = 5,
		section = colorSection
	)
	default int lowValuePrice()
	{
		return 20000;
	}

	@Alpha
	@ConfigItem(
		keyName = "mediumValueColor",
		name = "Medium value items",
		description = "Configures the color for medium value items.",
		position = 6,
		section = colorSection
	)
	default Color mediumValueColor()
	{
		return Color.decode("#99FF99");
	}

	@ConfigItem(
		keyName = "mediumValuePrice",
		name = "Medium value price",
		description = "Configures the start price for medium value items.",
		position = 7,
		section = colorSection
	)
	default int mediumValuePrice()
	{
		return 100000;
	}

	@Alpha
	@ConfigItem(
		keyName = "highValueColor",
		name = "High value items",
		description = "Configures the color for high value items.",
		position = 8,
		section = colorSection
	)
	default Color highValueColor()
	{
		return Color.decode("#FF9600");
	}

	@ConfigItem(
		keyName = "highValuePrice",
		name = "High value price",
		description = "Configures the start price for high value items.",
		position = 9,
		section = colorSection
	)
	default int highValuePrice()
	{
		return 1000000;
	}

	@Alpha
	@ConfigItem(
		keyName = "insaneValueColor",
		name = "Insane value items",
		description = "Configures the color for insane value items.",
		position = 10,
		section = colorSection
	)
	default Color insaneValueColor()
	{
		return Color.decode("#FF66B2");
	}

	@ConfigItem(
		keyName = "insaneValuePrice",
		name = "Insane value price",
		description = "Configures the start price for insane value items.",
		position = 11,
		section = colorSection
	)
	default int insaneValuePrice()
	{
		return 10000000;
	}

	@ConfigSection(
		name = "Tile highlight",
		description = "How tiles containing ground items are marked.",
		position = 3
	)
	String tileSection = "tileSection";

	@ConfigItem(
		keyName = "highlightTiles",
		name = "Highlight tiles",
		description = "Configures whether or not to highlight tiles containing ground items.",
		position = 0,
		section = tileSection
	)
	default boolean highlightTiles()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tileShape",
		name = "Shape",
		description = "Mark the whole tile, or draw a circle in the middle of the tile.",
		position = 1,
		section = tileSection
	)
	default TileShape tileShape()
	{
		return TileShape.TILE;
	}

	@Range(min = 10, max = 100)
	@ConfigItem(
		keyName = "tileCircleSize",
		name = "Circle size",
		description = "Circle diameter as a percentage of the tile width.",
		position = 2,
		section = tileSection
	)
	@Units(Units.PERCENT)
	default int tileCircleSize()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "tileHighlightedOnly",
		name = "Highlighted items only",
		description = "Only draw tiles under highlighted and value-tier items, not every item.",
		position = 3,
		section = tileSection
	)
	default boolean tileHighlightedOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tileOutlineMode",
		name = "Outline colour",
		description = "Use the item's colour for the tile outline, or one custom colour for every tile.",
		position = 4,
		section = tileSection
	)
	default TileColorMode tileOutlineMode()
	{
		return TileColorMode.ITEM_COLOR;
	}

	@Alpha
	@ConfigItem(
		keyName = "tileOutlineColor",
		name = "Custom outline colour",
		description = "Tile outline colour when the outline mode is set to custom.",
		position = 5,
		section = tileSection
	)
	default Color tileOutlineColor()
	{
		return Color.WHITE;
	}

	@Range(min = 0, max = 10)
	@ConfigItem(
		keyName = "tileOutlineWidth",
		name = "Outline width",
		description = "Tile outline thickness in pixels. 0 removes the outline.",
		position = 6,
		section = tileSection
	)
	@Units(Units.PIXELS)
	default int tileOutlineWidth()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "tileFillMode",
		name = "Fill colour",
		description = "Fill the tile with the item's colour, one custom colour, or nothing.",
		position = 7,
		section = tileSection
	)
	default TileFillMode tileFillMode()
	{
		return TileFillMode.ITEM_COLOR;
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "tileFillOpacity",
		name = "Item colour fill opacity",
		description = "Opacity of the fill when the fill mode is the item's colour (0 = invisible, 255 = solid).",
		position = 8,
		section = tileSection
	)
	default int tileFillOpacity()
	{
		return 50;
	}

	@Alpha
	@ConfigItem(
		keyName = "tileFillColor",
		name = "Custom fill colour",
		description = "Tile fill colour (with opacity) when the fill mode is set to custom.",
		position = 9,
		section = tileSection
	)
	default Color tileFillColor()
	{
		return new Color(0, 0, 0, 50);
	}

	@ConfigSection(
		name = "Despawn timer",
		description = "Timer style, when it is shown, and the ring/bar pulse.",
		position = 4
	)
	String timerSection = "timerSection";

	@ConfigItem(
		keyName = "groundItemTimers",
		name = "Despawn timer",
		description = "Shows despawn timers for items you've dropped and received as loot.",
		position = 0,
		section = timerSection
	)
	default DespawnTimerMode groundItemTimers()
	{
		return DespawnTimerMode.OFF;
	}

	@ConfigItem(
		keyName = "timerVisibility",
		name = "Show timer",
		description = "Always, only once the item is public (yellow) or pulsing, or only under a number of ticks left. Holding the hotkey always shows every timer.",
		position = 1,
		section = timerSection
	)
	default TimerVisibility timerVisibility()
	{
		return TimerVisibility.ALWAYS;
	}

	@Range(min = 1, max = 2000)
	@ConfigItem(
		keyName = "timerShowTicks",
		name = "Show timer under",
		description = "Ticks left at which the timer appears when 'Show timer' is set to under N ticks.",
		position = 2,
		section = timerSection
	)
	@Units(Units.TICKS)
	default int timerShowTicks()
	{
		return 100;
	}

	@Range(min = 0, max = 120)
	@ConfigItem(
		keyName = "ringPulseSeconds",
		name = "Pulse under",
		description = "Ring and bar timers pulse once this many seconds are left. 0 disables the pulse.",
		position = 3,
		section = timerSection
	)
	@Units(Units.SECONDS)
	default int ringPulseSeconds()
	{
		return 15;
	}

	@Alpha
	@ConfigItem(
		keyName = "ringPulseColor",
		name = "Pulse colour",
		description = "Colour of the ring or bar while it is pulsing.",
		position = 4,
		section = timerSection
	)
	default Color ringPulseColor()
	{
		return Color.RED;
	}

	@Range(min = 1, max = 6)
	@ConfigItem(
		keyName = "ringWidth",
		name = "Ring / bar thickness",
		description = "Thickness of the ring or bar timer in pixels.",
		position = 5,
		section = timerSection
	)
	@Units(Units.PIXELS)
	default int ringWidth()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "barWidth",
		name = "Bar width",
		description = "Bar timer spans the icon and the name, or just the icon.",
		position = 6,
		section = timerSection
	)
	default BarWidth barWidth()
	{
		return BarWidth.FULL;
	}

	@ConfigSection(
		name = "Renames",
		description = "Custom item names. Rename an item by holding the hotkey and right-clicking it.",
		position = 5
	)
	String renameSection = "renameSection";

	@ConfigItem(
		keyName = "groupDoses",
		name = "Group doses and charges",
		description = "Renaming 'Prayer potion(4)' renames every dose. The item's real dose is appended to the custom name, so you get 'PPot(3)', 'PPot(2)' and so on.",
		position = 0,
		section = renameSection
	)
	default boolean groupDoses()
	{
		return true;
	}

	@ConfigItem(
		keyName = "renames",
		name = "Item renames",
		description = "Your renames, one per line as Real name=Custom name. * works as a wildcard. Use the Rename menu option (hotkey + right-click an item) and its Renames > Export / Import entries rather than editing this by hand.",
		position = 1,
		section = renameSection
	)
	default String renames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "renames",
		name = "",
		description = ""
	)
	void setRenames(String renames);

	@ConfigSection(
		name = "Right-click menu",
		description = "How ground items appear in the right-click menu.",
		position = 6,
		closedByDefault = true
	)
	String menuSection = "menuSection";

	@ConfigItem(
		keyName = "menuHighlightMode",
		name = "Menu highlight mode",
		description = "Configures what to highlight in right-click menu.",
		position = 0,
		section = menuSection
	)
	default MenuHighlightMode menuHighlightMode()
	{
		return MenuHighlightMode.NAME;
	}

	@ConfigItem(
		keyName = "showMenuItemQuantities",
		name = "Show menu item quantities",
		description = "Configures whether or not to show the item quantities in the menu.",
		position = 1,
		section = menuSection
	)
	default boolean showMenuItemQuantities()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recolorMenuHiddenItems",
		name = "Recolor menu hidden items",
		description = "Configures whether or not hidden items in right-click menu will be recolored.",
		position = 2,
		section = menuSection
	)
	default boolean recolorMenuHiddenItems()
	{
		return false;
	}

	@ConfigItem(
		keyName = "deprioritizeHiddenItems",
		name = "Deprioritize menu hidden items",
		description = "Depriotizies the menu options for items which are hidden, requiring a right-click to pick up.",
		position = 3,
		section = menuSection
	)
	default boolean deprioritizeHiddenItems()
	{
		return false;
	}

	@ConfigItem(
		keyName = "collapseEntries",
		name = "Collapse ground item menu",
		description = "Collapses ground item menu entries together and appends count.",
		position = 4,
		section = menuSection
	)
	default boolean collapseEntries()
	{
		return false;
	}

	@ConfigItem(
		keyName = "renameInMenu",
		name = "Custom names in menu",
		description = "Also use your custom item names in the right-click menu. Rename an item by holding the hotkey and right-clicking it.",
		position = 5,
		section = menuSection
	)
	default boolean renameInMenu()
	{
		return true;
	}

	@ConfigSection(
		name = "Lootbeams",
		description = "Lootbeams for highlighted and valuable items.",
		position = 7,
		closedByDefault = true
	)
	String lootbeamSection = "lootbeamSection";

	@ConfigItem(
		keyName = "showLootbeamForHighlighted",
		name = "Highlighted item lootbeams",
		description = "Configures lootbeams to show for all highlighted items.",
		position = 0,
		section = lootbeamSection
	)
	default boolean showLootbeamForHighlighted()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showLootbeamTier",
		name = "Lootbeam tier",
		description = "Configures which price tiers will trigger a lootbeam.",
		position = 1,
		section = lootbeamSection
	)
	default HighlightTier showLootbeamTier()
	{
		return HighlightTier.HIGH;
	}

	@ConfigItem(
		keyName = "lootbeamStyle",
		name = "Lootbeam style",
		description = "Style of lootbeam to use.",
		position = 2,
		section = lootbeamSection
	)
	default Lootbeam.Style lootbeamStyle()
	{
		return Lootbeam.Style.MODERN;
	}

	@ConfigSection(
		name = "Notifications",
		description = "Desktop notifications for drops.",
		position = 8,
		closedByDefault = true
	)
	String notifySection = "notifySection";

	@ConfigItem(
		keyName = "notifyHighlightedDrops",
		name = "Notify for highlighted drops",
		description = "Configures whether or not to notify for drops on your highlighted list.",
		position = 0,
		section = notifySection
	)
	default boolean notifyHighlightedDrops()
	{
		return false;
	}

	@ConfigItem(
		keyName = "notifyTier",
		name = "Notify tier",
		description = "Configures which price tiers will trigger a notification on drop.",
		position = 1,
		section = notifySection
	)
	default HighlightTier notifyTier()
	{
		return HighlightTier.OFF;
	}

	@ConfigSection(
		name = "Hotkey",
		description = "The key that shows hidden items, the hide/highlight boxes and the rename menu.",
		position = 9,
		closedByDefault = true
	)
	String hotkeySection = "hotkeySection";

	@ConfigItem(
		keyName = "hotkey",
		name = "Hotkey",
		description = "Configures the hotkey used by the Ground Items plugin.",
		position = 0,
		section = hotkeySection
	)
	default Keybind hotkey()
	{
		return Keybind.ALT;
	}

	@ConfigItem(
		keyName = "doubleTapDelay",
		name = "Double-tap delay",
		description = "Delay for the double-tap ALT to hide ground items. 0 to disable.",
		position = 1,
		section = hotkeySection
	)
	@Units(Units.MILLISECONDS)
	default int doubleTapDelay()
	{
		return 250;
	}

}
