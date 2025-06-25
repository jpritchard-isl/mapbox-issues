package isl.mapbox.thinbindings.android.internal

import android.content.res.Resources
import com.mapbox.maps.MapLoaded
import com.mapbox.maps.MapLoadedCallback
import com.mapbox.maps.MapLoadingError
import com.mapbox.maps.MapLoadingErrorCallback
import com.mapbox.maps.MapLoadingErrorType
import com.mapbox.maps.MapView
import com.mapbox.maps.SourceDataLoaded
import com.mapbox.maps.SourceDataLoadedCallback
import com.mapbox.maps.SourceDataLoadedType
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.toSourceDataLoadedEventData
import com.mapbox.maps.plugin.scalebar.scalebar
import isl.mapbox.thinbindings.android.maprendererkotlin.IMapCallback

// This class is a helper for MapStyle, as it supports the Style.OnStyleLoaded interface which we don't want to expose to the ThinBindings.
class MapStyleListener(mapboxMapView: MapView): Style.OnStyleLoaded, MapLoadingErrorCallback,
    SourceDataLoadedCallback, MapLoadedCallback {
    private var theMapView: MapView = mapboxMapView
    private var registeredStyleInternalCallbackHandlers = mutableListOf<IMapStyleInternalCallbacks>();
    private var registeredMapCallbackHandlers = mutableListOf<IMapCallback>();


    init
    {
        theMapView.mapboxMap.subscribeMapLoadingError(this);
        theMapView.mapboxMap.subscribeMapLoaded(this);
        theMapView.mapboxMap.subscribeSourceDataLoaded(this);
    }

    /**
     * This method to be used internally to register for callbacks using the IMapStyleCallbacks method.
     * Note that this supports multiple listeners.
     */
    internal fun RegisterStyleInternalForCallback(handler: IMapStyleInternalCallbacks)
    {
        Logging.V("Register for callback.")
        registeredStyleInternalCallbackHandlers.add(handler);
    }

    // This method to be used by MapStyle to request that the map loads another style.
    public fun LoadStyle(theStyle : String)
    {
        var styleLoaded = theMapView.mapboxMap.styleURI;
        if (styleLoaded != theStyle)
        {
            Logging.D("Load style '${theStyle}'.")
            theMapView.mapboxMap.loadStyle(theStyle, this);
        }
    }

    // This method will be called back by the map when a style has been loaded.
    override fun onStyleLoaded(style: Style)
    {
        try
        {
            Logging.D("Style '${style.styleURI}' is loaded.")
            for (registeredStyleInternalCallbackHandler in registeredStyleInternalCallbackHandlers)
            {
                registeredStyleInternalCallbackHandler.MapStyleInternalCallback(style);
            }
            for (registeredMapCallbackHandler in registeredMapCallbackHandlers)
            {
                registeredMapCallbackHandler.StyleLoadedFromMapRendererKotlin();
            }
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    override fun run(mapLoadingError: MapLoadingError)
    {
        if (mapLoadingError.type == MapLoadingErrorType.TILE)
        {
            // Have downgraded this to Verbose, as repeated errors of this type can be logged when no network connection.
            Logging.V("Error loading map, type=${mapLoadingError.type}, message=${mapLoadingError.message}");
        }
        else
        {
            Logging.D("Error loading map, type=${mapLoadingError.type}, message=${mapLoadingError.message}");
        }

    }

    override fun run(sourceDataLoaded: SourceDataLoaded) {
        if (sourceDataLoaded.loaded == true)
        {
            // We only seem to receive TILE type messages.
            if (sourceDataLoaded.type == SourceDataLoadedType.TILE)
            {
                //Logging.D("Source data loaded type=${sourceDataLoaded.type}, tileId=${sourceDataLoaded.tileId}, begin=${sourceDataLoaded.timeInterval.begin}, end=${sourceDataLoaded.timeInterval.end}, loaded=${sourceDataLoaded.loaded}.");
            }
            else
            {
                //Logging.D("Source data loaded type=${sourceDataLoaded.type}, dataId=${sourceDataLoaded.dataId}, begin=${sourceDataLoaded.timeInterval.begin}, end=${sourceDataLoaded.timeInterval.end}, loaded=${sourceDataLoaded.loaded}.");
            }
        }
        if (sourceDataLoaded.dataId != null && dataSourceUpdateIds.isNullOrEmpty() == false)
        {
                        if (dataSourceUpdateIds.contains(sourceDataLoaded.dataId))
                        {
                            Logging.V("Found end of source data load ${sourceDataLoaded.dataId}");
                            dataSourceUpdateIds.remove(sourceDataLoaded.dataId);
                        }
        }
        else
        {
            // We are not always receiving eventData.dataId, but will maybe want to use this in the future,
            // So for now just stop the list we're waiting for from getting too long...
            if (dataSourceUpdateIds.isNullOrEmpty() == false)
            {
                while (dataSourceUpdateIds.count() > 100)
                {
                    dataSourceUpdateIds.removeAt(0);
                }
            }
        }
    }

    private var dataSourceUpdateIds = mutableListOf<String>();
    internal fun LogWhenDataSourceUpdated(id: String)
    {
        dataSourceUpdateIds.add(id);
    }

    override fun run(mapLoaded: MapLoaded) {
        Logging.D("MapLoaded begin=${mapLoaded.timeInterval.begin}, end=${mapLoaded.timeInterval.end}");
        for (registeredMapCallbackHandler in registeredMapCallbackHandlers)
        {
            registeredMapCallbackHandler.MapLoadedFromMapRendererKotlin();

            val density = Resources.getSystem().displayMetrics.density;
            val scalebar = theMapView.scalebar;
            scalebar.textSize = 20.0f * density;
        }
    }

    fun RegisterForMapCallback(mapHandler: IMapCallback)
    {
        Logging.V("Register for callback.")
        registeredMapCallbackHandlers.add(mapHandler);
    }

}