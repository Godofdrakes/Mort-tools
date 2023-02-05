package com.morttools;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import net.runelite.api.events.*;
import net.runelite.client.events.ConfigChanged;

public class PluginEvents
	implements IPluginEventsService
{
	public final PublishSubject<GameTick> gameTick = PublishSubject.create();
	public final PublishSubject<ConfigChanged> configChanged = PublishSubject.create();
	public final PublishSubject<WidgetLoaded> widgetLoaded = PublishSubject.create();
	public final PublishSubject<WidgetClosed> widgetClosed = PublishSubject.create();
	public final PublishSubject<VarbitChanged> varbitChanged = PublishSubject.create();
	public final PublishSubject<GameStateChanged> gameStateChanged = PublishSubject.create();
	public final PublishSubject<GameObjectSpawned> gameObjectSpawned = PublishSubject.create();
	public final PublishSubject<GameObjectDespawned> gameObjectDespawned = PublishSubject.create();
	public final PublishSubject<Integer> regionChanged = PublishSubject.create();

	@Override
	public Observable<GameTick> getGameTick() { return gameTick; }

	@Override
	public Observable<ConfigChanged> getConfigChanged() { return configChanged; }

	@Override
	public Observable<WidgetLoaded> getWidgetLoaded() { return widgetLoaded; }

	@Override
	public Observable<WidgetClosed> getWidgetClosed() { return widgetClosed; }

	@Override
	public Observable<VarbitChanged> getVarbitChanged() { return varbitChanged; }

	@Override
	public Observable<GameStateChanged> getGameStateChanged() { return gameStateChanged; }

	@Override
	public Observable<GameObjectSpawned> getGameObjectSpawned() { return gameObjectSpawned; }

	@Override
	public Observable<GameObjectDespawned> getGameObjectDespawned() { return gameObjectDespawned; }

	@Override
	public Observable<Integer> getRegionChanged() { return regionChanged; }
}
