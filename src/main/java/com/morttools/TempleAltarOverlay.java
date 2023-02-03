package com.morttools;

import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Observable;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TempleAltarOverlay
	extends Overlay
{
	@Value
	private static class ObjectColorSettings
	{
		private final Observable<Color> color;
		private final Observable<Integer> borderWidth;
		private final Observable<Integer> borderFeather;
	}

	@Value
	private static class ObjectColor
	{
		private final TileObject tileObject;
		private final ObjectComposition composition;
		private final ObjectColorSettings colorSettings;
	}

	private static ObjectColor getObjectColor( TileObject tileObject, Map<Integer,ObjectColorSettings> colorSettings, Client client )
	{
		return new ObjectColor(
			tileObject,
			client.getObjectDefinition( tileObject.getId() ),
			colorSettings.get( tileObject.getId() ) );
	}

	public static final int FLAMING_FIRE_ALTAR = 4090;
	public static final int UNLIT_FIRE_ALTAR = 4091;

	private final Map<Integer,ObjectColorSettings> colorSettingsMap = new HashMap<>();

	private final ModelOutlineRenderer modelOutlineRenderer;

	private Optional<ObjectColor> objectColor = Optional.empty();

	@Inject
	public TempleAltarOverlay(
		IPluginEventsService plugin,
		ModelOutlineRenderer modelOutlineRenderer )
	{
		this.modelOutlineRenderer = modelOutlineRenderer;

		// @todo use swtich operator to set color
	}

	@Override
	public Dimension render( Graphics2D graphics )
	{
//		modelOutlineRenderer
//			.drawOutline(object, (int)config.borderWidth(), color, config.outlineFeather());

		return null;
	}
}
