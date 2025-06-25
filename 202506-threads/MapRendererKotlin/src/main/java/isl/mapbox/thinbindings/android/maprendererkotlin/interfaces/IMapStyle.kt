package isl.mapbox.thinbindings.android.maprendererkotlin.map

import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.JobLayer
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.RoadSegmentLayer
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.ServicePointAndTradeSiteLayer
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.SymbolsLayer

/**
 * The MapStyle interface.
 */
interface IMapStyle
{
    /**
     * The uri to be used for ISL Light style.
     */
    val MapStyleSmartSuiteLight: String

    /**
     * The uri to be used for ISL Dark style.
     */
    val MapStyleSmartSuiteDark: String

    /**
     * The last loaded map style uri.
     */
    val styleUri : String

    /**
     * Load a new style using a uri, for example 'mapbox://styles/integrated-skills/cjwkdyuf72jlf1cmgs1w528xz' which is ISL's Light style.
     */
    fun LoadStyle(theStyle : String)

    /**
     * The nested Road Segment layer.
     */
    val roadSegmentLayer: RoadSegmentLayer

    /**
     * The nested Service Point and Trade Site layer.
     */
    val servicePointAndTradeSiteLayer: ServicePointAndTradeSiteLayer

    /**
     * The nested Job layer.
     */
    val jobLayer: JobLayer

    /**
     * The nested Symbol layer.
     */
    val symbolsLayer: SymbolsLayer

    /**
     * Determines whether the scalebar is shown, this defaults to false;
     */
    fun ShowScalebar(show: Boolean)
}

