package isl.mapbox.thinbindings.android.features

import isl.mapbox.thinbindings.android.positions.MapPosition
import java.util.UUID

/**
 * MapFeatures are objects that need to be rendered on the map.
 * This and the derived classes contain all the properties that are relevant to how the object is to be visually represented,
 * along with any additional properties pertinent to describing the selection of the feature on the map by the operator.
 */
public abstract class MapFeature
{
    /**
     * The Guid of the map feature, currently this is the same as the Guid for the source item eg ServicePoint.UniqueId but may change this and have explicit members for this???
     */
    public lateinit var UniqueGuid: UUID

    /**
     * Organisation unique identity of this asset.
     */
    public var Identity: String = ""

    /**
     * This is used when the MapFeature is signalled by the MapRenderer due to operator interaction with the map.
     */
    public var TappedPosition: MapPosition? = null

    /**
     * If this feature instance is in a duplicate position to another selected feature of the same type.
     **/
    public var DuplicatePosition: Boolean = false
}