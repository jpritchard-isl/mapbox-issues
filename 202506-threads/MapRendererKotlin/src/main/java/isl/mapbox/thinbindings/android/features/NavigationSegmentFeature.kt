package isl.mapbox.thinbindings.android.features

/**
 * A NavigationSegmentFeature will appear on the map where the path of an A2B navigation needs to be shown.
 * There will usually be multiple of these to show the whole navigation path.
 * The appearance will be different for segments that have been completed.
 */
public open class NavigationSegmentFeature : RouteRoadSegmentNonServiceFeature()
{
    /**
     * Whether the segment has been traversed as part of A2B navigation.
     */
    public var Completed: Boolean = false
}