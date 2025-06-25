package isl.mapbox.thinbindings.android.maprendererkotlin.map.layers

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
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
import isl.mapbox.thinbindings.android.features.JobFeature
import isl.mapbox.thinbindings.android.features.JobStatus
import isl.mapbox.thinbindings.android.internal.Logging
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
 * Manager of displaying Jobs on the map
 */
public class JobLayer(context: Context, theMapView: MapView, theMapStyleListener: MapStyleListener)
{
    private val mapView: MapView = theMapView
    private val theContext: Context = context
    private val theMapStyleListener: MapStyleListener = theMapStyleListener;
    private val sourceId: String = "GeojsonSourceJobs";

    private val imageId_JobFlagGreyOutline: String = "JobsFlagSymbol_GreyOutline";
    private val imageId_JobFlagBlueOutline : String = "JobsFlagSymbol_BlueOutline";
    private val imageId_JobFlagBlueFilled : String = "JobsFlagSymbol_BlueFilled";
    private val imageId_JobFlagOrangeOutline : String = "JobsFlagSymbol_OrangeOutline";
    private val imageId_JobFlagOrangeFilled : String = "JobsFlagSymbol_OrangeFilled";
    private val jobFlagImageOffset = listOf<Double>(11.0, -12.0)

    private var jobFeatures: MutableList<Feature> = mutableListOf<Feature>()

    private val JobStartZoom: Double = 9.0;
    private val JobLowZoom: Double = 11.0;
    private val JobHighZoom: Double = 22.0;

    private val Zone1LowZoom: Double = 3.0;
    private val Zone2LowZoom: Double = 6.0;
    private val Zone3LowZoom: Double = 9.0;
    private val Zone4LowZoom: Double = 18.0;
    private val JobFlagLowZoom: Double = 0.75;

    private val Zone1HighZoom: Double = 9.0;
    private val Zone2HighZoom: Double = 18.0;
    private val Zone3HighZoom: Double = 27.0;
    private val Zone4HighZoom: Double = 54.0;
    private val JobFlagHighZoom: Double = 1.5;

    private val ReportedColourExpression: Expression = FeatureColours.Blue;
    private val LabelColourExpression: Expression = FeatureColours.Orange;
    private val HighlightColourExpression: Expression = FeatureColours.Yellow;

    private val lock = ReentrantLock();
    private val timeout: Long = 100;
    private val timeoutUnits = TimeUnit.MILLISECONDS;

    /**
     * Initialise the map source and layers
     */
    internal fun CreateJobsSourceLayersAndFilters(theStyle: Style)
    {
        try
        {
            Logging.D("Will attempt to create layers and filters for ${theStyle}.")
            if (CheckIfSourceExistsAlready(theStyle) == true)
            {
                return;
            }

            AddFeaturesToSource(jobFeatures);

            LoadIconImages(theStyle);


            val jobsSymbolLayerNearlyDue: SymbolLayer = SymbolLayer("symbol_jobs_OrangeOutline", sourceId);
            jobsSymbolLayerNearlyDue.iconImage(imageId_JobFlagOrangeOutline);
            jobsSymbolLayerNearlyDue.iconOffset(jobFlagImageOffset);
            jobsSymbolLayerNearlyDue.iconIgnorePlacement(true);
            jobsSymbolLayerNearlyDue.iconAllowOverlap(true);
            jobsSymbolLayerNearlyDue.iconSize(
                Expression.interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(JobStartZoom); literal(0.0) }
                        stop { literal(JobLowZoom); literal(JobFlagLowZoom) }
                        stop { literal(JobHighZoom); literal(JobFlagHighZoom) }
                    }
                )
            );
            jobsSymbolLayerNearlyDue.filter(
                Expression.all
                (
                    Expression.gt(Expression.zoom(), literal(9.0)),
                    Expression.eq(Expression.get("overdue"), literal(false)),
                    Expression.eq(Expression.get("nearlydue"), literal(true)),
                    Expression.eq(Expression.get("notoverdue"), literal(false)),
                    Expression.eq(Expression.get("status"), literal(JobStatus.Pending.toString()))
                )
            );

            val jobsSymbolLayerPending: SymbolLayer = SymbolLayer("symbol_jobs_BlueOutline", sourceId);
            jobsSymbolLayerPending.iconImage(imageId_JobFlagBlueOutline);
            jobsSymbolLayerPending.iconOffset(jobFlagImageOffset);
            jobsSymbolLayerPending.iconIgnorePlacement(true);
            jobsSymbolLayerPending.iconAllowOverlap(true);
            jobsSymbolLayerPending.iconSize(
                Expression.interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(JobStartZoom); literal(0.0) }
                        stop { literal(JobLowZoom); literal(JobFlagLowZoom) }
                        stop { literal(JobHighZoom); literal(JobFlagHighZoom) }
                    }
                )
            );
            jobsSymbolLayerPending.filter(
                Expression.all
                    (
                    Expression.gt(Expression.zoom(), literal(9.0)),
                    Expression.eq(Expression.get("overdue"), literal(false)),
                    Expression.eq(Expression.get("nearlydue"), literal(false)),
                    Expression.eq(Expression.get("notoverdue"), literal(true)),
                    Expression.eq(Expression.get("status"), literal(JobStatus.Pending.toString()))
                )
            );

            val jobsSymbolLayerOverdue: SymbolLayer = SymbolLayer("symbol_jobs_OrangeFilled", sourceId);
            jobsSymbolLayerOverdue.iconImage(imageId_JobFlagOrangeFilled);
            jobsSymbolLayerOverdue.iconOffset(jobFlagImageOffset);
            jobsSymbolLayerOverdue.iconIgnorePlacement(true);
            jobsSymbolLayerOverdue.iconAllowOverlap(true);
            jobsSymbolLayerOverdue.iconSize(
                Expression.interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(JobStartZoom); literal(0.0) }
                        stop { literal(JobLowZoom); literal(JobFlagLowZoom) }
                        stop { literal(JobHighZoom); literal(JobFlagHighZoom) }
                    }
                )
            );
            jobsSymbolLayerOverdue.filter(
                Expression.all
                (
                    Expression.gt(Expression.zoom(), literal(9.0)),
                    Expression.eq(Expression.get("overdue"), literal(true)),
                    Expression.eq(Expression.get("nearlydue"), literal(false)),
                    Expression.eq(Expression.get("notoverdue"), literal(false)),
                    Expression.eq(Expression.get("status"), literal(JobStatus.Pending.toString()))
                )
            );

            val jobsSymbolLayerEnroute: SymbolLayer = SymbolLayer("symbol_jobs_BlueFilled", sourceId);
            jobsSymbolLayerEnroute.iconImage(imageId_JobFlagBlueFilled);
                jobsSymbolLayerEnroute.iconOffset(jobFlagImageOffset);
                jobsSymbolLayerEnroute.iconIgnorePlacement(true);
                jobsSymbolLayerEnroute.iconAllowOverlap(true);
                jobsSymbolLayerEnroute.iconSize(
                    Expression.interpolate(
                        {
                            linear()
                            zoom()
                            stop { literal(JobStartZoom); literal(0.0) }
                            stop { literal(JobLowZoom); literal(JobFlagLowZoom) }
                            stop { literal(JobHighZoom); literal(JobFlagHighZoom) }
                        }
                    )
                );
            jobsSymbolLayerEnroute.filter(
                Expression.all
                (
                    Expression.gt(Expression.zoom(), literal(9.0)),
                    Expression.eq(Expression.get("status"), literal(JobStatus.Enroute.toString()))
                )
            );

            val jobsSymbolLayerInProgress: SymbolLayer = SymbolLayer("symbol_jobs_BlueFilled_Inprogress", sourceId);
            jobsSymbolLayerInProgress.iconImage(imageId_JobFlagBlueFilled);
            jobsSymbolLayerInProgress.iconOffset(jobFlagImageOffset);
            jobsSymbolLayerInProgress.iconIgnorePlacement(true);
            jobsSymbolLayerInProgress.iconAllowOverlap(true);
            jobsSymbolLayerInProgress.iconSize(
                Expression.interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(JobStartZoom); literal(0.0) }
                        stop { literal(JobLowZoom); literal(JobFlagLowZoom) }
                        stop { literal(JobHighZoom); literal(JobFlagHighZoom) }
                    }
                )
            );
            jobsSymbolLayerInProgress.filter(
                Expression.all
                (
                    Expression.gt(Expression.zoom(), literal(9.0)),
                    Expression.eq(Expression.get("status"), literal(JobStatus.InProgress.toString()))
                )
            );

            val jobsSymbolLayerFinished: SymbolLayer = SymbolLayer("symbol_jobs_GreyOutlineFinished", sourceId);
            jobsSymbolLayerFinished.iconImage(imageId_JobFlagGreyOutline);
            jobsSymbolLayerFinished.iconOffset(jobFlagImageOffset);
            jobsSymbolLayerFinished.iconIgnorePlacement(true);
            jobsSymbolLayerFinished.iconAllowOverlap(true);
            jobsSymbolLayerFinished.iconSize(
                Expression.interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(JobStartZoom); literal(0.0) }
                        stop { literal(JobLowZoom); literal(JobFlagLowZoom) }
                        stop { literal(JobHighZoom); literal(JobFlagHighZoom) }
                    }
                )
            );
            jobsSymbolLayerFinished.filter(
                Expression.all
                (
                    Expression.gt(Expression.zoom(), literal(9.0)),
                    Expression.eq(Expression.get("status"), literal(JobStatus.Finished.toString()))
                )
            );

            val reportLayer: CircleLayer = CircleLayer("jobs_reportLayer", sourceId);
            reportLayer.visibility(Visibility.VISIBLE);
            reportLayer.circleRadius(
                Expression.interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(JobStartZoom); literal(0.0) }
                        stop { literal(JobLowZoom); literal(Zone2LowZoom) }
                        stop { literal(JobHighZoom); literal(Zone2HighZoom) }
                    }
                )
            );
            reportLayer.circleStrokeColor(ReportedColourExpression);
            reportLayer.circleStrokeWidth(2.0); // Was 1.0 in mobile???
            reportLayer.circleOpacity(0.0);
            reportLayer.circleColor(ReportedColourExpression)
            reportLayer.filter(
                Expression.all
                (
                    Expression.gt(Expression.zoom(), literal(9.0)), // Not in mobile code???
                    Expression.eq(Expression.get("reported"), literal(true))
                )
            );

            val textJobLayer: SymbolLayer = SymbolLayer("text_job", sourceId);
            textJobLayer.textField(Expression.get("name"));
            // NOTE: it is essential that the font stack defined here matches one exactly in the map styles being used where we
            // will be using offline maps, otherwise the text and symbol will not render for that offline map.
            // see https://docs.mapbox.com/help/troubleshooting/mobile-offline/ and https://docs.mapbox.com/help/glossary/font-stack/
            textJobLayer.textFont(listOf<String>("DIN Offc Pro Regular", "Arial Unicode MS Regular"));
            textJobLayer.textColor(LabelColourExpression);
            textJobLayer.textOpacity(1.0);
            textJobLayer.textSize(18.0);
            textJobLayer.textOffset(listOf<Double>(1.0, 0.0));
            textJobLayer.textAnchor(TextAnchor.LEFT);
            textJobLayer.textIgnorePlacement(true);
            textJobLayer.textAllowOverlap(true);
            textJobLayer.textMaxWidth(7.0); // Not in mobile???
            // x,y textoffset to apply to centre of text. 1.0 appears similar to text height. +ve to right and down
            // Note that if text is used in combination with an image then if they're not far enough apart then you'll only see one.
            textJobLayer.filter(
                Expression.all(
                    Expression.gte(Expression.zoom(), Expression.literal(14.9))
                )
            );

            val highlightLayerJob: CircleLayer = CircleLayer("jobs_highlightLayer", sourceId);
            highlightLayerJob.visibility(Visibility.VISIBLE);
            highlightLayerJob.circleRadius(
                Expression.interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(JobStartZoom); literal(0.0) } // In mobile was RouteStartZoom???
                        stop { literal(JobLowZoom); literal(Zone4LowZoom) } // In Mobile was RouteLowZoom???
                        stop { literal(JobHighZoom); literal(Zone4HighZoom) } // In mobile was RouteHighZoom???
                    }
                )
            );
            highlightLayerJob.circleOpacity(0.5);
            highlightLayerJob.circleColor(HighlightColourExpression);
            highlightLayerJob.filter(
                Expression.all(
                    Expression.eq(Expression.get("highlight"), literal(true))
                )
            );

            //ZS: TODO Need to create more layers for the different flag statuses defined in SS-1285

            // This first layer will be the bottom layer on the map
            theStyle.addLayer(highlightLayerJob);
            theStyle.addLayer(textJobLayer);
            theStyle.addLayer(jobsSymbolLayerFinished);

            theStyle.addLayer(reportLayer);
            theStyle.addLayer(jobsSymbolLayerPending);
            theStyle.addLayer(jobsSymbolLayerNearlyDue);
            theStyle.addLayer(jobsSymbolLayerOverdue);
            theStyle.addLayer(jobsSymbolLayerEnroute);
            theStyle.addLayer(jobsSymbolLayerInProgress);

            // This last layer will be the top layer on the map

        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    private fun LoadIconImages(theStyle: Style)
    {
        val jobsFlagGreyOutline = ContextCompat.getDrawable(theContext, R.drawable.jobsflagsymbol_greyoutline)?.toBitmap(40, 40)!!
        theStyle.addImage(imageId_JobFlagGreyOutline, jobsFlagGreyOutline);

        val jobsFlagBlueOutline = ContextCompat.getDrawable(theContext, R.drawable.jobsflagsymbol_blueoutline)?.toBitmap(40, 40)!!
        theStyle.addImage(imageId_JobFlagBlueOutline, jobsFlagBlueOutline);

        val jobsFlagBlueFilled = ContextCompat.getDrawable(theContext, R.drawable.jobsflagsymbol_bluefilled)?.toBitmap(40, 40)!!
        theStyle.addImage(imageId_JobFlagBlueFilled, jobsFlagBlueFilled);

        val jobsFlagOrangeOutline = ContextCompat.getDrawable(theContext, R.drawable.jobsflagsymbol_orangeoutline)?.toBitmap(40, 40)!!
        theStyle.addImage(imageId_JobFlagOrangeOutline, jobsFlagOrangeOutline);

        val jobsFlagOrangeFilled = ContextCompat.getDrawable(theContext, R.drawable.jobsflagsymbol_orangefilled)?.toBitmap(40, 40)!!
        theStyle.addImage(imageId_JobFlagOrangeFilled, jobsFlagOrangeFilled);
    }

    /**
     * Add jobs to the map
     */
    public fun AddJobsFunc(jobs: List<JobFeature>, id: String? = null): Boolean
    {
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                Logging.D("Called for ${jobs.count()} Jobs.");
                //var _servicePointFeatures = new List<Feature>();
                var count: Int = 0;
                var featuresToAdd = mutableListOf<Feature>();
                for (job in jobs)
                {
                    var found = GetJobFeatureById(job.UniqueGuid.toString())
                    if (found == null)
                    {
                        Logging.V("Add a job at ${job.Longitude} ${job.Latitude}");
                        var point: Point = Point.fromLngLat(job.Longitude, job.Latitude); // this is 2nd slowest bit
                        var feature = Feature.fromGeometry(point, null, job.UniqueGuid.toString()); // this is slowest bit

                        feature.addStringProperty("longitude", "%.6f".format(Locale.ENGLISH, job.Longitude));
                        feature.addStringProperty("latitude", "%.6f".format(Locale.ENGLISH, job.Latitude));

                        feature.addStringProperty("status", job.Status.toString());
                        // need to check first if item already in jobFeatureCollection, jobFeatures or geoJsonSource ??? unfortunately it's so slow to check, lets just add it in again!

                        feature.addBooleanProperty("reported", job.Reported);
                        feature.addBooleanProperty("overdue", job.Overdue);
                        feature.addBooleanProperty("highlight", false);
                        feature.addStringProperty("name", job.Name);
                        feature.addBooleanProperty("nearlydue", job.Nearlydue);
                        feature.addBooleanProperty("notoverdue", job.NotOverdue);

                        count++;
                        if (count < 5)
                        {
                            //serilogger.Here().Debug("Adding job id={0}, uniqueGuid={1}", job.Identity, job.UniqueGuid);
                        }
                        jobFeatures.add(feature);
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

    /**
     * Remove some jobs from the map
     */
    public fun RemoveJobsFunc(jobs: List<JobFeature>?, id: String? = null): Boolean
    {
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                var featuresToRemove = mutableListOf<Feature>();
                if (jobs == null)
                {
                    jobFeatures = mutableListOf<Feature>();
                    Logging.D("Called with null list of jobs so removing all.");
                    UpdateAllFeaturesInSource(jobFeatures, id);
                }
                else
                {
                    Logging.D("Called for ${jobs.count()} jobs, current total (before removal)=${jobFeatures.count()}");
                    for (job in jobs)
                    {
                        var found = GetJobFeatureById(job.UniqueGuid.toString())
                        if (found != null)
                        {
                            jobFeatures.remove(found);
                            featuresToRemove.add(found);
                        }
                    }
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

    /**
     * Amend the properties of some jobs
     */
    public fun ChangeJobProperties(changesRequired: List<JobPropertyBase>, id: String? = null) : Int
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
                    var found = GetJobFeatureById(changeRequired.JobInstanceGuid.toString());
                    if (found != null)
                    {
                        var json = found.toJson();
                        jobFeatures.remove(found);
                        found = Feature.fromJson(json);
                        if (changeRequired is JobReportedProperty)
                        {
                            Logging.V("Found job for JobInstanceGuid=${changeRequired.JobInstanceGuid}, will change 'reported' property to ${changeRequired.Reported}");
                            found.removeProperty("reported");
                            found.addBooleanProperty("reported", changeRequired.Reported);
                            count++;
                        }
                        else if (changeRequired is JobHighlightedProperty)
                        {
                            Logging.V("Found job for JobInstanceGuid=${changeRequired.JobInstanceGuid}, will change 'reported' property to ${changeRequired.Highlighted}");
                            found.removeProperty("highlight");
                            found.addBooleanProperty("highlight", changeRequired.Highlighted);
                            count++;
                        }
                        else if (changeRequired is JobStatusProperty)
                        {
                            Logging.V("Found job for JobInstanceGuid=${changeRequired.JobInstanceGuid}, will change 'status' property to ${changeRequired.JobState}");
                            found.removeProperty("status");
                            found.addStringProperty("status", changeRequired.JobState.name);
                            count++;
                        }
                        jobFeatures.add(found);
                        featuresToChange.add(found);
                    }
                    else
                    {
                        countFail++;
                        if (countFail < 5)
                        {
                            Logging.W("Attempt to change property for an unknown job JobInstanceGuid=${changeRequired.JobInstanceGuid}");
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

    /**
     * Get all the Point geometries for Jobs from the features
     */
    internal fun GetLocationsForJob(): List<Point>
    {
        var locations = mutableListOf<Point>();
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                for (index in jobFeatures.indices)
                {
                    var location = jobFeatures[index].geometry() as Point;
                    locations.add(location);
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

    internal fun GetLocationForJob(jobFeature: JobFeature): Point?
    {
        var location: Point? = null;
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                var foundJobFeature = GetJobFeatureById(jobFeature.UniqueGuid.toString())
                if (foundJobFeature != null)
                {
                    location = foundJobFeature.geometry() as Point;
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

    internal fun CreateJobFeature(newLocation: GeographicPosition, feature: Feature): JobFeature?
    {
        if (feature.geometry() is Point && feature.properties() != null)
        {
            var screenLocation = mapView.mapboxMap
                .pixelForCoordinate(Point.fromLngLat(newLocation.Longitude, newLocation.Latitude));

            var properties: JsonObject = feature.properties()!!;

            var reported = false;
            var reportedProperty = properties.get("reported");
            if (reportedProperty != null && reportedProperty.isJsonNull == false)
            {
                reported = reportedProperty.asBoolean;
            }

            var overdue = false;
            var overdueProperty = properties.get("overdue");
            if (overdueProperty != null && overdueProperty.isJsonNull == false)
            {
                overdue = overdueProperty.asBoolean;
            }

            var nearlydue = false;
            var nearlydueProperty = properties.get("nearlydue");
            if (nearlydueProperty != null && nearlydueProperty.isJsonNull == false)
            {
                nearlydue = nearlydueProperty.asBoolean;
            }

            var notoverdue = false;
            var notoverdueProperty = properties.get("notoverdue");
            if (notoverdueProperty != null && notoverdueProperty.isJsonNull == false)
            {
                notoverdue = notoverdueProperty.asBoolean;
            }

            var name = "";
            var nameProperty = properties.get("name");
            if (nameProperty != null && nameProperty.isJsonNull == false)
            {
                name = nameProperty.asString;
            }

            var tappedMapFeature = JobFeature();
            tappedMapFeature.UniqueGuid = UUID.fromString(feature.id());
            tappedMapFeature.Overdue = overdue;
            tappedMapFeature.Nearlydue = nearlydue;
            tappedMapFeature.NotOverdue = notoverdue;
            tappedMapFeature.Name = name;
            tappedMapFeature.Reported = reported;
            if (feature.id() != null)
            {
                tappedMapFeature.UniqueGuid =  UUID.fromString(feature.id());
            }

            var statusProperty = properties.get("status");
            var status = JobStatus.Unknown;
            if (statusProperty != null)
            {
                status = JobStatus.valueOf(statusProperty.asString);
            }
            tappedMapFeature.Status = status;

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
            Logging.D("JobFeature with UniqueGuid=${tappedMapFeature.UniqueGuid}, Name=${tappedMapFeature.Name}, Reported=${tappedMapFeature.Reported}, Status=${tappedMapFeature.Status.toString()}, NearlyDue=${tappedMapFeature.Nearlydue}, Overdue=${tappedMapFeature.Overdue}, Identity.${tappedMapFeature.Identity}");

            return tappedMapFeature;
        }
        else
        {
            return null;
        }
    }

    private fun GetJobFeatureById(subjectId: String): Feature?
    {
        if(jobFeatures.isNullOrEmpty()) return null;
        return jobFeatures.firstOrNull { x -> x.id().equals(subjectId) };
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
                "Will update ${features.count()} jobFeatures, number in featureCollection=${
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

                    Logging.V("Will update ${features.count()} jobFeatures.");
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

                    Logging.V("Will add ${features.count()} jobFeatures.");
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

                    Logging.V("Will remove ${features.count()} jobFeatures.");
                }
            //});
    }

}

public class JobHighlightedProperty : JobPropertyBase()
{
    public var Highlighted: Boolean = false;
}

public class JobReportedProperty : JobPropertyBase()
{
    public var Reported: Boolean = false;
}

public class JobStatusProperty : JobPropertyBase()
{
    public var JobState: JobStatus = JobStatus.Unknown;
}

public abstract class JobPropertyBase : MapFeaturePropertyBase()
{
    public lateinit var JobInstanceGuid: UUID
}
