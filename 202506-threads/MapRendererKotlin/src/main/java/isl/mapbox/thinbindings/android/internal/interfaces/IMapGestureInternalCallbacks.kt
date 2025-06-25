package isl.mapbox.thinbindings.android.internal

// This interface is to be supported by MapRendererKotlin/other class to enable MapGestureListener to callback to MapRendererKotlin/other class.
internal interface IMapGestureInternalCallbacks
{
    // The callback method for when a gesture has occurred.
    fun MapGestureInternalCallback(moveDetected: Boolean, rotateDetected: Boolean, shoveDetected: Boolean, flingDetected: Boolean, scaleDetected: Boolean)
}

