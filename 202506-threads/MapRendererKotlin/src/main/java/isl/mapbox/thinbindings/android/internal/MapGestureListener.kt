package isl.mapbox.thinbindings.android.internal

import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.android.gestures.ShoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.maps.MapIdle
import com.mapbox.maps.MapIdleCallback
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.OnFlingListener
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnRotateListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.OnShoveListener
import com.mapbox.maps.plugin.gestures.addOnFlingListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.gestures.addOnRotateListener
import com.mapbox.maps.plugin.gestures.addOnScaleListener
import com.mapbox.maps.plugin.gestures.addOnShoveListener
import com.mapbox.maps.plugin.gestures.removeOnFlingListener
import com.mapbox.maps.plugin.gestures.removeOnMoveListener
import com.mapbox.maps.plugin.gestures.removeOnRotateListener
import com.mapbox.maps.plugin.gestures.removeOnScaleListener
import com.mapbox.maps.plugin.gestures.removeOnShoveListener
import isl.mapbox.thinbindings.android.maprendererkotlin.MapRendererKotlin
import isl.mapbox.thinbindings.android.maprendererkotlin.IMapCallback
import isl.mapbox.thinbindings.android.maprendererkotlin.map.MapCamera
import isl.mapbox.thinbindings.android.maprendererkotlin.map.MapStyle
import java.lang.Math.sqrt

public class MapGestureListener(theMapView: MapView, mapCamera: MapCamera) : MapIdleCallback, OnRotateListener, OnMoveListener, OnShoveListener, OnScaleListener
{
    private val mapView: MapView = theMapView
    private lateinit var mapRenderer: MapRendererKotlin;
    private val mapCamera: MapCamera = mapCamera;

    private val mapFlingListener: OnFlingListener;

    private var distXMoved: Float = 0.0F;
    private var distYMoved: Float = 0.0F;

    private var moveDetected: Boolean = false;
    private var rotateDetected: Boolean = false;
    private var shoveDetected: Boolean = false;
    private var flingDetected: Boolean = false;
    private var scaleDetected: Boolean = false;

    private var registeredMapCallbackHandlers = mutableListOf<IMapCallback>();

    init
    {
        mapView.mapboxMap.subscribeMapIdle(this);

        mapFlingListener = OnFlingListener { OnFling() };
        mapView.mapboxMap.addOnFlingListener(mapFlingListener);

        mapView.mapboxMap.addOnRotateListener(this);

        mapView.mapboxMap.addOnMoveListener(this);

        mapView.mapboxMap.addOnShoveListener(this);

        mapView.mapboxMap.addOnScaleListener(this);
    }

    public fun StopListeningForGestures()
    {
        mapView.mapboxMap.removeOnFlingListener(mapFlingListener);
        mapView.mapboxMap.removeOnRotateListener(this);
        mapView.mapboxMap.removeOnMoveListener(this);
        mapView.mapboxMap.removeOnShoveListener(this);
        mapView.mapboxMap.removeOnScaleListener(this);
    }

    // This method to be used by MapRendererKotlin to register for callbacks using the IMapGestureInternalCallbacks method.
    public fun RegisterForCallback(mapRendererKotlin: MapRendererKotlin)
    {
        Logging.V("Register for callback.")
        mapRenderer = mapRendererKotlin;

    }

    private fun OnFling()
    {
        try
        {
            flingDetected = true;
            Logging.V("OnFling callback");
            CallBackToMapRenderer();
            flingDetected = false; // ???
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun run(mapIdle: MapIdle) {
        // Note- was hoping to use OnMapIdle to collate all gestures occuring at same time, however when we're updating the map every second due to location marker updates then we don't get OnMapIdle as unlike in Mapbox 9.7 this is not signalled.
        try
        {
            Logging.V("OnMapIdle callback.")
            for (registeredMapCallbackHandler in registeredMapCallbackHandlers)
            {
                registeredMapCallbackHandler.MapIdleFromMapRendererKotlin();
            }

        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun onRotate(detector: RotateGestureDetector)
    {
        try
        {
            Logging.V("onRotate deltaSinceStart=${detector.deltaSinceStart} gestureDuration=${detector.gestureDuration} deltaSinceLast=${detector.deltaSinceLast}.");
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun onRotateBegin(detector: RotateGestureDetector)
    {
        try
        {
            Logging.V("onRotateBegin deltaSinceStart=${detector.deltaSinceStart} gestureDuration=${detector.gestureDuration} deltaSinceLast=${detector.deltaSinceLast}.");
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun onRotateEnd(detector: RotateGestureDetector)
    {
        try
        {
            rotateDetected = true;
            Logging.V("onRotateEnd deltaSinceStart=${detector.deltaSinceStart} gestureDuration=${detector.gestureDuration} deltaSinceLast=${detector.deltaSinceLast}.");
            CallBackToMapRenderer();
            rotateDetected = false;
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun onMove(detector: MoveGestureDetector): Boolean
    {
        try
        {
            distXMoved += detector.lastDistanceX;
            distYMoved += detector.lastDistanceY;
            Logging.V("onMove lastDistX=${detector.lastDistanceX} lastDistY=${detector.lastDistanceY} gestureDuration${detector.gestureDuration}");
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
        return false;
    }

    override fun onMoveBegin(detector: MoveGestureDetector)
    {
        try
        {
            distXMoved = 0.0F;
            distYMoved = 0.0F;
            Logging.V("onMoveBegin lastDistX=${detector.lastDistanceX} lastDistY=${detector.lastDistanceY} gestureDuration${detector.gestureDuration}");
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun onMoveEnd(detector: MoveGestureDetector)
    {
        try
        {
            var xMoved: Float = 0.0F;
            if (mapView.width > 0)
            {
                xMoved = distXMoved / mapView.width;
            }
            var yMoved: Float = 0.0F;
            if (mapView.height > 0)
            {
                yMoved = distYMoved / mapView.height;
            }
            val distanceMoved = sqrt((xMoved * xMoved + yMoved * yMoved).toDouble());
            if ( distanceMoved > mapCamera.movementDetectinThreshold)
            {
                moveDetected = true;
                CallBackToMapRenderer();
                moveDetected = false;
            }
            Logging.V("onMoveEnd distanceMoved=${distanceMoved} lastDistX=${detector.lastDistanceX} lastDistY=${detector.lastDistanceY} gestureDuration=${detector.gestureDuration}");

        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun onShove(detector: ShoveGestureDetector)
    {
        try
        {
            Logging.V("onShove deltaPixelsSinceStart=${detector.deltaPixelsSinceStart} gestureDuration=${detector.gestureDuration} deltaPixelSinceLast=${detector.deltaPixelSinceLast}");
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun onShoveBegin(detector: ShoveGestureDetector)
    {
        try
        {
            Logging.V("onShoveBegin deltaPixelsSinceStart=${detector.deltaPixelsSinceStart} gestureDuration=${detector.gestureDuration} deltaPixelSinceLast=${detector.deltaPixelSinceLast}");
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun onShoveEnd(detector: ShoveGestureDetector)
    {
        try
        {
            shoveDetected = true;
            Logging.V("onShoveEnd deltaPixelsSinceStart=${detector.deltaPixelsSinceStart} gestureDuration=${detector.gestureDuration} deltaPixelSinceLast=${detector.deltaPixelSinceLast}");
            CallBackToMapRenderer();
            shoveDetected = false;
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun onScale(detector: StandardScaleGestureDetector) {
        try
        {
            Logging.V("onScale isScalingOut=${detector.isScalingOut} scaleFactor=${detector.scaleFactor} gestureDuration=${detector.gestureDuration}");
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun onScaleBegin(detector: StandardScaleGestureDetector) {
        try
        {
            Logging.V("onScale isScalingOut=${detector.isScalingOut} scaleFactor=${detector.scaleFactor} gestureDuration=${detector.gestureDuration}");
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun onScaleEnd(detector: StandardScaleGestureDetector) {
        try
        {
            scaleDetected = true;
            Logging.V("onScale isScalingOut=${detector.isScalingOut} scaleFactor=${detector.scaleFactor} gestureDuration=${detector.gestureDuration}");
            CallBackToMapRenderer();
            scaleDetected = false;
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    private fun CallBackToMapRenderer()
    {
        try
        {
            mapCamera.MapGestureInternalCallback(moveDetected, rotateDetected, shoveDetected, flingDetected, scaleDetected);
            Logging.D("Gesture detected.")
            if (::mapRenderer.isInitialized)
            {
                mapRenderer.MapGestureInternalCallback(moveDetected, rotateDetected, shoveDetected, flingDetected, scaleDetected);
            }
            else
            {
                Logging.D("No maprenderer registered for callback.")
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    internal fun RegisterForMapCallback(mapHandler: IMapCallback)
    {
        registeredMapCallbackHandlers.add(mapHandler);
    }

}