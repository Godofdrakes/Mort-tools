package com.morttools;

import net.runelite.api.ChatMessageType;

public class ChatMessenger
{
	public void onChatMessage( ChatMessageType messageType, String message )
	{
		chatMessageRouter.invoke( messageType, message );
	}

	public IMessageRouter<ChatMessageType,String> onChatMessageRouter()
	{
		return chatMessageRouter;
	}

	private final MessageRouter<ChatMessageType,String> chatMessageRouter = new MessageRouter<>();
}
