package com.morttools;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

public class TickSpan
{
	public static final TickSpan ZERO = new TickSpan( 0 );

	public static final long GAME_TICK_MS = 600;

	@Getter
	private final long ticks;

	public TickSpan( long ticks )
	{
		this.ticks = ticks;
	}

	public long getMilliseconds()
	{
		return ticks * GAME_TICK_MS;
	}

	public long getSeconds()
	{
		return TimeUnit.SECONDS.convert( getMilliseconds(), TimeUnit.MILLISECONDS );
	}

	public boolean isPositive()
	{
		// include 0 for the sake of simplicity
		return ticks >= 0;
	}

	public TickSpan add( long value )
	{
		return new TickSpan( this.ticks + value );
	}

	public TickSpan add( TickSpan other )
	{
		return new TickSpan( this.ticks + other.ticks );
	}

	public TickSpan subtract( long value )
	{
		return new TickSpan( this.ticks - value );
	}

	public TickSpan subtract( TickSpan other )
	{
		return new TickSpan( this.ticks - other.ticks );
	}
}
