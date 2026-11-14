package com.grounditemsmodifiable;

import com.grounditemsmodifiable.config.BoxArea;
import java.awt.Rectangle;

/**
 * Where a row's tier/highlight box goes: around the icon, the icon and the name, or the name alone.
 */
final class RowBox
{
	private RowBox()
	{
	}

	/**
	 * What the box of one row actually covers, or null when the row gets no box. Collapsed rows show no
	 * name, so they always box the icon. Rows without an icon only get a box in name-only mode.
	 */
	static BoxArea resolve(BoxArea area, boolean hasIcon, boolean collapsed)
	{
		if (!hasIcon)
		{
			return area == BoxArea.NAME ? BoxArea.NAME : null;
		}
		return collapsed ? BoxArea.ICON : area;
	}

	/**
	 * The box rectangle before padding. The icon sits at iconX/iconY; the name's baseline starts at
	 * textX/textY and spans ascent above and descent below it. iconSpan is the icon width plus its gap.
	 */
	static Rectangle bounds(BoxArea covers, int iconX, int iconY, int iconWidth, int iconHeight, int iconSpan,
		int textX, int textY, int textWidth, int ascent, int descent)
	{
		switch (covers)
		{
			case ICON:
				return new Rectangle(iconX, iconY, iconWidth, iconHeight);
			case NAME:
				return new Rectangle(textX, textY - ascent, textWidth, ascent + descent);
			case ICON_AND_NAME:
			default:
				final int top = Math.min(iconY, textY - ascent);
				final int bottom = Math.max(iconY + iconHeight, textY + descent);
				return new Rectangle(iconX, top, iconSpan + textWidth, bottom - top);
		}
	}
}
