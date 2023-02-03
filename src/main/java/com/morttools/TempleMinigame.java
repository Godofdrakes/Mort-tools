package com.morttools;

import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Observable;
import lombok.val;
import net.runelite.api.widgets.WidgetInfo;

public class TempleMinigame
{
	private static Observable<Boolean> IsFull( Observable<Integer> observable, int threshold )
	{
		return observable.map( value -> value >= threshold ).distinctUntilChanged();
	}

	private static Observable<Boolean> IsLow( Observable<Integer> observable, int threshold )
	{
		return observable.map( value -> value <= threshold ).distinctUntilChanged();
	}

	public static final int VARP_REPAIR = 343;
	public static final int VARP_RESOURCES = 344;
	public static final int VARP_SANCTITY = 345;

	public static final int REGION_TEMPLE = 13875;

	public static final int WIDGET_GROUP = 171;
	public static final int WIDGET_ID = WidgetInfo.PACK( WIDGET_GROUP, 2 );

	public final Observable<Integer> repair;
	public final Observable<Integer> resources;
	public final Observable<Integer> sanctity;

	public final Observable<Boolean> repairLow;
	public final Observable<Boolean> resourcesLow;
	public final Observable<Boolean> sanctityFull;

	public final Observable<Boolean> isInTemple;
	public final Observable<Boolean> isWidgetLoaded;

	@Inject
	public TempleMinigame( IPluginEventsService plugin )
	{
		repair = Observable.just( 0 )
			.concatWith( plugin.getVarbitChanged()
				// when repair changes
				.filter( event -> event.getVarpId() == VARP_REPAIR )
				// get latest value
				.map( event -> event.getValue() ) );

		resources = Observable.just( 0 )
			.concatWith( plugin.getVarbitChanged()
				.filter( event -> event.getVarpId() == VARP_RESOURCES )
				.map( event -> event.getValue() ) );

		sanctity = Observable.just( 0 )
			.concatWith( plugin.getVarbitChanged()
				.filter( event -> event.getVarpId() == VARP_SANCTITY )
				.map( event -> event.getValue() ) );

		repairLow = IsLow( repair, 90 );
		resourcesLow = IsLow( resources, 10 );
		sanctityFull = IsFull( sanctity, 100 );

		isInTemple = plugin.getRegionChanged()
			.map( value -> value == REGION_TEMPLE )
			.distinctUntilChanged();

		val widgetLoaded = plugin.getWidgetLoaded()
			.filter( event -> event.getGroupId() == WIDGET_GROUP )
			.map( event -> true );

		val widgetClosed = plugin.getWidgetClosed()
			.filter( event -> event.getGroupId() == WIDGET_GROUP )
			.map( event -> false );

		isWidgetLoaded = Observable.merge( widgetLoaded, widgetClosed )
			.distinctUntilChanged();
	}
}
