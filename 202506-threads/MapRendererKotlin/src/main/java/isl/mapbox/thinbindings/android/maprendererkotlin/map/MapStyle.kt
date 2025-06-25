package isl.mapbox.thinbindings.android.maprendererkotlin.map

import android.content.Context
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.rgba
import com.mapbox.maps.extension.style.utils.ColorUtils.rgbaExpressionToColorInt
import com.mapbox.maps.plugin.scalebar.scalebar
import isl.mapbox.thinbindings.android.internal.IMapStyleInternalCallbacks
import isl.mapbox.thinbindings.android.internal.Logging
import isl.mapbox.thinbindings.android.internal.MapStyleListener
import isl.mapbox.thinbindings.android.maprendererkotlin.IMapCallback
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.JobLayer
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.RoadSegmentLayer
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.ServicePointAndTradeSiteLayer
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.SymbolsLayer
import isl.mapbox.thinbindings.android.maprendererkotlin.misc.ConditionalCompile

/**
 * Implementation of IMapStyle and IMapStyleInternalCallbacks for controlling the map rendering
 */
public class MapStyle(context: Context, mapboxMapView: MapView) : IMapStyle, IMapStyleInternalCallbacks
{
    public override val MapStyleSmartSuiteLight: String = "mapbox://styles/integrated-skills/cjwkdyuf72jlf1cmgs1w528xz";  // SmartSuite Light (Default)
    public override val MapStyleSmartSuiteDark: String = "mapbox://styles/integrated-skills/cjwkdyjhy17la1cprwlk3uon5";   // SmartSuite Dark

    private val theContext: Context = context
    private val theMapView: MapView = mapboxMapView
    private var mapStyleListener: MapStyleListener
    public override lateinit var roadSegmentLayer: RoadSegmentLayer private set
    public override lateinit var servicePointAndTradeSiteLayer: ServicePointAndTradeSiteLayer private set
    public override lateinit var jobLayer: JobLayer private set
    public override lateinit var symbolsLayer: SymbolsLayer private set

    public override lateinit var styleUri: String private set

    init {
        mapStyleListener = MapStyleListener(theMapView)
        mapStyleListener.RegisterStyleInternalForCallback(this)
        roadSegmentLayer = RoadSegmentLayer(theContext, theMapView, mapStyleListener)
        servicePointAndTradeSiteLayer = ServicePointAndTradeSiteLayer(theContext, theMapView, mapStyleListener)
        jobLayer = JobLayer(theContext, theMapView, mapStyleListener)
        symbolsLayer = SymbolsLayer(theContext, theMapView, mapStyleListener)
        theMapView.scalebar.enabled = false;
    }

    public override fun ShowScalebar(show: Boolean)
    {
        theMapView.scalebar.enabled = show;
    }

    internal fun RegisterForCallback(handler: IMapStyleInternalCallbacks)
    {
        mapStyleListener.RegisterStyleInternalForCallback(handler);
    }

    // This method will be called back by the MapStyleListener.
    override fun MapStyleInternalCallback(theMapStyle: Style)
    {
        Logging.D("Map Style callback.")
        // Here we will set up all the layers for things like road segment features.
        roadSegmentLayer.CreateRoadSegmentSourceLayersAndFilters(theMapStyle)
        servicePointAndTradeSiteLayer.CreateServicePointsSourceLayersAndFilters(theMapStyle)
        jobLayer.CreateJobsSourceLayersAndFilters(theMapStyle)
        symbolsLayer.CreateSymbolSourceLayersAndFilters(theMapStyle)
    }

    override fun LoadStyle(theStyle: String)
    {
        try
        {
            Logging.D("Load Style '${theStyle}'.")
            mapStyleListener.LoadStyle(theStyle)
            styleUri = theStyle
            if (theStyle == MapStyleSmartSuiteLight)
            {
                val scalebar = theMapView.scalebar;
                scalebar.primaryColor = rgbaExpressionToColorInt(rgba(192.0, 192.0, 192.0, 1.0))!!;
                scalebar.textColor = rgbaExpressionToColorInt(rgba(192.0, 192.0, 192.0, 1.0))!!;
                scalebar.secondaryColor = rgbaExpressionToColorInt(rgba(255.0, 255.0, 255.0, 1.0))!!;
            }
            else if (theStyle == MapStyleSmartSuiteDark)
            {
                val scalebar = theMapView.scalebar;
                scalebar.primaryColor = rgbaExpressionToColorInt(rgba(128.0, 128.0, 128.0, 1.0))!!;
                scalebar.textColor = rgbaExpressionToColorInt(rgba(128.0, 128.0, 128.0, 1.0))!!;
                scalebar.showTextBorder = false;
                scalebar.secondaryColor = rgbaExpressionToColorInt(rgba(64.0, 64.0, 64.0, 1.0))!!;
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    internal fun RegisterForMapCallback(mapHandler: IMapCallback)
    {
        mapStyleListener.RegisterForMapCallback(mapHandler);
    }
}