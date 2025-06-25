package isl.mapbox.thinbindings.android.internal

import isl.mapbox.thinbindings.android.positions.GeographicPosition

/**
 * This interface is to be supported by MapCamera to enable LocationMarker to callback to MapCamera.
 */
internal interface ILocationMarkerInternalCallbacks
{
    // The callback method for when the location marker has been updated.
    fun LocationMarkerInternalCallback(newPosition: GeographicPosition, newText: String = "", newBearing: Double = 0.0, newSpeed: Double = 0.0)
}
