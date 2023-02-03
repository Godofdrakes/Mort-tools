package com.morttools;

import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
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
	private class ColorSetting
	{
		public final int value;
		public final Color color;

		public ColorSetting( int value, Color color )
		{
			this.value = value;
			this.color = color;
		}
	}

	private static Color getColor( int value, List<ColorSetting> colorSettings )
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

	private static String getText( int value )
	{
		return String.format( "%d%%", value );
	}

	private final CompositeDisposable disposable = new CompositeDisposable();
	private final PanelComponent panelComponent = new PanelComponent();

	private final List<ColorSetting> sanctityColors = new ArrayList<>();
	private final List<ColorSetting> resourceColors = new ArrayList<>();
	private final List<ColorSetting> repairColors = new ArrayList<>();

	private String repairText;
	private String resourcesText;
	private String sanctityText;

	private Color repairColor;
	private Color resourceColor;
	private Color sanctityColor;

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

		repairColors.add( new ColorSetting( 100, Color.GREEN ) );
		repairColors.add( new ColorSetting( 90, Color.YELLOW ) );
		repairColors.add( new ColorSetting( 0, Color.RED ) );

		resourceColors.add( new ColorSetting( 100, Color.GREEN ) );
		resourceColors.add( new ColorSetting( 10, Color.YELLOW ) );
		resourceColors.add( new ColorSetting( 0, Color.RED ) );

		sanctityColors.add( new ColorSetting( 100, Color.GREEN ) );
		sanctityColors.add( new ColorSetting( 10, Color.YELLOW ) );
		sanctityColors.add( new ColorSetting( 0, Color.RED ) );

		setPosition( OverlayPosition.TOP_RIGHT );
		setPriority( OverlayPriority.HIGHEST );

		setRepair( 0 );
		setResources( 0 );
		setSanctity( 0 );

		disposable.addAll(
			templeMinigame.repair.observeOn( scheduler ).subscribe( value -> setRepair( value ) ),
			templeMinigame.resources.observeOn( scheduler ).subscribe( value -> setResources( value ) ),
			templeMinigame.sanctity.observeOn( scheduler ).subscribe( value -> setSanctity( value ) )
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
		disposable.add( showOverlay.subscribe( value -> setEnabled( value ) ) );

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
		repairText = getText( value );
		repairColor = getColor( value, repairColors );
	}

	public void setResources( int value )
	{
		resourcesText = getText( value );
		resourceColor = getColor( value, resourceColors );
	}

	public void setSanctity( int value )
	{
		sanctityText = getText( value );
		sanctityColor = getColor( value, sanctityColors );
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
