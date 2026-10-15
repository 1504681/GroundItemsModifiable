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

import com.google.common.base.MoreObjects;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.Value;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemLayer;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import static net.runelite.api.TileItem.OWNERSHIP_GROUP;
import static net.runelite.api.TileItem.OWNERSHIP_OTHER;
import static net.runelite.api.TileItem.OWNERSHIP_SELF;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.FocusChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import com.grounditemsmodifiable.config.HighlightTier;
import com.grounditemsmodifiable.config.ItemHighlightMode;
import com.grounditemsmodifiable.config.MenuHighlightMode;
import static com.grounditemsmodifiable.config.MenuHighlightMode.BOTH;
import static com.grounditemsmodifiable.config.MenuHighlightMode.NAME;
import static com.grounditemsmodifiable.config.MenuHighlightMode.OPTION;
import com.grounditemsmodifiable.config.OwnershipFilterMode;
import com.grounditemsmodifiable.config.QuantityFormat;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.RSTimeUnit;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

@Slf4j
@PluginDescriptor(
	name = "Ground Items Modifiable",
	description = "Ground Items with custom tile fill and outline colours, renameable items and item icons drawn on the ground",
	tags = {"ground", "items", "loot", "highlight", "tile", "fill", "rename", "icon", "icons", "overlay", "lootbeam"}
)
public class GroundItemsModifiablePlugin extends Plugin
{
	static final String VERSION = "1.4.1";
	private static final String HIGHLIGHT_COLOR_PREFIX = "highlight_";
	// pre-1.1 storage of renames, migrated into the renames text box on start
	private static final String LEGACY_NAME_PREFIX = "name_";
	// "(4)", "(3)" ... dose and charge suffixes
	private static final Pattern DOSE_SUFFIX = Pattern.compile("\\s*\\(\\d+\\)$");
	// config group of the built-in Ground Items plugin, imported on first run
	private static final String CORE_GROUP = "grounditems";
	private static final String IMPORTED_KEY = "importedCoreConfig";

	@Value
	static class PriceHighlight
	{
		private final int price;
		private final Color color;
	}

	// ItemID for coins
	private static final int COINS = ItemID.COINS;

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private Map.Entry<Rectangle, GroundItem> textBoxBounds;

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private Map.Entry<Rectangle, GroundItem> hiddenBoxBounds;

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private Map.Entry<Rectangle, GroundItem> highlightBoxBounds;

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private boolean hotKeyPressed;

	@Getter(AccessLevel.PACKAGE)
	@Setter(AccessLevel.PACKAGE)
	private boolean hideAll;

	private ItemList highlightedItems = new ItemList(Collections.emptyList());
	private ItemList hiddenItems = new ItemList(Collections.emptyList());
	// lower-cased real name (dose stripped when grouping) -> custom name
	private volatile Map<String, String> renames = Collections.emptyMap();
	// wildcard patterns, in file order
	private volatile List<Map.Entry<String, String>> wildcardRenames = Collections.emptyList();

	@Inject
	private GroundItemHotkeyListener hotkeyListener;

	@Inject
	private GroundItemMouseAdapter mouseAdapter;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private GroundItemsModifiableConfig config;

	@Inject
	private GroundItemsModifiableOverlay overlay;

	@Inject
	private Notifier notifier;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ColorPickerManager colorPickerManager;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private PluginManager pluginManager;

	// the plugin this one replaces; matched by name so there is no hard link to the core class
	private static final String CORE_PLUGIN_CLASS = "net.runelite.client.plugins.grounditems.GroundItemsPlugin";
	// core plugins that load after this one during client start are turned off instead of turning this one off
	private static final long STARTUP_GRACE_MS = 15_000;
	private long startedAt;

	@Getter
	private final Table<WorldPoint, Integer, GroundItem> collectedGroundItems = HashBasedTable.create();
	private List<PriceHighlight> priceChecks = ImmutableList.of();
	private final Map<WorldPoint, Lootbeam> lootbeams = new HashMap<>();

	@Provides
	GroundItemsModifiableConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GroundItemsModifiableConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		mouseManager.registerMouseListener(mouseAdapter);
		keyManager.registerKeyListener(hotkeyListener);
		importCoreConfig(false);
		clientThread.invoke(this::migrateLegacyRenames);
		executor.execute(this::reset);
		startedAt = System.currentTimeMillis();
		if (config.exclusiveWithCore())
		{
			disableCorePlugin();
		}
	}

	private Plugin corePlugin()
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (CORE_PLUGIN_CLASS.equals(plugin.getClass().getName()))
			{
				return plugin;
			}
		}
		return null;
	}

	/** Turns the built-in Ground Items plugin off so the two do not draw over each other. */
	private void disableCorePlugin()
	{
		final Plugin core = corePlugin();
		if (core == null || !pluginManager.isPluginEnabled(core))
		{
			return;
		}
		// plugins start and stop on the Swing thread
		SwingUtilities.invokeLater(() ->
		{
			try
			{
				pluginManager.setPluginEnabled(core, false);
				pluginManager.stopPlugin(core);
				clientThread.invokeLater(() -> chatMessage("Ground Items Modifiable turned the built-in Ground Items plugin off."));
			}
			catch (PluginInstantiationException e)
			{
				log.warn("Could not stop the built-in Ground Items plugin", e);
			}
		});
	}

	private void disableSelf()
	{
		SwingUtilities.invokeLater(() ->
		{
			try
			{
				pluginManager.setPluginEnabled(this, false);
				pluginManager.stopPlugin(this);
			}
			catch (PluginInstantiationException e)
			{
				log.warn("Could not stop Ground Items Modifiable", e);
			}
		});
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		if (!config.exclusiveWithCore() || !event.isLoaded()
			|| !CORE_PLUGIN_CLASS.equals(event.getPlugin().getClass().getName()))
		{
			return;
		}
		if (System.currentTimeMillis() - startedAt < STARTUP_GRACE_MS)
		{
			// both were enabled when the client started: this plugin wins
			disableCorePlugin();
		}
		else
		{
			// the user switched the built-in plugin back on: step aside
			disableSelf();
		}
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		mouseManager.unregisterMouseListener(mouseAdapter);
		keyManager.unregisterKeyListener(hotkeyListener);
		collectedGroundItems.clear();
		clientThread.invokeLater(this::removeAllLootbeams);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(GroundItemsModifiableConfig.GROUP))
		{
			executor.execute(this::reset);
		}
	}

	/**
	 * First run only: copy every setting of the built-in Ground Items plugin (lists, colours, thresholds,
	 * per-item colours) so this plugin starts out looking exactly like the one it replaces.
	 */
	/**
	 * Copies the built-in Ground Items settings into this plugin's config group. Runs once on first start;
	 * {@code overwrite} re-runs it from the ::importgrounditems command and replaces values already set here.
	 */
	private int importCoreConfig(boolean overwrite)
	{
		final String group = GroundItemsModifiableConfig.GROUP;
		if (!overwrite && configManager.getConfiguration(group, IMPORTED_KEY) != null)
		{
			return 0;
		}
		int copied = 0;

		final String prefix = CORE_GROUP + ".";
		for (String wholeKey : configManager.getConfigurationKeys(prefix))
		{
			final String key = wholeKey.substring(prefix.length());
			if (key.isEmpty() || key.indexOf('.') >= 0)
			{
				continue;
			}

			final String value = configManager.getConfiguration(CORE_GROUP, key);
			if (value != null && (overwrite || configManager.getConfiguration(group, key) == null))
			{
				configManager.setConfiguration(group, key, value);
				copied++;
			}
		}

		configManager.setConfiguration(group, IMPORTED_KEY, true);
		return copied;
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded event)
	{
		var wv = event.getWorldView();
		collectedGroundItems.values().removeIf(g -> g.getItemLayer().getWorldView() == wv);
		lootbeams.values().removeIf(l -> l.getWorldView() == wv.getId());
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned itemSpawned)
	{
		TileItem item = itemSpawned.getItem();
		Tile tile = itemSpawned.getTile();
		ItemLayer layer = tile.getItemLayer();

		GroundItem groundItem = buildGroundItem(layer, item);
		GroundItem existing = collectedGroundItems.get(tile.getWorldLocation(), item.getId());
		if (existing != null)
		{
			existing.setQuantity(existing.getQuantity() + groundItem.getQuantity());
			// The spawn time remains set at the oldest spawn
			existing.reset();
		}
		else
		{
			collectedGroundItems.put(tile.getWorldLocation(), item.getId(), groundItem);
		}

		if (shouldDisplayItem(config.ownershipFilterMode(), groundItem.getOwnership(), client.getVarbitValue(VarbitID.IRONMAN)))
		{
			notifyHighlightedItem(groundItem);
		}

		handleLootbeam(tile.getWorldLocation());
	}

	void updateItemColor(GroundItem item)
	{
		if (item.color != null)
		{
			return;
		}

		final Color highlighted = getHighlighted(item);
		final boolean hidden = isHidden(item);

		item.highlighted = highlighted != null;
		item.hidden = hidden;
		item.color = getItemColor(highlighted, hidden);
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned itemDespawned)
	{
		TileItem item = itemDespawned.getItem();
		Tile tile = itemDespawned.getTile();

		GroundItem groundItem = collectedGroundItems.get(tile.getWorldLocation(), item.getId());
		if (groundItem == null)
		{
			return;
		}

		if (groundItem.getQuantity() <= item.getQuantity())
		{
			collectedGroundItems.remove(tile.getWorldLocation(), item.getId());
		}
		else
		{
			groundItem.setQuantity(groundItem.getQuantity() - item.getQuantity());
			// When picking up an item when multiple stacks appear on the ground,
			// it is not known which item is picked up, so we invalidate the spawn
			// time
			groundItem.setSpawnTime(null);
			groundItem.reset();
		}

		handleLootbeam(tile.getWorldLocation());
	}

	@Subscribe
	public void onItemQuantityChanged(ItemQuantityChanged itemQuantityChanged)
	{
		TileItem item = itemQuantityChanged.getItem();
		Tile tile = itemQuantityChanged.getTile();
		int oldQuantity = itemQuantityChanged.getOldQuantity();
		int newQuantity = itemQuantityChanged.getNewQuantity();

		int diff = newQuantity - oldQuantity;
		GroundItem groundItem = collectedGroundItems.get(tile.getWorldLocation(), item.getId());
		if (groundItem != null)
		{
			groundItem.setQuantity(groundItem.getQuantity() + diff);
			groundItem.reset();
		}

		handleLootbeam(tile.getWorldLocation());
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!config.collapseEntries())
		{
			return;
		}

		final MenuEntry[] menuEntries = client.getMenuEntries();
		final List<MenuEntryWithCount> newEntries = new ArrayList<>(menuEntries.length);

		outer:
		for (int i = menuEntries.length - 1; i >= 0; i--)
		{
			MenuEntry menuEntry = menuEntries[i];

			MenuAction menuType = menuEntry.getType();
			if (menuType == MenuAction.GROUND_ITEM_FIRST_OPTION || menuType == MenuAction.GROUND_ITEM_SECOND_OPTION
				|| menuType == MenuAction.GROUND_ITEM_THIRD_OPTION || menuType == MenuAction.GROUND_ITEM_FOURTH_OPTION
				|| menuType == MenuAction.GROUND_ITEM_FIFTH_OPTION || menuType == MenuAction.EXAMINE_ITEM_GROUND)
			{
				for (MenuEntryWithCount entryWCount : newEntries)
				{
					if (entryWCount.getEntry().equals(menuEntry))
					{
						entryWCount.increment();
						continue outer;
					}
				}
			}

			newEntries.add(new MenuEntryWithCount(menuEntry));
		}

		Collections.reverse(newEntries);

		client.setMenuEntries(newEntries.stream().map(e ->
		{
			final MenuEntry entry = e.getEntry();
			final int count = e.getCount();
			if (count > 1)
			{
				entry.setTarget(entry.getTarget() + " x " + count);
			}

			return entry;
		}).toArray(MenuEntry[]::new));
	}

	private GroundItem buildGroundItem(final ItemLayer layer, final TileItem item)
	{
		// Collect the data for the item
		final int itemId = item.getId();
		final ItemComposition itemComposition = itemManager.getItemComposition(itemId);
		final int realItemId = itemComposition.getNote() != -1 ? itemComposition.getLinkedNoteId() : itemId;
		final int alchPrice = itemComposition.getHaPrice();
		final int despawnTime = item.getDespawnTime() - client.getTickCount();
		final int visibleTime = item.getVisibleTime() - client.getTickCount();

		final GroundItem groundItem = GroundItem.builder()
			.id(itemId)
			.itemId(realItemId)
			.quantity(item.getQuantity())
			.itemLayer(layer)
			.name(itemComposition.getName())
			.haPrice(alchPrice)
			.tradeable(itemComposition.isGeTradeable())
			.ownership(item.getOwnership())
			.isPrivate(item.isPrivate())
			.spawnTime(Instant.now())
			.stackable(itemComposition.isStackable())
			.despawnTime(Duration.of(despawnTime, RSTimeUnit.GAME_TICKS))
			.visibleTime(Duration.of(visibleTime, RSTimeUnit.GAME_TICKS))
			.displayName(resolveRename(itemComposition.getName()))
			.build();

		// Update item price in case it is coins
		if (realItemId == COINS)
		{
			groundItem.setHaPrice(1);
			groundItem.setGePrice(1);
		}
		else
		{
			groundItem.setGePrice(itemManager.getItemPrice(realItemId));
		}

		return groundItem;
	}

	private void reset()
	{
		highlightedItems = new ItemList(Text.fromCSV(config.getHighlightItems()));
		hiddenItems = new ItemList(Text.fromCSV(config.getHiddenItems()));
		parseRenames();

		// Cache colors
		ImmutableList.Builder<PriceHighlight> priceCheckBuilder = ImmutableList.builder();

		if (config.insaneValuePrice() > 0)
		{
			priceCheckBuilder.add(new PriceHighlight(config.insaneValuePrice(), config.insaneValueColor()));
		}

		if (config.highValuePrice() > 0)
		{
			priceCheckBuilder.add(new PriceHighlight(config.highValuePrice(), config.highValueColor()));
		}

		if (config.mediumValuePrice() > 0)
		{
			priceCheckBuilder.add(new PriceHighlight(config.mediumValuePrice(), config.mediumValueColor()));
		}

		if (config.lowValuePrice() > 0)
		{
			priceCheckBuilder.add(new PriceHighlight(config.lowValuePrice(), config.lowValueColor()));
		}

		priceChecks = priceCheckBuilder.build();

		clientThread.invokeLater(() -> collectedGroundItems.values().forEach(item ->
		{
			item.reset();
			item.setDisplayName(resolveRename(item.getName()));
		}));
		clientThread.invokeLater(this::handleLootbeams);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		MenuAction type = MenuAction.of(event.getType());
		final boolean hotKeyPressed = client.isKeyPressed(KeyCode.KC_SHIFT);
		final boolean groundItemAction = type == MenuAction.GROUND_ITEM_FIRST_OPTION || type == MenuAction.GROUND_ITEM_SECOND_OPTION ||
			type == MenuAction.GROUND_ITEM_THIRD_OPTION || type == MenuAction.GROUND_ITEM_FOURTH_OPTION ||
			type == MenuAction.GROUND_ITEM_FIFTH_OPTION || type == MenuAction.WIDGET_TARGET_ON_GROUND_ITEM;

		if (!groundItemAction && type != MenuAction.EXAMINE_ITEM_GROUND)
		{
			return;
		}

		final int itemId = event.getIdentifier();
		final MenuEntry lastEntry = event.getMenuEntry();
		final GroundItem groundItem = findGroundItem(event);

		if (groundItemAction && groundItem != null)
		{
			updateItemColor(groundItem);

			final boolean canBeRecolored = groundItem.highlighted || (groundItem.hidden && config.recolorMenuHiddenItems());

			if ((config.itemHighlightMode() == ItemHighlightMode.MENU || config.itemHighlightMode() == ItemHighlightMode.BOTH) &&
				(canBeRecolored && !groundItem.color.equals(config.defaultColor())))
			{
				final MenuHighlightMode mode = config.menuHighlightMode();

				if (mode == BOTH || mode == OPTION)
				{
					lastEntry.setOption(ColorUtil.prependColorTag(lastEntry.getOption(), groundItem.color));
				}

				if (mode == BOTH || mode == NAME)
				{
					// <col=ff9040>Logs
					// <col=00ff00>Telekinetic Grab</col><col=ffffff> -> <col=ff9040>Logs
					String target = lastEntry.getTarget();

					int i = target.lastIndexOf('>');
					lastEntry.setTarget(target.substring(0, i - 11) + ColorUtil.colorTag(groundItem.color) + target.substring(i + 1));
				}
			}
		}

		if (groundItem != null && config.renameInMenu() && groundItem.isRenamed())
		{
			renameTarget(lastEntry, groundItem.getName(), groundItem.getDisplayName());
		}

		if (groundItemAction && groundItem != null)
		{
			final int quantity = groundItem.getQuantity();
			if (config.showMenuItemQuantities() && groundItem.isStackable() && quantity > 1)
			{
				lastEntry.setTarget(lastEntry.getTarget() + " (" + quantity + ")");
			}

			if (groundItem.hidden && !groundItem.highlighted && config.deprioritizeHiddenItems())
			{
				lastEntry.setDeprioritized(true);
			}
		}

		if (hotKeyPressed && type == MenuAction.EXAMINE_ITEM_GROUND)
		{
			final String target = lastEntry.getTarget();
			addColorMenu(itemId, target);

			final String realName = groundItem != null
				? groundItem.getName()
				: itemManager.getItemComposition(itemId).getName();
			final String key = renameKey(realName);
			final String customName = renames.get(key.toLowerCase());

			client.createMenuEntry(-1)
				.setOption("Rename")
				.setTarget(target)
				.setType(MenuAction.RUNELITE)
				.onClick(e -> openRename(key, customName));

			if (customName != null)
			{
				client.createMenuEntry(-1)
					.setOption("Reset name")
					.setTarget(target)
					.setType(MenuAction.RUNELITE)
					.onClick(e -> setRename(key, null));
			}

			Menu renamesMenu = client.createMenuEntry(-1)
				.setOption("Renames")
				.setTarget("")
				.setType(MenuAction.RUNELITE)
				.createSubMenu();
			renamesMenu.createMenuEntry(-1)
				.setOption("Export to clipboard")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> exportRenames());
			renamesMenu.createMenuEntry(-1)
				.setOption("Import from clipboard")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> importRenames());
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		switch (event.getCommand().toLowerCase())
		{
			case "exportrenames":
				exportRenames();
				break;
			case "importrenames":
				importRenames();
				break;
			case "importgrounditems":
				final int copied = importCoreConfig(true);
				chatMessage("Copied " + copied + " setting" + (copied == 1 ? "" : "s") + " from the built-in Ground Items plugin.");
				break;
			default:
				break;
		}
	}

	private GroundItem findGroundItem(MenuEntryAdded event)
	{
		final WorldView wv = client.getWorldView(event.getMenuEntry().getWorldViewId());
		if (wv == null)
		{
			return null;
		}

		final WorldPoint worldPoint = WorldPoint.fromScene(wv, event.getActionParam0(), event.getActionParam1(), wv.getPlane());
		return collectedGroundItems.get(worldPoint, event.getIdentifier());
	}

	private static void renameTarget(MenuEntry entry, String from, String to)
	{
		final String target = entry.getTarget();
		final int i = target.lastIndexOf(from);
		if (i >= 0)
		{
			entry.setTarget(target.substring(0, i) + to + target.substring(i + from.length()));
		}
	}

	private void addColorMenu(int itemId, String target)
	{
		MenuEntry parent = client.createMenuEntry(-1)
			.setOption("Color")
			.setTarget(target)
			.setType(MenuAction.RUNELITE);
		Menu submenu = parent.createSubMenu();
		Color color = getItemColor(itemId);

		if (color != null)
		{
			submenu.createMenuEntry(-1)
				.setOption("Reset")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> unsetItemColor(itemId));
		}

		submenu.createMenuEntry(-1)
			.setOption("Pick")
			.setType(MenuAction.RUNELITE)
			.onClick(e ->
				SwingUtilities.invokeLater(() ->
				{
					RuneliteColorPicker colorPicker = colorPickerManager.create(client,
						color != null ? color : Color.decode("#FFFFFF"), "Item color", true);
					colorPicker.setOnClose(c -> setItemColor(itemId, c));
					colorPicker.setVisible(true);
				}));

		var colors = Stream.concat(
				collectedGroundItems.values().stream()
					.map(GroundItem::getColor)
					.filter(Objects::nonNull),
				// add some default colors
				Stream.of(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA)
			)
			.distinct()
			.limit(5)
			.collect(Collectors.toList());

		colors.stream()
			.filter(c -> !c.equals(color))
			.forEach(c -> submenu.createMenuEntry(-1)
				.setOption(ColorUtil.prependColorTag("Color", c))
				.setType(MenuAction.RUNELITE)
				.onClick(e -> setItemColor(itemId, c)));
	}

	private void openRename(String key, String customName)
	{
		chatboxPanelManager.openTextInput("Rename " + key + " (leave empty to reset)")
			.value(customName != null ? customName : key)
			.onDone((Consumer<String>) value ->
			{
				final String name = value == null ? "" : value.trim();
				setRename(key, name.isEmpty() || name.equals(key) ? null : name);
			})
			.build();
	}

	void updateList(String item, boolean hiddenList)
	{
		final List<String> hiddenItemSet = new ArrayList<>(Text.fromCSV(config.getHiddenItems()));
		final List<String> highlightedItemSet = new ArrayList<>(Text.fromCSV(config.getHighlightItems()));

		if (hiddenList)
		{
			highlightedItemSet.removeIf(item::equalsIgnoreCase);
		}
		else
		{
			hiddenItemSet.removeIf(item::equalsIgnoreCase);
		}

		final List<String> items = hiddenList ? hiddenItemSet : highlightedItemSet;

		if (!items.removeIf(item::equalsIgnoreCase))
		{
			items.add(item);
		}

		config.setHiddenItems(Text.toCSV(hiddenItemSet));
		config.setHighlightedItem(Text.toCSV(highlightedItemSet));
	}

	private Color getHighlighted(GroundItem groundItem)
	{
		Color itemColor = getItemColor(groundItem.getItemId());

		final int hiddenOrHighlighted = isHiddenOrHighlighted(groundItem);
		if (hiddenOrHighlighted == HIGHLIGHTED)
		{
			return itemColor != null ? itemColor : config.highlightedColor();
		}

		// Explicit hide takes priority over implicit highlight
		if (hiddenOrHighlighted == HIDDEN)
		{
			return null;
		}

		// item highlight colors takes priority over price thresholds
		if (itemColor != null)
		{
			return itemColor;
		}

		final long price = getValueByMode(groundItem.getGePrice(), groundItem.getHaPrice());
		for (PriceHighlight highlight : priceChecks)
		{
			if (price > highlight.getPrice())
			{
				return highlight.getColor();
			}
		}

		return null;
	}

	private boolean isHidden(GroundItem groundItem)
	{
		final int hiddenOrHighlighted = isHiddenOrHighlighted(groundItem);
		final boolean canBeHidden = groundItem.getGePrice() > 0 || groundItem.isTradeable() || !config.dontHideUntradeables();
		final boolean underGe = groundItem.getGePrice() < config.getHideUnderValue();
		final boolean underHa = groundItem.getHaPrice() < config.getHideUnderValue();

		// Explicit highlight takes priority over implicit hide
		return hiddenOrHighlighted == HIDDEN || (hiddenOrHighlighted != HIGHLIGHTED && canBeHidden && underGe && underHa);
	}

	private static final int NONE = 0;
	private static final int HIGHLIGHTED = 1;
	private static final int HIDDEN = 2;

	private int isHiddenOrHighlighted(GroundItem item)
	{
		int hl = highlightedItems.matches(item);
		if (hl == ItemList.EXACT)
		{
			return HIGHLIGHTED;
		}

		int hi = hiddenItems.matches(item);
		if (hi == ItemList.EXACT)
		{
			return HIDDEN;
		}

		if (hl == ItemList.WILDCARD)
		{
			return HIGHLIGHTED;
		}
		if (hi == ItemList.WILDCARD)
		{
			return HIDDEN;
		}

		return NONE;
	}

	private Color getItemColor(Color highlighted, boolean hidden)
	{
		if (highlighted != null)
		{
			return highlighted;
		}

		if (hidden)
		{
			return config.hiddenColor();
		}

		return config.defaultColor();
	}

	@Subscribe
	public void onFocusChanged(FocusChanged focusChanged)
	{
		if (!focusChanged.isFocused())
		{
			setHotKeyPressed(false);
		}
	}

	private void notifyHighlightedItem(GroundItem item)
	{
		final int hiddenOrHighlighted = isHiddenOrHighlighted(item);

		final boolean shouldNotifyHighlighted = config.notifyHighlightedDrops() &&
			hiddenOrHighlighted == HIGHLIGHTED;

		final boolean shouldNotifyTier = config.notifyTier() != HighlightTier.OFF &&
			getValueByMode(item.getGePrice(), item.getHaPrice()) > config.notifyTier().getValueFromTier(config) &&
			hiddenOrHighlighted != HIDDEN;

		final String dropType;
		if (shouldNotifyHighlighted)
		{
			dropType = "highlighted";
		}
		else if (shouldNotifyTier)
		{
			dropType = "valuable";
		}
		else
		{
			return;
		}

		final StringBuilder notificationStringBuilder = new StringBuilder()
			.append("You received a ")
			.append(dropType)
			.append(" drop: ")
			.append(item.getDisplayName());

		if (item.getQuantity() > 1)
		{
			final String stack = QuantityFormatter.quantityToStackSize(item.getQuantity());
			if (config.quantityFormat() == QuantityFormat.TIMES)
			{
				notificationStringBuilder.append(" x ").append(stack);
			}
			else
			{
				notificationStringBuilder.append(" (").append(stack).append(')');
			}
		}

		notifier.notify(notificationStringBuilder.toString());
	}

	/**
	 * Colour of the value tier a price falls in, or the default item colour below every tier.
	 */
	Color getPriceTierColor(long price)
	{
		for (PriceHighlight highlight : priceChecks)
		{
			if (price > highlight.getPrice())
			{
				return highlight.getColor();
			}
		}

		return config.defaultColor();
	}

	long getValueByMode(long gePrice, int haPrice)
	{
		switch (config.valueCalculationMode())
		{
			case GE:
				return gePrice;
			case HA:
				return haPrice;
			default: // Highest
				return Math.max(gePrice, haPrice);
		}
	}

	private void handleLootbeam(WorldPoint worldPoint)
	{
		/*
		 * Return and remove the lootbeam from this location if lootbeam are disabled
		 * Lootbeam can be at this location if the config was just changed
		 */
		if (!(config.showLootbeamForHighlighted() || config.showLootbeamTier() != HighlightTier.OFF))
		{
			removeLootbeam(worldPoint);
			return;
		}

		long highestPrice = -1;
		GroundItem highestItem = null;
		Collection<GroundItem> groundItems = collectedGroundItems.row(worldPoint).values();
		final OwnershipFilterMode ownershipFilterMode = config.ownershipFilterMode();
		final int accountType = client.getVarbitValue(VarbitID.IRONMAN);
		for (GroundItem groundItem : groundItems)
		{
			if (!shouldDisplayItem(ownershipFilterMode, groundItem.getOwnership(), accountType))
			{
				continue;
			}

			/*
			 * highlighted items have the highest priority so if an item is highlighted at this location
			 * we can early return
			 */
			int hiddenOrHighlight = isHiddenOrHighlighted(groundItem);
			if (config.showLootbeamForHighlighted()
				&& hiddenOrHighlight == HIGHLIGHTED)
			{
				addLootbeam(worldPoint,
					MoreObjects.firstNonNull(getItemColor(groundItem.getItemId()), config.highlightedColor()));
				return;
			}

			// Explicit hide takes priority over implicit highlight
			if (hiddenOrHighlight == HIDDEN)
			{
				continue;
			}

			long itemPrice = getValueByMode(groundItem.getGePrice(), groundItem.getHaPrice());
			if (itemPrice > highestPrice)
			{
				highestPrice = itemPrice;
				highestItem = groundItem;
			}
		}

		if (highestItem != null && config.showLootbeamTier() != HighlightTier.OFF)
		{
			for (PriceHighlight highlight : priceChecks)
			{
				if (highestPrice > highlight.getPrice() && highestPrice > config.showLootbeamTier().getValueFromTier(config))
				{
					// use color from the most expensive item
					addLootbeam(worldPoint,
						MoreObjects.firstNonNull(getItemColor(highestItem.getItemId()), highlight.color));
					return;
				}
			}
		}

		removeLootbeam(worldPoint);
	}

	private void handleLootbeams()
	{
		for (WorldPoint worldPoint : collectedGroundItems.rowKeySet())
		{
			handleLootbeam(worldPoint);
		}
	}

	private void removeAllLootbeams()
	{
		for (Lootbeam lootbeam : lootbeams.values())
		{
			lootbeam.remove();
		}

		lootbeams.clear();
	}

	private void addLootbeam(WorldPoint worldPoint, Color color)
	{
		Lootbeam lootbeam = lootbeams.get(worldPoint);
		if (lootbeam == null)
		{
			lootbeam = new Lootbeam(client, clientThread, worldPoint, color, config.lootbeamStyle());
			lootbeams.put(worldPoint, lootbeam);
		}
		else
		{
			lootbeam.setColor(color);
			lootbeam.setStyle(config.lootbeamStyle());
		}
	}

	private void removeLootbeam(WorldPoint worldPoint)
	{
		Lootbeam lootbeam = lootbeams.remove(worldPoint);
		if (lootbeam != null)
		{
			lootbeam.remove();
		}
	}

	Color getItemColor(int itemId)
	{
		return configManager.getConfiguration(GroundItemsModifiableConfig.GROUP, HIGHLIGHT_COLOR_PREFIX + itemId, Color.class);
	}

	void setItemColor(int itemId, Color color)
	{
		configManager.setConfiguration(GroundItemsModifiableConfig.GROUP, HIGHLIGHT_COLOR_PREFIX + itemId, color);
	}

	void unsetItemColor(int itemId)
	{
		configManager.unsetConfiguration(GroundItemsModifiableConfig.GROUP, HIGHLIGHT_COLOR_PREFIX + itemId);
	}

	/**
	 * The name a rename is stored under: the real item name, minus the dose/charge suffix when grouping is on.
	 */
	String renameKey(String realName)
	{
		return config.groupDoses() ? DOSE_SUFFIX.matcher(realName).replaceFirst("") : realName;
	}

	/**
	 * Custom name for a real item name, or null when it has none. Exact lines win over wildcard lines.
	 * With dose grouping on, the item's own dose suffix is put back on the custom name, so a rename of
	 * "Overload (4)" to "Ovl" shows "Ovl (2)" on a 2-dose and "Ovl (4)" on a 4-dose.
	 */
	String resolveRename(String realName)
	{
		final String key = renameKey(realName).toLowerCase();
		String custom = renames.get(key);
		if (custom == null)
		{
			for (Map.Entry<String, String> wildcard : wildcardRenames)
			{
				if (WildcardMatcher.matches(wildcard.getKey(), key))
				{
					custom = wildcard.getValue();
					break;
				}
			}
		}

		if (custom == null)
		{
			return null;
		}

		if (config.groupDoses())
		{
			final Matcher dose = DOSE_SUFFIX.matcher(realName);
			if (dose.find())
			{
				// drop any dose the user typed into the custom name, then append the real one
				custom = DOSE_SUFFIX.matcher(custom).replaceFirst("") + dose.group();
			}
		}

		return custom;
	}

	/**
	 * "Real name=Custom name" lines to an ordered map keyed by the real name as written. Blank, comment and
	 * malformed lines are dropped; a repeated key keeps the last line.
	 */
	private static LinkedHashMap<String, String> parseRenameLines(String text)
	{
		final LinkedHashMap<String, String> result = new LinkedHashMap<>();
		if (text == null)
		{
			return result;
		}

		for (String line : text.split("\\r?\\n"))
		{
			final int eq = line.indexOf('=');
			if (eq <= 0 || line.trim().startsWith("#"))
			{
				continue;
			}

			final String key = line.substring(0, eq).trim();
			final String value = line.substring(eq + 1).trim();
			if (!key.isEmpty() && !value.isEmpty())
			{
				result.put(key, value);
			}
		}

		return result;
	}

	private void parseRenames()
	{
		final Map<String, String> exact = new HashMap<>();
		final List<Map.Entry<String, String>> wildcards = new ArrayList<>();

		for (Map.Entry<String, String> entry : parseRenameLines(config.renames()).entrySet())
		{
			final String key = entry.getKey().toLowerCase();
			if (key.indexOf('*') >= 0)
			{
				wildcards.add(Map.entry(key, entry.getValue()));
			}
			else
			{
				exact.put(key, entry.getValue());
			}
		}

		renames = exact;
		wildcardRenames = wildcards;
	}

	private void exportRenames()
	{
		final LinkedHashMap<String, String> current = parseRenameLines(config.renames());
		if (current.isEmpty())
		{
			chatMessage("No item renames to export.");
			return;
		}

		final StringBuilder sb = new StringBuilder();
		current.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
		chatMessage("Copied " + current.size() + " item rename" + (current.size() == 1 ? "" : "s") + " to the clipboard.");
	}

	private void importRenames()
	{
		String text;
		try
		{
			text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		}
		catch (Exception ex)
		{
			chatMessage("The clipboard has no text to import.");
			return;
		}

		final LinkedHashMap<String, String> incoming = parseRenameLines(text);
		if (incoming.isEmpty())
		{
			chatMessage("The clipboard has no 'Real name=Custom name' lines.");
			return;
		}

		// merge: incoming lines replace existing ones with the same key, everything else is kept
		final LinkedHashMap<String, String> merged = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : parseRenameLines(config.renames()).entrySet())
		{
			merged.put(entry.getKey().toLowerCase(), entry.getKey() + "=" + entry.getValue());
		}

		int added = 0;
		int replaced = 0;
		for (Map.Entry<String, String> entry : incoming.entrySet())
		{
			if (merged.put(entry.getKey().toLowerCase(), entry.getKey() + "=" + entry.getValue()) == null)
			{
				added++;
			}
			else
			{
				replaced++;
			}
		}

		config.setRenames(String.join("\n", merged.values()));
		chatMessage("Imported " + incoming.size() + " item rename" + (incoming.size() == 1 ? "" : "s")
			+ " (" + added + " new, " + replaced + " replaced).");
	}

	private void chatMessage(String message)
	{
		client.addChatMessage(ChatMessageType.CONSOLE, "", message, null);
	}

	/**
	 * Add, replace or (name == null) remove the line for one key in the renames text box.
	 */
	void setRename(String key, String name)
	{
		final List<String> lines = new ArrayList<>(Arrays.asList(config.renames().split("\n")));
		lines.removeIf(line ->
		{
			final int eq = line.indexOf('=');
			return line.trim().isEmpty() || (eq > 0 && line.substring(0, eq).trim().equalsIgnoreCase(key));
		});

		if (name != null)
		{
			lines.add(key + "=" + name);
		}

		config.setRenames(String.join("\n", lines));
	}

	/**
	 * Convert the hidden per-item-id rename keys of 1.0 into lines of the renames text box.
	 */
	private boolean migrateLegacyRenames()
	{
		final String group = GroundItemsModifiableConfig.GROUP;
		final String prefix = group + "." + LEGACY_NAME_PREFIX;
		final List<String> keys = configManager.getConfigurationKeys(prefix);
		if (keys.isEmpty())
		{
			return true;
		}

		if (client.getGameState().getState() < GameState.LOGIN_SCREEN.getState())
		{
			// item definitions are not loaded yet; try again next tick
			return false;
		}

		for (String wholeKey : keys)
		{
			final String key = wholeKey.substring(group.length() + 1);
			final String value = configManager.getConfiguration(group, key);
			try
			{
				final int itemId = Integer.parseInt(key.substring(LEGACY_NAME_PREFIX.length()));
				if (value != null && !value.isEmpty())
				{
					setRename(renameKey(itemManager.getItemComposition(itemId).getName()), value);
				}
			}
			catch (NumberFormatException ignored)
			{
			}
			configManager.unsetConfiguration(group, key);
		}

		return true;
	}

	/*
	 * All      -> none | self | other | group
	 * Drops    -> self | group
	 * Takeable -> none | self | group | (if a main then other)
	 */
	boolean shouldDisplayItem(OwnershipFilterMode filterMode, int ownership, int accountType)
	{
		switch (filterMode)
		{
			case DROPS:
				return ownership == OWNERSHIP_SELF || ownership == OWNERSHIP_GROUP;
			case TAKEABLE:
				return ownership != OWNERSHIP_OTHER || accountType == 0; // Mains can always take items
			default:
				return true;
		}
	}
}
