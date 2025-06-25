package isl.mapbox.thinbindings.android.maprendererkotlin.map

import android.os.Handler
import android.os.Looper
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import isl.mapbox.thinbindings.android.features.AbstractPointMapFeature
import isl.mapbox.thinbindings.android.features.DirectionOfTravel
import isl.mapbox.thinbindings.android.features.JobFeature
import isl.mapbox.thinbindings.android.features.LineMapFeature
import isl.mapbox.thinbindings.android.features.MapFeature
import isl.mapbox.thinbindings.android.features.PathToTarget
import isl.mapbox.thinbindings.android.features.RoadSegmentFeature
import isl.mapbox.thinbindings.android.features.RouteRoadSegmentNonServiceFeature
import isl.mapbox.thinbindings.android.features.ServicePointAndTradeSiteFeature
import isl.mapbox.thinbindings.android.features.SymbolMapFeature
import isl.mapbox.thinbindings.android.internal.ILocationMarkerInternalCallbacks
import isl.mapbox.thinbindings.android.internal.IMapGestureInternalCallbacks
import isl.mapbox.thinbindings.android.internal.IMapStyleInternalCallbacks
import isl.mapbox.thinbindings.android.internal.Logging
import isl.mapbox.thinbindings.android.maprendererkotlin.IMapCameraCallback
import isl.mapbox.thinbindings.android.maprendererkotlin.NullableType
import isl.mapbox.thinbindings.android.maprendererkotlin.misc.ConditionalCompile
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.MapCameraExtent
import isl.mapbox.thinbindings.android.positions.MapCameraPosition
import isl.mapbox.thinbindings.android.positions.MapPadding
import java.util.TimerTask
import java.util.UUID
import java.util.zip.DeflaterOutputStream

public class MapCamera(theMapView: MapView, locationMarker: LocationMarker, mapStyle: MapStyle) :
    IMapGestureInternalCallbacks, ILocationMarkerInternalCallbacks, IMapCamera,
    IMapStyleInternalCallbacks {
    private val mapView: MapView = theMapView
    private var useBearing: Boolean = false;
    private var showPathInTargetedExtent: Boolean = true
    private var showPathToTarget: Boolean = false;

    /**
     * This is value between 0.0 and 1.0 i.e. isn't pixels.
     */
    private var defaultPaddingForExtent: Double = 0.1;

    /**
     *  These are values between 0.0 and 1.0 i.e. aren't pixels.
     */
    private var mapPadding: MapPadding = MapPadding(0.0, 0.0, 0.0, 0.0);

    /**
     * In pixels.
     */
    private val defaultLogoPadding: MapPadding;
    private val defaultScalebarPadding: MapPadding;

    /**
     * In pixels.
     */
    private val defaultAttributionPadding: MapPadding;
    private var theExtentType: ExtentType = ExtentType.None;
    private val locationMarker: LocationMarker = locationMarker;
    private val mapStyle: MapStyle = mapStyle;
    private var targetedFeatures = listOf<MapFeature>();
    private var lastCameraOptions: CameraOptions? = null;

    private var theDefaultZoom: Double;
    private var theMaxZoom: Double;
    private val theMaxZoomForOperator = 22.0;

    private lateinit var cSharpMapCameraHandler: IMapCameraCallback;

    private var includeJobsInFullExtentCalcs: Boolean = false;
    private var includeServicePointsInFullExtentCalcs: Boolean = true;
    private var includeNonServiceSegmentsInFullExtentCalcs: Boolean = true;

    /**
     * Should be a value between 0 and 1.
     */
    public var movementDetectinThreshold: Double = 1.0 / 30.0;
    private var useFlingInDetection: Boolean = false;

    private val pathToTargetUUID = UUID.randomUUID();

    init
    {
        theDefaultZoom = 16.5;
        theMaxZoom = 18.0;
        locationMarker.RegisterForCallback(this);
        val attribution = mapView.attribution;
        defaultAttributionPadding = MapPadding(attribution.marginLeft.toDouble(), attribution.marginTop.toDouble(), attribution.marginRight.toDouble(), attribution.marginBottom.toDouble());

        val logo = mapView.logo;
        val scalebar = mapView.scalebar;
        defaultLogoPadding = MapPadding(logo.marginLeft.toDouble(), logo.marginTop.toDouble(), logo.marginRight.toDouble(), logo.marginBottom.toDouble());
        defaultScalebarPadding = MapPadding(scalebar.marginLeft.toDouble(), scalebar.marginTop.toDouble(), scalebar.marginRight.toDouble(), scalebar.marginBottom.toDouble()
        );

        var currentPosition = GetMapCameraPosition();
        SetMapCameraPosition(MapCameraPosition(currentPosition.Position, Bearing = null, Tilt = null, Zoom = NullableType(theDefaultZoom), null, MovementDuration = null));
        mapStyle.RegisterForCallback(this);
    }

    override fun GetMaxZoom(): Double
    {
        return theMaxZoom;
    }

    override fun SetMaxZoom(newMaxZoom: Double)
    {
        try
        {
            Logging.D("Set maximum zoom to ${newMaxZoom}");
            theMaxZoom = newMaxZoom;
            UpdateExtent();
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun GetDefaultZoom(): Double
    {
        return theDefaultZoom;
    }

    override fun SetDefaultZoom(newDefaultZoom: Double)
    {
        try
        {
            Logging.D("Set default zoom to ${newDefaultZoom}");
            theDefaultZoom = newDefaultZoom;
            UpdateExtent();
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun GetDefaultExtentPadding(): Double
    {
        return defaultPaddingForExtent;
    }

    override fun SetDefaultExtentPadding(newDefaultExtentPadding: Double)
    {
        try
        {
            defaultPaddingForExtent = newDefaultExtentPadding;
            UpdateExtent();
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    private var exponentialSmoothingCoefficentA: Double = 0.25;
    private var exponentialSmoothingPreviousOutput: Double = 0.0;

    private fun DoExpentialSmoothing(unfilteredInput: Double): Double
    {
        var filteredOutput: Double = 0.0;

        filteredOutput = exponentialSmoothingCoefficentA * unfilteredInput + ((1.0 - exponentialSmoothingCoefficentA) * exponentialSmoothingPreviousOutput);
        exponentialSmoothingPreviousOutput = filteredOutput;

        return filteredOutput;
    }

    override fun SetMapCameraPosition(mapCameraPosition: MapCameraPosition, adjustOffsetForSpeed: Boolean?)
    {
        try
        {
            Logging.V("Received Longitude=${mapCameraPosition.Position.Longitude} Latitude=${mapCameraPosition.Position.Latitude} Zoom=${mapCameraPosition.Zoom?.Value} Tilt=${mapCameraPosition.Tilt?.Value}, Bearing=${mapCameraPosition.Bearing?.Value} MoveType=${mapCameraPosition.MoveType} MovementDuration.${mapCameraPosition.MovementDuration?.Value}");
            val cameraPositionBuilder = CameraOptions.Builder()
                .center(Point.fromLngLat(mapCameraPosition.Position.Longitude, mapCameraPosition.Position.Latitude));
            if (mapCameraPosition.Zoom != null)
            {
                cameraPositionBuilder.zoom(mapCameraPosition.Zoom.Value);
            }
            if (mapCameraPosition.Tilt != null)
            {
                cameraPositionBuilder.pitch(mapCameraPosition.Tilt.Value);
            }
            if (useBearing == true && mapCameraPosition.Bearing != null)
            {
                cameraPositionBuilder.bearing(mapCameraPosition.Bearing.Value);
            }

            var paddingInPixels = GetMapPaddingInPixels();
            if (adjustOffsetForSpeed == true)
            {
                paddingInPixels = GetMapPaddingInPixelsWithMovementOffset(offsetDueToSpeed);
            }
            cameraPositionBuilder.padding(EdgeInsets(paddingInPixels.TopPadding, paddingInPixels.LeftPadding, paddingInPixels.BottomPadding, paddingInPixels.RightPadding))

            val cameraPosition = cameraPositionBuilder.build();
            //val attribution = mapView.attribution;

            //val logo = mapView.logo;

            Logging.V("Will set camera so that Long=${cameraPosition.center?.longitude()}, Lat=${cameraPosition.center?.latitude()}, zoom=${cameraPosition.zoom}, bearing=${cameraPosition.bearing}.");

            var animation: MapAnimationOptions? = null;
            if (mapCameraPosition.MovementDuration != null)
            {
                animation = MapAnimationOptions.Builder()
                    .duration(mapCameraPosition.MovementDuration.Value)
                    .build()
            }

            if (mapCameraPosition.MoveType == MoveCameraTypeEnum.Move || mapCameraPosition.MoveType == null || mapCameraPosition.MovementDuration == null)
            {
                mapView.mapboxMap.setCamera(cameraPosition);
                ReportCameraPositionChange(cameraPosition, null);
            }
            else if (mapCameraPosition.MoveType == MoveCameraTypeEnum.Ease)
            {
                mapView.mapboxMap.easeTo(cameraPosition, animation);
                ReportCameraPositionChange(cameraPosition, MoveCameraTypeEnum.Ease);
            }
            else if (mapCameraPosition.MoveType == MoveCameraTypeEnum.Animate)
            {
                mapView.mapboxMap.flyTo(cameraPosition, animation);
                ReportCameraPositionChange(cameraPosition, MoveCameraTypeEnum.Animate);
            }
            else
            {
                Logging.W("Not able to update camera position, due to confusion over animation required.");
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun GetMapCameraPosition(): MapCameraPosition
    {
        try
        {
            val cameraState = mapView.mapboxMap.cameraState;
            val mapCameraPosition = MapCameraPosition(GeographicPosition(cameraState.center.longitude(), cameraState.center.latitude()), NullableType(cameraState.bearing), NullableType(cameraState.pitch), NullableType(cameraState.zoom), null, null);
            return mapCameraPosition;
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            throw ex;
        }
    }

    override fun SetMapCameraExtent(mapCameraExtent: MapCameraExtent)
    {
        try
        {
            val height = mapView.height;
            val width = mapView.width;
            var extendPaddingTop = height * defaultPaddingForExtent;
            var extentPaddingBottom = height * defaultPaddingForExtent;
            var extentPaddingLeft = width * defaultPaddingForExtent;
            var extentPaddingRight = width * defaultPaddingForExtent;
            val paddingInPixels = GetMapPaddingInPixels();
            extendPaddingTop += paddingInPixels.TopPadding;
            extentPaddingLeft += paddingInPixels.LeftPadding;
            extentPaddingBottom += paddingInPixels.BottomPadding;
            extentPaddingRight += paddingInPixels.RightPadding;

            val cameraPosition = mapView.mapboxMap.cameraForCoordinateBounds(CoordinateBounds(Point.fromLngLat(
                mapCameraExtent.Extent.SouthWest.Longitude, mapCameraExtent.Extent.SouthWest.Latitude), Point.fromLngLat(mapCameraExtent.Extent.NorthEast.Longitude, mapCameraExtent.Extent.NorthEast.Latitude)), EdgeInsets(extendPaddingTop, extentPaddingLeft, extentPaddingBottom, extentPaddingRight)
            );

            Logging.V("Will set camera so that Long=${cameraPosition.center?.longitude()}, Lat=${cameraPosition.center?.latitude()}, zoom=${cameraPosition.zoom}, bearing=${cameraPosition.bearing}.");

            var animation: MapAnimationOptions? = null;
            if (mapCameraExtent.MovementDuration != null)
            {
                animation = MapAnimationOptions.Builder()
                    .duration(mapCameraExtent.MovementDuration.Value)
                    .build()
            }
            if (mapCameraExtent.MoveType == null || mapCameraExtent.MovementDuration == null)
            {
                mapView.mapboxMap.setCamera(cameraPosition);
                ReportCameraPositionChange(cameraPosition, null);
            }
            else if (mapCameraExtent.MoveType == MoveCameraTypeEnum.Ease)
            {
                mapView.mapboxMap.easeTo(cameraPosition, animation);
                ReportCameraPositionChange(cameraPosition, MoveCameraTypeEnum.Ease);
            }
            else if (mapCameraExtent.MoveType == MoveCameraTypeEnum.Animate)
            {
                mapView.mapboxMap.flyTo(cameraPosition, animation);
                ReportCameraPositionChange(cameraPosition, MoveCameraTypeEnum.Animate);
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    private fun SetMapCameraExtent(locations: List<Point>, initialMoveType: MoveCameraTypeEnum?)
    {
        Handler(Looper.getMainLooper()).post(
        {
            if (locations.isEmpty() == false)
            {
                val height = mapView.height;
                val width = mapView.width;
                var extendPaddingTop = height * defaultPaddingForExtent;
                var extentPaddingBottom = height * defaultPaddingForExtent;
                var extentPaddingLeft = width * defaultPaddingForExtent;
                var extentPaddingRight = width * defaultPaddingForExtent;
                val paddingInPixels = GetMapPaddingInPixels();
                extendPaddingTop += paddingInPixels.TopPadding;
                extentPaddingLeft += paddingInPixels.LeftPadding;
                extentPaddingBottom += paddingInPixels.BottomPadding;
                extentPaddingRight += paddingInPixels.RightPadding;

                var bearing: Double? = null;
                if (useBearing == true)
                {
                    bearing = locationMarker.lastLocationMarkerBearing;
                }

                // Now we really need to limit the zoom level.
                val maxZoomToUse = Math.max(theMaxZoom, theDefaultZoom)

                val cameraPositionBuilder = CameraOptions.Builder()
                cameraPositionBuilder.padding(EdgeInsets(0.0, 0.0, 0.0, 0.0));
                cameraPositionBuilder.bearing(bearing);
                val cameraOptions = cameraPositionBuilder.build();

                val cameraPosition = mapView.mapboxMap.cameraForCoordinates(locations, cameraOptions, EdgeInsets(extendPaddingTop, extentPaddingLeft, extentPaddingBottom, extentPaddingRight), maxZoomToUse, ScreenCoordinate(0.0, 0.0));

                if (cameraPosition.center != null)
                {
                    Logging.V("Will set camera (using ${locations.count()} locations) so that Long=${cameraPosition.center?.longitude()}, Lat=${cameraPosition.center?.latitude()}, zoom=${cameraPosition.zoom}, bearing=${cameraPosition.bearing}.");
                    Logging.V("... with paddingLeft=${extentPaddingLeft}, paddingTop=${extendPaddingTop}, paddingRight=${extentPaddingRight}, paddingBottom=${extentPaddingBottom}, bearing=${bearing}, initialMoveType=${initialMoveType}.");
                    for (location in  locations)
                    {
                        Logging.V("location long=${location.longitude()}, lat=${location.latitude()}.");
                    }

                    var animation = MapAnimationOptions.Builder()
                        .duration(500) // TBD
                        .build()

                    if (initialMoveType != null)
                    {
                        when (initialMoveType) {
                            MoveCameraTypeEnum.Move -> mapView.mapboxMap.setCamera(cameraPosition);
                            MoveCameraTypeEnum.Ease -> mapView.mapboxMap.easeTo(cameraPosition, animation);
                            MoveCameraTypeEnum.Animate -> mapView.mapboxMap.flyTo(cameraPosition, animation);
                        }
                    }
                    else
                    {
                        mapView.mapboxMap.easeTo(cameraPosition, animation);
                    }
                    ReportCameraPositionChange(cameraPosition, initialMoveType);
                }
                else
                {
                    Logging.E("Error, cameraPosition.center is null.")
                }
            }
        });
    }

    private fun ReportCameraPositionChange(cameraOptions: CameraOptions, moveType: MoveCameraTypeEnum?)
    {
        if (lastCameraOptions == null)
        {
            if (::cSharpMapCameraHandler.isInitialized)
            {
                var location = GeographicPosition(cameraOptions.center!!.longitude(), cameraOptions.center!!.latitude());
                var bearing: NullableType<Double>? = null;
                if (cameraOptions.bearing != null)
                {
                    bearing = NullableType(cameraOptions.bearing!!);
                }
                var pitch: NullableType<Double>? = null;
                if (cameraOptions.pitch != null)
                {
                    pitch = NullableType(cameraOptions.pitch!!);
                }
                var zoom: NullableType<Double>? = null;
                if (cameraOptions.zoom != null)
                {
                    zoom = NullableType(cameraOptions.zoom!!);
                }

                var newPosition = MapCameraPosition(location, bearing, pitch, zoom, moveType, null );
                cSharpMapCameraHandler.MapCameraPositionUpdateFromMapRendererKotlin(newPosition);
            }
        }
        else
        {
            if (::cSharpMapCameraHandler.isInitialized)
            {
                if (cameraOptions.center == null)
                {
                    Logging.E("Error, cameraOptions.center is null.")
                }
                else
                {
                    var location = GeographicPosition(cameraOptions.center!!.longitude(), cameraOptions.center!!.latitude());
                    var bearing: NullableType<Double>? = null;
                    if (cameraOptions.bearing != null && cameraOptions.bearing != lastCameraOptions!!.bearing)
                    {
                        bearing = NullableType(cameraOptions.bearing!!);
                    }
                    var pitch: NullableType<Double>? = null;
                    if (cameraOptions.pitch != null && cameraOptions.pitch != lastCameraOptions!!.pitch)
                    {
                        pitch = NullableType(cameraOptions.pitch!!);
                    }
                    var zoom: NullableType<Double>? = null;
                    if (cameraOptions.zoom != null && cameraOptions.zoom != lastCameraOptions!!.zoom)
                    {
                        zoom = NullableType(cameraOptions.zoom!!);
                    }

                    var newPosition = MapCameraPosition(location, bearing, pitch, zoom, moveType, null );
                    cSharpMapCameraHandler.MapCameraPositionUpdateFromMapRendererKotlin(newPosition);
                }
            }
        }
        lastCameraOptions = cameraOptions;
    }

    override fun SetOrientateToBearing(orientate: Boolean)
    {
        Logging.D("Orientate to bearing = ${orientate}");
        useBearing = orientate;
    }

    override fun GetOrientateToBearing(): Boolean
    {
        return useBearing;
    }
    override fun SetMapCameraPaddingInPixels(newMapPadding: MapPadding)
    {
        try
        {
            val widthInPixels = mapView.width;
            val heightInPixels = mapView.height;

            Logging.D("Set map padding to left=${newMapPadding.LeftPadding}, top=${newMapPadding.TopPadding}, right=${newMapPadding.RightPadding}, bottom=${newMapPadding.BottomPadding}");
            mapPadding = MapPadding(newMapPadding.LeftPadding / widthInPixels, newMapPadding.TopPadding / heightInPixels, newMapPadding.RightPadding / widthInPixels, newMapPadding.BottomPadding / heightInPixels); // = newMapPadding;

            val attribution = mapView.attribution;
            attribution.marginLeft =
                (defaultAttributionPadding.LeftPadding + newMapPadding.LeftPadding).toFloat();
            attribution.marginTop =
                (defaultAttributionPadding.TopPadding + newMapPadding.TopPadding).toFloat();
            attribution.marginRight =
                (defaultAttributionPadding.RightPadding + newMapPadding.RightPadding).toFloat();
            attribution.marginBottom =
                (defaultAttributionPadding.BottomPadding + newMapPadding.BottomPadding).toFloat();

            val logo = mapView.logo;
            logo.marginLeft =
                (defaultLogoPadding.LeftPadding + newMapPadding.LeftPadding).toFloat();
            logo.marginTop =
                (defaultLogoPadding.TopPadding + newMapPadding.TopPadding).toFloat();
            logo.marginRight =
                (defaultLogoPadding.RightPadding + newMapPadding.RightPadding).toFloat();
            logo.marginBottom =
                (defaultLogoPadding.BottomPadding + newMapPadding.BottomPadding).toFloat();

            val scalebar = mapView.scalebar;
            scalebar.marginLeft =
                (defaultScalebarPadding.LeftPadding + newMapPadding.LeftPadding).toFloat();
            scalebar.marginTop =
                (defaultScalebarPadding.TopPadding + newMapPadding.TopPadding).toFloat();
            scalebar.marginRight =
                (defaultScalebarPadding.RightPadding + newMapPadding.RightPadding).toFloat();
            scalebar.marginBottom =
                (defaultScalebarPadding.BottomPadding + newMapPadding.BottomPadding).toFloat();

            UpdateExtent();
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun GetMapPaddingInPixels(): MapPadding
    {
        var mapPaddingInPixels: MapPadding = MapPadding(mapPadding.LeftPadding * mapView.width, mapPadding.TopPadding * mapView.height, mapPadding.RightPadding * mapView.width, mapPadding.BottomPadding * mapView.height);
        return mapPaddingInPixels;
    }

    private fun GetMapPaddingInPixelsWithMovementOffset(additionalBottomPadding: Double): MapPadding
    {
        var mapPaddingInPixels: MapPadding = MapPadding(mapPadding.LeftPadding * mapView.width, mapPadding.TopPadding * mapView.height, mapPadding.RightPadding * mapView.width, (mapPadding.BottomPadding + additionalBottomPadding) * mapView.height);
        return mapPaddingInPixels;
    }

    override fun SetExtentType(newExtentType: ExtentType, initialMoveType: MoveCameraTypeEnum?)
    {
        try
        {
            Logging.D("Will now change extent from ${theExtentType} to ${newExtentType}.");
            theExtentType = newExtentType;
            UpdateVisibilityOfPath();
            UpdateExtent(initialMoveType);
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun GetExtentType(): ExtentType
    {
        return theExtentType;
    }

    override fun SetTargetedExtentInclusions(features: List<MapFeature>, initialMoveType: MoveCameraTypeEnum?)
    {
        try
        {
            Logging.D("Targeted extent set to include ${features.count()} features.")
            targetedFeatures = features;

            UpdateVisibilityOfPath();

            if (theExtentType == ExtentType.TargetedExtent)
            {
                UpdateTargetedExtent(initialMoveType);
            }
            else if (theExtentType == ExtentType.TargetedExtentAndLocation)
            {
                UpdateTargetedExtentAndLocation(initialMoveType);
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun GetTargetedExtentInclusions(): List<MapFeature> {
        return targetedFeatures;
    }

    private fun ShowPathToTarget()
    {
        HidePathToTarget();

        var pathToTarget = PathToTarget();
        pathToTarget.UniqueGuid = pathToTargetUUID;

        val targetedPosition = GetTargetedPoint()
        if (targetedPosition != null && locationMarker.lastLocationMarkerPosition != null) // ??? What do we do if locationMarker.lastLocationMarkerPosition is null?
        {
            val locationMarker = GeographicPosition(locationMarker.lastLocationMarkerPosition!!.Longitude, locationMarker.lastLocationMarkerPosition!!.Latitude);
            val points = mutableListOf<Point>();
            points.add(Point.fromLngLat(locationMarker.Longitude, locationMarker.Latitude));
            points.add(Point.fromLngLat(targetedPosition.Longitude, targetedPosition.Latitude));
            var lineString = LineString.fromLngLats(points);
            var geoJSONLineString = lineString.toJson();
            pathToTarget.ShapeGeoJSON = geoJSONLineString;
            mapStyle.roadSegmentLayer.AddPathToTarget(pathToTarget);
        }

    }

    private fun HidePathToTarget()
    {
        var pathToTarget = PathToTarget();
        pathToTarget.UniqueGuid = pathToTargetUUID;
        mapStyle.roadSegmentLayer.RemovePathToTarget(pathToTarget);
    }

    override fun SetShowPathInTargetedExtentMode(showPath: Boolean) {
        try
        {
            Logging.D("Show path in targeted extent mode = ${showPath}");
            showPathInTargetedExtent = showPath;
            UpdateVisibilityOfPath();
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun GetShowPathInTargetedExtentMode(): Boolean {
        return showPathInTargetedExtent;
    }

    override fun SetShowPathToTargetFeature(showPath: Boolean) {
        try
        {
            Logging.D("Show path = ${showPath}");
            showPathToTarget = showPath;
            UpdateVisibilityOfPath();
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }

    }

    override fun GetShowPathToTargetFeature(): Boolean {
        return showPathToTarget;
    }

    private fun UpdateVisibilityOfPath()
    {
        val shouldBeVisible = ShouldPathBeVisible();
        if (shouldBeVisible == true)
        {
            ShowPathToTarget();
        }
        else
        {
            HidePathToTarget();
        }
    }

    private fun ShouldPathBeVisible(): Boolean
    {
        var visible = false;
        if ((showPathToTarget == true) || (showPathInTargetedExtent == true && (theExtentType == ExtentType.TargetedExtent || theExtentType == ExtentType.TargetedExtentAndLocation)))
        {
            visible = true;
        }
        return visible;
    }

    private fun GetTargetedPoint(): GeographicPosition?
    {
        var position: GeographicPosition? = null;
        val targetedFeature = targetedFeatures.firstOrNull();
        if (targetedFeature != null)
        {
            if (targetedFeature is RouteRoadSegmentNonServiceFeature) // & therefor also RouteRoadSegmentFeature
            {
                var targetPoint: Point;
                val points = LineString.fromJson(targetedFeature.ShapeGeoJSON);
                if (targetedFeature.RouteDirectionOfTravel == DirectionOfTravel.With)
                {
                    targetPoint = points.coordinates().first();
                }
                else
                {
                    targetPoint = points.coordinates().last();
                }
                position = GeographicPosition(targetPoint.longitude(), targetPoint.latitude());
            }
            else if (targetedFeature is LineMapFeature)
            {
                val points = LineString.fromJson(targetedFeature.ShapeGeoJSON);
                position = GeographicPosition(points.coordinates().first().longitude(), points.coordinates().first().latitude()); // TBD depends upon directionoftravel.
            }
            else if (targetedFeature is AbstractPointMapFeature)
            {
                position = GeographicPosition(targetedFeature.Longitude, targetedFeature.Latitude);
            }
        }

        return position;
    }

    override fun SetIncludeJobsInFullExtent(include: Boolean)
    {
        try
        {
            includeJobsInFullExtentCalcs = include;
            UpdateExtent();
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun GetIncludeJobsInFullExtent(): Boolean {
        return includeJobsInFullExtentCalcs;
    }

    override fun SetIncludeServicePointsInFullExtent(include: Boolean)
    {
        try
        {
            includeServicePointsInFullExtentCalcs = include;
            UpdateExtent();
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun GetIncludeServicePointsInFullExtent(): Boolean {
        return includeServicePointsInFullExtentCalcs;
    }

    override fun SetIncludeNonServiceSegmentsInFullExtent(include: Boolean)
    {
        try
        {
            includeNonServiceSegmentsInFullExtentCalcs = include;
            UpdateExtent();
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun GetIncludeNonServiceSegmentsInFullExtent(): Boolean {
        return includeNonServiceSegmentsInFullExtentCalcs;
    }

    override fun SetMovementDetectionThreshold(threshold: Double, useFling: Boolean)
    {
        movementDetectinThreshold = threshold;
        useFlingInDetection = useFling;
    }

    internal fun RegisterForCallback(handler: IMapCameraCallback)
    {
        cSharpMapCameraHandler = handler;
    }

    private var selectedMapFeature: MapFeature? = null;
    internal fun SetSelectedMapFeature(mapFeature: MapFeature?)
    {
        selectedMapFeature = mapFeature;
        if (selectedMapFeature != null)
        {
            UpdateExtent();
        }
        // ??? TBD What do we do if null?
    }

    private fun UpdateExtent(initialMoveType: MoveCameraTypeEnum? = null)
    {
        Handler(Looper.getMainLooper()).post(
        {
            when (theExtentType)
            {
                ExtentType.None -> UpdateExtentNone();
                ExtentType.CentreOnLocation -> UpdateExtentCentreOnLocation(initialMoveType);
                ExtentType.FullExtent -> UpdateExtentFull(initialMoveType);
                ExtentType.FullExtentAndLocation -> UpdateExtentFullAndLocation(initialMoveType);
                ExtentType.TargetedExtent -> UpdateTargetedExtent(initialMoveType)
                ExtentType.TargetedExtentAndLocation -> UpdateTargetedExtentAndLocation(initialMoveType);
                ExtentType.CentreOnSelectedFeatures -> UpatedExtentCentreOnSelectedFeatures(initialMoveType);
                else -> Logging.E("Called for unhandled extentType ${theExtentType.name}.");
            }
        });
    }

    private fun UpdateExtentNone()
    {
        Logging.V("Nothing to update.");
        mapView.mapboxMap.setBounds(CameraBoundsOptions.Builder().maxZoom(theMaxZoomForOperator).build());

    }

    private fun UpdateExtentCentreOnLocation(initialMoveType: MoveCameraTypeEnum?)
    {
        if (locationMarker.locationMarkerVisible == true && locationMarker.lastLocationMarkerPosition != null)
        {
            var bearing: NullableType<Double>? = null;
            if (useBearing == true && locationMarker.lastLocationMarkerBearing != null)
            {
                bearing = NullableType(locationMarker.lastLocationMarkerBearing!!);
            }
            mapView.mapboxMap.setBounds(CameraBoundsOptions.Builder().maxZoom(theMaxZoomForOperator).build());

            Logging.V("Will set camera so that Long=${locationMarker.lastLocationMarkerPosition!!.Longitude}, Lat=${locationMarker.lastLocationMarkerPosition!!.Latitude}, zoom=${theDefaultZoom}, bearing=${bearing}.");

            when (initialMoveType) {
                MoveCameraTypeEnum.Move -> SetMapCameraPosition(MapCameraPosition(GeographicPosition(locationMarker.lastLocationMarkerPosition!!.Longitude, locationMarker.lastLocationMarkerPosition!!.Latitude), bearing, null, NullableType(theDefaultZoom), MoveCameraTypeEnum.Move, NullableType(500)), adjustOffsetForSpeed = true);
                MoveCameraTypeEnum.Ease -> SetMapCameraPosition(MapCameraPosition(GeographicPosition(locationMarker.lastLocationMarkerPosition!!.Longitude, locationMarker.lastLocationMarkerPosition!!.Latitude), bearing, null, NullableType(theDefaultZoom), MoveCameraTypeEnum.Ease, NullableType(500)), adjustOffsetForSpeed = true);
                MoveCameraTypeEnum.Animate -> SetMapCameraPosition(MapCameraPosition(GeographicPosition(locationMarker.lastLocationMarkerPosition!!.Longitude, locationMarker.lastLocationMarkerPosition!!.Latitude), bearing, null, NullableType(theDefaultZoom), MoveCameraTypeEnum.Animate, NullableType(500)), adjustOffsetForSpeed = true);
                null -> SetMapCameraPosition(MapCameraPosition(GeographicPosition(locationMarker.lastLocationMarkerPosition!!.Longitude, locationMarker.lastLocationMarkerPosition!!.Latitude), bearing, null, NullableType(theDefaultZoom), MoveCameraTypeEnum.Ease, NullableType(500)), adjustOffsetForSpeed = true);
            }

        }
    }

    private fun UpdateExtentFull(initialMoveType: MoveCameraTypeEnum?)
    {
        var allLocations = GetFullExtentLocations();
        Logging.V("Will set camera extent using ${allLocations.count()} locations.")
        SetMapCameraExtent(allLocations, initialMoveType);
    }

    private fun UpdateExtentFullAndLocation(initialMoveType: MoveCameraTypeEnum?)
    {
        var allLocations = mutableListOf<Point>();
        allLocations.addAll(GetFullExtentLocations());

        if (locationMarker.locationMarkerVisible == true && locationMarker.lastLocationMarkerPosition != null) {
            allLocations.add(
                Point.fromLngLat(
                    locationMarker.lastLocationMarkerPosition!!.Longitude,
                    locationMarker.lastLocationMarkerPosition!!.Latitude
                )
            );
        }
        Logging.V("Will set camera extent using ${allLocations.count()} locations.")
        SetMapCameraExtent(allLocations, initialMoveType);
    }

    private fun UpdateTargetedExtent(initialMoveType: MoveCameraTypeEnum?)
    {
        val allLocations = GetLocationsOfTargetedFeatures();
        if (allLocations.count() > 0)
        {
            Logging.V("Will set camera extent using ${allLocations.count()} locations.")
            SetMapCameraExtent(allLocations, initialMoveType);
        }
    }

    private fun UpdateTargetedExtentAndLocation(initialMoveType: MoveCameraTypeEnum?)
    {
        var allLocations: MutableList<Point> = mutableListOf<Point>();
        allLocations.addAll(GetLocationsOfTargetedFeatures());
        if (allLocations.count() > 0 && locationMarker.locationMarkerVisible == true && locationMarker.lastLocationMarkerPosition != null)
        {
            allLocations.add(Point.fromLngLat(locationMarker.lastLocationMarkerPosition!!.Longitude, locationMarker.lastLocationMarkerPosition!!.Latitude));
            Logging.V("Will set camera extent using ${allLocations.count()} locations.")
            SetMapCameraExtent(allLocations, initialMoveType);
        }
    }

    private fun GetLocationsOfTargetedFeatures(): List<Point>
    {
        var allLocations = mutableListOf<Point>();
        for (index in targetedFeatures.indices)
        {
            if (targetedFeatures[index] is RoadSegmentFeature) // This includes Non-service segments and route road segments.
            {
                var locations = mapStyle.roadSegmentLayer.GetLocationsForRoadSegment(targetedFeatures[index] as RoadSegmentFeature);
                allLocations.addAll(locations);
            }
            else if (targetedFeatures[index] is ServicePointAndTradeSiteFeature)
            {
                var location = mapStyle.servicePointAndTradeSiteLayer.GetLocationForServicePointsAndTradeSite(targetedFeatures[index] as ServicePointAndTradeSiteFeature);
                if (location != null) {
                    allLocations.add(location);
                }
            }
            else if (targetedFeatures[index] is JobFeature)
            {
                var location = mapStyle.jobLayer.GetLocationForJob(targetedFeatures[index] as JobFeature);
                if (location != null) {
                    allLocations.add(location);
                }
            }
            else if (targetedFeatures[index] is SymbolMapFeature)
            {
                var location = mapStyle.symbolsLayer.GetLocationForSymbol(targetedFeatures[index] as SymbolMapFeature)
                if (location != null) {
                    allLocations.add(location);
                }
            }
        }
        return allLocations;
    }

    private fun UpatedExtentCentreOnSelectedFeatures(initialMoveType: MoveCameraTypeEnum?)
    {
        if (selectedMapFeature != null)
        {
            if (selectedMapFeature is AbstractPointMapFeature)
            {
                var point = Point.fromLngLat((selectedMapFeature as AbstractPointMapFeature).Longitude, (selectedMapFeature as AbstractPointMapFeature).Latitude);
                Logging.V("Will set camera extent for a point map feature.")
                SetMapCameraExtent(listOf(point), initialMoveType);
            }
            else if (selectedMapFeature is LineMapFeature)
            {
                // We cannot rely on LineString.fromJson((selectedMapFeature as LineMapFeature).ShapeGeoJSON);
                // as the ShapeGeoJSON is sometimes linestring and sometimes multilinestring as the mapbox query is returning the found segment
                // in separate parts, some overlapping. Instead we'll use the Guid to find the segment ourselves...
                var locations = mapStyle.roadSegmentLayer.GetLocationsForRoadSegment(selectedMapFeature as RoadSegmentFeature);
                SetMapCameraExtent(locations, initialMoveType);
            }
        }
    }

    private fun GetFullExtentLocations(): List<Point>
    {
        val locations = mutableListOf<Point>();

        val roadsLocations = mapStyle.roadSegmentLayer.GetLocationsForRoadSegments(includeNonServiceSegmentsInFullExtentCalcs);
        locations.addAll(roadsLocations);

        val servicePointAndTradeLocations = mapStyle.servicePointAndTradeSiteLayer.GetLocationsForServicePointsAndTradeSites(includeServicePointsInFullExtentCalcs);
        locations.addAll(servicePointAndTradeLocations);

        if (includeJobsInFullExtentCalcs == true)
        {
            val jobLocations = mapStyle.jobLayer.GetLocationsForJob();
            locations.addAll(jobLocations);
        }

        return GetOuterPoints(locations);
    }

    private fun GetOuterPoints(locations: List<Point>): MutableList<Point>
    {
        var topPoint: Point? = null;
        var rightPoint: Point? = null;
        var bottomPoint: Point? = null;
        var leftPoint: Point? = null;
        for (index in locations.indices)
        {
            var location = locations[index];
            if (rightPoint == null || location.longitude() > rightPoint.longitude())
            {
                rightPoint = location;
            }
            if (leftPoint == null || location.longitude() < leftPoint.longitude())
            {
                leftPoint = location;
            }
            if (topPoint == null || location.latitude() > topPoint.latitude())
            {
                topPoint = location;
            }
            if (bottomPoint == null || location.latitude() < bottomPoint.latitude())
            {
                bottomPoint = location;
            }
            //locations.addAll(location.coordinates());
        }

        val outerLocations = mutableListOf<Point>();
        rightPoint?.let { outerLocations.add(it) };
        leftPoint?.let { outerLocations.add(it) };
        topPoint?.let { outerLocations.add(it) };
        bottomPoint?.let { outerLocations.add(it) };

        return outerLocations;
    }

    override fun MapGestureInternalCallback(
        moveDetected: Boolean,
        rotateDetected: Boolean,
        shoveDetected: Boolean,
        flingDetected: Boolean,
        scaleDetected: Boolean
    )
    {
        Logging.V("Gesture detected move=${moveDetected}, rotate=${rotateDetected}, shove=${shoveDetected}, fling=${flingDetected}, scale=${scaleDetected}.");
        if (theExtentType == ExtentType.None || theExtentType == ExtentType.CentreOnLocation)
        {
            if (scaleDetected && GetMapCameraPosition().Zoom != null)
            {
                if (GetMapCameraPosition().Zoom!!.Value == 25.5)
                {
                    Logging.D("Zoom level indicated as 25.5, will ignore this and set as maximum value ${theMaxZoom}");
                    theDefaultZoom = theMaxZoom;
                }
                else
                {
                    theDefaultZoom = GetMapCameraPosition().Zoom!!.Value;
                }
            }
        }
        val flingDetectedIfEnabled: Boolean;
        if (useFlingInDetection == true)
        {
            flingDetectedIfEnabled = flingDetected
        }
        else
        {
            flingDetectedIfEnabled = false;
        }
        var setToExtentNone: Boolean = false;
        // Here we will want to consider changing the extent mode ???
        if (theExtentType == ExtentType.None)
        {
            // Do nothing.
        }
        else if (theExtentType == ExtentType.CentreOnLocation && (moveDetected || shoveDetected || flingDetectedIfEnabled))
        {
            setToExtentNone = true;
        }
        else if (theExtentType == ExtentType.CentreOnSelectedFeatures && (moveDetected || rotateDetected || shoveDetected || flingDetectedIfEnabled || scaleDetected))
        {
            setToExtentNone = true;
        }
        else if (theExtentType == ExtentType.FullExtent && (moveDetected || rotateDetected || shoveDetected || flingDetectedIfEnabled || scaleDetected))
        {
            setToExtentNone = true;
        }
        else if (theExtentType == ExtentType.FullExtentAndLocation && (moveDetected || rotateDetected || shoveDetected || flingDetectedIfEnabled || scaleDetected))
        {
            setToExtentNone = true;
        }
        else if (theExtentType == ExtentType.TargetedExtent && (moveDetected || rotateDetected || shoveDetected || flingDetectedIfEnabled || scaleDetected))
        {
            setToExtentNone = true;
        }
        else if (theExtentType == ExtentType.TargetedExtentAndLocation && (moveDetected || rotateDetected || shoveDetected || flingDetectedIfEnabled || scaleDetected))
        {
            setToExtentNone = true;
        }

        if (setToExtentNone == true)
        {
            Logging.V("Change the extent mode to None.");
            SetExtentType(ExtentType.None);
            if (::cSharpMapCameraHandler.isInitialized) {
                cSharpMapCameraHandler.MapCameraExtentUpdateFromMapRendererKotlin(ExtentType.None);
            }
        }
    }

    override fun LocationMarkerInternalCallback(newPosition: GeographicPosition, newText: String, newBearing: Double, newSpeed: Double)
    {
        Logging.V("Received call from LocationMarker that position has been updated, so call UpdateExtent.");
        UpdatePathToTarget(newPosition);
        UpdateExtent(null);
        lastLocationMarkerSpeed = newSpeed;
        offsetDueToSpeed = CalculateOffsetDueToSpeed(newSpeed);
    }
    private var lastLocationMarkerSpeed: Double = 0.0;
    private var offsetDueToSpeed: Double = 0.0;

    private var speedForMaxOffset: Double = 15.0; // 33MPH ish
    private var maxOffsetAtMaxSpeed: Double = 0.0;
    override fun ConfigureMarkerOffsetDueToSpeed(speedForMaxOffset: Double, maxOffsetAtMaxSpeed: Double, filterCoeff: Double)
    {
        if (speedForMaxOffset > 0.0)
        {
            this.speedForMaxOffset = speedForMaxOffset;
        }
        if (maxOffsetAtMaxSpeed >= -1.0 && maxOffsetAtMaxSpeed <= 0.0)
        {
            this.maxOffsetAtMaxSpeed = maxOffsetAtMaxSpeed;
        }
        if (filterCoeff >= 0.0 && filterCoeff <= 1.0)
        {
            this.exponentialSmoothingCoefficentA = filterCoeff;
        }
    }

    private fun CalculateOffsetDueToSpeed(speed: Double): Double
    {
        var offset: Double = 0.0;
        if (useBearing == true)
        {
            var filteredSpeed = DoExpentialSmoothing(lastLocationMarkerSpeed);
            offset = (filteredSpeed / speedForMaxOffset) * maxOffsetAtMaxSpeed;
            if (offset > 0.0)
            {
                offset = 0.0;
            }
            else if (offset < maxOffsetAtMaxSpeed)
            {
                offset = maxOffsetAtMaxSpeed;
            }
        }

        return offset;
    }

    private fun UpdatePathToTarget(newPosition: GeographicPosition)
    {
        val targetedPosition = GetTargetedPoint()
        if (targetedPosition != null && locationMarker.lastLocationMarkerPosition != null) // ??? What do we do if locationMarker.lastLocationMarkerPosition is null?
        {
            val locationMarker = GeographicPosition(locationMarker.lastLocationMarkerPosition!!.Longitude, locationMarker.lastLocationMarkerPosition!!.Latitude);
            var points = mutableListOf<GeographicPosition>();
            points.add(GeographicPosition(locationMarker.Longitude, locationMarker.Latitude));
            points.add(GeographicPosition(targetedPosition.Longitude, targetedPosition.Latitude));
            mapStyle.roadSegmentLayer.ChangePathToTargetGeometry(pathToTargetUUID, points);
        }

    }

    override fun MapStyleInternalCallback(theMapStyle: Style) {
        Logging.V("Received call from MapStyle that style has been updated, so call UpdateExtent.");
        UpdateExtent();
    }
}

enum class MoveCameraTypeEnum {
    Move,
    Ease,
    Animate
}

public enum class ExtentType
{
    None,
    CentreOnLocation,
    CentreOnSelectedFeatures,
    FullExtent,
    FullExtentAndLocation,
    TargetedExtent,
    TargetedExtentAndLocation,
}

