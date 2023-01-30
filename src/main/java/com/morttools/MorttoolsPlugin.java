package com.morttools;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
	name = "Mort'tools"
)
public class MorttoolsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private MorttoolsConfig config;

	@Inject
	private Minigame minigame;

	private VarbitMessenger messenger;

	public MorttoolsPlugin()
	{
		messenger = new VarbitMessenger();
		messenger.addVarpHandler( 345, ( value ) -> minigame.setSanctity( value ) );
		messenger.addVarpHandler( 344, ( value ) -> minigame.setResources( value ) );
		messenger.addVarpHandler( 343, ( value ) -> minigame.setRepair( value ) );
	}

	@Override
	protected void startUp() throws Exception
	{

	}

	@Override
	protected void shutDown() throws Exception
	{
	}

	@Subscribe
	public void onVarbitChanged( VarbitChanged event )
	{
		messenger.invoke( event );
	}

	@Subscribe
	public void onGameStateChanged( GameStateChanged gameStateChanged )
	{
	}

	@Provides
	MorttoolsConfig provideConfig( ConfigManager configManager )
	{
		return configManager.getConfig( MorttoolsConfig.class );
	}
}
