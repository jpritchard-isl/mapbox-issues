package isl.mapbox.thinbindings.android.testapp

import android.util.Log
import isl.mapbox.thinbindings.android.features.ServicePointAndTradeSiteFeature
import isl.mapbox.thinbindings.android.maprendererkotlin.MapRendererKotlin
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.LongLatExtent
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

public class ServicePointAndTradeSiteTest(theMap: MapRendererKotlin, testExtent: LongLatExtent)
{
    private val TAG = "TestApp";
    private val theMap: MapRendererKotlin = theMap
    private var testExtent = testExtent;
    var servicePointFeatures = mutableListOf<ServicePointAndTradeSiteFeature>();

    public fun AddTestServicePointAndTradeSite(linePositionXIndex: Int, linePositionYIndex: Int, serviced: Boolean, text: String = "", flat: String = "", reported: Boolean = false, comments: Boolean = false, hasAction: Boolean = false, actioned: Boolean = false, stopped: Boolean = false, isTrade: Boolean = false, UniqueId: UUID, delayAddToRenderer: Boolean = false) : ServicePointAndTradeSiteFeature
    {
        if (delayAddToRenderer == false)
        {
            servicePointFeatures.clear();
        }

        var servicePointFeature: ServicePointAndTradeSiteFeature = ServicePointAndTradeSiteFeature()
        servicePointFeature.UniqueGuid = UniqueId;
        servicePointFeature.NameOrNumber = text;
        servicePointFeature.HasAction = hasAction;
        servicePointFeature.Actioned = actioned;
        servicePointFeature.Comments = comments;
        servicePointFeature.Reported = reported;
        servicePointFeature.Serviced = serviced;
        servicePointFeature.IsStopped = stopped;
        servicePointFeature.IsTrade = isTrade;
        servicePointFeature.Flat = flat;

        servicePointFeature = servicePointFeature.SetGeometry(linePositionXIndex, linePositionYIndex) as ServicePointAndTradeSiteFeature;
        servicePointFeatures.add(servicePointFeature);

        testExtent = testExtent.CalculateBoundingBox(GeographicPosition(servicePointFeature.Longitude, servicePointFeature.Latitude));

        if (delayAddToRenderer == false)
        {
            AddDelayedServicePointsToRenderer();
        }

        return servicePointFeature;
    }

    private fun AddDelayedServicePointsToRenderer()
    {
        theMap.mapStyle.servicePointAndTradeSiteLayer.AddServicePointsAndTradeSites(servicePointFeatures);
        servicePointFeatures.clear();
    }

    @OptIn(ExperimentalTime::class)
    public fun AddMultipleServicePoints(numberToAdd: Int)
    {
        val timeSource = TimeSource.Monotonic;
        val startTime = timeSource.markNow();

        var numberColumns: Int = Math.sqrt(numberToAdd.toDouble()).toInt();
        var numberRendered: Int = 0;
        var columnNumber: Int = 0;
        var rowNumber: Int = 0;
        while (numberRendered < numberToAdd)
        {
            while (columnNumber < numberColumns)
            {
                //Log.d(TAG, "Render sample road number ${numberRendered} at ${columnNumber}, ${rowNumber}.");
                AddTestServicePointAndTradeSite(- (columnNumber * 2) - 2, rowNumber + 7, false, text = "", flat = "", false, false, false, false, false, false, UUID.randomUUID(), true);
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
        AddDelayedServicePointsToRenderer();
        val finishTime = timeSource.markNow();
        val calcTime = addTime - startTime;
        val renderTime = finishTime - addTime;
        Log.i(TAG, "Time taken to generate service point data =${calcTime}.");
        Log.i(TAG, "Time taken to render service point data =${renderTime}.");
    }

}
