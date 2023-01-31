package com.morttools;

import net.runelite.api.events.VarbitChanged;

public class VarbitMessenger
{
	public interface IChangedHandler
	{
		void invoke( int value );
	}

	public void varbitChanged( VarbitChanged event )
	{
		assert ( event.getVarpId() != -1 || event.getVarbitId() != -1 );

		if ( event.getVarpId() != -1 )
		{
			varpRouter.invoke( event.getVarpId(), event.getValue() );
		}
		else
		{
			varbitRouter.invoke( event.getVarbitId(), event.getValue() );
		}
	}

	public IMessageRouter<Integer,Integer> getVarbitRouter()
	{
		return varbitRouter;
	}

	public IMessageRouter<Integer,Integer> getVarpRouter()
	{
		return varpRouter;
	}

	private final MessageRouter<Integer,Integer> varbitRouter = new MessageRouter<>();
	private final MessageRouter<Integer,Integer> varpRouter = new MessageRouter<>();
}
