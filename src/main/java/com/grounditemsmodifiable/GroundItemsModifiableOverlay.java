/*
 * Copyright (c) 2017, Aria <aria@ar1as.space>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
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

import com.grounditemsmodifiable.config.BarWidth;
import com.grounditemsmodifiable.config.CollapsedQuantity;
import com.grounditemsmodifiable.config.DespawnTimerMode;
import static com.grounditemsmodifiable.config.ItemHighlightMode.MENU;
import static com.grounditemsmodifiable.config.ItemHighlightMode.NONE;
import com.grounditemsmodifiable.config.OwnershipFilterMode;
import com.grounditemsmodifiable.config.PriceDisplayMode;
import com.grounditemsmodifiable.config.QuantityFormat;
import com.grounditemsmodifiable.config.TileColorMode;
import com.grounditemsmodifiable.config.TileFillMode;
import com.grounditemsmodifiable.config.TileShape;
import com.grounditemsmodifiable.config.TimerVisibility;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Path2D;
import java.awt.geom.Area;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemLayer;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuAction;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import com.grounditemsmodifiable.config.HighlightTier;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.QuantityFormatter;

public class GroundItemsModifiableOverlay extends Overlay
{
	private static final int MAX_DISTANCE = 2500;
	// We must offset the text on the z-axis such that
	// it doesn't obscure the ground items below it.
	private static final int OFFSET_Z = 20;
	// The 15 pixel gap between each drawn ground item.
	private static final int STRING_GAP = 15;
	// Size of the hidden/highlight boxes
	private static final int RECTANGLE_SIZE = 8;
	// Gap between the item icon and the item name
	private static final int ICON_GAP = 3;
	// icon box: padding around the icon and corner rounding
	private static final int BOX_PAD = 3;
	private static final int BOX_ARC = 6;
	// one light sweep across a shimmering box every this many ms
	private static final long SHIMMER_PERIOD_MS = 2400;
	// gap between stacked rows on a tile
	private static final int ROW_GAP = 2;
	// per frame: each drawn item's baseline offset from its tile's text point, and the top of each tile's stack
	private final Map<GroundItem, Integer> rowDy = new IdentityHashMap<>();
	private final Map<WorldPoint, Integer> tileTopDy = new HashMap<>();
	// Points used to approximate the ground circle
	private static final int CIRCLE_SEGMENTS = 24;
	private static final Color PUBLIC_TIMER_COLOR = Color.YELLOW;
	private static final Color PRIVATE_TIMER_COLOR = Color.GREEN;
	private static final int TIMER_OVERLAY_DIAMETER = 10;

	private final Client client;
	private final GroundItemsModifiablePlugin plugin;
	private final GroundItemsModifiableConfig config;
	private final ItemManager itemManager;
	private final StringBuilder itemStringBuilder = new StringBuilder();
	private final BackgroundComponent backgroundComponent = new BackgroundComponent();
	private final TextComponent textComponent = new TextComponent();
	private final ProgressPieComponent progressPieComponent = new ProgressPieComponent();
	private final Map<WorldPoint, Integer> offsetMap = new HashMap<>();
	private final List<GroundItem> visibleItems = new ArrayList<>();
	private final Map<WorldPoint, GroundItem> tileItems = new LinkedHashMap<>();
	private final Map<WorldPoint, long[]> tileTotals = new LinkedHashMap<>();
	// compact mode: tiles whose items the game currently offers under the cursor
	private final Set<WorldPoint> hoveredTiles = new HashSet<>();
	// whether Tile.getGroundItems() lists the right-click menu's top item first; learned from the live menu
	private Boolean pileListTopFirst;

	@Inject
	private GroundItemsModifiableOverlay(Client client, GroundItemsModifiablePlugin plugin, GroundItemsModifiableConfig config, ItemManager itemManager)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final boolean dontShowOverlay = (config.itemHighlightMode() == MENU || config.itemHighlightMode() == NONE
			|| plugin.isHideAll()) && !plugin.isHotKeyPressed();

		if (dontShowOverlay && !config.highlightTiles())
		{
			return null;
		}

		final FontMetrics fm = graphics.getFontMetrics();
		final Player player = client.getLocalPlayer();

		if (player == null)
		{
			return null;
		}

		offsetMap.clear();
		final Point mousePos = client.getMouseCanvasPosition();
		Collection<GroundItem> groundItemList = plugin.getCollectedGroundItems().values();
		GroundItem topGroundItem = null;

		if (plugin.isHotKeyPressed())
		{
			// Make copy of ground items because we are going to modify them here, and the array list supports our
			// desired behaviour here
			groundItemList = new ArrayList<>(groundItemList);
			final java.awt.Point awtMousePos = new java.awt.Point(mousePos.getX(), mousePos.getY());
			GroundItem groundItem = null;

			for (GroundItem item : groundItemList)
			{
				item.setOffset(offsetMap.compute(item.getItemLayer().getWorldLocation(), (k, v) -> v != null ? v + 1 : 0));

				if (groundItem != null)
				{
					continue;
				}

				if (plugin.getTextBoxBounds() != null
					&& item.equals(plugin.getTextBoxBounds().getValue())
					&& plugin.getTextBoxBounds().getKey().contains(awtMousePos))
				{
					groundItem = item;
					continue;
				}

				if (plugin.getHiddenBoxBounds() != null
					&& item.equals(plugin.getHiddenBoxBounds().getValue())
					&& plugin.getHiddenBoxBounds().getKey().contains(awtMousePos))
				{
					groundItem = item;
					continue;
				}

				if (plugin.getHighlightBoxBounds() != null
					&& item.equals(plugin.getHighlightBoxBounds().getValue())
					&& plugin.getHighlightBoxBounds().getKey().contains(awtMousePos))
				{
					groundItem = item;
				}
			}

			if (groundItem != null)
			{
				groundItemList.remove(groundItem);
				groundItemList.add(groundItem);
				topGroundItem = groundItem;
			}
		}

		plugin.setTextBoxBounds(null);
		plugin.setHiddenBoxBounds(null);
		plugin.setHighlightBoxBounds(null);

		final LocalPoint localLocation = player.getLocalLocation();
		final DespawnTimerMode groundItemTimers = config.groundItemTimers();
		final boolean outline = config.textOutline();
		final OwnershipFilterMode ownershipFilterMode = config.ownershipFilterMode();
		final int accountType = client.getVarbitValue(VarbitID.IRONMAN);
		final boolean showIcons = config.showItemIcons();
		final int iconHeight = config.iconSize();
		final boolean showTileTotal = config.showTileTotal();
		final QuantityFormat quantityFormat = config.quantityFormat();

		// Pass 1: decide which items are drawn at all
		visibleItems.clear();
		tileTotals.clear();
		for (GroundItem item : groundItemList)
		{
			final LocalPoint groundPoint = groundPoint(item);

			if (groundPoint == null
				|| (groundPoint.getWorldView() == WorldView.TOPLEVEL && localLocation.distanceTo(groundPoint) > MAX_DISTANCE)
				|| !plugin.shouldDisplayItem(ownershipFilterMode, item.getOwnership(), accountType))
			{
				continue;
			}

			plugin.updateItemColor(item);

			if (!item.highlighted && !plugin.isHotKeyPressed())
			{
				// Do not display hidden items
				if (item.hidden)
				{
					continue;
				}

				// Do not display non-highlighted items
				if (config.showHighlightedOnly())
				{
					continue;
				}
			}

			visibleItems.add(item);

			if (showTileTotal)
			{
				// [count, value]
				final long[] total = tileTotals.computeIfAbsent(item.getItemLayer().getWorldLocation(), k -> new long[2]);
				total[0]++;
				total[1] += plugin.getValueByMode(item.getGePrice(), item.getHaPrice());
			}
		}

		// Pass 2: tiles go under everything else, one polygon per tile
		if (config.highlightTiles())
		{
			if (config.tilesUnderActors())
			{
				// no overlay layer sits between the ground and actors, so cut every NPC and player's outline
				// out of the drawing area instead: the tile then looks like it is under them
				final Shape oldClip = graphics.getClip();
				final Area clip = new Area(oldClip != null ? oldClip : new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight()));
				final WorldView wv = client.getTopLevelWorldView();
				if (wv != null)
				{
					for (NPC npc : wv.npcs())
					{
						subtractHull(clip, npc);
					}
					for (Player p : wv.players())
					{
						subtractHull(clip, p);
					}
				}
				graphics.setClip(clip);
				renderTiles(graphics);
				graphics.setClip(oldClip);
			}
			else
			{
				renderTiles(graphics);
			}
		}

		if (dontShowOverlay)
		{
			return null;
		}

		if (showIcons)
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		}

		// Compact mode: a tile is expanded while the mouse is over what it drew last frame
		final boolean compact = config.compactMode() && showIcons;
		final CollapsedQuantity collapsedQuantity = config.collapsedQuantity();
		final HighlightTier iconBoxTier = config.iconBoxTier();
		final boolean boxWrapsName = config.iconBoxWrapsName();
		final HighlightTier shimmerTier = config.shimmerTier();
		final HighlightTier expandTier = config.expandTier();
		final boolean menuOrder = config.menuOrder();
		final BarWidth barWidth = config.barWidth();
		hoveredTiles.clear();
		// Ground item entries under the cursor, per tile: item id -> position in the menu (0 = top, the
		// left-click option). Entries are stored bottom first, so walk them backwards.
		final Map<Long, Map<Integer, Integer>> menuTiles = new HashMap<>();
		if (compact || menuOrder)
		{
			// A tile expands exactly when the game offers one of its items under the cursor ("Take", "Examine"...).
			// The game's own hit test is stable, unlike a box around icons whose layout changes when expanding.
			// Ground item entries carry the tile's scene x/y in param0/param1 plus the world view id (-1 is the
			// main world), so match on exactly those instead of converting to world coordinates.
			final MenuEntry[] entries = client.getMenuEntries();
			for (int i = entries.length - 1; i >= 0; i--)
			{
				final MenuEntry entry = entries[i];
				final MenuAction type = entry.getType();
				if (type == MenuAction.GROUND_ITEM_FIRST_OPTION || type == MenuAction.GROUND_ITEM_SECOND_OPTION
					|| type == MenuAction.GROUND_ITEM_THIRD_OPTION || type == MenuAction.GROUND_ITEM_FOURTH_OPTION
					|| type == MenuAction.GROUND_ITEM_FIFTH_OPTION || type == MenuAction.EXAMINE_ITEM_GROUND)
				{
					final Map<Integer, Integer> ranks = menuTiles.computeIfAbsent(
						sceneKey(entry.getWorldViewId(), entry.getParam0(), entry.getParam1()), k -> new HashMap<>());
					ranks.putIfAbsent(entry.getIdentifier(), ranks.size());
				}
			}
		}
		if (compact)
		{
			if (!menuTiles.isEmpty())
			{
				for (GroundItem item : visibleItems)
				{
					final LocalPoint lp = item.getItemLayer().getLocalLocation();
					if (lp != null && menuTiles.containsKey(sceneKey(item.getItemLayer().getWorldView().getId(), lp.getSceneX(), lp.getSceneY())))
					{
						hoveredTiles.add(item.getItemLayer().getWorldLocation());
					}
				}
			}
		}

		// Pass 2b: row layout. Each row is as tall as what it actually draws (icon or name, tier box padding,
		// a visible timer bar) and rows stack upwards from the tile with a small fixed gap, so rows without a
		// box or bar sit tight and nothing overlaps.
		rowDy.clear();
		tileTopDy.clear();
		{
			final Map<WorldPoint, List<GroundItem>> rowsByTile = new HashMap<>();
			final Map<GroundItem, Integer> order = new IdentityHashMap<>();
			final Map<WorldPoint, Integer> counter = new HashMap<>();
			for (GroundItem item : visibleItems)
			{
				final WorldPoint wp = item.getItemLayer().getWorldLocation();
				final int n = counter.merge(wp, 1, Integer::sum) - 1;
				order.put(item, plugin.isHotKeyPressed() ? item.getOffset() : n);
				rowsByTile.computeIfAbsent(wp, k -> new ArrayList<>()).add(item);
			}
			if (menuOrder)
			{
				// rows stack upwards, so the menu's top item gets the highest order and is drawn on top
				for (List<GroundItem> rows : rowsByTile.values())
				{
					final Map<Integer, Integer> ranks = pileRanks(rows.get(0).getItemLayer(), menuTiles);
					if (ranks != null)
					{
						for (GroundItem item : rows)
						{
							final Integer rank = ranks.get(item.getId());
							order.put(item, rank != null ? -rank : Integer.MIN_VALUE + order.get(item));
						}
					}
				}
			}
			final int ascent = fm.getAscent();
			final int descent = fm.getDescent();
			for (Map.Entry<WorldPoint, List<GroundItem>> tile : rowsByTile.entrySet())
			{
				final List<GroundItem> rows = tile.getValue();
				rows.sort(java.util.Comparator.comparingInt(order::get));
				final boolean tileCollapsed = compact && !plugin.isHotKeyPressed() && !hoveredTiles.contains(tile.getKey());
				int dy = 0;
				int prevTop = 0;
				boolean first = true;
				for (GroundItem item : rows)
				{
					final boolean hasBox = showIcons && hasTierBox(item, iconBoxTier);
					final TimerInfo t = groundItemTimers == DespawnTimerMode.BAR ? timerInfo(item) : null;
					final boolean hasBar = t != null && (plugin.isHotKeyPressed() || timerVisible(t));
					final boolean hasText = !tileCollapsed || hasTierBox(item, expandTier) || (item.getQuantity() > 1 && collapsedQuantity != CollapsedQuantity.OFF);
					int top;
					int bottom;
					int iconBottom = 0;
					if (showIcons)
					{
						top = -(ascent + iconHeight) / 2;
						bottom = top + iconHeight;
						iconBottom = bottom;
						if (hasText)
						{
							top = Math.min(top, -ascent);
							bottom = Math.max(bottom, descent);
						}
					}
					else
					{
						top = -ascent;
						bottom = descent;
					}
					if (hasBox)
					{
						top -= BOX_PAD;
						bottom += BOX_PAD;
					}
					if (hasBar)
					{
						// same placement as the bar drawing below
						final int under = hasBox ? bottom + 2 : showIcons ? Math.max(2, iconBottom + 2) : 2;
						bottom = Math.max(bottom, under + config.ringWidth());
					}
					if (!first)
					{
						int step = -prevTop + ROW_GAP + bottom;
						if (!showIcons)
						{
							step = Math.max(step, STRING_GAP);
						}
						dy -= step;
					}
					rowDy.put(item, dy);
					// relative to this row's baseline, for the next row's step
					prevTop = top;
					first = false;
					tileTopDy.put(tile.getKey(), dy + top);
				}
			}
		}

		// Pass 3: icons, names, timers and the hotkey boxes
		for (GroundItem item : visibleItems)
		{
			final LocalPoint groundPoint = groundPoint(item);
			if (groundPoint == null)
			{
				continue;
			}

			final Color color = item.color;

			itemStringBuilder.append(item.getDisplayName());

			if (item.getQuantity() > 1 && quantityFormat != QuantityFormat.HIDDEN)
			{
				itemStringBuilder.append(' ').append(formatStack(item.getQuantity(), quantityFormat));
			}

			if (item.getId() != ItemID.COINS)
			{
				PriceDisplayMode displayMode = config.priceDisplayMode();
				if (displayMode == PriceDisplayMode.BOTH)
				{
					if (item.getGePrice() > 0)
					{
						itemStringBuilder.append(" (GE: ")
							.append(QuantityFormatter.quantityToStackSize(item.getGePrice()))
							.append(" gp)");
					}

					if (item.getHaPrice() > 0)
					{
						itemStringBuilder.append(" (HA: ")
							.append(QuantityFormatter.quantityToStackSize(item.getHaPrice()))
							.append(" gp)");
					}
				}
				else if (displayMode != PriceDisplayMode.OFF)
				{
					final long price = displayMode == PriceDisplayMode.GE
						? item.getGePrice()
						: item.getHaPrice();

					if (price > 0)
					{
						itemStringBuilder
							.append(" (")
							.append(QuantityFormatter.quantityToStackSize(price))
							.append(" gp)");
					}
				}
			}

			final String itemString = itemStringBuilder.toString();
			itemStringBuilder.setLength(0);

			final Point textPoint = Perspective.getCanvasTextLocation(client,
				graphics,
				groundPoint,
				itemString,
				item.getItemLayer().getHeight() + OFFSET_Z);

			if (textPoint == null)
			{
				continue;
			}

			final int offset = plugin.isHotKeyPressed()
				? item.getOffset()
				: offsetMap.compute(item.getItemLayer().getWorldLocation(), (k, v) -> v != null ? v + 1 : 0);

			BufferedImage icon = null;
			int iconWidth = 0;
			int iconSpan = 0;
			if (showIcons)
			{
				icon = itemManager.getImage(item.getId(), item.getQuantity(), false);
				iconWidth = Math.max(1, iconHeight * icon.getWidth() / Math.max(1, icon.getHeight()));
				iconSpan = iconWidth + ICON_GAP;
			}

			final WorldPoint worldPoint = item.getItemLayer().getWorldLocation();
			final boolean collapsed = compact && icon != null && !plugin.isHotKeyPressed() && !hoveredTiles.contains(worldPoint)
				&& !hasTierBox(item, expandTier);
			final int textWidth = fm.stringWidth(itemString);
			// collapsed stacks keep a short "x 3" label after the icon
			// collapsed icons: just "1,000" by default, or the names' format; expanded names keep "x 1,000"
			final String stackLabel;
			if (item.getQuantity() <= 1 || collapsedQuantity == CollapsedQuantity.OFF)
			{
				stackLabel = null;
			}
			else if (collapsedQuantity == CollapsedQuantity.NUMBER)
			{
				stackLabel = QuantityFormatter.quantityToStackSize(item.getQuantity());
			}
			else
			{
				stackLabel = quantityFormat != QuantityFormat.HIDDEN ? formatStack(item.getQuantity(), quantityFormat) : null;
			}
			final String collapsedLabel = collapsed ? stackLabel : null;
			final int stackLabelSpan = stackLabel != null ? ICON_GAP + fm.stringWidth(stackLabel) : 0;

			// getCanvasTextLocation centres the text on the tile; shift it so icon + text is centred instead
			final int textX = textPoint.getX() + iconSpan / 2;
			final int textY = textPoint.getY() + rowDy.getOrDefault(item, 0);
			// collapsed: icon (plus its stack label) centred on the tile; otherwise icon sits left of the text
			// collapsed: every icon on the tile shares one centred column, stack numbers hang off to the right
			final int iconX = collapsed ? textPoint.getX() + textWidth / 2 - iconWidth / 2 : textX - iconSpan;
			final int iconY = textY - (fm.getAscent() + iconHeight) / 2;

			if (plugin.isHotKeyPressed())
			{
				final int stringWidth = fm.stringWidth(itemString);
				final int stringHeight = fm.getHeight();

				// Item bounds
				int x = textX - 2 - iconSpan;
				int y = textY - stringHeight - 2;
				int width = stringWidth + 4 + iconSpan;
				int height = stringHeight + 4;
				final Rectangle itemBounds = new Rectangle(x, y, width, height);

				// Hidden box
				x += width + 2;
				y = textY - (RECTANGLE_SIZE + stringHeight) / 2;
				width = height = RECTANGLE_SIZE;
				final Rectangle itemHiddenBox = new Rectangle(x, y, width, height);

				// Highlight box
				x += width + 2;
				final Rectangle itemHighlightBox = new Rectangle(x, y, width, height);

				boolean mouseInBox = itemBounds.contains(mousePos.getX(), mousePos.getY());
				boolean mouseInHiddenBox = itemHiddenBox.contains(mousePos.getX(), mousePos.getY());
				boolean mouseInHighlightBox = itemHighlightBox.contains(mousePos.getX(), mousePos.getY());

				if (mouseInBox)
				{
					plugin.setTextBoxBounds(new SimpleEntry<>(itemBounds, item));
				}
				else if (mouseInHiddenBox)
				{
					plugin.setHiddenBoxBounds(new SimpleEntry<>(itemHiddenBox, item));

				}
				else if (mouseInHighlightBox)
				{
					plugin.setHighlightBoxBounds(new SimpleEntry<>(itemHighlightBox, item));
				}

				boolean topItem = topGroundItem == item;

				// Draw background if hovering
				if (topItem && (mouseInBox || mouseInHiddenBox || mouseInHighlightBox))
				{
					backgroundComponent.setRectangle(itemBounds);
					backgroundComponent.render(graphics);
				}

				// Draw hidden box
				drawRectangle(graphics, itemHiddenBox, topItem && mouseInHiddenBox ? Color.RED : color, item.hidden, true);

				// Draw highlight box
				drawRectangle(graphics, itemHighlightBox, topItem && mouseInHighlightBox ? Color.GREEN : color, item.highlighted, false);
			}

			// value-tier box around the icon
			RoundRectangle2D box = null;
			Color boxColor = null;
			boolean shimmer = false;
			if (icon != null && iconBoxTier != HighlightTier.OFF)
			{
				final long value = plugin.getValueByMode(item.getGePrice(), item.getHaPrice());
				if (value > iconBoxTier.getValueFromTier(config))
				{
					// a colour picked for this item wins; otherwise the tier's. The name below uses the same colour,
					// so a highlighted-list colour cannot leave white text inside a green box
					final Color picked = plugin.getItemColor(item.getId());
					boxColor = picked != null ? picked : plugin.getPriceTierColor(value);
					if (!collapsed && boxWrapsName)
					{
						// one rectangle around icon and name
						final int top = Math.min(iconY, textY - fm.getAscent());
						final int bottom = Math.max(iconY + iconHeight, textY + fm.getDescent());
						box = new RoundRectangle2D.Float(iconX - BOX_PAD, top - BOX_PAD,
							iconSpan + textWidth + 2 * BOX_PAD, bottom - top + 2 * BOX_PAD, BOX_ARC, BOX_ARC);
					}
					else
					{
						box = new RoundRectangle2D.Float(iconX - BOX_PAD, iconY - BOX_PAD,
							iconWidth + 2 * BOX_PAD, iconHeight + 2 * BOX_PAD, BOX_ARC, BOX_ARC);
					}
					shimmer = shimmerTier != HighlightTier.OFF && value >= shimmerTier.getValueFromTier(config);
					graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					// shimmering boxes breathe between a faint and a stronger fill
					final int fillAlpha = shimmer ? (int) (40 + 45 * shimmerPulse()) : 50;
					graphics.setColor(ColorUtil.colorWithAlpha(boxColor, fillAlpha));
					graphics.fill(box);
				}
			}

			if (icon != null)
			{
				graphics.drawImage(icon, iconX, iconY, iconWidth, iconHeight, null);
			}
			if (shimmer)
			{
				drawShimmer(graphics, box);
			}
			// boxed items: name and stack size take the box's colour
			final Color textColor = boxColor != null ? boxColor : color;

			final TimerInfo timer = timerInfo(item);
			final boolean timerShown = timer != null && (plugin.isHotKeyPressed() || timerVisible(timer));
			if (box != null && !(timerShown && groundItemTimers == DespawnTimerMode.RING))
			{
				graphics.setStroke(new BasicStroke(1.5f));
				graphics.setColor(boxColor);
				graphics.draw(box);
			}
			// whatever sits under the icon starts below the box
			final int underIconY = box != null ? (int) Math.ceil(box.getMaxY()) + 2
				: icon != null ? Math.max(textY + 2, iconY + iconHeight + 2) : textY + 2;
			if (timerShown)
			{
				if (groundItemTimers == DespawnTimerMode.RING && box != null)
				{
					// the box border is the timer
					drawTimerBox(graphics, box, timer);
				}
				else if (groundItemTimers == DespawnTimerMode.RING)
				{
					if (icon != null)
					{
						drawTimerRing(graphics, iconX + iconWidth / 2, iconY + iconHeight / 2, Math.max(iconWidth, iconHeight) + 4, timer);
					}
					else
					{
						drawTimerRing(graphics, textX - TIMER_OVERLAY_DIAMETER / 2 - 2, textY - fm.getAscent() / 2, TIMER_OVERLAY_DIAMETER + 2, timer);
					}
				}
				else if (groundItemTimers == DespawnTimerMode.BAR)
				{
					if (box != null && (collapsed || barWidth == BarWidth.ICON))
					{
						drawTimerBar(graphics, (int) box.getX(), underIconY, (int) box.getWidth(), timer);
					}
					else if (icon != null && (collapsed || barWidth == BarWidth.ICON))
					{
						drawTimerBar(graphics, iconX, underIconY, iconWidth, timer);
					}
					else
					{
						drawTimerBar(graphics, textX - iconSpan, underIconY, iconSpan + textWidth, timer);
					}
				}
				// When the hotkey is pressed the hidden/highlight boxes are drawn to the right of the text,
				// so draw the pie since it is on the left hand side.
				else if (groundItemTimers == DespawnTimerMode.PIE || plugin.isHotKeyPressed())
				{
					drawTimerPieOverlay(graphics, (collapsed ? iconX : textX - iconSpan) - (box != null ? BOX_PAD + 1 : 0), textY, timer);
				}
				else if (!collapsed && (groundItemTimers == DespawnTimerMode.SECONDS || groundItemTimers == DespawnTimerMode.TICKS))
				{
					final String timerText;
					if (groundItemTimers == DespawnTimerMode.SECONDS)
					{
						timerText = String.format(" - %.1f", timer.remainingMillis / 1000f);
					}
					else // TICKS
					{
						timerText = String.format(" - %d", timer.remainingMillis / 600);
					}

					// The timer text is drawn separately to have its own color, and is intentionally not included
					// in the getCanvasTextLocation() call because the timer text can change per frame and we do not
					// use a monospaced font, which causes the text location on screen to jump around slightly each frame.
					textComponent.setText(timerText);
					textComponent.setColor(timer.color);
					textComponent.setOutline(outline);
					textComponent.setPosition(new java.awt.Point(textX + fm.stringWidth(itemString), textY));
					textComponent.render(graphics);
				}
			}


			if (collapsed)
			{
				if (collapsedLabel != null)
				{
					textComponent.setText(collapsedLabel);
					textComponent.setColor(textColor);
					textComponent.setOutline(outline);
					textComponent.setPosition(new java.awt.Point(iconX + iconWidth + ICON_GAP + (box != null ? BOX_PAD + 1 : 0), textY));
					textComponent.render(graphics);
				}
				continue;
			}

			textComponent.setText(itemString);
			textComponent.setColor(textColor);
			textComponent.setOutline(outline);
			textComponent.setPosition(new java.awt.Point(textX, textY));
			textComponent.render(graphics);
		}

		if (showTileTotal)
		{
			renderTileTotals(graphics, outline, compact && !plugin.isHotKeyPressed());
		}

		return null;
	}

	/**
	 * One extra line above the stack of a tile holding two or more items: the summed value, coloured by tier.
	 */
	private void renderTileTotals(Graphics2D graphics, boolean outline, boolean hoveredOnly)
	{
		final long minimum = config.tileTotalMinimum();

		for (Map.Entry<WorldPoint, long[]> entry : tileTotals.entrySet())
		{
			if (hoveredOnly && !hoveredTiles.contains(entry.getKey()))
			{
				continue;
			}

			final long count = entry.getValue()[0];
			final long value = entry.getValue()[1];
			if (count < 2 || value <= 0 || value < minimum)
			{
				continue;
			}

			final Integer lastOffset = offsetMap.get(entry.getKey());
			if (lastOffset == null)
			{
				continue;
			}

			// any item on the tile gives us the layer; the tile item map may be empty when tiles are off
			GroundItem sample = null;
			for (GroundItem item : visibleItems)
			{
				if (item.getItemLayer().getWorldLocation().equals(entry.getKey()))
				{
					sample = item;
					break;
				}
			}
			if (sample == null)
			{
				continue;
			}

			final LocalPoint groundPoint = groundPoint(sample);
			if (groundPoint == null)
			{
				continue;
			}

			final String text = "Total: " + QuantityFormatter.quantityToStackSize(value) + " gp";
			final Point textPoint = Perspective.getCanvasTextLocation(client, graphics, groundPoint, text,
				sample.getItemLayer().getHeight() + OFFSET_Z);
			if (textPoint == null)
			{
				continue;
			}

			textComponent.setText(text);
			textComponent.setColor(plugin.getPriceTierColor(value));
			textComponent.setOutline(outline);
			textComponent.setPosition(new java.awt.Point(textPoint.getX(), textPoint.getY() + tileTopDy.getOrDefault(entry.getKey(), 0) - ROW_GAP - graphics.getFontMetrics().getDescent()));
			textComponent.render(graphics);
		}
	}

	/**
	 * A ring that drains clockwise as the item approaches despawn, pulsing once under the configured threshold.
	 */
	private void drawTimerRing(Graphics2D graphics, int centreX, int centreY, int diameter, GroundItem groundItem)
	{
		final Instant now = Instant.now();
		final Instant spawnTime = groundItem.getSpawnTime();
		final Instant despawnTime = calculateDespawnTime(groundItem);
		Color ringColor = getItemTimerColor(groundItem);

		if (spawnTime == null || despawnTime == null || ringColor == null)
		{
			return;
		}

		final long remainingMillis = despawnTime.toEpochMilli() - now.toEpochMilli();
		final float remaining = (float) remainingMillis / (despawnTime.toEpochMilli() - spawnTime.toEpochMilli());
		float width = config.ringWidth();

		final int pulseSeconds = config.ringPulseSeconds();
		if (pulseSeconds > 0 && remainingMillis <= pulseSeconds * 1000L)
		{
			// 0..1 at roughly 1.5 Hz, faster in the last few seconds
			final double period = remainingMillis < 5000 ? 350.0 : 700.0;
			final double pulse = (Math.sin(now.toEpochMilli() / period * Math.PI) + 1) / 2;
			final Color base = config.ringPulseColor();
			ringColor = ColorUtil.colorWithAlpha(base, (int) (base.getAlpha() * (0.35 + 0.65 * pulse)));
			width += (float) pulse * 2;
		}

		final Stroke originalStroke = graphics.getStroke();
		final Object originalAa = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

		final double x = centreX - diameter / 2.0;
		final double y = centreY - diameter / 2.0;

		// faint full track so the drained part stays readable
		graphics.setColor(ColorUtil.colorWithAlpha(Color.BLACK, 90));
		graphics.draw(new Ellipse2D.Double(x, y, diameter, diameter));

		graphics.setColor(ringColor);
		graphics.draw(new Arc2D.Double(x, y, diameter, diameter, 90, -360 * remaining, Arc2D.OPEN));

		graphics.setStroke(originalStroke);
		if (originalAa != null)
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAa);
		}
	}

	private static LocalPoint groundPoint(GroundItem item)
	{
		final WorldView wv = item.getItemLayer().getWorldView();
		return LocalPoint.fromWorld(wv, item.getItemLayer().getWorldLocation());
	}

	/**
	 * Draw one polygon per tile. When several items share a tile the highlighted one decides the colour.
	 */
	private void renderTiles(Graphics2D graphics)
	{
		tileItems.clear();
		final boolean highlightedOnly = config.tileHighlightedOnly();

		final HighlightTier minTier = config.tileMinTier();
		final long minValue = minTier == HighlightTier.OFF ? Long.MAX_VALUE : minTier.getValueFromTier(config);
		for (GroundItem item : visibleItems)
		{
			if (highlightedOnly && !item.highlighted
				&& plugin.getValueByMode(item.getGePrice(), item.getHaPrice()) <= minValue)
			{
				continue;
			}

			final WorldPoint worldPoint = item.getItemLayer().getWorldLocation();
			final GroundItem current = tileItems.get(worldPoint);
			if (current == null || (item.highlighted && !current.highlighted))
			{
				tileItems.put(worldPoint, item);
			}
		}

		if (tileItems.isEmpty())
		{
			return;
		}

		final TileShape shape = config.tileShape();
		final double circleScale = config.tileCircleSize() / 100.0;
		final TileColorMode outlineMode = config.tileOutlineMode();
		final TileFillMode fillMode = config.tileFillMode();
		final int outlineWidth = config.tileOutlineWidth();
		final int fillOpacity = config.tileFillOpacity();
		final Color customOutline = config.tileOutlineColor();
		final Color customFill = config.tileFillColor();
		final Stroke originalStroke = graphics.getStroke();
		final Stroke stroke = new BasicStroke(outlineWidth);
		final Object originalAa = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		if (shape == TileShape.CIRCLE)
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}

		for (GroundItem item : tileItems.values())
		{
			final LocalPoint groundPoint = groundPoint(item);
			if (groundPoint == null)
			{
				continue;
			}

			final int height = item.getItemLayer().getHeight();
			final Polygon poly = Perspective.getCanvasTilePoly(client, groundPoint, height);
			if (poly == null)
			{
				continue;
			}

			final Shape mark = shape == TileShape.CIRCLE ? tileCircle(groundPoint, item.getItemLayer().getPlane(), height, circleScale) : poly;
			if (mark == null)
			{
				continue;
			}

			Color fill = null;
			switch (fillMode)
			{
				case ITEM_COLOR:
					fill = ColorUtil.colorWithAlpha(item.color, fillOpacity);
					break;
				case CUSTOM:
					fill = customFill;
					break;
				default:
					break;
			}

			if (fill != null && fill.getAlpha() > 0)
			{
				graphics.setColor(fill);
				graphics.fill(mark);
			}

			if (outlineWidth > 0)
			{
				graphics.setColor(outlineMode == TileColorMode.ITEM_COLOR ? item.color : customOutline);
				graphics.setStroke(stroke);
				graphics.draw(mark);
			}
		}

		graphics.setStroke(originalStroke);
		if (originalAa != null)
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAa);
		}
	}

	/**
	 * A circle lying on the ground: points around the tile centre in local coordinates, each projected through the
	 * game camera. Rotating or tilting the camera therefore distorts it exactly like the tile itself, and the
	 * per-point height lookup makes it follow slopes.
	 */
	private Shape tileCircle(LocalPoint centre, int plane, int zOffset, double scale)
	{
		final double radius = Perspective.LOCAL_HALF_TILE_SIZE * scale;
		final Polygon circle = new Polygon();

		for (int i = 0; i < CIRCLE_SEGMENTS; i++)
		{
			final double angle = 2 * Math.PI * i / CIRCLE_SEGMENTS;
			final LocalPoint edge = new LocalPoint(
				centre.getX() + (int) Math.round(Math.cos(angle) * radius),
				centre.getY() + (int) Math.round(Math.sin(angle) * radius),
				centre.getWorldView());
			final Point p = Perspective.localToCanvas(client, edge, plane, zOffset);
			if (p == null)
			{
				return null;
			}

			circle.addPoint(p.getX(), p.getY());
		}

		return circle;
	}

	private Instant calculateDespawnTime(GroundItem groundItem)
	{
		Instant spawnTime = groundItem.getSpawnTime();
		if (spawnTime == null)
		{
			return null;
		}

		Instant despawnTime = spawnTime.plus(groundItem.getDespawnTime());
		if (Instant.now().isAfter(despawnTime))
		{
			// that's weird
			return null;
		}

		return despawnTime;
	}

	private Color getItemTimerColor(GroundItem groundItem)
	{
		final Instant spawnTime = groundItem.getSpawnTime();
		if (spawnTime == null)
		{
			return null;
		}

		final Instant now = Instant.now();
		final Instant despawnTime = spawnTime.plus(groundItem.getDespawnTime());

		if (groundItem.isPrivate())
		{
			if (despawnTime.isAfter(now))
			{
				return PRIVATE_TIMER_COLOR;
			}

			return null;
		}

		// otherwise it is private until visibleTime, then it is public
		final Instant visibleTime = spawnTime.plus(groundItem.getVisibleTime());
		if (visibleTime.isAfter(now))
		{
			return PRIVATE_TIMER_COLOR;
		}
		if (despawnTime.isAfter(now))
		{
			return PUBLIC_TIMER_COLOR;
		}

		return null;
	}

	/**
	 * Everything the timer renderers need about one item, computed once per frame.
	 */
	private static final class TimerInfo
	{
		long remainingMillis;
		float remainingFraction;
		Color color;
		boolean pulsing;
		boolean isPublic;
		// where on the draining bar (0..1 of the full width) the item turns public; 0 when that never happens or already did
		float publicFraction;
	}

	private TimerInfo timerInfo(GroundItem groundItem)
	{
		final Instant now = Instant.now();
		final Instant spawnTime = groundItem.getSpawnTime();
		final Instant despawnTime = calculateDespawnTime(groundItem);
		final Color timerColor = getItemTimerColor(groundItem);

		if (spawnTime == null || despawnTime == null || timerColor == null)
		{
			return null;
		}

		final TimerInfo info = new TimerInfo();
		info.remainingMillis = despawnTime.toEpochMilli() - now.toEpochMilli();
		info.remainingFraction = (float) info.remainingMillis / (despawnTime.toEpochMilli() - spawnTime.toEpochMilli());
		info.color = timerColor;
		info.isPublic = timerColor == PUBLIC_TIMER_COLOR;
		if (!info.isPublic && !groundItem.isPrivate())
		{
			final long total = despawnTime.toEpochMilli() - spawnTime.toEpochMilli();
			final long publicFor = total - groundItem.getVisibleTime().toMillis();
			if (publicFor > 0 && publicFor < total)
			{
				info.publicFraction = (float) publicFor / total;
			}
		}
		final int pulseSeconds = config.ringPulseSeconds();
		info.pulsing = pulseSeconds > 0 && info.remainingMillis <= pulseSeconds * 1000L;
		return info;
	}

	private boolean timerVisible(TimerInfo timer)
	{
		switch (config.timerVisibility())
		{
			case PUBLIC_OR_PULSING:
				return timer.isPublic || timer.pulsing;
			case UNDER_TICKS:
				return timer.remainingMillis / 600 < config.timerShowTicks();
			default:
				return true;
		}
	}

	/**
	 * 0..1 throb shared by the ring and bar, faster in the last five seconds.
	 */
	private static double pulsePhase(TimerInfo timer)
	{
		final double period = timer.remainingMillis < 5000 ? 350.0 : 700.0;
		return (Math.sin(Instant.now().toEpochMilli() / period * Math.PI) + 1) / 2;
	}

	private Color pulsedColor(TimerInfo timer, double pulse)
	{
		final Color base = config.ringPulseColor();
		return ColorUtil.colorWithAlpha(base, (int) (base.getAlpha() * (0.35 + 0.65 * pulse)));
	}

	private void drawTimerPieOverlay(Graphics2D graphics, int textX, int textY, TimerInfo timer)
	{
		progressPieComponent.setDiameter(TIMER_OVERLAY_DIAMETER);
		// Shift over to not be on top of the text
		int x = textX - TIMER_OVERLAY_DIAMETER;
		int y = textY - TIMER_OVERLAY_DIAMETER / 2;
		progressPieComponent.setPosition(new Point(x, y));
		progressPieComponent.setFill(timer.color);
		progressPieComponent.setBorderColor(timer.color);
		progressPieComponent.setProgress(timer.remainingFraction); // drains over time
		progressPieComponent.render(graphics);
	}

	/**
	 * A ring that drains clockwise as the item approaches despawn, pulsing once under the configured threshold.
	 */
	private void drawTimerRing(Graphics2D graphics, int centreX, int centreY, int diameter, TimerInfo timer)
	{
		Color ringColor = timer.color;
		float width = config.ringWidth();

		if (timer.pulsing)
		{
			final double pulse = pulsePhase(timer);
			ringColor = pulsedColor(timer, pulse);
			width += (float) pulse * 2;
		}

		final Stroke originalStroke = graphics.getStroke();
		final Object originalAa = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

		final double x = centreX - diameter / 2.0;
		final double y = centreY - diameter / 2.0;

		// faint full track so the drained part stays readable
		graphics.setColor(ColorUtil.colorWithAlpha(Color.BLACK, 90));
		graphics.draw(new Ellipse2D.Double(x, y, diameter, diameter));

		graphics.setColor(ringColor);
		graphics.draw(new Arc2D.Double(x, y, diameter, diameter, 90, -360 * timer.remainingFraction, Arc2D.OPEN));

		graphics.setStroke(originalStroke);
		if (originalAa != null)
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAa);
		}
	}

	/**
	 * A thin bar under the icon and name that shrinks from the right as the item approaches despawn.
	 */
	private void drawTimerBar(Graphics2D graphics, int x, int y, int width, TimerInfo timer)
	{
		final int height = config.ringWidth();
		Color barColor = timer.color;
		if (timer.pulsing)
		{
			barColor = pulsedColor(timer, pulsePhase(timer));
		}

		graphics.setColor(ColorUtil.colorWithAlpha(Color.BLACK, 120));
		graphics.fillRect(x - 1, y - 1, width + 2, height + 2);

		graphics.setColor(barColor);
		graphics.fillRect(x, y, Math.max(1, Math.round(width * timer.remainingFraction)), height);

		// tick where the drop goes public: the bar has drained to here when other players can see it
		if (timer.publicFraction > 0 && config.barPublicMarker())
		{
			final int markX = x + Math.round(width * timer.publicFraction);
			graphics.setColor(Color.BLACK);
			graphics.fillRect(markX - 2, y - 2, 4, height + 4);
			graphics.setColor(PUBLIC_TIMER_COLOR);
			graphics.fillRect(markX - 1, y - 1, 2, height + 2);
		}
	}

	private void drawRectangle(Graphics2D graphics, Rectangle rect, Color color, boolean inList, boolean hiddenBox)
	{
		graphics.setColor(Color.BLACK);
		graphics.drawRect(rect.x + 1, rect.y + 1, rect.width, rect.height);

		graphics.setColor(color);
		graphics.draw(rect);

		if (inList)
		{
			graphics.fill(rect);
		}

		graphics.setColor(Color.WHITE);
		// Minus symbol
		graphics.drawLine
			(
				rect.x + 2,
				rect.y + (rect.height / 2),
				rect.x + rect.width - 2,
				rect.y + (rect.height / 2)
			);

		if (!hiddenBox)
		{
			// Plus symbol
			graphics.drawLine
				(
					rect.x + (rect.width / 2),
					rect.y + 2,
					rect.x + (rect.width / 2),
					rect.y + rect.height - 2
				);
		}

	}
	private static void subtractHull(Area clip, Actor actor)
	{
		final Shape hull = actor.getConvexHull();
		if (hull != null)
		{
			clip.subtract(new Area(hull));
		}
	}

	/**
	 * Box border used as the despawn timer: a dark track, then the remaining time drawn clockwise from top centre.
	 */
	private void drawTimerBox(Graphics2D graphics, RoundRectangle2D box, TimerInfo timer)
	{
		final Path2D path = boxPerimeter(box);
		final float length = pathLength(path);
		final float width = config.ringWidth();
		Color color = timer.color;
		if (timer.pulsing)
		{
			color = pulsedColor(timer, pulsePhase(timer));
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setStroke(new BasicStroke(width + 2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
		graphics.setColor(ColorUtil.colorWithAlpha(Color.BLACK, 120));
		graphics.draw(path);

		final float drawn = Math.max(0.5f, length * Math.max(0f, Math.min(1f, timer.remainingFraction)));
		graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f,
			new float[]{drawn, length + 1f}, 0f));
		graphics.setColor(color);
		graphics.draw(path);

		// where the drop goes public, like on the bar
		if (timer.publicFraction > 0 && config.barPublicMarker())
		{
			final float[] at = pointAlong(path, length * timer.publicFraction);
			graphics.setStroke(new BasicStroke(1f));
			graphics.setColor(Color.BLACK);
			graphics.fill(new java.awt.geom.Ellipse2D.Float(at[0] - 2.5f, at[1] - 2.5f, 5f, 5f));
			graphics.setColor(PUBLIC_TIMER_COLOR);
			graphics.fill(new java.awt.geom.Ellipse2D.Float(at[0] - 1.5f, at[1] - 1.5f, 3f, 3f));
		}
	}

	/** 0..1, slow breathing shared by every shimmering box. */
	private static double shimmerPulse()
	{
		return (Math.sin(System.currentTimeMillis() / 600.0) + 1) / 2;
	}

	/** A soft diagonal band of light that sweeps left to right across the box, then rests. */
	private static void drawShimmer(Graphics2D graphics, RoundRectangle2D box)
	{
		final double t = (System.currentTimeMillis() % SHIMMER_PERIOD_MS) / (double) SHIMMER_PERIOD_MS;
		// the sweep takes the first 45% of the period; the rest is a pause
		final double sweep = t / 0.45;
		if (sweep > 1)
		{
			return;
		}
		final float band = (float) Math.max(10, box.getHeight());
		final float travel = (float) box.getWidth() + band * 2;
		final float cx = (float) (box.getX() - band + travel * sweep);
		final float cy = (float) box.getCenterY();
		// one band, slanted: clear -> bright -> clear across its width, tilted by the height
		final java.awt.LinearGradientPaint paint = new java.awt.LinearGradientPaint(
			new java.awt.geom.Point2D.Float(cx - band / 2, cy + band / 4),
			new java.awt.geom.Point2D.Float(cx + band / 2, cy - band / 4),
			new float[]{0f, 0.5f, 1f},
			new Color[]{new Color(255, 255, 255, 0), new Color(255, 255, 255, 110), new Color(255, 255, 255, 0)});
		final Shape oldClip = graphics.getClip();
		final java.awt.Paint oldPaint = graphics.getPaint();
		graphics.clip(box);
		graphics.setPaint(paint);
		graphics.fill(new Rectangle2D.Float(cx - band, (float) box.getY(), band * 2, (float) box.getHeight()));
		graphics.setPaint(oldPaint);
		graphics.setClip(oldClip);
	}

	/** Rounded-box outline starting at top centre and running clockwise. */
	private static Path2D boxPerimeter(RoundRectangle2D box)
	{
		final float x = (float) box.getX();
		final float y = (float) box.getY();
		final float w = (float) box.getWidth();
		final float h = (float) box.getHeight();
		final float r = (float) box.getArcWidth() / 2f;
		final Path2D.Float p = new Path2D.Float();
		p.moveTo(x + w / 2, y);
		p.lineTo(x + w - r, y);
		p.quadTo(x + w, y, x + w, y + r);
		p.lineTo(x + w, y + h - r);
		p.quadTo(x + w, y + h, x + w - r, y + h);
		p.lineTo(x + r, y + h);
		p.quadTo(x, y + h, x, y + h - r);
		p.lineTo(x, y + r);
		p.quadTo(x, y, x + r, y);
		p.lineTo(x + w / 2, y);
		return p;
	}

	private static float pathLength(Shape shape)
	{
		float length = 0;
		final float[] c = new float[6];
		float lx = 0;
		float ly = 0;
		for (PathIterator it = shape.getPathIterator(null, 0.5); !it.isDone(); it.next())
		{
			final int seg = it.currentSegment(c);
			if (seg == PathIterator.SEG_LINETO)
			{
				length += (float) Math.hypot(c[0] - lx, c[1] - ly);
			}
			lx = c[0];
			ly = c[1];
		}
		return length;
	}

	private static float[] pointAlong(Shape shape, float distance)
	{
		final float[] c = new float[6];
		float lx = 0;
		float ly = 0;
		float travelled = 0;
		for (PathIterator it = shape.getPathIterator(null, 0.5); !it.isDone(); it.next())
		{
			final int seg = it.currentSegment(c);
			if (seg == PathIterator.SEG_LINETO)
			{
				final float d = (float) Math.hypot(c[0] - lx, c[1] - ly);
				if (travelled + d >= distance && d > 0)
				{
					final float t = (distance - travelled) / d;
					return new float[]{lx + (c[0] - lx) * t, ly + (c[1] - ly) * t};
				}
				travelled += d;
			}
			lx = c[0];
			ly = c[1];
		}
		return new float[]{lx, ly};
	}

	/**
	 * Item id -> position in the right-click menu (0 = top) for the pile on this item layer's tile. Taken from
	 * the live menu when the cursor is over the tile, otherwise from the tile's item list. That list is compared
	 * against the live menu whenever both are known, so its direction is learned rather than assumed.
	 */
	private Map<Integer, Integer> pileRanks(ItemLayer layer, Map<Long, Map<Integer, Integer>> menuTiles)
	{
		final LocalPoint lp = layer.getLocalLocation();
		final WorldView wv = layer.getWorldView();
		if (lp == null || wv == null)
		{
			return null;
		}
		final Tile[][][] tiles = wv.getScene().getTiles();
		final int plane = layer.getPlane();
		final int x = lp.getSceneX();
		final int y = lp.getSceneY();
		if (plane < 0 || plane >= tiles.length || x < 0 || x >= tiles[plane].length || y < 0 || y >= tiles[plane][x].length)
		{
			return null;
		}
		final Tile tile = tiles[plane][x][y];
		final List<TileItem> pile = tile != null ? tile.getGroundItems() : null;

		final Map<Integer, Integer> live = menuTiles.get(sceneKey(wv.getId(), x, y));
		if (pile == null || pile.isEmpty())
		{
			return live;
		}

		final List<Integer> ids = new ArrayList<>();
		for (TileItem ti : pile)
		{
			if (!ids.contains(ti.getId()))
			{
				ids.add(ti.getId());
			}
		}
		if (live != null && live.size() > 1)
		{
			// learn which end of the list the menu starts at from two items both lists contain
			int first = -1;
			for (int id : ids)
			{
				final Integer rank = live.get(id);
				if (rank == null)
				{
					continue;
				}
				if (first < 0)
				{
					first = rank;
				}
				else if (rank != first)
				{
					pileListTopFirst = rank > first;
					break;
				}
			}
			return live;
		}
		if (live != null)
		{
			return live;
		}

		// until the menu has been seen over a pile, assume the end holding the item drawn on top is the menu's top
		final boolean topFirst = pileListTopFirst != null ? pileListTopFirst : pile.get(0) == layer.getTop() || pile.get(pile.size() - 1) != layer.getTop();
		final Map<Integer, Integer> ranks = new HashMap<>();
		for (int i = 0; i < ids.size(); i++)
		{
			ranks.put(ids.get(topFirst ? i : ids.size() - 1 - i), i);
		}
		return ranks;
	}

	private boolean hasTierBox(GroundItem item, HighlightTier tier)
	{
		return tier != HighlightTier.OFF && plugin.getValueByMode(item.getGePrice(), item.getHaPrice()) > tier.getValueFromTier(config);
	}

	private static long sceneKey(int worldViewId, int sceneX, int sceneY)
	{
		return ((long) worldViewId << 32) | ((sceneX & 0xFFFFL) << 16) | (sceneY & 0xFFFFL);
	}

	private static String formatStack(int quantity, QuantityFormat format)
	{
		final String stack = QuantityFormatter.quantityToStackSize(quantity);
		return format == QuantityFormat.TIMES ? "x " + stack : "(" + stack + ")";
	}

}
