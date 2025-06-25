package isl.mapbox.thinbindings.android.maprendererkotlin

import isl.mapbox.thinbindings.android.maprendererkotlin.map.ExtentType
import isl.mapbox.thinbindings.android.positions.MapCameraPosition

/**
 * This should be supported by MapRenderer in the C# portion of ThinBindings so that map camera data can be passed back to there.
 */
interface IMapCameraCallback
{
    /**
     * This will be called when the map extent has been changed by the maprendererkotlin (rather than by the client software).
     */
    fun MapCameraExtentUpdateFromMapRendererKotlin(newExtent: ExtentType);

    /**
     * This will be called when the map camera position has been changed by the maprendererkotlin due to extent change or location update (rather than by the map gestures).
     */
    fun MapCameraPositionUpdateFromMapRendererKotlin(newMapCameraPosition: MapCameraPosition);
}

