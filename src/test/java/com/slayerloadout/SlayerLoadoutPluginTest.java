package com.slayerloadout;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Development entry point. Launches the RuneLite client with this plugin
 * side-loaded so it can be tested without publishing to the Plugin Hub.
 *
 * <p>Run via the {@code run} Gradle task or the green arrow in IntelliJ.</p>
 */
public class SlayerLoadoutPluginTest
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SlayerLoadoutPlugin.class);
		RuneLite.main(args);
	}
}
