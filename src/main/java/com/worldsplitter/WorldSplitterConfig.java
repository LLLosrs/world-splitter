package com.worldsplitter;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(WorldSplitterConfig.GROUP)
public interface WorldSplitterConfig extends Config
{
	String GROUP = "worldsplitter";

	@ConfigSection(
		name = "Solo mode",
		description = "Manual settings used when you are not in a synced group",
		position = 0
	)
	String soloSection = "soloSection";

	@ConfigSection(
		name = "World pool",
		description = "Which worlds are eligible to be split up",
		position = 1
	)
	String poolSection = "poolSection";

	@ConfigSection(
		name = "Highlighting",
		description = "How your assigned worlds are shown in the world switcher",
		position = 2
	)
	String highlightSection = "highlightSection";

	@ConfigSection(
		name = "Group sync",
		description = "Optional live group synchronization through a self-hosted server",
		position = 3
	)
	String groupSection = "groupSection";

	@ConfigItem(
		keyName = "totalPeople",
		name = "Total people",
		description = "How many people are splitting the world pool",
		section = soloSection,
		position = 0
	)
	@Range(min = 1, max = 200)
	default int totalPeople()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "myPosition",
		name = "My position",
		description = "Your position in the group, where 1 is the first person",
		section = soloSection,
		position = 1
	)
	@Range(min = 1, max = 200)
	default int myPosition()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "worldsPerPerson",
		name = "Worlds per person",
		description = "Number of consecutive worlds assigned to each person in solo mode",
		section = soloSection,
		position = 2
	)
	@Range(min = 1, max = 100)
	default int worldsPerPerson()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "worldPoolStart",
		name = "Pool start world",
		description = "Lowest world number to include",
		section = poolSection,
		position = 0
	)
	default int worldPoolStart()
	{
		return 301;
	}

	@ConfigItem(
		keyName = "worldPoolEnd",
		name = "Pool end world",
		description = "Highest world number to include",
		section = poolSection,
		position = 1
	)
	default int worldPoolEnd()
	{
		return 580;
	}

	@ConfigItem(
		keyName = "includeMembersWorlds",
		name = "Include members worlds",
		description = "Include members worlds in the pool",
		section = poolSection,
		position = 2
	)
	default boolean includeMembersWorlds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeF2pWorlds",
		name = "Include free worlds",
		description = "Include free-to-play worlds in the pool",
		section = poolSection,
		position = 3
	)
	default boolean includeF2pWorlds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includePvpWorlds",
		name = "Include dangerous worlds",
		description = "Include PVP, high-risk, Deadman, LMS and tournament worlds",
		section = poolSection,
		position = 4
	)
	default boolean includePvpWorlds()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightEnabled",
		name = "Highlight assigned worlds",
		description = "Highlight your assigned worlds in the in-game world switcher",
		section = highlightSection,
		position = 0
	)
	default boolean highlightEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight colour",
		description = "Colour used for assigned worlds",
		section = highlightSection,
		position = 1
	)
	default Color highlightColor()
	{
		return new Color(76, 175, 80, 120);
	}

	@ConfigItem(
		keyName = "enableGroupSync",
		name = "Enable group sync",
		description = "Allow create, join, heartbeat and leave requests to the configured server",
		warning = "This feature submits your IP address to a 3rd-party server "
			+ "not controlled or verified by RuneLite developers",
		section = groupSection,
		position = 0
	)
	default boolean enableGroupSync()
	{
		return false;
	}

	@ConfigItem(
		keyName = "serverUrl",
		name = "Sync server URL",
		description = "HTTPS base URL of your self-hosted World Splitter sync server",
		section = groupSection,
		position = 1
	)
	default String serverUrl()
	{
		return "";
	}

	@ConfigItem(
		keyName = "activeGroupCode",
		name = "",
		description = "",
		hidden = true
	)
	default String activeGroupCode()
	{
		return "";
	}

	@ConfigItem(
		keyName = "activeGroupCode",
		name = "",
		description = "",
		hidden = true
	)
	void setActiveGroupCode(String code);

	@ConfigItem(
		keyName = "activeMemberId",
		name = "",
		description = "",
		hidden = true
	)
	default String activeMemberId()
	{
		return "";
	}

	@ConfigItem(
		keyName = "activeMemberId",
		name = "",
		description = "",
		hidden = true
	)
	void setActiveMemberId(String id);
}
