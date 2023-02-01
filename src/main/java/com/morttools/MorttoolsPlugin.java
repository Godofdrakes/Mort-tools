package com.morttools;

import com.google.inject.Provides;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
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
	private final PublishSubject<Boolean> pluginEnabled = PublishSubject.create();

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
			.map( value -> value <= threshold )
			.distinctUntilChanged();
	}

	@Override
	protected void startUp() throws Exception
	{
		log.debug( "startUp" );

		// config changes are pushed from a background thread
		// we need to observe these changes on the client thread to avoid threading issues
		final val clientConfigChanged = configChanged
			.observeOn( Schedulers.from( runnable -> clientThread.invoke( runnable ) ) );

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

		subscriptions.add( IsLow( repairChanged, 90 )
			// when repair goes low
			.filter( isLow -> isLow == true )
			// if notifications are enabled
			.takeWhile( isLow -> config.getNotifyRepair() )
			// no more than once every five seconds
			.throttleLast( 5, TimeUnit.SECONDS )
			// notify the player
			.subscribe( isLow -> notifier.notify( TempleNotifications.repairLowText ) ) );

		subscriptions.add( IsLow( resourcesChanged, 10 )
			.filter( isLow -> isLow == true )
			.takeWhile( isLow -> config.getNotifyResources() )
			.throttleLast( 5, TimeUnit.SECONDS )
			.subscribe( isLow -> notifier.notify( TempleNotifications.resourcesLowText ) ) );

		subscriptions.add( IsFull( sanctityChanged, 100 )
			.filter( isFull -> isFull == true )
			.takeWhile( isFull -> config.getNotifySanctity() )
			.throttleLast( 5, TimeUnit.SECONDS )
			.subscribe( isFull -> notifier.notify( TempleNotifications.sanctityFullText ) ) );

		final val regionChanged = Observable.fromCallable( () -> -1 )
			.concatWith( gameStateChanged
				// when logged in
				.filter( event -> event.getGameState() == GameState.LOGGED_IN )
				// get world region
				.map( event ->
				{
					val player = client.getLocalPlayer();
					return player != null ? player.getWorldLocation().getRegionID() : -1;
				} ) )
			// only emit when value changes
			.distinctUntilChanged();

		final val isInTemple = regionChanged
			.map( regionId -> regionId == TempleMinigame.RegionId );

		final val pluginIsEnabled = pluginEnabled
			.filter( value -> value == true );

		final val shouldReplaceWidget = Observable.fromCallable( () -> config.getReplaceWidget() )
			.concatWith( configChanged
				// when value changes
				.filter( event -> event.getKey() == MorttoolsConfig.replaceWidget )
				// get latest value
				.map( event -> config.getReplaceWidget() ) );

		final val showOverlay = Observable.combineLatest(
				pluginIsEnabled,
				shouldReplaceWidget,
				isInTemple,
				( a, b, c ) -> a && b && c )
			.distinctUntilChanged();

		subscriptions.add( showOverlay
			.filter( value -> value == true )
			.subscribe( value -> overlayManager.add( templeOverlay ) ) );

		subscriptions.add( showOverlay
			.filter( value -> value == false )
			.subscribe( value -> overlayManager.remove( templeOverlay ) ) );

		subscriptions.add( showOverlay
			.subscribe( value ->
			{
				final val widget = client.getWidget( TempleMinigame.WidgetId );
				if ( widget != null ) widget.setHidden( value );
			} ) );

		pluginEnabled.onNext( true );
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug( "shutDown" );

		pluginEnabled.onNext( false );

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
