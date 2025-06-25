package isl.mapbox.thinbindings.android.internal

import com.mapbox.common.LogConfiguration
import com.mapbox.common.LogWriterBackend
import com.mapbox.common.LoggingLevel

public class MapboxLogListener : LogWriterBackend
{

    init
    {
        LogConfiguration.registerLogWriterBackend(this);
        LogConfiguration.setLoggingLevel(LoggingLevel.INFO);
    }

    override fun writeLog(level: LoggingLevel, message: String)
    {
        var updatedLevel: LoggingLevel = level;
        if (message.startsWith("Invalid size is used for setting the map view, fall back to the default size{64, 64}") == true)
        {
            updatedLevel = LoggingLevel.INFO;
        }

        if (message.endsWith("is not implemented.")) return;

        when (updatedLevel)
        {
            LoggingLevel.DEBUG -> Logging.D(message);
            LoggingLevel.INFO -> Logging.I(message);
            LoggingLevel.WARNING -> Logging.W(message);
            LoggingLevel.ERROR -> Logging.E(message);
        }
    }

}