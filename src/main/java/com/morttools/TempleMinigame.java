package com.morttools;

import lombok.Getter;

public class TempleMinigame
{
    public static final int VarpSanctity = 345;
    public static final int VarpResources = 344;
    public static final int VarpRepair = 343;

    public static final int RegionId = 13875;

    public static final int WidgetGroup = 171;

    public static boolean IsNowFull( int oldValue, int value )
    {
        return oldValue < 100 && value >= 100;
    }

    public static boolean IsNowNone( int oldValue, int value )
    {
        return oldValue > 0 && value <= 100;
    }

    public enum Event
    {
        SanctityChanged,
        SanctityFull,
        SanctityNone,

        ResourcesChanged,
        ResourcesFull,
        ResourcesNone,

        RepairChanged,
        RepairFull,
        RepairNone,
    }

    public void setSanctity( int value )
    {
        final int oldValue = sanctity;

        sanctity = value;

        eventRouter.invoke( Event.SanctityChanged, this );

        if ( IsNowFull( oldValue, value ) )
            eventRouter.invoke( Event.SanctityFull, this );
        else if ( IsNowNone( oldValue, value ) )
            eventRouter.invoke( Event.SanctityNone, this );
    }

    public void setResources( int value )
    {
        final int oldValue = resources;

        resources = value;

        eventRouter.invoke( Event.ResourcesChanged, this );

        if ( IsNowFull( oldValue, value ) )
            eventRouter.invoke( Event.ResourcesFull, this );
        else if ( IsNowNone( oldValue, value ) )
            eventRouter.invoke( Event.ResourcesNone, this );
    }

    public void setRepair( int value )
    {
        final int oldValue = repair;

        repair = value;

        eventRouter.invoke( Event.RepairChanged, this );

        if ( IsNowFull( oldValue, value ) )
            eventRouter.invoke( Event.RepairFull, this );
        else if ( IsNowNone( oldValue, value ) )
            eventRouter.invoke( Event.RepairNone, this );
    }

    public IMessageRouter<Event,TempleMinigame> getEventRouter()
    {
        return eventRouter;
    }

    @Getter
    private int sanctity;

    @Getter
    private int resources;

    @Getter
    private int repair;

    private final MessageRouter<Event,TempleMinigame> eventRouter = new MessageRouter<>();
}
