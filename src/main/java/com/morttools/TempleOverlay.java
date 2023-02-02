package com.morttools;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.val;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
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

	private static Color getColor( List<ColorSetting> colorSettings, int value )
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

	private static final int WidgetGroup = 171;
	private static final int WidgetChild = 2;
	private static final int WidgetId = WidgetInfo.PACK( WidgetGroup, WidgetChild );

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

	public TempleOverlay( MorttoolsPlugin plugin, TempleMinigame temple, Scheduler scheduler )
	{
		val config = plugin.getConfig();

		repairColors.add( new ColorSetting( 100, Color.GREEN ) );
		repairColors.add( new ColorSetting( 90, Color.YELLOW ) );
		repairColors.add( new ColorSetting( 0, Color.RED ) );

		resourceColors.add( new ColorSetting( 100, Color.GREEN ) );
		resourceColors.add( new ColorSetting( 10, Color.YELLOW ) );
		resourceColors.add( new ColorSetting( 0, Color.RED ) );

		sanctityColors.add( new ColorSetting( 100, Color.GREEN ) );
		sanctityColors.add( new ColorSetting( 10, Color.YELLOW ) );
		sanctityColors.add( new ColorSetting( 0, Color.RED ) );

		disposable.addAll(
			temple.repair.observeOn( scheduler ).subscribe( value -> setRepair( value ) ),
			temple.resources.observeOn( scheduler ).subscribe( value -> setResources( value ) ),
			temple.sanctity.observeOn( scheduler ).subscribe( value -> setSanctity( value ) )
		);

		// overlay enabled/disabled from config
		val overlayEnabled = Observable
			.concat(
				Observable.just( config.getOverlayEnabled() ),
				plugin.getConfigChanged()
					.filter( event -> event.getKey() == MorttoolsConfig.overlayEnabled )
					.map( event -> config.getOverlayEnabled() ) );

		// default minigame widget loaded
		val widgetLoaded = plugin.getWidgetLoaded()
			.filter( event -> event.getGroupId() == WidgetGroup )
			.map( event -> true );

		// default minigame widget closed
		val widgetClosed = plugin.getWidgetClosed()
			.filter( event -> event.getGroupId() == WidgetGroup )
			.map( event -> false );

		val widgetDesired = widgetLoaded.mergeWith( widgetClosed );

		val showOverlay = Observable.combineLatest(
			widgetDesired,
			overlayEnabled.observeOn( scheduler ),
			( a, b ) -> a && b
		);

		if ( plugin.getLog().isDebugEnabled() )
		{
			disposable.addAll(
				widgetDesired.subscribe( value -> plugin.getLog().debug( "widgetDesired: {}", value ) ),
				overlayEnabled.subscribe( value -> plugin.getLog().debug( "overlayEnabled: {}", value ) )
			);
		}

		// replace default minigame widget if loaded and overlay is enabled
		disposable.add( showOverlay
			.subscribe( value ->
			{
				val widget = plugin.getClient().getWidget( WidgetId );
				if ( widget != null ) widget.setHidden( value );

				val overlayManager = plugin.getOverlayManager();
				if ( value ) overlayManager.add( this );
				else overlayManager.remove( this );
			} ) );

		// Revert any changes we make to the overall client
		disposable.add( Disposable.fromAction( () ->
		{
			val widget = plugin.getClient().getWidget( WidgetId );
			if ( widget != null ) widget.setHidden( false );

			plugin.getOverlayManager().remove( this );
		} ) );

		setPosition( OverlayPosition.TOP_RIGHT );
		setPriority( OverlayPriority.HIGHEST );

		setRepair( 0 );
		setResources( 0 );
		setSanctity( 0 );
	}

	public void setRepair( int value )
	{
		repairText = String.format( "%d%%", value );
		repairColor = getColor( repairColors, value );
	}

	public void setResources( int value )
	{
		resourcesText = String.format( "%d%%", value );
		resourceColor = getColor( resourceColors, value );
	}

	public void setSanctity( int value )
	{
		sanctityText = String.format( "%d%%", value );
		sanctityColor = getColor( sanctityColors, value );
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
