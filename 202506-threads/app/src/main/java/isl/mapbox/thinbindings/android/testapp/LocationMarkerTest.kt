package isl.mapbox.thinbindings.android.testapp

import isl.mapbox.thinbindings.android.features.SymbolMapFeature
import isl.mapbox.thinbindings.android.maprendererkotlin.MapRendererKotlin
import isl.mapbox.thinbindings.android.maprendererkotlin.map.LocationMarkerType
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.LongLatExtent

public class LocationMarkerTest(theMap: MapRendererKotlin)
{
    private val TAG = "TestApp";
    private val theMap: MapRendererKotlin = theMap
    private var testExtent = LongLatExtent(GeographicPosition(Double.MIN_VALUE, Double.MIN_VALUE), GeographicPosition(Double.MAX_VALUE, Double.MAX_VALUE));

    public fun AddTestLocationMarker(linePositionXIndex: Int, linePositionYIndex: Int, text: String = "", includeText: Boolean = false)
    {

        theMap.locationMarker.ShowLocationMarker(true, includeText);
        SetOrChangeLocationMarkerPosition(linePositionXIndex, linePositionYIndex, text);
        theMap.locationMarker.SetLocationMarkerType(LocationMarkerType.LocationWithDirection);
        //theMap.locationMarker.ShowLocationMarker(true, includeText);
    }

    public fun SetOrChangeLocationMarkerPosition(linePositionXIndex: Int, linePositionYIndex: Int, newText: String = "", newBearing: Double = 0.0, newSpeed: Double = 0.0)
    {
        var tempSymbolFeature = SymbolMapFeature();
        tempSymbolFeature = tempSymbolFeature.SetGeometry(linePositionXIndex, linePositionYIndex) as SymbolMapFeature;

        theMap.locationMarker.SetOrChangeLocationMarkerPosition(GeographicPosition(tempSymbolFeature.Longitude, tempSymbolFeature.Latitude), newText, newBearing, newSpeed);
    }

}