package com.morttools;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup( "morttools" )
public interface MorttoolsConfig extends Config
{
	public static final String replaceWidget = "replaceWidget";
	public static final String notifyRepair = "notifyRepair";
	public static final String notifyResources = "notifyResources";
	public static final String notifySanctity = "notifySanctity";

	@ConfigSection(
		name = "Temple",
		description = "Temple",
		position = 0
	)
	String templeSection = "templeSection";

	@ConfigItem(
		keyName = replaceWidget,
		name = "Replace Temple UI",
		description = "Replace the temple's default interface with an overlay that can be moved",
		section = templeSection,
		position = 0
	)
	default boolean getReplaceWidget() { return true; }

	@ConfigItem(
		keyName = notifyRepair,
		name = "Notify on low repair",
		description = "Trigger a notification when the temple's repair level goes below 10%",
		section = templeSection,
		position = 100
	)
	default boolean getNotifyRepair() { return true; }

	@ConfigItem(
		keyName = notifyResources,
		name = "Notify on low resources",
		description = "Trigger a notification when the temple's resource level goes below 10%",
		section = templeSection,
		position = 200
	)
	default boolean getNotifyResources() { return true; }

	@ConfigItem(
		keyName = notifySanctity,
		name = "Notify on full sanctity",
		description = "Trigger a notification when the temple's sanctity reaches 100%",
		section = templeSection,
		position = 300
	)
	default boolean getNotifySanctity() { return true; }
}
