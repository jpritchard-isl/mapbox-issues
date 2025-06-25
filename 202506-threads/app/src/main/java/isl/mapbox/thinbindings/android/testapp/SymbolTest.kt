package isl.mapbox.thinbindings.android.testapp

import isl.mapbox.thinbindings.android.features.AdhocFeature
import isl.mapbox.thinbindings.android.features.PointOfInterestFeature
import isl.mapbox.thinbindings.android.features.SymbolMapFeature
import isl.mapbox.thinbindings.android.features.SymbolType
import isl.mapbox.thinbindings.android.maprendererkotlin.MapRendererKotlin
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.LongLatExtent
import java.util.UUID

public class SymbolTest(theMap: MapRendererKotlin, testExtent: LongLatExtent)
{
    private val TAG = "TestApp";
    private val theMap: MapRendererKotlin = theMap
    private var testExtent = testExtent;

    public fun AddTestSymbol(linePositionXIndex: Int, linePositionYIndex: Int, symbolType: SymbolType, text: String = "", UniqueId: UUID): SymbolMapFeature
    {
        var symbolFeatures = mutableListOf<SymbolMapFeature>();

        var symbolFeature = SymbolMapFeature();
        if (symbolType == SymbolType.Adhoc)
        {
            symbolFeature = AdhocFeature();
        }
        else if (symbolType == SymbolType.PoI)
        {
            symbolFeature = PointOfInterestFeature();
        }
        symbolFeature.UniqueGuid = UniqueId;
        symbolFeature.Identity = ""; // Was a Guid in Mobile???
        symbolFeature.Text = text;
        symbolFeature.Symbol = symbolType;


        symbolFeature = symbolFeature.SetGeometry(linePositionXIndex, linePositionYIndex) as SymbolMapFeature;
        symbolFeatures.add(symbolFeature);

        testExtent = testExtent.CalculateBoundingBox(GeographicPosition(symbolFeature.Longitude, symbolFeature.Latitude));

        theMap.mapStyle.symbolsLayer.AddSymbols(symbolFeatures);

        return symbolFeature;
    }

    public fun MoveTestSymbol(linePositionXIndex: Int, linePositionYIndex: Int, UniqueId: UUID, newBearing: Double = 0.0)
    {
        var tempSymbolFeature = SymbolMapFeature();
        tempSymbolFeature = tempSymbolFeature.SetGeometry(linePositionXIndex, linePositionYIndex) as SymbolMapFeature;

        theMap.mapStyle.symbolsLayer.ChangeSymbolPosition(UniqueId, GeographicPosition(tempSymbolFeature.Longitude, tempSymbolFeature.Latitude), newBearing = newBearing);
    }
}