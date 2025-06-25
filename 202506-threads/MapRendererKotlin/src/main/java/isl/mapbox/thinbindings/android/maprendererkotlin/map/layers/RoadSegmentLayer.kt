package isl.mapbox.thinbindings.android.maprendererkotlin.map.layers

import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.gson.JsonObject
import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiLineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextJustify
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
import isl.mapbox.thinbindings.android.features.DirectionOfTravel
import isl.mapbox.thinbindings.android.features.DirectionsOfService
import isl.mapbox.thinbindings.android.features.ManoeuvreType
import isl.mapbox.thinbindings.android.features.NavigationSegmentFeature
import isl.mapbox.thinbindings.android.features.PathToTarget
import isl.mapbox.thinbindings.android.features.RoadSegmentFeature
import isl.mapbox.thinbindings.android.features.RoadSegmentFeatureType
import isl.mapbox.thinbindings.android.features.RouteRoadSegmentFeature
import isl.mapbox.thinbindings.android.features.RouteRoadSegmentNonServiceFeature
import isl.mapbox.thinbindings.android.internal.Logging
import isl.mapbox.thinbindings.android.internal.MapStyleListener
import isl.mapbox.thinbindings.android.maprendererkotlin.misc.ConditionalCompile
import isl.mapbox.thinbindings.android.maprendererkotlin.misc.FeatureColours
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.LongLatExtent
import isl.mapbox.thinbindings.android.positions.MapPosition
import isl.mapbox.thinbindings.android.positions.ScreenPosition
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

// This class handles rendering of road segment.
// Important Note: RoadSegmentFeatures may also include RouteRoadSegmentNonServiceFeature and RouteRoadSegmentFeature which are derivatives.
public class RoadSegmentLayer(context: Context, theMapView: MapView, theMapStyleListener: MapStyleListener)
{
    private val mapView: MapView = theMapView
    private val theContext: Context = context
    private val theMapStyleListener: MapStyleListener = theMapStyleListener
    private val sourceId: String = "GeojsonSourceLines"

    private val imageId_RoadArrowBlue: String = "roadArrowBlue"
    private val imageId_RoadArrowGrey: String = "roadArrowGrey"
    private val imageId_RoadArrowGreen: String = "roadArrowGreen"
    private val imageId_ManoeuvreLocation: String = "manoeuvreLocation"

    private var lineFeatures: MutableList<Feature> = mutableListOf<Feature>()
    private lateinit var unservicedLineLayer: LineLayer
    private lateinit var directionArrowUnservicedLayer: SymbolLayer
    private lateinit var servicedLineLayer: LineLayer
    private lateinit var partiallyServicedUnderLineLayer: LineLayer
    private lateinit var partiallyServicedLineLayer: LineLayer
    private lateinit var directionArrowServicedLayer: SymbolLayer
    private lateinit var reportLineLayerSide1: LineLayer
    private lateinit var reportLineLayerSide2: LineLayer
    private lateinit var commentLineLayer: LineLayer
    private lateinit var commentForegroundLineLayer: LineLayer
    private lateinit var manoeuvreRouteSegmentLineLayer: LineLayer
    private lateinit var manoeuvreAfterRouteSegmentLineLayer: LineLayer

    // Colour Definitons
    private val UnservicedColourExpression: Expression = FeatureColours.Blue
    private val ReportedColourExpression: Expression = FeatureColours.Blue
    private val CompletedSegmentColourExpression: Expression = FeatureColours.Grey
    private val PathToTargetColourExpression: Expression = FeatureColours.Orange
    private val LabelColourExpression: Expression = FeatureColours.Orange


    private val CommentsYellowColourExpression: Expression = FeatureColours.Yellow
    private val CommentsRedColourExpression: Expression = FeatureColours.Red

    private val NavigationColourExpression: Expression = FeatureColours.Green
    private val ManoeuvreColourExpression: Expression = FeatureColours.BoldOrange

    private val SnappedFeatureColourExpression: Expression = FeatureColours.Purple
    private val HighlightColourExpression: Expression = FeatureColours.Yellow


    private val RoadStartZoom: Double = 10.0
    private val RoadLowZoom: Double = 11.0
    private val RoadMidZoom: Double = 16.0
    private val RoadHighZoom: Double = 22.0

    private val PathStartZoom: Double = 6.0
    private val PathLowZoom: Double = 8.0
    private val PathMidZoom: Double = 16.0
    private val PathHighZoom: Double = 22.0

    private val RoadWidthAtStartZoom: Double = 0.0;
    private val RoadWidthAtLowZoom: Double = 2.0;
    private val RoadWidthAtMidZoom: Double = 5.0;
    private val RoadWidthAtHighZoom: Double = 5.0;

    private val RoadAdornmentStartZoom: Double = 12.0;
    private val RoadAdornmentLowZoom: Double = 13.0;
    private val RoadAdornmentMidZoom: Double = 16.0;
    private val RoadAdornmentHighZoom: Double = 22.0;

    private var DriveOnRight = true;
    private var DriveOnLeft = true;
    private var Opacity: Double = 1.0;

    private val density: Float;

    private val lock = ReentrantLock();
    private val timeout: Long = 100;
    private val timeoutUnits = TimeUnit.MILLISECONDS;

    init {
        density = Resources.getSystem().displayMetrics.density;
    }

    /**
     * Initialise source and styling layers on the map
     */
    internal fun CreateRoadSegmentSourceLayersAndFilters(theStyle: Style) {
        try {
            Logging.D("Will attempt to create layers and filters for ${theStyle}.")
            if (CheckIfSourceExistsAlready(theStyle) == true)
            {
                return;
            }
            AddFeaturesToSource(lineFeatures);

            val lineDashes = listOf<Double>(1.0, 1.0)
            val lineDashesShort = listOf<Double>(0.5, 1.5)
            val lineDashesLong = listOf<Double>(1.5, 1.5)
            //val lineDashesLongInverse = listOf<Double>(0.0, 1.5, 1.5)

            LoadSymbolImages(theStyle);

            directionArrowUnservicedLayer = SymbolLayer("roadSegment_directionArrowUnservicedLayer", sourceId);
            directionArrowUnservicedLayer.filter(Expression.all(
                Expression.gt(Expression.zoom(), literal(12.9)),
                Expression.all(
                    Expression.neq(Expression.get("serviced"), literal(true)),
                    Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.RouteRoad.toString())), // ???
                ),
            ))
            directionArrowUnservicedLayer.iconImage(imageId_RoadArrowBlue)
            directionArrowUnservicedLayer.iconOpacity(Opacity);
            directionArrowUnservicedLayer.symbolPlacement(SymbolPlacement.LINE_CENTER)
            directionArrowUnservicedLayer.symbolSpacing(literal(0.50))
            directionArrowUnservicedLayer.iconIgnorePlacement(literal(true))
            directionArrowUnservicedLayer.iconAllowOverlap(literal(true))
            directionArrowUnservicedLayer.iconRotate(Expression.switchCase(Expression.eq(Expression.get("directiontravel"), literal("Against")), literal(180.0), literal(0.0)))
            directionArrowUnservicedLayer.iconSize(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0 * density) }
                stop { literal(RoadAdornmentLowZoom); literal(0.05 * density) }
                stop { literal(RoadAdornmentMidZoom); literal(0.45 * density) }
                stop { literal(RoadAdornmentHighZoom); literal(0.45 * density) }
            })

            val directionArrowNavigationLayer = SymbolLayer("roadSegment_directionArrowNavigationLayer", sourceId);
            directionArrowNavigationLayer.filter(Expression.all(
                Expression.gt(Expression.zoom(), literal(12.9)),
                Expression.all(
                    Expression.neq(Expression.get("completed"), literal(true)),
                    Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.A2BNavigation.toString())), // ???
                ),
            ))
            directionArrowNavigationLayer.iconImage(imageId_RoadArrowGreen)
            directionArrowNavigationLayer.iconOpacity(Opacity);
            directionArrowNavigationLayer.symbolPlacement(SymbolPlacement.LINE_CENTER)
            directionArrowNavigationLayer.symbolSpacing(literal(0.50))
            directionArrowNavigationLayer.iconIgnorePlacement(literal(true))
            directionArrowNavigationLayer.iconAllowOverlap(literal(true))
            directionArrowNavigationLayer.iconRotate(Expression.switchCase(Expression.eq(Expression.get("directiontravel"), literal("Against")), literal(180.0), literal(0.0)))
            directionArrowNavigationLayer.iconSize(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0 * density) }
                stop { literal(RoadAdornmentLowZoom); literal(0.05 * density) }
                stop { literal(RoadAdornmentMidZoom); literal(0.45 * density) }
                stop { literal(RoadAdornmentHighZoom); literal(0.45 * density) }
            })

            val nonServiceLineLayer = LineLayer("roadSegment_nonServiceLineLayer", sourceId);
            nonServiceLineLayer.filter(Expression.any(
                Expression.all(
                    Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.RouteRoadNonService.toString()))
                )
            ));
            nonServiceLineLayer.lineDasharray(lineDashesShort);
            nonServiceLineLayer.lineCap(LineCap.BUTT);
            nonServiceLineLayer.lineJoin(LineJoin.ROUND);
            nonServiceLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadStartZoom); literal(RoadWidthAtStartZoom) }
                stop { literal(RoadLowZoom); literal(RoadWidthAtLowZoom) }
                stop { literal(RoadMidZoom); literal(RoadWidthAtMidZoom) }
                stop { literal(RoadHighZoom); literal(RoadWidthAtHighZoom) }
            });
            nonServiceLineLayer.lineColor(CompletedSegmentColourExpression);

            partiallyServicedUnderLineLayer = LineLayer("roadSegment_partiallyServicedUnderLineLayer", sourceId);  // THIS ONE
            partiallyServicedUnderLineLayer.filter(Expression.any(
                Expression.all(
                    Expression.eq(Expression.get("serviced"), literal(true)),
                    Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.RouteRoad.toString()))
                )
            ));
            partiallyServicedUnderLineLayer.lineCap(LineCap.BUTT);
            partiallyServicedUnderLineLayer.lineJoin(LineJoin.ROUND);
            partiallyServicedUnderLineLayer.lineOpacity(Opacity);
            partiallyServicedUnderLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadStartZoom); literal(RoadWidthAtStartZoom) }
                stop { literal(RoadLowZoom); literal(RoadWidthAtLowZoom) }
                stop { literal(RoadMidZoom); literal(RoadWidthAtMidZoom) }
                stop { literal(RoadHighZoom); literal(RoadWidthAtHighZoom) }
            });
            partiallyServicedUnderLineLayer.lineColor(CompletedSegmentColourExpression)

            unservicedLineLayer = LineLayer("roadSegment_unservicedLineLayer", sourceId);
            unservicedLineLayer.filter(Expression.any(
                Expression.all(
                    Expression.eq(Expression.get("serviced"), literal(false)),
                    Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.RouteRoad.toString()))
                )
            ));
            unservicedLineLayer.lineCap(LineCap.BUTT);
            unservicedLineLayer.lineJoin(LineJoin.ROUND);
            unservicedLineLayer.lineOpacity(Opacity);
            unservicedLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadStartZoom); literal(RoadWidthAtStartZoom) }
                stop { literal(RoadLowZoom); literal(RoadWidthAtLowZoom) }
                stop { literal(RoadMidZoom); literal(RoadWidthAtMidZoom) }
                stop { literal(RoadHighZoom); literal(RoadWidthAtHighZoom) }
            });
            unservicedLineLayer.lineColor(UnservicedColourExpression);

            partiallyServicedLineLayer = LineLayer("roadSegment_partiallyServicedLineLayer", sourceId);
            partiallyServicedLineLayer.filter(Expression.any(
                Expression.all(
                    Expression.eq(Expression.get("serviced"), literal(true)),
                    Expression.lt(Expression.get("passescompleted"), Expression.get("passestotal")),
                    Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.RouteRoad.toString()))
                )
            ));
            partiallyServicedLineLayer.lineDasharray(lineDashesLong);
            partiallyServicedLineLayer.lineCap(LineCap.BUTT);
            partiallyServicedLineLayer.lineJoin(LineJoin.ROUND);
            partiallyServicedLineLayer.lineOpacity(Opacity);
            partiallyServicedLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadStartZoom); literal(RoadWidthAtStartZoom) }
                stop { literal(RoadLowZoom); literal(RoadWidthAtLowZoom) }
                stop { literal(RoadMidZoom); literal(RoadWidthAtMidZoom) }
                stop { literal(RoadHighZoom); literal(RoadWidthAtHighZoom) }
            });
            partiallyServicedLineLayer.lineColor(CompletedSegmentColourExpression)

            servicedLineLayer = LineLayer("roadSegment_servicedLineLayer", sourceId);
            servicedLineLayer.filter(Expression.any(
                Expression.all(
                    Expression.eq(Expression.get("serviced"), literal(true)),
                    Expression.eq(Expression.get("passescompleted"), Expression.get("passestotal")),
                    Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.RouteRoad.toString()))
                )
            ));
            servicedLineLayer.lineCap(LineCap.BUTT);
            servicedLineLayer.lineJoin(LineJoin.ROUND);
            servicedLineLayer.lineOpacity(Opacity);
            servicedLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadStartZoom); literal(RoadWidthAtStartZoom) }
                stop { literal(RoadLowZoom); literal(RoadWidthAtLowZoom) }
                stop { literal(RoadMidZoom); literal(RoadWidthAtMidZoom) }
                stop { literal(RoadHighZoom); literal(RoadWidthAtHighZoom) }
            });
            servicedLineLayer.lineColor(CompletedSegmentColourExpression)

            directionArrowServicedLayer = SymbolLayer("roadSegment_directionArrowServicedLayer", sourceId);
            directionArrowServicedLayer.filter(Expression.all(
                Expression.gt(Expression.zoom(), literal(12.9)),
                Expression.any(
                    Expression.eq(Expression.get("serviced"), literal(true)),
                    Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.RouteRoadNonService.toString())),
                ),
            ));
            directionArrowServicedLayer.iconImage(imageId_RoadArrowGrey);
            directionArrowServicedLayer.iconOpacity(Opacity);
            directionArrowServicedLayer.symbolPlacement(SymbolPlacement.LINE_CENTER);
            directionArrowServicedLayer.symbolSpacing(literal(0.50));
            directionArrowServicedLayer.iconIgnorePlacement(literal(true));
            directionArrowServicedLayer.iconAllowOverlap(literal(true));
            directionArrowServicedLayer.iconRotate(Expression.switchCase(Expression.eq(Expression.get("directiontravel"), literal("Against")), literal(180.0), literal(0.0)));
            directionArrowServicedLayer.iconSize(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0 * density) }
                stop { literal(RoadAdornmentLowZoom); literal(0.05 * density) }
                stop { literal(RoadAdornmentMidZoom); literal(0.45 * density) }
                stop { literal(RoadAdornmentHighZoom); literal(0.45 * density) }
            })

            commentLineLayer = LineLayer("roadSegment_commentLayer", sourceId);
            commentLineLayer.filter(Expression.all(
                Expression.eq(Expression.get("comments"), literal(true)),
                Expression.gt(Expression.zoom(), literal(14.0)),
                Expression.neq(Expression.get("type"), literal(RoadSegmentFeatureType.Road.toString()))
            ));
            commentLineLayer.lineCap(LineCap.BUTT);
            commentLineLayer.lineJoin(LineJoin.ROUND);
            commentLineLayer.lineOpacity(Opacity);
            commentLineLayer.lineGapWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(4.0) }
                stop { literal(RoadAdornmentMidZoom); literal(6.0) }
                stop { literal(RoadAdornmentHighZoom); literal(6.0) }
            });
            commentLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(1.5) }
                stop { literal(RoadAdornmentMidZoom); literal(3.0) }
                stop { literal(RoadAdornmentHighZoom); literal(3.0) }
            });
            commentLineLayer.lineColor(CommentsYellowColourExpression);

            val roadCommentlineLayer = LineLayer("roadSegment_roadCommentLayer", sourceId);
            roadCommentlineLayer.filter(Expression.all(
                Expression.eq(Expression.get("comments"), literal(true)),
                Expression.gt(Expression.zoom(), literal(14.0)),
                Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.Road.toString()))
            ));
            roadCommentlineLayer.lineCap(LineCap.BUTT);
            roadCommentlineLayer.lineJoin(LineJoin.ROUND);
            roadCommentlineLayer.lineGapWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(4.0) }
                stop { literal(RoadAdornmentMidZoom); literal(6.0) }
                stop { literal(RoadAdornmentHighZoom); literal(6.0) }
            });
            roadCommentlineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(1.5) }
                stop { literal(RoadAdornmentMidZoom); literal(3.0) }
                stop { literal(RoadAdornmentHighZoom); literal(3.0) }
            });
            roadCommentlineLayer.lineColor(CommentsYellowColourExpression);

            commentForegroundLineLayer = LineLayer("roadSegment_commentForegroundLayer", sourceId);
            commentForegroundLineLayer.filter(Expression.all(
                Expression.eq(Expression.get("comments"), literal(true)),
                Expression.gt(Expression.zoom(), literal(14.0)),
                Expression.neq(Expression.get("type"), literal(RoadSegmentFeatureType.Road.toString()))
            ));
            commentForegroundLineLayer.lineDasharray(lineDashes);
            commentForegroundLineLayer.lineCap(LineCap.BUTT);
            commentForegroundLineLayer.lineJoin(LineJoin.ROUND);
            commentForegroundLineLayer.lineOpacity(Opacity);
            commentForegroundLineLayer.lineGapWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(4.0) }
                stop { literal(RoadAdornmentMidZoom); literal(6.0) }
                stop { literal(RoadAdornmentHighZoom); literal(6.0) }
            });
            commentForegroundLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(1.5) }
                stop { literal(RoadAdornmentMidZoom); literal(3.0) }
                stop { literal(RoadAdornmentHighZoom); literal(3.0) }
            });
            commentForegroundLineLayer.lineColor(CommentsRedColourExpression)

            val roadCommentForegroundLineLayer = LineLayer("roadSegment_roadCommentForegroundLayer", sourceId);
            roadCommentForegroundLineLayer.filter(Expression.all(
                Expression.eq(Expression.get("comments"), literal(true)),
                Expression.gt(Expression.zoom(), literal(14.0)),
                Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.Road.toString()))
            ));
            roadCommentForegroundLineLayer.lineDasharray(lineDashes);
            roadCommentForegroundLineLayer.lineCap(LineCap.BUTT);
            roadCommentForegroundLineLayer.lineJoin(LineJoin.ROUND);
            roadCommentForegroundLineLayer.lineGapWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(4.0) }
                stop { literal(RoadAdornmentMidZoom); literal(6.0) }
                stop { literal(RoadAdornmentHighZoom); literal(6.0) }
            });
            roadCommentForegroundLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(1.5) }
                stop { literal(RoadAdornmentMidZoom); literal(3.0) }
                stop { literal(RoadAdornmentHighZoom); literal(3.0) }
            });
            roadCommentForegroundLineLayer.lineColor(CommentsRedColourExpression)

            val highlightLineLayer = LineLayer("highlightLineLayer", sourceId);
            highlightLineLayer.filter(Expression.eq(Expression.get("highlight"), literal(true)));
            highlightLineLayer.lineCap(LineCap.BUTT);
            highlightLineLayer.lineJoin(LineJoin.ROUND);
            highlightLineLayer.lineOpacity(literal(0.5));
            highlightLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(10.0) }
                stop { literal(RoadAdornmentMidZoom); literal(24.0) }
                stop { literal(RoadAdornmentHighZoom); literal(24.0) }
            });
            highlightLineLayer.lineColor(HighlightColourExpression);

            val snappedLineLayer = LineLayer("snappedLineLayer", sourceId);
            snappedLineLayer.filter(Expression.all(
                Expression.eq(Expression.get("snapped"), literal(true)),
                Expression.eq(Expression.get("directiontravel"), literal("With")),
                Expression.gt(Expression.zoom(), literal(14.0))
            ));
            snappedLineLayer.lineCap(LineCap.BUTT);
            snappedLineLayer.lineJoin(LineJoin.ROUND);
            //snappedLineLayer.lineOpacity(literal(0.2));
            snappedLineLayer.lineWidth(Expression.interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(1.0) }
                stop { literal(RoadAdornmentMidZoom); literal(2.0) }
                stop { literal(RoadAdornmentHighZoom); literal(2.0) }
            });
            snappedLineLayer.lineOffset(Expression.interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(-9.0) }
                stop { literal(RoadAdornmentMidZoom); literal(-18.0) }
                stop { literal(RoadAdornmentHighZoom); literal(-18.0) }
            });
            snappedLineLayer.lineColor(SnappedFeatureColourExpression);

            val snappedLineLayerAgainst = LineLayer("snappedLineLayerAgainst", sourceId);
            snappedLineLayerAgainst.filter(Expression.all(
                Expression.eq(Expression.get("snapped"), literal(true)),
                Expression.eq(Expression.get("directiontravel"), literal("Against")),
                Expression.gt(Expression.zoom(), literal(14.0))
            ));
            snappedLineLayerAgainst.lineCap(LineCap.BUTT);
            snappedLineLayerAgainst.lineJoin(LineJoin.ROUND);
            //snappedLineLayerAgainst.lineOpacity(literal(0.2));
            snappedLineLayerAgainst.lineWidth(Expression.interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(1.0) }
                stop { literal(RoadAdornmentMidZoom); literal(2.0) }
                stop { literal(RoadAdornmentHighZoom); literal(2.0) }
            });
            snappedLineLayerAgainst.lineOffset(Expression.interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(9.0) }
                stop { literal(RoadAdornmentMidZoom); literal(18.0) }
                stop { literal(RoadAdornmentHighZoom); literal(18.0) }
            });
            snappedLineLayerAgainst.lineColor(SnappedFeatureColourExpression);

            manoeuvreRouteSegmentLineLayer = LineLayer("manoeuvreRouteSegment", sourceId);
            manoeuvreRouteSegmentLineLayer.filter(Expression.all(
                Expression.eq(Expression.get("manoeuvreRouteSegment"), literal("Upto")),
                Expression.gt(Expression.zoom(), literal(14.0))
                ));
            manoeuvreRouteSegmentLineLayer.lineCap(LineCap.BUTT);
            manoeuvreRouteSegmentLineLayer.lineJoin(LineJoin.BEVEL);
            manoeuvreRouteSegmentLineLayer.lineOpacity(0.5 * Opacity);
            manoeuvreRouteSegmentLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(12.0) }
                stop { literal(RoadAdornmentMidZoom); literal(20.0) }
                stop { literal(RoadAdornmentHighZoom); literal(20.0) }
            });
            manoeuvreRouteSegmentLineLayer.lineColor(ManoeuvreColourExpression)

            manoeuvreAfterRouteSegmentLineLayer = LineLayer("manoeuvreAfterRouteSegment", sourceId);
            manoeuvreAfterRouteSegmentLineLayer.filter(Expression.all(
                Expression.eq(Expression.get("manoeuvreRouteSegment"), literal("After")),
                Expression.gt(Expression.zoom(), literal(14.0))
            ));
            manoeuvreAfterRouteSegmentLineLayer.lineCap(LineCap.BUTT);
            manoeuvreAfterRouteSegmentLineLayer.lineJoin(LineJoin.BEVEL);
            manoeuvreAfterRouteSegmentLineLayer.lineOpacity(0.25 * Opacity);
            manoeuvreAfterRouteSegmentLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(12.0) }
                stop { literal(RoadAdornmentMidZoom); literal(20.0) }
                stop { literal(RoadAdornmentHighZoom); literal(20.0) }
            });
            manoeuvreAfterRouteSegmentLineLayer.lineColor(ManoeuvreColourExpression)

            reportLineLayerSide1 = LineLayer("reportLineLayerSide1", sourceId);
            reportLineLayerSide1.filter(Expression.all(
                Expression.eq(Expression.get("reported"), literal(true)),
                Expression.any(
                    Expression.all(Expression.eq(Expression.get("driveOnLeft"), literal(true)),
                        Expression.eq(Expression.get("directiontravel"), literal("With"))),
                    Expression.all(Expression.eq(Expression.get("driveOnRight"), literal(true)),
                        Expression.eq(Expression.get("directiontravel"), literal("Against"))),
                    Expression.eq(Expression.get("directionservice"), literal("Both"))
                ),
                Expression.gt(Expression.zoom(), literal(14.0))
            ));
            reportLineLayerSide1.lineCap(LineCap.BUTT);
            reportLineLayerSide1.lineJoin(LineJoin.ROUND);
            reportLineLayerSide1.lineOpacity(Opacity);
            reportLineLayerSide1.lineWidth(Expression.interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(1.0) }
                stop { literal(RoadAdornmentMidZoom); literal(2.0) }
                stop { literal(RoadAdornmentHighZoom); literal(2.0) }
            });
            reportLineLayerSide1.lineOffset(Expression.interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(-5.0) }
                stop { literal(RoadAdornmentMidZoom); literal(-10.0) }
                stop { literal(RoadAdornmentHighZoom); literal(-10.0) }
            });
            reportLineLayerSide1.lineColor(ReportedColourExpression);

            reportLineLayerSide2 = LineLayer("reportLineLayerSide2", sourceId);
            reportLineLayerSide2.filter(Expression.all(
                Expression.eq(Expression.get("reported"), literal(true)),
                Expression.any(
                    Expression.all(Expression.eq(Expression.get("driveOnLeft"), literal(true)),
                        Expression.eq(Expression.get("directiontravel"), literal("Against"))),
                    Expression.all(Expression.eq(Expression.get("driveOnRight"), literal(true)),
                        Expression.eq(Expression.get("directiontravel"), literal("With"))),
                    Expression.eq(Expression.get("directionservice"), literal("Both"))
                ),
                Expression.gt(Expression.zoom(), literal(14.0))
            ));
            reportLineLayerSide2.lineCap(LineCap.BUTT);
            reportLineLayerSide2.lineJoin(LineJoin.ROUND);
            reportLineLayerSide2.lineOpacity(Opacity);
            reportLineLayerSide2.lineWidth(Expression.interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(1.0) }
                stop { literal(RoadAdornmentMidZoom); literal(2.0) }
                stop { literal(RoadAdornmentHighZoom); literal(2.0) }
            });
            reportLineLayerSide2.lineOffset(Expression.interpolate
            {
                linear()
                zoom()
                stop { literal(RoadAdornmentStartZoom); literal(0.0) }
                stop { literal(RoadAdornmentLowZoom); literal(5.0) }
                stop { literal(RoadAdornmentMidZoom); literal(10.0) }
                stop { literal(RoadAdornmentHighZoom); literal(10.0) }
            });
            reportLineLayerSide2.lineColor(ReportedColourExpression);

            val pathToTargetLineLayer: LineLayer = LineLayer("roadSegment_pathToTargetLayer", sourceId);
            pathToTargetLineLayer.filter(Expression.all(
                Expression.eq(Expression.get("ispathtotarget"), literal(true)),
                Expression.gt(Expression.zoom(), literal(6.0))
            ));
            pathToTargetLineLayer.lineDasharray(lineDashes);
            pathToTargetLineLayer.lineCap(LineCap.BUTT);
            pathToTargetLineLayer.lineJoin(LineJoin.ROUND);
            pathToTargetLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(PathStartZoom); literal(RoadWidthAtStartZoom) }
                stop { literal(PathLowZoom); literal(RoadWidthAtLowZoom) }
                stop { literal(PathMidZoom); literal(RoadWidthAtMidZoom) }
                stop { literal(PathHighZoom); literal(RoadWidthAtHighZoom) }
            });
            pathToTargetLineLayer.lineColor(PathToTargetColourExpression)

            val navigationBeforeLineLayer = LineLayer("roadSegment_navigationBeforeLayer", sourceId);
            navigationBeforeLineLayer.filter(Expression.all(
                Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.A2BNavigation.toString())),
                Expression.eq(Expression.get("completed"), literal(false)),
                Expression.eq(Expression.get("a2bNavigationSegment"), literal(
                    A2BNavigationSegmentStageProperty.A2BNavigationStage.Before.toString())),
                Expression.gt(Expression.zoom(), literal(6.0))
            ));
            //navigationBeforelineLayer.lineDasharray(lineDashes);
            navigationBeforeLineLayer.lineCap(LineCap.BUTT);
            navigationBeforeLineLayer.lineJoin(LineJoin.ROUND);
            navigationBeforeLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(PathStartZoom); literal(RoadWidthAtStartZoom) }
                stop { literal(PathLowZoom); literal(RoadWidthAtLowZoom) }
                stop { literal(PathMidZoom); literal(RoadWidthAtMidZoom) }
                stop { literal(PathHighZoom); literal(RoadWidthAtHighZoom) }
            });
            navigationBeforeLineLayer.lineColor(NavigationColourExpression)

            val navigationAfterLineLayer = LineLayer("roadSegment_navigationAfterLayer", sourceId);
            navigationAfterLineLayer.filter(Expression.all(
                Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.A2BNavigation.toString())),
                Expression.eq(Expression.get("completed"), literal(false)),
                Expression.eq(Expression.get("a2bNavigationSegment"), literal(
                    A2BNavigationSegmentStageProperty.A2BNavigationStage.After.toString())),
                Expression.gt(Expression.zoom(), literal(6.0))
            ));
            //navigationAfterlineLayer.lineDasharray(lineDashes);
            navigationAfterLineLayer.lineCap(LineCap.BUTT);
            navigationAfterLineLayer.lineJoin(LineJoin.ROUND);
            navigationAfterLineLayer.lineOpacity(literal(1.0));
            navigationAfterLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(PathStartZoom); literal(RoadWidthAtStartZoom) }
                stop { literal(PathLowZoom); literal(RoadWidthAtLowZoom) }
                stop { literal(PathMidZoom); literal(RoadWidthAtMidZoom) }
                stop { literal(PathHighZoom); literal(RoadWidthAtHighZoom) }
            });
            navigationAfterLineLayer.lineColor(NavigationColourExpression)

            val navigationCompletedLineLayer = LineLayer("roadSegment_navigationCompletedLayer", sourceId);
            navigationCompletedLineLayer.filter(Expression.all(
                Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.A2BNavigation.toString())),
                Expression.eq(Expression.get("completed"), literal(true)),
                Expression.gt(Expression.zoom(), literal(6.0))
            ));
            navigationCompletedLineLayer.lineDasharray(lineDashes);
            navigationCompletedLineLayer.lineCap(LineCap.BUTT);
            navigationCompletedLineLayer.lineJoin(LineJoin.ROUND);
            navigationCompletedLineLayer.lineWidth(interpolate
            {
                linear()
                zoom()
                stop { literal(PathStartZoom); literal(RoadWidthAtStartZoom) }
                stop { literal(PathLowZoom); literal(RoadWidthAtLowZoom) }
                stop { literal(PathMidZoom); literal(RoadWidthAtMidZoom) }
                stop { literal(PathHighZoom); literal(RoadWidthAtHighZoom) }
            });
            navigationCompletedLineLayer.lineColor(CompletedSegmentColourExpression)

            val textSymbolLayer = SymbolLayer("roadSegment_text", sourceId)
            textSymbolLayer.textField(Expression.get("text"))
            // NOTE: it is essential that the font stack defined here matches one exactly in the map styles being used where we
            // will be using offline maps, otherwise the text and symbol will not render for that offline map.
            // see https://docs.mapbox.com/help/troubleshooting/mobile-offline/ and https://docs.mapbox.com/help/glossary/font-stack/
            textSymbolLayer.textFont(arrayListOf("DIN Offc Pro Regular", "Arial Unicode MS Regular"))
            textSymbolLayer.textColor(LabelColourExpression)
            textSymbolLayer.textOpacity(1.0)
            textSymbolLayer.textSize(16.0)
            textSymbolLayer.textOffset(arrayListOf(2.0, -0.5))
            //textSymbolLayer.textVariableAnchor(listOf<String>(TextAnchor.BOTTOM.value, TextAnchor.BOTTOM_RIGHT.value))
            textSymbolLayer.textAnchor(TextAnchor.BOTTOM)
            textSymbolLayer.textJustify(TextJustify.CENTER)
            textSymbolLayer.textIgnorePlacement(true)
            textSymbolLayer.textAllowOverlap(false)
            textSymbolLayer.textRotate(0.0)
            textSymbolLayer.textMaxWidth(15.0)
            // x,y textoffset to apply to centre of text. 1.0 appears similar to text height. +ve to right and down
            // Note that if text is used in combination with an image then if they're not far enough apart then you'll only see one.
            textSymbolLayer.filter(Expression.all(Expression.any(
                Expression.all(Expression.eq(Expression.get("driveOnLeft"), literal(true)),
                    Expression.eq(Expression.get("directiontravel"), literal("With"))),
                Expression.all(Expression.eq(Expression.get("driveOnRight"), literal(true)),
                    Expression.eq(Expression.get("directiontravel"), literal("Against"))),
                Expression.eq(Expression.get("type"), literal(RoadSegmentFeatureType.Road.toString()))
            ),
                Expression.gte(Expression.zoom(), literal(17.0))))

            val textSymbolLayerLower = SymbolLayer("roadSegment_text2", sourceId)
            textSymbolLayerLower.textField(Expression.get("text"))
            // NOTE: it is essential that the font stack defined here matches one exactly in the map styles being used where we
            // will be using offline maps, otherwise the text and symbol will not render for that offline map.
            // see https://docs.mapbox.com/help/troubleshooting/mobile-offline/ and https://docs.mapbox.com/help/glossary/font-stack/
            textSymbolLayerLower.textFont(arrayListOf("DIN Offc Pro Regular", "Arial Unicode MS Regular"))
            textSymbolLayerLower.textColor(LabelColourExpression)
            textSymbolLayerLower.textOpacity(1.0)
            textSymbolLayerLower.textSize(16.0)
            textSymbolLayerLower.textOffset(arrayListOf(2.0, +0.5))
            textSymbolLayerLower.textAnchor(TextAnchor.TOP)
            textSymbolLayer.textJustify(TextJustify.CENTER)
            textSymbolLayerLower.textIgnorePlacement(true)
            textSymbolLayerLower.textAllowOverlap(true)
            textSymbolLayerLower.textRotate(0.0)
            textSymbolLayerLower.textMaxWidth(15.0)
            // x,y textoffset to apply to centre of text. 1.0 appears similar to text height. +ve to right and down
            // Note that if text is used in combination with an image then if they're not far enough apart then you'll only see one.
            textSymbolLayerLower.filter(Expression.all(Expression.any(
                Expression.all(Expression.eq(Expression.get("driveOnLeft"), literal(true)),
                    Expression.eq(Expression.get("directiontravel"), literal("Against"))),
                Expression.all(Expression.eq(Expression.get("driveOnRight"), literal(true)),
                    Expression.eq(Expression.get("directiontravel"), literal("With"))),
            ),
                Expression.gte(Expression.zoom(), literal(17.0)),
                Expression.neq(Expression.get("driveOnLeft"), Expression.get("driveOnRight"))
            ),)


            val snappedSequenceTextLayer = SymbolLayer("snappedSegmentSequence_text", sourceId)
            snappedSequenceTextLayer.textField(Expression.get("sequence"))
            // NOTE: it is essential that the font stack defined here matches one exactly in the map styles being used where we
            // will be using offline maps, otherwise the text and symbol will not render for that offline map.
            // see https://docs.mapbox.com/help/troubleshooting/mobile-offline/ and https://docs.mapbox.com/help/glossary/font-stack/
            snappedSequenceTextLayer.textFont(arrayListOf("DIN Offc Pro Regular", "Arial Unicode MS Regular"))
            snappedSequenceTextLayer.textColor(SnappedFeatureColourExpression)
            snappedSequenceTextLayer.textOpacity(1.0)
            snappedSequenceTextLayer.textSize(30.0)

            snappedSequenceTextLayer.textOffset(listOf<Double>(-1.0, -1.0));
            snappedSequenceTextLayer.textAnchor(TextAnchor.CENTER);
            snappedSequenceTextLayer.textIgnorePlacement(true);
            snappedSequenceTextLayer.textAllowOverlap(true);
            snappedSequenceTextLayer.textMaxWidth(15.0);
            // x,y textoffset to apply to centre of text. 1.0 appears similar to text height. +ve to right and down
            // Note that if text is used in combination with an image then if they're not far enough apart then you'll only see one.
//            snappedSequenceTextLayer.filter(Expression.all(Expression.any(
//                Expression.all(Expression.eq(Expression.get("driveOnLeft"), literal(true)),
//                    Expression.eq(Expression.get("directiontravel"), literal("With"))),
//                Expression.all(Expression.eq(Expression.get("driveOnRight"), literal(true)),
//                    Expression.eq(Expression.get("directiontravel"), literal("Against"))),
//            ),
//                Expression.gte(Expression.zoom(), literal(17.0))))

            snappedSequenceTextLayer.filter(Expression.all(
                Expression.eq(Expression.get("snapped"), literal(true)),
                Expression.gt(Expression.zoom(), literal(14.0))
            ));


            // This first layer will be the bottom layer on the map
            theStyle.addLayer(textSymbolLayer);
            theStyle.addLayer(textSymbolLayerLower);
            theStyle.addLayer(snappedSequenceTextLayer);
            theStyle.addLayer(snappedLineLayer);
            theStyle.addLayer(snappedLineLayerAgainst);
            theStyle.addLayer(manoeuvreRouteSegmentLineLayer);
            theStyle.addLayer(manoeuvreAfterRouteSegmentLineLayer);
            theStyle.addLayer(highlightLineLayer);
            theStyle.addLayer(commentLineLayer);
            theStyle.addLayer(roadCommentlineLayer);
            theStyle.addLayer(commentForegroundLineLayer);
            theStyle.addLayer(roadCommentForegroundLineLayer);
            theStyle.addLayer(reportLineLayerSide1);
            theStyle.addLayer(reportLineLayerSide2);
            theStyle.addLayer(directionArrowServicedLayer);
            theStyle.addLayer(nonServiceLineLayer);

            theStyle.addLayer(partiallyServicedUnderLineLayer); // new
            theStyle.addLayer(unservicedLineLayer);
            theStyle.addLayer(partiallyServicedLineLayer); // new
            theStyle.addLayer(servicedLineLayer);
//            theStyle.addLayer(partiallyServicedLineLayerWith);
//            theStyle.addLayer(partiallyServicedLineLayerAgainst);
//            theStyle.addLayer(unservicedLineLayerWith);
//            theStyle.addLayer(unservicedLineLayerAgainst);

            theStyle.addLayer(directionArrowUnservicedLayer);
            theStyle.addLayer(pathToTargetLineLayer);
            theStyle.addLayer(navigationBeforeLineLayer);
            theStyle.addLayer(navigationAfterLineLayer);
            theStyle.addLayer(navigationCompletedLineLayer);
            theStyle.addLayer(directionArrowNavigationLayer);
            // This last layer will be the top layer on the map

        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    public fun SetSideOfRoad(driveOnRight: Boolean, driveOnLeft: Boolean)
    {
        DriveOnRight = driveOnRight;
        DriveOnLeft = driveOnLeft;
    }

    public fun SetRoadSegmentOpacity(opacity: Double)
    {
        Handler(Looper.getMainLooper()).post(
        {
            Opacity = opacity;

            unservicedLineLayer.lineOpacity(Opacity);
            directionArrowUnservicedLayer.iconOpacity(Opacity);

            servicedLineLayer.lineOpacity(Opacity);
            partiallyServicedUnderLineLayer.lineOpacity(Opacity);
            partiallyServicedLineLayer.lineOpacity(Opacity);
            directionArrowServicedLayer.iconOpacity(Opacity);

            reportLineLayerSide1.lineOpacity(Opacity);
            reportLineLayerSide2.lineOpacity(Opacity);

            commentForegroundLineLayer.lineOpacity(Opacity);
            commentLineLayer.lineOpacity(Opacity);

            manoeuvreRouteSegmentLineLayer.lineOpacity(0.5 * Opacity);
            manoeuvreAfterRouteSegmentLineLayer.lineOpacity(0.25 * Opacity);
        });
    }

    public fun AddRoadSegments(roadSegments: List<RoadSegmentFeature>, id: String? = null): Boolean
    {
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                Logging.D("AddRoadSegments called with ${roadSegments.count()}.")
                if (ConditionalCompile.detailedFeatureCreationLogging == true)
                {
                    for (index in roadSegments.indices) {
                        val feature = roadSegments[index]
                        if (feature is RouteRoadSegmentFeature) {
                            Logging.V("Route Road Segment ${index} is '${feature.Name}', routeDirectionOfTravel=${feature.RouteDirectionOfTravel}, directionOfService=${feature.DirectionOfService}, serviced=${feature.Serviced}")

                        } else if (feature is RouteRoadSegmentNonServiceFeature) {

                            Logging.V("Non-service Route Road Segment ${index} is '${feature.Name}', routeDirectionOfTravel=${feature.RouteDirectionOfTravel}")

                        } else {
                            Logging.V("RoadSegment ${index} is '${feature.Name}'")

                        }
                    }
                }
                val featuresToAdd = mutableListOf<Feature>();
                for (roadSegment in roadSegments)
                {
                    val found = GetLineFeatureById(roadSegment.UniqueGuid.toString());
                    if (found == null)
                    {
                        if (roadSegment.ShapeGeoJSON.isEmpty() == false)
                        {
                            val lineString: LineString = LineString.fromJson (roadSegment.ShapeGeoJSON)
                            val feature = Feature.fromGeometry(lineString, null, roadSegment.UniqueGuid.toString())
                            var featureType: RoadSegmentFeatureType;
                            if (roadSegment is RouteRoadSegmentFeature)
                            {
                                featureType = RoadSegmentFeatureType.RouteRoad;
                            }
                            else if (roadSegment is NavigationSegmentFeature)
                            {
                                featureType = RoadSegmentFeatureType.A2BNavigation;
                            }
                            else if (roadSegment is RouteRoadSegmentNonServiceFeature)
                            {
                                featureType = RoadSegmentFeatureType.RouteRoadNonService;
                            }
                            else //if (roadSegment is RoadSegmentFeature)
                            {
                                featureType = RoadSegmentFeatureType.Road;
                            }
                            feature.addStringProperty("type", featureType.toString()); // This was int version of enum in C# originally.
                            feature.addStringProperty("roadguid", roadSegment.RoadGuid.toString());
                            feature.addStringProperty("note", roadSegment.Note);
                            feature.addStringProperty("text", roadSegment.Text);
                            feature.addBooleanProperty("highlight", false);
                            feature.addBooleanProperty("snapped", false);
                            feature.addStringProperty("name", roadSegment.Name);
                            if (roadSegment.Comments == true || roadSegment.Note.isEmpty() == false)
                            {
                                feature.addBooleanProperty("comments", true);
                            }
                            feature.addStringProperty("identity", roadSegment.Identity);
                            feature.addNumberProperty("length", roadSegment.Length);
                            if (roadSegment is RouteRoadSegmentNonServiceFeature)
                            {
                                feature.addStringProperty("routeguid", roadSegment.RouteGuid.toString());
                                feature.addStringProperty("sequence", roadSegment.Sequence.toString());
                                feature.addStringProperty("directiontravel", roadSegment.RouteDirectionOfTravel.toString());
                                feature.addStringProperty("bearingdesc", roadSegment.BearingDescription);
                                feature.addBooleanProperty("reported", roadSegment.Reported);
                                feature.addStringProperty("servicenote", roadSegment.ServiceNote);
                                feature.addBooleanProperty("driveOnRight", DriveOnRight);
                                feature.addBooleanProperty("driveOnLeft", DriveOnLeft);
                                feature.addNumberProperty("manoeuvre", roadSegment.ManoeuvreAtEnd.value);
                            }
                            if (roadSegment is RouteRoadSegmentFeature)
                            {
                                feature.addBooleanProperty("serviced", roadSegment.Serviced);
                                feature.addStringProperty("directionservice", roadSegment.DirectionOfService.toString());
                                feature.addNumberProperty("passescompleted", roadSegment.PassesCompleted);
                                feature.addNumberProperty("passestotal", roadSegment.PassesTotal);
                                if (roadSegment.Comments == true || roadSegment.Note.isNullOrEmpty() == false || roadSegment.ServiceNote.isNullOrEmpty() == false)
                                {
                                    feature.addBooleanProperty("comments", true);
                                }
                                else
                                {
                                    feature.addBooleanProperty("comments", false);
                                }
                            }
                            if (roadSegment is NavigationSegmentFeature)
                            {
                                feature.addBooleanProperty("completed", roadSegment.Completed);
                                feature.addStringProperty("a2bNavigationSegment", A2BNavigationSegmentStageProperty.A2BNavigationStage.Before.toString());
                            }
                            if (roadSegment.BoundingBox != null)
                            {
                                val boundingBox = BoundingBox.fromLngLats(
                                    roadSegment.BoundingBox!!.SouthWest.Longitude,
                                    roadSegment.BoundingBox!!.SouthWest.Latitude,
                                    roadSegment.BoundingBox!!.NorthEast.Longitude,
                                    roadSegment.BoundingBox!!.NorthEast.Latitude
                                );
                                val boundingBoxGeoJSON = boundingBox.toJson();
                                feature.addStringProperty("boundingbox", boundingBoxGeoJSON);
                            }

                            lineFeatures.add(feature);
                            featuresToAdd.add(feature);
                        }
                        else
                        {
                            Logging.W("ShapeGeoJSON is empty UniqueGuid=${roadSegment.UniqueGuid}");
                        }
                    }
                }

                //UpdateAllFeaturesInSource(lineFeatures, id);
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
        return true
    }

    public fun AddPathToTarget(pathToTarget: PathToTarget, id: String? = null): Boolean
    {
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                Logging.D("AddPathToTarget called .")
                if (pathToTarget.ShapeGeoJSON.isNullOrEmpty() == false)
                {
                    val lineString: LineString = LineString.fromJson (pathToTarget.ShapeGeoJSON)
                    val feature = Feature.fromGeometry(lineString, null, pathToTarget.UniqueGuid.toString())
                    //var type: RoadSegmentFeatureType;
                    feature.addBooleanProperty("ispathtotarget", true);

                    lineFeatures+=(feature);
                    AddSomeFeaturesInSource(listOf(feature), id);
                }
                else
                {
                    Logging.W("ShapeGeoJSON is empty UniqueGuid=${pathToTarget.UniqueGuid}");
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
        return true
    }

    public fun RemoveRoadSegments(roadSegments: List<RoadSegmentFeature>?, id: String? = null): Boolean
    {
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                var featuresToRemove = mutableListOf<Feature>();
                if (roadSegments == null)
                {
                    lineFeatures = mutableListOf<Feature>();
                    Logging.D("Called with null list of roadSegments so removing all.");
                    UpdateAllFeaturesInSource(lineFeatures, id);
                }
                else
                {
                    Logging.D("Called for ${roadSegments.count()} roadSegments, current total (before removal)=${lineFeatures.count()}");
                    for (roadSegment in roadSegments)
                    {
                        var found = GetLineFeatureById(roadSegment.UniqueGuid.toString());
                        if (found != null)
                        {
                            lineFeatures.remove(found);
                            featuresToRemove.add(found);
                        }
                    }
                    Logging.D("After removing road segments, current total=${lineFeatures.count()}");
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

    internal fun RemovePathToTarget(pathToTarget: PathToTarget, id: String? = null): Boolean
    {
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                var found = GetLineFeatureById(pathToTarget.UniqueGuid.toString());
                if (found != null)
                {
                    Logging.D("Will remove pathToTarget ${pathToTarget.UniqueGuid}.");
                    lineFeatures.remove(found);
                    RemoveSomeFeaturesInSource(listOf(found), id);
                }
                else
                {
                    Logging.D("Not able to remove pathToTarget ${pathToTarget.UniqueGuid} as not found.");
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

    private var featuresToChange = mutableListOf<Feature>();
    private fun ChangeRoadSegmentProperty(changeRequired: RoadSegmentPropertyBase) : Boolean
    {
        var count = 0;
        var success = false;
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                var found = GetLineFeatureById(changeRequired.RouteOrRoadSegmentGuid.toString());
                if (found != null)
                {
                    var json = found.toJson();
                    lineFeatures.remove(found);
                    found = Feature.fromJson(json);
                    if (changeRequired is RouteRoadSegmentServicedProperty)
                    {
                        // Expression.Eq(Expression.Get("type"), (Java.Lang.Number)((int)RoadSegmentFeatureType.RouteRoadNonService))
                        val type = found.getStringProperty("type");
                        if (type != RoadSegmentFeatureType.RouteRoadNonService.toString()) // ??? AAAAAAAAH
                        {
                            Logging.V("Found routeRoadSegment for RouteSegmentGuid=${changeRequired.RouteOrRoadSegmentGuid}, will change 'serviced' property to ${changeRequired.Serviced}, passesCompleted=${changeRequired.PassesCompleted}, passesTotal=${changeRequired.PassesTotal}");
                            found.removeProperty("serviced");
                            found.addBooleanProperty("serviced", changeRequired.Serviced);
                            found.removeProperty("passescompleted");
                            found.addNumberProperty("passescompleted", changeRequired.PassesCompleted);
                            found.removeProperty("passestotal");
                            found.addNumberProperty("passestotal", changeRequired.PassesTotal);
                            count++;
                            success = true;
                        }
                    }
                    else if (changeRequired is RouteRoadSegmentNonServiceReportedProperty)
                    {
                        Logging.V("Found routeRoadSegment for RouteSegmentGuid=${changeRequired.RouteOrRoadSegmentGuid}, will change 'reported' property to ${changeRequired.Reported}");
                        found.removeProperty("reported");
                        found.addBooleanProperty("reported", changeRequired.Reported);
                        count++;
                        success = true;
                    }
                    else if (changeRequired is RouteRoadSegmentNonServiceSnappedProperty)
                    {
                        Logging.V("Found routeRoadSegment for RouteSegmentGuid=${changeRequired.RouteOrRoadSegmentGuid}, will change 'snapped' property to ${changeRequired.Snapped}");
                        found.removeProperty("snapped");
                        found.addBooleanProperty("snapped", changeRequired.Snapped);
                        count++;
                        success = true;
                    }
                    else if (changeRequired is ManoeuvreSegmentProperty)
                    {
                        Logging.V("Found ManoeuvreSegmentProperty for RouteSegmentGuid=${changeRequired.RouteOrRoadSegmentGuid}, will change 'ManoeuvreRouteSegment' property to ${changeRequired.ManoeuvreRouteSegment}");
                        found.removeProperty("manoeuvreRouteSegment");
                        found.addStringProperty("manoeuvreRouteSegment", changeRequired.ManoeuvreRouteSegment.toString());
                        count++;
                        success = true;
                    }
                    else if (changeRequired is RoadSegmentHighlightedProperty)
                    {
                        Logging.V("Found ManoeuvreSegmentProperty for RouteSegmentGuid=${changeRequired.RouteOrRoadSegmentGuid}, will change 'ManoeuvreRouteSegment' property to ${changeRequired.Highlighted}");
                        found.removeProperty("highlight");
                        found.addBooleanProperty("highlight", changeRequired.Highlighted);
                        count++;
                        success = true;
                    }
                    else if (changeRequired is A2BNavigationSegmentStageProperty)
                    {
                        Logging.V("Found A2BNavigationSegmentStageProperty for RouteSegmentGuid=${changeRequired.RouteOrRoadSegmentGuid}, will change 'A2BNavigationSegmentStageProperty' property to ${changeRequired.A2BNavigationSegment}");
                        found.removeProperty("a2bNavigationSegment");
                        found.addStringProperty("a2bNavigationSegment", changeRequired.A2BNavigationSegment.toString());
                        count++;
                        success = true;
                    }
                    else if (changeRequired is A2BNavigationSegmentCompletedProperty)
                    {
                        Logging.V("Found A2BNavigationSegmentCompletedProperty for RouteSegmentGuid=${changeRequired.RouteOrRoadSegmentGuid}, will change 'A2BNavigationSegmentCompletedProperty' property to ${changeRequired.Completed}");
                        found.removeProperty("completed");
                        found.addBooleanProperty("completed", changeRequired.Completed);
                        count++;
                        success = true;
                    }
                    lineFeatures.add(found);
                    featuresToChange.add(found);
                }
                else
                {

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
        return success;
    }

    public fun ChangeRoadSegmentProperties(changesRequired: List<RoadSegmentPropertyBase>, id: String? = null) : Int
    {
        var count: Int = 0;
        var countFail: Int = 0;
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                Logging.D("Called with list of ${changesRequired.count()} property changes");
                featuresToChange.clear();
                for (changeRequired in changesRequired)
                {
                    var success = ChangeRoadSegmentProperty(changeRequired);
                    if (success == true)
                    {
                        count++;
                    }
                    else
                    {
                        countFail++;
                        if (countFail < 5)
                        {
                            Logging.W("Attempt to change property for an unknown segment RouteSegmentGuid=${changeRequired.RouteOrRoadSegmentGuid}");
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

    public fun ChangePathToTargetGeometry(UniqueId: UUID, newGeometry: List<GeographicPosition>): Boolean
    {
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                var foundIndex: Int = 0;
                var found: Boolean = false;
                for (index in lineFeatures.indices)
                {
                    if (lineFeatures[index].id()?.lowercase() == UniqueId.toString().lowercase())
                    {
                        found = true;
                        break;
                    }
                    foundIndex++;
                }
                if (found == true)
                {
                    val isPathToTarget = lineFeatures[foundIndex].getBooleanProperty("ispathtotarget");
                    if (isPathToTarget != null && isPathToTarget == true)
                    {
                        var points = mutableListOf<Point>();
                        for (geographicPosition in newGeometry)
                        {
                            points.add(Point.fromLngLat(geographicPosition.Longitude, geographicPosition.Latitude));
                        }
                        val lineString: LineString = LineString.fromLngLats(points);

                        lineFeatures[foundIndex] = Feature.fromGeometry(lineString, null, lineFeatures[foundIndex].id());
                        lineFeatures[foundIndex].addBooleanProperty("ispathtotarget", true);


                        Logging.V("ChangePathToTargetGeometry found feature for id=${UniqueId}");

                        UpdateSomeFeaturesInSource(listOf(lineFeatures[foundIndex]));
                    }
                }
                else
                {
                    Logging.W("NOT found feature for id=${UniqueId}");
                    return false;
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

    internal fun GetLocationsForRoadSegments(includeNonServiceSegmentsInFullExtent: Boolean): List<Point>
    {
        var locations = mutableListOf<Point>();
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                var nonServiceSegments = lineFeatures.filter { (includeNonServiceSegmentsInFullExtent == true && it.getStringProperty("type") == RoadSegmentFeatureType.RouteRoadNonService.name) || it.getStringProperty("type") == RoadSegmentFeatureType.RouteRoad.name }
                for (index in nonServiceSegments.indices)
                {
                    var location = nonServiceSegments[index].geometry() as LineString;

                    locations.addAll(location.coordinates());
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

    internal fun GetLocationsForRoadSegment(roadSegmentFeature: RoadSegmentFeature): List<Point>
    {
        var locations = mutableListOf<Point>();
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                var roadSegment = GetLineFeatureById(roadSegmentFeature.UniqueGuid.toString());
                if (roadSegment != null)
                {
                    var location = roadSegment.geometry() as LineString;
                    locations.addAll(location.coordinates());
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

    internal fun GetGeometryForRoadSegment(roadSegmentFeature: RoadSegmentFeature): String
    {
        var locationJson = "";
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                var roadSegment = GetLineFeatureById(roadSegmentFeature.UniqueGuid.toString());
                if (roadSegment != null)
                {
                    var location = roadSegment.geometry() as LineString;
                    locationJson = location.toJson();
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
        return locationJson;
    }

    /**
     * Create and return a TappedMapFeature from a mapbox map feature
     */
    internal fun ReCreateRoadSegmentFeature(newLocation: GeographicPosition, feature: Feature): RoadSegmentFeature?
    {
        if (feature.geometry() is LineString || feature.geometry() is MultiLineString)
        {
            var screenLocation = mapView.mapboxMap
                .pixelForCoordinate(Point.fromLngLat(newLocation.Longitude, newLocation.Latitude));

            var properties: JsonObject? = feature.properties();

            if (properties != null)
            {
                var type: RoadSegmentFeatureType = RoadSegmentFeatureType.Road;
                var typeProperty = properties.get("type");
                if (typeProperty != null)
                {
                    type = RoadSegmentFeatureType.valueOf(typeProperty.asString);
                }

                var passesCompleted = 0;
                var passesCompletedProperty = properties.get("passescompleted");
                if (passesCompletedProperty != null)
                {
                    passesCompleted = passesCompletedProperty.asInt;
                }

                var passesTotal = 0;
                var passesTotalProperty = properties.get("passestotal");
                if (passesTotalProperty != null)
                {
                    passesTotal = passesTotalProperty.asInt;
                }

                var comments = false;
                var commentsProperty = properties.get("comments");
                if (commentsProperty != null)
                {
                    comments = commentsProperty.asBoolean;
                }

                var reported = false;
                var reportedProperty = properties.get("reported");
                if (reportedProperty != null && reportedProperty.isJsonNull == false)
                {
                    reported = reportedProperty.asBoolean;
                }

                var serviced = false;
                var servicedProperty = properties.get("serviced");
                if (servicedProperty != null && servicedProperty.isJsonNull == false)
                {
                    serviced = servicedProperty.asBoolean;
                }

                var note = "";
                var noteProperty = properties.get("note");
                if (noteProperty != null && noteProperty.isJsonNull == false)
                {
                    note = noteProperty.asString;
                }

                var text = "";
                var textProperty = properties.get("text");
                if (textProperty != null && textProperty.isJsonNull == false)
                {
                    text = textProperty.asString;
                }

                var sequence = 0L;
                var sequenceProperty = properties.get("sequence");
                if (sequenceProperty != null && sequenceProperty.isJsonNull == false)
                {
                    sequence = sequenceProperty.asLong;
                }

                var serviceNote = "";
                var serviceCommentProperty = properties.get("servicenote");
                if (serviceCommentProperty != null && serviceCommentProperty.isJsonNull == false)
                {
                    serviceNote = serviceCommentProperty.asString;
                }

                var name = "";
                var nameProperty = properties.get("name");
                if (nameProperty != null && nameProperty.isJsonNull == false)
                {
                    name = nameProperty.asString;
                }

                var bearingDesc = "";
                var bearingDescProperty = properties.get("bearingdesc");
                if (bearingDescProperty != null && bearingDescProperty.isJsonNull == false)
                {
                    bearingDesc = bearingDescProperty.asString;
                }

                var routeGuid = UUID.randomUUID(); // ??? TBD
                var routeGuidProperty = properties.get("routeguid");
                if (routeGuidProperty != null && routeGuidProperty.isJsonNull == false)
                {
                    routeGuid = UUID.fromString(routeGuidProperty.asString);
                }

                var roadGuid = UUID.randomUUID();
                var roadGuidProperty = properties.get("roadguid");
                if (roadGuidProperty != null && roadGuidProperty.isJsonNull == false)
                {
                    roadGuid = UUID.fromString(roadGuidProperty.asString);
                }

                var boundingBox = "";
                var boundingBoxProperty = properties.get("boundingbox");
                if (boundingBoxProperty != null && boundingBoxProperty.isJsonNull == false)
                {
                    boundingBox = boundingBoxProperty.asString;
                }
                var MapBoxBoundingBox = BoundingBox.fromJson(boundingBox);

                var identity = "";
                var identityProperty = properties.get("identity");
                if (identityProperty != null && identityProperty.isJsonNull == false)
                {
                    identity = identityProperty.asString;
                }

                var length = 0.0;
                var lengthProperty = properties.get("length");
                if (lengthProperty != null && lengthProperty.isJsonNull == false)
                {
                    length = lengthProperty.asDouble;
                }

                var completed = false;
                var completedProperty = properties.get("completed");
                if (completedProperty != null && completedProperty.isJsonNull == false)
                {
                    completed = completedProperty.asBoolean;
                }

                var tappedMapFeature = RoadSegmentFeature();
                if (type == RoadSegmentFeatureType.Road)
                {
                    tappedMapFeature = RoadSegmentFeature();
                    tappedMapFeature.Comments = comments;
                    tappedMapFeature.Note = note;
                    tappedMapFeature.Text = text;
                    tappedMapFeature.Name = name;
                    tappedMapFeature.RoadGuid = roadGuid;
                    tappedMapFeature.BoundingBox = LongLatExtent(NorthEast = GeographicPosition(Longitude = MapBoxBoundingBox.east(), Latitude = MapBoxBoundingBox.north()), SouthWest = GeographicPosition(Longitude = MapBoxBoundingBox.west(), Latitude = MapBoxBoundingBox.south()));
                    tappedMapFeature.Identity = identity;
                    tappedMapFeature.TappedPosition = MapPosition(GeoPosition = GeographicPosition(Longitude = newLocation.Longitude, Latitude = newLocation.Latitude), ScreenPosition = ScreenPosition(X = screenLocation.x, Y = screenLocation.y, mapView.width, mapView.height));
                    if (feature.id() != null)
                    {
                        tappedMapFeature.UniqueGuid = UUID.fromString(feature.id());
                    }
                    var locations = GetGeometryForRoadSegment(tappedMapFeature);
                    tappedMapFeature.ShapeGeoJSON = locations;
                    tappedMapFeature.Length = length;

                    Logging.D("RoadSegmentFeature with UniqueGuid=${tappedMapFeature.UniqueGuid}, roadSegmentGuid=${tappedMapFeature.RoadGuid}, Name=${tappedMapFeature.Name}, Text=${tappedMapFeature.Text}, Note=${tappedMapFeature.Note}, Comments=${tappedMapFeature.Comments}, Identity=${tappedMapFeature.Identity.toString()}");
                }
                else if (type == RoadSegmentFeatureType.RouteRoadNonService)
                {
                    tappedMapFeature = RouteRoadSegmentNonServiceFeature();
                    tappedMapFeature.Comments = comments;
                    tappedMapFeature.Reported = reported;
                    tappedMapFeature.Note = note;
                    tappedMapFeature.Text = text;
                    tappedMapFeature.Sequence = sequence;
                    tappedMapFeature.ServiceNote = serviceNote;
                    tappedMapFeature.Name = name;
                    tappedMapFeature.BearingDescription = bearingDesc;
                    tappedMapFeature.RouteGuid = routeGuid;
                    tappedMapFeature.RoadGuid = roadGuid;
                    tappedMapFeature.BoundingBox = LongLatExtent(NorthEast = GeographicPosition(Longitude = MapBoxBoundingBox.east(), Latitude = MapBoxBoundingBox.north()), SouthWest = GeographicPosition(Longitude = MapBoxBoundingBox.west(), Latitude = MapBoxBoundingBox.south()));
                    tappedMapFeature.Identity = identity;
                    tappedMapFeature.TappedPosition = MapPosition(GeoPosition = GeographicPosition(Longitude = newLocation.Longitude, Latitude = newLocation.Latitude), ScreenPosition = ScreenPosition(X = screenLocation.x, Y = screenLocation.y, mapView.width, mapView.height));
                    tappedMapFeature.RouteDirectionOfTravel = GetDirectionOfTravelForFeature(feature);
                    if (feature.id() != null)
                    {
                        tappedMapFeature.UniqueGuid = UUID.fromString(feature.id());
                    }
                    var locations = GetGeometryForRoadSegment(tappedMapFeature);
                    tappedMapFeature.ShapeGeoJSON = locations;
                    tappedMapFeature.Length = length;
                    tappedMapFeature.ManoeuvreAtEnd = GetManoeuvreTypeForFeature(feature);

                    Logging.D("RouteRoadSegmentNonServiceFeature with UniqueGuid=${tappedMapFeature.UniqueGuid}, roadSegmentGuid=${tappedMapFeature.RoadGuid}, Name=${tappedMapFeature.Name}, Text=${tappedMapFeature.Text}, Note=${tappedMapFeature.Note}, Comments=${tappedMapFeature.Comments}, Reported=${tappedMapFeature.Reported}, routeSegmentGuid=${tappedMapFeature.RouteGuid}, Identity=${tappedMapFeature.Identity}");
                }
                else if (type == RoadSegmentFeatureType.RouteRoad)
                {
                    tappedMapFeature = RouteRoadSegmentFeature();
                    tappedMapFeature.PassesCompleted = passesCompleted;
                    tappedMapFeature.PassesTotal = passesTotal;
                    tappedMapFeature.Comments = comments;
                    tappedMapFeature.Reported = reported;
                    tappedMapFeature.Note = note;
                    tappedMapFeature.Text = text;
                    tappedMapFeature.Sequence = sequence;
                    tappedMapFeature.ServiceNote = serviceNote;
                    tappedMapFeature.Name = name;
                    tappedMapFeature.BearingDescription = bearingDesc;
                    tappedMapFeature.RouteGuid = routeGuid;
                    tappedMapFeature.RoadGuid = roadGuid;
                    tappedMapFeature.BoundingBox = LongLatExtent(NorthEast = GeographicPosition(Longitude = MapBoxBoundingBox.east(), Latitude = MapBoxBoundingBox.north()),  SouthWest = GeographicPosition(Longitude = MapBoxBoundingBox.west(), Latitude = MapBoxBoundingBox.south()));
                    tappedMapFeature.Identity = identity;

                    tappedMapFeature.TappedPosition = MapPosition(GeoPosition = GeographicPosition(Longitude = newLocation.Longitude, Latitude = newLocation.Latitude), ScreenPosition = ScreenPosition(X = screenLocation.x, Y = screenLocation.y, mapView.width, mapView.height));
                    tappedMapFeature.Serviced = serviced; //GetServicedPropertyForFeature(feature);
                    tappedMapFeature.RouteDirectionOfTravel = GetDirectionOfTravelForFeature(feature);
                    tappedMapFeature.DirectionOfService = GetDirectionOfServiceForFeature(feature);
                    if (feature.id() != null)
                    {
                        tappedMapFeature.UniqueGuid = UUID.fromString(feature.id());
                    }
                    var locations = GetGeometryForRoadSegment(tappedMapFeature);
                    tappedMapFeature.ShapeGeoJSON = locations;
                    tappedMapFeature.Length = length;
                    tappedMapFeature.ManoeuvreAtEnd = GetManoeuvreTypeForFeature(feature);

                    Logging.D("RouteRoadSegmentFeature with UniqueGuid=${tappedMapFeature.UniqueGuid}, roadSegmentGuid=${tappedMapFeature.RoadGuid}, Name=${tappedMapFeature.Name}, Text=${tappedMapFeature.Text}, Note=${tappedMapFeature.Note}, Serviced=${tappedMapFeature.Serviced.toString()}, Comments=${tappedMapFeature.Comments}, Reported=${tappedMapFeature.Reported}, routeSegmentGuid=${tappedMapFeature.RouteGuid}, Identity={tappedMapFeature.Identity}");
                }
                else // We don't do a recreate of A2BNavigation type at the moment. But if we did then remember to include the additional 'completed' property.
                {

                }
                return tappedMapFeature;
            }
        }
        else
        {
            return null;
        }
        return null;
    }

    private fun GetLineFeatureById(subjectId: String): Feature?
    {
        if(lineFeatures.isNullOrEmpty()) return null;
        return lineFeatures.firstOrNull{ x -> x.id() == subjectId };
    }

    private fun GetDirectionOfTravelForFeature(feature: Feature): DirectionOfTravel
    {
        var directionOfTravel: DirectionOfTravel = DirectionOfTravel.With;
        if (feature.properties() != null)
        {
            var directionProperty = feature.properties()!!.get("directiontravel");
            if (directionProperty != null)
            {
                directionOfTravel = DirectionOfTravel.valueOf(directionProperty.asString);
            }
        }
        return directionOfTravel;
    }

    private fun GetDirectionOfServiceForFeature(feature: Feature): DirectionsOfService
    {
        var directionOfService: DirectionsOfService = DirectionsOfService.Both;
        if (feature.properties() != null)
        {
            var directionProperty = feature.properties()!!.get("directionservice");
            if (directionProperty != null)
            {
                directionOfService = DirectionsOfService.valueOf(directionProperty.asString);
            }
        }
        return directionOfService;
    }

    private fun GetManoeuvreTypeForFeature(feature: Feature): ManoeuvreType
    {
        var manoeuvreType: ManoeuvreType = ManoeuvreType.None;
        if (feature.properties() != null)
        {
            var manoeuvreTypeProperty = feature.properties()!!.get("manoeuvre");
            if (ManoeuvreType != null)
            {
                manoeuvreType = ManoeuvreType.getByValue(manoeuvreTypeProperty.asInt)!!;
            }
        }
        return manoeuvreType;
    }

    /**
     * Attempt to load bitmaps into Mapbox to use on Symbol layers
     */
    private fun LoadSymbolImages(theStyle: Style)
    {
        val blueArrow = ContextCompat.getDrawable(theContext, R.drawable.roadarrowbluesquarelarge)?.toBitmap(120, 120)!!
        theStyle.addImage(imageId_RoadArrowBlue, blueArrow);
        val greyArrow = ContextCompat.getDrawable(theContext, R.drawable.roadarrowgreysquarelarge)?.toBitmap(120, 120)!!
        theStyle.addImage(imageId_RoadArrowGrey, greyArrow);
        val greenArrow = ContextCompat.getDrawable(theContext, R.drawable.roadarrowgreenlarge)?.toBitmap(120, 120)!!
        theStyle.addImage(imageId_RoadArrowGreen, greenArrow);
        val snappedLocationSymbol = ContextCompat.getDrawable(theContext, R.drawable.snappedlocation)?.toBitmap()!!
        theStyle.addImage(imageId_ManoeuvreLocation, snappedLocationSymbol);
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

    /**
     * Add Source with Features to Mapbox Map
     */
    private fun AddFeaturesToSource(features: List<Feature>) {
        var featureCollection = FeatureCollection.fromFeatures(features);

        val geoJsonSource = geoJsonSource(sourceId)
        {
            featureCollection(featureCollection)
        }

        mapView.mapboxMap.style?.addSource(geoJsonSource);
    }

    private fun UpdateAllFeaturesInSource(features: List<Feature>, id: String? = null) {
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
                geoJsonSource?.featureCollection(featureCollection, id)
            }
            else
            {
                geoJsonSource?.featureCollection(featureCollection)
            };

            Logging.V(
                "Will update ${features.count()} lineFeatures, number in featureCollection=${
                    featureCollection.features()?.count()
                }"
            );
        //});
    }

    private fun UpdateSomeFeaturesInSource(features: List<Feature>, id: String? = null)
    {
        // Check if anything to do
        if(features.isEmpty()) return;

        //Handler(Looper.getMainLooper()).post {
            if (id.isNullOrEmpty() == false) {
                theMapStyleListener.LogWhenDataSourceUpdated(id);
            }

            var geoJsonSource: GeoJsonSource? =
                mapView.mapboxMap.style?.getSourceAs<GeoJsonSource>(sourceId);

            if (id.isNullOrEmpty() == false) {
                geoJsonSource?.updateGeoJSONSourceFeatures(features, id);
            } else {
                geoJsonSource?.updateGeoJSONSourceFeatures(features);
            }

            Logging.V("Will update ${features.count()} lineFeatures.");
        //};
    }
    private fun AddSomeFeaturesInSource(features: List<Feature>, id: String? = null)
    {
        // Check if anything to do
        if(features.isEmpty()) return;

        //Handler(Looper.getMainLooper()).post {
            if (id.isNullOrEmpty() == false) {
                theMapStyleListener.LogWhenDataSourceUpdated(id);
            }

            var geoJsonSource: GeoJsonSource? =
                mapView.mapboxMap.style?.getSourceAs<GeoJsonSource>(sourceId);

            if (id.isNullOrEmpty() == false) {
                geoJsonSource?.addGeoJSONSourceFeatures(features, id);
            } else {
                geoJsonSource?.addGeoJSONSourceFeatures(features);
            }

            Logging.V("Will add ${features.count()} lineFeatures.");
        //};
    }

    private fun RemoveSomeFeaturesInSource(features: List<Feature>, id: String? = null)
    {
        // Check if anything to do
        if(features.isEmpty()) return;

        //Handler(Looper.getMainLooper()).post {
            if (id.isNullOrEmpty() == false) {
                theMapStyleListener.LogWhenDataSourceUpdated(id);
            }

            var featureIds = mutableListOf<String>();
            for (feature in features) {
                featureIds.add(feature.id().toString());
            }

            var geoJsonSource: GeoJsonSource? =
                mapView.mapboxMap.style?.getSourceAs<GeoJsonSource>(sourceId);

            if (id.isNullOrEmpty() == false) {
                geoJsonSource?.removeGeoJSONSourceFeatures(featureIds, id);
            } else {
                geoJsonSource?.removeGeoJSONSourceFeatures(featureIds);
            }

            Logging.V("Will remove ${features.count()} lineFeatures.");
        //};
    }

}

public class RoadSegmentHighlightedProperty : RoadSegmentPropertyBase()
{
    public var Highlighted: Boolean = false;
}

public class RouteRoadSegmentServicedProperty : RoadSegmentPropertyBase()
{
    //public var Serviced: RoadSegmentServiced = RoadSegmentServiced.Unserviced;
    public var Serviced: Boolean = false // Note that we don't need a partially serviced state any more so use a bool here instead of RoadSegmentServiced.
    public var PassesTotal: Int = 0;
    public var PassesCompleted: Int = 0;
}

public class RouteRoadSegmentNonServiceReportedProperty : RoadSegmentPropertyBase()
{
    public var Reported: Boolean = false;
}

public class RouteRoadSegmentNonServiceSnappedProperty : RoadSegmentPropertyBase()
{
    public var Snapped: Boolean = false;
}

public class ManoeuvreSegmentProperty : RoadSegmentPropertyBase()
{
    public var ManoeuvreRouteSegment: ManoeuvreHighlightType = ManoeuvreHighlightType.None;

    public enum class ManoeuvreHighlightType
    {
        None,
        Upto,
        After,
    }
}

public class A2BNavigationSegmentStageProperty : RoadSegmentPropertyBase()
{
    public var A2BNavigationSegment: A2BNavigationStage = A2BNavigationStage.Before;

    public enum class A2BNavigationStage
    {
        Before,
        After,
    }
}

public class A2BNavigationSegmentCompletedProperty : RoadSegmentPropertyBase()
{
    public var Completed: Boolean = false;
}

public abstract class RoadSegmentPropertyBase : MapFeaturePropertyBase()
{
    public lateinit var RouteOrRoadSegmentGuid: UUID
}

public abstract class MapFeaturePropertyBase
{

}

/// <summary>
/// This enumerated type is for use in conjunction with <see cref="ChangeRoadSegmentStatusFunc"/>.
/// </summary>
public enum class RoadSegmentServiced
{
    Unserviced, // = 0
    PartiallyServiced, // = 1
    Serviced, // = 2
}