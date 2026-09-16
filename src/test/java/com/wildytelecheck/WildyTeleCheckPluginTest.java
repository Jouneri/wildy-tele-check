package com.wildytelecheck;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WildyTeleCheckPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(WildyTeleCheckPlugin.class);
        RuneLite.main(args);
    }
}