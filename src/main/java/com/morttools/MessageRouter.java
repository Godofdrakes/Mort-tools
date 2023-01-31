package com.morttools;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageRouter<TKey, TValue> implements IMessageRouter<TKey,TValue>
{
	@Override
	public IMessageRouter<TKey,TValue> connect( TKey key, IMessageHandler<TValue> handler )
	{
		findOrAddHandlerList( key ).add( handler );
		return this;
	}

	@Override
	public IMessageRouter<TKey,TValue> connectAny( IMessageHandlerPair<TKey,TValue> handler )
	{
		anyHandler.add( handler );
		return this;
	}

	public void invoke( TKey key, TValue value )
	{
		anyHandler.forEach( handler -> handler.invoke( key, value ) );

		List<IMessageHandler<TValue>> handlerList = findHandlerList( key );
		if ( handlerList != null )
		{
			handlerList.forEach( handler -> handler.invoke( value ) );
		}
	}

	private List<IMessageHandler<TValue>> findHandlerList( TKey key )
	{
		return handlerMap.getOrDefault( key, null );
	}

	private List<IMessageHandler<TValue>> findOrAddHandlerList( TKey key )
	{
		List<IMessageHandler<TValue>> handlerList = findHandlerList( key );

		if ( handlerList == null )
		{
			handlerList = new ArrayList<>();
			handlerMap.put( key, handlerList );
		}

		return handlerList;
	}

	private final List<IMessageHandlerPair<TKey,TValue>> anyHandler = new ArrayList<>();

	private final Map<TKey,List<IMessageHandler<TValue>>> handlerMap = new HashMap<>();
}
