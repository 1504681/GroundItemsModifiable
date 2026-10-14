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

	@ConfigSection(
		name = "Item lists",
		description = "The highlighted and hidden item lists.",
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
		position = 2
	)
	default boolean showHighlightedOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dontHideUntradeables",
		name = "Do not hide untradeables",
		description = "Configures whether or not untradeable items ignore hiding under settings.",
		position = 3
	)
	default boolean dontHideUntradeables()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMenuItemQuantities",
		name = "Show menu item quantities",
		description = "Configures whether or not to show the item quantities in the menu.",
		position = 4
	)
	default boolean showMenuItemQuantities()
	{
		return true;
	}

	@ConfigItem(
		keyName = "recolorMenuHiddenItems",
		name = "Recolor menu hidden items",
		description = "Configures whether or not hidden items in right-click menu will be recolored.",
		position = 5
	)
	default boolean recolorMenuHiddenItems()
	{
		return false;
	}

	@ConfigItem(
		keyName = "deprioritizeHiddenItems",
		name = "Deprioritize menu hidden items",
		description = "Depriotizies the menu options for items which are hidden, requiring a right-click to pick up.",
		position = 5
	)
	default boolean deprioritizeHiddenItems()
	{
		return false;
	}

	@ConfigItem(
		keyName = "notifyHighlightedDrops",
		name = "Notify for highlighted drops",
		description = "Configures whether or not to notify for drops on your highlighted list.",
		position = 7
	)
	default boolean notifyHighlightedDrops()
	{
		return false;
	}

	@ConfigItem(
		keyName = "notifyTier",
		name = "Notify tier",
		description = "Configures which price tiers will trigger a notification on drop.",
		position = 8
	)
	default HighlightTier notifyTier()
	{
		return HighlightTier.OFF;
	}

	@ConfigItem(
		keyName = "priceDisplayMode",
		name = "Price display mode",
		description = "Configures which price types are shown alongside ground item name.",
		position = 9
	)
	default PriceDisplayMode priceDisplayMode()
	{
		return PriceDisplayMode.BOTH;
	}

	@ConfigItem(
		keyName = "itemHighlightMode",
		name = "Item highlight mode",
		description = "Configures how ground items will be highlighted.",
		position = 10
	)
	default ItemHighlightMode itemHighlightMode()
	{
		return ItemHighlightMode.BOTH;
	}

	@ConfigItem(
		keyName = "menuHighlightMode",
		name = "Menu highlight mode",
		description = "Configures what to highlight in right-click menu.",
		position = 11
	)
	default MenuHighlightMode menuHighlightMode()
	{
		return MenuHighlightMode.NAME;
	}

	@ConfigItem(
		keyName = "highlightValueCalculation",
		name = "Highlight value calculation",
		description = "Configures which coin value is used to determine highlight color.",
		position = 12
	)
	default ValueCalculationMode valueCalculationMode()
	{
		return ValueCalculationMode.HIGHEST;
	}

	@ConfigItem(
		keyName = "hideUnderValue",
		name = "Hide under value",
		description = "Configures hidden ground items under both GE and HA value.",
		position = 13
	)
	default int getHideUnderValue()
	{
		return 0;
	}

	@Alpha
	@ConfigItem(
		keyName = "defaultColor",
		name = "Default items",
		description = "Configures the color for default, non-highlighted items.",
		position = 14
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
		position = 15
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
		position = 16
	)
	default Color hiddenColor()
	{
		return Color.GRAY;
	}

	@Alpha
	@ConfigItem(
		keyName = "lowValueColor",
		name = "Low value items",
		description = "Configures the color for low value items.",
		position = 17
	)
	default Color lowValueColor()
	{
		return Color.decode("#66B2FF");
	}

	@ConfigItem(
		keyName = "lowValuePrice",
		name = "Low value price",
		description = "Configures the start price for low value items.",
		position = 18
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
		position = 19
	)
	default Color mediumValueColor()
	{
		return Color.decode("#99FF99");
	}

	@ConfigItem(
		keyName = "mediumValuePrice",
		name = "Medium value price",
		description = "Configures the start price for medium value items.",
		position = 20
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
		position = 21
	)
	default Color highValueColor()
	{
		return Color.decode("#FF9600");
	}

	@ConfigItem(
		keyName = "highValuePrice",
		name = "High value price",
		description = "Configures the start price for high value items.",
		position = 22
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
		position = 23
	)
	default Color insaneValueColor()
	{
		return Color.decode("#FF66B2");
	}

	@ConfigItem(
		keyName = "insaneValuePrice",
		name = "Insane value price",
		description = "Configures the start price for insane value items.",
		position = 24
	)
	default int insaneValuePrice()
	{
		return 10000000;
	}

	@ConfigItem(
		keyName = OWNERSHIP_FILTER_MODE,
		name = "Ownership filter",
		description = "Show all items, takeable items, or only your or your group's drops.",
		position = 25
	)
	default OwnershipFilterMode ownershipFilterMode()
	{
		return OwnershipFilterMode.ALL;
	}

	@ConfigItem(
		keyName = "doubleTapDelay",
		name = "Double-tap delay",
		description = "Delay for the double-tap ALT to hide ground items. 0 to disable.",
		position = 26
	)
	@Units(Units.MILLISECONDS)
	default int doubleTapDelay()
	{
		return 250;
	}

	@ConfigItem(
		keyName = "collapseEntries",
		name = "Collapse ground item menu",
		description = "Collapses ground item menu entries together and appends count.",
		position = 27
	)
	default boolean collapseEntries()
	{
		return false;
	}

	@ConfigItem(
		keyName = "groundItemTimers",
		name = "Despawn timer",
		description = "Shows despawn timers for items you've dropped and received as loot.",
		position = 28
	)
	default DespawnTimerMode groundItemTimers()
	{
		return DespawnTimerMode.OFF;
	}

	@ConfigItem(
		keyName = "textOutline",
		name = "Text outline",
		description = "Use an outline around text instead of a text shadow.",
		position = 29
	)
	default boolean textOutline()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showLootbeamForHighlighted",
		name = "Highlighted item lootbeams",
		description = "Configures lootbeams to show for all highlighted items.",
		position = 30
	)
	default boolean showLootbeamForHighlighted()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showLootbeamTier",
		name = "Lootbeam tier",
		description = "Configures which price tiers will trigger a lootbeam.",
		position = 31
	)
	default HighlightTier showLootbeamTier()
	{
		return HighlightTier.HIGH;
	}

	@ConfigItem(
		keyName = "lootbeamStyle",
		name = "Lootbeam style",
		description = "Style of lootbeam to use.",
		position = 32
	)
	default Lootbeam.Style lootbeamStyle()
	{
		return Lootbeam.Style.MODERN;
	}

	@ConfigItem(
		keyName = "hotkey",
		name = "Hotkey",
		description = "Configures the hotkey used by the Ground Items plugin.",
		position = 33
	)
	default Keybind hotkey()
	{
		return Keybind.ALT;
	}

	@ConfigSection(
		name = "Tile highlight",
		description = "How tiles containing ground items are drawn.",
		position = 40
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
		name = "Despawn timer and tile totals",
		description = "Ring and bar despawn timers, when timers are shown, and the per-tile value total.",
		position = 42
	)
	String ringSection = "ringSection";

	@Range(min = 0, max = 120)
	@ConfigItem(
		keyName = "ringPulseSeconds",
		name = "Pulse under",
		description = "Ring and bar timers pulse once this many seconds are left. 0 disables the pulse.",
		position = 0,
		section = ringSection
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
		position = 1,
		section = ringSection
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
		position = 2,
		section = ringSection
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
		position = 3,
		section = ringSection
	)
	default BarWidth barWidth()
	{
		return BarWidth.FULL;
	}

	@ConfigItem(
		keyName = "timerVisibility",
		name = "Show timer",
		description = "Always, only once the item is public (yellow) or pulsing, or only under a number of ticks left. Holding the hotkey always shows every timer.",
		position = 10,
		section = ringSection
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
		position = 11,
		section = ringSection
	)
	@Units(Units.TICKS)
	default int timerShowTicks()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "showTileTotal",
		name = "Tile value total",
		description = "When two or more items share a tile, draw a total value line above them, coloured by value tier.",
		position = 20,
		section = ringSection
	)
	default boolean showTileTotal()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tileTotalMinimum",
		name = "Total minimum value",
		description = "Only show the tile total when it is at least this much.",
		position = 21,
		section = ringSection
	)
	default int tileTotalMinimum()
	{
		return 0;
	}

	@ConfigSection(
		name = "Icons and names",
		description = "Item icons drawn on the ground and custom item names.",
		position = 41
	)
	String iconSection = "iconSection";

	@ConfigItem(
		keyName = "showItemIcons",
		name = "Show item icons",
		description = "Draw the item's icon next to its name on the ground.",
		position = 0,
		section = iconSection
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
		position = 1,
		section = iconSection
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
		position = 2,
		section = iconSection
	)
	default boolean compactMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "renameInMenu",
		name = "Custom names in menu",
		description = "Also use your custom item names in the right-click menu. Rename an item by holding the hotkey and right-clicking it.",
		position = 3,
		section = iconSection
	)
	default boolean renameInMenu()
	{
		return true;
	}

	@ConfigItem(
		keyName = "quantityFormat",
		name = "Stack size format",
		description = "How the stack size is written after the name: 'Prayer potion(4) x 3' or 'Prayer potion(4) (3)'.",
		position = 5,
		section = iconSection
	)
	default QuantityFormat quantityFormat()
	{
		return QuantityFormat.TIMES;
	}

	@ConfigItem(
		keyName = "groupDoses",
		name = "Group doses and charges",
		description = "Renaming 'Prayer potion(4)' renames every dose. The item's real dose is appended to the custom name, so you get 'PPot(3)', 'PPot(2)' and so on.",
		position = 3,
		section = iconSection
	)
	default boolean groupDoses()
	{
		return true;
	}

	@ConfigItem(
		keyName = "renames",
		name = "Item renames",
		description = "Your renames, one per line as Real name=Custom name. * works as a wildcard. Use the Rename menu option (hotkey + right-click an item) and its Renames > Export / Import entries rather than editing this by hand.",
		position = 4,
		section = iconSection
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
}
