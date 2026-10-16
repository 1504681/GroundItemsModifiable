package com.grounditemsmodifiable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.ItemLayer;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.objenesis.ObjenesisStd;

public class PileOrderTest
{
	private static TileItem item(int id)
	{
		final TileItem ti = mock(TileItem.class);
		when(ti.getId()).thenReturn(id);
		return ti;
	}

	@SuppressWarnings("unchecked")
	private static Map<Integer, Integer> ranks(GroundItemsModifiableOverlay overlay, ItemLayer layer, Map<Long, Map<Integer, Integer>> menu) throws Exception
	{
		final Method m = GroundItemsModifiableOverlay.class.getDeclaredMethod("pileRanks", ItemLayer.class, Map.class);
		m.setAccessible(true);
		return (Map<Integer, Integer>) m.invoke(overlay, layer, menu);
	}

	@Test
	public void ordersByPileThenLearnsFromMenu() throws Exception
	{
		final List<TileItem> pile = Arrays.asList(item(1), item(2), item(2), item(3));
		final Tile tile = mock(Tile.class);
		when(tile.getGroundItems()).thenReturn(pile);
		final Tile[][][] tiles = new Tile[4][104][104];
		tiles[0][10][20] = tile;
		final Scene scene = mock(Scene.class);
		when(scene.getTiles()).thenReturn(tiles);
		final WorldView wv = mock(WorldView.class);
		when(wv.getScene()).thenReturn(scene);
		when(wv.getId()).thenReturn(-1);
		final ItemLayer layer = mock(ItemLayer.class);
		when(layer.getWorldView()).thenReturn(wv);
		when(layer.getPlane()).thenReturn(0);
		final LocalPoint lp = LocalPoint.fromScene(10, 20, wv);
		when(layer.getLocalLocation()).thenReturn(lp);
		when(layer.getTop()).thenReturn(pile.get(3));

		final GroundItemsModifiableOverlay overlay = new ObjenesisStd().newInstance(GroundItemsModifiableOverlay.class);
		// built without its constructor, so set the field initialiser by hand
		final java.lang.reflect.Field memory = GroundItemsModifiableOverlay.class.getDeclaredField("menuMemory");
		memory.setAccessible(true);
		memory.set(overlay, new HashMap<>());
		final Map<Long, Map<Integer, Integer>> none = new HashMap<>();

		// item drawn on top is last in the list -> list read backwards
		Map<Integer, Integer> r = ranks(overlay, layer, none);
		assertEquals(Integer.valueOf(0), r.get(3));
		assertEquals(Integer.valueOf(1), r.get(2));
		assertEquals(Integer.valueOf(2), r.get(1));

		// live menu says list order is menu order: used as is, and learned for other tiles
		final Map<Integer, Integer> live = new HashMap<>();
		live.put(1, 0);
		live.put(2, 1);
		live.put(3, 2);
		final Map<Long, Map<Integer, Integer>> menu = new HashMap<>();
		menu.put((-1L << 32) | (10L << 16) | 20L, live);
		assertEquals(live, ranks(overlay, layer, menu));

		r = ranks(overlay, layer, none);
		assertEquals(Integer.valueOf(0), r.get(1));
		assertEquals(Integer.valueOf(2), r.get(3));

		// a menu reordered by another plugin is remembered after the cursor leaves the tile
		final Map<Integer, Integer> swapped = new HashMap<>();
		swapped.put(2, 0);
		swapped.put(3, 1);
		swapped.put(1, 2);
		menu.put((-1L << 32) | (10L << 16) | 20L, swapped);
		ranks(overlay, layer, menu);
		assertEquals(swapped, ranks(overlay, layer, none));

		// an item the remembered menu never saw goes under the remembered order
		final List<TileItem> grown = Arrays.asList(item(1), item(2), item(3), item(4));
		when(tile.getGroundItems()).thenReturn(grown);
		r = ranks(overlay, layer, none);
		assertEquals(Integer.valueOf(0), r.get(2));
		assertEquals(Integer.valueOf(1), r.get(3));
		assertEquals(Integer.valueOf(2), r.get(1));
		assertEquals(true, r.get(4) > 2);

		// hovering another pile does not touch this one's memory
		final Map<Long, Map<Integer, Integer>> other = new HashMap<>();
		final Map<Integer, Integer> elsewhere = new HashMap<>();
		elsewhere.put(1, 0);
		elsewhere.put(4, 1);
		other.put((-1L << 32) | (11L << 16) | 20L, elsewhere);
		r = ranks(overlay, layer, other);
		assertEquals(Integer.valueOf(0), r.get(2));

		// the tile can't be read: the remembered order still applies
		when(scene.getTiles()).thenReturn(new Tile[4][104][104]);
		assertEquals(swapped, ranks(overlay, layer, none));
	}
}
