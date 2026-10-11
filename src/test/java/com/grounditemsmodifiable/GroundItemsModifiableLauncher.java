package com.grounditemsmodifiable;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

// ./gradlew run, or run this from the IDE with -ea
public class GroundItemsModifiableLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GroundItemsModifiablePlugin.class);
		RuneLite.main(args);
	}
}
