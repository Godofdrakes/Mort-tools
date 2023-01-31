package com.morttools;

import net.runelite.api.Client;

public class WidgetMessenger
{
	public void widgetLoaded( int groupId, Client client )
	{
		widgetLoadedRouter.invoke( groupId, client );
	}

	public IMessageRouter<Integer,Client> getWidgetLoadedRouter()
	{
		return widgetLoadedRouter;
	}

	private final MessageRouter<Integer,Client> widgetLoadedRouter = new MessageRouter<>();
}
