package isl.mapbox.thinbindings.android.maprendererkotlin

/**
 * This should be supported by MapRenderer in the C# portion of ThinBindings so that map events can be passed back to there.
 */
interface IMapCallback
{
    /**
     * Once registered this method will be called from MapRendererKotlin components to indicate a style is loaded.
     */
    fun StyleLoadedFromMapRendererKotlin();

    /**
     * Once registered this method will be called from MapRendererKotlin components to indicate that the map is loaded.
     */
    fun MapLoadedFromMapRendererKotlin();

    /**
     * Once registered this method will be called from MapRendererKotlin components to indicate that the map is idle.
     */
    fun MapIdleFromMapRendererKotlin();
}