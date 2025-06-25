package isl.mapbox.thinbindings.android.maprendererkotlin.map

import android.os.Handler
import android.os.Looper
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiLineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.QueriedRenderedFeature
import com.mapbox.maps.QueriedSourceFeature
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.SourceQueryOptions
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.turf.TurfMeasurement
import isl.mapbox.thinbindings.android.features.AdhocFeature
import isl.mapbox.thinbindings.android.features.JobFeature
import isl.mapbox.thinbindings.android.features.MapFeature
import isl.mapbox.thinbindings.android.features.PointOfInterestFeature
import isl.mapbox.thinbindings.android.features.RoadSegmentFeature
import isl.mapbox.thinbindings.android.features.RouteRoadSegmentFeature
import isl.mapbox.thinbindings.android.features.RouteRoadSegmentNonServiceFeature
import isl.mapbox.thinbindings.android.features.ServicePointAndTradeSiteFeature
import isl.mapbox.thinbindings.android.features.SymbolType
import isl.mapbox.thinbindings.android.internal.ILocationMarkerInternalCallbacks
import isl.mapbox.thinbindings.android.internal.IMapClickInternalCallbacks
import isl.mapbox.thinbindings.android.internal.Logging
import isl.mapbox.thinbindings.android.internal.MapClickListener
import isl.mapbox.thinbindings.android.maprendererkotlin.IFeatureSelectionCallback
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.JobHighlightedProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.JobPropertyBase
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.RoadSegmentHighlightedProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.RoadSegmentPropertyBase
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.ServicePointHighlightedProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.SymbolHighlightedProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.SymbolPropertyBase
import isl.mapbox.thinbindings.android.maprendererkotlin.misc.ConditionalCompile
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.MapPosition
import isl.mapbox.thinbindings.android.positions.RoadPosition
import java.io.InvalidObjectException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore

public class FeatureSelection(theMapView: MapView, theMapClickListener: MapClickListener, theMapCamera: MapCamera, theLocationMarker: LocationMarker, theMapStyle: MapStyle) : IMapClickInternalCallbacks,
    ILocationMarkerInternalCallbacks,
    IFeatureSelection {
    private val mapView: MapView = theMapView
    private val mapClickListener: MapClickListener = theMapClickListener;
    private val mapCamera: MapCamera = theMapCamera;
    private val locationMarker: LocationMarker = theLocationMarker;
    private val mapStyle: MapStyle = theMapStyle;

    private val semaphore = Semaphore(1);
    public override var autoDetectRoadPosition = true;
    public override var autoRoadPositionDetectRate: Int = 5;
    private var numberOfLocationUpdatesUntilRoadPositionAutoDetect: Int = autoRoadPositionDetectRate;
    public override var featureSelectionAllowed: Boolean = false;
    public override var tappedMapFeatures = mutableListOf<MapFeature>();
    public override var selectedMapFeature: MapFeature? = null;
    public override var selectedMapFeatures = mutableListOf<MapFeature>();
    public var selectedFeaturesInternalCopy = mutableListOf<MapFeature>();
    private var multiSelectAllowed: Boolean = false;
    private var adhocReport: AdhocFeature? = null;

    private lateinit var cSharpMapCameraHandler: IFeatureSelectionCallback;

    init
    {
        mapClickListener.RegisterForCallback(this);
        locationMarker.RegisterForCallback(this);
    }

    public override var MultipleSelectionAllowed: Boolean
        get() = multiSelectAllowed;
        set(value)
        {
            multiSelectAllowed = value;
            if (multiSelectAllowed == false)
            {
                if (selectedFeaturesInternalCopy.count() > 1)
                {
                    while (selectedFeaturesInternalCopy.count() > 1)
                    {
                        var lastFeature = selectedFeaturesInternalCopy.last();
                        ClearSpecificHighlight(lastFeature);
                        selectedFeaturesInternalCopy.remove(lastFeature);
                        lastSelectedFeature = selectedFeaturesInternalCopy.last();
                    }
                    selectedMapFeatures = selectedFeaturesInternalCopy.toMutableList();
                    selectedMapFeature = selectedFeaturesInternalCopy.firstOrNull();
                    if (::cSharpMapCameraHandler.isInitialized)
                    {
                        cSharpMapCameraHandler.FeaturesTapped(tappedMapFeatures);
                        cSharpMapCameraHandler.FeaturesSelected(selectedMapFeature, selectedMapFeatures);
                    }
                }
            }

        }

    override fun MapClickInternalCallback(clickDetected: MapPosition)
    {
        try
        {
            if (::cSharpMapCameraHandler.isInitialized)
            {
                cSharpMapCameraHandler.MapClickDetected(clickDetected);
            }
            if (featureSelectionAllowed == true)
            {
                var features = DetectRouteFeatures(clickDetected.GeoPosition);
                ProcessFoundFeatures(clickDetected.GeoPosition, features);
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun MapLongClickInternalCallback(longClickDetected: MapPosition)
    {
        try
        {
            if (::cSharpMapCameraHandler.isInitialized)
            {
                cSharpMapCameraHandler.MapLongClickDetected(longClickDetected);
            }
            if (featureSelectionAllowed == true && MultipleSelectionAllowed == false)
            {
                // Will at some point do creation of Adhoc Feature here.
                ClearCurrentHighlight();
                ClearCurrentAdhocReport();
                adhocReport = mapStyle.symbolsLayer.CreateAdhocFeature(longClickDetected.GeoPosition);
                if (adhocReport != null)
                {
                    mapStyle.symbolsLayer.AddSymbols(listOf(adhocReport!!));

                    var tempSelectedMapFeatures = mutableListOf<MapFeature>(adhocReport!!);
                    // ??? TBD Do we put adhocFeature into tempSelectedMapFeatures so it will also appear in selectedMapFeatures?
                    var lastSelectedFeature = DetermineFinalFeatureSelection(tempSelectedMapFeatures, adhocReport);

                    selectedMapFeature = lastSelectedFeature;
                    mapCamera.SetSelectedMapFeature(selectedMapFeature);
                    selectedMapFeatures = tempSelectedMapFeatures.toMutableList();
                    if (::cSharpMapCameraHandler.isInitialized)
                    {
                        cSharpMapCameraHandler.FeaturesTapped(tappedMapFeatures);
                        cSharpMapCameraHandler.FeaturesSelected(selectedMapFeature, selectedMapFeatures);
                    }

                }
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }


    /***
     * If wait is true then method will return the road position once it is known, if false then returns sooner and road position notified in registered callback.
     *
     * Will not return any results if the map is zoomed in beyond 15.0
     *
     * @param position The point to find the nearest road to.
     * @param wait If true, will block and return once road position is known.
     * @return A RoadPosition object with information about the closest in range road, or NULL.
     */
    override fun GetRoadPosition(position: GeographicPosition, wait: Boolean) : RoadPosition?
    {
        // Note that the use of this with wait = true, is no longer available due to changes to the threading used on nested methods.
        // So throw exception if called with wait = true, code may be reworked at a later point.
        if (wait == true)
        {
            throw InvalidObjectException("Wait property needs to be false only.");
        }

        var roadPosition:RoadPosition? = null;

        Handler(Looper.getMainLooper()).post(
        {
            try
            {
                Logging.V("GetRoadPosition called with position=${position.Longitude},${position.Latitude}, wait=${wait}.");
                if (semaphore.tryAcquire() == true) {
                    Logging.V("Semaphore acquired.")
                    val zoom = mapCamera.GetMapCameraPosition().Zoom;
                    if (zoom != null && zoom.Value > 15.0) {

                        var screenLocation = mapView.mapboxMap
                            .pixelForCoordinate(
                                Point.fromLngLat(
                                    position.Longitude,
                                    position.Latitude
                                )
                            );

                        getRoadPositionLatch = CountDownLatch(1);
                        // Note: We cannot repeatedly call mapbox queryRenderedFeatures on UI thread as the callbacks do not happen until this method returns,
                        // so run a worker thread to do the loop, and call queryRenderedFeatures on UI thread, using a countdown latch to wait for the callback.
                        Thread(Runnable {
                            var offset: Double = 10.0;
                            roadFeaturesFound = false;
                            while (offset < 250.0 && roadFeaturesFound == false) {
                                var loop = 0;
                                roadQueryReturned = false;
                                val rect: ScreenBox = ScreenBox(
                                    ScreenCoordinate(
                                        screenLocation.x - offset,
                                        screenLocation.y - offset
                                    ),
                                    ScreenCoordinate(
                                        screenLocation.x + offset,
                                        screenLocation.y + offset
                                    )
                                );

                                getRoadFeatureslatch = CountDownLatch(1);
                                roadFeaturesFound = GetRoadFeaturesAtLocation(rect, roadLayerIds);
                                getRoadFeatureslatch.await();

                                if (ConditionalCompile.detailedFeatureSelectionLogging) {
                                    Logging.V("Offset=${offset}, featuresFound=${roadFeaturesFound}, loop=${loop}");
                                }

                                if (roadFeaturesFound == true && roadFeatures != null) {
                                    var count = roadFeatures!!.count();
                                    if (ConditionalCompile.detailedFeatureSelectionLogging) {
                                        Logging.V("There are ${count} features.");
                                    }

                                    var theRoad = FilterHighestPriorityRoadFeature(roadFeatures!!);
                                    if (theRoad != null) {
                                        getSourceFeatureslatch = CountDownLatch(1);
                                        GetSourceFeaturesForVisibleMap();
                                        getSourceFeatureslatch.await();
                                        if (sourceFeatures != null) {
                                            var sourceCount = sourceFeatures!!.count();
                                            if (ConditionalCompile.detailedFeatureSelectionLogging) {
                                                Logging.V("There are ${sourceCount} source features.");
                                            }
                                            roadPosition = GetBestMatchRoadFeature(theRoad, position);

                                            if (wait == true)
                                            {
                                                getRoadPositionLatch.countDown();
                                            }
                                            else
                                            {
                                                if (::cSharpMapCameraHandler.isInitialized)
                                                {
                                                    cSharpMapCameraHandler.RoadPositionDetected(roadPosition);
                                                }
                                                if (roadPosition == null) {
                                                    Logging.V("Done with road position null.");
                                                } else {
                                                    Logging.V("Done with road position name=${roadPosition!!.Name}.");
                                                }

                                                semaphore.release();
                                                Logging.V("Semaphore released (call made with wait = false).")
                                            }
                                        }
                                    }

                                }

                                offset += 10.0;
                                loop++;
                            }
                            if (/*offset >= 500.0 && */roadFeaturesFound == false)
                            {
                                semaphore.release();
                                Logging.V("Semaphore released (No road feature found).")
                            }
                        }).start();
                    }
                }
                else
                {
                    Logging.W("Failed to acquire semaphore.");
                }
            }
            catch (ex: Exception)
            {
                Logging.E("Exception = ${ex.message}")
                if (ConditionalCompile.throwAfterLoggingException) throw ex;
            }
            finally
            {
                if (wait == true)
                {
                    getRoadPositionLatch.await();
                    semaphore.release();
                    Logging.V("Semaphore released (call made with wait = true).")
                    if (roadPosition == null) {
                        Logging.D("Done with road position null.");
                    } else {
                        Logging.D("Done with road position name=${roadPosition!!.Name}.");
                    }
                }
            }
        });
        return roadPosition;
    }

    public override fun ClearSelectedMapFeatures()
    {
        try
        {
            for (selectedFeature in selectedFeaturesInternalCopy)
            {
                ClearSpecificHighlight(selectedFeature);
            }
            selectedFeaturesInternalCopy.clear();
            lastSelectedFeature = null;
            selectedMapFeatures.clear();
            selectedMapFeature = null;
            mapCamera.SetSelectedMapFeature(null);
            ClearCurrentAdhocReport();
            // ??? Do we clear tappedMapFeatures as well?
            if (::cSharpMapCameraHandler.isInitialized)
            {
                cSharpMapCameraHandler.FeaturesTapped(tappedMapFeatures);
                cSharpMapCameraHandler.FeaturesSelected(selectedMapFeature, selectedMapFeatures);
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    override fun ToggleSelectedMapFeature(mapFeature: MapFeature)
    {
        try
        {
            // ??? What is desired behaviour for updating the selectedMapFeatures and selectedMapFeature?
            if (mapFeature is ServicePointAndTradeSiteFeature || mapFeature is RoadSegmentFeature || mapFeature is AdhocFeature || mapFeature is PointOfInterestFeature || mapFeature is JobFeature)
            {
                if (mapFeature.TappedPosition == null)
                {
                    var changes = HighlightMapFeature(mapFeature);
                    if (changes > 0)
                    {
                        var tempSelectedMapFeatures = mutableListOf<MapFeature>();
                        tempSelectedMapFeatures.add(mapFeature);
                        var lastSelectedFeature = DetermineFinalFeatureSelection(tempSelectedMapFeatures, mapFeature);

                        selectedMapFeature = lastSelectedFeature;
                        mapCamera.SetSelectedMapFeature(selectedMapFeature);
                        selectedMapFeatures = tempSelectedMapFeatures.toMutableList();
                        if (::cSharpMapCameraHandler.isInitialized)
                        {
                            cSharpMapCameraHandler.FeaturesTapped(tappedMapFeatures);
                            cSharpMapCameraHandler.FeaturesSelected(selectedMapFeature, selectedMapFeatures);
                        }
                    }
                }
                else
                {

                }
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    private fun ClearCurrentAdhocReport()
    {
        if (adhocReport != null)
        {
            mapStyle.symbolsLayer.RemoveSymbols(listOf(adhocReport!!));
            adhocReport = null;
        }
    }

    private var getRoadFeatureslatch = CountDownLatch(1);
    private var getSourceFeatureslatch = CountDownLatch(1);
    private var getRoadPositionLatch = CountDownLatch(1);
    private var roadQueryReturned = false
    private var roadFeaturesFound = false;
    private var roadFeatures: MutableList<QueriedRenderedFeature>? = null;
    private var sourceFeatures: MutableList<QueriedSourceFeature>? = null;

    private fun GetRoadFeaturesAtLocation(rect: ScreenBox, layerIds: List<String>): Boolean
    {
        roadFeaturesFound = false;
        //mapView.mapboxMap.executeOnRenderThread() {
        Handler(Looper.getMainLooper()).post({
            mapView.mapboxMap.queryRenderedFeatures(
                RenderedQueryGeometry(rect),
                RenderedQueryOptions(layerIds, null)
            )
            {
                if (ConditionalCompile.detailedFeatureSelectionLogging) {
                    Logging.V("Query callback received for ${it.value?.count()}")
                }
                if (it.value?.count() != null && it.value!!.count() > 0) {
                    roadFeatures = it.value;
                    roadFeaturesFound = true;
                }
                getRoadFeatureslatch.countDown();
            };
        });
        return roadFeaturesFound;
    }

    private var possibleSlassifications = listOf<String>( "motorway", "trunk", "primary", "secondary", "tertiary", "street", "link", "service", "pedestrian", "track", "path" );
    private fun FilterHighestPriorityRoadFeature(features: MutableList<QueriedRenderedFeature>): Feature?
    {
        var theRoad: Feature? = null;
        if (features.count() > 0)
        {
            //need to select in the following pecking order - motorway, trunk, primary, secondary?, tertiary, street, link?, service, pedestrian, track, path
            var found = false;
            for (possibleClassification in possibleSlassifications)
            {
                for (feature in features)
                {
                    if (possibleClassification == feature.queriedFeature.feature.getStringProperty("class"))
                    {
                        theRoad = feature.queriedFeature.feature;
                        if (ConditionalCompile.detailedFeatureSelectionLogging) {
                            Logging.V("Found highest priority road feature - ${possibleClassification}");
                        }
                        found = true;
                        break;
                    }
                }
                if (found == true)
                {
                    break;
                }
            }
            if (found == false)
            {
                theRoad = features.first().queriedFeature.feature;
            }
        }
        return theRoad;
    }

    private fun GetListOfPointsForGeometry(theRoad: Feature): List<Point>
    {
        // Convert the road geometry to list of points for easier comparison
        var theRoadPoints = mutableListOf<Point>();
        if (theRoad.geometry() is LineString)
        {
            theRoadPoints = com.mapbox.turf.TurfMeta.coordAll(theRoad.geometry() as LineString);
            if (ConditionalCompile.detailedFeatureSelectionLogging) {
                Logging.V("Road ID=${theRoad.id()} is LineString of ${theRoadPoints.count()} points.");
            }
        }
        else if (theRoad.geometry() is MultiLineString)
        {
            theRoadPoints = com.mapbox.turf.TurfMeta.coordAll(theRoad.geometry() as MultiLineString);
            if (ConditionalCompile.detailedFeatureSelectionLogging) {
                Logging.V("Road ID=${theRoad.id()} is MultiLineString of ${theRoadPoints.count()} points.");
            }
        }

        return theRoadPoints;
    }

    private fun GetSourceFeaturesForVisibleMap()//: List<Feature>
    {
        //mapView.mapboxMap.executeOnRenderThread()
        Handler(Looper.getMainLooper()).post(
        {
            mapView.mapboxMap.querySourceFeatures("composite", SourceQueryOptions(listOf<String>("road"), Expression.all()), )
            {
                if (ConditionalCompile.detailedFeatureSelectionLogging) {
                    Logging.V("Query callback received for ${it.value?.count()}")
                }
                if (it.value?.count() != null && it.value!!.count() > 0) {
                    sourceFeatures = it.value;
                }
                getSourceFeatureslatch.countDown();
            };
        });
    }

    private fun GetBestMatchRoadFeature(theRoad: Feature, position: GeographicPosition): RoadPosition?
    {
        var roadPosition: RoadPosition? = null;
        if (roadFeatures != null && sourceFeatures != null)
        {
            var featureMatches = mutableListOf<FeatureMatch>();
            var theRoadPoints = GetListOfPointsForGeometry(theRoad);

            for (sourceFeature in sourceFeatures!!)
            {
                var ID = sourceFeature.queriedFeature.feature.id();
                var name = sourceFeature.queriedFeature.feature.getProperty("name");
                var nameString = "";
                if (name != null) {
                    nameString = name.asString;
                }
                var reference = sourceFeature.queriedFeature.feature.getProperty("ref");
                var referenceString = "";
                if (reference != null) {
                    referenceString = reference.asString;
                }
                if (ConditionalCompile.detailedFeatureSelectionLogging) {
                    Logging.V("Found source feature with name=${name}.");
                }

                var theFeaturePoints = mutableListOf<Point>();
                if (sourceFeature.queriedFeature.feature.geometry() is LineString)
                {
                    theFeaturePoints = com.mapbox.turf.TurfMeta.coordAll(sourceFeature.queriedFeature.feature.geometry() as LineString);
                    if (ConditionalCompile.detailedFeatureSelectionLogging) {
                        Logging.V("Source Feature ID=${ID} is LineString of ${theFeaturePoints.count()} points.");
                    }
                }
                else if (sourceFeature.queriedFeature.feature.geometry() is MultiLineString)
                {
                    theFeaturePoints = com.mapbox.turf.TurfMeta.coordAll(sourceFeature.queriedFeature.feature.geometry() as MultiLineString);
                    //var mls = sourceFeature.queriedFeature.feature.geometry() as MultiLineString;
                    //var sls = mls.lineStrings();
                    if (ConditionalCompile.detailedFeatureSelectionLogging) {
                        Logging.V("Source Feature ID=${ID} is MultiLineString of ${theFeaturePoints.count()} points.");
                    }
                }
                var matchCount = MatchPoints(theRoadPoints, theFeaturePoints);
                if (matchCount > 0) {
                    if (ConditionalCompile.detailedFeatureSelectionLogging) {
                        if (matchCount == theRoadPoints.count())
                        {
                            Logging.V("Full match (${matchCount} of ${theRoadPoints.count()}) to ${nameString}");
                        }
                        else
                        {
                            Logging.V("Partial match (${matchCount} of ${theRoadPoints.count()}) to ${nameString}");
                        }
                        if (sourceFeature.queriedFeature.feature.geometry() is LineString)
                        {
                            Logging.V("Source Feature is LineString.");
                        }
                        else if (sourceFeature.queriedFeature.feature.geometry() is MultiLineString)
                        {
                            var mls = sourceFeature.queriedFeature.feature.geometry() as MultiLineString;
                            var sls = mls.lineStrings();
                            Logging.V("Source Feature is MultiLineString with ${sls.count()} LineStrings");
                        }
                    }

                    featureMatches.add(FeatureMatch(matchCount, theRoadPoints.count(), nameString, referenceString));
                }
            }

            var classification = theRoad.getProperty("class");
            var classificationString = "";
            if (classification != null) {
                classificationString = classification.asString;
            }

            if (featureMatches.count() > 0)
            {
                featureMatches.sortBy{ it.MatchingPoints };
                var bestMatchFeature: FeatureMatch = featureMatches.last();
                if (ConditionalCompile.detailedFeatureSelectionLogging) {
                    Logging.V("Best partial match = ${bestMatchFeature.RoadName}, ${bestMatchFeature.Reference}");
                }

                var accuracy: Double = bestMatchFeature.MatchingPoints.toDouble() / bestMatchFeature.FeaturePoints;
                roadPosition = RoadPosition(classificationString, bestMatchFeature.RoadName, bestMatchFeature.Reference, accuracy, accuracy > 0.3, position);
            }
            else
            {
                roadPosition = RoadPosition("", "", "", 0.0, false, position);
                if (ConditionalCompile.detailedFeatureSelectionLogging) {
                    Logging.V("No partial Source feature match found.");
                }
            }

        }

        return roadPosition;
    }

    private class FeatureMatch(val MatchingPoints: Int, val FeaturePoints: Int, val RoadName: String, val Reference: String)
    {

    }

    private fun MatchPoints(a: List<Point>, b: List<Point>): Int
    {
        var pointMatchCount = 0;
        var pointCount = 0;

        for (pointA in a)
        {
            if (b.count() > pointCount)
            {
                var pointB = b[pointCount];
                if (pointA.longitude().equals(pointB.longitude()) && pointA.latitude().equals(pointB.latitude()))
                {
                    pointMatchCount++;
                }
            }
            pointCount++;
        }
        if (ConditionalCompile.detailedFeatureSelectionLogging) {
            Logging.V("Match data - point count = ${pointCount}, of which match = ${pointMatchCount}.");
        }

        return pointMatchCount;
    }

    fun RegisterForCallback(featureSelectionHandler: IFeatureSelectionCallback)
    {
        cSharpMapCameraHandler = featureSelectionHandler;
    }

    override fun LocationMarkerInternalCallback(
        newPosition: GeographicPosition,
        newText: String,
        newBearing: Double,
        newSpeed: Double
    )
    {
        numberOfLocationUpdatesUntilRoadPositionAutoDetect--;
        if (numberOfLocationUpdatesUntilRoadPositionAutoDetect <=0)
        {
            numberOfLocationUpdatesUntilRoadPositionAutoDetect = autoRoadPositionDetectRate;
            if (autoDetectRoadPosition == true)
            {
                GetRoadPosition(newPosition, false);
            }
        }
    }

    private fun DetectRouteFeatures(position: GeographicPosition): MutableList<QueriedRenderedFeature>
    {
        Logging.V("DetectRouteFeature called with position=${position.Longitude},${position.Latitude}.");
        var features = mutableListOf<QueriedRenderedFeature>();
        val zoom = mapCamera.GetMapCameraPosition().Zoom;
        if (zoom != null && zoom.Value > 15.0)
        {

            var screenLocation = mapView.mapboxMap
                .pixelForCoordinate(Point.fromLngLat(position.Longitude, position.Latitude));

            var thisMethodLatch = CountDownLatch(1);

            // Note: We cannot repeatedly call mapbox queryRenderedFeatures on UI thread as the callbacks do not happen until this method returns,
            // so run a worker thread to do the loop, and call queryRenderedFeatures on mapbox render thread, using a countdown latch to wait for the callback.
            Thread(Runnable
            {
                var offset: Double = 1.0;
                var loop = 0;
                var repeatAFewMore: Int = 2;
                while (offset < 70.0 && (features.count() == 0 || repeatAFewMore > 0))
                {
                    //featureQueryReturned = false;
                    val rect: ScreenBox = ScreenBox(
                        ScreenCoordinate(
                            screenLocation.x - offset,
                            screenLocation.y - offset
                        ),
                        ScreenCoordinate(
                            screenLocation.x + offset,
                            screenLocation.y + offset
                        )
                    );
                    features = GetFeaturesAtLocation(rect, featurelayerIds);
                    if (ConditionalCompile.detailedFeatureSelectionLogging) {
                        Logging.V("At Offset=${offset}, detected featuresCount=${features.count()}, loop=${loop}");
                    }
                    if (features.count() > 0)
                    {
                        repeatAFewMore--;
                    }
                    offset += 5.0;
                    loop++;
                }
                thisMethodLatch.countDown();
            }).start();

            thisMethodLatch.await();
        }
        return features;
    }

    private fun ProcessFoundFeatures(position: GeographicPosition, features: MutableList<QueriedRenderedFeature>)
    {
        var count = features.count();
        if (ConditionalCompile.detailedFeatureSelectionLogging) {
            Logging.D("There are ${count} features.");
        }
        var tempTappedMapFeatures = mutableListOf<MapFeature>();
        var index = 0;
        for (feature in features)
        {
            var mapFeature: MapFeature? = ReCreateMapFeature(position, feature.queriedFeature);
            if (mapFeature != null && AddToTappedFeaturesIfUnique(tempTappedMapFeatures, mapFeature) == true)
            {
                Logging.V("Feature (${index}) type=${mapFeature::class}, UniqueGuid=${mapFeature.UniqueGuid.toString()}");
            }
            index++;
        }
        // Following sort is required where 2 road segments with comments at same location are tapped to select and tapped to deselect as they are received in different order which prevents the deselection.
        tempTappedMapFeatures.sortBy { it.UniqueGuid };
        SortTappedFeatures(tempTappedMapFeatures);
        SortFlatsAtSameLocation(tempTappedMapFeatures);

        val keyMapFeature = DetermineKeyFeature(tempTappedMapFeatures);

        var tempSelectedMapFeatures = SelectFeaturesSameTypeAndLocationAsSample(tempTappedMapFeatures, keyMapFeature);

        tappedMapFeatures = tempTappedMapFeatures.toMutableList();

        if (MultipleSelectionAllowed == false || (MultipleSelectionAllowed == true && (keyMapFeature is ServicePointAndTradeSiteFeature || keyMapFeature is RoadSegmentFeature)))
        {
            val lastSelectedFeature = DetermineFinalFeatureSelection(tempSelectedMapFeatures, keyMapFeature);
            HighlightMapFeature(lastSelectedFeature);

            selectedMapFeature = lastSelectedFeature;
            mapCamera.SetSelectedMapFeature(selectedMapFeature);
            selectedMapFeatures = tempSelectedMapFeatures.toMutableList();
        }

        if (::cSharpMapCameraHandler.isInitialized)
        {
            cSharpMapCameraHandler.FeaturesTapped(tappedMapFeatures);
            cSharpMapCameraHandler.FeaturesSelected(selectedMapFeature, selectedMapFeatures);
        }

    }

    private fun AddToTappedFeaturesIfUnique(mapFeatures: MutableList<MapFeature>, newMapFeature: MapFeature): Boolean
    {
        var canAdd = true;
        val newFeatureType = newMapFeature::class;
        for (mapFeature in mapFeatures)
        {
            if (mapFeature::class == newFeatureType)
            {
                if (newMapFeature.UniqueGuid == mapFeature.UniqueGuid)
                {
                    canAdd = false;
                    Logging.V("Will ignore new mapfeature of type ${newFeatureType.simpleName} as there is one of the same type with same uniqueGuid ${newMapFeature.UniqueGuid}.");
                    break;
                }
            }
        }
        if (canAdd == true)
        {
            mapFeatures.add(newMapFeature);
        }
        return canAdd;

    }

    private fun SortTappedFeatures(mapFeatures: MutableList<MapFeature>)
    {
        // The sort order will be the reverse of the order that we move feature types to the top of the list.
        MoveFeaturesToTopOfList(mapFeatures, AdhocFeature::class.simpleName!!);
        MoveFeaturesToTopOfList(mapFeatures, PointOfInterestFeature::class.simpleName!!);
        MoveFeaturesToTopOfList(mapFeatures, JobFeature::class.simpleName!!);
        MoveFeaturesToTopOfList(mapFeatures, ServicePointAndTradeSiteFeature::class.simpleName!!, isTrade = true);
        MoveFeaturesToTopOfList(mapFeatures, ServicePointAndTradeSiteFeature::class.simpleName!!, isTrade = false);
        MoveFeaturesToTopOfList(mapFeatures, RouteRoadSegmentFeature::class.simpleName!!);
        MoveFeaturesToTopOfList(mapFeatures, RouteRoadSegmentNonServiceFeature::class.simpleName!!);
        MoveFeaturesToTopOfList(mapFeatures, RoadSegmentFeature::class.simpleName!!);
    }

    private fun MoveFeaturesToTopOfList(mapFeatures: MutableList<MapFeature>, featureType: String, isTrade: Boolean? = null)
    {
        val numberInList = mapFeatures.count();
        for (index in 0..numberInList-1)
        {
            if (mapFeatures[index]::class.simpleName == featureType)
            {
                if (isTrade == null)
                {
                    Logging.V("Move ${featureType} to top of list.");
                    val removed = mapFeatures.removeAt(index);
                    mapFeatures.add(0, removed);
                }
                else if (mapFeatures[index] is ServicePointAndTradeSiteFeature && (mapFeatures[index] as ServicePointAndTradeSiteFeature).IsTrade == isTrade)
                {
                    Logging.V("Move ${featureType} (isTrade=${isTrade}) to top of list.");
                    val removed = mapFeatures.removeAt(index);
                    mapFeatures.add(0, removed);
                }
            }
        }
    }

    private fun SortFlatsAtSameLocation(mapFeatures: MutableList<MapFeature>)
    {
        // When a set of service points at the same location (flats a-d for example) then we should sort these so they are displayed in order in the report view (and then serviced view).
        if (mapFeatures.count() > 1)
        {
            var filteredListOfServicePointFeatures: List<ServicePointAndTradeSiteFeature> = mapFeatures.filterIsInstance<ServicePointAndTradeSiteFeature>();
            if (filteredListOfServicePointFeatures.count() == mapFeatures.count())
            {
                Logging.V("Sorted ${filteredListOfServicePointFeatures.count()} flats.");
                mapFeatures.clear();
                mapFeatures.addAll(filteredListOfServicePointFeatures);
                mapFeatures.sortBy { (it as ServicePointAndTradeSiteFeature).Flat };
            }
        }

    }

    // We consider the first road segment feature (any of 3 types) to be key feature, followed by the first service point, followed by the first other feature.
    private fun DetermineKeyFeature(tappedMapFeatures: MutableList<MapFeature>): MapFeature?
    {
        val firstRoadSegment = tappedMapFeatures.firstOrNull(){ it is RoadSegmentFeature || it is RouteRoadSegmentNonServiceFeature || it is RouteRoadSegmentFeature };
        val firstServicePoint = tappedMapFeatures.firstOrNull(){ it is ServicePointAndTradeSiteFeature };
        var keyMapFeature: MapFeature? = tappedMapFeatures.firstOrNull();
        if (firstRoadSegment != null)
        {
            Logging.V("The initial key feature is a ${firstRoadSegment::class.simpleName} with UniqueId${firstRoadSegment.UniqueGuid}.");
            keyMapFeature = firstRoadSegment;
            var justRoadSegments = tappedMapFeatures.filter { it::class.simpleName == "RoadSegmentFeature" };
            if (justRoadSegments.count() > 1)
            {
                if (ConditionalCompile.detailedFeatureSelectionLogging) {
                    Logging.V("However there was more than one Road Segment so checking if another one is more appropriate to use...");
                }
                for (justRoadSegment in justRoadSegments)
                {
                    if (ConditionalCompile.detailedFeatureSelectionLogging) {
                        Logging.V("Checking road segment with UniqueId=${justRoadSegment.UniqueGuid}.");
                    }
                    var relatedSegments = tappedMapFeatures.filter { it is RouteRoadSegmentNonServiceFeature && it.RoadGuid == justRoadSegment.UniqueGuid }
                    if (relatedSegments.any() == true)
                    {
                        Logging.V("Road Segment (${justRoadSegment.UniqueGuid}) has ${relatedSegments.count()} related route segments so will use this as key map feature instead.")
                        keyMapFeature = justRoadSegment;
                    }
                }
            }
        }
        else if (firstServicePoint != null)
        {
            Logging.V("The key feature is a ${firstServicePoint::class.simpleName}.");
            keyMapFeature = firstServicePoint;
        }
        else
        {
            if (keyMapFeature == null)
            {
                Logging.V("The key feature is a null.");
            }
            else
            {
                Logging.V("The key feature is a ${keyMapFeature::class.simpleName}.");
            }
        }
        return keyMapFeature;
    }

    private fun SelectFeaturesSameTypeAndLocationAsSample(tappedMapFeatures: MutableList<MapFeature>, sample: MapFeature?): MutableList<MapFeature>
    {
        var selectedFeatures = mutableListOf<MapFeature>();
        var additionalSelectedFeatures = mutableListOf<MapFeature>();
        if (sample != null)
        {
            // Currently we only select if a service point feature (not trade) or one of the three road segment types.
            Logging.V("Request to select features of type ${sample::class.simpleName}.");
            for (index in tappedMapFeatures.indices)
            {
                var feature = tappedMapFeatures[index];
                if ((feature is RoadSegmentFeature || feature is RouteRoadSegmentNonServiceFeature || feature is RouteRoadSegmentFeature) &&
                    (sample is RoadSegmentFeature || sample is RouteRoadSegmentNonServiceFeature || sample is RouteRoadSegmentFeature))
                {
                    Logging.V("Feature ${index} is type ${feature::class.simpleName}");
                    if ((feature as RoadSegmentFeature).RoadGuid == (sample as RoadSegmentFeature).RoadGuid)
                    {
                        Logging.V(" and also has same underlying RoadGuid so adding to selected features.")
                        feature.DuplicatePosition = true;
                        selectedFeatures.add(feature);
                    }
                    else if (feature is RouteRoadSegmentFeature || feature is RouteRoadSegmentNonServiceFeature)
                    {
                        if (CheckIfGeometryIsRelated(feature, sample) == true)
                        {
                            Logging.V(" and also some of underlying geometry the same so adding to additional selected features.")
                            feature.DuplicatePosition = true;
                            additionalSelectedFeatures.add(feature);
                        }
                    }
                }
                else if (feature is ServicePointAndTradeSiteFeature && feature.IsTrade == false && sample is ServicePointAndTradeSiteFeature && sample.IsTrade == false)
                {
                    Logging.V("Feature ${index} is type ${feature::class.simpleName} with IsTrade=${feature.IsTrade}.");
                    var startPoint = Point.fromLngLat(feature.Longitude, feature.Latitude);
                    var endPoint = Point.fromLngLat(sample.Longitude, sample.Latitude);

                    var distance = TurfMeasurement.distance(startPoint, endPoint);
                    if (distance < 0.0005) // km so this is 0.5m
                    {
                        Logging.V(" and is within 0.5m so adding to selected features.")
                        feature.DuplicatePosition = true;
                        selectedFeatures.add(feature);
                    }
                }
                else if (feature is JobFeature && sample is JobFeature)
                {
                    Logging.V("Feature ${index} is type ${feature::class.simpleName} with IsTrade=${feature.IsTrade}.");
                    var startPoint = Point.fromLngLat(feature.Longitude, feature.Latitude);
                    var endPoint = Point.fromLngLat(sample.Longitude, sample.Latitude);

                    var distance = TurfMeasurement.distance(startPoint, endPoint);
                    if (distance < 0.0005) // km so this is 0.5m
                    {
                        Logging.V(" and is within 0.5m so adding to selected features.")
                        feature.DuplicatePosition = true;
                        selectedFeatures.add(feature);
                    }
                }
                else if (selectedFeatures.isEmpty() == true)
                {
                    // In this case we will only want to include the sample feature.
                    selectedFeatures.add(sample);
                    break;
                }
            }
            if ((sample is RoadSegmentFeature || sample is RouteRoadSegmentNonServiceFeature || sample is RouteRoadSegmentFeature))
            {
                if (selectedFeatures.count() ==1)
                {
                    selectedFeatures.addAll(additionalSelectedFeatures);
                    Logging.V("Have added additional ${additionalSelectedFeatures.count()} selected features.")
                }
            }
        }
        Logging.D("There were ${selectedFeatures.count()} features selected.")
        return selectedFeatures; // ??? TBD
    }

    private fun CheckIfGeometryIsRelated(roadFeatureA: RoadSegmentFeature, roadFeatureB: RoadSegmentFeature): Boolean
    {
        var related: Boolean = false;
        var a = roadFeatureA.ShapeGeoJSON.substringAfter("[");
        a = a.substringBeforeLast("]");
        var b = roadFeatureB.ShapeGeoJSON.substringAfter("[");
        b = b.substringBeforeLast("]");
        if (a.contains(b) || b.contains(a))
        {
            related = true;
        }
        return related;
    }

    private fun HighlightMapFeature(mapFeature: MapFeature?): Int
    {
        var changes = 0;
        if (mapFeature != null)
        {
            Logging.D("Highlight a feature of type ${mapFeature::class.simpleName}.")
            if (mapFeature is RouteRoadSegmentFeature || mapFeature is RouteRoadSegmentNonServiceFeature || mapFeature is RoadSegmentFeature)
            {
                var highlightProperty = RoadSegmentHighlightedProperty();
                highlightProperty.Highlighted = true;
                highlightProperty.RouteOrRoadSegmentGuid = mapFeature.UniqueGuid;
                changes = mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf<RoadSegmentPropertyBase>(highlightProperty));
            }
            else if (mapFeature is ServicePointAndTradeSiteFeature)
            {
                var highlightedProperty = ServicePointHighlightedProperty();
                highlightedProperty.Highlighted = true;
                highlightedProperty.ServicePointAndTradeSiteGuid = mapFeature.UniqueGuid;
                changes = mapStyle.servicePointAndTradeSiteLayer.ChangeServicePointAndTradeSiteProperties(listOf<ServicePointHighlightedProperty>(highlightedProperty));
            }
            else if (mapFeature is JobFeature)
            {
                var highlightedProperty = JobHighlightedProperty();
                highlightedProperty.Highlighted = true;
                highlightedProperty.JobInstanceGuid = mapFeature.UniqueGuid;
                changes = mapStyle.jobLayer.ChangeJobProperties(listOf<JobPropertyBase>(highlightedProperty));
            }
            else if (mapFeature is AdhocFeature || mapFeature is PointOfInterestFeature)
            {
                var highlightedProperty = SymbolHighlightedProperty();
                highlightedProperty.Highlighted = true;
                highlightedProperty.SymbolGuid = mapFeature.UniqueGuid;
                changes = mapStyle.symbolsLayer.ChangeSymbolProperties(listOf<SymbolPropertyBase>(highlightedProperty));
            }
        }
        return changes;
    }

    private fun ClearCurrentHighlight()
    {
        if (lastSelectedFeature != null)
        {
            ClearSpecificHighlight(lastSelectedFeature!!);
        }
    }

    private fun ClearSpecificHighlight(mapFeature: MapFeature)
    {
        Logging.D("Clear the highlight of a feature of type ${mapFeature::class.simpleName}.")
        if (mapFeature is ServicePointAndTradeSiteFeature)
        {
            var highlightedProperty = ServicePointHighlightedProperty();
            highlightedProperty.Highlighted = false;
            highlightedProperty.ServicePointAndTradeSiteGuid = mapFeature.UniqueGuid;
            mapStyle.servicePointAndTradeSiteLayer.ChangeServicePointAndTradeSiteProperties(listOf<ServicePointHighlightedProperty>(highlightedProperty));
        }
        else if (mapFeature is RoadSegmentFeature)
        {
            var highlightProperty = RoadSegmentHighlightedProperty();
            highlightProperty.Highlighted = false;
            highlightProperty.RouteOrRoadSegmentGuid = mapFeature.UniqueGuid;
            mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf<RoadSegmentPropertyBase>(highlightProperty));
        }
        else if (mapFeature is AdhocFeature || mapFeature is PointOfInterestFeature)
        {
            var highlightedProperty = SymbolHighlightedProperty();
            highlightedProperty.Highlighted = false;
            highlightedProperty.SymbolGuid = mapFeature.UniqueGuid;
            mapStyle.symbolsLayer.ChangeSymbolProperties(listOf<SymbolPropertyBase>(highlightedProperty));
        }
        else if (mapFeature is JobFeature)
        {
            var highlightedProperty = JobHighlightedProperty();
            highlightedProperty.Highlighted = false;
            highlightedProperty.JobInstanceGuid = mapFeature.UniqueGuid;
            mapStyle.jobLayer.ChangeJobProperties(listOf<JobPropertyBase>(highlightedProperty));
        }
        else
        {

        }

    }

    private var lastSelectedFeature: MapFeature? = null;
    private fun DetermineFinalFeatureSelection(mapFeatures: MutableList<MapFeature>, keyMapFeature: MapFeature?): MapFeature? // ??? TBD Better name required for method.
    {
        if (keyMapFeature == null)
        {
            Logging.D("Called with a list of ${mapFeatures.count()} features and a null key map feature.");
            if (MultipleSelectionAllowed == false)
            {
                Logging.V("Multiple selection not allowed, so clearing all highlights and selectedMapFeatures.")
                if (keyMapFeature is AdhocFeature == false)
                {
                    ClearCurrentAdhocReport();
                }
                while (selectedFeaturesInternalCopy.count() > 0)
                {
                    var lastFeature = selectedFeaturesInternalCopy.last();
                    ClearSpecificHighlight(lastFeature);
                    selectedFeaturesInternalCopy.remove(lastFeature);
                }
                mapFeatures.clear();
                lastSelectedFeature = null;
            }
        }
        else
        {
            Logging.D("Called with a list of ${mapFeatures.count()} features and a key map feature of type ${keyMapFeature::class.simpleName}.");
            if (MultipleSelectionAllowed == false)
            {
                if (keyMapFeature is AdhocFeature == false)
                {
                    ClearCurrentAdhocReport();
                }

                if (selectedFeaturesInternalCopy.count() > 0)
                {
                    Logging.V("Clearing highlights for previously selected ${selectedFeaturesInternalCopy.count()} features.")
                }
                while (selectedFeaturesInternalCopy.count() > 0)
                {
                    var lastFeature = selectedFeaturesInternalCopy.last();
                    ClearSpecificHighlight(lastFeature);
                    selectedFeaturesInternalCopy.remove(lastFeature);
                }

                if (lastSelectedFeature == null || (keyMapFeature.UniqueGuid != lastSelectedFeature!!.UniqueGuid))
                {
                    for (mapFeature in mapFeatures)
                    {
                        selectedFeaturesInternalCopy.add(mapFeature);
                    }
                    lastSelectedFeature = keyMapFeature;
                }
                else
                {
                    lastSelectedFeature = null;
                }

            }
            else if (MultipleSelectionAllowed == true && (keyMapFeature is RoadSegmentFeature || (keyMapFeature is ServicePointAndTradeSiteFeature && keyMapFeature.IsTrade == false)))
            {
                if (lastSelectedFeature == null)
                {
                    Logging.D("multipleSelectionAllowed=${MultipleSelectionAllowed}, lastSelectedFeature=null, keyMapFeature=${keyMapFeature::class.simpleName}.");
                }
                else
                {
                    Logging.D("multipleSelectionAllowed=${MultipleSelectionAllowed}, lastSelectedFeature=${lastSelectedFeature!!::class.simpleName}, keyMapFeature=${keyMapFeature::class.simpleName}.");
                }
                if (lastSelectedFeature == null || (lastSelectedFeature is RoadSegmentFeature && keyMapFeature is RoadSegmentFeature) || (lastSelectedFeature is ServicePointAndTradeSiteFeature && (keyMapFeature is ServicePointAndTradeSiteFeature && keyMapFeature.IsTrade == false)))
                {
                    for (tappedMapFeature in mapFeatures)
                    {
                        var found = selectedFeaturesInternalCopy.firstOrNull{ item -> item.UniqueGuid == tappedMapFeature.UniqueGuid };
                        if (found == null)
                        {
                            selectedFeaturesInternalCopy.add(tappedMapFeature);
                            lastSelectedFeature = keyMapFeature;
                        }
                        else
                        {
                            selectedFeaturesInternalCopy.remove(found);
                            ClearSpecificHighlight(found);
                            if (selectedFeaturesInternalCopy.count() > 0)
                            {
                                lastSelectedFeature = selectedFeaturesInternalCopy.last();
                            }
                            else
                            {
                                lastSelectedFeature = null;
                            }
                        }
                    }
                }
            }
            mapFeatures.clear();

            // We need to get updated versions of the features in case any of them have changed properties.
            // This is only relevant in SmartSuite Mobile for service points.
            for(featureInternalCopy in selectedFeaturesInternalCopy)
            {
                if (featureInternalCopy is ServicePointAndTradeSiteFeature)
                {
                    val servicePoint =
                        mapStyle.servicePointAndTradeSiteLayer.FindServicePointsAndTradeSites(featureInternalCopy.UniqueGuid);
                    if (servicePoint != null)
                    {
                        featureInternalCopy.Serviced = servicePoint.Serviced;
                        featureInternalCopy.Reported = servicePoint.Reported;
                        featureInternalCopy.Actioned = servicePoint.Actioned;
                    }
                }
            }
            //var mapFeature: MapFeature? = CreateMapFeature(position, feature.feature);
            mapFeatures.addAll(selectedFeaturesInternalCopy);
        }
        return lastSelectedFeature;
    }

    private var getFeatureslatch = CountDownLatch(1);

    private fun GetFeaturesAtLocation(rect: ScreenBox, layerIds: List<String>): MutableList<QueriedRenderedFeature>
    {
        getFeatureslatch = CountDownLatch(1);
        var features = mutableListOf<QueriedRenderedFeature>();
        mapView.mapboxMap.executeOnRenderThread()
        {
            mapView.mapboxMap.queryRenderedFeatures(
                RenderedQueryGeometry(rect),
                RenderedQueryOptions(layerIds, null)
            )
            {
                if (ConditionalCompile.detailedFeatureSelectionLogging) {
                    Logging.V("Query callback received for ${it.value?.count()} features detected at location.")
                }
                if (it.value?.count() != null && it.value!!.count() > 0)
                {
                    features = it.value!!;
                    Logging.V("Features now has ${features.count()} values?")
                }
                getFeatureslatch.countDown();
            };
        };
        getFeatureslatch.await()
        return features;
    }

    private fun ReCreateMapFeature(newLocation: GeographicPosition, feature: QueriedFeature): MapFeature?
    {
        var tappedMapFeature: MapFeature? = null;
        try
        {
            if (feature.feature.geometry() is LineString || feature.feature.geometry() is MultiLineString)
            {
                tappedMapFeature = mapStyle.roadSegmentLayer.ReCreateRoadSegmentFeature(newLocation, feature.feature);
            }
            else if (feature.feature.geometry() is Point)
            {
                var properties: JsonObject? = feature.feature.properties();

                if (properties != null)
                {
                    var symbolProperty = properties.get("symbol");
                    var symbol = "";
                    if (symbolProperty != null)
                    {
                        symbol = symbolProperty.asString;
                    }

//                    var textProperty = properties.get("text");
//                    var text = "";
//                    if (textProperty != null)
//                    {
//                        text = textProperty.asString;
//                    }

                    var isOverdueProperty = properties.get("overdue"); // ??? TBD

                    if (symbol.isNullOrEmpty() == false)
                    {
                        if (symbol == SymbolType.Adhoc.name)
                        {
                            tappedMapFeature = mapStyle.symbolsLayer.CreateAdhocFeature(newLocation, feature.feature);
                        }
                        else if (symbol == SymbolType.PoI.name)
                        {
                            tappedMapFeature = mapStyle.symbolsLayer.CreatePointOfInterestFeature(newLocation, feature.feature);
                        }
                        else
                        {
                            // We don't create map feature for other symbols.
                        }
                    }
                    else if (isOverdueProperty != null)
                    {
                        tappedMapFeature = mapStyle.jobLayer.CreateJobFeature(newLocation, feature.feature);

                    }
                    else
                    {
                        tappedMapFeature = mapStyle.servicePointAndTradeSiteLayer.CreateServicePointAndTradeSiteFeature(newLocation, feature.feature);
                    }
                }
            }

        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
        return tappedMapFeature;
    }

    private val roadLayerIds = listOf<String>(
        "road-path-bg",
        "road-steps-bg",
        "road-sidewalk-bg",
        "road-pedestrian-case",
        "road-street-low",
        "road-street_limited-low",
        "road-service-link-track-case",
        "road-street_limited-case",
        "road-street-case",
        "road-secondary-tertiary-case",
        "road-primary-case",
        "road-motorway_link-case",
        "road-trunk_link-case",
        "road-trunk-case",
        "road-motorway-case",
        "road-motorway-trunk-case",
        "road-construction",
        "road-sidewalks",
        "road-path",
        "road-steps",
        "road-trunk_link",
        "road-motorway_link",
        "road-pedestrian",
        "road-pedestrian-polygon-fill",
        "road-pedestrian-polygon-pattern",
        "road-polygon",
        "road-service-link-track",
        "road-street_limited",
        "road-street",
        "road-secondary-tertiary",
        "road-primary",
        "road-oneway-arrows-blue-minor",
        "road-oneway-arrows-blue-major",
        "road-trunk",
        "road-motorway",
        "road-rail",
        "road-rail-tracks",
        "road-oneway-arrows-white"
    );

    private val featurelayerIds = listOf<String>(
        // The following from the Road Segment Layer.
        //"roadSegment_directionArrowUnservicedLayer",
        "roadSegment_nonServiceLineLayer",
        "roadSegment_partiallyServicedUnderLineLayer",
        "roadSegment_unservicedLineLayer",
        "roadSegment_partiallyServicedLineLayer",
        "roadSegment_servicedLineLayer",
        //"roadSegment_directionArrowServicedLayer",

        //"roadSegment_commentLayer",
        "roadSegment_roadCommentLayer",
        //"roadSegment_commentForegroundLayer",
        //"highlightLineLayer",
        //"snappedLineLayer",
        //"snappedLineLayerAgainst",
        //"manoeuvreRouteSegment",
        //"manoeuvreAfterRouteSegment",
        //"roadSegment_reportLayer",
        //"reportLineLayerSide1",
        //"reportLineLayerSide2",
        //"roadSegment_pathThoTargetLayer",

        // The following from the Service Point and Trade Site layer.
        "servicePoint_unservicedLayerRoute",
        "servicePoint_unservicedLayerTrade",
        "servicePoint_unservicedLargeLayerRoute",
        "servicePoint_unservicedLargeLayerTrade",
        "servicePoint_servicedLayerRoute",
        "servicePoint_servicedLayerTrade",
        "servicePoint_servicedLargeLayerRoute",
        "servicePoint_servicedLargeLayerTrade",
//                "servicePoint_actionLayer",
//                "symbol_redYellowCircleSegmentsRoute",
//                "symbol_greyCircleSegmentsRoute",
//                "symbol_redYellowCircleSegmentsTrade",
//                "symbol_orangeCross",
//                "servicePoint_highlightLayer",
//                "servicePoint_reportLayerRoute",
//                "servicePoint_reportLayerTrade",
//                "servicePoint_actionedLayer",
//                "servicePoint_text",
//                "servicePoint_text2",

        // The following from the Jobs Layer
        "symbol_jobs_OrangeOutline",
        "symbol_jobs_BlueOutline",
        "symbol_jobs_OrangeFilled",
        "symbol_jobs_BlueFilled",
        "symbol_jobs_BlueFilled_Inprogress",
        "symbol_jobs_GreyOutlineFinished",
//                "jobs_reportLayer",
//                "text_job",
//                "jobs_highlightLayer",

        // The following from the Symbol layer.
//                "symbol_text",
        "symbol_poi",
        "symbol_adhoc",
//                "symbol_snappedLocation",
//                "symbol_crumbTrail",
//                "symbol_location",
//                "symbol_location_with_direction",
//                "symbol_locationSearching",
//                "symbol_locationDisabled",
//                "symbol_routeOutline",
//                "symbol_tradeOutline",
//                "symbol_highlight",
    );

}
