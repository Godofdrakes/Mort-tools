package com.morttools;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.Optional;

@Slf4j
@PluginDescriptor(
	name = "Mort'tools"
)
public class MorttoolsPlugin extends Plugin
{
	private static Optional<WorldPoint> GetWorldLocation( Client client )
	{
		assert ( client != null );

		Player player = client.getLocalPlayer();
		if ( player == null ) return Optional.empty();

		return Optional.of( player.getWorldLocation() );
	}

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Client client;

	@Inject
	private Notifier notifier;

	@Inject
	private MorttoolsConfig config;

	private ClientMessenger clientMessenger = new ClientMessenger();
	private PlayerMessenger playerMessenger = new PlayerMessenger();
	private VarbitMessenger varbitMessenger = new VarbitMessenger();
	private WidgetMessenger widgetMessenger = new WidgetMessenger();

	private TempleMinigame templeMinigame = new TempleMinigame();
	private TempleNotifications templeNotifications = new TempleNotifications();
	private TempleOverlay templeOverlay = new TempleOverlay();

	public MorttoolsPlugin()
	{
		varbitMessenger.getVarpRouter()
			// push latest varp data to minigame logic and overlays
			.connect( TempleMinigame.VarpSanctity, value -> templeMinigame.setSanctity( value ) )
			.connect( TempleMinigame.VarpSanctity, value -> templeOverlay.setSanctity( value ) )
			.connect( TempleMinigame.VarpResources, value -> templeMinigame.setResources( value ) )
			.connect( TempleMinigame.VarpResources, value -> templeOverlay.setResources( value ) )
			.connect( TempleMinigame.VarpRepair, value -> templeMinigame.setRepair( value ) )
			.connect( TempleMinigame.VarpRepair, value -> templeOverlay.setRepair( value ) );

		templeMinigame.getEventRouter()
			// notify on full sanctity
			.connect( TempleMinigame.Event.SanctityFull, temple -> templeNotifications.notifySanctityFull( config, notifier ) );

		clientMessenger.getGameStateRouter()
			// aggressively remove any previously added overlays
			.connectAny( ( gameState, client ) -> overlayManager.remove( templeOverlay ) )
			// push region changes to player messenger
			.connect( GameState.LOGGED_IN, client ->
			{
				Optional<WorldPoint> location = GetWorldLocation( client );
				if ( location.isPresent() )
				{
					final int regionId = location.get().getRegionID();
					final Player localPlayer = client.getLocalPlayer();
					playerMessenger.playerRegionChanged( regionId, localPlayer );
				}
			} );

		playerMessenger.getPlayerRegionRouter()
			// add overlay when entering temple
			.connect( TempleMinigame.RegionId, player -> overlayManager.add( templeOverlay ) );

		widgetMessenger.getWidgetLoadedRouter()
			.connect( TempleMinigame.WidgetGroup, ( client ) ->
			{
				// @todo right group, wrong child id?
				Widget widget = client.getWidget( WidgetInfo.PACK( 172, 2 ) );
				if ( widget != null )
				{
					widget.setHidden( true );
				}
			} );
	}

	@Override
	protected void startUp() throws Exception
	{

	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove( templeOverlay );
	}

	@Subscribe
	public void onVarbitChanged( VarbitChanged event )
	{
		varbitMessenger.varbitChanged( event );
	}

	@Subscribe
	public void onGameStateChanged( GameStateChanged gameStateChanged )
	{
		clientMessenger.gameStateChanged( gameStateChanged.getGameState(), client );
	}

	@Subscribe
	public void onWidgetLoaded( WidgetLoaded event )
	{
		widgetMessenger.widgetLoaded( event.getGroupId(), client );
	}

	@Provides
	MorttoolsConfig provideConfig( ConfigManager configManager )
	{
		return configManager.getConfig( MorttoolsConfig.class );
	}
}
