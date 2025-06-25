package isl.mapbox.thinbindings.android.features

/**
 * POI such as a Depoy, Tip or Waste Transfer Station.
 */
public class PointOfInterestFeature : SymbolMapFeature()
{
    /**
     * This indicates if a comment is present due to PointOfInterest.Note having a value;
     */
    public var Comments: Boolean = false;

    /**
     * A note attached to this Point of Interest
     */
    public var Note: String = "";
}
