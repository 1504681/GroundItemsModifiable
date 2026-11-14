package com.grounditemsmodifiable;

import com.grounditemsmodifiable.config.BoxArea;
import java.awt.Rectangle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class RowBoxTest
{
	// icon 16x16 at (100, 90), gap 3, name 40 px wide on baseline 105 with ascent 11 and descent 3
	private static Rectangle bounds(BoxArea area)
	{
		return RowBox.bounds(area, 100, 90, 16, 16, 19, 119, 105, 40, 11, 3);
	}

	@Test
	public void boundsPerArea()
	{
		assertEquals(new Rectangle(100, 90, 16, 16), bounds(BoxArea.ICON));
		assertEquals(new Rectangle(119, 94, 40, 14), bounds(BoxArea.NAME));
		assertEquals(new Rectangle(100, 90, 59, 18), bounds(BoxArea.ICON_AND_NAME));
	}

	@Test
	public void resolve()
	{
		for (BoxArea area : BoxArea.values())
		{
			// collapsed rows show no name
			assertEquals(BoxArea.ICON, RowBox.resolve(area, true, true));
			assertEquals(area, RowBox.resolve(area, true, false));
		}
		// without icons only name-only mode draws a box
		assertEquals(BoxArea.NAME, RowBox.resolve(BoxArea.NAME, false, false));
		assertNull(RowBox.resolve(BoxArea.ICON, false, false));
		assertNull(RowBox.resolve(BoxArea.ICON_AND_NAME, false, false));
	}
}
