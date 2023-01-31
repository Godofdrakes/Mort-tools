package com.morttools;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup( "morttools" )
public interface MorttoolsConfig extends Config
{
	@ConfigSection(
		name = "Temple",
		description = "Temple",
		position = 0
	)
	String templeSection = "templeSection";

	@ConfigItem(
		keyName = "notifySanctity",
		name = "Notify on full sanctity",
		description = "Trigger a notification when the temple's sanctity reaches 100%",
		section = templeSection
	)
	default boolean getNotifySanctity() { return true; }
}
