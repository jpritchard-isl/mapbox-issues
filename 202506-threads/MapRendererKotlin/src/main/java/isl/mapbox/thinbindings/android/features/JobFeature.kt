package isl.mapbox.thinbindings.android.features

/**
 * A Job instance feature
 */
public class JobFeature : AbstractPointMapFeature()
{
    /**
     * This inicates the job status for this feature.
     */
    public var Status: JobStatus = JobStatus.Unknown;

    /**
     * This indicates if the job instance is overdue
     */
    public var Overdue: Boolean = false;

    /**
     * This indicates if the job instance is not overdue
     */
    public var NotOverdue: Boolean = false;

    /**
     * This indicates if the job instance is nearly
     */
    public var Nearlydue: Boolean = false;

    /**
     * This indicates that a report has been raised for this feature.
     */
    public var Reported: Boolean = false;

    /**
     * The Name of the job Instance
     */
    public var Name: String = "";
}
