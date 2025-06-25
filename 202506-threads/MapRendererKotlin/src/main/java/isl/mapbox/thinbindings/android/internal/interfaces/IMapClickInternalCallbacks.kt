package isl.mapbox.thinbindings.android.internal

import isl.mapbox.thinbindings.android.positions.MapPosition

// This interface is to be supported by MapRendererKotlin/other class to enable MapClickListener to callback to MapRendererKotlin/other class.
internal interface IMapClickInternalCallbacks
{
    // The callback method for when a click has occurred.
    fun MapClickInternalCallback(clickDetected: MapPosition);

    // The callback method for when a long click has occurred.
    fun MapLongClickInternalCallback(longClickDetected: MapPosition);
}

