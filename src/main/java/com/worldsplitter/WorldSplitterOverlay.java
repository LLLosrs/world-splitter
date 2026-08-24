package com.worldsplitter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Highlights assigned worlds in the in-game world switcher without covering the world list. */
class WorldSplitterOverlay extends Overlay
{
	private static final int HORIZONTAL_PADDING = 4;
	private static final int VERTICAL_PADDING = 2;
	private static final int FILL_ALPHA = 24;
	private static final int BORDER_ALPHA = 220;

	private final Client client;
	private final WorldSplitterPlugin plugin;
	private final WorldSplitterConfig config;

	@Inject
	private WorldSplitterOverlay(
			Client client,
			WorldSplitterPlugin plugin,
			WorldSplitterConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightEnabled())
		{
			return null;
		}

		Set<Integer> assigned = plugin.getAssignedWorlds();
		if (assigned.isEmpty())
		{
			return null;
		}

		Widget root = client.getWidget(InterfaceID.WORLDSWITCHER, 0);
		if (root == null || root.isHidden())
		{
			return null;
		}

		Rectangle rootBounds = root.getBounds();
		if (rootBounds == null || rootBounds.width <= 0 || rootBounds.height <= 0)
		{
			return null;
		}

		Color configured = config.highlightColor();
		Color fill = withAlpha(configured, FILL_ALPHA);
		Color border = withAlpha(configured, BORDER_ALPHA);

		Deque<Widget> stack = new ArrayDeque<>();
		stack.push(root);

		while (!stack.isEmpty())
		{
			Widget widget = stack.pop();
			if (widget == null || widget.isHidden())
			{
				continue;
			}

			Integer worldNumber = parseWorldNumber(widget.getText());
			if (worldNumber != null && assigned.contains(worldNumber))
			{
				highlightWorldNumber(graphics, widget, rootBounds, fill, border);
			}

			pushChildren(stack, widget.getDynamicChildren());
			pushChildren(stack, widget.getStaticChildren());
			pushChildren(stack, widget.getNestedChildren());
		}

		return null;
	}

	/**
	 * Highlight only the actual world-number widget.
	 *
	 * Do not expand to the parent widget: in the world switcher the parent can be
	 * a whole column/container, which creates the large solid bar that obscures
	 * the interface.
	 */
	private static void highlightWorldNumber(
			Graphics2D graphics,
			Widget widget,
			Rectangle rootBounds,
			Color fill,
			Color border)
	{
		Rectangle bounds = widget.getBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}

		Rectangle highlightBounds = new Rectangle(
				bounds.x - HORIZONTAL_PADDING,
				bounds.y - VERTICAL_PADDING,
				bounds.width + (HORIZONTAL_PADDING * 2),
				bounds.height + (VERTICAL_PADDING * 2)
		);

		Rectangle clipped = highlightBounds.intersection(rootBounds);
		if (clipped.isEmpty())
		{
			return;
		}

		graphics.setColor(fill);
		graphics.fillRect(clipped.x, clipped.y, clipped.width, clipped.height);

		graphics.setColor(border);
		graphics.drawRect(
				clipped.x,
				clipped.y,
				Math.max(0, clipped.width - 1),
				Math.max(0, clipped.height - 1)
		);
	}

	private static Color withAlpha(Color color, int alpha)
	{
		if (color == null)
		{
			return new Color(0, 255, 0, alpha);
		}

		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private static void pushChildren(Deque<Widget> stack, Widget[] children)
	{
		if (children == null)
		{
			return;
		}

		for (Widget child : children)
		{
			if (child != null)
			{
				stack.push(child);
			}
		}
	}

	private static Integer parseWorldNumber(String text)
	{
		if (text == null || text.length() != 3)
		{
			return null;
		}

		for (int i = 0; i < text.length(); i++)
		{
			if (!Character.isDigit(text.charAt(i)))
			{
				return null;
			}
		}

		try
		{
			int value = Integer.parseInt(text);
			return value >= 300 && value < 1000 ? value : null;
		}
		catch (NumberFormatException ignored)
		{
			return null;
		}
	}
}
