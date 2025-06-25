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
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
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
import isl.mapbox.thinbindings.android.features.AdhocFeature
import isl.mapbox.thinbindings.android.features.PointOfInterestFeature
import isl.mapbox.thinbindings.android.features.SymbolMapFeature
import isl.mapbox.thinbindings.android.features.SymbolType
import isl.mapbox.thinbindings.android.internal.Logging
import isl.mapbox.thinbindings.android.internal.MapStyleListener
import isl.mapbox.thinbindings.android.maprendererkotlin.misc.ConditionalCompile
import isl.mapbox.thinbindings.android.maprendererkotlin.misc.FeatureColours
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.MapPosition
import isl.mapbox.thinbindings.android.positions.ScreenPosition
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

public class SymbolsLayer(context: Context, theMapView: MapView, theMapStyleListener: MapStyleListener)
{
    private val mapView: MapView = theMapView
    private val theContext: Context = context
    private val theMapStyleListener: MapStyleListener = theMapStyleListener;
    private val sourceId: String = "GeojsonSourceSymbols";
    private val symbolFeatures: MutableList<Feature> = mutableListOf<Feature>()

    private val SymbolIncreasingStartZoom: Double = 12.0;
    private val SymbolIncreasingLowZoom: Double = 13.0;
    private val SymbolIncreasingHighZoom: Double = 16.0;

    private val SymbolLowSize: Double = 0.5;
    private val SymbolHighSize: Double = 1.0;

    private val SymbolDecreasingStartZoom: Double = 1.0;
    private val SymbolDecreasingLowZoom: Double = 11.0;
    private val SymbolDecreasingHighZoom: Double = 12.0;

    private val LabelColour = FeatureColours.Orange
    private val HighlightColour = FeatureColours.Yellow

    private val lock = ReentrantLock();
    private val timeout: Long = 100;
    private val timeoutUnits = TimeUnit.MILLISECONDS;

    internal fun CreateSymbolSourceLayersAndFilters(theStyle: Style)
    {
        try
        {
            Logging.D("Will attempt to create layers and filters for ${theStyle}.")
            if (CheckIfSourceExistsAlready(theStyle) == true)
            {
                return;
            }
            AddFeaturesToSource(symbolFeatures);

            LoadSymbolImages(theStyle);

            val textSymbolLayer:SymbolLayer = SymbolLayer("symbol_text", sourceId);
            textSymbolLayer.textField(Expression.get("text"));
                // NOTE: it is essential that the font stack defined here matches one exactly in the map styles being used where we
                // will be using offline maps, otherwise the text and symbol will not render for that offline map.
                // see https://docs.mapbox.com/help/troubleshooting/mobile-offline/ and https://docs.mapbox.com/help/glossary/font-stack/
            textSymbolLayer.textFont(listOf<String>("DIN Offc Pro Regular", "Arial Unicode MS Regular"));
            textSymbolLayer.textColor(LabelColour);
            textSymbolLayer.textOpacity(1.0);
            textSymbolLayer.textSize(18.0);
            textSymbolLayer.textOffset(listOf<Double>(1.25, 0.0));
            textSymbolLayer.textAnchor(TextAnchor.LEFT);
            textSymbolLayer.textIgnorePlacement(true);
            textSymbolLayer.textAllowOverlap(true);
                // x,y textoffset to apply to centre of text. 1.0 appears similar to text height. +ve to right and down
                // Note that if text is used in combination with an image then if they're not far enough apart then you'll only see one.
            textSymbolLayer.filter(
                Expression.all
                    (
                    Expression.gte(Expression.zoom(), Expression.literal(15.0)),
                )
            );

            val poiSymbolLayer: SymbolLayer = SymbolLayer("symbol_poi", sourceId);
            poiSymbolLayer.iconImage(SymbolType.PoI.name);
            poiSymbolLayer.iconOffset(listOf<Double>(20.0, -20.0));
            poiSymbolLayer.iconIgnorePlacement(true);
            poiSymbolLayer.iconAllowOverlap(true);
            poiSymbolLayer.iconSize(
                Expression.interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(SymbolIncreasingStartZoom); literal(0.0) }
                        stop { literal(SymbolIncreasingLowZoom); literal(SymbolLowSize) }
                        stop { literal(SymbolIncreasingHighZoom); literal(SymbolHighSize) }
                    }
                )
            );
            poiSymbolLayer.filter(
                Expression.all
                    (
                    Expression.gte(Expression.zoom(), Expression.literal(13.0)),
                    Expression.eq(Expression.get("symbol"), literal(SymbolType.PoI.name))
                )
            );

            val adhocSymbolLayer: SymbolLayer = SymbolLayer("symbol_adhoc", sourceId);
            adhocSymbolLayer.iconImage(SymbolType.Adhoc.name);
            adhocSymbolLayer.iconIgnorePlacement(true);
            adhocSymbolLayer.iconAllowOverlap(true);
            adhocSymbolLayer.iconSize(
                Expression.interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(SymbolIncreasingStartZoom); literal(0.0) }
                        stop { literal(SymbolIncreasingLowZoom); literal(SymbolLowSize / 2.0) } // was 0.1 in mobile
                        stop { literal(SymbolIncreasingHighZoom); literal(SymbolHighSize / 2.0) } // was 0.5 in mobile
                    }
                )
            );
            adhocSymbolLayer.filter(
                Expression.all
                    (
                    Expression.gte(Expression.zoom(), Expression.literal(13.0)),
                    Expression.eq(Expression.get("symbol"), literal(SymbolType.Adhoc.name))
                )
            );

            val snappedLocationSymbolLayer: SymbolLayer = SymbolLayer("symbol_snappedLocation", sourceId);
            snappedLocationSymbolLayer.iconImage(SymbolType.SnappedLocation.name);
            snappedLocationSymbolLayer.iconSize(1.0);
            snappedLocationSymbolLayer.iconIgnorePlacement(true);
            snappedLocationSymbolLayer.iconAllowOverlap(true);
            snappedLocationSymbolLayer.filter(
                Expression.all
                    (
                    Expression.gte(Expression.zoom(), Expression.literal(13.0)),
                    Expression.eq(Expression.get("symbol"), literal(SymbolType.SnappedLocation.name))
                )
            );

            val manoeuvreLocationSymbolLayer: SymbolLayer = SymbolLayer("symbol_manoeuvreLocation", sourceId);
            manoeuvreLocationSymbolLayer.iconImage(SymbolType.ManoeuvreLocation.name);
            manoeuvreLocationSymbolLayer.iconSize(1.0);
            manoeuvreLocationSymbolLayer.iconIgnorePlacement(true);
            manoeuvreLocationSymbolLayer.iconAllowOverlap(true);
            manoeuvreLocationSymbolLayer.filter(
                Expression.all
                    (
                    Expression.gte(Expression.zoom(), Expression.literal(13.0)),
                    Expression.eq(Expression.get("symbol"), literal(SymbolType.ManoeuvreLocation.name))
                )
            );

            val crumbTrailSymbolLayer: SymbolLayer = SymbolLayer("symbol_crumbTrail", sourceId);
            crumbTrailSymbolLayer.iconImage(SymbolType.CrumbTrail.name);
            crumbTrailSymbolLayer.iconIgnorePlacement(true);
            crumbTrailSymbolLayer.iconSize(0.75);
            crumbTrailSymbolLayer.iconAllowOverlap(true);
            crumbTrailSymbolLayer.filter(
                Expression.all
                    (
                    Expression.gte(Expression.zoom(), Expression.literal(13.0)),
                    Expression.eq(Expression.get("symbol"), literal(SymbolType.CrumbTrail.name))
                )
            );

            val locationSymbolLayer: SymbolLayer = SymbolLayer("symbol_location", sourceId);
            locationSymbolLayer.iconImage(SymbolType.Location.name);
            locationSymbolLayer.iconSize(0.75);
            locationSymbolLayer.iconIgnorePlacement(true);
            locationSymbolLayer.iconAllowOverlap(true);
            locationSymbolLayer.filter(
                Expression.all
                    (
                    Expression.eq(Expression.get("symbol"), literal(SymbolType.Location.name)),
                )
            );

            val locationSymbolLayerWithDirection: SymbolLayer = SymbolLayer("symbol_location_with_direction", sourceId);
            locationSymbolLayerWithDirection.iconImage(SymbolType.LocationWithDirection.name);
            locationSymbolLayerWithDirection.iconSize(0.85); // For some reason had to make this slightly larger than other location symbols so it renders same on map.
            locationSymbolLayerWithDirection.iconOffset(listOf<Double>(0.0, -4.5)); // Found by trial and error.
            locationSymbolLayerWithDirection.iconIgnorePlacement(true);
            locationSymbolLayerWithDirection.iconAllowOverlap(true);
            locationSymbolLayerWithDirection.iconRotate(Expression.get("bearing"));
            locationSymbolLayerWithDirection.iconRotationAlignment(IconRotationAlignment.MAP);
            locationSymbolLayerWithDirection.filter(
                Expression.all
                    (
                    Expression.eq(Expression.get("symbol"), literal(SymbolType.LocationWithDirection.name)),
                )
            );

            val locationSearchingSymbolLayer: SymbolLayer = SymbolLayer("symbol_locationSearching", sourceId);
            locationSearchingSymbolLayer.iconImage(SymbolType.LocationSearching.name);
            locationSearchingSymbolLayer.iconSize(0.75);
            locationSearchingSymbolLayer.iconIgnorePlacement(true);
            locationSearchingSymbolLayer.iconAllowOverlap(true);
            locationSearchingSymbolLayer.filter(
                Expression.all
                    (
                    Expression.eq(Expression.get("symbol"), literal(SymbolType.LocationSearching.name))
                )
            );

            val locationDisabledSymbolLayer: SymbolLayer = SymbolLayer("symbol_locationDisabled", sourceId);
            locationDisabledSymbolLayer.iconImage(SymbolType.LocationDisabled.name);
            locationDisabledSymbolLayer.iconSize(0.75);
            locationDisabledSymbolLayer.iconIgnorePlacement(true);
            locationDisabledSymbolLayer.iconAllowOverlap(true);
            locationDisabledSymbolLayer.filter(
                Expression.all
                    (
                    Expression.eq(Expression.get("symbol"), literal(SymbolType.LocationDisabled.name))
                )
            );

            val routeOutlineSymbolLayer: SymbolLayer = SymbolLayer("symbol_routeOutline", sourceId);
            routeOutlineSymbolLayer.iconImage(SymbolType.RouteOutline.name);
            routeOutlineSymbolLayer.iconIgnorePlacement(true);
            routeOutlineSymbolLayer.iconAllowOverlap(true);
            routeOutlineSymbolLayer.iconSize(1.0);
            routeOutlineSymbolLayer.iconOpacity(
                Expression.interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(SymbolDecreasingStartZoom); literal(SymbolHighSize) }
                        stop { literal(SymbolDecreasingLowZoom); literal(SymbolLowSize) }
                        stop { literal(SymbolDecreasingHighZoom); literal(0.0) }
                    }
                )
            );
            routeOutlineSymbolLayer.filter(
                Expression.all
                    (
                    Expression.lt(Expression.zoom(), Expression.literal(13.0)),
                    Expression.eq(Expression.get("symbol"), literal(SymbolType.RouteOutline.name)),
                    Expression.eq(Expression.get("istrade"), literal(false))
                )
            );

            val tradeOutlineSymbolLayer: SymbolLayer = SymbolLayer("symbol_tradeOutline", sourceId);
            tradeOutlineSymbolLayer.iconImage(SymbolType.TradeOutline.name);
            tradeOutlineSymbolLayer.iconIgnorePlacement(true);
            tradeOutlineSymbolLayer.iconAllowOverlap(true);
            tradeOutlineSymbolLayer.iconSize(1.0);
            tradeOutlineSymbolLayer.iconOpacity(
                Expression.interpolate(
                    {
                        linear()
                        zoom()
                        stop { literal(SymbolDecreasingStartZoom); literal(SymbolHighSize) }
                        stop { literal(SymbolDecreasingLowZoom - 2.0); literal(SymbolLowSize) }
                        stop { literal(SymbolDecreasingHighZoom - 2.0); literal(0.0) }
                    }
                )
            );
            tradeOutlineSymbolLayer.filter(
                Expression.all
                    (
                    Expression.lt(Expression.zoom(), Expression.literal(13.0)),
                    Expression.eq(Expression.get("symbol"), literal(SymbolType.TradeOutline.name)),
                    Expression.eq(Expression.get("istrade"), literal(true))
                )
            );

            val highlightSymbolLayer: CircleLayer = CircleLayer("symbol_highlight", sourceId);
            highlightSymbolLayer.visibility(Visibility.VISIBLE);
            highlightSymbolLayer.circleRadius(15.0);
            highlightSymbolLayer.circleOpacity(0.5);
            highlightSymbolLayer.circleColor(HighlightColour);
            highlightSymbolLayer.filter(
                Expression.all
                    (
                    Expression.gte(Expression.zoom(), Expression.literal(14.0)),
                    Expression.eq(Expression.get("highlight"), literal(true))
                )
            );

            // This first layer will be the bottom layer on the map
            theStyle.addLayer(textSymbolLayer);
            theStyle.addLayer(poiSymbolLayer);
            theStyle.addLayer(locationSymbolLayer);
            theStyle.addLayer(locationSymbolLayerWithDirection);
            theStyle.addLayer(locationSearchingSymbolLayer);
            theStyle.addLayer(locationDisabledSymbolLayer);
            theStyle.addLayer(snappedLocationSymbolLayer);
            theStyle.addLayer(manoeuvreLocationSymbolLayer);
            theStyle.addLayer(crumbTrailSymbolLayer);
            theStyle.addLayer(routeOutlineSymbolLayer);
            theStyle.addLayer(tradeOutlineSymbolLayer);
            theStyle.addLayer(adhocSymbolLayer);
            theStyle.addLayer(highlightSymbolLayer);
            // This last layer will be the top layer on the map
        }
        catch (ex: Exception)
        {
            Logging.E("Exception = ${ex.message}")
        }
    }

    public fun AddSymbols(symbols: List<SymbolMapFeature>, id: String? = null): Boolean
    {
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                Logging.D("Called for ${symbols.count()} Symbols.");
                var featuresToAdd = mutableListOf<Feature>();
                for (symbol in symbols)
                {
                    var found = GetSymbolFeatureById(symbol.UniqueGuid.toString())
                    if (found == null)
                    {
                        Logging.V("Add a symbol at ${symbol.Longitude} ${symbol.Latitude}");
                        var point: Point = Point.fromLngLat(symbol.Longitude, symbol.Latitude); // this is 2nd slowest bit
                        var feature = Feature.fromGeometry(point, null, symbol.UniqueGuid.toString()); // this is slowest bit

                        feature.addStringProperty("symbol", symbol.Symbol.name);
                        feature.addStringProperty("text", symbol.Text);
                        feature.addBooleanProperty("istrade", symbol.IsTrade);
                        feature.addBooleanProperty("highlight", false);
                        feature.addNumberProperty("bearing", 0.0);

                        symbolFeatures.add(feature);
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

    public fun RemoveSymbols(symbols: List<SymbolMapFeature>?, id: String? = null): Boolean
    {
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                var featuresToRemove = mutableListOf<Feature>();
                if (symbols == null)
                {
                    symbolFeatures.clear()
                    Logging.D("Called with null list of symbols so removing all.");
                    UpdateAllFeaturesInSource(symbolFeatures, id);
                }
                else
                {
                    Logging.D("Called for ${symbols.count()} symbols, current total (before removal)=${symbolFeatures.count()}");
                    for (symbol in symbols)
                    {
                        var found = GetSymbolFeatureById(symbol.UniqueGuid.toString())
                        if (found != null)
                        {
                            symbolFeatures.remove(found);
                            featuresToRemove.add(found);
                        }
                    }
                    Logging.D("After removing symbols, current total=${symbolFeatures.count()}");
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

    public fun ChangeSymbolPosition(UniqueId: UUID , newPosition: GeographicPosition, newIcon: SymbolType? = null, newText: String? = null, newBearing: Double = 0.0): Boolean
    {
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                // NOTE - was not able to change faeture geometry in same fashion as have changed properties elsewhere.
                // Tried following:
                //Feature found = _reportFeatureCollection.Features().FirstOrDefault(x => x.Id().Equals(UniqueId, StringComparison.OrdinalIgnoreCase));
                // or
                //Feature found = _reportFeatures.FirstOrDefault(x => x.Id().Equals(UniqueId, StringComparison.OrdinalIgnoreCase));
                // then
                //found = Feature.FromGeometry(Point.FromLngLat(newPosition.Longitude, newPosition.Latitude), null, found.Id());
                //found.AddStringProperty("text", oldText);
                //found.AddStringProperty("symbol", oldIcon);
                // but changes never appeared in _reportFeatures or _reportFeatureCollection, so have finally used following method...
                //Log(LogType.Debug, "ChangeSymbolPositionFunc called for id={0}, new position longitude={1}, latitude={2}", UniqueId, newPosition.Longitude, newPosition.Latitude);
                var foundIndex: Int = 0;
                var found: Boolean = false;
                for (index in symbolFeatures.indices)
                {
                    if (symbolFeatures[index].id()?.lowercase() == UniqueId.toString().lowercase())
                    {
                        found = true;
                        break;
                    }
                    foundIndex++;
                }
                if (found == true)
                {
                    val oldIcon: String = symbolFeatures[foundIndex].getStringProperty("symbol");
                    val oldText: String = symbolFeatures[foundIndex].getStringProperty("text");

                    symbolFeatures[foundIndex] = Feature.fromGeometry(Point.fromLngLat(newPosition.Longitude, newPosition.Latitude), null, symbolFeatures[foundIndex].id());
                    if (newText == null)
                    {
                        symbolFeatures[foundIndex].addStringProperty("text", oldText);
                    }
                    else
                    {
                        symbolFeatures[foundIndex].addStringProperty("text", newText);
                    }

                    if (newIcon != null)
                    {
                        symbolFeatures[foundIndex].addStringProperty("symbol", newIcon.name);
                        if (newIcon == SymbolType.LocationWithDirection)
                        {
                            symbolFeatures[foundIndex].addNumberProperty("bearing", newBearing);
                        }
                        else
                        {
                            symbolFeatures[foundIndex].removeProperty("bearing");
                        }
                    }
                    else
                    {
                        symbolFeatures[foundIndex].addStringProperty("symbol", oldIcon);
                        if (oldIcon == SymbolType.LocationWithDirection.name) {
                            symbolFeatures[foundIndex].addNumberProperty("bearing", newBearing);
                        }
                        else
                        {
                            symbolFeatures[foundIndex].removeProperty("bearing");
                        }
                    }
                    Logging.V("ChangeSymbolPosition found feature for id=${UniqueId}, symbol=${oldIcon}, text=${oldText}");

                    UpdateSomeFeaturesInSource(listOf(symbolFeatures[foundIndex]));

                }
                else
                {
                    Logging.W("NOT found feature for id=${UniqueId}");
                    return false;
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

    internal fun GetLocationForSymbol(symbolFeature: SymbolMapFeature): Point?
    {
        var location: Point? = null;
        if (lock.tryLock(timeout, timeoutUnits))
        {
            try
            {
                var foundSymbolFeature = GetSymbolFeatureById(symbolFeature.UniqueGuid.toString())
                if (foundSymbolFeature != null)
                {
                    location = foundSymbolFeature.geometry() as Point;
                }
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

    public fun ChangeSymbolProperties(changesRequired: List<SymbolPropertyBase>, id: String? = null) : Int
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
                    var found = GetSymbolFeatureById(changeRequired.SymbolGuid.toString())
                    if (found != null)
                    {
                        var json = found.toJson();
                        symbolFeatures.remove(found);
                        found = Feature.fromJson(json);
                        if (changeRequired is SymbolHighlightedProperty)
                        {
                            Logging.V("Found symbol for SymbolGuid=${changeRequired.SymbolGuid}, will change 'highlight' property to ${changeRequired.Highlighted}");
                            found.removeProperty("highlight");
                            found.addBooleanProperty("highlight", changeRequired.Highlighted);
                            count++;
                        }
                        else if (changeRequired is SymbolSymbolTypeProperty)
                        {
                            Logging.V("Found symbol for SymbolGuid=${changeRequired.SymbolGuid}, will change 'symbol' property to ${changeRequired.Symbol}");
                            found.removeProperty("symbol");
                            found.addStringProperty("symbol", changeRequired.Symbol.name);
                            count++;
                        }
                        symbolFeatures.add(found);
                        featuresToChange.add(found);
                    }
                    else
                    {
                        countFail++;
                        if (countFail < 5)
                        {
                            Logging.W("Attempt to change property for an unknown symbol SymbolGuid=${changeRequired.SymbolGuid}");
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

    internal fun CreatePointOfInterestFeature(newLocation: GeographicPosition, feature: Feature): PointOfInterestFeature?
    {
        if (feature.geometry() is Point && feature.properties() != null)
        {
            var properties: JsonObject = feature.properties()!!;

            var symbolProperty = properties.get("symbol");
            var symbol: SymbolType = SymbolType.PoI;
            if (symbolProperty != null)
            {
                symbol = SymbolType.valueOf(symbolProperty.asString);
            }

            var text = "";
            var textProperty = properties.get("text");
            if (textProperty != null)
            {
                text = textProperty.asString;
            }

            var screenLocation = mapView.mapboxMap
                .pixelForCoordinate(Point.fromLngLat(newLocation.Longitude, newLocation.Latitude));

            var tappedMapFeature: PointOfInterestFeature = PointOfInterestFeature();
            tappedMapFeature.UniqueGuid = UUID.fromString(feature.id());
            tappedMapFeature.Symbol = symbol;
            tappedMapFeature.Text = text;
            tappedMapFeature.TappedPosition = MapPosition(GeoPosition = GeographicPosition(Longitude = newLocation.Longitude, Latitude = newLocation.Latitude), ScreenPosition = ScreenPosition(X = screenLocation.x, Y = screenLocation.y, mapView.width, mapView.height));
            if (feature.id() != null)
            {
                tappedMapFeature.UniqueGuid = UUID.fromString(feature.id());
            }

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
            Logging.D("PointOfInterestFeature with UniqueGuid=${tappedMapFeature.UniqueGuid}, Text=${tappedMapFeature.Text}, Note=${tappedMapFeature.Note}, Comments=${tappedMapFeature.Comments}, Bearing=${tappedMapFeature.Bearing}, Symbol=${tappedMapFeature.Symbol}, Identity.${tappedMapFeature.Identity}");

            return tappedMapFeature;
        }
        else
        {
            return null;
        }
    }

    internal fun CreateAdhocFeature(newLocation: GeographicPosition, feature: Feature? = null): AdhocFeature?
    {
        var tappedMapFeature: AdhocFeature? = null;
        var screenLocation = mapView.mapboxMap
            .pixelForCoordinate(Point.fromLngLat(newLocation.Longitude, newLocation.Latitude));
        if (feature == null)
        {
            var reportGuid: UUID = UUID.randomUUID();
            tappedMapFeature = AdhocFeature();
            tappedMapFeature.UniqueGuid = reportGuid;
            tappedMapFeature.Identity = reportGuid.toString();
            tappedMapFeature.Symbol = SymbolType.Adhoc;
            tappedMapFeature.Text = "Adhoc report."; // ??? TBD How do we localize this text?
            tappedMapFeature.Longitude = newLocation.Longitude;
            tappedMapFeature.Latitude = newLocation.Latitude;
            tappedMapFeature.JustCreated = true;
            tappedMapFeature.TappedPosition = MapPosition(GeoPosition = GeographicPosition(Longitude = newLocation.Longitude, Latitude = newLocation.Latitude), ScreenPosition = ScreenPosition(X = screenLocation.x, Y = screenLocation.y, mapView.width, mapView.height));
        }
        else if (feature.geometry() is Point)
        {
            if (feature.properties() != null)
            {
                var properties: JsonObject? = feature.properties();

                var symbolProperty = properties!!.get("symbol");
                var symbol: SymbolType = SymbolType.PoI;
                if (symbolProperty != null)
                {
                    symbol = SymbolType.valueOf(symbolProperty.asString);
                }

                var text = "";
                var textProperty = properties.get("text");
                if (textProperty != null)
                {
                    text = textProperty.asString;
                }

                tappedMapFeature = AdhocFeature();

                tappedMapFeature.UniqueGuid = UUID.fromString(feature.id());
                tappedMapFeature.Symbol = symbol;
                tappedMapFeature.Text = text;
                tappedMapFeature.TappedPosition = MapPosition(GeoPosition = GeographicPosition( Longitude = newLocation.Longitude, Latitude = newLocation.Latitude), ScreenPosition = ScreenPosition(X = screenLocation.x, Y = screenLocation.y, mapView.width, mapView.height));
                tappedMapFeature.Longitude = newLocation.Longitude;
                tappedMapFeature.Latitude = newLocation.Latitude;
                if (feature.id() != null)
                {
                    tappedMapFeature.UniqueGuid = UUID.fromString(feature.id());
                    tappedMapFeature.Identity = feature.id().toString();
                    Logging.D("Feature id is '${feature.id()}'.")
                }
                else
                {
                    Logging.W("Feature id is null.")
                }
                Logging.D("AdhocFeature with UniqueGuid=${tappedMapFeature.UniqueGuid}, Text=${tappedMapFeature.Text}, Identity.${tappedMapFeature.Identity}");
            }
        }
        else
        {
            tappedMapFeature = null;
        }

        return tappedMapFeature;
    }

    /**
     * Attempt to load bitmap images into Mapbox to use as symbols.
     */
    private fun LoadSymbolImages(theStyle: Style) {
        val poiSymbol = ContextCompat.getDrawable(theContext, R.drawable.poi)?.toBitmap()!!
        theStyle.addImage(SymbolType.PoI.name, poiSymbol);

        val routeOutlineSymbol = ContextCompat.getDrawable(theContext, R.drawable.routeoutline)?.toBitmap()!!
        theStyle.addImage(SymbolType.RouteOutline.name, routeOutlineSymbol);

        val tradeOutlineSymbol = ContextCompat.getDrawable(theContext, R.drawable.tradeoutline)?.toBitmap()!!
        theStyle.addImage(SymbolType.TradeOutline.name, tradeOutlineSymbol);

        val snappedLocationSymbol = ContextCompat.getDrawable(theContext, R.drawable.snappedlocation)?.toBitmap(40, 40)!!
        theStyle.addImage(SymbolType.SnappedLocation.name, snappedLocationSymbol);

        val manoeuvreLocationSymbol = ContextCompat.getDrawable(theContext, R.drawable.manoeuvrelocationgreencircle)?.toBitmap(40, 40)!!
        theStyle.addImage(SymbolType.ManoeuvreLocation.name, manoeuvreLocationSymbol);

        val crumbTrailSymbol = ContextCompat.getDrawable(theContext, R.drawable.crumbtrail)?.toBitmap()!!
        theStyle.addImage(SymbolType.CrumbTrail.name, crumbTrailSymbol);

        val currentLocationSymbol = ContextCompat.getDrawable(theContext, R.drawable.mylocationoutline)?.toBitmap()!!
        theStyle.addImage(SymbolType.Location.name, currentLocationSymbol);

        val currentLocationWithDirectionSymbol = ContextCompat.getDrawable(theContext, R.drawable.mylocationoutlinewithdirection)?.toBitmap()!!
        theStyle.addImage(SymbolType.LocationWithDirection.name, currentLocationWithDirectionSymbol);

        val searchingLocationSymbol = ContextCompat.getDrawable(theContext, R.drawable.locationsearchingoutline)?.toBitmap()!!
        theStyle.addImage(SymbolType.LocationSearching.name, searchingLocationSymbol);

        val disabledLocationSymbol = ContextCompat.getDrawable(theContext, R.drawable.locationdisabledoutline)?.toBitmap()!!
        theStyle.addImage(SymbolType.LocationDisabled.name, disabledLocationSymbol);

        val adhocSymbol = ContextCompat.getDrawable(theContext, R.drawable.adhoc)?.toBitmap()!!
        theStyle.addImage(SymbolType.Adhoc.name, adhocSymbol);
    }

    /**
     * Find and return the symbol feature by it's id
     */
    private fun GetSymbolFeatureById(subjectId: String): Feature?
    {
        if(symbolFeatures.isNullOrEmpty()) return null;
        return symbolFeatures.firstOrNull { x -> x.id().equals(subjectId) }
    }

    private fun CheckIfSourceExistsAlready(theStyle: Style): Boolean
    {
        val testGeoJsonSource: Source?  = theStyle.getSource(sourceId);
        if (testGeoJsonSource != null)
        {
            // We shouldn't find ourselves here but just in case...
            Logging.W("For some reason we already have a source of this name.");

            return true;
        }
        return false;
    }
    private fun AddFeaturesToSource(features: List<Feature>)
    {
        var symbolFeatureCollection = FeatureCollection.fromFeatures(features);

        val geoJsonSource = geoJsonSource(sourceId)
        {
            featureCollection(symbolFeatureCollection)
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
            var symbolFeatureCollection = FeatureCollection.fromFeatures(features.toMutableList());
            var geoJsonSource: GeoJsonSource? = mapView.mapboxMap.style?.getSourceAs<GeoJsonSource>(sourceId);
            if (id.isNullOrEmpty() == false)
            {
                geoJsonSource?.featureCollection(symbolFeatureCollection, id);
            }
            else
            {
                geoJsonSource?.featureCollection(symbolFeatureCollection);
            }
            Logging.V("Will update ${features.count()} symbolFeatures, number in symbolFeatureCollection=${symbolFeatureCollection.features()?.count()}");
        //});
    }

    private fun UpdateSomeFeaturesInSource(features: List<Feature>, id: String? = null)
    {
        //Handler(Looper.getMainLooper()).post(
        //    {
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
                    Logging.V("Will update ${features.count()} symbolFeatures.");
                }
          //  });
    }
    private fun AddSomeFeaturesInSource(features: List<Feature>, id: String? = null)
    {
        //Handler(Looper.getMainLooper()).post(
          //  {
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
                    Logging.V("Will add ${features.count()} symbolFeatures.");
                }
       //     });
    }

    private fun RemoveSomeFeaturesInSource(features: List<Feature>, id: String? = null)
    {
  //      Handler(Looper.getMainLooper()).post(
  //          {
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
                    Logging.V("Will remove ${features.count()} symbolFeatures.");
                }
  //          });
    }

}

public class SymbolSymbolTypeProperty : SymbolPropertyBase()
{
    public var Symbol: SymbolType = SymbolType.Custom;
}

public class SymbolHighlightedProperty : SymbolPropertyBase()
{
    public var Highlighted: Boolean = false;
}

public abstract class SymbolPropertyBase : MapFeaturePropertyBase()
{
    public lateinit var SymbolGuid: UUID
}
