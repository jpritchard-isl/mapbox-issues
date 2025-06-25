package isl.mapbox.thinbindings.android.maprendererkotlin.map

import isl.mapbox.thinbindings.android.features.SymbolMapFeature
import isl.mapbox.thinbindings.android.features.SymbolType
import isl.mapbox.thinbindings.android.internal.ILocationMarkerInternalCallbacks
import isl.mapbox.thinbindings.android.internal.Logging
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.SymbolPropertyBase
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.SymbolSymbolTypeProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.SymbolsLayer
import isl.mapbox.thinbindings.android.maprendererkotlin.misc.ConditionalCompile
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import java.util.UUID

public class LocationMarker(symbolsLayer: SymbolsLayer) : ILocationMarker {
    private val symbolsLayer: SymbolsLayer = symbolsLayer;
    private var symbolMapFeature: SymbolMapFeature? = null;

    public override var locationMarkerVisible: Boolean = false;
    public override var lastLocationMarkerPosition: GeographicPosition? = null;
    public override var lastLocationMarkerBearing: Double? = null;
    private var showText: Boolean = false;

    private var showCrumbTrail: Boolean = false;
    private var numberOfCrumbsToShow: Int = 0;
    private var showCrumbTrailText: Boolean = false;
    private val crumbTrail = mutableListOf<SymbolMapFeature>();

    private val registeredCallbackHandlers = mutableListOf<ILocationMarkerInternalCallbacks>();

    init
    {
        locationMarkerVisible = false;
        lastLocationMarkerPosition = null;
        lastLocationMarkerBearing = null;
        if (symbolMapFeature == null)
        {
            symbolMapFeature = SymbolMapFeature();
            symbolMapFeature!!.Symbol = SymbolType.LocationDisabled; // ???
            symbolMapFeature!!.Identity = "02B0DBF0-8B54-4522-9B79-E9E8867B03BA";
            symbolMapFeature!!.UniqueGuid = UUID.fromString(symbolMapFeature!!.Identity);
        }
    }

    /**
     * Note that this registration method supports multiple listeners.
      */
    internal fun RegisterForCallback(handler: ILocationMarkerInternalCallbacks)
    {
        registeredCallbackHandlers.add(handler);
    }

    override fun SetOrChangeLocationMarkerPosition(newPosition: GeographicPosition, newText: String, newBearing: Double, newSpeed: Double)
    {
        try
        {
            if (symbolMapFeature != null)
            {
                symbolMapFeature!!.Longitude = newPosition.Longitude;
                symbolMapFeature!!.Latitude = newPosition.Latitude;
                symbolMapFeature!!.Bearing = newBearing;
                symbolMapFeature!!.Text = newText;
                if (locationMarkerVisible == true)
                {
                    var textIfRequired: String = "";
                    if (showText == true)
                    {
                        textIfRequired = newText;
                    }
                    if (newSpeed > 0.5) // ??? TBD
                    {
                        lastLocationMarkerBearing = newBearing;
                    }
                    else
                    {
                        lastLocationMarkerBearing = null;
                    }
                    var requiredSymbol: SymbolType? = null;
                    if (symbolMapFeature!!.Symbol == SymbolType.LocationWithDirection)
                    {
                        if (newSpeed > 0.5) // ??? TBD
                        {
                            requiredSymbol = SymbolType.LocationWithDirection;
                        }
                        else
                        {
                            requiredSymbol = SymbolType.Location;
                        }
                    }
                    symbolsLayer.ChangeSymbolPosition(UUID.fromString(symbolMapFeature!!.Identity), newPosition, newIcon = requiredSymbol, newText = textIfRequired, newBearing = newBearing);
                    lastLocationMarkerPosition = newPosition;
                    if (showCrumbTrail == true)
                    {
                        var crumb = SymbolMapFeature();
                        crumb.UniqueGuid = UUID.randomUUID();
                        crumb.Identity = crumb.UniqueGuid.toString();
                        crumb.Longitude = symbolMapFeature!!.Longitude;
                        crumb.Latitude = symbolMapFeature!!.Latitude;
                        if (showCrumbTrailText == true)
                        {
                            crumb.Text = newText;
                        }
                        crumb.Symbol = SymbolType.CrumbTrail;
                        crumb.CustomSymbol = symbolMapFeature!!.CustomSymbol;
                        crumbTrail.add(crumb);
                        symbolsLayer.AddSymbols(listOf<SymbolMapFeature>(crumb));

                        while (crumbTrail.count() > numberOfCrumbsToShow)
                        {
                            symbolsLayer.RemoveSymbols(listOf<SymbolMapFeature>(crumbTrail.elementAt(0)));
                            crumbTrail.removeAt(0);
                        }

                    }

                    for (registeredCallbackHandler in registeredCallbackHandlers)
                    {
                        registeredCallbackHandler.LocationMarkerInternalCallback(newPosition, newText, newBearing, newSpeed);
                    }
                }
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun ShowLocationMarker(show: Boolean, includeText: Boolean)
    {
        try
        {
            if (symbolMapFeature != null)
            {
                if (show == true && locationMarkerVisible == false)
                {
                    symbolsLayer.AddSymbols(listOf<SymbolMapFeature>(symbolMapFeature!!));
                    locationMarkerVisible = true;
                }
                else if (show == false && locationMarkerVisible == true)
                {
                    symbolsLayer.RemoveSymbols(listOf<SymbolMapFeature>(symbolMapFeature!!));
                    locationMarkerVisible = false;
                }
                else
                {
                    // LOG warning here???
                }
                showText = includeText;
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun SetLocationMarkerType(locationMarkerType: LocationMarkerType)
    {
        try
        {
            if (symbolMapFeature != null)
            {
                var newSymbolType: SymbolType? = null;
                when (locationMarkerType)
                {
                    LocationMarkerType.Location -> newSymbolType = SymbolType.Location;
                    LocationMarkerType.LocationWithDirection -> newSymbolType = SymbolType.LocationWithDirection;
                    LocationMarkerType.LocationSearching -> newSymbolType = SymbolType.LocationSearching;
                    LocationMarkerType.LocationDisabled -> newSymbolType = SymbolType.LocationDisabled;
                    else -> Logging.E("Called for unhandled locationMarkerType ${locationMarkerType.name}.");
                }
                if (newSymbolType != symbolMapFeature!!.Symbol)
                {
                    symbolMapFeature!!.Symbol = newSymbolType!!;
                    if (locationMarkerVisible == true)
                    {
                        var symbolProperty = SymbolSymbolTypeProperty();
                        symbolProperty.Symbol = newSymbolType;
                        symbolProperty.SymbolGuid = symbolMapFeature!!.UniqueGuid;
                        symbolsLayer.ChangeSymbolProperties(listOf<SymbolPropertyBase>(symbolProperty));

                    }
                }
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun ShowLocationMarkerCrumbTrail(show: Boolean, numberOfCrumbs: Int, includeText: Boolean)
    {
        try
        {
            if (symbolMapFeature != null)
            {
                if (showCrumbTrail == true && show == false)
                {
                    // Clear list and also rendered crumb symbols.
                    while (crumbTrail.count() > 0)
                    {
                        symbolsLayer.RemoveSymbols(listOf<SymbolMapFeature>(crumbTrail.elementAt(0)));
                        crumbTrail.removeAt(0);
                    }
                    showCrumbTrail = false;
                    numberOfCrumbsToShow = 0;
                    showCrumbTrailText = false;
                }
                else if (showCrumbTrail == false && show == true)
                {
                    showCrumbTrail = true;
                    numberOfCrumbsToShow = numberOfCrumbs;
                    showCrumbTrailText = includeText;
                }
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

}

public enum class LocationMarkerType
{
    Location,
    LocationWithDirection,
    LocationSearching,
    LocationDisabled,
}