package isl.mapbox.thinbindings.android.maprendererkotlin

import isl.mapbox.thinbindings.android.maprendererkotlin.IMapCallback
import isl.mapbox.thinbindings.android.maprendererkotlin.map.IFeatureSelection
import isl.mapbox.thinbindings.android.maprendererkotlin.map.ILocationMarker
import isl.mapbox.thinbindings.android.maprendererkotlin.map.IMapCamera
import isl.mapbox.thinbindings.android.maprendererkotlin.map.IMapStyle

/**
 * This will be used by the MapRenderer in the C# portion of the ThinBindings
  */
interface IMapRendererKotlin
{
    /**
     * The Map Style object.
     */
    val mapStyle: IMapStyle

    /**
     * The Location Marker object.
     */
    val locationMarker: ILocationMarker;

    /**
     * The Map Camera object.
     */
    val mapCamera: IMapCamera;

    /**
     * The Feature Selection object.
     */
    val featureSelection: IFeatureSelection;

    /**
     * This should be called when the MapRenderer is started (usually in the OnElementChanged method).
     */
    fun StartInstance()

    /**
     * This should be called when the MapRenderer is disposed of.
     */
    fun DisposeOfInstance()

    /**
     * This should be called when the parents Activity or Fragment OnLowMemory is called.
     */
    fun OnLowMemory()

    /**
     * This should be called by MapRenderer to register for callbacks from MapRendererKotlin components for the purpose of logging.
     */
    fun RegisterForLoggingCallback(loggingHandler: ILoggingCallback)

    /**
     * This should be called by MapRenderer to register for callbacks from MapRendererKotlin components for when a map gesture has been performed by the operator.
     */
    fun RegisterForGestureCallback(gestureHandler: IGestureCallback);

    /**
     * This should be called by MapRenderer to register for callbacks from MapRendererKotlin components for when a map camera updates have occurred.
     */
    fun RegisterForMapCameraCallback(mapCameraHandler: IMapCameraCallback);

    /**
     * This should be called by MapRenderer to register for callbacks from MapRendererKotlin components for when feature selection has been performed by the operator.
     */
    fun RegisterForFeatureSelectionCallback(featureSelectionHandler: IFeatureSelectionCallback);

    /**
     * This should be called by MapRenderer to register for callback from MapRendererKotlin components for when map is loaded, idle or style loaded.
     */
    fun RegisterForMapCallback(mapHandler: IMapCallback);

    /**
     * This determines if metric or imperial units are used, default is metric.
     */
    var metricUnits: Boolean;
}
