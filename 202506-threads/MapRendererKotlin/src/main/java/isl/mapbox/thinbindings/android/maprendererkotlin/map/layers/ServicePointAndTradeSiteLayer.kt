package isl.mapbox.thinbindings.android.maprendererkotlin.map.layers

import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import isl.mapbox.thinbindings.android.internal.Logging
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.literal
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.Source
import com.mapbox.maps.extension.style.sources.addGeoJSONSourceFeatures
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.sources.removeGeoJSONSourceFeatures
import com.mapbox.maps.extension.style.sources.updateGeoJSONSourceFeatures
import isl.mapbox.thinbindings.android.R
import isl.mapbox.thinbindings.android.features.ServicePointAndTradeSiteFeature
import isl.mapbox.thinbindings.android.internal.MapStyleListener
import isl.mapbox.thinbindings.android.maprendererkotlin.misc.ConditionalCompile
import isl.mapbox.thinbindings.android.maprendererkotlin.misc.FeatureColours
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.MapPosition
import isl.mapbox.thinbindings.android.positions.ScreenPosition
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Note that this class is used for service points as well as trade sites, but in the code the member and method names may just refer to service points for brevity.
 */
public class ServicePointAndTradeSiteLayer(context: Context, theMapView: MapView, theMapStyleListener: MapStyleListener)
{
    private val mapView: MapView = theMapView
    private val theContext: Context = context
    private val theMapStyleListener: MapStyleListener = theMapStyleListener;
    private val sourceId: String = "GeojsonSourcePoints";

    private val imageId_RedYellowCircleSegments: String = "redYellowCircleSegments";
    private val imageId_GreyCircleSegments: String = "greyCircleSegments";
    private val imageId_OrangeCrossSymbol: String = "orangeCrossSymbol";

    private lateinit var reportLayerRoute: CircleLayer;
    private lateinit var actionLayerRoute: CircleLayer;
    private lateinit var actionedLayerRoute: CircleLayer;
    private lateinit var unservicedLargeLayerRoute: CircleLayer;
    private lateinit var servicedLargeLayerRoute: CircleLayer;
    private lateinit var commentSymbolLayer1Route: SymbolLayer;
    private lateinit var commentServicedSymbolLayer1Route: SymbolLayer;
    private lateinit var unservicedLayerRoute: CircleLayer;
    private lateinit var servicedLayerRoute: CircleLayer;
    private lateinit var textSymbolLayer: SymbolLayer;
    private lateinit var textSymbolLayer2: SymbolLayer;

    private var ServicePointOpacity: Double = 1.0;

    private var servicePointFeatures: MutableList<Feature> = mutableListOf<Feature>()

    private val UnservicedColour = FeatureColours.Blue
    private val ReportedColour = FeatureColours.Blue
    private val ServicedColour = FeatureColours.Grey
    private val ActionableColour = FeatureColours.Orange
    private val ActionedGrey = FeatureColours.Grey
    private val LabelColour = FeatureColours.Orange
    private val HighlightColour = FeatureColours.Yellow

    private val RouteStartZoom: Double = 14.0;
    private val RouteLowZoom: Double = 16.0;
    private val RouteHighZoom: Double = 22.0;

    private val TradeStartZoom: Double = 6.0;
    private val TradeLowZoom: Double = 8.0;
    private val TradeHighZoom: Double = 22.0;

    private val Zone1LowZoom: Double = 3.0;
    private val Zone2LowZoom: Double = 6.0;
    private val Zone3LowZoom: Double = 9.0;
    private val Zone4LowZoom: Double = 18.0;

    private val Zone1HighZoom: Double = 9.0;
    private val Zone2HighZoom: Double = 18.0;
    private val Zone3HighZoom: Double = 27.0;
    private val Zone4HighZoom: Double = 54.0;

    // Use these instead to enable really large zoom for inspecting SP layout design.
//    private val Zone1HighZoom: Double = 27.0;
//    private val Zone2HighZoom: Double = 54.0;
//    private val Zone3HighZoom: Double = 81.0;
//    private val Zone4HighZoom: Double = 162.0;

    // This is just a adjustment so that the circle matches the Zone 2 circle size.
    private val RedYellowTrefoilImageRadius: Double; //needs to be 20.0 for density 2.0 & 26.5 for density 1.5.

    private val lock = ReentrantLock();
    private val timeout: Long = 100;
    private val timeoutUnits = TimeUnit.MILLISECONDS;

    init {
        val density = Resources.getSystem().displayMetrics.density;
        RedYellowTrefoilImageRadius = 40.0 / density;
    }
    internal fun CreateServicePointsSourceLayersAndFilters(theStyle: Style)
    {
        try
        {
            Logging.D("Will attempt to create layers and filters for ${theStyle}.")
            if (CheckIfSourceExistsAlready(theStyle) == true)
            {
                return;
            }
            AddFeaturesToSource(servicePointFeatures);

            LoadSymbolImages(theStyle)

            unservicedLayerRoute = CircleLayer("servicePoint_unservicedLayerRoute", sourceId);
            unservicedLayerRoute.visibility(Visibility.VISIBLE);
            unservicedLayerRoute.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(RouteStartZoom); literal(0.0) }
                        stop { literal(RouteLowZoom); literal(Zone1LowZoom) }
                        stop { literal(RouteHighZoom); literal(Zone1HighZoom) }
                    }
                )
            );
            unservicedLayerRoute.circleColor(UnservicedColour);
            unservicedLayerRoute.circleOpacity(ServicePointOpacity);
            unservicedLayerRoute.filter(
                Expression.all
                (
                    Expression.eq(Expression.get("serviced"), literal(false)),
                    Expression.eq(Expression.get("istrade"), literal(false))
                )
            );

            val unservicedLayerTrade: CircleLayer = CircleLayer("servicePoint_unservicedLayerTrade", sourceId);
            unservicedLayerTrade.visibility(Visibility.VISIBLE);
            unservicedLayerTrade.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(TradeStartZoom); literal(0.0) }
                        stop { literal(TradeLowZoom); literal(Zone1LowZoom) }
                        stop { literal(TradeHighZoom); literal(Zone1HighZoom) }
                    }
                )
            );
            unservicedLayerTrade.circleColor(UnservicedColour)
            unservicedLayerTrade.filter(
                Expression.all
                (
                    Expression.eq(Expression.get("serviced"), literal(false)),
                    Expression.eq(Expression.get("istrade"), literal(true))
                )
            );

            unservicedLargeLayerRoute = CircleLayer("servicePoint_unservicedLargeLayerRoute", sourceId);
            unservicedLargeLayerRoute.visibility(Visibility.VISIBLE);
            unservicedLargeLayerRoute.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(RouteStartZoom); literal(0.0) }
                        stop { literal(RouteLowZoom); literal(Zone2LowZoom) }
                        stop { literal(RouteHighZoom); literal(Zone2HighZoom) }
                    }
                )
            );
            unservicedLargeLayerRoute.circleColor(UnservicedColour);
            unservicedLargeLayerRoute.circleOpacity(ServicePointOpacity);
            unservicedLargeLayerRoute.filter(
                Expression.all
                (
                    Expression.eq(Expression.get("serviced"), literal(false)),
                    Expression.eq(Expression.get("istrade"), literal(false))
                )
            );

            val unservicedLargeLayerTrade: CircleLayer = CircleLayer("servicePoint_unservicedLargeLayerTrade", sourceId);
            unservicedLargeLayerTrade.visibility(Visibility.VISIBLE);
            unservicedLargeLayerTrade.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(TradeStartZoom); literal(0.0) }
                        stop { literal(TradeLowZoom); literal(Zone2LowZoom) }
                        stop { literal(TradeHighZoom); literal(Zone2HighZoom) }
                    }
                )
            );
            unservicedLargeLayerTrade.circleColor(UnservicedColour)
            unservicedLargeLayerTrade.filter(
                Expression.all
                (
                    Expression.eq(Expression.get("serviced"), literal(false)),
                    Expression.eq(Expression.get("istrade"), literal(true))
                )
            );

            servicedLayerRoute = CircleLayer("servicePoint_servicedLayerRoute", sourceId);
            servicedLayerRoute.visibility(Visibility.VISIBLE);
            servicedLayerRoute.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(RouteStartZoom); literal(0.0) }
                        stop { literal(RouteLowZoom); literal(Zone1LowZoom) }
                        stop { literal(RouteHighZoom); literal(Zone1HighZoom) }
                    }
                )
            );
            servicedLayerRoute.circleColor(ServicedColour)
            servicedLayerRoute.circleOpacity(ServicePointOpacity);
            servicedLayerRoute.filter(
                Expression.all
                (
                    Expression.eq(Expression.get("serviced"), literal(true)),
                    Expression.eq(Expression.get("istrade"), literal(false))
                )
            );

            val servicedLayerTrade: CircleLayer = CircleLayer("servicePoint_servicedLayerTrade", sourceId);
            servicedLayerTrade.visibility(Visibility.VISIBLE);
            servicedLayerTrade.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(TradeStartZoom); literal(0.0) }
                        stop { literal(TradeLowZoom); literal(Zone1LowZoom) }
                        stop { literal(TradeHighZoom); literal(Zone1HighZoom) }
                    }
                )
            );
            servicedLayerTrade.circleColor(ServicedColour);
            servicedLayerTrade.filter(
                Expression.all
                (
                    Expression.eq(Expression.get("serviced"), literal(true)),
                    Expression.eq(Expression.get("istrade"), literal(true))
                )
            );

            servicedLargeLayerRoute = CircleLayer("servicePoint_servicedLargeLayerRoute", sourceId);
            servicedLargeLayerRoute.visibility(Visibility.VISIBLE);
            servicedLargeLayerRoute.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(RouteStartZoom); literal(0.0) }
                        stop { literal(RouteLowZoom); literal(Zone2LowZoom) }
                        stop { literal(RouteHighZoom); literal(Zone2HighZoom) }
                    }
                )
            );
            servicedLargeLayerRoute.circleColor(ServicedColour);
            servicedLargeLayerRoute.circleOpacity(ServicePointOpacity);
            servicedLargeLayerRoute.filter(
                Expression.all(
                    Expression.eq(Expression.get("serviced"), literal(true)),
                    Expression.eq(Expression.get("istrade"), literal(false))
                )
            );

            val servicedLargeLayerTrade: CircleLayer = CircleLayer("servicePoint_servicedLargeLayerTrade", sourceId);
            servicedLargeLayerTrade.visibility(Visibility.VISIBLE);
            servicedLargeLayerTrade.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(TradeStartZoom); literal(0.0) }
                        stop { literal(TradeLowZoom); literal(Zone2LowZoom) }
                        stop { literal(TradeHighZoom); literal(Zone2HighZoom) }
                    }
                )
            );
            servicedLargeLayerTrade.circleColor(ServicedColour);
            servicedLargeLayerTrade.filter(
                Expression.all(
                    Expression.eq(Expression.get("serviced"), literal(true)),
                    Expression.eq(Expression.get("istrade"), literal(true))
                )
            );

            actionLayerRoute = CircleLayer("servicePoint_actionLayer", sourceId);
            actionLayerRoute.visibility(Visibility.VISIBLE);
            actionLayerRoute.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(RouteStartZoom); literal(0.0) }
                        stop { literal(RouteLowZoom); literal(Zone3LowZoom) }
                        stop { literal(RouteHighZoom); literal(Zone3HighZoom) }
                    }
                )
            );
            actionLayerRoute.circleColor(ActionableColour);
            actionLayerRoute.circleOpacity(ServicePointOpacity);
            actionLayerRoute.filter(
                Expression.all(
                    Expression.eq(Expression.get("hasaction"), literal(true))
                )
            );

            commentSymbolLayer1Route = SymbolLayer("symbol_redYellowCircleSegmentsRoute", sourceId);
            commentSymbolLayer1Route.iconImage(imageId_RedYellowCircleSegments);
            commentSymbolLayer1Route.iconIgnorePlacement(true);
            commentSymbolLayer1Route.iconAllowOverlap(true);
            commentSymbolLayer1Route.iconSize(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(RouteStartZoom); literal(0.0) }
                        stop { literal(RouteLowZoom); literal(Zone2LowZoom / RedYellowTrefoilImageRadius) }
                        stop { literal(RouteHighZoom); literal(Zone2HighZoom / RedYellowTrefoilImageRadius) }
                    }
                )
            );
            //commentSymbolLayer1Route.textIgnorePlacement(false);
            //commentSymbolLayer1Route.textAllowOverlap(false);
            commentSymbolLayer1Route.iconOpacity(ServicePointOpacity);
            commentSymbolLayer1Route.filter(
                Expression.all(
                    //Expression.gt(Expression.zoom(), 12.9)
                    Expression.eq(Expression.get("comments"), literal(true)),
                    Expression.eq(Expression.get("istrade"), literal(false)),
                )
            );

            commentServicedSymbolLayer1Route = SymbolLayer("symbol_greyCircleSegmentsRoute", sourceId);
            commentServicedSymbolLayer1Route.iconImage(imageId_GreyCircleSegments);
            commentServicedSymbolLayer1Route.iconIgnorePlacement(true);
            commentServicedSymbolLayer1Route.iconAllowOverlap(true);
            commentServicedSymbolLayer1Route.iconSize(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(RouteStartZoom); literal(0.0) }
                        stop { literal(RouteLowZoom); literal(Zone2LowZoom / RedYellowTrefoilImageRadius) }
                        stop { literal(RouteHighZoom); literal(Zone2HighZoom / RedYellowTrefoilImageRadius) }
                    }
                )
            );
            //commentServicedSymbolLayer1Route.textIgnorePlacement(false);
            //commentServicedSymbolLayer1Route.textAllowOverlap(false);
            commentServicedSymbolLayer1Route.iconOpacity(ServicePointOpacity);
            commentServicedSymbolLayer1Route.filter(
                Expression.all(
                    //Expression.gt(Expression.zoom(), 12.9)
                    Expression.eq(Expression.get("comments"), literal(true)),
                    Expression.eq(Expression.get("istrade"), literal(false)),
                    Expression.eq(Expression.get("serviced"), literal(true))
                )
            );

            val commentSymbolLayer1Trade: SymbolLayer = SymbolLayer("symbol_redYellowCircleSegmentsTrade", sourceId);
            commentSymbolLayer1Trade.iconImage(imageId_RedYellowCircleSegments);
            commentSymbolLayer1Trade.iconIgnorePlacement(true);
            commentSymbolLayer1Trade.iconAllowOverlap(true);
            commentSymbolLayer1Trade.iconSize(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(TradeStartZoom); literal(0.0) }
                        stop { literal(TradeLowZoom); literal(Zone2LowZoom / RedYellowTrefoilImageRadius) }
                        stop { literal(TradeHighZoom); literal(Zone2HighZoom / RedYellowTrefoilImageRadius) }
                    }
                )
            );
            //commentSymbolLayer1Trade.textIgnorePlacement(false);
            //commentSymbolLayer1Trade.textAllowOverlap(false);
            commentSymbolLayer1Trade.filter(
                Expression.all(
                    Expression.eq(Expression.get("comments"), literal(true)),
                    Expression.eq(Expression.get("istrade"), literal(true))
                )
            );

            val crossSymbolLayer1Trade: SymbolLayer = SymbolLayer("symbol_orangeCross", sourceId);
            crossSymbolLayer1Trade.iconImage(imageId_OrangeCrossSymbol);
            crossSymbolLayer1Trade.iconIgnorePlacement(true);
            crossSymbolLayer1Trade.iconAllowOverlap(true);
            crossSymbolLayer1Trade.iconSize(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(TradeStartZoom); literal(0.0) }
                        stop { literal(TradeLowZoom); literal(Zone3LowZoom / RedYellowTrefoilImageRadius) }
                        stop { literal(TradeHighZoom); literal(Zone3HighZoom / RedYellowTrefoilImageRadius) }
                    }
                )
            );
            //crossSymbolLayer1Trade.textIgnorePlacement(false);
            //crossSymbolLayer1Trade.textAllowOverlap(false);
            crossSymbolLayer1Trade.filter(
                Expression.all(
                    //Expression.gt(Expression.zoom(), 12.9),
                    Expression.eq(Expression.get("isstopped"), literal(true))
                )
            );

            val highlightLayerRoute: CircleLayer  = CircleLayer("servicePoint_highlightLayer", sourceId);
            highlightLayerRoute.visibility(Visibility.VISIBLE);
            highlightLayerRoute.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(RouteStartZoom); literal(0.0) }
                        stop { literal(RouteLowZoom); literal(Zone4LowZoom) }
                        stop { literal(RouteHighZoom); literal(Zone4HighZoom) }
                    }
                )
            );
            highlightLayerRoute.circleOpacity(0.5);
            highlightLayerRoute.circleColor(HighlightColour);
            highlightLayerRoute.filter(
                Expression.all(
                    Expression.eq(Expression.get("highlight"), literal(true)),
                )
            );

            val proximityLayer: CircleLayer  = CircleLayer("servicePoint_proximityLayer", sourceId);
            proximityLayer.visibility(Visibility.VISIBLE);
            proximityLayer.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(RouteStartZoom); literal(0.0) }
                        stop { literal(RouteLowZoom); literal(Zone3LowZoom + 4.0) }
                        stop { literal(RouteHighZoom); literal(Zone3HighZoom + 4.0) }
                    }
                )
            );
            proximityLayer.circleStrokeColor(HighlightColour);
            proximityLayer.circleStrokeWidth(4.0);
            proximityLayer.circleOpacity(0.0);
            proximityLayer.circleColor(HighlightColour);
            proximityLayer.filter(
                Expression.all(
                    Expression.eq(Expression.get("proximity"), literal(true)),
                )
            );

            reportLayerRoute = CircleLayer("servicePoint_reportLayerRoute", sourceId);
            reportLayerRoute.visibility(Visibility.VISIBLE);
            reportLayerRoute.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(RouteStartZoom); literal(0.0) }
                        stop { literal(RouteLowZoom); literal(Zone3LowZoom + 2.0) }
                        stop { literal(RouteHighZoom); literal(Zone3HighZoom + 2.0) }
                    }
                )
            );
            reportLayerRoute.circleStrokeColor(ReportedColour);
            reportLayerRoute.circleStrokeWidth(2.0);
            reportLayerRoute.circleOpacity(0.0);
            reportLayerRoute.circleStrokeOpacity(ServicePointOpacity);
            reportLayerRoute.circleColor(ReportedColour);
            reportLayerRoute.filter(
                Expression.all(
                    Expression.eq(Expression.get("reported"), literal(true)),
                    Expression.eq(Expression.get("istrade"), literal(false))
                )
            );

            val reportLayerTrade: CircleLayer = CircleLayer("servicePoint_reportLayerTrade", sourceId);
            reportLayerTrade.visibility(Visibility.VISIBLE);
            reportLayerTrade.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(TradeStartZoom); literal(0.0) }
                        stop { literal(TradeLowZoom); literal(Zone3LowZoom + 2.0) }
                        stop { literal(TradeHighZoom); literal(Zone3HighZoom + 2.0) }
                    }
                )
            );
            reportLayerTrade.circleStrokeColor(ReportedColour);
            reportLayerTrade.circleStrokeWidth(2.0);
            reportLayerTrade.circleOpacity(0.0);
            reportLayerTrade.circleColor(ReportedColour);
            reportLayerTrade.filter(
                Expression.all(
                    Expression.eq(Expression.get("reported"), literal(true)),
                    Expression.eq(Expression.get("istrade"), literal(true))
                )
            );

            actionedLayerRoute = CircleLayer("servicePoint_actionedLayer", sourceId);
            actionedLayerRoute.visibility(Visibility.VISIBLE);
            actionedLayerRoute.circleRadius(
                interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(RouteStartZoom); literal(0.0) }
                        stop { literal(RouteLowZoom); literal(Zone3LowZoom) }
                        stop { literal(RouteHighZoom); literal(Zone3HighZoom) }
                    }
                )
            );
//            actionedLayerRoute.circleStrokeColor(ActionedGrey);
//            actionedLayerRoute.circleStrokeWidth(2.0);
//            actionedLayerRoute.circleOpacity(0.0);
            actionedLayerRoute.circleColor(ActionedGrey);
            actionedLayerRoute.circleOpacity(ServicePointOpacity);
            actionedLayerRoute.filter(
                Expression.all(
                    Expression.eq(Expression.get("actioned"), literal(true))
                )
            );

            textSymbolLayer = SymbolLayer("servicePoint_text", sourceId);
            textSymbolLayer.textField(Expression.get("number"));
                // NOTE: it is essential that the font stack defined here matches one exactly in the map styles being used where we
                // will be using offline maps, otherwise the text and symbol will not render for that offline map.
                // see https://docs.mapbox.com/help/troubleshooting/mobile-offline/ and https://docs.mapbox.com/help/glossary/font-stack/
                textSymbolLayer.textFont(listOf<String>("DIN Offc Pro Regular", "Arial Unicode MS Regular"));
                textSymbolLayer.textColor(LabelColour);
                textSymbolLayer.textOpacity(1.0 * ServicePointOpacity);
                textSymbolLayer.textSize(16.0);
                textSymbolLayer.textOffset(listOf<Double>(1.0, 0.0));
                textSymbolLayer.textAnchor(TextAnchor.LEFT);
                textSymbolLayer.textIgnorePlacement(true);
                textSymbolLayer.textAllowOverlap(true);
                textSymbolLayer.textMaxWidth(7.0);
                // x,y textoffset to apply to centre of text. 1.0 appears similar to text height. +ve to right and down
                // Note that if text is used in combination with an image then if they're not far enough apart then you'll only see one.
                textSymbolLayer.filter(
                    Expression.all(
                        Expression.gte(Expression.zoom(), literal(17.0))
                    )
                );

            textSymbolLayer2 = SymbolLayer("servicePoint_text2", sourceId);
            textSymbolLayer2.textField(Expression.get("flat"));
                // NOTE: it is essential that the font stack defined here matches one exactly in the map styles being used where we
                // will be using offline maps, otherwise the text and symbol will not render for that offline map.
                // see https://docs.mapbox.com/help/troubleshooting/mobile-offline/ and https://docs.mapbox.com/help/glossary/font-stack/
            textSymbolLayer2.textFont(listOf<String>("DIN Offc Pro Regular", "Arial Unicode MS Regular"));
            textSymbolLayer2.textColor(LabelColour);
            textSymbolLayer2.textOpacity(1.0 * ServicePointOpacity);
            textSymbolLayer2.textSize(16.0);
            textSymbolLayer2.textOffset(listOf<Double>(1.0,1.0));
            textSymbolLayer2.textAnchor(TextAnchor.LEFT);
            textSymbolLayer2.textIgnorePlacement(true);
            textSymbolLayer2.textAllowOverlap(true);
                // x,y textoffset to apply to centre of text. 1.0 appears similar to text height. +ve to right and down
                // Note that if text is used in combination with an image then if they're not far enough apart then you'll only see one.
            textSymbolLayer2.filter(
                Expression.all(
                    Expression.gte(Expression.zoom(), literal(17.0))
                )
            );

            // This first layer will be the bottom layer on the map
            theStyle.addLayer(highlightLayerRoute);
            theStyle.addLayer(proximityLayer);
            theStyle.addLayer(reportLayerRoute);
            theStyle.addLayer(reportLayerTrade);
            theStyle.addLayer(actionLayerRoute);
            theStyle.addLayer(actionedLayerRoute);
            theStyle.addLayer(unservicedLargeLayerRoute);
            theStyle.addLayer(unservicedLargeLayerTrade);
            theStyle.addLayer(servicedLargeLayerRoute);
            theStyle.addLayer(servicedLargeLayerTrade);
            theStyle.addLayer(commentSymbolLayer1Route);
            theStyle.addLayer(commentServicedSymbolLayer1Route);
            theStyle.addLayer(commentSymbolLayer1Trade);
            theStyle.addLayer(unservicedLayerRoute);
            theStyle.addLayer(unservicedLayerTrade);
            theStyle.addLayer(servicedLayerRoute);
            theStyle.addLayer(servicedLayerTrade);
            theStyle.addLayer(crossSymbolLayer1Trade);
            theStyle.addLayer(textSymbolLayer2);
            theStyle.addLayer(textSymbolLayer);
            // This last layer will be the top layer on the map

        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    public fun AddServicePointsAndTradeSites(servicePointsAndTradeSites: List<ServicePointAndTradeSiteFeature>, id: String? = null): Boolean
    {
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                Logging.D("Called for ${servicePointsAndTradeSites.count()} ServicePoints.");
                var count: Int = 0;
                var featuresToAdd = mutableListOf<Feature>();
                for (servicePointsAndTradeSite in servicePointsAndTradeSites)
                {
                    var found = GetServicePointFeatureById(servicePointsAndTradeSite.UniqueGuid.toString())
                    if (found == null)
                    {
                        if (ConditionalCompile.detailedFeatureCreationLogging == true)
                        {
                            Logging.V("Add a service point of trade site at ${servicePointsAndTradeSite.Longitude} ${servicePointsAndTradeSite.Latitude}");
                        }
                        var point: Point = Point.fromLngLat(servicePointsAndTradeSite.Longitude, servicePointsAndTradeSite.Latitude); // this is 2nd slowest bit
                        var feature = Feature.fromGeometry(point, null, servicePointsAndTradeSite.UniqueGuid.toString()); // this is slowest bit

                        feature.addStringProperty("longitude", "%.6f".format(Locale.ENGLISH, servicePointsAndTradeSite.Longitude));
                        feature.addStringProperty("latitude", "%.6f".format(Locale.ENGLISH, servicePointsAndTradeSite.Latitude));

                        feature.addBooleanProperty("serviced", servicePointsAndTradeSite.Serviced);
                        // need to check first if item already in servicePointsAndTradeSite, _servicePointFeatures or geoJsonSource ??? unfortunately it's so slow to check, lets just add it in again!

                        feature.addStringProperty("note", servicePointsAndTradeSite.Note);
                        feature.addStringProperty("servicecomment", servicePointsAndTradeSite.ServiceComment);

                        if (servicePointsAndTradeSite.Comments == true || servicePointsAndTradeSite.Note.isNullOrEmpty() == false || servicePointsAndTradeSite.ServiceComment.isNullOrEmpty() == false)
                        {
                            feature.addBooleanProperty("comments", true);
                        }
                        else
                        {
                            feature.addBooleanProperty("comments", false);
                        }

                        feature.addStringProperty("action", servicePointsAndTradeSite.Action);
                        if (servicePointsAndTradeSite.HasAction == true || servicePointsAndTradeSite.Action.isNullOrEmpty() == false)
                        {
                            feature.addBooleanProperty("hasaction", true);
                        }
                        else
                        {
                            feature.addBooleanProperty("hasaction", false);
                        }
                        feature.addBooleanProperty("reported", servicePointsAndTradeSite.Reported);
                        feature.addBooleanProperty("actioned", servicePointsAndTradeSite.Actioned);
                        feature.addStringProperty("number", servicePointsAndTradeSite.NameOrNumber);
                        feature.addStringProperty("flat", servicePointsAndTradeSite.Flat);
                        feature.addBooleanProperty("istrade", servicePointsAndTradeSite.IsTrade);
                        feature.addBooleanProperty("isstopped", servicePointsAndTradeSite.IsStopped);

                        count++;
                        if (count < 5)
                        {
                            //serilogger.Here().Debug("Adding servicePointsAndTradeSite id={0}, uniqueGuid={1}", servicePointsAndTradeSite.Identity, servicePointsAndTradeSite.UniqueGuid);
                        }
                        servicePointFeatures.add(feature);
                        featuresToAdd.add(feature);
                    }
                }

                AddSomeFeaturesInSource(featuresToAdd, id);
            }
            catch (ex: Exception)
            {
                Logging.E("Exception = ${ex.message}")
                if (ConditionalCompile.throwAfterLoggingException) throw ex;
            }
            finally
            {
                lock.unlock();
            }
        }
        else
        {
            Logging.W("Timeout waiting for lock.");
        }
        return true;
    }

    public fun FindServicePointsAndTradeSites(id: UUID): ServicePointAndTradeSiteFeature?
    {
        var servicePoint: ServicePointAndTradeSiteFeature? = null;
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {

                var found = GetServicePointFeatureById(id.toString())
                if (found != null)
                {
                    Logging.V("Found service point or trade site for id=${id.toString()}");

                    //found.getStringProperty()

                    servicePoint = ServicePointAndTradeSiteFeature();
                    servicePoint.UniqueGuid = UUID.fromString(found.id()); // ??? TBD repeated below.
                    servicePoint.Comments = found.getBooleanProperty("comments")?: false;
                    servicePoint.Reported = found.getBooleanProperty("reported")?: false;
                    servicePoint.Note = found.getStringProperty("note")?: "";
                    servicePoint.ServiceComment = found.getStringProperty("serviceComment")?: "";
                    servicePoint.HasAction = found.getBooleanProperty("hasaction")?: false;
                    servicePoint.Action = found.getStringProperty("action")?: "";
                    servicePoint.Actioned = found.getBooleanProperty("actioned")?: false;
                    servicePoint.IsTrade = found.getBooleanProperty("istrade")?: false;
                    servicePoint.IsStopped = found.getBooleanProperty("stopped")?: false;
                    servicePoint.NameOrNumber = found.getStringProperty("number")?: "";
                    servicePoint.Flat = found.getStringProperty("flat")?: "";
                    servicePoint.Serviced = found.getBooleanProperty("serviced")?: false;
                    servicePoint.Longitude = found.getNumberProperty("longitude").toDouble();
                    servicePoint.Latitude = found.getNumberProperty("latitude").toDouble();
                    //servicePoint.TappedPosition = ???
                    Logging.D("ServicePointAndTradeSiteFeature with UniqueGuid=${servicePoint.UniqueGuid}, Flat=${servicePoint.Flat}, NameOrNumber=${servicePoint.NameOrNumber}, Address=${servicePoint.Address}, IsTrade=${servicePoint.IsTrade}, Note=${servicePoint.Note}, Comments=${servicePoint.Comments}, ServiceComment=${servicePoint.ServiceComment}, Serviced=${servicePoint.Serviced}, Action=${servicePoint.Action}, Actioned=${servicePoint.Actioned}, IsStopped=${servicePoint.IsStopped}, Reported=${servicePoint.Reported}.");

                }
                else
                {
                    Logging.D("Not able to find service point or trade site for id=${id.toString()}");
                }
            }
            catch (ex: Exception)
            {
                Logging.E("Exception = ${ex.message}")
                if (ConditionalCompile.throwAfterLoggingException) throw ex;
            }
            finally
            {
                lock.unlock();
            }
        }
        else
        {
            Logging.W("Timeout waiting for lock.");
        }
        return servicePoint;
    }

    public fun RemoveServicePointsAndTradeSites(servicePointsAndTradeSites: List<ServicePointAndTradeSiteFeature>?, id: String? = null): Boolean
    {
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                var featuresToRemove = mutableListOf<Feature>();
                if (servicePointsAndTradeSites == null)
                {
                    servicePointFeatures = mutableListOf<Feature>();
                    Logging.D("Called with null list of servicePointsAndTradesSites so removing all.");
                    UpdateAllFeaturesInSource(servicePointFeatures, id);
                }
                else
                {
                    Logging.D("Called for ${servicePointsAndTradeSites.count()} service points or trade sites, current total (before removal)=${servicePointFeatures.count()}");
                    for (servicePointAndTradeSite in servicePointsAndTradeSites)
                    {
                        var found = GetServicePointFeatureById(servicePointAndTradeSite.UniqueGuid.toString())
                        if (found != null)
                        {
                            servicePointFeatures.remove(found);
                            featuresToRemove.add(found);
                        }
                    }
                    Logging.D("After removing service points or trade sites, current total=${servicePointFeatures.count()}");
                    RemoveSomeFeaturesInSource(featuresToRemove, id);
                }
            }
            catch (ex: Exception)
            {
                Logging.E("Exception = ${ex.message}")
                if (ConditionalCompile.throwAfterLoggingException) throw ex;
            }
            finally
            {
                lock.unlock();
            }
        }
        else
        {
            Logging.W("Timeout waiting for lock.");
        }
        return true;
    }

    public fun ChangeServicePointAndTradeSiteProperty(changesRequired: ServicePointAndTradeSitePropertyBase) : Int
    {
        // TBD
        return 0
    }

    public fun ChangeServicePointAndTradeSiteProperties(changesRequired: List<ServicePointAndTradeSitePropertyBase>, id: String? = null) : Int
    {
        var count: Int = 0;
        var countFail: Int = 0;
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                Logging.D("Called with list of ${changesRequired.count()} property changes");
                var featuresToChange = mutableListOf<Feature>();
                for (changeRequired in changesRequired)
                {
                    var found = GetServicePointFeatureById(changeRequired.ServicePointAndTradeSiteGuid.toString())
                    if (found != null)
                    {
                        var json = found.toJson();
                        servicePointFeatures.remove(found);
                        found = Feature.fromJson(json);
                        if (changeRequired is ServicePointServicedProperty)
                        {
                            Logging.V("Found service point or trade site for ServicePointAndTradeSiteGuid=${changeRequired.ServicePointAndTradeSiteGuid}, will change 'serviced' property to ${changeRequired.Serviced}");
                            found.removeProperty("serviced");
                            found.addBooleanProperty("serviced", changeRequired.Serviced);
                            count++;
                        }
                        else if (changeRequired is ServicePointReportedProperty)
                        {
                            Logging.V("Found service point or trade site for ServicePointAndTradeSiteGuid=${changeRequired.ServicePointAndTradeSiteGuid}, will change 'reported' property to ${changeRequired.Reported}");
                            found.removeProperty("reported");
                            found.addBooleanProperty("reported", changeRequired.Reported);
                            count++;
                        }
                        else if (changeRequired is ServicePointActionedProperty)
                        {
                            Logging.V("Found service point or trade site for ServicePointAndTradeSiteGuid=${changeRequired.ServicePointAndTradeSiteGuid}, will change 'actioned' property to ${changeRequired.Actioned}");
                            found.removeProperty("actioned");
                            found.addBooleanProperty("actioned", changeRequired.Actioned);
                            count++;
                        }
                        else if (changeRequired is ServicePointHighlightedProperty)
                        {
                            Logging.V("Found service point or trade site for ServicePointAndTradeSiteGuid=${changeRequired.ServicePointAndTradeSiteGuid}, will change 'highlighted' property to ${changeRequired.Highlighted}");
                            found.removeProperty("highlight");
                            found.addBooleanProperty("highlight", changeRequired.Highlighted);
                            count++;
                        }
                        else if (changeRequired is ServicePointProximityProperty)
                        {
                            Logging.V("Found service point or trade site for ServicePointAndTradeSiteGuid=${changeRequired.ServicePointAndTradeSiteGuid}, will change 'proximity' property to ${changeRequired.InProximity}");
                            found.removeProperty("proximity");
                            found.addBooleanProperty("proximity", changeRequired.InProximity);
                            count++;
                        }
                        servicePointFeatures.add(found);
                        featuresToChange.add(found);
                    }
                    else
                    {
                        countFail++;
                        if (countFail < 5)
                        {
                            Logging.W("Attempt to change property for an unknown service point or trade site ServicePointAndTradeSiteGuid=${changeRequired.ServicePointAndTradeSiteGuid}");
                        }
                    }
                }
                UpdateSomeFeaturesInSource(featuresToChange, id);
            }
            catch (ex: Exception)
            {
                Logging.E("Exception = ${ex.message}")
                if (ConditionalCompile.throwAfterLoggingException) throw ex;
            }
            finally
            {
                lock.unlock();
            }
        }
        else
        {
            Logging.W("Timeout waiting for lock.");
        }
        return count;
    }

    internal fun GetLocationsForServicePointsAndTradeSites(includeServicePointsInFullExtent: Boolean): List<Point>
    {
        var locations = mutableListOf<Point>();
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                for (index in servicePointFeatures.indices)
                {
                    val isTrade = servicePointFeatures[index].getBooleanProperty("istrade")?: false;
                    if (includeServicePointsInFullExtent == true || isTrade == true)
                    {
                        var location = servicePointFeatures[index].geometry() as Point;
                        locations.add(location);
                    }
                }
            }
            catch (ex: Exception)
            {
                Logging.E("Exception = ${ex.message}")
                if (ConditionalCompile.throwAfterLoggingException) throw ex;
            }
            finally
            {
                lock.unlock();
            }
        }
        else
        {
            Logging.W("Timeout waiting for lock.");
        }
        return locations;
    }

    internal fun GetLocationForServicePointsAndTradeSite(servicePointAndTradeSiteFeature: ServicePointAndTradeSiteFeature): Point?
    {
        var location: Point? = null;
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {

                var foundServicePointAndTradeSiteFeature = GetServicePointFeatureById(servicePointAndTradeSiteFeature.UniqueGuid.toString())
                if (foundServicePointAndTradeSiteFeature != null)
                {
                    location = foundServicePointAndTradeSiteFeature.geometry() as Point;
                }
            }
            catch (ex: Exception)
            {
                Logging.E("Exception = ${ex.message}")
                if (ConditionalCompile.throwAfterLoggingException) throw ex;
            }
            finally
            {
                lock.unlock();
            }
        }
        else
        {
            Logging.W("Timeout waiting for lock.");
        }
        return location;
    }

    /**
     * Create a tapped map feature from a mapbox geojson feature
     */
    internal fun CreateServicePointAndTradeSiteFeature(newLocation: GeographicPosition, feature: Feature): ServicePointAndTradeSiteFeature?
    {
        if (feature.geometry() is Point && feature.properties() != null)
        {
            var screenLocation = mapView.mapboxMap
                .pixelForCoordinate(Point.fromLngLat(newLocation.Longitude, newLocation.Latitude));

            var properties: JsonObject = feature.properties()!!;

            var comments = false;
            var commentsProperty = properties.get("comments");
            if (commentsProperty != null && commentsProperty.isJsonNull == false)
            {
                comments = commentsProperty.asBoolean;
            }

            var reported = false;
            var reportedProperty = properties.get("reported");
            if (reportedProperty != null && reportedProperty.isJsonNull == false)
            {
                reported = reportedProperty.asBoolean;
            }

            var hasAction = false;
            var hasActionProperty = properties.get("hasaction");
            if (hasActionProperty != null && hasActionProperty.isJsonNull == false)
            {
                hasAction = hasActionProperty.asBoolean;
            }

            var action = "";
            var actionProperty = properties.get("action");
            if (actionProperty != null && actionProperty.isJsonNull == false)
            {
                action = actionProperty.asString;
            }

            var actioned = false;
            var actionedProperty = properties.get("actioned");
            if (actionedProperty != null && actionedProperty.isJsonNull == false)
            {
                actioned = actionedProperty.asBoolean;
            }

            var stopped = false;
            var stoppedProperty = properties.get("isstopped");
            if (stoppedProperty != null && stoppedProperty.isJsonNull == false)
            {
                stopped = stoppedProperty.asBoolean;
            }

            var isTrade = false;
            var isTradeProperty = properties.get("istrade");
            if (isTradeProperty != null && isTradeProperty.isJsonNull == false)
            {
                isTrade = isTradeProperty.asBoolean;
            }

            var note = "";
            var noteProperty = properties.get("note");
            if (noteProperty != null && noteProperty.isJsonNull == false)
            {
                note = noteProperty.asString;
            }

            var serviceComment = "";
            var serviceCommentProperty = properties.get("servicecomment");
            if (serviceCommentProperty != null && serviceCommentProperty.isJsonNull == false)
            {
                serviceComment = serviceCommentProperty.asString;
            }

            var nameOrNumber = "";
            var nameOrNumberProperty = properties.get("number");
            if (nameOrNumberProperty != null && nameOrNumberProperty.isJsonNull == false)
            {
                nameOrNumber = nameOrNumberProperty.asString;
            }

            var flat = "";
            var flatProperty = properties.get("flat");
            if (flatProperty != null && flatProperty.isJsonNull == false)
            {
                flat = flatProperty.asString;
            }

            var tappedMapFeature = ServicePointAndTradeSiteFeature();
            tappedMapFeature.UniqueGuid = UUID.fromString(feature.id()); // ??? TBD repeated below.
            tappedMapFeature.Comments = comments;
            tappedMapFeature.Reported = reported;
            tappedMapFeature.Note = note;
            tappedMapFeature.ServiceComment = serviceComment;
            tappedMapFeature.HasAction = hasAction;
            tappedMapFeature.Action = action;
            tappedMapFeature.Actioned = actioned;
            tappedMapFeature.IsTrade = isTrade;
            tappedMapFeature.IsStopped = stopped;
            tappedMapFeature.NameOrNumber = nameOrNumber;
            tappedMapFeature.Flat = flat;
            if (feature.id() != null)
            {
                tappedMapFeature.UniqueGuid = UUID.fromString(feature.id());
            }

            var servicedProperty = properties.get("serviced");
            if (servicedProperty != null && servicedProperty.isJsonNull == false)
            {
                tappedMapFeature.Serviced = servicedProperty.asBoolean;
            }

            tappedMapFeature.TappedPosition = MapPosition(GeoPosition = GeographicPosition(Longitude = newLocation.Longitude, Latitude = newLocation.Latitude), ScreenPosition = ScreenPosition(X = screenLocation.x, Y = screenLocation.y, mapView.width, mapView.height));
            var LongitudeProperty = properties.get("longitude");
            if (LongitudeProperty != null && LongitudeProperty.isJsonNull == false)
            {
                tappedMapFeature.Longitude = LongitudeProperty.asDouble;
            }
            else
            {
                if (feature.geometry() is Point)
                {
                    tappedMapFeature.Longitude = (feature.geometry() as Point).longitude();
                }
            }

            var LatitudeProperty = properties.get("latitude");
            if (LatitudeProperty != null && LatitudeProperty.isJsonNull == false)
            {
                tappedMapFeature.Latitude = LatitudeProperty.asDouble;
            }
            else
            {
                if (feature.geometry() is Point)
                {
                    tappedMapFeature.Latitude = (feature.geometry() as Point).latitude();
                }
            }
            Logging.D("ServicePointAndTradeSiteFeature with UniqueGuid=${tappedMapFeature.UniqueGuid}, Flat=${tappedMapFeature.Flat}, NameOrNumber=${tappedMapFeature.NameOrNumber}, Address=${tappedMapFeature.Address}, IsTrade=${tappedMapFeature.IsTrade}, Note=${tappedMapFeature.Note}, Comments=${tappedMapFeature.Comments}, ServiceComment=${tappedMapFeature.ServiceComment}, Serviced=${tappedMapFeature.Serviced}, Action=${tappedMapFeature.Action}, Actioned=${tappedMapFeature.Actioned}, IsStopped=${tappedMapFeature.IsStopped}, Reported=${tappedMapFeature.Reported}.");
            return tappedMapFeature;
        }
        else
        {
            return null;
        }
    }

    /**
     * Find and return a service point feature by id.
     */
    private fun GetServicePointFeatureById(subjectId: String): Feature?
    {
        if(servicePointFeatures.isNullOrEmpty()) return null;
        return servicePointFeatures.firstOrNull{ x -> x.id() == subjectId};
    }

    /**
     * Attempt to load bitmap icons into Mapbox for symbols
     */
    private fun LoadSymbolImages(theStyle: Style)
    {
        val redYellowCircleSegments = ContextCompat.getDrawable(theContext, R.drawable.redyellowcirclesegments80px)?.toBitmap(80, 80)!!
        theStyle.addImage(imageId_RedYellowCircleSegments, redYellowCircleSegments);
        val greyCircleSegments = ContextCompat.getDrawable(theContext, R.drawable.greycirclesegments80px)?.toBitmap(80, 80)!!
        theStyle.addImage(imageId_GreyCircleSegments, greyCircleSegments);
        val orangeCross = ContextCompat.getDrawable(theContext, R.drawable.orangecross40px)?.toBitmap(64, 64)!!
        theStyle.addImage(imageId_OrangeCrossSymbol, orangeCross);
    }

    private fun CheckIfSourceExistsAlready(theStyle: Style): Boolean {
        val testGeoJsonSource: Source? = theStyle.getSource(sourceId);
        if (testGeoJsonSource != null) {
            // We shouldn't find ourselves here but just in case...
            Logging.W("For some reason we already have a source of this name.");

            return true;
        }
        return false;
    }

    private fun AddFeaturesToSource(features: List<Feature>) {
        var featureCollection = FeatureCollection.fromFeatures(features);

        val geoJsonSource = geoJsonSource(sourceId)
        {
            featureCollection(featureCollection)
        }
        mapView.mapboxMap.style?.addSource(geoJsonSource)

    }

    private fun UpdateAllFeaturesInSource(features: List<Feature>, id: String? = null)
    {
        //Handler(Looper.getMainLooper()).post(
        //{
            if (id.isNullOrEmpty() == false)
            {
                theMapStyleListener.LogWhenDataSourceUpdated(id);
            }
            var featureCollection = FeatureCollection.fromFeatures(features.toMutableList());
            var geoJsonSource: GeoJsonSource? =
                mapView.mapboxMap.style?.getSourceAs<GeoJsonSource>(sourceId);
            if (id.isNullOrEmpty() == false)
            {
                geoJsonSource?.featureCollection(featureCollection, id);
            }
            else
            {
                geoJsonSource?.featureCollection(featureCollection);
            }
            Logging.V(
                "Will update ${features.count()} servicePointFeatures, number in featureCollection=${
                    featureCollection.features()?.count()
                }"
            );
        //});
    }

    private fun UpdateSomeFeaturesInSource(features: List<Feature>, id: String? = null)
    {
        //Handler(Looper.getMainLooper()).post(
            //{
                if (features.count() > 0)
                {
                    if (id.isNullOrEmpty() == false)
                    {
                        theMapStyleListener.LogWhenDataSourceUpdated(id);
                    }
                    var geoJsonSource: GeoJsonSource? = mapView.mapboxMap.style?.getSourceAs<GeoJsonSource>(sourceId);
                    if (id.isNullOrEmpty() == false)
                    {
                        geoJsonSource?.updateGeoJSONSourceFeatures(features, id);
                    }
                    else
                    {
                        geoJsonSource?.updateGeoJSONSourceFeatures(features);
                    }
                    Logging.V("Will update ${features.count()} servicePointFeatures.");
                }
            //});
    }
    private fun AddSomeFeaturesInSource(features: List<Feature>, id: String? = null)
    {
        //Handler(Looper.getMainLooper()).post(
            //{
                if (features.count() > 0)
                {
                    if (id.isNullOrEmpty() == false)
                    {
                        theMapStyleListener.LogWhenDataSourceUpdated(id);
                    }
                    var geoJsonSource: GeoJsonSource? = mapView.mapboxMap.style?.getSourceAs<GeoJsonSource>(sourceId);
                    if (id.isNullOrEmpty() == false)
                    {
                        geoJsonSource?.addGeoJSONSourceFeatures(features, id);
                    }
                    else
                    {
                        geoJsonSource?.addGeoJSONSourceFeatures(features);
                    }

                    Logging.V("Will add ${features.count()} servicePointFeatures.");
                }
            //});
    }

    private fun RemoveSomeFeaturesInSource(features: List<Feature>, id: String? = null)
    {
        //Handler(Looper.getMainLooper()).post(
            //{
                if (features.count() > 0)
                {
                    if (id.isNullOrEmpty() == false)
                    {
                        theMapStyleListener.LogWhenDataSourceUpdated(id);
                    }
                    var geoJsonSource: GeoJsonSource? = mapView.mapboxMap.style?.getSourceAs<GeoJsonSource>(sourceId);
                    var featureIds = mutableListOf<String>();
                    for (feature in features)
                    {
                        featureIds.add(feature.id().toString());
                    }
                    if (id.isNullOrEmpty() == false)
                    {
                        geoJsonSource?.removeGeoJSONSourceFeatures(featureIds, id);
                    }
                    else
                    {
                        geoJsonSource?.removeGeoJSONSourceFeatures(featureIds);
                    }

                    Logging.V("Will remove ${features.count()} servicePointFeatures.");
                }
            //});
    }

    public fun SetServicePointOpacity(opacity: Double): Boolean
    {
        Handler(Looper.getMainLooper()).post(
        {
            ServicePointOpacity = opacity;

            reportLayerRoute.circleStrokeOpacity(ServicePointOpacity);
            actionLayerRoute.circleOpacity(ServicePointOpacity);
            actionedLayerRoute.circleOpacity(ServicePointOpacity);
            unservicedLargeLayerRoute.circleOpacity(ServicePointOpacity);
            servicedLargeLayerRoute.circleOpacity(ServicePointOpacity);
            commentSymbolLayer1Route.iconOpacity(ServicePointOpacity);
            commentServicedSymbolLayer1Route.iconOpacity(ServicePointOpacity);
            unservicedLayerRoute.circleOpacity(ServicePointOpacity);
            servicedLayerRoute.circleOpacity(ServicePointOpacity);
            textSymbolLayer.textOpacity(1.0 * ServicePointOpacity);
            textSymbolLayer2.textOpacity(1.0 * ServicePointOpacity);

        });
        return true;
    }

}

public class ServicePointProximityProperty : ServicePointAndTradeSitePropertyBase()
{
    public var InProximity: Boolean = false;
}

public class ServicePointHighlightedProperty : ServicePointAndTradeSitePropertyBase()
{
    public var Highlighted: Boolean = false;
}
public class ServicePointCommentsProperty : ServicePointAndTradeSitePropertyBase()
{
    public var CommentsPresent: Boolean = false;
}

public class ServicePointServicedProperty : ServicePointAndTradeSitePropertyBase()
{
    public var Serviced: Boolean = false;
}

public class ServicePointReportedProperty : ServicePointAndTradeSitePropertyBase()
{
    public var Reported: Boolean = false;
}

public class ServicePointActionedProperty : ServicePointAndTradeSitePropertyBase()
{
    public var Actioned: Boolean = false;
}

public abstract class ServicePointAndTradeSitePropertyBase : MapFeaturePropertyBase()
{
    public lateinit var ServicePointAndTradeSiteGuid: UUID
}
