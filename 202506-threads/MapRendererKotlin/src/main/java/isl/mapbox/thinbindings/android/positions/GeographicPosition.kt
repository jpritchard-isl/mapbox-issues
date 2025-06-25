package isl.mapbox.thinbindings.android.positions

// This is a duplicate of ISL.DataTypes.Geography.LongLatPoint
// Longitude: East/West Longitude in Decimal Degrees
// Latitude: North/South Latitude in Decimal Degrees
public class GeographicPosition (val Longitude: Double, val Latitude: Double) {}

// This is a duplicate of ISL.DataTypes.Geography.LongLatExtent
// Care should be taken when using an extent crossing -180/180 degrees east/west, or at the poles -90/90 degrees.
// NorthEast: North East corner of the extent
// SouthWest: South West corner of the extent
public class LongLatExtent (var NorthEast: GeographicPosition, var SouthWest: GeographicPosition) {}