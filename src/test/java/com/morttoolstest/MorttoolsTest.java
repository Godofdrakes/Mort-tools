package com.morttoolstest;

import com.morttools.PluginEvents;
import com.morttools.TempleMinigame;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;
import junit.framework.TestCase;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetModalMode;

public class MorttoolsTest extends TestCase
{
	private static VarbitChanged varpChanged( int varp, int value )
	{
		VarbitChanged event = new VarbitChanged();
		event.setVarpId( varp );
		event.setValue( value );
		return event;
	}

	private static Observable<VarbitChanged> templeValuesChanged( int value )
	{
		return Observable.just(
			varpChanged( TempleMinigame.VARP_REPAIR, value ),
			varpChanged( TempleMinigame.VARP_RESOURCES, value ),
			varpChanged( TempleMinigame.VARP_SANCTITY, value )
		);
	}

	private static Observable<Integer> minMaxMin()
	{
		Observable<Integer> minMax = Observable.range( 1, 100 );
		Observable<Integer> maxMin = minMax.map( value -> 100 - value );
		return Observable.concat( minMax, maxMin );
	}

	private static Observable<Integer> adventure()
	{
		final int temple = TempleMinigame.REGION_TEMPLE;
		return Observable.range( temple - 5, 10 );
	}

	private static WidgetLoaded widgetLoaded( int groupId )
	{
		WidgetLoaded event = new WidgetLoaded();
		event.setGroupId( groupId );
		return event;
	}

	private static WidgetClosed widgetClosed( int groupId )
	{
		return new WidgetClosed( groupId, WidgetModalMode.MODAL_CLICKTHROUGH, true );
	}

	private static Observable<WidgetLoaded> loadWidgets()
	{
		final int widgetGroup = TempleMinigame.WIDGET_GROUP;
		return Observable.range( widgetGroup - 5, 10 )
			.map( MorttoolsTest::widgetLoaded );
	}

	private static Observable<WidgetClosed> unloadWidgets()
	{
		final int widgetGroup = TempleMinigame.WIDGET_GROUP;
		return Observable.range( widgetGroup - 5, 10 )
			.map( MorttoolsTest::widgetClosed );
	}

	public void testTempleMinigame()
	{
		PluginEvents pluginEvents = new PluginEvents();

		TestObserver<Integer> repair = TestObserver.create();
		TestObserver<Integer> resources = TestObserver.create();
		TestObserver<Integer> sanctity = TestObserver.create();
		TestObserver<Boolean> repairLow = TestObserver.create();
		TestObserver<Boolean> resourcesLow = TestObserver.create();
		TestObserver<Boolean> sanctityFull = TestObserver.create();
		TestObserver<Boolean> isInTemple = TestObserver.create();
		TestObserver<Boolean> isWidgetLoaded = TestObserver.create();

		TempleMinigame templeMinigame = new TempleMinigame( pluginEvents );

		templeMinigame.repair.subscribe( repair );
		templeMinigame.resources.subscribe( resources );
		templeMinigame.sanctity.subscribe( sanctity );
		templeMinigame.repairLow.subscribe( repairLow );
		templeMinigame.resourcesLow.subscribe( resourcesLow );
		templeMinigame.sanctityFull.subscribe( sanctityFull );
		templeMinigame.isInTemple.subscribe( isInTemple );
		templeMinigame.isWidgetLoaded.subscribe( isWidgetLoaded );

		// empty, fill, and then empty all values
		minMaxMin()
			.flatMap( MorttoolsTest::templeValuesChanged )
			.subscribe( pluginEvents.varbitChanged::onNext );

		// travel through several regions
		adventure().subscribe( pluginEvents.regionChanged::onNext );

		// load and unload some widgets
		loadWidgets().subscribe( pluginEvents.widgetLoaded::onNext );
		unloadWidgets().subscribe( pluginEvents.widgetClosed::onNext );

		// make sure nothing was dropped
		repair.assertValueCount( 201 );
		resources.assertValueCount( 201 );
		sanctity.assertValueCount( 201 );

		// these should only change when the value goes above/below the threshold
		repairLow.assertValues( true, false, true );
		resourcesLow.assertValues( true, false, true );
		sanctityFull.assertValues( false, true, false );

		// this should only change uppon entering/exiting the temple
		isInTemple.assertValues( false, true, false );

		// this should only change when the temple widget is loaded/unloaded
		isWidgetLoaded.assertValues( true, false );
	}
}
