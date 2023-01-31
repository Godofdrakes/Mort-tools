package com.morttoolstest;

import com.morttools.IMessageRouter;
import com.morttools.MessageRouter;
import junit.framework.TestCase;

public class MorttoolsTest extends TestCase
{
	public class MessageHandler<T> implements IMessageRouter.IMessageHandler<T>
	{
		public T value;

		public MessageHandler( T value )
		{
			this.value = value;
		}

		@Override
		public void invoke( T value )
		{
			this.value = value;
		}
	}

	public void testMessageRouterConnect()
	{
		final int expectedKey = 42;
		final int expectedValue = 117;

		MessageHandler<Integer> goodHandler = new MessageHandler<>( 0 );
		MessageHandler<Integer> badHandler = new MessageHandler<>( 0 );

		MessageRouter<Integer,Integer> messageRouter = new MessageRouter<>();

		messageRouter.connect( expectedKey, goodHandler );
		messageRouter.connect( 0, badHandler );

		messageRouter.invoke( expectedKey, expectedValue );

		assertEquals( expectedValue, goodHandler.value.intValue() );
		assertEquals( 0, badHandler.value.intValue() );
	}
}
