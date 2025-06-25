package isl.mapbox.thinbindings.android.features

import isl.mapbox.thinbindings.android.positions.LongLatExtent

/**
 * A LineMapFeature will be used for representing road and route segments.
 */
public abstract class LineMapFeature : MapFeature()
{
    /**
     * Latitude and longitude coordinates that describe the shape of the road segment, represented in GeoJSON format.
     */
    public var ShapeGeoJSON: String = ""

    /**
     * The LongLatExtent that all the points of the shape fit within.
     */
    public var BoundingBox: LongLatExtent? = null // ???

    /**
     * Normalised length in meters
     */
    public var Length: Double = 0.0;
}