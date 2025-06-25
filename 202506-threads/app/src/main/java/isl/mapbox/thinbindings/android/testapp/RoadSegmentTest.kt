package isl.mapbox.thinbindings.android.testapp

import android.util.Log
import isl.mapbox.thinbindings.android.features.RoadSegmentFeature
import com.mapbox.geojson.*
import isl.mapbox.thinbindings.android.features.DirectionOfTravel
import isl.mapbox.thinbindings.android.features.DirectionsOfService
import isl.mapbox.thinbindings.android.features.ManoeuvreType
import isl.mapbox.thinbindings.android.features.NavigationSegmentFeature
import isl.mapbox.thinbindings.android.features.RouteRoadSegmentFeature
import isl.mapbox.thinbindings.android.features.RouteRoadSegmentNonServiceFeature
import isl.mapbox.thinbindings.android.maprendererkotlin.MapRendererKotlin
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.RoadSegmentHighlightedProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.RoadSegmentPropertyBase
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.RouteRoadSegmentServicedProperty
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.LongLatExtent
import java.lang.Math.sqrt
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

public class RoadSegmentTest(theMap: MapRendererKotlin, testExtent: LongLatExtent)
{
    private val TAG = "TestApp";
    private val theMap: MapRendererKotlin = theMap
    private var testExtent = testExtent;

    private fun SetGeometry(roadSegmentFeature: RoadSegmentFeature, lineXIndex: Int, lineYIndex: Int, length: Int = 1, vertical: Boolean = true, hasShape: Boolean = false, numberIntermediatePoints: Int = 0) : RoadSegmentFeature
    {
        var latitude0: Double;
        var longitude0: Double;
        var longitude1: Double;
        var latitude1: Double;
        var longitudeA: Double = 0.0;
        var latitudeA: Double = 0.0;
        var longitudeB: Double = 0.0;
        var latitudeB: Double = 0.0;
        var intermediatePoints = mutableListOf<Point>();

        if (vertical == true)
        {
            latitude0 = defaultMapKeyLatitude + (lineYIndex * latitudeNominalScale);
            latitude1 = latitude0 + (length * latitudeNominalScale);
            longitude0 = defaultMapKeyLongitude + (longitudeNominalScale * lineXIndex);
            longitude1 = longitude0;

            if (hasShape == true)
            {
                latitudeA = latitude0 + (length * latitudeNominalScale / 3.0);
                latitudeB = latitude0 + (length * latitudeNominalScale * 2.0 / 3.0);
                longitudeA = longitude0 + (longitudeNominalScale * length / 2.0);
                longitudeB = longitude0 + (longitudeNominalScale * length / 2.0);
            }
            else if (numberIntermediatePoints > 0)
            {
                for (index in 0..numberIntermediatePoints-1)
                {
                    intermediatePoints += Point.fromLngLat(longitude0, latitude0 + (latitudeNominalScale * length) * index / (numberIntermediatePoints + 1));
                }
            }
        }
        else
        {
            longitude0 = defaultMapKeyLongitude + (lineXIndex * longitudeNominalScale);
            longitude1 = longitude0 + (length * longitudeNominalScale);
            latitude0 = defaultMapKeyLatitude + (latitudeNominalScale * lineYIndex);
            latitude1 = latitude0;

            if (hasShape == true)
            {
                longitudeA = longitude0 + (length * longitudeNominalScale / 3.0);
                longitudeB = longitude0 + (length * longitudeNominalScale * 2.0 / 3.0);
                latitudeA = latitude0 + (latitudeNominalScale * length / 2.0);
                latitudeB = latitude0 - (latitudeNominalScale * length / 2.0);
            }
            else if (numberIntermediatePoints > 0)
            {
                for (index in 1..numberIntermediatePoints)
                {
                    intermediatePoints += Point.fromLngLat(longitude0 + (longitudeNominalScale * length) * index / (numberIntermediatePoints + 1), latitude0);
                }
            }
        }
        var positions: List<Point> = mutableListOf<Point>();
        positions += Point.fromLngLat(longitude0, latitude0);
        if (hasShape == true)
        {
            positions += Point.fromLngLat(longitudeA, latitudeA);
            positions += Point.fromLngLat(longitudeB, latitudeB);
        }
        else if (numberIntermediatePoints > 0)
        {
            for (intermediatePoint in intermediatePoints)
            {
                positions += intermediatePoint;
            }
        }
        positions += Point.fromLngLat(longitude1, latitude1);

        var lineString = LineString.fromLngLats(positions);
        var geoJSONLineString = lineString.toJson();
        roadSegmentFeature.ShapeGeoJSON = geoJSONLineString;

        var BoundingBox = LongLatExtent(GeographicPosition(longitude1, latitude1), GeographicPosition(longitude0, latitude0));
        roadSegmentFeature.BoundingBox = BoundingBox;

        return roadSegmentFeature;
    }

    public fun AddTestRoadSegment(UniqueId: UUID, linePositionXIndex: Int, linePositionYIndex: Int, length: Int = 2, vertical: Boolean = true, text:String = "", note: String = "", numberIntermediatePoints: Int = 0): RoadSegmentFeature
    {
        var roadSegmentFeatures = mutableListOf<RoadSegmentFeature>();

        var roadSegmentFeature = RoadSegmentFeature();
        roadSegmentFeature.UniqueGuid = UniqueId;
        roadSegmentFeature.RoadGuid = UniqueId;
        roadSegmentFeature.Text = text;
        roadSegmentFeature.Comments = note.isNullOrEmpty() == false;
        roadSegmentFeature.Note = note;
        roadSegmentFeature.Length = length.toDouble();

        roadSegmentFeature = SetGeometry(roadSegmentFeature, linePositionXIndex, linePositionYIndex, length = length, vertical = vertical, numberIntermediatePoints = numberIntermediatePoints);
        Log.d(TAG, "Shape=${roadSegmentFeature.ShapeGeoJSON}, bounds=${roadSegmentFeature.BoundingBox}")
        roadSegmentFeatures += roadSegmentFeature;

        testExtent = CalculateBoundingBox(testExtent, roadSegmentFeature.BoundingBox);

        //AddRoadSegmentsFunc?.Invoke(roadSegmentFeatures);
        theMap.mapStyle.roadSegmentLayer.AddRoadSegments(roadSegmentFeatures);

        return  roadSegmentFeature;
    }

    public fun AddTestRouteRoadNonServiceSegment(UniqueId: UUID, RoadId: UUID, linePositionXIndex: Int, linePositionYIndex: Int, sequence: Long, routeDirectionOfTravel: DirectionOfTravel, length: Int = 2, vertical: Boolean = true, text: String = "", reported: Boolean = false, note: String = "", serviceNote: String = "", numberIntermediatePoints: Int = 0, manoeuvreAtEnd: ManoeuvreType = ManoeuvreType.None)
    {
        var routeRoadSegmentNonServiceFeatures = mutableListOf<RouteRoadSegmentNonServiceFeature>();

        var routeRoadSegmentNonServiceFeature = RouteRoadSegmentNonServiceFeature();
        routeRoadSegmentNonServiceFeature.UniqueGuid = UniqueId;
        routeRoadSegmentNonServiceFeature.RoadGuid = RoadId;
        routeRoadSegmentNonServiceFeature.RouteGuid = UniqueId;
        routeRoadSegmentNonServiceFeature.Sequence = sequence;
        routeRoadSegmentNonServiceFeature.RouteDirectionOfTravel = routeDirectionOfTravel;
        routeRoadSegmentNonServiceFeature.Text = text;
        routeRoadSegmentNonServiceFeature.Reported = reported;
        routeRoadSegmentNonServiceFeature.Comments = note.isNullOrEmpty() == false || serviceNote.isNullOrEmpty() == false;
        routeRoadSegmentNonServiceFeature.Note = note;
        routeRoadSegmentNonServiceFeature.ServiceNote = serviceNote;
        routeRoadSegmentNonServiceFeature.ManoeuvreAtEnd = manoeuvreAtEnd;
        routeRoadSegmentNonServiceFeature.Length = length.toDouble();

        routeRoadSegmentNonServiceFeature = SetGeometry(routeRoadSegmentNonServiceFeature, linePositionXIndex, linePositionYIndex, length = length, vertical = vertical, numberIntermediatePoints = numberIntermediatePoints) as RouteRoadSegmentNonServiceFeature;
        routeRoadSegmentNonServiceFeatures += (routeRoadSegmentNonServiceFeature);

        testExtent = CalculateBoundingBox(testExtent, routeRoadSegmentNonServiceFeature.BoundingBox);

        theMap.mapStyle.roadSegmentLayer.AddRoadSegments(routeRoadSegmentNonServiceFeatures)
    }

    public fun AddTestNavigationSegment(UniqueId: UUID, RoadId: UUID, linePositionXIndex: Int, linePositionYIndex: Int, sequence: Long, routeDirectionOfTravel: DirectionOfTravel, length: Int = 2, vertical: Boolean = true, text: String = "", completed: Boolean = false, numberIntermediatePoints: Int = 0): NavigationSegmentFeature
    {
        var navigationSegmentFeatures = mutableListOf<NavigationSegmentFeature>();

        var navigationSegmentFeature = NavigationSegmentFeature();
        navigationSegmentFeature.UniqueGuid = UniqueId;
        navigationSegmentFeature.RoadGuid = RoadId;
        navigationSegmentFeature.RouteGuid = UniqueId;
        navigationSegmentFeature.Sequence = sequence;
        navigationSegmentFeature.RouteDirectionOfTravel = routeDirectionOfTravel;
        navigationSegmentFeature.Text = text;
        navigationSegmentFeature.Reported = false;
        navigationSegmentFeature.Comments = false;
        navigationSegmentFeature.Note = "";
        navigationSegmentFeature.ServiceNote = "";
        navigationSegmentFeature.Completed = completed;
        navigationSegmentFeature.Length = length.toDouble();
        navigationSegmentFeature.ManoeuvreAtEnd = ManoeuvreType.Straight;

        navigationSegmentFeature = SetGeometry(navigationSegmentFeature, linePositionXIndex, linePositionYIndex, length = length, vertical = vertical, numberIntermediatePoints = numberIntermediatePoints) as NavigationSegmentFeature;
        navigationSegmentFeatures += (navigationSegmentFeature);

        testExtent = CalculateBoundingBox(testExtent, navigationSegmentFeature.BoundingBox);

        theMap.mapStyle.roadSegmentLayer.AddRoadSegments(navigationSegmentFeatures);

        return navigationSegmentFeature;
    }

    private var routeRoadSegmentFeatures = mutableListOf<RouteRoadSegmentFeature>();
    public fun AddTestRouteRoadSegment(UniqueId: UUID, RoadId: UUID, linePositionXIndex: Int, linePositionYIndex: Int, sequence: Long, routeDirectionOfTravel: DirectionOfTravel, passesCompleted: Int, passesTotal: Int, serviced: Boolean, directionOfService: DirectionsOfService = DirectionsOfService.With, length: Int = 2, vertical: Boolean = true, text: String = "", note: String = "", serviceNote: String = "", reported: Boolean = false, delayAddToRenderer: Boolean = false, hasShape: Boolean = false, numberIntermediatePoints: Int = 0, manoeuvreAtEnd: ManoeuvreType = ManoeuvreType.None): RouteRoadSegmentFeature
    {
        if (delayAddToRenderer == false)
        {
            routeRoadSegmentFeatures.clear();
        }

        var routeRoadSegmentFeature = RouteRoadSegmentFeature();

        routeRoadSegmentFeature.UniqueGuid = UniqueId;
        routeRoadSegmentFeature.RoadGuid = RoadId;
        routeRoadSegmentFeature.RouteGuid = UniqueId;
        routeRoadSegmentFeature.DirectionOfService = directionOfService;
        routeRoadSegmentFeature.RouteDirectionOfTravel = routeDirectionOfTravel;
        routeRoadSegmentFeature.Sequence = sequence;
        routeRoadSegmentFeature.Serviced = serviced;
        routeRoadSegmentFeature.PassesCompleted = passesCompleted;
        routeRoadSegmentFeature.PassesTotal = passesTotal;
        routeRoadSegmentFeature.Text = text; // "Seq=" + sequence.toString();
        routeRoadSegmentFeature.Reported = reported;
        routeRoadSegmentFeature.Comments = note.isNullOrEmpty() == false || serviceNote.isNullOrEmpty() == false;
        routeRoadSegmentFeature.Note = note;
        routeRoadSegmentFeature.ServiceNote = serviceNote;
        routeRoadSegmentFeature.ManoeuvreAtEnd = manoeuvreAtEnd;
        routeRoadSegmentFeature.Length = length.toDouble();

        routeRoadSegmentFeature = SetGeometry(routeRoadSegmentFeature, linePositionXIndex, linePositionYIndex, length = length, vertical = vertical, hasShape, numberIntermediatePoints = numberIntermediatePoints) as RouteRoadSegmentFeature;
        Log.d(TAG, "Text=${routeRoadSegmentFeature.Text}, Shape=${routeRoadSegmentFeature.ShapeGeoJSON}, bounds=${routeRoadSegmentFeature.BoundingBox}")
        routeRoadSegmentFeatures += routeRoadSegmentFeature;

        testExtent = CalculateBoundingBox(testExtent, routeRoadSegmentFeature.BoundingBox);

        if (delayAddToRenderer == false)
        {
            AddDelayedRoadSegmentsToRenderer();
        }

        return routeRoadSegmentFeature;
    }

    private fun AddDelayedRoadSegmentsToRenderer()
    {
        theMap.mapStyle.roadSegmentLayer.AddRoadSegments(routeRoadSegmentFeatures, UUID.randomUUID().toString());
        routeRoadSegmentFeatures.clear();
    }
    @OptIn(ExperimentalTime::class)
    public fun AddMultipleRoadSegments(numberToAdd: Int)
    {
        var UUIDOfFirst: UUID = UUID.randomUUID();
        var UUIDOfMid: UUID = UUID.randomUUID();
        var UUIDOfLast: UUID = UUID.randomUUID();
        val timeSource = TimeSource.Monotonic;
        val startTime = timeSource.markNow();

        var numberColumns: Int = sqrt(numberToAdd.toDouble()).toInt();
        var numberRendered: Int = 0;
        var columnNumber: Int = 0;
        var rowNumber: Int = 0;
        while (numberRendered < numberToAdd)
        {
            while (columnNumber < numberColumns)
            {
                Log.d(TAG, "Render sample road number ${numberRendered} at ${columnNumber}, ${rowNumber}.");
                val UniqueId = UUID.randomUUID();
                val RoadId = UUID.randomUUID();
                AddTestRouteRoadSegment(UniqueId, RoadId, columnNumber * 2, rowNumber + 7, sequence = numberRendered.toLong(), routeDirectionOfTravel = DirectionOfTravel.With, 0, 0, false, length = 1, vertical = false, text = "", reported = false, delayAddToRenderer = true, hasShape = true);
                if (numberRendered == 0)
                {
                    UUIDOfFirst = UniqueId;
                }
                if (numberRendered == numberToAdd - 1)
                {
                    UUIDOfLast = UniqueId;
                }
                if (numberRendered == numberToAdd / 2)
                {
                    UUIDOfMid = UniqueId;
                }
                numberRendered++;
                columnNumber++;
                if (numberRendered == numberToAdd)
                {
                    break;
                }
            }
            columnNumber = 0;
            rowNumber++;
        }
        val addTime = timeSource.markNow();
        AddDelayedRoadSegmentsToRenderer();
        val finishTime = timeSource.markNow();
        val calcTime = addTime - startTime;
        val renderTime = finishTime - addTime;
        Log.i(TAG, "Time taken to generate road segment data =${calcTime}.");
        Log.i(TAG, "Time taken to render road segment data =${renderTime}.");
        // Now let's modify some road segments before the data source has finished loading.
        var changes = mutableListOf<RoadSegmentPropertyBase>();
        var changeFirst = RouteRoadSegmentServicedProperty();
        changeFirst.RouteOrRoadSegmentGuid = UUIDOfFirst;
        changeFirst.Serviced = true;
        changeFirst.PassesTotal = 1;
        changeFirst.PassesCompleted = 1;
        changes.add(changeFirst);

        var changeMid = RouteRoadSegmentServicedProperty();
        changeMid.RouteOrRoadSegmentGuid = UUIDOfMid;
        changeMid.Serviced = true;
        changeMid.PassesTotal = 1;
        changeMid.PassesCompleted = 1;
        changes.add(changeMid);

        var changeLast = RoadSegmentHighlightedProperty();
        changeLast.RouteOrRoadSegmentGuid = UUIDOfLast;
        changeLast.Highlighted = true;
//        changeLast.Serviced = true;
//        changeLast.PassesTotal = 1;
//        changeLast.PassesCompleted = 1;
        changes.add(changeLast);
        theMap.mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(changes);
    }

    private fun CalculateBoundingBox(currentBoundingBox: LongLatExtent, additionalBoundingBox: LongLatExtent?) : LongLatExtent
    {
        if (additionalBoundingBox != null) {
            currentBoundingBox.SouthWest = GeographicPosition(
                Longitude = Math.min(
                    currentBoundingBox.SouthWest.Longitude,
                    additionalBoundingBox.SouthWest.Longitude
                ),
                Latitude = Math.min(
                    currentBoundingBox.SouthWest.Latitude,
                    additionalBoundingBox.SouthWest.Latitude
                )
            );
            currentBoundingBox.NorthEast = GeographicPosition(
                Longitude = Math.max(
                    currentBoundingBox.NorthEast.Longitude,
                    additionalBoundingBox.NorthEast.Longitude
                ),
                Latitude = Math.max(
                    currentBoundingBox.NorthEast.Latitude,
                    additionalBoundingBox.NorthEast.Latitude
                )
            );
        }
        return currentBoundingBox;
    }

}
