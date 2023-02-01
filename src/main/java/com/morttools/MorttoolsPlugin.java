package com.morttools;

import com.google.inject.Provides;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
	name = "Mort'tools"
)
public class MorttoolsPlugin extends Plugin
{
	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Client client;

	@Inject
	private Notifier notifier;

	@Inject
	private MorttoolsConfig config;


	private final TempleOverlay templeOverlay = new TempleOverlay();

	private final PublishSubject<ConfigChanged> configChanged = PublishSubject.create();
	private final PublishSubject<WidgetLoaded> widgetLoaded = PublishSubject.create();
	private final PublishSubject<VarbitChanged> varbitChanged = PublishSubject.create();
	private final PublishSubject<GameStateChanged> gameStateChanged = PublishSubject.create();
	private final PublishSubject<MorttoolsPlugin> pluginStartUp = PublishSubject.create();
	private final PublishSubject<MorttoolsPlugin> pluginShutDown = PublishSubject.create();

	private final List<Disposable> subscriptions = new ArrayList<>();

	private static Observable<Boolean> IsFull( Observable<Integer> observable, int threshold )
	{
		return observable
			.map( value -> value >= threshold )
			.distinctUntilChanged();
	}

	private static Observable<Boolean> IsLow( Observable<Integer> observable, int threshold )
	{
		return observable
			.map( value -> value < threshold )
			.distinctUntilChanged();
	}

	@Override
	protected void startUp() throws Exception
	{
		log.debug( "startUp" );

		// allows events sent from task threads to be observed on the client thread
		// final val clientScheduler = Schedulers.from( runnable -> clientThread.invoke( runnable ) );

		final val repairChanged = varbitChanged
			// when repair changes
			.filter( event -> event.getVarpId() == TempleMinigame.VarpRepair )
			// get latest value
			.map( event -> event.getValue() );

		final val resourcesChanged = varbitChanged
			.filter( event -> event.getVarpId() == TempleMinigame.VarpResources )
			.map( event -> event.getValue() );

		final val sanctityChanged = varbitChanged
			.filter( event -> event.getVarpId() == TempleMinigame.VarpSanctity )
			.map( event -> event.getValue() );

		// push values to overlay
		subscriptions.add( repairChanged.subscribe( value -> templeOverlay.setRepair( value ) ) );
		subscriptions.add( resourcesChanged.subscribe( value -> templeOverlay.setResources( value ) ) );
		subscriptions.add( sanctityChanged.subscribe( value -> templeOverlay.setSanctity( value ) ) );

		subscriptions.add( IsFull( sanctityChanged, 100 )
			// when sanctity is full
			.filter( isFull -> isFull == true )
			// while notifications are enabled
			.takeWhile( isFull -> config.getNotifySanctity() )
			// no more than once every five seconds
			.throttleLast( 5, TimeUnit.SECONDS )
			// notify sanctity is full
			.subscribe( isFull -> notifier.notify( TempleNotifications.sanctityFullText ) ) );

		final val regionChanged = gameStateChanged
			// when logged in
			.filter( event -> event.getGameState() == GameState.LOGGED_IN )
			.map( event -> (Object) event )
			// or when the plugin starts up
			.mergeWith( pluginStartUp.map( plugin -> (Object) plugin ) )
			// get world region
			.map( o ->
			{
				val player = client.getLocalPlayer();
				return player != null ? player.getWorldLocation().getRegionID() : -1;
			} )
			// only emit when value changes
			.distinctUntilChanged();

		if ( log.isDebugEnabled() )
		{
			subscriptions.add( regionChanged
				.subscribe( regionId -> log.debug( "entered region {}", regionId ) ) );
		}

		final val isInTemple = regionChanged
			// is temple region?
			.map( regionId -> regionId == TempleMinigame.RegionId );

		subscriptions.add( isInTemple
			.filter( value -> value == true )
			.subscribe( value -> overlayManager.add( templeOverlay ) ) );
		subscriptions.add( isInTemple
			.filter( value -> value == false )
			.subscribe( value -> overlayManager.remove( templeOverlay ) ) );

		subscriptions.add( widgetLoaded
			// when temple widgets are loaded
			.filter( event -> event.getGroupId() == TempleMinigame.WidgetGroup )
			.map( event -> (Object) event )
			// or when plugin starts up
			.mergeWith( pluginStartUp.map( plugin -> (Object) plugin ) )
			.map( o -> Optional.ofNullable( client.getWidget( TempleMinigame.WidgetId ) ) ) // RxJava does not allow null
			.filter( widget -> widget.isPresent() )
			// hide temple widgets
			.subscribe( widget -> widget.get().setHidden( true ) ) );

		// on plugin shutdown
		subscriptions.add( pluginShutDown
			// if temple widgets are loaded
			.map( event -> Optional.ofNullable( client.getWidget( TempleMinigame.WidgetId ) ) )
			.filter( widget -> widget.isPresent() )
			// un-hide temple widgets
			.subscribe( widget -> widget.get().setHidden( false ) ) );

		pluginStartUp.onNext( this );
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug( "shutDown" );

		pluginShutDown.onNext( this );

		subscriptions.forEach( disposable -> disposable.dispose() );
		subscriptions.clear();
	}

	@Subscribe
	public void onConfigChanged( ConfigChanged event )
	{
		configChanged.onNext( event );
	}

	@Subscribe
	public void onVarbitChanged( VarbitChanged event )
	{
		varbitChanged.onNext( event );
	}

	@Subscribe
	public void onGameStateChanged( GameStateChanged event )
	{
		gameStateChanged.onNext( event );
	}

	@Subscribe
	public void onWidgetLoaded( WidgetLoaded event )
	{
		widgetLoaded.onNext( event );
	}

	@Provides
	MorttoolsConfig provideConfig( ConfigManager configManager )
	{
		return configManager.getConfig( MorttoolsConfig.class );
	}
}
