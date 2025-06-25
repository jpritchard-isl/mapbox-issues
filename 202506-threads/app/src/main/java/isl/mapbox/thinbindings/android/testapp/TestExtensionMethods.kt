package isl.mapbox.thinbindings.android.testapp

import isl.mapbox.thinbindings.android.features.AbstractPointMapFeature
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.LongLatExtent

const val longitudeNominalScale: Double = 0.0000875;
const val latitudeNominalScale: Double = 0.00005;

const val defaultMapKeyLongitude: Double = -0.8921660393774573; // Pera Office in Melton Mowbray - Leicestershire
const val defaultMapKeyLatitude: Double = 52.770348980486375; // Pera Office in Melton Mowbray - Leicestershire


public fun AbstractPointMapFeature.SetGeometry(lineXIndex: Int, lineYIndex: Int) : AbstractPointMapFeature
{
    var latitude: Double = defaultMapKeyLatitude + (latitudeNominalScale * lineYIndex);
    var longitude: Double = defaultMapKeyLongitude + (longitudeNominalScale * lineXIndex);
    this.Longitude = longitude;
    this.Latitude = latitude;

    return this;
}

public fun LongLatExtent.CalculateBoundingBox(additionalPoint: GeographicPosition): LongLatExtent
{
    this.SouthWest = GeographicPosition(Longitude = Math.min(this.SouthWest.Longitude, additionalPoint.Longitude), Latitude = Math.min(this.SouthWest.Latitude, additionalPoint.Latitude));
    this.NorthEast = GeographicPosition(Longitude = Math.max(this.NorthEast.Longitude, additionalPoint.Longitude), Latitude = Math.max(this.NorthEast.Latitude, additionalPoint.Latitude));
    return this;
}
