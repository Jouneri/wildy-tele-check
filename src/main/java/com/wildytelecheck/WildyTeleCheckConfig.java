package com.wildytelecheck;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("wildytelecheck")
public interface WildyTeleCheckConfig extends Config
{
    @ConfigSection(
        name = "Accepted teleports",
        description = "Choose which teleport items count as valid Wilderness escape methods.",
        position = 0
    )
    String acceptedTeleportsSection = "acceptedTeleportsSection";

    @ConfigSection(
        name = "Alert settings",
        description = "Configure the warning sound and chat alert.",
        position = 1
    )
    String alertSettingsSection = "alertSettingsSection";

    @ConfigItem(
        keyName = "allowGlory",
        name = "Amulet of glory",
        description = "Accept any charged Amulet of glory as a valid escape teleport",
        position = 0,
        section = acceptedTeleportsSection
    )
    default boolean allowGlory()
    {
        return true;
    }

    @ConfigItem(
        keyName = "allowRoyalSeedPod",
        name = "Royal seed pod",
        description = "Accept a Royal seed pod as a valid escape teleport",
        position = 1,
        section = acceptedTeleportsSection
    )
    default boolean allowRoyalSeedPod()
    {
        return true;
    }

    @ConfigItem(
        keyName = "allowRingOfWealth",
        name = "Ring of wealth",
        description = "Accept any charged Ring of wealth as a valid escape teleport",
        position = 2,
        section = acceptedTeleportsSection
    )
    default boolean allowRingOfWealth()
    {
        return true;
    }

    @ConfigItem(
        keyName = "allowCombatBracelet",
        name = "Combat bracelet",
        description = "Accept any charged Combat bracelet as a valid escape teleport",
        position = 3,
        section = acceptedTeleportsSection
    )
    default boolean allowCombatBracelet()
    {
        return true;
    }

    @ConfigItem(
        keyName = "allowSkillsNecklace",
        name = "Skills necklace",
        description = "Accept any charged Skills necklace as a valid escape teleport",
        position = 4,
        section = acceptedTeleportsSection
    )
    default boolean allowSkillsNecklace()
    {
        return true;
    }

    @ConfigItem(
        keyName = "allowSlayerRing",
        name = "Slayer ring",
        description = "Accept any charged Slayer ring as a valid escape teleport",
        position = 5,
        section = acceptedTeleportsSection
    )
    default boolean allowSlayerRing()
    {
        return true;
    }

    @ConfigItem(
        keyName = "allowEscapeCrystal",
        name = "Escape crystal",
        description = "Accept an Escape crystal as a valid escape teleport",
        position = 6,
        section = acceptedTeleportsSection
    )
    default boolean allowEscapeCrystal()
    {
        return true;
    }

    @ConfigItem(
        keyName = "chatWarning",
        name = "Chat alert",
        description = "Show the warning text in game chat when the alert fires",
        position = 7,
        section = alertSettingsSection
    )
    default boolean chatWarning()
    {
        return true;
    }

    @ConfigItem(
        keyName = "warningText",
        name = "Chat alert text",
        description = "Text shown in game chat when the alert fires",
        position = 8,
        section = alertSettingsSection
    )
    default String warningText()
    {
        return "Dumbass you forgot your teleport!";
    }

    @ConfigItem(
        keyName = "soundAlert",
        name = "Sound alert",
        description = "Play a warning sound when entering the Wilderness without an accepted teleport",
        position = 9,
        section = alertSettingsSection
    )
    default boolean soundAlert()
    {
        return true;
    }

    @Range(
        min = 0,
        max = 100
    )
    @Units(Units.PERCENT)
    @ConfigItem(
        keyName = "alertVolume",
        name = "Sound alert volume",
        description = "Volume of the warning sound",
        position = 10,
        section = alertSettingsSection
    )
    default int alertVolume()
    {
        return 100;
    }

    @ConfigItem(
        keyName = "customSoundFileName",
        name = "Custom sound alert file",
        description = "Optional WAV file name in this plugin's RuneLite data folder. If it does not exist, the bundled sound is used.",
        position = 11,
        section = alertSettingsSection
    )
    default String customSoundFileName()
    {
        return "alert.wav";
    }

    @ConfigItem(
        keyName = "testSound",
        name = "Test sound alert",
        description = "Toggle this setting to play the current warning sound",
        position = 12,
        section = alertSettingsSection
    )
    default boolean testSound()
    {
        return false;
    }
}
