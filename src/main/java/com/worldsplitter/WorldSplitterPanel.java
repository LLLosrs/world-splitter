package com.worldsplitter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class WorldSplitterPanel extends PluginPanel
{
	private final WorldSplitterPlugin plugin;
	private final JLabel modeLabel = new JLabel();
	private final JLabel worldsLabel = new JLabel();
	private final JLabel groupInfoLabel = new JLabel();
	private final JLabel actionStatusLabel = new JLabel();
	private final JPanel notInGroupPanel = new JPanel(new GridLayout(2, 1, 0, 6));
	private final JPanel inGroupPanel = new JPanel(new GridLayout(2, 1, 0, 6));
	private final JTextField joinCodeField = new JTextField();
	private final JButton createGroupButton = new JButton("Create group");
	private final JButton joinGroupButton = new JButton("Join group");
	private final JButton leaveGroupButton = new JButton("Leave group");
	private final JButton copyCodeButton = new JButton("Copy code");

	WorldSplitterPanel(WorldSplitterPlugin plugin)
	{
		this.plugin = plugin;
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

		JLabel title = new JLabel("World Splitter");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setAlignmentX(CENTER_ALIGNMENT);
		content.add(title);
		content.add(Box.createRigidArea(new Dimension(0, 8)));

		modeLabel.setForeground(Color.LIGHT_GRAY);
		modeLabel.setAlignmentX(CENTER_ALIGNMENT);
		content.add(modeLabel);

		worldsLabel.setFont(FontManager.getRunescapeBoldFont());
		worldsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		worldsLabel.setAlignmentX(CENTER_ALIGNMENT);
		content.add(Box.createRigidArea(new Dimension(0, 6)));
		content.add(worldsLabel);
		content.add(Box.createRigidArea(new Dimension(0, 10)));

		groupInfoLabel.setForeground(Color.LIGHT_GRAY);
		groupInfoLabel.setAlignmentX(CENTER_ALIGNMENT);
		groupInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
		content.add(groupInfoLabel);
		content.add(Box.createRigidArea(new Dimension(0, 8)));

		actionStatusLabel.setForeground(new Color(255, 190, 80));
		actionStatusLabel.setAlignmentX(CENTER_ALIGNMENT);
		actionStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		content.add(actionStatusLabel);
		content.add(Box.createRigidArea(new Dimension(0, 8)));

		createGroupButton.addActionListener(e -> plugin.createGroup());

		JPanel joinRow = new JPanel(new BorderLayout(4, 0));
		joinCodeField.setColumns(8);
		joinRow.add(joinCodeField, BorderLayout.CENTER);
		joinRow.add(joinGroupButton, BorderLayout.EAST);
		joinGroupButton.addActionListener(e -> plugin.joinGroup(joinCodeField.getText()));

		leaveGroupButton.addActionListener(e -> plugin.leaveGroup());
		copyCodeButton.addActionListener(e -> copyGroupCode());

		notInGroupPanel.add(createGroupButton);
		notInGroupPanel.add(joinRow);

		JPanel copyRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		copyRow.add(copyCodeButton);
		inGroupPanel.add(copyRow);
		inGroupPanel.add(leaveGroupButton);

		JPanel groupControls = new JPanel();
		groupControls.setLayout(new BoxLayout(groupControls, BoxLayout.Y_AXIS));
		groupControls.add(notInGroupPanel);
		groupControls.add(inGroupPanel);
		content.add(groupControls);

		add(content, BorderLayout.NORTH);
		refresh();
	}

	void refresh()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::refresh);
			return;
		}

		boolean inGroup = plugin.isInGroup();
		boolean busy = plugin.isGroupActionInProgress();
		boolean syncReady = plugin.isGroupSyncReady();
		notInGroupPanel.setVisible(!inGroup);
		inGroupPanel.setVisible(inGroup);

		createGroupButton.setEnabled(syncReady && !busy);
		joinGroupButton.setEnabled(syncReady && !busy);
		joinCodeField.setEnabled(syncReady && !busy);
		leaveGroupButton.setEnabled(!busy);
		copyCodeButton.setEnabled(!busy && inGroup);

		if (inGroup)
		{
			modeLabel.setText("Group mode - code " + plugin.getActiveGroupCode());
			groupInfoLabel.setText(
				"<html><center>" + plugin.getGroupMemberCount()
					+ " people in group<br>you are #"
					+ (plugin.getGroupMemberIndex() + 1) + "</center></html>");
		}
		else
		{
			modeLabel.setText("Solo mode");
			groupInfoLabel.setText(syncReady
				? "Create or join a group to sync worlds live"
				: "Enable group sync and set a server URL in settings");
		}

		String actionStatus = plugin.getActionStatus();
		actionStatusLabel.setText(actionStatus == null ? "" : actionStatus);
		updateWorldsLabel(plugin.getAssignedWorldsSorted());
		revalidate();
		repaint();
	}

	private void copyGroupCode()
	{
		String code = plugin.getActiveGroupCode();
		if (code != null && !code.isEmpty())
		{
			Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new StringSelection(code), null);
		}
	}

	private void updateWorldsLabel(List<Integer> worlds)
	{
		if (worlds.isEmpty())
		{
			worldsLabel.setText("No worlds assigned");
		}
		else if (worlds.size() == 1)
		{
			worldsLabel.setText("World " + worlds.get(0));
		}
		else
		{
			worldsLabel.setText(
				"Worlds " + worlds.get(0) + " - " + worlds.get(worlds.size() - 1)
					+ " (" + worlds.size() + ")");
		}
	}
}
