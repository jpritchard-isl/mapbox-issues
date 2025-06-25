package isl.mapbox.thinbindings.android.internal

import isl.mapbox.thinbindings.android.maprendererkotlin.ILoggingCallback
import isl.mapbox.thinbindings.android.maprendererkotlin.LogLevel

public class Logging {

    internal companion object {
        private val TAG = "MapRendererKotlin"
        private lateinit var cSharpLogHandler: ILoggingCallback

        internal fun Log(level: LogLevel, message: String, levelsUpToGetContext: Int = 2) {
            val stackTrace = Throwable().stackTrace[levelsUpToGetContext]
            val filename = stackTrace.fileName
            val classname = stackTrace.className.splitToSequence(".").last()
            val method = stackTrace.methodName
            val linenumber = stackTrace.lineNumber
            val threadid = Thread.currentThread().id
            val threadname = Thread.currentThread().name

            if (Companion::cSharpLogHandler.isInitialized)
            {
                cSharpLogHandler.LogFromMapRendererKotlin(level, filename, classname, method, linenumber, message, threadname, threadid)
            }
            else
            {
                val fullmessage = "${classname} ${linenumber} ${method} (${threadname}:${threadid}): ${message}"
                when (level)
                {
                    LogLevel.Verbose -> android.util.Log.v(TAG, fullmessage)
                    LogLevel.Debug -> android.util.Log.d(TAG, fullmessage)
                    LogLevel.Info -> android.util.Log.i(TAG, fullmessage)
                    LogLevel.Warning -> android.util.Log.w(TAG, fullmessage)
                    LogLevel.Error -> android.util.Log.e(TAG, fullmessage)
                }
            }

        }

        internal fun V(message: String)
        {
            Log(LogLevel.Verbose, message, 2)
        }

        internal fun D(message: String)
        {
            Log(LogLevel.Debug, message, 2)
        }

        internal fun I(message: String)
        {
            Log(LogLevel.Info, message, 2)
        }

        internal fun W(message: String)
        {
            Log(LogLevel.Warning, message, 2)
        }

        internal fun E(message: String)
        {
            Log(LogLevel.Error, message, 2)
        }

        internal fun RegisterCallback(handler: ILoggingCallback)
        {
            cSharpLogHandler = handler
        }
    }
}