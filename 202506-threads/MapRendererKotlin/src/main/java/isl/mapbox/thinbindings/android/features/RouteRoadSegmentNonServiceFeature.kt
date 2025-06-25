package isl.mapbox.thinbindings.android.features

import java.util.UUID

/**
 * A RouteRoadSegmentNonServiceFeature may be part of a route but will have no service points associated with it.
 */
open class RouteRoadSegmentNonServiceFeature : RoadSegmentFeature()
{
    /**
     * Numeric order this road segment should be visited in.
     */
    public var Sequence: Long = 0

    /**
     * The planned direction of travel along the RoadSegment topological direction.  May only be With or Against, must never be Both., obtained from the RouteSegment
     */
    public lateinit var RouteDirectionOfTravel: DirectionOfTravel

    /**
     * The description of the bearing (e.g. NorthWest).
     */
    public var BearingDescription: String = ""

    /**
     * The ServiceNote obtained from the RouteSegment.
     */
    public var ServiceNote: String = ""

    /**
     * This indicates that a report has been raised for this feature.
     */
    public var Reported: Boolean = false

    /**
     * The UniqueGuid taken from the RouteSegment. This will be also be used for UniqueGuid.
     */
    public lateinit var RouteGuid: UUID

    /**
     * The manoeuvre at the end of this route segment if travelled in the planned direction
     * of service, to move onto the next route segment.
     */
    public lateinit var ManoeuvreAtEnd: ManoeuvreType

}