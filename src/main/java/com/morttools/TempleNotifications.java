package com.morttools;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.val;

import java.util.concurrent.TimeUnit;

public class TempleNotifications implements Disposable
{
	private final String repairLowText = "The temple's repair level is low!";
	private final String resourcesLowText = "The temple's resource pool is low!";
	private final String sanctityFullText = "The temple has been sanctified!";

	private static Observable<Boolean> IsFull( Observable<Integer> observable, int threshold )
	{
		return observable.map( value -> value >= threshold ).distinctUntilChanged();
	}

	private static Observable<Boolean> IsLow( Observable<Integer> observable, int threshold )
	{
		return observable.map( value -> value <= threshold ).distinctUntilChanged();
	}

	private final CompositeDisposable disposable = new CompositeDisposable();

	public TempleNotifications( MorttoolsPlugin plugin, TempleMinigame temple, Scheduler scheduler )
	{
		final val repairLow = IsLow( temple.repair, 90 );
		final val resourcesLow = IsLow( temple.resources, 10 );
		final val sanctityFull = IsFull( temple.sanctity, 100 );

		disposable.add( repairLow
			// when repair goes low
			.filter( isLow -> isLow == true )
			// if notifications are enabled
			.takeWhile( isLow -> plugin.getConfig().getNotifyRepair() )
			// no more than once every five seconds
			// prevents spam as the value fluctuates
			.throttleLast( 5, TimeUnit.SECONDS )
			// some implementations of throttle use a background thread
			.observeOn( scheduler )
			// notify the player
			.subscribe( isLow -> plugin.getNotifier().notify( repairLowText ) ) );

		disposable.add( resourcesLow
			.filter( isLow -> isLow == true )
			.takeWhile( isLow -> plugin.getConfig().getNotifyResources() )
			.throttleLast( 5, TimeUnit.SECONDS )
			.observeOn( scheduler )
			.subscribe( isLow -> plugin.getNotifier().notify( resourcesLowText ) ) );

		disposable.add( sanctityFull
			.filter( isFull -> isFull == true )
			.takeWhile( isFull -> plugin.getConfig().getNotifySanctity() )
			.throttleLast( 5, TimeUnit.SECONDS )
			.observeOn( scheduler )
			.subscribe( isFull -> plugin.getNotifier().notify( sanctityFullText ) ) );

		if ( plugin.getLog().isDebugEnabled() )
		{
			disposable.addAll(
				repairLow.subscribe( value -> plugin.getLog().debug( "repairLow: {}", value ) ),
				resourcesLow.subscribe( value -> plugin.getLog().debug( "resourcesLow: {}", value ) ),
				sanctityFull.subscribe( value -> plugin.getLog().debug( "sanctityFull: {}", value ) )
			);
		}
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
