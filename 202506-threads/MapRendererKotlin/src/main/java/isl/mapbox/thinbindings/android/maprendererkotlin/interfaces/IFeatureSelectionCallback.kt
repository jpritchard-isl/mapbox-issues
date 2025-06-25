package isl.mapbox.thinbindings.android.maprendererkotlin

import isl.mapbox.thinbindings.android.features.MapFeature
import isl.mapbox.thinbindings.android.positions.MapPosition
import isl.mapbox.thinbindings.android.positions.RoadPosition

/**
 * This should be supported by MapRenderer in the C# portion of ThinBindings so that feature selections can be passed back to there.
 */
interface IFeatureSelectionCallback
{
    /**
     * This will be called when a map click has been detected.
     */
    fun MapClickDetected(clickDetected: MapPosition);

    /**
     * This will be called when a long map click has been detected.
     */
    fun MapLongClickDetected(longClickDetected: MapPosition);

    /**
     * This will be called after GetRoadPosition has been called with the wait parameter set as false.
     */
    fun RoadPositionDetected(roadPosition: RoadPosition?);

    /**
     * This will be called after a map click has resulted in features being detected.
     */
    fun FeaturesTapped(tappedMapFeatures: MutableList<MapFeature>);

    /**
     * ??
     */
    fun FeaturesSelected(selectedMapFeature: MapFeature?, selectedMapFeatures: MutableList<MapFeature>)

}
