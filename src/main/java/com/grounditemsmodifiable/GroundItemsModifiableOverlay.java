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

import com.grounditemsmodifiable.config.DespawnTimerMode;
import static com.grounditemsmodifiable.config.ItemHighlightMode.MENU;
import static com.grounditemsmodifiable.config.ItemHighlightMode.NONE;
import com.grounditemsmodifiable.config.OwnershipFilterMode;
import com.grounditemsmodifiable.config.PriceDisplayMode;
import com.grounditemsmodifiable.config.TileColorMode;
import com.grounditemsmodifiable.config.TileFillMode;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
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

		// Pass 1: decide which items are drawn at all
		visibleItems.clear();
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
		}

		// Pass 2: tiles go under everything else, one polygon per tile
		if (config.highlightTiles())
		{
			renderTiles(graphics);
		}

		if (dontShowOverlay)
		{
			return null;
		}

		if (showIcons)
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
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

			if (item.getQuantity() > 1)
			{
				itemStringBuilder.append(" (")
					.append(QuantityFormatter.quantityToStackSize(item.getQuantity()))
					.append(')');
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

			// getCanvasTextLocation centres the text on the tile; shift it so icon + text is centred instead
			final int textX = textPoint.getX() + iconSpan / 2;
			final int textY = textPoint.getY() - (STRING_GAP * offset);

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

			if (icon != null)
			{
				final int iconX = textX - iconSpan;
				final int iconY = textY - (fm.getAscent() + iconHeight) / 2;
				graphics.drawImage(icon, iconX, iconY, iconWidth, iconHeight, null);
			}

			// When the hotkey is pressed the hidden/highlight boxes are drawn to the right of the text,
			// so always draw the pie since it is on the left hand side.
			if (groundItemTimers == DespawnTimerMode.PIE || plugin.isHotKeyPressed())
			{
				drawTimerPieOverlay(graphics, textX - iconSpan, textY, item);
			}
			else if (groundItemTimers == DespawnTimerMode.SECONDS || groundItemTimers == DespawnTimerMode.TICKS)
			{
				Instant despawnTime = calculateDespawnTime(item);
				Color timerColor = getItemTimerColor(item);
				if (despawnTime != null && timerColor != null)
				{
					long despawnTimeMillis = despawnTime.toEpochMilli() - Instant.now().toEpochMilli();
					final String timerText;
					if (groundItemTimers == DespawnTimerMode.SECONDS)
					{
						timerText = String.format(" - %.1f", despawnTimeMillis / 1000f);
					}
					else // TICKS
					{
						timerText = String.format(" - %d", despawnTimeMillis / 600);
					}

					// The timer text is drawn separately to have its own color, and is intentionally not included
					// in the getCanvasTextLocation() call because the timer text can change per frame and we do not
					// use a monospaced font, which causes the text location on screen to jump around slightly each frame.
					textComponent.setText(timerText);
					textComponent.setColor(timerColor);
					textComponent.setOutline(outline);
					textComponent.setPosition(new java.awt.Point(textX + fm.stringWidth(itemString), textY));
					textComponent.render(graphics);
				}
			}

			textComponent.setText(itemString);
			textComponent.setColor(color);
			textComponent.setOutline(outline);
			textComponent.setPosition(new java.awt.Point(textX, textY));
			textComponent.render(graphics);
		}

		return null;
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

		for (GroundItem item : visibleItems)
		{
			if (highlightedOnly && !item.highlighted)
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

		final TileColorMode outlineMode = config.tileOutlineMode();
		final TileFillMode fillMode = config.tileFillMode();
		final int outlineWidth = config.tileOutlineWidth();
		final int fillOpacity = config.tileFillOpacity();
		final Color customOutline = config.tileOutlineColor();
		final Color customFill = config.tileFillColor();
		final Stroke originalStroke = graphics.getStroke();
		final Stroke stroke = new BasicStroke(outlineWidth);

		for (GroundItem item : tileItems.values())
		{
			final LocalPoint groundPoint = groundPoint(item);
			if (groundPoint == null)
			{
				continue;
			}

			final Polygon poly = Perspective.getCanvasTilePoly(client, groundPoint, item.getItemLayer().getHeight());
			if (poly == null)
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
				graphics.fill(poly);
			}

			if (outlineWidth > 0)
			{
				graphics.setColor(outlineMode == TileColorMode.ITEM_COLOR ? item.color : customOutline);
				graphics.setStroke(stroke);
				graphics.draw(poly);
			}
		}

		graphics.setStroke(originalStroke);
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

	private void drawTimerPieOverlay(Graphics2D graphics, int textX, int textY, GroundItem groundItem)
	{
		Instant now = Instant.now();
		Instant spawnTime = groundItem.getSpawnTime();
		Instant despawnTime = calculateDespawnTime(groundItem);
		Color fillColor = getItemTimerColor(groundItem);

		if (spawnTime == null || despawnTime == null || fillColor == null)
		{
			return;
		}

		float percent = (float) (now.toEpochMilli() - spawnTime.toEpochMilli()) / (despawnTime.toEpochMilli() - spawnTime.toEpochMilli());

		progressPieComponent.setDiameter(TIMER_OVERLAY_DIAMETER);
		// Shift over to not be on top of the text
		int x = textX - TIMER_OVERLAY_DIAMETER;
		int y = textY - TIMER_OVERLAY_DIAMETER / 2;
		progressPieComponent.setPosition(new Point(x, y));
		progressPieComponent.setFill(fillColor);
		progressPieComponent.setBorderColor(fillColor);
		progressPieComponent.setProgress(1 - percent); // inverse so pie drains over time
		progressPieComponent.render(graphics);
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
}
