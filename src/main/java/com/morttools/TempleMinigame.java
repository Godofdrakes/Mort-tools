package com.morttools;

import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import lombok.val;
import net.runelite.api.widgets.WidgetInfo;

import java.util.HashSet;

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

	public static final int FLAMING_FIRE_ALTAR = 4090;
	public static final int UNLIT_FIRE_ALTAR = 4091;
	public static final int DAMAGED_FIRE_ALTAR = 4092;

	public static final TickSpan ALTAR_LIT_TICKS = new TickSpan( 100 );

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
	public final Observable<Boolean> isAltarLit;

	public final Observable<TickSpan> altarTicksRemaining;

	@Inject
	public TempleMinigame( IPluginEventsService plugin, Scheduler scheduler )
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

		val altarObjects = new HashSet<Integer>()
		{{
			add( FLAMING_FIRE_ALTAR );
			add( UNLIT_FIRE_ALTAR );
			add( DAMAGED_FIRE_ALTAR );
		}};

		val altarSpawned = plugin.getGameObjectSpawned()
			.map( value -> value.getGameObject().getId() )
			.filter( value -> altarObjects.contains( value ) );

		val altarDespawned = plugin.getGameObjectDespawned()
			.map( value -> value.getGameObject().getId() )
			.filter( value -> altarObjects.contains( value ) );

		isAltarLit = Observable.merge(
				altarSpawned.map( value -> value == FLAMING_FIRE_ALTAR ),
				altarDespawned.map( value -> false )
			)
			.distinctUntilChanged();

		val gameTicks = Observable.defer( () -> plugin.getGameTick() );

		// count the number of ticks since the altar was lit
		val altarLitTicksElapsed = gameTicks.scan(
			TickSpan.ZERO,
			( tickSpan, gameTick ) -> tickSpan.add( 1 ) );

		// convert to ticks until altar is unlit
		// if we didn't actually see the altar be lit this will be wrong
		// just have a min of zero, it'll sort itself out next time it's lit
		val altarLitTicksRemaining = altarLitTicksElapsed
			.map( value -> ALTAR_LIT_TICKS.subtract( value ) )
			.filter( value -> value.isPositive() );

		// zero out value when altar is unlit
		val altarUnlit = Observable.just( TickSpan.ZERO )
			.concatWith( Observable.never() );

		val altarTicksSwitch = isAltarLit
			.map( value -> value ? altarLitTicksRemaining : altarUnlit );

		altarTicksRemaining = Observable.switchOnNext( altarTicksSwitch );
	}
}
