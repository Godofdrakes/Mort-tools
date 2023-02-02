package com.morttools;

import com.google.inject.Provides;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;

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

	private CompositeDisposable disposable;

	private final PublishSubject<ConfigChanged> configChanged = PublishSubject.create();
	private final PublishSubject<WidgetLoaded> widgetLoaded = PublishSubject.create();
	private final PublishSubject<WidgetClosed> widgetClosed = PublishSubject.create();
	private final PublishSubject<VarbitChanged> varbitChanged = PublishSubject.create();
	private final PublishSubject<GameStateChanged> gameStateChanged = PublishSubject.create();
	private final BehaviorSubject<Integer> regionChanged = BehaviorSubject.create();

	public Observable<ConfigChanged> getConfigChanged()
	{
		return configChanged;
	}

	public Observable<WidgetLoaded> getWidgetLoaded()
	{
		return widgetLoaded;
	}

	public Observable<WidgetClosed> getWidgetClosed()
	{
		return widgetClosed;
	}

	public Observable<VarbitChanged> getVarbitChanged()
	{
		return varbitChanged;
	}

	public Observable<GameStateChanged> getGameStateChanged()
	{
		return gameStateChanged;
	}

	public Observable<Integer> getRegionChanged()
	{
		return regionChanged;
	}

	public OverlayManager getOverlayManager()
	{
		return overlayManager;
	}

	public MorttoolsConfig getConfig()
	{
		return config;
	}

	public Notifier getNotifier()
	{
		return notifier;
	}

	public Client getClient()
	{
		return client;
	}

	public Logger getLog()
	{
		return log;
	}

	@Override
	protected void startUp() throws Exception
	{
		// config changes are pushed from a background thread
		// must use a scheduler to safely observe events from the client thread instead
		final val clientScheduler = Schedulers.from( runnable -> clientThread.invoke( runnable ) );

		val templeMinigame = new TempleMinigame( this, clientScheduler );

		disposable = new CompositeDisposable();

		disposable.addAll(
			templeMinigame,
			new TempleOverlay( this, templeMinigame, clientScheduler ),
			new TempleNotifications( this, templeMinigame, clientScheduler ) );

		disposable.add( gameStateChanged
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
			.subscribe( regionChanged::onNext ) );
	}

	@Override
	protected void shutDown() throws Exception
	{
		disposable.dispose();
		disposable = null;
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
