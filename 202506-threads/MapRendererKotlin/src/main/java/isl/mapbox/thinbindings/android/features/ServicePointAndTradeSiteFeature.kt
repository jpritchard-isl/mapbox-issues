package isl.mapbox.thinbindings.android.features

/**
 * Properties of a service or trade site feature
 */
public class ServicePointAndTradeSiteFeature : AbstractPointMapFeature()
{
    /**
     * This inicates the serviced status for this feature.
     */
    public var Serviced:Boolean = false;

    /**
     * This indicates if a comment is present due to either the RouteServicePoint.ServiceComment or servicePoint.Note having a value.
     */
    public var Comments: Boolean = false;

    /**
     * The ServiceComment obtained from the RouteServicePoint.
     */
    public var ServiceComment: String = "";

    /**
     * The Note obtained from the ServicePoint.
     */
    public var Note: String = "";

    /**
     * This indicates that a report has been raised for this feature.
     */
    public var Reported: Boolean = false;

    /**
     * This indicates that an action can be performed on this feature due to RouteServicePoint.Action having a value.
     */
    public var HasAction: Boolean = false;

    /**
     * This indicates that the action has been performed for this feature.
     */
    public var Actioned: Boolean = false;

    /**
     * The Action obtained from the RouteServicePoint.
     */
    public var Action: String = "";

    /**
     * Where the service point is being used for trade and the site has a stop against it (usually for non-payment).
     */
    public var IsStopped: Boolean = false;

    /**
     * A shortened form of the address (will initially just contain the Guid)
     */
    public var Address: String = "";

    /**
     * The name or number of the property.
     */
    public var NameOrNumber: String = "";

    /**
     * The sub building unit e.g. flat or apartment
     */
    public var Flat: String = "";
}
