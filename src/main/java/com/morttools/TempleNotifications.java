package com.morttools;

import net.runelite.client.Notifier;

import java.awt.*;

public class TempleNotifications
{
	public static final String sanctityFullText = "The temple's sanctity is 100%";

	public void notifySanctityFull( MorttoolsConfig config, Notifier notifier )
	{
		if ( config.getNotifySanctity() )
		{
			notifier.notify( sanctityFullText, TrayIcon.MessageType.INFO );
		}
	}
}
