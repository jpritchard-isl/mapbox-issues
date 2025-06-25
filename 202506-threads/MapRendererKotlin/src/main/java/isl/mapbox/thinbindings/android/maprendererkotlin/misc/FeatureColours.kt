package isl.mapbox.thinbindings.android.maprendererkotlin.misc

import com.mapbox.maps.extension.style.expressions.generated.Expression

/**
 * Colour definitions for reuse in Map Layers
 */
internal object FeatureColours {

    /**
     * Typically used for a feature that needs attention
     */
    val Orange = Expression.rgb(255.0, 111.0, 000.0);

    /**
     * Typically used to highlight a feature that is currently selected or focused
     * Also used with red to show a feature has comments
     */
    val Yellow = Expression.rgb(255.0, 255.0, 0.0);

    /**
     * Typically used for features that have been completed and do not need attention
     */
    val Grey = Expression.rgb(200.0, 200.0, 200.0);

    /**
     * Typically used for features that are yet to be completed
     */
    val Blue =  Expression.rgb(91.0, 154.0, 255.0);

    /**
     * Used with Yellow to indicate feature has comments
     */
    val Red: Expression = Expression.rgb(255.0, 0.0, 0.0)

    /**
     * Used for non-planned routes
     */
    val Green: Expression = Expression.rgb(0.0, 214.0, 25.0);

    /**
     * Used for Manoeuvre indication
     */
    val BoldOrange: Expression = Expression.rgb(255.0, 150.0, 50.0)

    /**
     * Used for dev/debugging features
     */
    val Purple: Expression = Expression.rgb(160.0, 0.0, 255.0);
}