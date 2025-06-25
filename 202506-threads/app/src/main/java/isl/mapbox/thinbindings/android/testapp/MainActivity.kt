package isl.mapbox.thinbindings.android.testapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import isl.mapbox.thinbindings.android.features.DirectionOfTravel
import isl.mapbox.thinbindings.android.features.DirectionsOfService
import isl.mapbox.thinbindings.android.features.JobFeature
import isl.mapbox.thinbindings.android.features.JobStatus
import isl.mapbox.thinbindings.android.features.ManoeuvreType
import isl.mapbox.thinbindings.android.features.MapFeature
import isl.mapbox.thinbindings.android.features.NavigationSegmentFeature
import isl.mapbox.thinbindings.android.features.PointOfInterestFeature
import isl.mapbox.thinbindings.android.features.RoadSegmentFeature
import isl.mapbox.thinbindings.android.features.RouteRoadSegmentFeature
import isl.mapbox.thinbindings.android.features.ServicePointAndTradeSiteFeature
import isl.mapbox.thinbindings.android.features.SymbolMapFeature
import isl.mapbox.thinbindings.android.features.SymbolType
import isl.mapbox.thinbindings.android.maprendererkotlin.IFeatureSelectionCallback
import isl.mapbox.thinbindings.android.maprendererkotlin.IGestureCallback
import isl.mapbox.thinbindings.android.maprendererkotlin.MapRendererKotlin
import isl.mapbox.thinbindings.android.maprendererkotlin.map.ExtentType
import isl.mapbox.thinbindings.android.maprendererkotlin.IMapCameraCallback
import isl.mapbox.thinbindings.android.maprendererkotlin.IMapCallback
import isl.mapbox.thinbindings.android.maprendererkotlin.map.LocationMarkerType
import isl.mapbox.thinbindings.android.maprendererkotlin.map.MoveCameraTypeEnum
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.A2BNavigationSegmentCompletedProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.A2BNavigationSegmentStageProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.JobPropertyBase
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.JobReportedProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.ManoeuvreSegmentProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.RoadSegmentPropertyBase
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.RouteRoadSegmentNonServiceSnappedProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.RouteRoadSegmentServicedProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.ServicePointAndTradeSitePropertyBase
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.ServicePointProximityProperty
import isl.mapbox.thinbindings.android.maprendererkotlin.map.layers.ServicePointServicedProperty
import isl.mapbox.thinbindings.android.positions.GeographicPosition
import isl.mapbox.thinbindings.android.positions.LongLatExtent
import isl.mapbox.thinbindings.android.positions.MapCameraPosition
import isl.mapbox.thinbindings.android.positions.MapPadding
import isl.mapbox.thinbindings.android.positions.MapPosition
import isl.mapbox.thinbindings.android.positions.RoadPosition
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import kotlin.concurrent.scheduleAtFixedRate


class MainActivity : CompoundButton.OnCheckedChangeListener, AdapterView.OnItemSelectedListener, AppCompatActivity(),
    IMapCameraCallback, IGestureCallback, IFeatureSelectionCallback,
    MultiFeatureCreationDialogueFragment.IMultiFeatureCreationDialogListener, IMapCallback {
    lateinit var theMap: MapRendererKotlin
    lateinit var roadSegmentTest: RoadSegmentTest
    lateinit var servicePointAndTradeSiteTest: ServicePointAndTradeSiteTest
    lateinit var jobTest: JobTest
    lateinit var symbolTest: SymbolTest
    lateinit var locationMarkerTest: LocationMarkerTest
    private val TAG = "TestApp";
    private val ShowDeveloperFeatures = true;

    private var mapInitiallyLoaded = false;
    private lateinit var longitude: EditText;
    private lateinit var latitude: EditText;
    private lateinit var zoom: EditText;
    private lateinit var pitch: EditText;
    private lateinit var rotation: EditText;
    private lateinit var roadPositionDisplay: EditText;
    private lateinit var featuresTappedNumber: TextView;
    private lateinit var featuresSelectedNumber: TextView;

    private lateinit var spinnerExtent: Spinner;

    private var testExtent = LongLatExtent(GeographicPosition(-Double.MAX_VALUE, -Double.MAX_VALUE), GeographicPosition(Double.MAX_VALUE, Double.MAX_VALUE));

    private lateinit var targetedServicePointAndTradeSiteFeature: ServicePointAndTradeSiteFeature;
    private  lateinit var targetedRoadSegmentFeature: RoadSegmentFeature;
    private  lateinit var targetedRouteSegmentFeature: RoadSegmentFeature;
    private lateinit var targetedJobFeature: JobFeature;
    private lateinit var targetedPoIFeature: PointOfInterestFeature;
    private lateinit var a2BNavigationCompleted: NavigationSegmentFeature;
    private lateinit var a2BNavigationBefore: NavigationSegmentFeature;
    private lateinit var a2BNavigationAfter: NavigationSegmentFeature;

    private lateinit var hammerItSwitch: Switch;
    private lateinit var showPathSwitch: Switch;
    private lateinit var showPathInTargetedExtentSwitch: Switch;
    private var timer: TimerTask? = null;

    override fun onRestart() {
        super.onRestart()
    }

    override fun onResume() {
        super.onResume();
        mapInitiallyLoaded = false;
        setup();
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    private fun setup()
    {
        setContentView(R.layout.activity_main)

        val lightButton: Button = findViewById(R.id.lightButton)
        val darkButton: Button = findViewById(R.id.darkButton)
        longitude = findViewById(R.id.editTextLongitude)
        latitude = findViewById(R.id.editTextTextLatitude)
        zoom = findViewById(R.id.editTextZoom)
        pitch = findViewById(R.id.editTextPitch)
        rotation = findViewById(R.id.editTextRotation)
        val getButton: Button = findViewById(R.id.getButton)
        lightButton.setOnClickListener(onLightButton)
        darkButton.setOnClickListener(onDarkButton)
        getButton.setOnClickListener(onGetButton)
        val paddingSwitch: Switch = findViewById(R.id.switchPadding);
        hammerItSwitch = findViewById(R.id.switchHammer);
        showPathSwitch = findViewById(R.id.switchShowPath);
        showPathSwitch.setOnCheckedChangeListener(this);
        paddingSwitch.setOnCheckedChangeListener(this);
        val extentPaddingSwitch: Switch = findViewById(R.id.switchExtentPadding);
        extentPaddingSwitch.setOnCheckedChangeListener(this);
        val bearingSwitch: Switch = findViewById(R.id.switchUseBearing);
        bearingSwitch.setOnCheckedChangeListener(this);
        val restartButton: Button = findViewById(R.id.restartButton);
        restartButton.setOnClickListener(onRestartButton);
        val multiFeatureButton: Button = findViewById(R.id.multiFeatureButton);
        multiFeatureButton.setOnClickListener(onMultiFeatureButton);
        roadPositionDisplay = findViewById(R.id.editTextRoadPosition)
        val featuresTappedButton: Button = findViewById(R.id.tappedFeaturesButton);
        featuresTappedButton.setOnClickListener(onFeaturesTappedButton);
        featuresTappedNumber = findViewById(R.id.textViewTappedFeatureCount)
        featuresSelectedNumber = findViewById(R.id.textViewSelectedFeatureCount);
        val featuresSelectedButton: Button = findViewById(R.id.selectedFeaturesButton);
        featuresSelectedButton.setOnClickListener(onFeaturesSelectedButton);
        val multipleSelectionSwitch: Switch = findViewById(R.id.switchMultipleSelection);
        multipleSelectionSwitch.setOnCheckedChangeListener(this);
        val clearSelectionButton: Button = findViewById(R.id.clearSelectionButton);
        clearSelectionButton.setOnClickListener(onClearSelectionButton);
        showPathInTargetedExtentSwitch = findViewById(R.id.switchShowPathInTargetedExtent);
        showPathInTargetedExtentSwitch.setOnCheckedChangeListener(this);

        spinnerExtent = findViewById(R.id.spinnerExtent);
        val arrayListExtent = ArrayList<String>()
        arrayListExtent.add("None")
        arrayListExtent.add("CentreOnLocation")
        arrayListExtent.add("CentreOnSelectedFeatures")
        arrayListExtent.add("FullExtent")
        arrayListExtent.add("FullExtentAndLocation")
        arrayListExtent.add("TargetedExtent")
        arrayListExtent.add("TargetedExtentAndLocation")
        val arrayAdapterExtent = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayListExtent)
        arrayAdapterExtent.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerExtent.adapter = arrayAdapterExtent;
        spinnerExtent.setOnItemSelectedListener(this);

        val spinnerTarget: Spinner = findViewById(R.id.spinnerTargetedFeature);
        val arrayListTarget = ArrayList<String>()
        arrayListTarget.add("None")
        arrayListTarget.add("Service Point")
        arrayListTarget.add("Job")
        arrayListTarget.add("PoI")
        arrayListTarget.add("Route Segment")
        arrayListTarget.add("Road Comment")
        arrayListTarget.add("PoI and Road Comment")
        arrayListTarget.add("A2B Navigation")
        val arrayAdapterTarget = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayListTarget)
        arrayAdapterTarget.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTarget.adapter = arrayAdapterTarget;
        spinnerTarget.setOnItemSelectedListener(this);

        theMap = MapRendererKotlin(this, useTextureView = true)
        var linear_layout = this.findViewById(R.id.linear_layout) as LinearLayout
        linear_layout.addView(theMap)

        // If we are not using the lifecycle Plugin then manually start the instance
        theMap.StartInstance();

        // We will set a 'default' style here, as without this we don't get the style callback and therefore setup of the layers needed for rendering features.
        theMap.mapStyle.LoadStyle(theMap.mapStyle.MapStyleSmartSuiteLight)
        roadSegmentTest = RoadSegmentTest(theMap, testExtent)
        servicePointAndTradeSiteTest = ServicePointAndTradeSiteTest(theMap, testExtent)
        jobTest = JobTest(theMap, testExtent);
        symbolTest = SymbolTest(theMap, testExtent);
        locationMarkerTest = LocationMarkerTest(theMap);
        theMap.featureSelection.autoRoadPositionDetectRate = 4;
        theMap.RegisterForMapCameraCallback(this);
        theMap.RegisterForGestureCallback(this);
        theMap.RegisterForFeatureSelectionCallback(this);
        theMap.RegisterForMapCallback(this);
        theMap.featureSelection.featureSelectionAllowed = true;
        theMap.mapCamera.SetIncludeJobsInFullExtent(false);
        theMap.mapStyle.ShowScalebar(false);
        //theMap.mapCamera.SetMovementDetectionThreshold(0.0333, false);
        theMap.mapCamera.ConfigureMarkerOffsetDueToSpeed(speedForMaxOffset = 1.0, maxOffsetAtMaxSpeed = -0.5, filterCoeff = 0.25);

        showPathInTargetedExtentSwitch.isChecked = theMap.mapCamera.GetShowPathInTargetedExtentMode();

    }

    override fun MapLoadedFromMapRendererKotlin()
    {
        if (mapInitiallyLoaded == false)
        {
            finishSetup();
        }
        mapInitiallyLoaded = true;
    }

    private  fun finishSetup()
    {
        DrawMapKey();
        //theMap.mapCamera.SetExtentType(ExtentType.FullExtent);
    }

    // If we are not using the lifecycle Plugin then manually stop the instance
    override fun onStop() {
        super.onStop()
        theMap.DisposeOfInstance();
    }

    val onLightButton = View.OnClickListener {
        theMap.mapStyle.LoadStyle(theMap.mapStyle.MapStyleSmartSuiteLight)
        //var readBackStyleUir = theMap.mapStyle.styleUri
    }

    val onDarkButton = View.OnClickListener {
        theMap.mapStyle.LoadStyle(theMap.mapStyle.MapStyleSmartSuiteDark)
        //var readBackStyleUir = theMap.mapStyle.styleUri
    }

    val onRestartButton = View.OnClickListener {
        val points = listOf<point>(point(0,0, 0.0, 0.0), point(1,0,90.0, 1.0), point(2,0,90.0, 1.0), point(3,0,90.0, 1.0), point(4,0,90.0, 1.0), point(4,1,0.0, 1.0), point(4,2,0.0, 1.0), point(4,3,0.0, 1.0),point(4,4,0.0, 1.0), point(3,4,270.0, 1.0), point(2,4,270.0, 1.0), point(1,4,270.0, 1.0), point(0,4,270.0, 1.0), point(0,3,180.0, 1.0), point(0,2,180.0, 1.0), point(0,1,180.0, 1.0), point(0,0,180.0, 1.0), point(0,0,180.0, 0.0));

        demoCount = 0;
        var refreshRate: Long;
        if (hammerItSwitch.isChecked)
        {
            refreshRate = 100;
        }
        else
        {
            refreshRate = 1000;
        }
        if (timer != null)
        {
            timer!!.cancel();
            timer = null;
        }
        timer = Timer().scheduleAtFixedRate(0, refreshRate)
        {
            if (demoCount == 0)
            {
                this@MainActivity.runOnUiThread(java.lang.Runnable
                {
                    theMap.locationMarker.SetLocationMarkerType(LocationMarkerType.LocationWithDirection);
                    //theMap.mapCamera.SetMapCameraPadding(MapPadding(0.25, 0.0, 0.0, 0.0, 1000));
                });
            }
            else if (demoCount == 5)
            {
                Handler(Looper.getMainLooper()).post(
                    {
                        theMap.locationMarker.ShowLocationMarkerCrumbTrail(true, 5, true);
                    });
            }
            else if (demoCount == 9)
            {
                Handler(Looper.getMainLooper()).post(
                {
                    var manoeuvreUpTo = ManoeuvreSegmentProperty();
                    manoeuvreUpTo.RouteOrRoadSegmentGuid = UUID.fromString("E4CF077A-F5A9-4AB2-9B44-4869F297C74B");
                    manoeuvreUpTo.ManoeuvreRouteSegment = ManoeuvreSegmentProperty.ManoeuvreHighlightType.None;
                    var serviced = RouteRoadSegmentServicedProperty();
                    serviced.RouteOrRoadSegmentGuid = UUID.fromString("E4CF077A-F5A9-4AB2-9B44-4869F297C74B");
                    serviced.Serviced = true;
                    var manoeuvreAfter = ManoeuvreSegmentProperty();
                    manoeuvreAfter.RouteOrRoadSegmentGuid = UUID.fromString("50A0B5CD-0870-415B-BA5A-BEECCAC78F26");
                    manoeuvreAfter.ManoeuvreRouteSegment = ManoeuvreSegmentProperty.ManoeuvreHighlightType.Upto;
                    // Instead of doing these in one call we'll do them separately as often with our Mobile App there will be separate calls in quick succession.
                    //theMap.mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf<RoadSegmentPropertyBase>(manoeuvreUpTo, manoeuvreAfter, serviced));

                    theMap.mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf<RoadSegmentPropertyBase>(manoeuvreUpTo));
                    //Thread.sleep(1000, 100000)
                    theMap.mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf<RoadSegmentPropertyBase>(manoeuvreAfter));
                    theMap.mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf<RoadSegmentPropertyBase>(serviced));

                    val servicePointPropertyChanges = mutableListOf<ServicePointAndTradeSitePropertyBase>();
                    val reportedChange = ServicePointServicedProperty();
                    reportedChange.ServicePointAndTradeSiteGuid = UUID.fromString("9D098B03-A6D2-4622-83B7-430551538629");
                    reportedChange.Serviced = false;
                    servicePointPropertyChanges.add(reportedChange);
                    theMap.mapStyle.servicePointAndTradeSiteLayer.ChangeServicePointAndTradeSiteProperties(servicePointPropertyChanges);
                });

            }
            else if (demoCount == 13)
            {
                Handler(Looper.getMainLooper()).post(
                    {
                        theMap.locationMarker.ShowLocationMarkerCrumbTrail(false);
                        //theMap.mapStyle.roadSegmentLayer.SetRoadSegmentOpacity(0.25);
                        //theMap.mapStyle.servicePointAndTradeSiteLayer.SetServicePointOpacity(0.25);
                    });
            }
            else if (demoCount == points.count() - 1)
            {

            }

            if (demoCount < points.count())
            {
                moveToNextPosition(points[demoCount]);
                demoCount++;
            }
            else if (demoCount == points.count())
            {
                Handler(Looper.getMainLooper()).post(
                {
                    var manoeuvreUpTo = ManoeuvreSegmentProperty();
                    manoeuvreUpTo.RouteOrRoadSegmentGuid = UUID.fromString("E4CF077A-F5A9-4AB2-9B44-4869F297C74B");
                    manoeuvreUpTo.ManoeuvreRouteSegment = ManoeuvreSegmentProperty.ManoeuvreHighlightType.Upto;
                    var serviced = RouteRoadSegmentServicedProperty();
                    serviced.RouteOrRoadSegmentGuid = UUID.fromString("E4CF077A-F5A9-4AB2-9B44-4869F297C74B");
                    serviced.Serviced = false;
                    var manoeuvreAfter = ManoeuvreSegmentProperty();
                    manoeuvreAfter.RouteOrRoadSegmentGuid = UUID.fromString("50A0B5CD-0870-415B-BA5A-BEECCAC78F26");
                    manoeuvreAfter.ManoeuvreRouteSegment = ManoeuvreSegmentProperty.ManoeuvreHighlightType.After;
                    theMap.mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf<RoadSegmentPropertyBase>(manoeuvreUpTo, manoeuvreAfter, serviced));

                    val servicePointPropertyChanges = mutableListOf<ServicePointAndTradeSitePropertyBase>();
                    val reportedChange = ServicePointServicedProperty();
                    reportedChange.ServicePointAndTradeSiteGuid = UUID.fromString("9D098B03-A6D2-4622-83B7-430551538629");
                    reportedChange.Serviced = true;
                    servicePointPropertyChanges.add(reportedChange);
                    theMap.mapStyle.servicePointAndTradeSiteLayer.ChangeServicePointAndTradeSiteProperties(servicePointPropertyChanges);

                    //theMap.mapStyle.roadSegmentLayer.SetRoadSegmentOpacity(1.0);
                    //theMap.mapStyle.servicePointAndTradeSiteLayer.SetServicePointOpacity(1.0);
                    theMap.locationMarker.SetLocationMarkerType(LocationMarkerType.LocationSearching);
                });

                if (hammerItSwitch.isChecked) {
                    demoCount = 0;
                }
                else
                {
                    timer?.cancel();
                }
            }
        }

    }

    val onMultiFeatureButton = View.OnClickListener {
        var multiDialogue = MultiFeatureCreationDialogueFragment(1000, 1000);
        multiDialogue.show(supportFragmentManager, "multifeatures");
    }

    val onFeaturesTappedButton = View.OnClickListener {
        var dialogue: SelectedFeaturesDialogueFragment = SelectedFeaturesDialogueFragment(theMap.featureSelection.tappedMapFeatures, "Tapped Features:");
        dialogue.show(supportFragmentManager, "tappedfeatures");
    }

    val onClearSelectionButton = View.OnClickListener {
        theMap.featureSelection.ClearSelectedMapFeatures();
        //theMap.mapStyle.roadSegmentLayer.SetRoadSegmentOpacity(0.25);
        //theMap.mapStyle.servicePointAndTradeSiteLayer.SetServicePointOpacity(0.25);
    }
    val onFeaturesSelectedButton = View.OnClickListener {
        var dialogue: SelectedFeaturesDialogueFragment = SelectedFeaturesDialogueFragment(theMap.featureSelection.selectedMapFeatures, "Selected Features:", theMap.featureSelection.selectedMapFeature, true);
        dialogue.show(supportFragmentManager, "selectedfeatures");
    }

    val onGetButton = View.OnClickListener {
        var mapCameraPosition = theMap.mapCamera.GetMapCameraPosition();
        UpdateCameraPositionDisplay(mapCameraPosition);
    }

    private fun UpdateCameraPositionDisplay(mapCameraPosition: MapCameraPosition)
    {
        longitude.setText(String.format("%.6f", mapCameraPosition.Position.Longitude));
        latitude.setText(String.format("%.6f", mapCameraPosition.Position.Latitude));
        if (mapCameraPosition.Zoom != null)
        {
            zoom.setText(mapCameraPosition.Zoom!!.Value.toFloat().toString());
            Log.v(TAG,"Camera zoom now indicated as ${mapCameraPosition.Zoom!!.Value}");
        }
        if (mapCameraPosition.Bearing != null)
        {
            rotation.setText(mapCameraPosition.Bearing!!.Value.toFloat().toString());
        }
        if (mapCameraPosition.Tilt != null)
        {
            pitch.setText(mapCameraPosition.Tilt!!.Value.toFloat().toString());
        }
    }

    public override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (buttonView.id == R.id.switchPadding)
        {
            if (isChecked)
            {
                theMap.mapCamera.SetMapCameraPaddingInPixels(MapPadding(0.25 * theMap.width, 0.0, 0.0, 0.0));
            }
            else
            {
                theMap.mapCamera.SetMapCameraPaddingInPixels(MapPadding(0.0, 0.0, 0.0, 0.0));
            }
        }
        else if (buttonView.id == R.id.switchUseBearing)
        {
            if (isChecked)
            {
                theMap.mapCamera.SetOrientateToBearing(true);
            }
            else
            {
                theMap.mapCamera.SetOrientateToBearing(false);
            }
        }
        else if (buttonView.id == R.id.switchExtentPadding)
        {
            if (isChecked)
            {
                theMap.mapCamera.SetDefaultExtentPadding(0.1);
            }
            else
            {
                theMap.mapCamera.SetDefaultExtentPadding(0.00001);
            }
        }
        else if (buttonView.id == R.id.switchMultipleSelection)
        {
            if (isChecked)
            {
                theMap.featureSelection.MultipleSelectionAllowed = true;
            }
            else
            {
                theMap.featureSelection.MultipleSelectionAllowed = false;
            }
        }
        else if (buttonView.id == R.id.switchShowPath)
        {
            if (isChecked)
            {
                theMap.mapCamera.SetShowPathToTargetFeature(true);
            }
            else
            {
                theMap.mapCamera.SetShowPathToTargetFeature(false);
            }
        }
        else if (buttonView.id == R.id.switchShowPathInTargetedExtent)
        {
            if (isChecked)
            {
                theMap.mapCamera.SetShowPathInTargetedExtentMode(true);
            }
            else
            {
                theMap.mapCamera.SetShowPathInTargetedExtentMode(false);
            }
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long)
    {
        Log.d(TAG, "Item selected ${p2}.");
        if (p0?.id == R.id.spinnerExtent)
        {
            if (p2 == ExtentType.None.ordinal)
            {
                theMap.mapCamera.SetExtentType(ExtentType.None);
            }
            else if (p2 == ExtentType.CentreOnLocation.ordinal)
            {
                theMap.mapCamera.SetExtentType(ExtentType.CentreOnLocation, MoveCameraTypeEnum.Animate);
            }
            else if (p2 == ExtentType.CentreOnSelectedFeatures.ordinal)
            {
                theMap.mapCamera.SetExtentType(ExtentType.CentreOnSelectedFeatures);
            }
            else if (p2 == ExtentType.FullExtent.ordinal)
            {
                theMap.mapCamera.SetExtentType(ExtentType.FullExtent, MoveCameraTypeEnum.Move);
            }
            else if (p2 == ExtentType.FullExtentAndLocation.ordinal)
            {
                theMap.mapCamera.SetExtentType(ExtentType.FullExtentAndLocation, MoveCameraTypeEnum.Move);
            }
            else if (p2 == ExtentType.TargetedExtent.ordinal)
            {
                theMap.mapCamera.SetExtentType(ExtentType.TargetedExtent);
            }
            else if (p2 == ExtentType.TargetedExtentAndLocation.ordinal)
            {
                theMap.mapCamera.SetExtentType(ExtentType.TargetedExtentAndLocation);
            }
            else
            {

            }
        }
        if (p0?.id == R.id.spinnerTargetedFeature)
        {
            var target = p0.getItemAtPosition(p2);
            if (target == "None")
            {
                theMap.mapCamera.SetTargetedExtentInclusions(listOf<MapFeature>());
                theMap.featureSelection.ClearSelectedMapFeatures();
            }
            else if (target == "Service Point")
            {
                theMap.mapCamera.SetTargetedExtentInclusions(listOf<MapFeature>(targetedServicePointAndTradeSiteFeature));
                theMap.featureSelection.ToggleSelectedMapFeature(targetedServicePointAndTradeSiteFeature);
            }
            else if (target == "Job")
            {
                theMap.mapCamera.SetTargetedExtentInclusions(listOf<MapFeature>(targetedJobFeature));
                theMap.featureSelection.ToggleSelectedMapFeature(targetedJobFeature);
            }
            else if (target == "PoI")
            {
                theMap.mapCamera.SetTargetedExtentInclusions(listOf<MapFeature>(targetedPoIFeature));
                theMap.featureSelection.ToggleSelectedMapFeature(targetedPoIFeature);
            }
            else if (target == "Route Segment")
            {
                theMap.mapCamera.SetTargetedExtentInclusions(listOf<MapFeature>(targetedRouteSegmentFeature));
                theMap.featureSelection.ToggleSelectedMapFeature(targetedRouteSegmentFeature);
            }
            else if (target == "Road Comment")
            {
                theMap.mapCamera.SetTargetedExtentInclusions(listOf<MapFeature>(targetedRoadSegmentFeature));
                theMap.featureSelection.ToggleSelectedMapFeature(targetedRoadSegmentFeature);
            }
            else if (target == "PoI and Road Comment")
            {
                theMap.featureSelection.ClearSelectedMapFeatures();
                theMap.mapCamera.SetTargetedExtentInclusions(listOf<MapFeature>(targetedPoIFeature, targetedRoadSegmentFeature));
            }
            else if (target == "A2B Navigation")
            {
                theMap.featureSelection.ClearSelectedMapFeatures();
                theMap.mapCamera.SetTargetedExtentInclusions(listOf<MapFeature>(a2BNavigationCompleted, a2BNavigationBefore, a2BNavigationAfter));
            }
            else
            {

            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?)
    {
        Log.d(TAG, "Nothing selected.")
    }

    private fun DrawMapKey()
    {
        try
        {
            drawMainRoadSegments();
            drawMultiPassSegments();
            //drawAdditionalRoadSegments();
            //DrawRoadSegmentWithCommentCombinationsGivingProblems()
            //DrawProblematicRoadSegment();
            drawServicePointsAndTradeSites();
            drawJobs();
            drawSymbols();
            drawSuperimposedFeatures();
            drawLocationMarkers();
            drawReportsBySide();

            //theMap.mapCamera.SetExtentType(ExtentType.FullExtent);
        }
        catch (ex: Exception)
        {
            Log.e(TAG, "Exception = ${ex.message}")
        }
    }

    private fun DrawRoadSegmentWithCommentCombinationsGivingProblems()
    {
        // Top right
        // This is the one we want to show as selected as the route road segment and route road non-service segment refer to this one.
        roadSegmentTest.AddTestRoadSegment(UniqueId = UUID.fromString("853DF7C8-BA77-4636-9D8D-38C8B8A252D9"), -2, 9, vertical = false, note = "A Note", length = 12, text = "Road Segment with comment.");

        // This one used to appear first in list of selected features due to the lower Guid.
        roadSegmentTest.AddTestRoadSegment(UniqueId = UUID.fromString("4036F829-9706-447B-A975-879DD41CBBF4"), -2, 9, vertical = false, note = "A Note", length = 12, text = "Another Road Segment with comment.");


        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("FC4420C8-B0B5-4392-BAAC-308206DCED35"), RoadId = UUID.fromString("853DF7C8-BA77-4636-9D8D-38C8B8A252D9"), -2, 9, 1, DirectionOfTravel.With, 0, 0, serviced = false, vertical = false, length = 12, text = "Unserviced.");


        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("7C21FC34-41DD-4BC7-97E4-D6EC23B4E7A1"), RoadId = UUID.fromString("853DF7C8-BA77-4636-9D8D-38C8B8A252D9"), -2, 9, 1, DirectionOfTravel.With, length = 12, vertical = false, text = "Non-Service.", reported = false);


        //Top middle
        roadSegmentTest.AddTestRoadSegment(UniqueId = UUID.fromString("BA7C6C08-8F1B-49E4-8922-53DF6667DAB7"), -16, 9, vertical = false, note = "A Note", length = 12, text = "Road Segment with comment.", numberIntermediatePoints = 5);

        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("BE92CE36-4A7C-4535-ABA6-8768662D3187"), RoadId = UUID.fromString("B791468B-E4ED-475C-8DA4-981AB10D269A"), -16, 9, 1, DirectionOfTravel.With, length = 6, vertical = false, text = "Non-Service A.", reported = false, numberIntermediatePoints = 2); // Note RoadId is non-existent

        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("ACF7208E-A883-460B-A02F-EAA9AE8122F0"), RoadId = UUID.fromString("B791468B-E4ED-475C-8DA4-981AB10D269A"), -10, 9, 1, DirectionOfTravel.Against, length = 6, vertical = false, text = "Non-Service B.", reported = false, numberIntermediatePoints = 2); // Note RoadId is non-existent

        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("E4F0121A-0526-4F3C-8440-C759BBCD9422"), RoadId = UUID.fromString("B791468B-E4ED-475C-8DA4-981AB10D269A"), -10, 9, 1, DirectionOfTravel.Against, length = 6, vertical = true, text = "Non-Service Vert.", reported = false, numberIntermediatePoints = 2); // Note RoadId is non-existent


        //Top left
        roadSegmentTest.AddTestRoadSegment(UniqueId = UUID.fromString("40D072E5-BB75-414C-9AAA-4CEF11C6072F"), -30, 9, vertical = false, note = "A Note", length = 6, text = "Road Segment with comment A.", numberIntermediatePoints = 2);

        roadSegmentTest.AddTestRoadSegment(UniqueId = UUID.fromString("007487DA-769A-4F25-B4AA-CD030CC328F0"), -24, 9, vertical = false, note = "A Note", length = 6, text = "Road Segment with comment B.", numberIntermediatePoints = 2);

        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("3C2277FD-3418-4E60-8707-5B9D1E5569DC"), RoadId = UUID.fromString("B791468B-E4ED-475C-8DA4-981AB10D269A"), -30, 9, 1, DirectionOfTravel.With, length = 12, vertical = false, text = "Non-Service.", reported = false, numberIntermediatePoints = 5); // Note RoadId is non-existent

        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("3F2F0046-0A6C-4A77-9BCC-EAC6C99E5B1D"), RoadId = UUID.fromString("B791468B-E4ED-475C-8DA4-981AB10D269A"), -24, 9, 1, DirectionOfTravel.Against, length = 6, vertical = true, text = "Non-Service Vert.", reported = false, numberIntermediatePoints = 2); // Note RoadId is non-existent
    }
    private fun drawMainRoadSegments()
    {
        //theMap.mapStyle.roadSegmentLayer.SetSideOfRoad(driveOnLeft = true, driveOnRight = false);

        // Draw 12 road segments that will remain on the map (note that some are achieved by overlaying 2 segments on the same location).
        targetedRouteSegmentFeature = roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("3A914834-9F7D-40A2-8925-70526A2C0797"), RoadId = UUID.fromString("4A2FB290-C817-4E2B-818E-614298BB14FC"), -2, 5, 1, DirectionOfTravel.With, 0, 0, serviced = false, vertical = false, length = 6, text = "Unserviced.", manoeuvreAtEnd = ManoeuvreType.SharpLeft);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("ADA64B41-E19C-4161-8DFE-7AFBE63719F0"), RoadId = UUID.fromString("315BE244-102B-4DB6-B22D-B1E70C68B803"), -2, 3, 1, DirectionOfTravel.With, 1, 1, serviced = true, vertical = false, length = 6, text = "Serviced.");
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("2BA1E26D-4A1A-4A59-9CCA-E73BA539971B"), RoadId = UUID.fromString("CB59951E-7044-4891-B429-26E419A065CC"), -2, 1, 1, DirectionOfTravel.With, 1, 1, serviced = false, vertical = false, length = 6, text = "Comments.", note = "", serviceNote = "A Service Note");
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("A00CCC2F-21C1-495C-B6EC-14C79BBE8832"), RoadId = UUID.fromString("0D085326-419E-4375-9D04-F591BE2A2CF9"), -2, -1, 1, DirectionOfTravel.With, 1, 1, serviced = false, vertical = false, length = 6, text = "Reported.", reported = true);

        // 2 segments on same location.
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("969C4EDB-1E91-4F19-A459-9909A03EE303"), RoadId = UUID.fromString("58A0023E-8A8B-4417-9E9F-CE47DE934247"), 8, 5, 1, DirectionOfTravel.With, 0, 0, serviced = false, vertical = false, length = 6,  text = "Unserviced in either direction.");
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("7342B77A-61A0-4280-AFFB-A84B08327B65"), RoadId = UUID.fromString("58A0023E-8A8B-4417-9E9F-CE47DE934247"), 8, 5, 2, DirectionOfTravel.Against, 0, 0, serviced = false, vertical = false, length = 6);

        // 2 segments on same location.
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("60D2C502-A195-4CC0-A1F6-58C909B4522E"), RoadId = UUID.fromString("3E136767-83BD-4960-9C02-3415C932638A"), 8, 3, 1, DirectionOfTravel.With, 0, 0, serviced = false, vertical = false, length = 6, text = "Unserviced in one direction.");
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("316ADCF8-6BA3-414A-9112-2DA18CE19767"), RoadId = UUID.fromString("3E136767-83BD-4960-9C02-3415C932638A"), 8, 3, 2, DirectionOfTravel.Against, 1, 2, serviced = true, vertical = false, length = 6);

        // 2 segments on same location.
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("7D21B5E8-B378-4C61-87D0-F248FE857ACF"), RoadId = UUID.fromString("01A02102-6601-49A5-BC2B-3AF697D3EE42"), 8, 1, 1, DirectionOfTravel.With, 2, 2, serviced = true, vertical = false, length = 6, text = "Fully serviced.");
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("A85D2919-0A82-4488-A0EF-AB8E67B9EA00"), RoadId = UUID.fromString("01A02102-6601-49A5-BC2B-3AF697D3EE42"), 8, 1, 2, DirectionOfTravel.Against, 1, 2, serviced = true, vertical = false, length = 6);

        // This road segment should be marked as being serviced (i.e. a red highlight) but we will change this status later.
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("1CC5A977-B898-4BDD-8496-C9BEE5ADF473"), RoadId = UUID.fromString("9BB21B41-C9C6-451E-AB10-F65997168EC8"), 8, -1, 1, DirectionOfTravel.With, 0, 0, serviced = false, vertical = false, length = 6, text = "Currently being serviced.");

        roadSegmentTest.AddTestRoadSegment(UniqueId = UUID.fromString("41AAF753-A558-4902-98C3-4C8122451F69"), -2, -3, vertical = false, note = "A Note", length = 6, text = "Non-Route with comments.");

        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("7C21FC34-41DD-4BC7-97E4-D6EC23B4E7A1"), RoadId = UUID.fromString("5D9F3F77-1A87-4288-910C-DEAE9ED6DC2A"), 8, -3, 1, DirectionOfTravel.With, length = 6, vertical = false, text = "Non-Service.", reported = false, manoeuvreAtEnd = ManoeuvreType.SharpRight);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("61F5E08F-9DF4-4167-9483-41334C04B5E0"), RoadId = UUID.fromString("E63363BA-8806-4260-9270-F9CC85B46FA3"), -2, -5, 1, DirectionOfTravel.With, length = 6, vertical = false, text = "Non-Service with comments.", serviceNote = "A Service Note", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("8DB63EB2-8CC6-4F86-8D93-71DB9BF1657C"), RoadId = UUID.fromString("815FD4A5-0001-418C-A09E-4E9AD382540A"), 8, -5, 1, DirectionOfTravel.With, length = 6, vertical = false, text = "Non-Service reported.", reported = true);

        // Draw an additional road segment that will be removed to test the remove road segments method.
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("9CC17406-F230-4221-9AB4-76AF51EFD060"), RoadId = UUID.fromString("467AF9BE-E30C-4FE7-A61C-E344F14AED38"), 16, 5, 1, DirectionOfTravel.With, 0, 0, serviced = false, vertical = false, length = 6, text = "Should not be visible.");

        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("E4CF077A-F5A9-4AB2-9B44-4869F297C74B"), RoadId = UUID.fromString("8286E4B9-1CA5-4A06-AD3B-BC7E5B8C6002"), -6, -27, 1, DirectionOfTravel.With, 0, 0, serviced = false, vertical = false, length = 6, text = "Manoeuvre upto.");
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("50A0B5CD-0870-415B-BA5A-BEECCAC78F26"), RoadId = UUID.fromString("03C18248-6F4B-462B-A0D6-3ADF64EA35D7"), 0, -27, 1, DirectionOfTravel.With, 0, 0, serviced = false, vertical = false, length = 6, text = "Manoeuvre after.");

        a2BNavigationCompleted = roadSegmentTest.AddTestNavigationSegment(UniqueId = UUID.fromString("C2882FEF-E8B7-4611-8A15-186D4CE51CA6"), RoadId = UUID.fromString("68E095F2-0708-4946-B2AD-04179F8EBC74"), -15, -30, 1, DirectionOfTravel.With, length = 6, text = "A2B complete.", completed = false, vertical = false); // Will use a property update to set this as completed lower down.

        a2BNavigationBefore = roadSegmentTest.AddTestNavigationSegment(UniqueId = UUID.fromString("20D150BB-7362-465C-A7D8-E5AE907A35B8"), RoadId = UUID.fromString("5045869D-D38B-481D-A73F-F801DFCB8A4C"), -9, -30, 1, DirectionOfTravel.With, length = 6, text = "A2B upto..", completed = false, vertical = false);

        a2BNavigationAfter = roadSegmentTest.AddTestNavigationSegment(UniqueId = UUID.fromString("F2BEFA52-7E50-4075-8513-15FAC0DACBE2"), RoadId = UUID.fromString("57C704A2-A4AD-4801-8199-7F8CA8090843"), -3, -30, 1, DirectionOfTravel.With, length = 6, text = "A2B after.", completed = false, vertical = false);

        // Remove the additional road segment.
        val segmentsToRemove = mutableListOf<RoadSegmentFeature>();
        var roadSegmentFeature = RoadSegmentFeature();
        roadSegmentFeature.UniqueGuid = UUID.fromString("9CC17406-F230-4221-9AB4-76AF51EFD060");
        segmentsToRemove.add(roadSegmentFeature);
        if (ConditionalCompileUI.showHiddenItemsForScreenshot == false)
        {
            theMap.mapStyle.roadSegmentLayer.RemoveRoadSegments(segmentsToRemove);
        }

        // Set the red highlight on this road segment.
        var snappedPropertyChange = RouteRoadSegmentNonServiceSnappedProperty();
        snappedPropertyChange.RouteOrRoadSegmentGuid = UUID.fromString("1CC5A977-B898-4BDD-8496-C9BEE5ADF473");
        snappedPropertyChange.Snapped = true;
        theMap.mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf<RoadSegmentPropertyBase>(snappedPropertyChange));

        var manoeuvreUpTo = ManoeuvreSegmentProperty();
        manoeuvreUpTo.RouteOrRoadSegmentGuid = UUID.fromString("E4CF077A-F5A9-4AB2-9B44-4869F297C74B");
        manoeuvreUpTo.ManoeuvreRouteSegment = ManoeuvreSegmentProperty.ManoeuvreHighlightType.Upto;
        theMap.mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf<RoadSegmentPropertyBase>(manoeuvreUpTo));
        var manoeuvreAfter = ManoeuvreSegmentProperty();
        manoeuvreAfter.RouteOrRoadSegmentGuid = UUID.fromString("50A0B5CD-0870-415B-BA5A-BEECCAC78F26");
        manoeuvreAfter.ManoeuvreRouteSegment = ManoeuvreSegmentProperty.ManoeuvreHighlightType.After;
        theMap.mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf<RoadSegmentPropertyBase>(manoeuvreAfter));

        targetedRoadSegmentFeature = roadSegmentTest.AddTestRoadSegment(UniqueId = UUID.fromString("DC6C4A1C-2807-4E11-86A6-19D5C4F4A41E"), -62, -10, vertical = true, note = "A Note", length = 6, text = "Targeted Road Segment");

        var a2BNavigateAfter = A2BNavigationSegmentStageProperty();
        a2BNavigateAfter.RouteOrRoadSegmentGuid = UUID.fromString("F2BEFA52-7E50-4075-8513-15FAC0DACBE2");
        a2BNavigateAfter.A2BNavigationSegment = A2BNavigationSegmentStageProperty.A2BNavigationStage.After;
        theMap.mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf<RoadSegmentPropertyBase>(a2BNavigateAfter));

        var a2BNavigateComplete = A2BNavigationSegmentCompletedProperty();
        a2BNavigateComplete.RouteOrRoadSegmentGuid = UUID.fromString("C2882FEF-E8B7-4611-8A15-186D4CE51CA6");
        a2BNavigateComplete.Completed = true;
        theMap.mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf(a2BNavigateComplete));

        // This line always works correctly whereas chaining properties one at a time as done above does not always.
        //theMap.mapStyle.roadSegmentLayer.ChangeRoadSegmentProperties(listOf(manoeuvreUpTo, manoeuvreAfter, a2BNavigateAfter, a2BNavigateComplete));

        if (ConditionalCompileUI.renderSegmentsBehindA2B == true)
        {
            roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("9719455C-3CDB-4045-A022-E9CC01BEEA8E"), RoadId = UUID.fromString("A7E50954-C1E7-4646-B86E-9B80ECF6B5FC"), -15, -30, 1, DirectionOfTravel.With, 0, 0, serviced = false, vertical = false, length = 18, text = "", serviceNote = "Test");
        }

    }

    private fun drawMultiPassSegments()
    {
        //Draw additional road segments that consist of multi-pass serviceable segments in either direction, some combined with non-service segments

        // First one is two passes in each direction, all un-serviced (and a non-service segment thrown in).
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("92CE4B83-A0F7-4B2B-A5E0-6D690FC3BB6D"), RoadId = UUID.fromString("6B571C21-4DCE-4FDC-9ECC-D64073B3FF34"), -14, 5, 1, DirectionOfTravel.With, 0, 4, serviced = false, length = 6, vertical = false, text = "1. Multi-pass both directions (and non-service left)", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("A171C57C-69CC-45F7-BF87-247F86C15A20"), RoadId = UUID.fromString("6B571C21-4DCE-4FDC-9ECC-D64073B3FF34"), -14, 5, 2, DirectionOfTravel.With, 0, 4, serviced = false, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("DB6D392C-D748-4E72-B8A3-4D7474FE7A67"), RoadId = UUID.fromString("6B571C21-4DCE-4FDC-9ECC-D64073B3FF34"), -14, 5, 3, DirectionOfTravel.Against, 0, 4, serviced = false, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("173CCE02-3D69-48F5-8EA2-65EC842A28F1"), RoadId = UUID.fromString("6B571C21-4DCE-4FDC-9ECC-D64073B3FF34"), -14, 5, 4, DirectionOfTravel.Against, 0, 4, serviced = false, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("A6BF0675-0764-4218-91D5-A0454525957B"), RoadId = UUID.fromString("6B571C21-4DCE-4FDC-9ECC-D64073B3FF34"), -14, 5, 5, DirectionOfTravel.With, length = 6, vertical = false, "", reported = false);

        // Second one is two passes in each direction, one serviced (and a non-service segment thrown in).
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("6412AD83-0BD8-4429-A42D-682A1984C908"), RoadId = UUID.fromString("DDFA443D-EE4B-45FD-B01D-854DBA3922C2"), -14, 2, 1, DirectionOfTravel.With, 1, 4, serviced = true, length = 6, vertical = false, text = "2. Multi-pass both directions, one serviced right (and non-service left)", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("D30277F3-4406-4249-9B8B-1695BC2EEFA3"), RoadId = UUID.fromString("DDFA443D-EE4B-45FD-B01D-854DBA3922C2"), -14, 2, 2, DirectionOfTravel.With, 0, 4, serviced = false, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("A99DADA8-98A9-410B-9E3F-7B985CAC6669"), RoadId = UUID.fromString("DDFA443D-EE4B-45FD-B01D-854DBA3922C2"), -14, 2, 3, DirectionOfTravel.Against, 0, 4, serviced = false, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("7888BD7D-A7C5-412B-B4FD-EB11511F7F54"), RoadId = UUID.fromString("DDFA443D-EE4B-45FD-B01D-854DBA3922C2"), -14, 2, 4, DirectionOfTravel.Against, 0, 4, serviced = false, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("1E73C565-D8C9-422D-AABE-EA2FE1CE7D0D"), RoadId = UUID.fromString("DDFA443D-EE4B-45FD-B01D-854DBA3922C2"), -14, 2, 5, DirectionOfTravel.With, length = 6, vertical = false, "", reported = false);

        // Third one is two passes in each direction, one direction fully serviced (and a non-service segment thrown in).
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("386AF1F5-9C3B-4363-B8A6-99D006DDBF18"), RoadId = UUID.fromString("360ABBF9-DCB3-48C8-B5F3-E6F467145548"), -14, -1, 1, DirectionOfTravel.With, 1, 4, serviced = true, length = 6, vertical = false, text = "3. Multi-pass both directions, fully serviced right (and non-service left)", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("B78F3D1A-A352-4016-8302-0161FFE8809A"), RoadId = UUID.fromString("360ABBF9-DCB3-48C8-B5F3-E6F467145548"), -14, -1, 2, DirectionOfTravel.With, 2, 4, serviced = true, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("35400F20-040D-4D19-861C-755AC900ECAA"), RoadId = UUID.fromString("360ABBF9-DCB3-48C8-B5F3-E6F467145548"), -14, -1, 3, DirectionOfTravel.Against, 0, 4, serviced = false, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("BE397439-42FE-46C4-8BB4-4B2D803AAA21"), RoadId = UUID.fromString("360ABBF9-DCB3-48C8-B5F3-E6F467145548"), -14, -1, 4, DirectionOfTravel.Against, 0, 4, serviced = false, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("587B3880-6D02-409E-A0F0-8E3C043CFACE"), RoadId = UUID.fromString("360ABBF9-DCB3-48C8-B5F3-E6F467145548"), -14, -1, 5, DirectionOfTravel.With, length = 6, vertical = false, "", reported = false);

        // Fourth one is two passes in each direction, both directions fully serviced (and a non-service segment thrown in).
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("E5FDBB30-8EB2-4BAA-A3AF-C1573E236C3D"), RoadId = UUID.fromString("9116883B-526D-4705-B409-4D68877B101D"), -14, -4, 1, DirectionOfTravel.With, 1, 4, serviced = true, length = 6, vertical = false, text = "4. Multi-pass both directions, fully serviced (and non-service left)", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("0A533956-8B58-4E8E-B486-DB63E2315C6F"), RoadId = UUID.fromString("9116883B-526D-4705-B409-4D68877B101D"), -14, -4, 2, DirectionOfTravel.With, 2, 4, serviced = true, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("42B15BBB-8A80-4A58-AEA2-8ECC29499BCA"), RoadId = UUID.fromString("9116883B-526D-4705-B409-4D68877B101D"), -14, -4, 3, DirectionOfTravel.Against, 3, 4, serviced = true, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("E072E05F-3A87-4CCD-8CEE-3F5A19FC77EC"), RoadId = UUID.fromString("9116883B-526D-4705-B409-4D68877B101D"), -14, -4, 4, DirectionOfTravel.Against, 4, 4, serviced = true, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("6DDC3D3F-0A0F-413E-AACC-4327CE30E030"), RoadId = UUID.fromString("9116883B-526D-4705-B409-4D68877B101D"), -14, -4, 5, DirectionOfTravel.With, length = 6, vertical = false, "", reported = false);

    }

    private fun drawReportsBySide()
    {
        theMap.mapStyle.roadSegmentLayer.SetSideOfRoad(driveOnLeft = true, driveOnRight = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("66CB34CE-EC08-4235-8C07-0C417D21D486"), RoadId = UUID.fromString("FEA58DB6-74BF-480A-B8AB-4C9A6DFF025E"), -14, -7, 1, DirectionOfTravel.With, 1, 4, serviced = true, length = 6, vertical = false, text = "5. Report on left side", reported = true);

        theMap.mapStyle.roadSegmentLayer.SetSideOfRoad(driveOnLeft = false, driveOnRight = true);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("0362400E-E6E5-4709-8909-423BAB2156DC"), RoadId = UUID.fromString("5F276BCA-418B-4463-9808-9108DE14C759"), -14, -10, 1, DirectionOfTravel.With, 1, 4, serviced = true, length = 6, vertical = false, text = "6. Report on right side", reported = true);

        theMap.mapStyle.roadSegmentLayer.SetSideOfRoad(driveOnLeft = false, driveOnRight = true);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("2FA80E70-7732-4801-AF2F-08A2C85839A8"), RoadId = UUID.fromString("6E35B1D5-29A3-447B-81F9-EFDF926C4D6D"), -14, -13, 1, DirectionOfTravel.With, 1, 4, serviced = true, directionOfService = DirectionsOfService.Both, length = 6, vertical = false, text = "7. Report on right side, meander", reported = true);

    }

/*
    private fun drawAdditionalRoadSegments()
    {
        // Draw 12 additional road segments. These are complex situations where multiple segments are shown at the same location
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("64FD2E7A-25CC-45A2-962F-B49B5CCD77A1"), RoadId = UUID.fromString("742FC679-5DFF-4AB5-8572-86825AAE5132"), -14, 5, 1, DirectionOfTravel.With, 0, 0, serviced = false, length = 6, vertical = false, text = "1. Route segment right, non-service left", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("73DCEC4A-E0AB-4E0F-ADED-FB184143389B"), RoadId = UUID.fromString("742FC679-5DFF-4AB5-8572-86825AAE5132"), -14, 5, 2, DirectionOfTravel.Against, length = 6, vertical = false, "", reported = false);

        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("020E8242-B3E7-4C6C-B386-68E573E248B9"), RoadId = UUID.fromString("8B8061C2-BC4F-4481-8FCA-5DE4C46615F0"), -14, 2, 1, DirectionOfTravel.With, 1, 1, serviced = true, length = 6, vertical = false, text = "1a. Serviced to right", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("000FC061-8C3E-4D3F-A0B7-BCC38CDCCCE1"), RoadId = UUID.fromString("8B8061C2-BC4F-4481-8FCA-5DE4C46615F0"), -14, 2, 2, DirectionOfTravel.Against, length = 6, vertical = false, "", reported = false);

        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("60F917DB-9405-4470-B6D9-1FFD22E571CF"), RoadId = UUID.fromString("C416D8DA-039C-4006-8C8A-45F9E45A48CD"), -14, -1, 1, DirectionOfTravel.With, 0, 0, serviced = false, length = 6, vertical = false, text = "2. Route segment right & left, non-service left", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("B50FEC71-C437-4081-8038-142D256DCA33"), RoadId = UUID.fromString("C416D8DA-039C-4006-8C8A-45F9E45A48CD"), -14, -1, 2, DirectionOfTravel.Against, 0, 0, serviced = false, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UUID.fromString("A03C82CC-C0EF-46B1-9582-CACC83542394"), RoadId = UUID.fromString("C416D8DA-039C-4006-8C8A-45F9E45A48CD"), -14, -1, 3, DirectionOfTravel.Against, length = 6, vertical = false, "", reported = false);

        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("E2D02D2C-80CA-425A-8C7E-1D264383525F"), RoadId = UUID.fromString("423E7965-0801-4862-80A3-126BDBB828EC"), -14, -4, 1, DirectionOfTravel.With, 1, 2, serviced = true, length = 6, vertical = false, text = "2a. serviced right", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("22516DAC-9920-40EC-803D-5CB51A15934A"), RoadId = UUID.fromString("423E7965-0801-4862-80A3-126BDBB828EC"), -14, -4, 2, DirectionOfTravel.Against, 0, 0, serviced = false, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("E06219CB-D5B5-4C83-B98A-154671F26EC4"), RoadId = UUID.fromString("423E7965-0801-4862-80A3-126BDBB828EC"), -14, -4, 3, DirectionOfTravel.Against, length = 6, vertical = false, "", reported = false);

        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("995864B0-4ACC-48B7-848B-0FB6C87E0040"), RoadId = UUID.fromString("5C701D6A-6E54-4969-9AC2-709A9D2F23BF"), -14, -7, 1, DirectionOfTravel.With, 1, 2, serviced = true, length = 6, vertical = false, text = "2b. serviced right & left", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("E7B5A100-B018-4279-91DD-948CE10DE55D"), RoadId = UUID.fromString("5C701D6A-6E54-4969-9AC2-709A9D2F23BF"), -14, -7, 2, DirectionOfTravel.Against, 2, 2, serviced = true, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("9D3B0EC1-8670-4AEE-9900-EE5F4EAA27CC"), RoadId = UUID.fromString("5C701D6A-6E54-4969-9AC2-709A9D2F23BF"), -14, -7, 3, DirectionOfTravel.Against, length = 6, vertical = false, "", reported = false);

        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("AD0E29F4-000E-43F5-ADEE-A30AFE107620"), RoadId = UUID.fromString("1206526E-CEA1-4EC9-95EF-A8844258845C"), -14, -10, 1, DirectionOfTravel.With, 0, 0, serviced = false, length = 6, vertical = false, text = "3. Route segment right & left, non-service right & left", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("32124BAF-7340-4949-9A57-CD2E4128A9DA"), RoadId = UUID.fromString("1206526E-CEA1-4EC9-95EF-A8844258845C"), -14, -10, 2, DirectionOfTravel.Against, 0, 0, serviced = false, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("279B3955-ABFF-4A21-89A3-5E9CC485AD47"), RoadId = UUID.fromString("1206526E-CEA1-4EC9-95EF-A8844258845C"), -14, -10, 3, DirectionOfTravel.With, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("C874F542-B685-462B-A7FE-9CD4405F5010"), RoadId = UUID.fromString("1206526E-CEA1-4EC9-95EF-A8844258845C"), -14, -10, 4, DirectionOfTravel.Against, length = 6, vertical = false, text = "", reported = false);

        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("B9E9C80A-079D-4442-BDA6-889E5411628D"), RoadId = UUID.fromString("36AB360E-2955-4F16-B041-56508D6CC6D3"), -14, -13, 1, DirectionOfTravel.With, 1, 2, serviced = true, length = 6, vertical = false, text = "3a. Serviced right", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("701833BD-0A4D-46E7-AE73-4EC4CA16FE44"), RoadId = UUID.fromString("36AB360E-2955-4F16-B041-56508D6CC6D3"), -14, -13, 2, DirectionOfTravel.Against, 0, 0, serviced = false, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("17696A4B-687E-4310-8F9E-A2537A2A85D0"), RoadId = UUID.fromString("36AB360E-2955-4F16-B041-56508D6CC6D3"), -14, -13, 3, DirectionOfTravel.With, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("4D16E2B6-B94E-424F-A311-BE6369E67396"), RoadId = UUID.fromString("36AB360E-2955-4F16-B041-56508D6CC6D3"), -14, -13, 4, DirectionOfTravel.Against, length = 6, vertical = false, text = "", reported = false);

        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("98626ED3-D6BE-4118-B50D-71F305205F8C"), RoadId = UUID.fromString("AB608AD5-D375-4EF1-9FB2-5E55E0189CC8"), -14, -16, 1, DirectionOfTravel.With, 1, 2, serviced = true, length = 6, vertical = false, text = "3b. Serviced right & left", reported = false);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("F4B30D7A-62B7-45C9-986F-56D9ED2B9620"), RoadId = UUID.fromString("AB608AD5-D375-4EF1-9FB2-5E55E0189CC8"), -14, -16, 2, DirectionOfTravel.Against, 2, 2, serviced = true, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("A3543F4B-7EE1-4E01-A3CE-DB0FD562977E"), RoadId = UUID.fromString("AB608AD5-D375-4EF1-9FB2-5E55E0189CC8"), -14, -16, 3, DirectionOfTravel.With, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("A3408801-18B4-4EBB-82C0-6343EC239DF0"), RoadId = UUID.fromString("AB608AD5-D375-4EF1-9FB2-5E55E0189CC8"), -14, -16, 4, DirectionOfTravel.Against, length = 6, vertical = false, text = "", reported = false);

        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("A65CB9A0-C9AB-488F-9DE9-1F6502D00C0A"), RoadId = UUID.fromString("6D350D74-D993-4534-9675-7BD25D6C6B91"), -14, -19, 1, DirectionOfTravel.With, length = 6, vertical = false, "4. non-service both ways", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("A05DFE2A-7788-41B1-859A-560A5DA20731"), RoadId = UUID.fromString("6D350D74-D993-4534-9675-7BD25D6C6B91"), -14, -19, 2, DirectionOfTravel.Against, length = 6, vertical = false, text = "", reported = false);

        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("B0152DBC-6079-4536-9079-DFC24FC60D61"), RoadId = UUID.fromString("9AF6D914-807C-4B10-9D38-89BACD9D1D2F"), -14, -21, 1, DirectionOfTravel.With, 0, 0, serviced = false, length = 6, vertical = false, text = "5. Route segment right, non-service both ways", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("5BB75082-E716-4800-B31F-7821CA14CFA2"), RoadId = UUID.fromString("9AF6D914-807C-4B10-9D38-89BACD9D1D2F"), -14, -21, 2, DirectionOfTravel.Against, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("40E809BA-D1D3-4A0B-B1EF-0B87A09AC69A"), RoadId = UUID.fromString("9AF6D914-807C-4B10-9D38-89BACD9D1D2F"), -14, -21, 3, DirectionOfTravel.With, length = 6, vertical = false, text = "", reported = false);

        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("4AA8A4DD-3F0C-4A46-BF8D-036606631310"), RoadId = UUID.fromString("434B51B6-D6F7-4333-A512-A21B4E408737"), -14, -24, 1, DirectionOfTravel.With, 1, 1, serviced = true, length = 6, vertical = false, text = "5a. Route segment right serviced, non-service both ways", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("E874EE0B-A558-4A13-B92E-7C9784736426"), RoadId = UUID.fromString("434B51B6-D6F7-4333-A512-A21B4E408737"), -14, -24, 2, DirectionOfTravel.Against, length = 6, vertical = false, text = "", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("C7B48714-4F94-4F00-A5D0-E13263DF9511"), RoadId = UUID.fromString("434B51B6-D6F7-4333-A512-A21B4E408737"), -14, -24, 3, DirectionOfTravel.With, length = 6, vertical = false, text = "", reported = false);

        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("D2C921ED-1C0D-4A26-978C-AE6E0E77CB3C"), RoadId = UUID.fromString("9895C8FF-CEDD-42C0-BA50-AE277C105216"), -14, -27, 1, DirectionOfTravel.With, length = 6, vertical = false, text = "Non-Service with road.", serviceNote = "A Service Note", reported = false);
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("A08AFCEA-481A-42D3-B8A7-88DBA949B15A"), RoadId = UUID.fromString("9895C8FF-CEDD-42C0-BA50-AE277C105216"), -14, -27, 2, DirectionOfTravel.Against, length = 6, vertical = false, serviceNote = "Another Service Note", reported = false);
        roadSegmentTest.AddTestRoadSegment(UniqueId = UUID.fromString("9895C8FF-CEDD-42C0-BA50-AE277C105216"), -14, -27, vertical = false, note = "A Note", length = 6);

        targetedRoadSegmentFeature = roadSegmentTest.AddTestRoadSegment(UniqueId = UUID.fromString("DC6C4A1C-2807-4E11-86A6-19D5C4F4A41E"), -62, -10, vertical = true, note = "A Note", length = 6, text = "Targeted Road Segment");

        roadSegmentTest.AddTestRoadSegment(UniqueId = UUID.fromString("8185D3F6-D2D0-4B0E-A8DB-2752B8F9DC25"), -62, -10, vertical = true, note = "A Note", length = 6, text = "");
    }
*/

    private fun DrawProblematicRoadSegment()
    {
        var roads = mutableListOf<RoadSegmentFeature>();
        var road = RouteRoadSegmentFeature();
        road.UniqueGuid = UUID.randomUUID();
        road.RoadGuid = road.UniqueGuid;
        road.RouteGuid = road.UniqueGuid;
        road.DirectionOfService = DirectionsOfService.With;
        road.RouteDirectionOfTravel = DirectionOfTravel.With;
        road.Sequence = 1;
        road.PassesTotal = 1;
        road.PassesCompleted = 0;

        var positions = mutableListOf<Point>();
        positions.add(Point.fromLngLat(-1.191558,52.651447));
        positions.add(Point.fromLngLat(-1.191032,52.651902));
        positions.add(Point.fromLngLat(-1.190943,52.651938));
        positions.add(Point.fromLngLat(-1.190898,52.651946));
        positions.add(Point.fromLngLat(-1.190839,52.651946));
        positions.add(Point.fromLngLat(-1.190780,52.651945));
        positions.add(Point.fromLngLat(-1.190721,52.651936));
        positions.add(Point.fromLngLat(-1.190692,52.651918));
        positions.add(Point.fromLngLat(-1.189433,52.651244));
        positions.add(Point.fromLngLat(-1.189404,52.651208));
        positions.add(Point.fromLngLat(-1.189390,52.651181));
        positions.add(Point.fromLngLat(-1.189390,52.651145));
        positions.add(Point.fromLngLat(-1.189421,52.651100));
        positions.add(Point.fromLngLat(-1.189505,52.651029));
        positions.add(Point.fromLngLat(-1.189886,52.650708));

        var lineString = LineString.fromLngLats(positions);
        var geoJSONLineString = lineString.toJson();
        road.ShapeGeoJSON = geoJSONLineString;

        var BoundingBox = LongLatExtent(GeographicPosition(-1.191558,52.651447), GeographicPosition(-1.189886,52.650708));
        road.BoundingBox = BoundingBox;

        roads.add(road);
        theMap.mapStyle.roadSegmentLayer.AddRoadSegments(roads);
    }

    private fun drawServicePointsAndTradeSites()
    {
        // Draw 14 service points that will remain on the map.
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(-5, -8, false, "Unserviced.", UniqueId =  UUID.fromString("C75CACC3-395B-49F0-BB2C-5BCAE06A69DE"));
        // This service point will initially be rendered as not serviced, but will be marked a sserviced to test the change service point and trade site properties method.
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(2, -8, false, "Should be Serviced.", UniqueId = UUID.fromString("9D098B03-A6D2-4622-83B7-430551538629"));
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(9, -8, true, reported = true, text = "& Reported.", UniqueId = UUID.fromString("8CF74E3E-6D07-4EEA-9D3E-559C640BBA5B"));

        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(-5, -10, false, hasAction = true, text = "Has action.", UniqueId = UUID.fromString("5384870D-EDBF-4C81-9E22-BF932D2D9C68"));
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(2, -10, true, hasAction = true, text = "Serviced.", UniqueId = UUID.fromString("23E8144B-3179-442B-A967-BCF12367FC20"));
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(9, -10, true, hasAction = true, reported = true, text = "& Reported.", UniqueId = UUID.fromString("5E286B23-C81D-4A47-AFDE-3FB2884BECAD"));
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(15, -10, true, hasAction = true, reported = true, actioned = true, text = "& Actioned.", UniqueId = UUID.fromString("79E3243E-2C0C-49CA-8FB4-2E8674CA3CF9"));

        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(-5, -12, false, comments = true, text = "Has comment.", UniqueId = UUID.fromString("3D34C713-0658-4BFA-8F4E-2211E77AC5E7"));
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(2, -12, true, comments = true, text = "Serviced.", UniqueId = UUID.fromString("D7ECA5B9-5668-4F36-82C3-BF686C2C0FFD"));
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(9, -12, true, comments = true, reported = true, text = "& Reported.", UniqueId = UUID.fromString("2AF0FF96-E2A1-4A33-98C3-4B8B528CDA18"));

        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(-5, -14, false, comments = true, hasAction = true, text = "Has comment & action.", UniqueId = UUID.fromString("C6E81650-0382-4BD7-898B-D3C1F2049EC0"));
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(2, -14, true, comments = true, hasAction = true, text = "Serviced.", UniqueId = UUID.fromString("9FBAA2B6-3AA8-43A4-8509-870C65348482"));
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(9, -14, true, comments = true, hasAction = true, reported = true, text = "& Reported.", UniqueId = UUID.fromString("C5AA497D-D9EA-4944-BDC5-A0E0D8001F58"));
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(15, -14, true, comments = true, hasAction = true, reported = true, actioned = true, text = "& Actioned.", UniqueId = UUID.fromString("BFDA3156-33A7-412B-B253-3A0C3EE49395"));

        // Add a single trade site that will remain on the map.
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(-5, -16, false, stopped = true, isTrade = true, text = "Stopped (trade site).", UniqueId = UUID.fromString("A5A04320-7E76-49B8-B3DC-D186E004CD8C"));

        // Add a service point that will be shown as in proximity
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(9, -16, false, comments = false, hasAction = false, text = "In proximity.", UniqueId = UUID.fromString("B7373357-05EE-4DAD-8587-41F55F420FC8"));

        // Add an additional service point that will be removed to test the remove service point and trade site method.
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(2, -16, false, "Should not be visible.", UniqueId =  UUID.fromString("A6D407CB-9C78-4719-AC0F-837295E1E4FB"));

        // Remove the additional service point.
        val servicePointsToRemove = mutableListOf<ServicePointAndTradeSiteFeature>();
        var servicePointFeature = ServicePointAndTradeSiteFeature();
        servicePointFeature.UniqueGuid = UUID.fromString("A6D407CB-9C78-4719-AC0F-837295E1E4FB");
        servicePointsToRemove.add(servicePointFeature);
        if (ConditionalCompileUI.showHiddenItemsForScreenshot == false)
        {
            theMap.mapStyle.servicePointAndTradeSiteLayer.RemoveServicePointsAndTradeSites(servicePointsToRemove);
        }
        // Change the 2nd service point to be marked as serviced.
        val servicePointPropertyChanges = mutableListOf<ServicePointAndTradeSitePropertyBase>();
        val reportedChange = ServicePointServicedProperty();
        reportedChange.ServicePointAndTradeSiteGuid = UUID.fromString("9D098B03-A6D2-4622-83B7-430551538629");
        reportedChange.Serviced = true;
        servicePointPropertyChanges.add(reportedChange);
        val proximityChange = ServicePointProximityProperty();
        proximityChange.ServicePointAndTradeSiteGuid = UUID.fromString("B7373357-05EE-4DAD-8587-41F55F420FC8");
        proximityChange.InProximity = true;
        servicePointPropertyChanges.add(proximityChange);
        theMap.mapStyle.servicePointAndTradeSiteLayer.ChangeServicePointAndTradeSiteProperties(servicePointPropertyChanges);

        targetedServicePointAndTradeSiteFeature = servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(24, 18, false, text= "Targeted Service Point Feature", UniqueId = UUID.fromString("CDA5CA1F-D9ED-46FC-AEE5-C6508CFAE394"));
    }

    private fun drawJobs()
    {
        // Draw 6 jobs that will remain on the map.
        jobTest.AddTestJob(-5, -19, JobStatus.Pending, notoverdue = true, name = "Pending.", UniqueId = UUID.fromString("A6CD365A-71C3-408A-A166-EED15586265C"));
        jobTest.AddTestJob(2, -19, JobStatus.Enroute, name = "Enroute/InProgress.", UniqueId = UUID.fromString("298B5895-AA3D-4F28-B511-72B509F32C16"));
        jobTest.AddTestJob(-5, -21, JobStatus.Pending, nearlydue = true, name = "Job Nearlydue.", UniqueId = UUID.fromString("89BD7EEB-E0CF-44FD-A256-EE9A3576EE57"));
        jobTest.AddTestJob(2, -21, JobStatus.Pending, overdue = true, name = "Job Overdue.", UniqueId = UUID.fromString("785C250C-F1E9-4D6E-9984-D4CD5F4A2A70"));
        jobTest.AddTestJob(-5, -23, JobStatus.Finished, notoverdue = true, name = "Finished.", UniqueId = UUID.fromString("0CC55810-9F26-466E-A7F8-79FDC382F02E"));
        // This last job of the 6 will be initially indicated as not reported.
        jobTest.AddTestJob(2, -23, JobStatus.Pending, overdue = true, reported = false, name = "Should be Reported.", UniqueId = UUID.fromString("8FF6499F-1377-4A5A-AD95-4F8683C86F19"));

        // Draw an additional job that will be rmeoved to test the remove job method.
        jobTest.AddTestJob(-5, -25, JobStatus.Pending, overdue = true, reported = false, name = "Should not be visible.", UniqueId = UUID.fromString("CA2DA554-1DED-4398-A7B7-09F36DCA83BF"));

        // Remove the additional job.
        val jobsToRemove = mutableListOf<JobFeature>();
        var jobFeatureToRemove = JobFeature();
        jobFeatureToRemove.UniqueGuid = UUID.fromString("CA2DA554-1DED-4398-A7B7-09F36DCA83BF");
        jobsToRemove.add(jobFeatureToRemove);
        if (ConditionalCompileUI.showHiddenItemsForScreenshot == false)
        {
            theMap.mapStyle.jobLayer.RemoveJobsFunc(jobsToRemove);
        }
        // Mark the 6th job as reported.
        val jobPropertyChanges = mutableListOf<JobPropertyBase>();
        val jobReportedChange = JobReportedProperty();
        jobReportedChange.JobInstanceGuid = UUID.fromString("8FF6499F-1377-4A5A-AD95-4F8683C86F19");
        jobReportedChange.Reported = true;
        jobPropertyChanges.add(jobReportedChange);
        theMap.mapStyle.jobLayer.ChangeJobProperties(jobPropertyChanges);

        targetedJobFeature = jobTest.AddTestJob(20, -134, JobStatus.Enroute, name = "Targeted Job Feature", UniqueId = UUID.fromString("5E6E1AD3-4D01-4CB3-BF29-E0435ADFAEC3"));
    }

    private fun drawSymbols()
    {
        // Draw 9 road segments that will remain on the map (although RouteOutline requires low zoom to be visible).
        symbolTest.AddTestSymbol(22, 1, symbolType = SymbolType.PoI, text = "Point of Interest.", UniqueId = UUID.fromString("2B8697A2-5A2A-4469-BCCF-F489CA70D482"));
        symbolTest.AddTestSymbol(22, -1, symbolType = SymbolType.Adhoc, text = "Adhoc Report.", UniqueId = UUID.fromString("8F960610-2F24-474F-9976-32C0AE194A7C"));
        symbolTest.AddTestSymbol(22, -3, symbolType = SymbolType.Location, text = "Location.", UniqueId = UUID.fromString("9A7071FA-F64B-42D4-AC03-9E56FF864007"));
        symbolTest.AddTestSymbol(22, -5, symbolType = SymbolType.LocationSearching, text = "Location Searching.", UniqueId = UUID.fromString("02164A7E-777E-436C-B0EB-EC17FE6B8F6E"));
        symbolTest.AddTestSymbol(22, -7, symbolType = SymbolType.LocationDisabled, text = "Location Disabled.", UniqueId = UUID.fromString("3563FACC-6154-46CE-A913-7521530D1ACC"));
        symbolTest.AddTestSymbol(22, 5, symbolType = SymbolType.CrumbTrail, text = "No. 1", UniqueId = UUID.fromString("06BC34A5-76ED-4194-A3FE-629CF5AC9BEC"));
        symbolTest.AddTestSymbol(22, 3, symbolType = SymbolType.SymbolText, text = "Just text.", UniqueId = UUID.fromString("02718FB1-F108-4996-9A31-30D210AF6398"));
        symbolTest.AddTestSymbol(22, -9, symbolType = SymbolType.SnappedLocation, text = "Snapped Location.", UniqueId = UUID.fromString("DBADF712-DCC7-44E6-9891-0A92B119F662"));
        symbolTest.AddTestSymbol(22, -11, symbolType = SymbolType.ManoeuvreLocation, text = "Manoeuvre Location.", UniqueId = UUID.fromString("46A81802-B27E-4D90-BFC6-1DD3BED39197"));
        symbolTest.AddTestSymbol(22, -13, symbolType = SymbolType.RouteOutline, text = "Route Locator (on very low zoom only).", UniqueId = UUID.fromString("166E48D8-974C-48E6-A1FD-1EEDDE751AD7"));

        // Draw an additional symbol that will be removed to test the remove symbol method.
        symbolTest.AddTestSymbol(22, -15, symbolType = SymbolType.Adhoc, text = "Should not be visible.", UniqueId = UUID.fromString("FACF47D5-0478-4A3E-AC20-473632800106"));

        // Remove the additional symbol.
        val symbolsToRemove = mutableListOf<SymbolMapFeature>();
        var symbolFeature = SymbolMapFeature();
        symbolFeature.UniqueGuid = UUID.fromString("FACF47D5-0478-4A3E-AC20-473632800106");
        symbolsToRemove.add(symbolFeature);
        if (ConditionalCompileUI.showHiddenItemsForScreenshot == false)
        {
            theMap.mapStyle.symbolsLayer.RemoveSymbols(symbolsToRemove);
        }
        // Draw a 2nd additional symbol that will be moved to test the change symbol position method.
        symbolTest.AddTestSymbol(22, 5, symbolType = SymbolType.CrumbTrail, text = "Should be at base of list.", UniqueId = UUID.fromString("A01AFC10-4DD4-4597-BBE0-559054E1D90B"));

        // Move the 2nd additional to the base of the list.
        symbolTest.MoveTestSymbol(22, -21, UniqueId = UUID.fromString("A01AFC10-4DD4-4597-BBE0-559054E1D90B"));

        // Draw a location marker with direction.
        symbolTest.AddTestSymbol(22, 5, symbolType = SymbolType.LocationWithDirection, text = "Should be pointing North.", UniqueId = UUID.fromString("34CF3511-2ACB-4B88-9013-E3A9941D80FE"));

        // Move the location marker with direction and include a bearing.
        symbolTest.MoveTestSymbol(22, -17, UniqueId = UUID.fromString("34CF3511-2ACB-4B88-9013-E3A9941D80FE"), newBearing = 0.0);

        // Draw a 2nd location marker with direction.
        symbolTest.AddTestSymbol(22, 5, symbolType = SymbolType.LocationWithDirection, text = "Should be pointing East.", UniqueId = UUID.fromString("59461A5E-5CA7-4761-BD5A-1E1CBB2FD3DA"));

        // Move the 2nd location marker with direction and include a bearing.
        symbolTest.MoveTestSymbol(22, -19, UniqueId = UUID.fromString("59461A5E-5CA7-4761-BD5A-1E1CBB2FD3DA"), newBearing = 90.0);

        targetedPoIFeature = symbolTest.AddTestSymbol(-222, -30, symbolType = SymbolType.PoI, text = "Targeted PoI Feature", UniqueId = UUID.fromString("1AA44774-0FE3-4F07-8D63-8580BC8B59EF")) as PointOfInterestFeature;
    }
    class point(x: Int, y: Int, bearing: Double, speed: Double){

        public val x: Int = x;
        public val y: Int = y;
        public val bearing: Double = bearing;
        public val speed: Double = speed;
    }

    var demoCount: Int = 100;
    public fun drawLocationMarkers()
    {
        locationMarkerTest.AddTestLocationMarker(0, 0, "0,0");
    }

    val startingPoint = point(0, 10, 0.0, 0.0);
    val scale = 5;
    private fun moveToNextPosition(point: point)
    {
        val adjustedPoint = point((point.x * scale) + startingPoint.x, (point.y * scale) + startingPoint.y, point.bearing, point.speed);
        Log.i(TAG, "Move... ${adjustedPoint.x},${adjustedPoint.y} ${adjustedPoint.bearing}")
        this@MainActivity.runOnUiThread(java.lang.Runnable
        {
            try
            {
                var text: String = "${adjustedPoint.x},${adjustedPoint.y} - ${adjustedPoint.bearing}";
                locationMarkerTest.SetOrChangeLocationMarkerPosition(adjustedPoint.x, adjustedPoint.y, text, adjustedPoint.bearing, adjustedPoint.speed);
            }
            catch (ex: java.lang.Exception)
            {
                Log.e(TAG,"Exception = ${ex.message}")
            }
        });
    }

    private fun drawSuperimposedFeatures()
    {
        // Add 3 flats at same location.
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(15, -18, false, comments = true, hasAction = true, reported = false, actioned = false, text = "3 Flats", flat = "a", UniqueId = UUID.fromString("D4DABC8B-8FAA-4F95-B1F7-7C73A05602FB"));
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(15, -18, false, comments = false, hasAction = false, reported = false, actioned = false, text = "", flat = "b", UniqueId = UUID.fromString("F6ED8E2C-C29E-4606-A40F-6B620D2AE628"));
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(15, -18, false, comments = false, hasAction = false, reported = false, actioned = false, text = "", flat = "c", UniqueId = UUID.fromString("4AAA032E-F0FE-4AC7-B5D7-BE401EE93156"));

        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("F38AECFF-8641-4212-B45D-AADC82A00D4E"), RoadId = UUID.fromString("80A6FF48-4E75-4631-9700-D2C147357209"), 15, -22, 1, DirectionOfTravel.With, 0, 0, serviced = false, vertical = false, length = 3, text = "One.");
        roadSegmentTest.AddTestRoadSegment(UniqueId = UUID.fromString("0E07798F-303D-4858-937F-F4F6E8AAEBE4"), 15, -22, vertical = true, note = "A Note", length = 3, text = "Two.");
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("5670024C-B118-43B7-8BD4-E617773719E3"), RoadId = UUID.fromString("134432D6-6127-4CA0-BF11-FFD4D79743FE"), 12, -22, 1, DirectionOfTravel.With, length = 3, vertical = false, text = "Three.", reported = false);


        roadSegmentTest.AddTestRoadSegment(UniqueId = UUID.fromString("AE938646-A7F4-4758-8938-D93E46D7B302"), 14, -25, vertical = false, note = "A Note", length = 2);
        roadSegmentTest.AddTestRouteRoadSegment(UniqueId = UUID.fromString("32FD9030-E660-45CB-AF30-D3E639A719E2"), RoadId = UUID.fromString("AE938646-A7F4-4758-8938-D93E46D7B302"), 14, -25, 1, DirectionOfTravel.With, 0, 0, serviced = false, vertical = false, length = 2, text = "");
        roadSegmentTest.AddTestRouteRoadNonServiceSegment(UniqueId = UUID.fromString("732A0A56-618F-42C3-A36C-287FFD44F469"), RoadId = UUID.fromString("AE938646-A7F4-4758-8938-D93E46D7B302"), 14, -25, 1, DirectionOfTravel.With, length = 2, vertical = false, text = "", reported = false);
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(15, -25, false, comments = true, hasAction = true, reported = false, actioned = false, text = "", flat = "", UniqueId = UUID.fromString("0BD687F0-8087-40DD-AF73-FC22F47F9C00"));
        servicePointAndTradeSiteTest.AddTestServicePointAndTradeSite(15, -25, isTrade = true, stopped = true, serviced = false, comments = true, hasAction = true, reported = false, actioned = false, text = "", flat = "", UniqueId = UUID.fromString("8BDFB169-EACA-4778-8E58-D02007179D3C"));
        jobTest.AddTestJob(15, -25, JobStatus.Pending, notoverdue = true, name = "", UniqueId = UUID.fromString("1C8FC887-CE6B-42BB-A84D-ED7E4EAC637B"));
        symbolTest.AddTestSymbol(15, -25, symbolType = SymbolType.PoI, text = "", UniqueId = UUID.fromString("CCF6CB2F-DFCB-4DD3-B792-C37436401479"));
        symbolTest.AddTestSymbol(15, -25, symbolType = SymbolType.Adhoc, text = "", UniqueId = UUID.fromString("3B92DB45-B077-4658-8332-E543E57DFC9B"));

    }

    override fun MapCameraExtentUpdateFromMapRendererKotlin(newExtent: ExtentType)
    {
        Log.i(TAG, "Callback received from Map to say Extent has changed to ${newExtent.toString()}");
        spinnerExtent.setSelection(newExtent.ordinal);
    }

    override fun MapCameraPositionUpdateFromMapRendererKotlin(newMapCameraPosition: MapCameraPosition) {
        Log.i(TAG, "Callback received from Map to say MapCameraPosition has changed.");
        UpdateCameraPositionDisplay(newMapCameraPosition);
    }

    override fun GestureFromMapRendererKotlin(
        moveDetected: Boolean,
        rotateDetected: Boolean,
        shoveDetected: Boolean,
        flingDetected: Boolean,
        scaleDetected: Boolean
    ) {
        Log.i(TAG, "Callback received from Map to say a gesture has been detected move=${moveDetected}, rotate=${rotateDetected}, shove=${shoveDetected}, fling=${flingDetected}, scale=${scaleDetected}.");
        var mapCameraPosition = theMap.mapCamera.GetMapCameraPosition();
        UpdateCameraPositionDisplay(mapCameraPosition);
    }

    override fun MapClickDetected(clickDetected: MapPosition) {
        //TODO("Not yet implemented")
    }

    override fun MapLongClickDetected(longClickDetected: MapPosition) {
        //TODO("Not yet implemented")
        var roadPosition = theMap.featureSelection.GetRoadPosition(longClickDetected.GeoPosition, false);
        if (roadPosition != null) {
            roadPositionDisplay.setText(roadPosition.Name);
        } else {
            //roadPositionDisplay.setText("NK");
        }
    }

    override fun RoadPositionDetected(roadPosition: RoadPosition?) {
        runOnUiThread() {
            if (roadPosition != null) {
                roadPositionDisplay.setText(roadPosition.Name);
            } else {
                roadPositionDisplay.setText("NK");
            }
        }
    }

    override fun FeaturesTapped(tappedMapFeatures: MutableList<MapFeature>)
    {
        featuresTappedNumber.text = "${tappedMapFeatures.count()} Tapped.";
    }

    override fun FeaturesSelected(selectedMapFeature: MapFeature?, selectedMapFeatures: MutableList<MapFeature>)
    {
        featuresSelectedNumber.text = "${theMap.featureSelection.selectedMapFeatures.count()} Selected."
    }

    override fun onCreateMultiFeatures(numberRouteSegments: Int, numberServicePoints: Int) {
        Log.d(TAG, "onCreateMultiFeatures for numberRoadSegments=${numberRouteSegments}, numberServicePoints=${numberServicePoints}.");
        roadSegmentTest.AddMultipleRoadSegments(numberRouteSegments);
        servicePointAndTradeSiteTest.AddMultipleServicePoints(numberServicePoints);
    }

    override fun StyleLoadedFromMapRendererKotlin() {
        //TODO("Not yet implemented")
    }

    override fun MapIdleFromMapRendererKotlin() {
        //TODO("Not yet implemented")
    }

}