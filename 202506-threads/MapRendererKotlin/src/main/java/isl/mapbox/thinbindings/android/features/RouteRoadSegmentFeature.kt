package isl.mapbox.thinbindings.android.features

/**
 * A RouteRoadSegmentFeature is a serviceable segment (i.e. it has service points associated with it)
 * It is based on a combination of a RoadSegment and a RouteSegment.
 */
class RouteRoadSegmentFeature : RouteRoadSegmentNonServiceFeature()
{
    /**
     * This indicates the serviced status for this feature.
     */
    //public lateinit var Serviced: RoadSegmentServiced
    public var Serviced: Boolean = false

    /**
     * The number of passes that have been completed on the Road Segment this route segment over.
     */
    public var PassesCompleted: Int = 0

    /**
     * Total number of passes expected on the Road Segment this route segment over.
     */
    public var PassesTotal: Int = 0

    /**
     * The ServiceNote obtained from the RouteSegment.
     */
//    public var ServiceNote: String = ""

    /**
     * The direction of travel along the RoadSegment topological direction in which the segment is allowed to be serviced., obtained from the RouteSegment
     */
    public lateinit var DirectionOfService: DirectionsOfService
}

/*
public enum class RoadSegmentServiced
{
    Unserviced, // = 0,
    PartiallyServiced,
    Serviced
}
*/

/**
 *
 */
public enum class RoadSegmentFeatureType
{
    Road,
    RouteRoadNonService,
    RouteRoad,
    A2BNavigation,
}
