package isl.mapbox.thinbindings.android.positions

import isl.mapbox.thinbindings.android.maprendererkotlin.NullableType
import isl.mapbox.thinbindings.android.maprendererkotlin.map.MoveCameraTypeEnum

public class MapCameraPosition(val Position: GeographicPosition, val Bearing: NullableType<Double>?, val Tilt: NullableType<Double>?, val Zoom: NullableType<Double>?, val MoveType: MoveCameraTypeEnum?, val MovementDuration: NullableType<Long>?)
{

}
