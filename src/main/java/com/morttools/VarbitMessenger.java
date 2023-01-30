package com.morttools;

import net.runelite.api.events.VarbitChanged;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VarbitMessenger
{
	public void addVarbitHandler( int id, VarbitHandler handler )
	{
		assert ( handler != null );
		findHandlerList( varbitChanged, id, true ).add( handler );
	}

	public void addVarpHandler( int id, VarbitHandler handler )
	{
		assert ( handler != null );
		findHandlerList( varpChanged, id, true ).add( handler );
	}

	public void removeVarbitHandler( int id, VarbitHandler handler )
	{
		assert ( handler != null );
		findHandlerList( varbitChanged, id, false ).remove( handler );
	}

	public void removeVarpHandler( int id, VarbitHandler handler )
	{
		assert ( handler != null );
		findHandlerList( varpChanged, id, false ).remove( handler );
	}

	public void invoke( VarbitChanged event )
	{
		List<VarbitHandler> handlerList = null;

		assert ( event.getVarpId() != -1 || event.getVarbitId() != -1 );

		if ( event.getVarbitId() != -1 )
		{
			handlerList = findHandlerList( varbitChanged, event.getVarbitId(), false );
		}
		else if ( event.getVarpId() != -1 )
		{
			handlerList = findHandlerList( varpChanged, event.getVarbitId(), false );
		}

		if ( handlerList != null )
		{
			handlerList.forEach( handler -> handler.varbitChanged( event.getValue() ) );
		}
	}

	private static List<VarbitHandler> findHandlerList( Map<Integer,List<VarbitHandler>> handlerMap, int id, boolean add )
	{
		List<VarbitHandler> handlerList = handlerMap.getOrDefault( id, null );

		if ( handlerList == null && add )
		{
			handlerList = handlerMap.put( id, new ArrayList<>() );
		}

		return handlerList;
	}

	private final Map<Integer,List<VarbitHandler>> varbitChanged = new HashMap<>();
	private final Map<Integer,List<VarbitHandler>> varpChanged = new HashMap<>();
}
