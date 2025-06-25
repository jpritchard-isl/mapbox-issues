package isl.mapbox.thinbindings.android.features

/**
 * This will be used to render a line from the current location to a targeted feature.
 */
public open class PathToTarget : LineMapFeature()
{
    /**
     * True, for all path to target features
     */
    public val IsPathToTarget = true;
}