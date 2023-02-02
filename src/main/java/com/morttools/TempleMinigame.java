package com.morttools;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class TempleMinigame implements Disposable
{
	private static final int VARP_REPAIR = 343;
	private static final int VARP_RESOURCES = 344;
	private static final int VARP_SANCTITY = 345;

	private static final int REGION_TEMPLE = 13875;

	public final Observable<Integer> repair;
	public final Observable<Integer> resources;
	public final Observable<Integer> sanctity;

	private final CompositeDisposable disposable = new CompositeDisposable();

	public TempleMinigame( MorttoolsPlugin plugin, Scheduler scheduler )
	{
		repair = plugin.getVarbitChanged()
			// when repair changes
			.filter( event -> event.getVarpId() == VARP_REPAIR )
			// get latest value
			.map( event -> event.getValue() );

		resources = plugin.getVarbitChanged()
			.filter( event -> event.getVarpId() == VARP_RESOURCES )
			.map( event -> event.getValue() );

		sanctity = plugin.getVarbitChanged()
			.filter( event -> event.getVarpId() == VARP_SANCTITY )
			.map( event -> event.getValue() );

		if ( plugin.getLog().isDebugEnabled() )
		{
			disposable.addAll(
				repair.subscribe( value -> plugin.getLog().debug( "repair: {}", value ) ),
				resources.subscribe( value -> plugin.getLog().debug( "resources: {}", value ) ),
				sanctity.subscribe( value -> plugin.getLog().debug( "sanctity: {}", value ) )
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
