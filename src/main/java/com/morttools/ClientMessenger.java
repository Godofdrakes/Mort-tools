package com.morttools;

import net.runelite.api.Client;
import net.runelite.api.GameState;

public class ClientMessenger
{
	public void gameStateChanged( GameState gameState, Client client )
	{
		gameStateRouter.invoke( gameState, client );
	}

	public IMessageRouter<GameState,Client> getGameStateRouter()
	{
		return gameStateRouter;
	}

	private final MessageRouter<GameState,Client> gameStateRouter = new MessageRouter<>();
}
