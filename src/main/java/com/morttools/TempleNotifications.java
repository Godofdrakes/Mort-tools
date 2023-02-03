package com.morttools;

import com.google.inject.Inject;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import net.runelite.client.Notifier;

import java.util.concurrent.TimeUnit;

public class TempleNotifications implements Disposable
{
	private final String repairLowText = "The temple's repair level is low!";
	private final String resourcesLowText = "The temple's resource pool is low!";
	private final String sanctityFullText = "The temple has been sanctified!";

	private final CompositeDisposable disposable = new CompositeDisposable();

	@Inject
	public TempleNotifications(
		TempleMinigame templeMinigame,
		MorttoolsConfig config,
		Notifier notifier
	)
	{
		disposable.add( templeMinigame.repairLow
			// when repair goes low
			.filter( isLow -> isLow == true )
			// if notifications are enabled
			.takeWhile( isLow -> config.getNotifyRepair() )
			// no more than once every five seconds
			// prevents spam as the value fluctuates
			.throttleFirst( 5, TimeUnit.SECONDS )
			// notify the player
			.subscribe( isLow -> notifier.notify( repairLowText ) ) );

		disposable.add( templeMinigame.resourcesLow
			.filter( isLow -> isLow == true )
			.takeWhile( isLow -> config.getNotifyResources() )
			.throttleFirst( 5, TimeUnit.SECONDS )
			.subscribe( isLow -> notifier.notify( resourcesLowText ) ) );

		disposable.add( templeMinigame.sanctityFull
			.filter( isFull -> isFull == true )
			.takeWhile( isFull -> config.getNotifySanctity() )
			.throttleFirst( 5, TimeUnit.SECONDS )
			.subscribe( isFull -> notifier.notify( sanctityFullText ) ) );
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
