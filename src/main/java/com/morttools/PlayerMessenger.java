package com.morttools;

import net.runelite.api.Player;

public class PlayerMessenger
{
	public void playerRegionChanged( Integer regionId, Player localPlayer )
	{
		playerRegionRouter.invoke( regionId, localPlayer );
	}

	public IMessageRouter<Integer,Player> getPlayerRegionRouter()
	{
		return playerRegionRouter;
	}

	private final MessageRouter<Integer,Player> playerRegionRouter = new MessageRouter<>();
}
