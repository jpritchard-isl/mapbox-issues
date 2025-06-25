package isl.mapbox.thinbindings.android.maprendererkotlin.map

import isl.mapbox.thinbindings.android.maprendererkotlin.map.LocationMarkerType
import isl.mapbox.thinbindings.android.positions.GeographicPosition

/**
 * The Location Marker object.
 */
interface ILocationMarker
{
    /**
     * This provides indication of whether the location marker is visible.
     */
    val locationMarkerVisible: Boolean

    /**
     * This provides indication of the last known location marker position.
     */
    val lastLocationMarkerPosition: GeographicPosition?

    /**
     * This provides indication of the last known location marker bearing.
     */
    val lastLocationMarkerBearing: Double?

    /**
     * This sets the initial or subsequent location marker position. Note that if location marker type is 'LocationWithDirection' and newSpeed is > 0.5 then the direction arrow will be shown pointing according to the newBearing.
     */
    fun SetOrChangeLocationMarkerPosition(newPosition: GeographicPosition, newText: String = "", newBearing: Double = 0.0, newSpeed: Double = 0.0);

    /**
     * This determines whether the location marker is shown, and if it is - whether the text should also be shown.
     *
     * @param show Display the location marker
     * @param includeText Display label
     */
    fun ShowLocationMarker(show: Boolean, includeText: Boolean = false);

    /**
     * This sets the symbol used to indicate the location.
     *
     * @param locationMarkerType
     */
    fun SetLocationMarkerType(locationMarkerType: LocationMarkerType);

    /**
     * This determines whether the crimb trail is left behind the location marker and how long the trail is. The inclusion of text can be independent of whether text is shown for the location marker.
     *
     * @param show Display the crumbs
     * @param numberOfCrumbs
     * @param includeText Label the crumbs
     */
    fun ShowLocationMarkerCrumbTrail(show: Boolean, numberOfCrumbs: Int = 10, includeText: Boolean = false);
}