# Wildy Tele Check

A RuneLite plugin that warns you when you enter the Wilderness without a selected level-30 escape teleport.

## Supported teleports

Wildy Tele Check can recognise:

- Amulet of glory
- Royal seed pod
- Ring of wealth
- Combat bracelet
- Skills necklace
- Slayer ring
- Escape crystal

Any one enabled teleport is enough.

Charged versions of jewellery are recognised automatically. Uncharged jewellery does not count.

The plugin checks both your inventory and equipped items.

## How it works

When you enter the Wilderness, Wildy Tele Check checks whether you have at least one of your enabled escape teleports.

If none are found, the plugin:

- Plays the warning sound
- Displays your warning text in game chat

The default warning text is:

`You forgot your teleport!`

All supported teleport options are enabled by default.

## Sound

Wildy Tele Check includes a built-in warning sound, so no setup is required.

You can also use your own WAV file.

Place it here:

`%USERPROFILE%\.runelite\wildy-tele-check\alert.wav`

If a custom sound is present, it replaces the built-in sound.

The alert volume can be adjusted in the plugin settings.

## Settings

You can:

- Enable or disable individual teleport methods
- Change the alert volume
- Test the alert sound
- Change the warning text
- Enable or disable the in-game chat warning
- Use a custom WAV alert sound

## Development

Run the plugin in RuneLite developer mode with:

`.\gradlew.bat clean run`