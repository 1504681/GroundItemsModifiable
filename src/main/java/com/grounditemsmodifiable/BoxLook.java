package com.grounditemsmodifiable;

import com.grounditemsmodifiable.config.BoxBorder;
import com.grounditemsmodifiable.config.BoxCorners;
import com.grounditemsmodifiable.config.BoxFill;
import com.grounditemsmodifiable.config.BoxFillColor;
import com.grounditemsmodifiable.config.BoxStyle;
import com.grounditemsmodifiable.config.HighlightTier;
import com.grounditemsmodifiable.config.IconBacking;
import com.grounditemsmodifiable.config.Palette;
import com.grounditemsmodifiable.config.TextStyle;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import lombok.AllArgsConstructor;
import net.runelite.client.util.ColorUtil;

/**
 * One resolved look for the tier boxes: a preset theme, or the individual Custom settings. Also draws it.
 */
@AllArgsConstructor
final class BoxLook
{
	private static final Color DARK = new Color(16, 16, 20);
	private static final Color STONE = new Color(62, 53, 41);

	final BoxCorners corners;
	final BoxStyle style;
	final BoxFill fill;
	final BoxFillColor fillColor;
	final BoxBorder border;
	final HighlightTier glowTier;
	final IconBacking iconBacking;
	final TextStyle text;
	final Palette palette;

	static BoxLook of(GroundItemsModifiableConfig config)
	{
		switch (config.theme())
		{
			case OSRS:
				return new BoxLook(BoxCorners.NOTCHED, BoxStyle.BOX, BoxFill.GRADIENT, BoxFillColor.STONE,
					BoxBorder.DARK_EDGE, HighlightTier.OFF, IconBacking.NONE, TextStyle.BOLD, Palette.NORMAL);
			case MINIMAL:
				return new BoxLook(BoxCorners.SQUARE, BoxStyle.STRIPE, BoxFill.FLAT, BoxFillColor.DARK,
					BoxBorder.NONE, HighlightTier.OFF, IconBacking.NONE, TextStyle.NORMAL, Palette.NORMAL);
			case NEON:
				return new BoxLook(BoxCorners.ROUNDED, BoxStyle.BOX, BoxFill.FLAT, BoxFillColor.DARK,
					BoxBorder.SOLID, HighlightTier.LOW, IconBacking.NONE, TextStyle.NORMAL, Palette.NORMAL);
			case PASTEL:
				return new BoxLook(BoxCorners.PILL, BoxStyle.BOX, BoxFill.FLAT, BoxFillColor.TIER,
					BoxBorder.SOLID, HighlightTier.HIGH, IconBacking.NONE, TextStyle.NORMAL, Palette.PASTEL);
			case CUSTOM:
				return new BoxLook(config.boxCorners(), config.boxStyle(), config.boxFill(), config.boxFillColor(),
					config.boxBorder(), config.glowTier(), config.iconBacking(), config.textStyle(), config.palette());
			case CLASSIC:
			default:
				return new BoxLook(BoxCorners.ROUNDED, BoxStyle.BOX, BoxFill.FLAT, BoxFillColor.TIER,
					BoxBorder.SOLID, HighlightTier.OFF, IconBacking.NONE, TextStyle.NORMAL, Palette.NORMAL);
		}
	}

	/** The box colour after the palette: pastel mixes it halfway to white. Used for fill, border and name. */
	Color tint(Color color)
	{
		return palette == Palette.PASTEL ? mix(color, Color.WHITE, 0.5f) : color;
	}

	/** Corner radius for a box of this height. */
	private float radius(RoundRectangle2D box)
	{
		switch (corners)
		{
			case SQUARE:
				return 0f;
			case PILL:
				return (float) Math.min(box.getHeight(), box.getWidth()) / 2f;
			case NOTCHED:
				return 4f;
			case ROUNDED:
			default:
				return (float) box.getArcWidth() / 2f;
		}
	}

	/** The box outline starting at top centre and running clockwise, so the ring timer can drain along it. */
	Path2D perimeter(RoundRectangle2D box)
	{
		final float x = (float) box.getX();
		final float y = (float) box.getY();
		final float w = (float) box.getWidth();
		final float h = (float) box.getHeight();
		final float r = radius(box);
		final boolean cut = corners == BoxCorners.NOTCHED;
		final Path2D.Float p = new Path2D.Float();
		p.moveTo(x + w / 2, y);
		p.lineTo(x + w - r, y);
		corner(p, cut, x + w, y, x + w, y + r);
		p.lineTo(x + w, y + h - r);
		corner(p, cut, x + w, y + h, x + w - r, y + h);
		p.lineTo(x + r, y + h);
		corner(p, cut, x, y + h, x, y + h - r);
		p.lineTo(x, y + r);
		corner(p, cut, x, y, x + r, y);
		p.lineTo(x + w / 2, y);
		p.closePath();
		return p;
	}

	private static void corner(Path2D.Float p, boolean cut, float cx, float cy, float x, float y)
	{
		if (cut)
		{
			p.lineTo(x, y);
		}
		else
		{
			p.quadTo(cx, cy, x, y);
		}
	}

	boolean glows(long value, GroundItemsModifiableConfig config)
	{
		return glowTier != HighlightTier.OFF && value > glowTier.getValueFromTier(config);
	}

	/** Soft halo in the tier colour, breathing with the shimmer; drawn before the fill. */
	void drawGlow(Graphics2D g, Shape shape, Color color, double pulse)
	{
		final int alpha = (int) (18 + 22 * pulse);
		for (int i = 3; i >= 1; i--)
		{
			g.setStroke(new BasicStroke(i * 2.5f + 1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(ColorUtil.colorWithAlpha(color, alpha));
			g.draw(shape);
		}
	}

	/** Box fill, plus the accent stripe for the stripe style. Shimmering tier fills breathe with pulse. */
	void drawFill(Graphics2D g, Shape shape, Color tierColor, boolean shimmer, double pulse)
	{
		final Color base;
		final int alpha;
		switch (fillColor)
		{
			case DARK:
				base = DARK;
				alpha = shimmer ? (int) (150 + 40 * pulse) : 170;
				break;
			case STONE:
				base = STONE;
				alpha = shimmer ? (int) (180 + 40 * pulse) : 200;
				break;
			case TIER:
			default:
				base = tierColor;
				alpha = palette == Palette.PASTEL
					? (shimmer ? (int) (70 + 45 * pulse) : 85)
					: (shimmer ? (int) (40 + 45 * pulse) : 50);
				break;
		}

		final Rectangle2D b = shape.getBounds2D();
		switch (fill)
		{
			case FLAT:
				g.setColor(ColorUtil.colorWithAlpha(base, alpha));
				g.fill(shape);
				break;
			case GRADIENT:
			{
				final Paint old = g.getPaint();
				g.setPaint(new LinearGradientPaint(
					new Point2D.Double(b.getX(), b.getY()), new Point2D.Double(b.getX(), b.getMaxY()),
					new float[]{0f, 1f},
					new Color[]{ColorUtil.colorWithAlpha(mix(base, Color.WHITE, 0.25f), alpha),
						ColorUtil.colorWithAlpha(mix(base, Color.BLACK, 0.35f), alpha)}));
				g.fill(shape);
				g.setPaint(old);
				break;
			}
			case GLASS:
			{
				g.setColor(ColorUtil.colorWithAlpha(base, alpha));
				g.fill(shape);
				final Shape oldClip = g.getClip();
				g.clip(shape);
				g.setColor(new Color(255, 255, 255, 34));
				g.fill(new Rectangle2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight() / 2));
				g.setColor(new Color(255, 255, 255, 90));
				g.fill(new Rectangle2D.Double(b.getX(), b.getY() + 1, b.getWidth(), 1));
				g.setClip(oldClip);
				break;
			}
			case NONE:
			default:
				break;
		}

		if (style == BoxStyle.STRIPE)
		{
			final Shape oldClip = g.getClip();
			g.clip(shape);
			g.setColor(tierColor);
			g.fill(new Rectangle2D.Double(b.getX(), b.getY(), 3, b.getHeight()));
			g.setClip(oldClip);
		}
	}

	/** Border line; the stripe style has none. */
	void drawBorder(Graphics2D g, Shape shape, RoundRectangle2D box, Color color)
	{
		if (style == BoxStyle.STRIPE)
		{
			return;
		}
		switch (border)
		{
			case SOLID:
				g.setStroke(new BasicStroke(1.5f));
				g.setColor(color);
				g.draw(shape);
				break;
			case DARK_EDGE:
				g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
				g.setColor(new Color(0, 0, 0, 170));
				g.draw(shape);
				g.setStroke(new BasicStroke(1.5f));
				g.setColor(color);
				g.draw(shape);
				break;
			case DOUBLE:
			{
				g.setStroke(new BasicStroke(1f));
				g.setColor(color);
				g.draw(shape);
				final RoundRectangle2D inner = new RoundRectangle2D.Double(box.getX() + 2, box.getY() + 2,
					box.getWidth() - 4, box.getHeight() - 4, Math.max(0, box.getArcWidth() - 4), Math.max(0, box.getArcHeight() - 4));
				g.setColor(ColorUtil.colorWithAlpha(color, 150));
				g.draw(perimeter(inner));
				break;
			}
			case NONE:
			default:
				break;
		}
	}

	/** Dark slot behind an icon. */
	void drawIconBacking(Graphics2D g, int x, int y, int w, int h)
	{
		switch (iconBacking)
		{
			case SQUARE:
				g.setColor(new Color(0, 0, 0, 110));
				g.fill(new RoundRectangle2D.Float(x - 1, y - 1, w + 2, h + 2, 4, 4));
				g.setStroke(new BasicStroke(1f));
				g.setColor(new Color(90, 80, 64, 150));
				g.draw(new RoundRectangle2D.Float(x - 1, y - 1, w + 2, h + 2, 4, 4));
				break;
			case CIRCLE:
			{
				final float d = Math.max(w, h) + 2;
				g.setColor(new Color(0, 0, 0, 110));
				g.fill(new Ellipse2D.Float(x + w / 2f - d / 2, y + h / 2f - d / 2, d, d));
				break;
			}
			case NONE:
			default:
				break;
		}
	}

	private static Color mix(Color a, Color b, float t)
	{
		return new Color(
			Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
			Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
			Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
	}
}
