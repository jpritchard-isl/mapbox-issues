package isl.mapbox.thinbindings.android.maprendererkotlin

/**
 * This has bee added to facilitate passing of nullable basic types like Double? as parameters through the bindings from C# maprenderer.
 * This does not need to be used for passing more complex types (and enums).
 */
public class NullableType<T>(private val value: T)
{
    public val Value: T = value;
    public fun GetValue(): T
    {
        return Value;
    }
}