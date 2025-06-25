package isl.mapbox.thinbindings.android.positions

/***
 * Object to contain data about the results of a search for nearest road to a point.
 *
 * @property Classification Class value property of the map feature.
 * @property Name Street name
 * @property Number Reference
 * @property CertaintyIndex Accuracy of this result
 * @property Accurate Accuracy better than threshold
 * @property Position Point this result is for
 */
public class RoadPosition(
    val Classification: String,
    val Name: String,
    val Number: String,
    val CertaintyIndex: Double,
    val Accurate: Boolean,
    val Position: GeographicPosition)
{
}