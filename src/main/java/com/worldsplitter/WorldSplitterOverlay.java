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
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Highlights assigned world rows in the in-game world switcher. */
class WorldSplitterOverlay extends Overlay
{
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

		Widget root = client.getWidget(WidgetID.WORLD_SWITCHER_GROUP_ID, 0);
		if (root == null || root.isHidden())
		{
			return null;
		}

		Color highlight = config.highlightColor();
		graphics.setColor(highlight);

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
				fillWidgetRow(graphics, widget);
			}

			pushChildren(stack, widget.getDynamicChildren());
			pushChildren(stack, widget.getStaticChildren());
			pushChildren(stack, widget.getNestedChildren());
		}

		return null;
	}

	private static void fillWidgetRow(Graphics2D graphics, Widget widget)
	{
		Rectangle bounds = widget.getBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}

		Widget parent = widget.getParent();
		Rectangle parentBounds = parent == null ? null : parent.getBounds();
		Rectangle rowBounds = parentBounds != null && parentBounds.width > bounds.width
			? parentBounds
			: bounds;
		graphics.fillRect(rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height);
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
