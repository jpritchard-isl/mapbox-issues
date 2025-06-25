package isl.mapbox.thinbindings.android.testapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class MultiFeatureCreationDialogueFragment(defaultNumberRouteRoadfSegments: Int, defaultNumberServicePoints: Int) : DialogFragment()
{
    internal lateinit var listener: IMultiFeatureCreationDialogListener
    private var defaultNumberRouteRoadfSegments = defaultNumberRouteRoadfSegments;
    private var defaultNumberServicePoints = defaultNumberServicePoints;
    private lateinit var buttonCreateMultiFeatures: Button;
    public interface IMultiFeatureCreationDialogListener {
        fun onCreateMultiFeatures(numberRouteSegments: Int, numberServicePoints: Int);
    }

    override fun onAttach(context: Context)
    {
        super.onAttach(context)
        try
        {
            listener = context as IMultiFeatureCreationDialogListener
        }
        catch (e: ClassCastException)
        {
            throw ClassCastException((context.toString() + " must implement NoticeDialogListener"))
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog
    {
        return activity?.let {
            val builder = AlertDialog.Builder(it);

            val test = getLayoutInflater().inflate(R.layout.multifeaturecreationdisplay, null);
            var NumberRoadSegments: EditText = test!!.findViewById(R.id.editTextNumberOfRoadSegments);
            NumberRoadSegments.setText(defaultNumberRouteRoadfSegments.toString());
            var NumberServicePoints: EditText = test!!.findViewById(R.id.editTextNumberOfServicePoints);
            NumberServicePoints.setText(defaultNumberServicePoints.toString());

            buttonCreateMultiFeatures = test!!.findViewById(R.id.buttonCreateMultiFeatures);
            buttonCreateMultiFeatures.setOnClickListener(onCreateMultiFeaturesButton);

            builder.setView(test)
                .setTitle("Create multiple features")
                .setNegativeButton("Close", DialogInterface.OnClickListener
                { dialog, id ->
                    getDialog()?.cancel()
                })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    val onCreateMultiFeaturesButton = View.OnClickListener {
        //var aButton = it.findViewById<Button>(R.id.multiFeatureButton); // always null! so use persisted buttonCreateMultiFeatures instead!
        buttonCreateMultiFeatures.setEnabled(false);
        val test = getDialog();
        var NumberRoadSegments: EditText = test!!.findViewById(R.id.editTextNumberOfRoadSegments);
        var NumberServicePoints: EditText = test!!.findViewById(R.id.editTextNumberOfServicePoints);
        //var number = NumberRoadSegments.text.toString().toInt();
        //mainActivity.ShowMultiFeatures(NumberRoadSegments.text.toString().toInt(), NumberServicePoints.text.toString().toInt());

        listener.onCreateMultiFeatures(NumberRoadSegments.text.toString().toInt(), NumberServicePoints.text.toString().toInt());
    }

}