package com.morttools;

public interface IMessageRouter<TKey, TValue>
{
	interface IMessageHandler<T>
	{
		void invoke( T value );
	}

	interface IMessageHandlerPair<T, U>
	{
		void invoke( T t, U u );
	}

	IMessageRouter<TKey,TValue> connect( TKey key, IMessageHandler<TValue> handler );

	IMessageRouter<TKey,TValue> connectAny( IMessageHandlerPair<TKey,TValue> handler );
}
