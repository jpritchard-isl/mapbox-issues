package isl.mapbox.thinbindings.android.maprendererkotlin

/**
 * This should be supported by MapRenderer in the C# portion of ThinBindings so that map gestures can be passed back to there.
 */
interface IGestureCallback
{
    /**
     * Once registered this method will be called from MapRendererKotlin components to indicate  gesture has been done to the map.
     */
    fun GestureFromMapRendererKotlin(moveDetected: Boolean, rotateDetected: Boolean, shoveDetected: Boolean, flingDetected: Boolean, scaleDetected: Boolean);
}

