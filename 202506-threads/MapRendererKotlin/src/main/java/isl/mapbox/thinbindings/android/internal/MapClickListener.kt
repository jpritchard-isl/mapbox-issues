package isl.mapbox.thinbindings.android.internal

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapLongClickListener
import com.mapbox.maps.plugin.gestures.removeOnMapClickListener
import com.mapbox.maps.plugin.gestures.removeOnMapLongClickListener
import isl.mapbox.thinbindings.android.maprendererkotlin.map.FeatureSelection
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.MapPosition
import isl.mapbox.thinbindings.android.positions.ScreenPosition

public class MapClickListener(theMapView: MapView) : OnMapClickListener, OnMapLongClickListener
{
    private val mapView: MapView = theMapView
    private lateinit var featureSelection: FeatureSelection;

    init
    {
        mapView.mapboxMap.addOnMapClickListener(this);
        mapView.mapboxMap.addOnMapLongClickListener(this);
    }

    public fun StopListeningForClicks()
    {
        mapView.mapboxMap.removeOnMapClickListener(this);
        mapView.mapboxMap.removeOnMapLongClickListener(this);
    }

    override fun onMapClick(point: Point): Boolean
    {
        try
        {
            Logging.D("Click at ${point.longitude()}, ${point.latitude()}.");
            var screenLocation = mapView.mapboxMap.pixelForCoordinate(point);
            var clickPosition = MapPosition(GeographicPosition(point.longitude(), point.latitude()), ScreenPosition(screenLocation.x, screenLocation.y, mapView.height, mapView.width));

            if (::featureSelection.isInitialized)
            {
                featureSelection.MapClickInternalCallback(clickPosition);
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
        return true;
    }

    override fun onMapLongClick(point: Point): Boolean
    {
        try
        {
            Logging.D("Long Click at ${point.longitude()}, ${point.latitude()}.");
            var screenLocation = mapView.mapboxMap.pixelForCoordinate(point);
            var longClickPosition = MapPosition(GeographicPosition(point.longitude(), point.latitude()), ScreenPosition(screenLocation.x, screenLocation.y, mapView.height, mapView.width));

            if (::featureSelection.isInitialized)
            {
                featureSelection.MapLongClickInternalCallback(longClickPosition);
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
        return true;
    }

    // This method to be used by MapRendererKotlin to register for callbacks using the IMapGestureInternalCallbacks method.
    public fun RegisterForCallback(theFeatureSelection: FeatureSelection)
    {
        Logging.V("Register for callback.")
        featureSelection = theFeatureSelection;

    }

}