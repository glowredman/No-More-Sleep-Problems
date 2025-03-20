package glowredman.nmsp;

import java.util.Calendar;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    acceptableRemoteVersions = "*",
    acceptedMinecraftVersions = "[1.7.10]",
    modid = "nmsp",
    name = "No More Sleep Problems",
    version = Tags.VERSION)
public class NMSP {

    private static final Logger LOGGER = LogManager.getLogger("nmsp");
    private static final Calendar CALENDAR = Calendar.getInstance();

    static float requiredPlayers;
    static double yLevelMiners;
    static long checkInterval;
    static String enterBedMessage;
    static String leaveBedMessage;
    private static String skipNightMessage;
    private static final Table<Integer, Integer, String> SPECIAL_MESSAGES = HashBasedTable.create();

    @EventHandler
    public static void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());

        requiredPlayers = config.getFloat(
            "requiredPlayers",
            Configuration.CATEGORY_GENERAL,
            0.5f,
            0.0f,
            1.0f,
            "Percentage of players in the overworld (excluding miners) which is required to skip the night");
        yLevelMiners = config.getInt(
            "yLevelMiners",
            Configuration.CATEGORY_GENERAL,
            64,
            0,
            255,
            "Max Y-coordinate a player is allowed to be at to be counted as miner");
        checkInterval = config.getInt(
            "checkInterval",
            Configuration.CATEGORY_GENERAL,
            20,
            1,
            Integer.MAX_VALUE,
            "How often (in ticks) should be checked if the night can be skipped");
        enterBedMessage = config.getString(
            "enterBedMessage",
            Configuration.CATEGORY_GENERAL,
            "§f%1$s§6 went to bed. %2$s/%3$s (%4$.0f%%)",
            """
                What text to display when a player goes to bed - leave empty to disable
                Placeholders:
                 %1$s - player name
                 %2$s - number of sleeping players
                 %3$s - number of total players
                 %4$f - percentage of players sleeping
                 %% - a single '%' character
                More details about Java formatting can be found here: https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html#syntax
                """);
        leaveBedMessage = config.getString(
            "leaveBedMessage",
            Configuration.CATEGORY_GENERAL,
            "§f%1$s§6 left their bed. %2$s/%3$s (%4$.0f%%)",
            """
                What text to display when a player leaves the bed - leave empty to disable
                Placeholders:
                 %1$s - player name
                 %2$s - number of sleeping players
                 %3$s - number of total players
                 %4$f - percentage of players sleeping
                 %% - a single '%' character
                More details about Java formatting can be found here: https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html#syntax
                """);
        skipNightMessage = config.getString(
            "skipNightMessage",
            Configuration.CATEGORY_GENERAL,
            "§6A new day begins. Good morning everyone!",
            "What text to display when the night is skipped - leave empty to disable");
        parseSpecialMessages(
            config.getStringList(
                "specialSkipNightMessages",
                Configuration.CATEGORY_GENERAL,
                new String[] { "01-01:§6Good morning everyone! HAPPY NEW YEAR!",
                    "10-31:§5Good morning everyone! HAPPY HALLOWEEN!",
                    "12-25:§cGood morning everyone! HAPPY CHRISTMAS!" },
                "Skip night messages for specific days in the year - format: MM-DD:T (MM: month, DD: day, T: text to use)"));

        if (config.hasChanged()) {
            config.save();
        }

        FMLCommonHandler.instance()
            .bus()
            .register(new NMSPEventHandler());
    }

    private static void parseSpecialMessages(String[] lines) {
        for (String line : lines) {
            String[] lineParts = line.split(":", 2);
            if (lineParts.length != 2) {
                LOGGER.warn(
                    "Config error: line '{}' of property specialSkipNightMessages is not formatted correctly ({})",
                    line,
                    line.isEmpty() ? "empty" : "':' missing");
                continue;
            }
            String[] dateParts = lineParts[0].split("-", 2);
            if (dateParts.length != 2) {
                LOGGER.warn(
                    "Config error: line '{}' of property specialSkipNightMessages is not formatted correctly ({})",
                    lineParts[0],
                    lineParts[0].isEmpty() ? "nothing infront of ':'" : "'-' missing");
                continue;
            }
            int month;
            try {
                month = Integer.parseInt(dateParts[0]);
            } catch (NumberFormatException e) {
                LOGGER.warn("Config error: '{}' is not a valid number", dateParts[0]);
                continue;
            }
            if (month < 1 || month > 12) {
                LOGGER.warn("Config error: '{}' is not a valid month", dateParts[0]);
                continue;
            }
            int day;
            try {
                day = Integer.parseInt(dateParts[1]);
            } catch (NumberFormatException e) {
                LOGGER.warn("Config error: '{}' is not a valid number", dateParts[1]);
                continue;
            }
            if (day < 1 || day > 31) {
                LOGGER.warn("Config error: '{}' is not a valid day", dateParts[1]);
                continue;
            }
            SPECIAL_MESSAGES.put(month - 1, day, lineParts[1]);
        }
    }

    static String getSkipNightMessage() {
        int month = CALENDAR.get(Calendar.MONTH);
        int day = CALENDAR.get(Calendar.DAY_OF_MONTH);
        String specialMessage = SPECIAL_MESSAGES.get(month, day);
        return specialMessage == null ? skipNightMessage : specialMessage;
    }
}
