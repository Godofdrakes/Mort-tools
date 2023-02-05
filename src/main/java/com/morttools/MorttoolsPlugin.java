package com.morttools;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.*;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
	name = "Mort'tools"
)
public class MorttoolsPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ClientThread clientThread;
	@Inject
	private MorttoolsConfig config;
	@Inject
	private Notifier notifier;
	@Inject
	private Client client;
	@Inject
	private PluginEvents pluginEvents;

	private CompositeDisposable disposable;

	@Provides
	MorttoolsConfig getConfig( ConfigManager configManager )
	{
		return configManager.getConfig( MorttoolsConfig.class );
	}

	@Provides
	public Scheduler getScheduler( ClientThread clientThread )
	{
		// scheduler used to ensure subscriptions are invoked on the client thread
		return Schedulers.from( clientThread::invoke );
	}

	@Override
	public void configure( Binder binder )
	{
		binder.bind( IPluginEventsService.class ).to( PluginEvents.class );
		binder.bind( PluginEvents.class ).in( Scopes.SINGLETON );
		binder.bind( TempleMinigame.class ).in( Scopes.SINGLETON );
		binder.bind( TempleOverlay.class );
		binder.bind( TempleNotifications.class );
	}

	private <T> Disposable logValues( Observable<T> observable, String label )
	{
		return observable.subscribe( value -> log.debug( "{}: {}", label, value ) );
	}

	@Override
	protected void startUp() throws Exception
	{
		disposable = new CompositeDisposable();

		val templeMinigame = this.injector.getInstance( TempleMinigame.class );
		val templeOverlay = this.injector.getInstance( TempleOverlay.class );
		val templeNotifications = this.injector.getInstance( TempleNotifications.class );

		disposable.addAll( templeOverlay, templeNotifications );

		// monitor for region changes
		disposable.add( pluginEvents.getGameStateChanged()
			// when logged in
			.filter( event -> event.getGameState() == GameState.LOGGED_IN )
			// get world region
			.map( event ->
			{
				val player = client.getLocalPlayer();
				return player != null ? player.getWorldLocation().getRegionID() : -1;
			} )
			// only emit when value changes
			.distinctUntilChanged()
			.subscribe( pluginEvents.regionChanged::onNext ) );

		if ( log.isDebugEnabled() )
		{
			disposable.addAll(
				logValues( templeMinigame.repair, "repair" ),
				logValues( templeMinigame.resources, "resources" ),
				logValues( templeMinigame.sanctity, "sanctity" ),
				logValues( templeMinigame.repairLow, "repairLow" ),
				logValues( templeMinigame.resourcesLow, "resourcesLow" ),
				logValues( templeMinigame.sanctityFull, "sanctityFull" ),
				logValues( templeMinigame.isInTemple, "isInTemple" ),
				logValues( templeMinigame.isWidgetLoaded, "isWidgetLoaded" ),
				logValues( templeMinigame.isAltarLit, "isAltarLit" )
			);
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		disposable.dispose();
		disposable = null;
	}

	@Subscribe
	public void onGameTick( GameTick event ) { pluginEvents.gameTick.onNext( event ); }

	@Subscribe
	public void onConfigChanged( ConfigChanged event ) { pluginEvents.configChanged.onNext( event ); }

	@Subscribe
	public void onVarbitChanged( VarbitChanged event ) { pluginEvents.varbitChanged.onNext( event ); }

	@Subscribe
	public void onGameStateChanged( GameStateChanged event ) { pluginEvents.gameStateChanged.onNext( event ); }

	@Subscribe
	public void onWidgetLoaded( WidgetLoaded event ) { pluginEvents.widgetLoaded.onNext( event ); }

	@Subscribe
	public void onWidgetClosed( WidgetClosed event ) { pluginEvents.widgetClosed.onNext( event ); }

	@Subscribe
	public void onGameObjectSpawned( GameObjectSpawned event ) { pluginEvents.gameObjectSpawned.onNext( event ); }

	@Subscribe
	public void onGameObjectDespawned( GameObjectDespawned event ) { pluginEvents.gameObjectDespawned.onNext( event ); }
}
