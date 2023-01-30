package com.morttools;

import net.runelite.api.Client;
import net.runelite.api.Player;

public class ClientExtensions
{
	public static final int REGION_INVALID = -1;

	public static final int getLocalPlayerRegion( Client client )
	{
		Player player = client.getLocalPlayer();
		if ( player == null )
		{
			return REGION_INVALID;
		}

		return player.getWorldLocation().getRegionID();
	}

	public static final boolean isInRegion( Client client, int region )
	{
		return getLocalPlayerRegion( client ) == region;
	}
}
