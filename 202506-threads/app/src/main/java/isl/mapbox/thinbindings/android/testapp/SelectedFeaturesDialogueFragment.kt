package isl.mapbox.thinbindings.android.testapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import isl.mapbox.thinbindings.android.features.AdhocFeature
import isl.mapbox.thinbindings.android.features.JobFeature
import isl.mapbox.thinbindings.android.features.MapFeature
import isl.mapbox.thinbindings.android.features.PointOfInterestFeature
import isl.mapbox.thinbindings.android.features.RoadSegmentFeature
import isl.mapbox.thinbindings.android.features.RouteRoadSegmentFeature
import isl.mapbox.thinbindings.android.features.RouteRoadSegmentNonServiceFeature
import isl.mapbox.thinbindings.android.features.ServicePointAndTradeSiteFeature

class SelectedFeaturesDialogueFragment(mapFeatures: List<MapFeature>, title: String, mapFeature: MapFeature? = null, includeSelectedFeature: Boolean = false): DialogFragment()
{
    var mapFeatures = mapFeatures;
    var mapFeature = mapFeature;
    var includeSelectedFeature = includeSelectedFeature;
    val title = title;

    // See https://developer.android.com/develop/ui/views/components/dialogs
    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog
    {
        return activity?.let {
            val builder = AlertDialog.Builder(it);

            val test = getLayoutInflater().inflate(R.layout.selectedfeaturesdisplay, null);
            var selectedFeaturesText: TextView = test!!.findViewById(R.id.selectedfeature);
            selectedFeaturesText.text = GetFeatureDisplayText(mapFeature, mapFeatures, includeSelectedFeature);
            builder.setView(test)
                .setTitle(title)
                .setNegativeButton("Cancel", DialogInterface.OnClickListener
                { dialog, id ->
                    getDialog()?.cancel()
                })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun GetFeatureDisplayText(mapFeature: MapFeature?, tappedMapFeatures: List<MapFeature>, includeSelectedFeature: Boolean): String
    {
        var message = "";
        if (includeSelectedFeature == true)
        {
            if (mapFeature == null)
            {
                message += "Seleced Map Feature is null.\n"
            }
            else
            {
                message += "Selected Map Feature = ";
                message += GetFeatureDisplayText(mapFeature) + "\n";
            }
        }
        if (tappedMapFeatures.count() > 0)
        {
            for (index in tappedMapFeatures.indices)
            {
                message += "${index}. ";
                val feature = tappedMapFeatures[index];
                message += GetFeatureDisplayText(feature);
            }
        }
        else
        {
            message += "SelectedMapFeatures = None.";
        }
        return message;
    }

    private fun GetFeatureDisplayText(feature: MapFeature?): String
    {
        var message = "";
        if (feature is RouteRoadSegmentFeature)
        {
            message += "RouteRoadSegmentFeature with UniqueGuid=${feature.UniqueGuid}, roadSegmentGuid=${feature.RoadGuid}, Name=${feature.Name}, Text=${feature.Text}, Note=${feature.Note}, ServiceNote=${feature.ServiceNote}, Comments=${feature.Comments}, Serviced=${feature.Serviced.toString()}, Reported=${feature.Reported}, routeSegmentGuid=${feature.RouteGuid}, Identity=${feature.Identity}, Manoeuvre=${feature.ManoeuvreAtEnd}, Length=${feature.Length}.";
        }
        else if (feature is RouteRoadSegmentNonServiceFeature)
        {

            message += "RouteRoadSegmentNonServiceFeature with UniqueGuid=${feature.UniqueGuid}, roadSegmentGuid=${feature.RoadGuid}, Name=${feature.Name}, Text=${feature.Text}, Note=${feature.Note}, ServiceNote=${feature.ServiceNote}, Comments=${feature.Comments}, Reported=${feature.Reported}, routeSegmentGuid=${feature.RouteGuid}, Identity=${feature.Identity}, Manoeuvre=${feature.ManoeuvreAtEnd}, Length=${feature.Length}."
        }
        else if (feature is RoadSegmentFeature)
        {
            message += ("RoadSegmentFeature with UniqueGuid=${feature.UniqueGuid}, roadSegmentGuid=${feature.RoadGuid}, Name=${feature.Name}, Text=${feature.Text}, Note=${feature.Note}, Comments=${feature.Comments}, Identity=${feature.Identity}, Length=${feature.Length}.");
        }
        else if (feature is AdhocFeature)
        {
            message += "AdhocFeature with UniqueGuid=${feature.UniqueGuid}, Text=${feature.Text}, Identity.${feature.Identity}, JustCreated=${feature.JustCreated}";
        }
        else if (feature is PointOfInterestFeature)
        {
            message += "PointOfInterestFeature with UniqueGuid=${feature.UniqueGuid}, Text=${feature.Text}, Note=${feature.Note}, Comments=${feature.Comments}, Bearing=${feature.Bearing}, Symbol=${feature.Symbol}, Identity.${feature.Identity}";
        }
        else if (feature is JobFeature)
        {
            message += "JobFeature with UniqueGuid=${feature.UniqueGuid}, Name=${feature.Name}, Reported=${feature.Reported}, Status=${feature.Status.toString()}, NearlyDue=${feature.Nearlydue}, Overdue=${feature.Overdue}, Identity.${feature.Identity}";
        }
        else if (feature is ServicePointAndTradeSiteFeature)
        {
            message += "ServicePointAndTradeSiteFeature with UniqueGuid=${feature.UniqueGuid}, Flat=${feature.Flat}, NameOrNumber=${feature.NameOrNumber}, Address=${feature.Address}, IsTrade=${feature.IsTrade}, Note=${feature.Note}, Comments=${feature.Comments}, ServiceComment=${feature.ServiceComment}, Serviced=${feature.Serviced}, Action=${feature.Action}, Actioned=${feature.Actioned}, IsStopped=${feature.IsStopped}, Reported=${feature.Reported}.";
        }
        message += "\n";

        return message;
    }
}