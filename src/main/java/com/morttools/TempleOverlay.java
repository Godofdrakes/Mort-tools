package com.morttools;

import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.Value;
import lombok.val;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TempleOverlay extends Overlay implements Disposable
{
	@Value
	private class ColorSetting
	{
		public final long value;
		public final Color color;
	}

	private static Color getColor( long value, List<ColorSetting> colorSettings )
	{
		for ( ColorSetting setting : colorSettings )
		{
			if ( value >= setting.value )
			{
				return setting.color;
			}
		}

		return Color.white;
	}

	private static String getText( long value, String suffix )
	{
		return String.format( "%d%s", value, suffix );
	}

	private final CompositeDisposable disposable = new CompositeDisposable();
	private final PanelComponent panelComponent = new PanelComponent();

	private final List<ColorSetting> sanctityColorSettings = new ArrayList<>();
	private final List<ColorSetting> resourceColorSettings = new ArrayList<>();
	private final List<ColorSetting> repairColorSettings = new ArrayList<>();
	private final List<ColorSetting> altarColorSettings = new ArrayList<>();

	private String repairText;
	private String resourcesText;
	private String sanctityText;
	private String altarText;

	private Color repairColor;
	private Color resourceColor;
	private Color sanctityColor;
	private Color altarColor;

	private MorttoolsConfig.AltarLit altarDisplayMode;

	private final Client client;
	private final OverlayManager overlayManager;

	@Inject
	public TempleOverlay(
		IPluginEventsService plugin,
		TempleMinigame templeMinigame,
		MorttoolsConfig config,
		OverlayManager overlayManager,
		Client client,
		Scheduler scheduler )
	{
		this.overlayManager = overlayManager;
		this.client = client;

		repairColorSettings.add( new ColorSetting( 100, Color.GREEN ) );
		repairColorSettings.add( new ColorSetting( 90, Color.YELLOW ) );
		repairColorSettings.add( new ColorSetting( 0, Color.RED ) );

		resourceColorSettings.add( new ColorSetting( 100, Color.GREEN ) );
		resourceColorSettings.add( new ColorSetting( 10, Color.YELLOW ) );
		resourceColorSettings.add( new ColorSetting( 0, Color.RED ) );

		sanctityColorSettings.add( new ColorSetting( 100, Color.GREEN ) );
		sanctityColorSettings.add( new ColorSetting( 10, Color.YELLOW ) );
		sanctityColorSettings.add( new ColorSetting( 0, Color.RED ) );

		altarColorSettings.add( new ColorSetting( 10, Color.GREEN ) );
		altarColorSettings.add( new ColorSetting( 1, Color.yellow ) );
		altarColorSettings.add( new ColorSetting( 0, Color.RED ) );

		setPosition( OverlayPosition.TOP_RIGHT );
		setPriority( OverlayPriority.HIGHEST );

		setRepair( 0 );
		setResources( 0 );
		setSanctity( 0 );
		setAltarDisplayMode( MorttoolsConfig.AltarLit.SecondsRemaining );
		setAltarTickSpan( TickSpan.ZERO );

		val showAltarSeconds = Observable.fromCallable( () -> config.getAltarLitOverlay() )
			.concatWith( plugin.getConfigChanged()
				.filter( value -> value.getKey().equals( MorttoolsConfig.altarLitOverlay ) )
				.map( value -> config.getAltarLitOverlay() ) );

		disposable.addAll(
			templeMinigame.repair.subscribe( this::setRepair ),
			templeMinigame.resources.subscribe( this::setResources ),
			templeMinigame.sanctity.subscribe( this::setSanctity ),
			templeMinigame.altarTicksRemaining.subscribe( this::setAltarTickSpan ),
			showAltarSeconds.observeOn( scheduler ).subscribe( this::setAltarDisplayMode )
		);

		// overlay enabled/disabled from config
		val overlayEnabled = Observable.fromCallable( () -> config.getOverlayEnabled() )
			.concatWith( plugin.getConfigChanged()
				.filter( event -> event.getKey().equals( MorttoolsConfig.overlayEnabled ) )
				.map( event -> config.getOverlayEnabled() ) );

		val showOverlay = Observable.combineLatest(
				templeMinigame.isWidgetLoaded,
				overlayEnabled,
				( a, b ) -> a && b
			)
			.observeOn( scheduler );

		// replace default minigame widget if loaded and overlay is enabled
		disposable.add( showOverlay.subscribe( this::setEnabled ) );

		// Revert any changes we make to the overall client
		disposable.add( Disposable.fromAction( () -> setEnabled( false ) ) );
	}

	public void setEnabled( boolean value )
	{
		val widget = client.getWidget( TempleMinigame.WIDGET_ID );
		if ( widget != null ) widget.setHidden( value );

		if ( value ) overlayManager.add( this );
		else overlayManager.remove( this );
	}

	public void setRepair( int value )
	{
		repairText = getText( value, "%" );
		repairColor = getColor( value, repairColorSettings );
	}

	public void setResources( int value )
	{
		resourcesText = getText( value, "%" );
		resourceColor = getColor( value, resourceColorSettings );
	}

	public void setSanctity( int value )
	{
		sanctityText = getText( value, "%" );
		sanctityColor = getColor( value, sanctityColorSettings );
	}

	public void setAltarTickSpan( TickSpan value )
	{
		switch (altarDisplayMode)
		{
			case Disabled:
				// do nothing
				break;
			case TicksRemaining:
				val ticks = value.getTicks();
				altarText = getText( ticks, "t" );
				altarColor = getColor( ticks, altarColorSettings );
				break;
			case SecondsRemaining:
				val seconds = value.getSeconds();
				altarText = getText( seconds, "s" );
				altarColor = getColor( seconds, altarColorSettings );
				break;
		}
	}

	public void setAltarDisplayMode( MorttoolsConfig.AltarLit value )
	{
		altarDisplayMode = value;
	}

	@Override
	public Dimension render( Graphics2D graphics )
	{
		panelComponent.getChildren().clear();

		panelComponent.getChildren().add( TitleComponent.builder()
			.text( "* Temple Repair *" )
			.color( Color.WHITE )
			.build() );

		panelComponent.setPreferredSize( new Dimension( 130, 0 ) );

		panelComponent.getChildren().add( LineComponent.builder()
			.left( "Repair:" )
			.right( repairText )
			.rightColor( repairColor )
			.build() );

		panelComponent.getChildren().add( LineComponent.builder()
			.left( "Resources:" )
			.right( resourcesText )
			.rightColor( resourceColor )
			.build() );

		panelComponent.getChildren().add( LineComponent.builder()
			.left( "Sanctity:" )
			.right( sanctityText )
			.rightColor( sanctityColor )
			.build() );

		if ( altarDisplayMode != MorttoolsConfig.AltarLit.Disabled )
		{
			panelComponent.getChildren().add( LineComponent.builder()
				.left( "Altar:" )
				.right( altarText )
				.rightColor( altarColor )
				.build() );
		}

		return panelComponent.render( graphics );
	}

	@Override
	public void dispose()
	{
		disposable.dispose();
	}

	@Override
	public boolean isDisposed()
	{
		return disposable.isDisposed();
	}
}
