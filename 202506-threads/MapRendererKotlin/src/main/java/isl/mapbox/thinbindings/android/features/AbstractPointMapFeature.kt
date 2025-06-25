package isl.mapbox.thinbindings.android.features

/**
 * Abstract definition of a generic point feature on the map
 */
public abstract class AbstractPointMapFeature : MapFeature()
{
    //public ISL.DataTypes.Geography.LongLatPoint Location;
    //We will implement as Lat and Long initially but before we can implement shape in LineMapFeature we will need to think about whether to represent this using GeoJSON???
    public var Longitude: Double = 0.0;
    public var Latitude: Double = 0.0;

    /**
     * If the feature point is to be used to render for trade purposes eg a trade site, or the blue route sysmbol.
     * Q? - JP should this be in the abstract class? Seems like it ought to live in a derived class.
     */
    public var IsTrade: Boolean = false;
}