package com.morttools;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TempleOverlay extends Overlay
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

	private final PanelComponent panelComponent = new PanelComponent();

	private final List<ColorSetting> sanctityColors = new ArrayList<>();
	private final List<ColorSetting> resourceColors = new ArrayList<>();
	private final List<ColorSetting> repairColors = new ArrayList<>();

	private String repair;
	private String resources;
	private String sanctity;

	private Color repairColor;
	private Color resourceColor;
	private Color sanctityColor;

	public TempleOverlay()
	{
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
	}

	public void setRepair( int value )
	{
		repair = String.format( "%d%%", value );
		repairColor = getColor( repairColors, value );
	}

	public void setResources( int value )
	{
		resources = String.format( "%d%%", value );
		resourceColor = getColor( resourceColors, value );
	}

	public void setSanctity( int value )
	{
		sanctity = String.format( "%d%%", value );
		sanctityColor = getColor( sanctityColors, value );
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
			.right( repair )
			.rightColor( repairColor )
			.build() );

		panelComponent.getChildren().add( LineComponent.builder()
			.left( "Resources:" )
			.right( resources )
			.rightColor( resourceColor )
			.build() );

		panelComponent.getChildren().add( LineComponent.builder()
			.left( "Sanctity:" )
			.right( sanctity )
			.rightColor( sanctityColor )
			.build() );

		return panelComponent.render( graphics );
	}
}
