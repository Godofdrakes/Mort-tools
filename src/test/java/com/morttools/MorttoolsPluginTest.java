package com.morttools;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MorttoolsPluginTest
{
	public static void main( String[] args ) throws Exception
	{
		ExternalPluginManager.loadBuiltin( MorttoolsPlugin.class );
		RuneLite.main( args );
	}
}