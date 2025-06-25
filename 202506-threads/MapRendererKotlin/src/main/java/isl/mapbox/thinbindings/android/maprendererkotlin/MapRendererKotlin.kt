package isl.mapbox.thinbindings.android.maprendererkotlin

import android.content.Context
import android.widget.FrameLayout
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ConstrainMode
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.GlyphsRasterizationOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.TileStoreUsageMode
import com.mapbox.maps.applyDefaultParams
import com.mapbox.maps.mapsOptions
import com.mapbox.maps.plugin.Plugin
import com.mapbox.maps.plugin.scalebar.scalebar
import isl.mapbox.thinbindings.android.R
import isl.mapbox.thinbindings.android.internal.IMapGestureInternalCallbacks
import isl.mapbox.thinbindings.android.internal.IMapStyleInternalCallbacks
import isl.mapbox.thinbindings.android.internal.Logging
import isl.mapbox.thinbindings.android.internal.MapClickListener
import isl.mapbox.thinbindings.android.maprendererkotlin.map.LocationMarker
import isl.mapbox.thinbindings.android.maprendererkotlin.map.MapCamera
import isl.mapbox.thinbindings.android.internal.MapGestureListener
import isl.mapbox.thinbindings.android.internal.MapboxLogListener
import isl.mapbox.thinbindings.android.maprendererkotlin.IMapCallback
import isl.mapbox.thinbindings.android.maprendererkotlin.map.FeatureSelection
import isl.mapbox.thinbindings.android.maprendererkotlin.map.IFeatureSelection
import isl.mapbox.thinbindings.android.maprendererkotlin.map.ILocationMarker
import isl.mapbox.thinbindings.android.maprendererkotlin.map.IMapCamera
import isl.mapbox.thinbindings.android.maprendererkotlin.map.IMapStyle
import isl.mapbox.thinbindings.android.maprendererkotlin.map.MapStyle
import isl.mapbox.thinbindings.android.maprendererkotlin.misc.ConditionalCompile

const val defaultMapKeyLongitude: Double = -1.5385025; // This is Leeds office
const val defaultMapKeyLatitude: Double = 53.8294068; // This is Leeds office

public enum class LogLevel {
    Verbose,
    Debug,
    Info,
    Warning,
    Error
}

/***
 * Displayable component wrapper around Mapbox
 */
class MapRendererKotlin(context: Context, useTextureView: Boolean) : FrameLayout(context), IMapRendererKotlin, IMapGestureInternalCallbacks
{
    public override lateinit var mapStyle: IMapStyle private set
    public override lateinit var locationMarker: ILocationMarker private set
    public override lateinit var mapCamera: IMapCamera private set

    private lateinit var mapGestureListener: MapGestureListener;

    private lateinit var mapClickListener: MapClickListener;
    private lateinit var mapboxLogListener: MapboxLogListener;

    private lateinit var mapboxMapView: MapView
    private val theContext: Context = context
    private val useTextureView = useTextureView;

    private lateinit var cSharpGestureHandler: IGestureCallback

    public override lateinit var featureSelection: IFeatureSelection private set;

    init {
        Initialise()
        this.addView(this.mapboxMapView)
    }

    /**
     * Initialise Mapbox with access token and defaults.
     */
    private fun Initialise() {
        Logging.D("Initialise called.")
        mapboxLogListener = MapboxLogListener();
        // Set the application-scoped ResourceOptionsManager with customised token and tile store usage mode
        // so that all MapViews created with default config will apply these settings.
        MapboxOptions.accessToken = context.getString(R.string.mapbox_access_token);
        MapboxOptions.mapsOptions.tileStoreUsageMode = TileStoreUsageMode.DISABLED;

        // set map options
        val mapOptions = MapOptions.Builder().applyDefaultParams(context)
            .constrainMode(ConstrainMode.HEIGHT_ONLY)
            .glyphsRasterizationOptions(
                GlyphsRasterizationOptions.Builder()
                    .rasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
                    // Font family is required when the GlyphsRasterizationMode is set to IDEOGRAPHS_RASTERIZED_LOCALLY or ALL_GLYPHS_RASTERIZED_LOCALLY
                    .fontFamily("sans-serif")
                    .build()
            )
            .build()

        // plugins that will be loaded as part of MapView initialisation (Note Camera must be listed before Gestures!
        val plugins : List<Plugin> = listOf( Plugin.Mapbox(Plugin.MAPBOX_ATTRIBUTION_PLUGIN_ID), Plugin.Mapbox(
            Plugin.MAPBOX_LOGO_PLUGIN_ID
        ), Plugin.Mapbox(
            Plugin.MAPBOX_CAMERA_PLUGIN_ID
        ), Plugin.Mapbox(Plugin.MAPBOX_GESTURES_PLUGIN_ID), Plugin.Mapbox(
            Plugin.MAPBOX_COMPASS_PLUGIN_ID
        ), Plugin.Mapbox(Plugin.MAPBOX_SCALEBAR_PLUGIN_ID)
            /*, com.mapbox.maps.plugin.Plugin.Mapbox(MAPBOX_LIFECYCLE_PLUGIN_ID)*/
        )

        // set initial camera position
        val initialCameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(defaultMapKeyLongitude, defaultMapKeyLatitude))
            .zoom(15.0)
            .bearing(0.0)
            .build()

        val mapInitOptions =
            MapInitOptions(
                context,
                mapOptions,
                plugins,
                initialCameraOptions,
                useTextureView
            )

        // create view programmatically
        mapboxMapView = MapView(context, mapInitOptions)

        mapboxMapView.setMaximumFps(15);

        mapStyle = MapStyle(context, mapboxMapView)
        //mapStyle.LoadStyle(MapStyleSmartSuiteLight)
        locationMarker = LocationMarker(mapStyle.symbolsLayer);
        mapCamera = MapCamera(mapboxMapView, locationMarker as LocationMarker, mapStyle as MapStyle);
        mapGestureListener = MapGestureListener(mapboxMapView, mapCamera as MapCamera);
        mapClickListener = MapClickListener(mapboxMapView);
        featureSelection = FeatureSelection(mapboxMapView, mapClickListener, mapCamera as MapCamera, locationMarker as LocationMarker, mapStyle as MapStyle);
    }

    /**
     * Call Mapbox onStart, sets units to Metric
     */
    override fun StartInstance()
    {
        try
        {
            Logging.D("StartInstance called.")
            mapboxMapView.onStart()
            metricUnits = true;
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    /**
     * Stop listening to callbacks and call Mapbox.onStop.
     */
    override fun DisposeOfInstance()
    {
        try
        {
            Logging.D("DisposeOfInstance called.")
            mapGestureListener.StopListeningForGestures();
            mapClickListener.StopListeningForClicks();
            mapboxMapView.onStop()
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    /**
     * Pass on LowMemory call to Mapbox
     */
    override fun OnLowMemory()
    {
        mapboxMapView.onLowMemory();
    }

    /**
     * Register for callbacks to Logging events
     * @param loggingHandler Instance of class implementing ILoggingCallback
     */
    override fun RegisterForLoggingCallback(loggingHandler: ILoggingCallback)
    {
        try
        {
            Logging.RegisterCallback(loggingHandler)
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    /**
     * Register for Gestures
     * @param gestureHandler Instance of object implementing IGestureCallbacks
     */
    override fun RegisterForGestureCallback(gestureHandler: IGestureCallback)
    {
        try
        {
            cSharpGestureHandler = gestureHandler;
            mapGestureListener.RegisterForCallback(this);
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    /**
     * Register for Map Camera callbacks
     * @param mapCameraHandler Instance of object implementing IMapCameraCallbacks
     */
    override fun RegisterForMapCameraCallback(mapCameraHandler: IMapCameraCallback)
    {
        try
        {
            (mapCamera as MapCamera).RegisterForCallback(mapCameraHandler);
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    /**
     * Register for Feature Selection callback
     * @param featureSelectionHandler Instance of object implementing IFeatureSelectionCallback
     */
    override fun RegisterForFeatureSelectionCallback(featureSelectionHandler: IFeatureSelectionCallback)
    {
        try
        {
            (featureSelection as FeatureSelection).RegisterForCallback(featureSelectionHandler);
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
            if (ConditionalCompile.throwAfterLoggingException) throw ex;
        }
    }

    /**
     * Register for Map callbacks
     * @param mapHandler Instance of object implementing IMapCallbacks
     */
    override fun RegisterForMapCallback(mapHandler: IMapCallback)
    {
        mapGestureListener.RegisterForMapCallback(mapHandler);
        (mapStyle as MapStyle).RegisterForMapCallback(mapHandler);
    }

    /**
     * Is Scale Bar set to Metric Units
     */
    override var metricUnits: Boolean
        get() = mapboxMapView.scalebar.isMetricUnits;
        set(value)
        {
            mapboxMapView.scalebar.isMetricUnits = value;
        }

    /**
     * Pass on a map gesture to cSharp Gesture Handler
     *
     * @param moveDetected
     * @param rotateDetected
     * @param shoveDetected
     * @param flingDetected
     * @param scaleDetected
     */
    override fun MapGestureInternalCallback(moveDetected: Boolean, rotateDetected: Boolean, shoveDetected: Boolean, flingDetected: Boolean, scaleDetected: Boolean)
    {
        Logging.V("");
        if (::cSharpGestureHandler.isInitialized)
        {
            cSharpGestureHandler.GestureFromMapRendererKotlin(moveDetected, rotateDetected, shoveDetected, flingDetected, scaleDetected);
        }

    }

}