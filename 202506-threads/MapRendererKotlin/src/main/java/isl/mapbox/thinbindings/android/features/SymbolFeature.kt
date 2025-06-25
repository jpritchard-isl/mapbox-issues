package isl.mapbox.thinbindings.android.features

/**
 * Other point feautres that should be displayed as symbols
 */
public open class SymbolMapFeature : AbstractPointMapFeature()
{
    /**
     * The type of the icon to be used. If custom is selected then also set the friendly name in CustomSymbol.
     */
    public var Symbol: SymbolType = SymbolType.Custom;


    /**
     * The 'friendly name' of the custom symbol to be used.
     */
    public var CustomSymbol: String = "";

    /**
     * Optional supporting text to be displayed with the icon.
     */
    public var Text: String = "";

    /**
     * ???
     */
    public var Bearing: Double = 0.0;
}

public enum class SymbolType
{
    PoI,
    RouteOutline,
    TradeOutline,
    SymbolText,
    SnappedLocation,
    ManoeuvreLocation,
    CrumbTrail,
    Location,
    LocationWithDirection,
    LocationSearching,
    LocationDisabled,
    Adhoc,
    Custom,
}
