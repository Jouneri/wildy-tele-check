package com.wildytelecheck;

import com.google.inject.Provides;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Filepath;

@Slf4j
@PluginDescriptor(
    name = "Wildy Tele Check",
    internalName = "wildy_tele_check",
    legacyDataDirectory = "wildy-tele-check",
    description = "Warns when entering the Wilderness without a selected level-30 escape teleport",
    tags = {"wilderness", "teleport", "warning", "pvp", "glory", "seed-pod"}
)
public class WildyTeleCheckPlugin extends Plugin
{
    private static final int OUTSIDE_TICKS_TO_ARM = 3;
    private static final String BUNDLED_SOUND_RESOURCE = "/com/wildytelecheck/default-alert.wav";
    private static final String DEFAULT_CUSTOM_SOUND_FILE = "alert.wav";
    private static final String DEFAULT_WARNING_TEXT = "Dumbass you forgot your teleport!";

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private AudioPlayer audioPlayer;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private WildyTeleCheckConfig config;

    private int consecutiveOutsideTicks;
    private boolean entryArmed;
    private boolean initialized;
    private volatile boolean started;

    @Provides
    WildyTeleCheckConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(WildyTeleCheckConfig.class);
    }

    @Override
    protected void startUp()
    {
        resetTracking();
        started = true;
    }

    @Override
    protected void shutDown()
    {
        started = false;
        resetTracking();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!started || !"wildytelecheck".equals(event.getGroup()))
        {
            return;
        }

        if ("testSound".equals(event.getKey()))
        {
            playAlertSound();
            return;
        }

        if (isAcceptedTeleportSetting(event.getKey()) && noAcceptedTeleportsAfterChange(event))
        {
            clientThread.invokeLater(() ->
            {
                if (client.getGameState() == GameState.LOGGED_IN)
                {
                    client.addChatMessage(
                        ChatMessageType.GAMEMESSAGE,
                        "",
                        "<col=ff0000><b>WILDY TELE CHECK:</b></col> No accepted teleports selected, you will always be warned.",
                        null
                    );
                }
            });
        }
    }

    private boolean isAcceptedTeleportSetting(String key)
    {
        return "allowGlory".equals(key)
            || "allowRoyalSeedPod".equals(key)
            || "allowRingOfWealth".equals(key)
            || "allowCombatBracelet".equals(key)
            || "allowSkillsNecklace".equals(key)
            || "allowSlayerRing".equals(key)
            || "allowEscapeCrystal".equals(key);
    }

    /**
     * ConfigChanged can fire while the settings panel is still applying the
     * changed value. Use event.getNewValue() for the setting that just changed
     * and the config object for the other settings.
     */
    private boolean noAcceptedTeleportsAfterChange(ConfigChanged event)
    {
        final boolean newValue = Boolean.parseBoolean(event.getNewValue());

        final boolean glory = "allowGlory".equals(event.getKey())
            ? newValue : config.allowGlory();
        final boolean seedPod = "allowRoyalSeedPod".equals(event.getKey())
            ? newValue : config.allowRoyalSeedPod();
        final boolean wealth = "allowRingOfWealth".equals(event.getKey())
            ? newValue : config.allowRingOfWealth();
        final boolean combatBracelet = "allowCombatBracelet".equals(event.getKey())
            ? newValue : config.allowCombatBracelet();
        final boolean skillsNecklace = "allowSkillsNecklace".equals(event.getKey())
            ? newValue : config.allowSkillsNecklace();
        final boolean slayerRing = "allowSlayerRing".equals(event.getKey())
            ? newValue : config.allowSlayerRing();
        final boolean escapeCrystal = "allowEscapeCrystal".equals(event.getKey())
            ? newValue : config.allowEscapeCrystal();

        return !glory
            && !seedPod
            && !wealth
            && !combatBracelet
            && !skillsNecklace
            && !slayerRing
            && !escapeCrystal;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        switch (event.getGameState())
        {
            case HOPPING:
            case LOGIN_SCREEN:
            case LOGGING_IN:
            case CONNECTION_LOST:
                resetTracking();
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        final boolean inWilderness = client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1;

        if (!initialized)
        {
            initialized = true;
            consecutiveOutsideTicks = inWilderness ? 0 : OUTSIDE_TICKS_TO_ARM;
            entryArmed = !inWilderness;
            return;
        }

        if (!inWilderness)
        {
            if (consecutiveOutsideTicks < OUTSIDE_TICKS_TO_ARM)
            {
                consecutiveOutsideTicks++;
            }

            if (consecutiveOutsideTicks >= OUTSIDE_TICKS_TO_ARM)
            {
                entryArmed = true;
            }

            return;
        }

        consecutiveOutsideTicks = 0;

        if (entryArmed)
        {
            entryArmed = false;

            if (!hasSelectedEscapeTeleport())
            {
                alert();
            }
        }
    }

    private void resetTracking()
    {
        consecutiveOutsideTicks = 0;
        entryArmed = false;
        initialized = false;
    }

    /**
     * Any one enabled teleport is enough. Both worn equipment and inventory
     * are checked so the escape item may be worn or carried.
     */
    private boolean hasSelectedEscapeTeleport()
    {
        return containerHasSelectedEscapeTeleport(client.getItemContainer(InventoryID.WORN))
            || containerHasSelectedEscapeTeleport(client.getItemContainer(InventoryID.INV));
    }

    private boolean containerHasSelectedEscapeTeleport(ItemContainer container)
    {
        if (container == null)
        {
            return false;
        }

        for (Item item : container.getItems())
        {
            if (item == null || item.getId() <= 0)
            {
                continue;
            }

            final String itemName = client.getItemDefinition(item.getId()).getName();

            if (itemName != null && matchesEnabledTeleport(itemName))
            {
                return true;
            }
        }

        return false;
    }

    private boolean matchesEnabledTeleport(String itemName)
    {
        if (config.allowGlory()
            && (isChargedVariant(itemName, "Amulet of glory")
                || isChargedTrimmedGlory(itemName)))
        {
            return true;
        }

        if (config.allowRoyalSeedPod() && itemName.equalsIgnoreCase("Royal seed pod"))
        {
            return true;
        }

        if (config.allowRingOfWealth()
            && (isChargedVariant(itemName, "Ring of wealth")
                || isChargedImbuedRingOfWealth(itemName)))
        {
            return true;
        }

        if (config.allowCombatBracelet() && isChargedVariant(itemName, "Combat bracelet"))
        {
            return true;
        }

        if (config.allowSkillsNecklace() && isChargedVariant(itemName, "Skills necklace"))
        {
            return true;
        }

        if (config.allowSlayerRing() && isChargedVariant(itemName, "Slayer ring"))
        {
            return true;
        }

        return config.allowEscapeCrystal() && itemName.equalsIgnoreCase("Escape crystal");
    }

    /**
     * Matches a positive numeric charge suffix, with or without a space before
     * the parentheses. Uncharged variants do not match.
     */
    private boolean isChargedVariant(String itemName, String baseName)
    {
        final String normalizedName = itemName.toLowerCase(Locale.ROOT).replace(" ", "");
        final String normalizedBase = baseName.toLowerCase(Locale.ROOT).replace(" ", "");
        final String prefix = normalizedBase + "(";

        if (!normalizedName.startsWith(prefix) || !normalizedName.endsWith(")"))
        {
            return false;
        }

        return hasPositiveNumericSuffix(normalizedName, prefix.length());
    }

    /**
     * Matches charged imbued Rings of wealth: (i1) through (i5). The uncharged
     * Ring of wealth (i) intentionally does not count.
     */
    private boolean isChargedImbuedRingOfWealth(String itemName)
    {
        final String normalized = itemName.toLowerCase(Locale.ROOT).replace(" ", "");
        final String prefix = "ringofwealth(i";

        if (!normalized.startsWith(prefix) || !normalized.endsWith(")"))
        {
            return false;
        }

        return hasPositiveNumericSuffix(normalized, prefix.length());
    }

    /**
     * A charged trimmed glory works exactly like a normal charged glory.
     * Eternal glory is intentionally not accepted.
     */
    private boolean isChargedTrimmedGlory(String itemName)
    {
        final String normalized = itemName.toLowerCase(Locale.ROOT).replace(" ", "");
        final String prefix = "amuletofglory(t";

        if (!normalized.startsWith(prefix) || !normalized.endsWith(")"))
        {
            return false;
        }

        return hasPositiveNumericSuffix(normalized, prefix.length());
    }

    private boolean hasPositiveNumericSuffix(String normalizedName, int suffixStart)
    {
        final String chargeText = normalizedName.substring(suffixStart, normalizedName.length() - 1);

        if (chargeText.isEmpty())
        {
            return false;
        }

        for (int i = 0; i < chargeText.length(); i++)
        {
            if (!Character.isDigit(chargeText.charAt(i)))
            {
                return false;
            }
        }

        try
        {
            return Integer.parseInt(chargeText) > 0;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    private String getWarningText()
    {
        final String configured = config.warningText().trim();
        return configured.isEmpty() ? DEFAULT_WARNING_TEXT : configured;
    }

    private void alert()
    {
        final String warningText = getWarningText();

        if (config.chatWarning())
        {
            client.addChatMessage(
                ChatMessageType.GAMEMESSAGE,
                "",
                "<col=ff0000><b>WILDY TELE CHECK:</b></col> " + warningText,
                null
            );
        }

        if (config.soundAlert())
        {
            playAlertSound();
        }
    }

    /**
     * Queue sound loading and playback away from the client thread because it
     * may perform disk I/O for a custom sound file.
     */
    private void playAlertSound()
    {
        final int volumePercent = Math.max(0, Math.min(100, config.alertVolume()));
        final String configuredFileName = config.customSoundFileName();

        if (volumePercent == 0)
        {
            return;
        }

        executor.execute(() -> playAlertSoundTask(configuredFileName, volumePercent));
    }

    /**
     * Prefer a user-provided sound in this plugin's data directory. If no custom
     * file exists, use the bundled resource.
     */
    private void playAlertSoundTask(String configuredFileName, int volumePercent)
    {
        if (!started)
        {
            return;
        }

        try
        {
            final Filepath soundDirectory = getPluginDirectory();
            final Filepath customSound = getCustomSoundFile(soundDirectory, configuredFileName);
            final float gainDb = volumePercentToGainDb(volumePercent);

            if (customSound != null && customSound.isFile())
            {
                audioPlayer.play(customSound, gainDb);
                return;
            }

            audioPlayer.play(WildyTeleCheckPlugin.class, BUNDLED_SOUND_RESOURCE, gainDb);
        }
        catch (IOException | UnsupportedAudioFileException | LineUnavailableException e)
        {
            log.warn("Failed to play Wildy Tele Check alert sound", e);
        }
    }

    private Filepath getCustomSoundFile(Filepath soundDirectory, String configuredFileName)
    {
        String fileName = configuredFileName == null ? "" : configuredFileName.trim();

        if (fileName.isEmpty())
        {
            fileName = DEFAULT_CUSTOM_SOUND_FILE;
        }

        try
        {
            return soundDirectory.joinSegment(fileName);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    private float volumePercentToGainDb(int volumePercent)
    {
        return (float) (20.0 * Math.log10(volumePercent / 100.0));
    }
}
