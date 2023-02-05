package com.morttools;

import io.reactivex.rxjava3.core.Observable;
import net.runelite.api.events.*;
import net.runelite.client.events.ConfigChanged;

public interface IPluginEventsService
{
	Observable<GameTick> getGameTick();

	Observable<ConfigChanged> getConfigChanged();

	Observable<WidgetLoaded> getWidgetLoaded();

	Observable<WidgetClosed> getWidgetClosed();

	Observable<VarbitChanged> getVarbitChanged();

	Observable<GameStateChanged> getGameStateChanged();

	Observable<GameObjectSpawned> getGameObjectSpawned();

	Observable<GameObjectDespawned> getGameObjectDespawned();

	Observable<Integer> getRegionChanged();
}
