package isl.mapbox.thinbindings.android.maprendererkotlin

import isl.mapbox.thinbindings.android.maprendererkotlin.LogLevel

/**
 * This should be supported by the MapRenderer in the C# portion of ThinBindings so that logging can be passed back to there for logging via Serilog.
 */
interface ILoggingCallback
{
    /**
     * Once registered this method will be called from MapRendererKotlin components.
     */
    fun LogFromMapRendererKotlin(level: LogLevel, filename: String, classname: String, method: String, linenumber: Int, message: String, threadname: String, threadid: Long)
}

