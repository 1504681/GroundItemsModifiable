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
import com.grounditemsmodifiable.config.QuantityFormat;
import com.grounditemsmodifiable.config.TileColorMode;
import com.grounditemsmodifiable.config.TileFillMode;
import com.grounditemsmodifiable.config.TileShape;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
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
				final String stack = QuantityFormatter.quantityToStackSize(item.getQuantity());
				if (quantityFormat == QuantityFormat.TIMES)
				{
					itemStringBuilder.append(" x ").append(stack);
				}
				else
				{
					itemStringBuilder.append(" (").append(stack).append(')');
				}
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
			if (groundItemTimers == DespawnTimerMode.RING)
			{
				if (icon != null)
				{
					final int iconX = textX - iconSpan;
					final int iconY = textY - (fm.getAscent() + iconHeight) / 2;
					drawTimerRing(graphics, iconX + iconWidth / 2, iconY + iconHeight / 2, Math.max(iconWidth, iconHeight) + 4, item);
				}
				else
				{
					drawTimerRing(graphics, textX - TIMER_OVERLAY_DIAMETER / 2 - 2, textY - fm.getAscent() / 2, TIMER_OVERLAY_DIAMETER + 2, item);
				}
			}
			else if (groundItemTimers == DespawnTimerMode.PIE || plugin.isHotKeyPressed())
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

		if (showTileTotal)
		{
			renderTileTotals(graphics, outline);
		}

		return null;
	}

	/**
	 * One extra line above the stack of a tile holding two or more items: the summed value, coloured by tier.
	 */
	private void renderTileTotals(Graphics2D graphics, boolean outline)
	{
		final long minimum = config.tileTotalMinimum();

		for (Map.Entry<WorldPoint, long[]> entry : tileTotals.entrySet())
		{
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
			textComponent.setPosition(new java.awt.Point(textPoint.getX(), textPoint.getY() - STRING_GAP * (lastOffset + 1)));
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
