package isl.mapbox.thinbindings.android.maprendererkotlin.map

import isl.mapbox.thinbindings.android.features.MapFeature
import isl.mapbox.thinbindings.android.maprendererkotlin.map.ExtentType
import isl.mapbox.thinbindings.android.maprendererkotlin.map.MapCamera
import isl.mapbox.thinbindings.android.positions.MapCameraExtent
import isl.mapbox.thinbindings.android.positions.MapCameraPosition
import isl.mapbox.thinbindings.android.positions.MapPadding

/**
 * The Map Camera interface.
 */
interface IMapCamera
{
    /**
     * Gets the maximum zoom level used in extent modes.
     */
    fun GetMaxZoom(): Double

    /**
     * Sets the maximum zoom level used in extent modes.
     */
    fun SetMaxZoom(newMaxZoom: Double)

    /**
     * Gets the default zoom used in Centre-on-location extent mode.
     */
    fun GetDefaultZoom(): Double

    /**
     * Sets the default zoom used in Centre-on-location extent mode.
     */
    fun SetDefaultZoom(newDefaultZoom: Double)

    /**
     * Gets the default padding included around extents.
     */
    fun GetDefaultExtentPadding(): Double

    /**
     * Sets the default padding included around extents.
     */
    fun SetDefaultExtentPadding(newDefaultExtentPadding: Double)

    /**
     * Sets the position of the Map Camera
     *
     * @param mapCameraPosition New map camera position
     * @param adjustOffsetForSpeed When true, adjust the camera position when travelling at speed.
     */
    fun SetMapCameraPosition(mapCameraPosition: MapCameraPosition, adjustOffsetForSpeed: Boolean? = false)

    /**
     * Gets the position of the Map Camera
     */
    fun GetMapCameraPosition(): MapCameraPosition

    /**
     * Sets the extent (where this is determined by the client software).
     */
    fun SetMapCameraExtent(mapCameraExtent: MapCameraExtent)

    /**
     * Sets the whether the map should orientate to the given bearing (or to the location marker bearing when in extent modes).
     */
    fun SetOrientateToBearing(orientate: Boolean = false)

    /**
     * Gets whether the map should orientate to bearing.
     */
    fun GetOrientateToBearing(): Boolean

    /**
     * Sets additional padding around the map (for instance to shift the content when an overlay is to be superimposed on the map).
     */
    fun SetMapCameraPaddingInPixels(newMapPadding: MapPadding)

    /**
     * Get the current map padding in pixels.
     */
    fun GetMapPaddingInPixels(): MapPadding

    /**
     * Sets the extent mode used (where the extent itself will be calculated automatically).
     */
    fun SetExtentType(newExtentType: ExtentType, initialMoveType: MoveCameraTypeEnum? = null)

    /**
     * Gets the extent mode used.
     */
    fun GetExtentType(): ExtentType

    /**
     * Sets the list of features that are to be used for the two targeted extent modes.
     */
    fun SetTargetedExtentInclusions(features: List<MapFeature>, initialMoveType: MoveCameraTypeEnum? = null)

    /**
     * Gets the list of features that are to used for the two targeted extent modes.
     */
    fun GetTargetedExtentInclusions(): List<MapFeature>;

    /**
     * Sets whether the path from current location to the targeted feature should be shown if in 'targeted extent' or 'targeted extent and location' modes (default is true).
     */
    fun SetShowPathInTargetedExtentMode(showPath: Boolean = false)

    /**
     * Gets whether the path from current location to the targeted feature should be shown if in 'targeted extent' or 'targeted extent and location' modes (default is true).
     */
    fun GetShowPathInTargetedExtentMode(): Boolean

    /**
     * Sets whether the path from current location to the targeted feature should be shown (default is false).
     */
    fun SetShowPathToTargetFeature(showPath: Boolean = false)

    /**
     * Gets whether the path from current location to the targeted feature should be shown (default is false).
     */
    fun GetShowPathToTargetFeature(): Boolean

    /**
     * Sets whether jobs are included in the calculation of the FullExtent and FullExtentAndLocation.
     */
    fun SetIncludeJobsInFullExtent(include: Boolean = false)

    /**
     * Gets whether jobs are included in the calculation of the FullExtent and FullExtentAndLocation.
     */
    fun GetIncludeJobsInFullExtent(): Boolean

    /**
     * The proportion of the screen over which the user must pan in order for this to be detected as movement, which will change the extent mode to 'None'.
     * The default value is 1/30.
     */
    fun SetMovementDetectionThreshold(threshold: Double, useFling: Boolean = false)

    /**
     * Sets whether non-service segments are included in the calculation of the FullExtent and FullExtentAndLocation.
     */
    fun SetIncludeNonServiceSegmentsInFullExtent(include: Boolean = true)

    /**
     * Gets whether non-service segments are included in the calculation of the FullExtent and FullExtentAndLocation.
     */
    fun GetIncludeNonServiceSegmentsInFullExtent(): Boolean

    /**
     * Sets whether service points are included in the calculation of the FullExtent and FullExtentAndLocation.
     */
    fun SetIncludeServicePointsInFullExtent(include: Boolean = true)

    /**
     * Gets whether service points are included in the calculation of the FullExtent and FullExtentAndLocation.
     */
    fun GetIncludeServicePointsInFullExtent(): Boolean

    /**
     * Configure the auto offsetting of camera by speed.
     * @param speedForMaxOffset Speed at which the position will be maximally offset
     * @param maxOffsetAtMaxSpeed The offset reached at max speed.
     * @param filterCoeff Filter coefficient to smooth the offsetting.
     */
    fun ConfigureMarkerOffsetDueToSpeed(
        speedForMaxOffset: Double,
        maxOffsetAtMaxSpeed: Double,
        filterCoeff: Double
    )
}
