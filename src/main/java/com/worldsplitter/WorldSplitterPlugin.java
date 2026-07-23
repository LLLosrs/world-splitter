package com.worldsplitter;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.WorldType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

@Slf4j
@PluginDescriptor(
	name = "World Splitter",
	description = "Splits eligible worlds into consecutive blocks and highlights your worlds",
	tags = {"world", "worlds", "split", "group", "hopper"}
)
public class WorldSplitterPlugin extends Plugin
{
	@Inject
	private WorldSplitterConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WorldSplitterOverlay overlay;

	@Inject
	private WorldService worldService;

	@Inject
	private GroupSyncClient groupSyncClient;

	@Inject
	private ScheduledExecutorService executor;

	private final AtomicBoolean groupActionInProgress = new AtomicBoolean();

	private WorldSplitterPanel panel;
	private NavigationButton navButton;
	private ScheduledFuture<?> heartbeatTask;
	private volatile Set<Integer> assignedWorlds = Collections.emptySet();

	private volatile boolean inGroup;
	private volatile String groupCode;
	private volatile String memberId;
	private volatile int groupMemberIndex;
	private volatile int groupMemberCount = 1;
	private volatile String actionStatus = "";

	@Provides
	WorldSplitterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WorldSplitterConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new WorldSplitterPanel(this);

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("World Splitter")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(overlay);

		if (isGroupSyncReady())
		{
			String savedCode = config.activeGroupCode();
			String savedMember = config.activeMemberId();
			if (!isBlank(savedCode) && !isBlank(savedMember))
			{
				groupCode = savedCode;
				memberId = savedMember;
				inGroup = true;
				actionStatus = "Restoring saved group...";
				startHeartbeat();
				executor.execute(this::doHeartbeat);
			}
		}

		recomputeAssignedWorlds();
	}

	@Override
	protected void shutDown()
	{
		stopHeartbeat();
		overlayManager.remove(overlay);
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
		panel = null;
		navButton = null;
		assignedWorlds = Collections.emptySet();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!WorldSplitterConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("enableGroupSync".equals(event.getKey()) && !config.enableGroupSync() && inGroup)
		{
			stopHeartbeat();
			clearLocalGroupState();
			actionStatus = "Group sync disabled";
		}
		recomputeAssignedWorlds();
		refreshPanel();
	}

	void createGroup()
	{
		runGroupAction("Creating group...", () ->
		{
			GroupSyncClient.GroupState state = groupSyncClient.createGroup(config.serverUrl());
			applyGroupState(state);
			actionStatus = "Group created";
		});
	}

	void joinGroup(String code)
	{
		final String requestedCode = code == null ? "" : code.trim();
		if (requestedCode.isEmpty())
		{
			actionStatus = "Enter a 6-character group code";
			refreshPanel();
			return;
		}

		runGroupAction("Joining group...", () ->
		{
			GroupSyncClient.GroupState state = groupSyncClient.joinGroup(
				config.serverUrl(), requestedCode);
			applyGroupState(state);
			actionStatus = "Group joined";
		});
	}

	void leaveGroup()
	{
		if (!groupActionInProgress.compareAndSet(false, true))
		{
			return;
		}

		stopHeartbeat();
		String codeSnapshot = groupCode;
		String memberSnapshot = memberId;
		clearLocalGroupState();
		actionStatus = "Leaving group...";
		refreshPanel();

		executor.execute(() ->
		{
			try
			{
				if (config.enableGroupSync()
					&& !isBlank(codeSnapshot)
					&& !isBlank(memberSnapshot))
				{
					groupSyncClient.leaveGroup(
						config.serverUrl(), codeSnapshot, memberSnapshot);
				}
				actionStatus = "Group left";
			}
			catch (Exception e)
			{
				log.debug("Failed to notify World Splitter server of group leave", e);
				actionStatus = "Group left locally";
			}
			finally
			{
				groupActionInProgress.set(false);
				refreshPanel();
			}
		});
	}

	boolean isInGroup()
	{
		return inGroup;
	}

	boolean isGroupActionInProgress()
	{
		return groupActionInProgress.get();
	}

	boolean isGroupSyncReady()
	{
		return config.enableGroupSync() && !isBlank(config.serverUrl());
	}

	String getActionStatus()
	{
		return actionStatus;
	}

	String getActiveGroupCode()
	{
		return groupCode == null ? "" : groupCode;
	}

	int getGroupMemberCount()
	{
		return groupMemberCount;
	}

	int getGroupMemberIndex()
	{
		return groupMemberIndex;
	}

	Set<Integer> getAssignedWorlds()
	{
		return assignedWorlds;
	}

	List<Integer> getAssignedWorldsSorted()
	{
		List<Integer> list = new ArrayList<>(assignedWorlds);
		Collections.sort(list);
		return list;
	}

	private void runGroupAction(String pendingMessage, CheckedRunnable action)
	{
		if (!isGroupSyncReady())
		{
			actionStatus = config.enableGroupSync()
				? "Set a sync server URL in the plugin settings"
				: "Enable group sync in the plugin settings";
			refreshPanel();
			return;
		}

		if (!groupActionInProgress.compareAndSet(false, true))
		{
			return;
		}

		actionStatus = pendingMessage;
		refreshPanel();
		executor.execute(() ->
		{
			try
			{
				action.run();
			}
			catch (Exception e)
			{
				log.warn("World Splitter group action failed", e);
				actionStatus = readableError(e);
			}
			finally
			{
				groupActionInProgress.set(false);
				refreshPanel();
			}
		});
	}

	private void applyGroupState(GroupSyncClient.GroupState state) throws Exception
	{
		if (!isGroupSyncReady())
		{
			throw new IllegalStateException("Group sync was disabled");
		}

		inGroup = true;
		groupCode = state.getCode();
		memberId = state.getMemberId();
		groupMemberIndex = state.getIndex();
		groupMemberCount = Math.max(1, state.getTotalMembers());
		persistGroupState();
		startHeartbeat();
		recomputeAssignedWorlds();
	}

	private void clearLocalGroupState()
	{
		inGroup = false;
		groupCode = null;
		memberId = null;
		groupMemberIndex = 0;
		groupMemberCount = 1;
		persistGroupState();
		recomputeAssignedWorlds();
	}

	private void persistGroupState()
	{
		config.setActiveGroupCode(groupCode == null ? "" : groupCode);
		config.setActiveMemberId(memberId == null ? "" : memberId);
	}

	private void startHeartbeat()
	{
		stopHeartbeat();
		if (isGroupSyncReady() && inGroup)
		{
			heartbeatTask = executor.scheduleAtFixedRate(
				this::doHeartbeat, 20, 20, TimeUnit.SECONDS);
		}
	}

	private void stopHeartbeat()
	{
		if (heartbeatTask != null)
		{
			heartbeatTask.cancel(false);
			heartbeatTask = null;
		}
	}

	private void doHeartbeat()
	{
		String codeSnapshot = groupCode;
		String memberSnapshot = memberId;
		if (!isGroupSyncReady() || !inGroup
			|| isBlank(codeSnapshot) || isBlank(memberSnapshot))
		{
			return;
		}

		try
		{
			GroupSyncClient.GroupState state = groupSyncClient.heartbeat(
				config.serverUrl(), codeSnapshot, memberSnapshot);
			boolean changed = state.getIndex() != groupMemberIndex
				|| state.getTotalMembers() != groupMemberCount;
			groupMemberIndex = state.getIndex();
			groupMemberCount = Math.max(1, state.getTotalMembers());
			actionStatus = "Group connected";

			if (changed)
			{
				recomputeAssignedWorlds();
			}
			refreshPanel();
		}
		catch (Exception e)
		{
			log.debug("World Splitter heartbeat failed; it will retry", e);
			actionStatus = "Group sync unavailable; retrying";
			refreshPanel();
		}
	}

	private void recomputeAssignedWorlds()
	{
		executor.execute(() ->
		{
			List<Integer> pool = buildWorldPool();
			List<Integer> mine;
			if (inGroup)
			{
				mine = WorldAllocator.allocateEven(
					pool,
					Math.max(1, groupMemberCount),
					Math.max(0, groupMemberIndex));
			}
			else
			{
				mine = WorldAllocator.allocateFixedSize(
					pool,
					Math.max(1, config.totalPeople()),
					Math.max(1, config.worldsPerPerson()),
					Math.max(1, config.myPosition()));
			}

			assignedWorlds = Collections.unmodifiableSet(new LinkedHashSet<>(mine));
			refreshPanel();
		});
	}

	private List<Integer> buildWorldPool()
	{
		List<Integer> pool = new ArrayList<>();
		List<World> worlds = null;

		try
		{
			WorldResult result = worldService.getWorlds();
			worlds = result == null ? null : result.getWorlds();
		}
		catch (Exception e)
		{
			log.debug("Failed to fetch the live world list", e);
		}

		if (worlds == null || worlds.isEmpty())
		{
			for (int world = config.worldPoolStart(); world <= config.worldPoolEnd(); world++)
			{
				pool.add(world);
			}
			return pool;
		}

		for (World world : worlds)
		{
			int id = world.getId();
			if (id < config.worldPoolStart() || id > config.worldPoolEnd())
			{
				continue;
			}

			Set<WorldType> worldTypes = WorldUtil.toWorldTypes(world.getTypes());
			boolean members = worldTypes.contains(WorldType.MEMBERS);
			boolean dangerous = worldTypes.contains(WorldType.PVP)
				|| worldTypes.contains(WorldType.HIGH_RISK)
				|| worldTypes.contains(WorldType.DEADMAN)
				|| worldTypes.contains(WorldType.LAST_MAN_STANDING)
				|| worldTypes.contains(WorldType.PVP_ARENA)
				|| worldTypes.contains(WorldType.TOURNAMENT_WORLD);

			if (dangerous && !config.includePvpWorlds())
			{
				continue;
			}
			if (members && !config.includeMembersWorlds())
			{
				continue;
			}
			if (!members && !config.includeF2pWorlds())
			{
				continue;
			}

			pool.add(id);
		}

		pool.sort(Comparator.naturalOrder());
		return pool;
	}

	private void refreshPanel()
	{
		WorldSplitterPanel currentPanel = panel;
		if (currentPanel != null)
		{
			currentPanel.refresh();
		}
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}

	private static String readableError(Exception e)
	{
		String message = e.getMessage();
		if (isBlank(message))
		{
			return "Group sync request failed";
		}
		return message.length() > 90 ? message.substring(0, 90) : message;
	}

	@FunctionalInterface
	private interface CheckedRunnable
	{
		void run() throws Exception;
	}
}
