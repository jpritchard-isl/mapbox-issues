package isl.mapbox.thinbindings.android.maprendererkotlin.map

import isl.mapbox.thinbindings.android.features.MapFeature
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.RoadPosition

/**
 * The Feature Selection object.
 */
interface IFeatureSelection
{
    /**
     * This enables the ablity to select map features
     */
    var featureSelectionAllowed: Boolean;

    /**
     * This will contain the map features that were detected due to the last map click. Note that duplicates will have been removed and the list will be sorted.
     */
    val tappedMapFeatures: List<MapFeature>;

    /**
     * This will contain the most significant map feature detected due to the last map click.
     */
    val selectedMapFeature: MapFeature?;

    /**
     * This will contain a subset of the last tapped map features combined with previous ones where multipleSelectionAllowed is true.
     */
    val selectedMapFeatures: List<MapFeature>;

    /**
     * This will clear the selectedMapFeature, selectedMapFeatures and associate highlights on the map, and also any current adhoc report.
     */
    fun ClearSelectedMapFeatures();

    /**
     * Toggle the selected highlight of a map feature
     */
    fun ToggleSelectedMapFeature(mapFeature: MapFeature);

    /**
     * This will enable accumulated selection of multiple map features of the same type.
     */
    var MultipleSelectionAllowed: Boolean

    /**
     * If set true then every so often a call to update the location marker position will also result in a call to GetRoadPosition,
     * with the result signalled using the IFeatureSelectionCallback interface to any object registered using the RegisterForFeatureSelectionCallback
     * method of the IMapRendererKotlin Interface. The default value is true.
     */
    var autoDetectRoadPosition: Boolean

    /**
     * This sets the rate at which the auto detect road position is performed. The default value is 5.
     */
    var autoRoadPositionDetectRate: Int;

    /**
     * If wait is true then method will return the road position once it is known, if false then returns sooner and road position notified in registered callback.
     */
    fun GetRoadPosition(position: GeographicPosition, wait: Boolean = false): RoadPosition?
}