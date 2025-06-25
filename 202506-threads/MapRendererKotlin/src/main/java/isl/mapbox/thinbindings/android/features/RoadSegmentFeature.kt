package isl.mapbox.thinbindings.android.features

import java.util.UUID

/**
 * A RoadSegmentFeature is included on the map not because it is part of a route but because there is a note that needs to be brought to the attention of the operator.
 * The rendering of the segment will indicate the presence of a note.
 */
public open class RoadSegmentFeature : LineMapFeature()
{
    /**
     * The Name obtained from the RoadSegment.
     */
    public var Name: String = ""

    /**
     * The UniqueGuid taken from the RoadSegment, note that the RoadSegment.UniqueGuid will be used for UniqueGuid of this feature.
     */
    public lateinit var RoadGuid: UUID

    /**
     * The Note obtained from the RoadSegment.
     */
    public var Note: String = ""

    /**
     * This indicates if a comment is present due to either the RoadSegment.Note or RouteSegment.ServiceNote having a value.
     */
    public var Comments: Boolean = false

    /**
     * Annotation required on the map. (probably only for debugging purposes?
     */
    public var Text: String = ""
}