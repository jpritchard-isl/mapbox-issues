package isl.mapbox.thinbindings.android.internal

import com.mapbox.maps.Style

/**
 * This interface is to be supported by classes wanting callback from MapStyleListener.
 */
internal interface IMapStyleInternalCallbacks
{
    /**
     * The callback method for when a style has been loaded.
     */
    fun MapStyleInternalCallback(theMapStyle: Style)
}

