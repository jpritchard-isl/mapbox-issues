package isl.mapbox.thinbindings.android.testapp

import isl.mapbox.thinbindings.android.features.JobFeature
import isl.mapbox.thinbindings.android.features.JobStatus
import isl.mapbox.thinbindings.android.features.ServicePointAndTradeSiteFeature
import isl.mapbox.thinbindings.android.maprendererkotlin.MapRendererKotlin
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.LongLatExtent
import java.util.UUID

public class JobTest(theMap: MapRendererKotlin, testExtent: LongLatExtent)
{
    private val TAG = "TestApp";
    private val theMap: MapRendererKotlin = theMap
    private var testExtent = testExtent;

    public fun AddTestJob(linePositionXIndex: Int, linePositionYIndex: Int, status: JobStatus, reported: Boolean = false, overdue: Boolean = false, nearlydue: Boolean = false, notoverdue: Boolean = false, isTrade: Boolean = false, name: String = "", UniqueId: UUID): JobFeature
    {
        var jobFeatures = mutableListOf<JobFeature>();

        var jobFeature: JobFeature = JobFeature();
        jobFeature.UniqueGuid = UniqueId;
        jobFeature.Overdue = overdue;
        jobFeature.Nearlydue = nearlydue;
        jobFeature.NotOverdue = notoverdue;
        jobFeature.Reported = reported;
        jobFeature.IsTrade = isTrade;
        jobFeature.Status = status;
        jobFeature.Name = name;

        jobFeature = jobFeature.SetGeometry(linePositionXIndex, linePositionYIndex) as JobFeature;
        jobFeatures.add(jobFeature);

        testExtent = testExtent.CalculateBoundingBox(GeographicPosition(jobFeature.Longitude, jobFeature.Latitude));

        theMap.mapStyle.jobLayer.AddJobsFunc(jobFeatures);

        return jobFeature;
    }

}