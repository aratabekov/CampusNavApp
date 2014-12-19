package com.ksunavigation.team.campusnavapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.ksunavigation.team.campusnavapp.routing.Graph;
import com.ksunavigation.team.campusnavapp.routing.Point;
import com.ksunavigation.team.campusnavapp.utils.ParserUtils;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements
    LocationListener,
    GooglePlayServicesClient.ConnectionCallbacks,
    GooglePlayServicesClient.OnConnectionFailedListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Button btn_Locate;
    private Button btn_saveCar;
    private Button btn_Back;
    private Button btn_Cancel;
    private TextView txt_Info;
    private Marker locationMarker;
    private Marker carMarker;
    private Marker finishMarker;
    private ListView mListView;
    private Polyline currentRoute;
    private Point currentLocation;
    private Graph graph;
    public static final int DEST_CODE=777;

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    // Handle to SharedPreferences for this app
    private SharedPreferences mPrefs;

    // Handle to a SharedPreferences editor
    private SharedPreferences.Editor mEditor;

    /*
     * Note if updates have been turned on. Starts out as "false"; is set to "true" in the
     * method handleRequestSuccess of LocationUpdateReceiver.
     *
     */
    private boolean mUpdatesRequested = false;
    //private boolean showListView=false;

    public void displayCarIfExists() {
        UserInfoDatabase userInfoDatabase = new UserInfoDatabase(getApplicationContext());
        Point carLocation = userInfoDatabase.getCarLocation();
        if (carLocation != null) {
            double lon = carLocation.getX();
            double lat = carLocation.getY();
            carMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("Car Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
        }
    }

    /*
     * Initialize the Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        this.graph=new Graph();
        constructGraph();
        setUpMapIfNeeded();

        this.txt_Info=(TextView)findViewById(R.id.textInfo);
        this.btn_Locate=(Button) findViewById(R.id.locate_me);
        this.btn_Cancel=(Button) findViewById(R.id.cancel_route);
        this.btn_saveCar=(Button) findViewById(R.id.save_car);
        this.btn_Back= (Button) findViewById(R.id.back_to_map);
        this.btn_saveCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //currentLocation = new Point (34.035579, -84.584751); // Just for testing purposes
                Log.i("testTag", "button clicked");
                double lon = MapsActivity.this.currentLocation.getX();
                double lat = MapsActivity.this.currentLocation.getY();
                UserInfoDatabase userInfoDatabase = new UserInfoDatabase(getApplicationContext());
                Log.i("Saving Car",lat+" "+lon);


                userInfoDatabase.saveCarLocation(MapsActivity.this.currentLocation);

                displayCarIfExists();
            }
        });

        this.mListView=(ListView) findViewById(R.id.list_view);

        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        // Note that location updates are off until the user turns them on
        //mUpdatesRequested = false;

        // Open Shared Preferences
        mPrefs = getSharedPreferences(LocationUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        // Get an editor
        mEditor = mPrefs.edit();

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);

        //doMySearch(("stuff"));



       /*if(mPrefs.contains("X")&& mPrefs.contains("Y") && mPrefs.contains(LocationUtils.KEY_UPDATES_REQUESTED)){
            Popup("stuff");
            double x=Double.valueOf( mPrefs.getString("X","0"));
            double y=Double.valueOf( mPrefs.getString("Y","0"));
            this.currentLocation=new Point(x,y);
            this.mUpdatesRequested=mPrefs.getBoolean(LocationUtils.KEY_UPDATES_REQUESTED,false);
            if(mUpdatesRequested){
                if (servicesConnected()) {
                    startPeriodicUpdates();
                    //
                }
            }

        }*/

        handleIntent(getIntent());

        displayCarIfExists();

        //Popup("oncreate");

    }
    @Override
    protected void onNewIntent(Intent intent) {
        // Because this activity has set launchMode="singleTop", the system calls this method
        // to deliver the intent if this actvity is currently the foreground activity when
        // invoked again (when the user executes a search from this activity, we don't create
        // a new instance of this activity, so the system delivers the search intent here)
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // handles a click on a search suggestion; launches activity to show word
            //Intent destIntent = new Intent(this, DestinationActivity.class).putExtra("CURRENT_LOCATION", currentLocation.getX()+","+currentLocation.getY());;
            Intent destIntent = new Intent(this, DestinationActivity.class);
            destIntent.setData(intent.getData());
            startActivityForResult(destIntent, MapsActivity.DEST_CODE);
            //finish();
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // handles a search query
            String query = intent.getStringExtra(SearchManager.QUERY);

            showResults(query);
            mListView.setVisibility(View.VISIBLE);
            btn_Locate.setVisibility(View.INVISIBLE);
            btn_saveCar.setVisibility(View.INVISIBLE);

        }
    }




    public void constructGraph(){

        PathDatabase path=new PathDatabase(getApplicationContext());
        String[] columns = new String[] {
                BaseColumns._ID,
                PathDatabase.KEY_NAME,
                PathDatabase.KEY_POINTS};

        Cursor cur= path.GetPaths(columns);
        cur.moveToPosition(-1);
        String r="";
        if(cur!=null) {
            while (cur.moveToNext()) {
                List<Point> list = new ArrayList<Point>();

                String content = cur.getString(2);
                list = ParserUtils.buildingPointsParser(content);

                if (list.size() != 0) {
                    //Popup(list.toString());
                    graph.addEdge(list);

                }
            }
        }
    }

    public void Popup(String query){
        new AlertDialog.Builder(this)
                .setTitle("Alert box")
                .setMessage(query)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showResults(String query) {

        Cursor cursor = managedQuery(BuildingProvider.CONTENT_URI, null, null,
                new String[]{query}, null);
        //Popup(query);
        if (cursor == null) {
            // There are no results
            //mTextView.setText(getString(R.string.no_results, new Object[] {query}));
        } else {
            // Display the number of results
            int count = cursor.getCount();
            //Popup(""+count);
            String countString = getResources().getQuantityString(R.plurals.search_results,
                    count, new Object[] {count, query});
            //mTextView.setText(countString);

            // Specify the columns we want to display in the result
            String[] from = new String[] { BuildingDatabase.KEY_BUILDING};

            // Specify the corresponding layout elements where we want the columns to go
            int[] to = new int[] { R.id.building};

            // Create a simple cursor adapter for the definitions and apply them to the ListView
            SimpleCursorAdapter destinations = new SimpleCursorAdapter(this,
                    R.layout.result, cursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            mListView.setAdapter(destinations);

            // Define the on-click listener for the list items
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // Build the Intent used to open DestinationActivity with a specific word Uri
                    //Intent wordIntent = new Intent(getApplicationContext(), DestinationActivity.class).putExtra("CURRENT_LOCATION", currentLocation.getX()+","+currentLocation.getY());;
                    Intent destIntent = new Intent(getApplicationContext(), DestinationActivity.class);

                    Uri data = Uri.withAppendedPath(BuildingProvider.CONTENT_URI,
                            String.valueOf(id));
                    destIntent.setData(data);
                    //startActivity(wordIntent);
                    startActivityForResult(destIntent,MapsActivity.DEST_CODE);
                }
            });
            //mListView.setVisibility(View.VISIBLE);
            //Popup("here");//
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        //searchView.setIconified(false);
        //SearchView searchView = (SearchView)findViewById(R.id.search);
        /*searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //currentLocation = new Point (34.035579, -84.584751); // Just for testing purposes
                Popup("happened");
            }
        });*/

        final MapsActivity thisActivity=this;
        setSearchViewOnClickListener(searchView,new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //currentLocation = new Point (34.035579, -84.584751); // Just for testing purposes

                if (v instanceof TextView) {
                    TextView text = (TextView)v;
                    //text.setFocusable(false);
                    if(text.getText().length()==0){
                        //Popup("happened");

                        UserInfoDatabase db=new UserInfoDatabase(getApplicationContext());
                        Cursor cursor=db.getSavedHistoryCursor();

                        String[] from = new String[] { BuildingDatabase.KEY_BUILDING };

                        // Specify the corresponding layout elements where we want the columns to go
                        int[] to = new int[] { R.id.building };

                        // Create a simple cursor adapter for the definitions and apply them to the ListView
                        SimpleCursorAdapter destinations = new SimpleCursorAdapter(thisActivity,
                                R.layout.result, cursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                        mListView.setAdapter(destinations);


                        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                // Build the Intent used to open DestinationActivity with a specific word Uri
                                //Intent wordIntent = new Intent(getApplicationContext(), DestinationActivity.class).putExtra("CURRENT_LOCATION", currentLocation.getX()+","+currentLocation.getY());;
                                Intent destIntent = new Intent(getApplicationContext(), DestinationActivity.class);

                                Uri data = Uri.withAppendedPath(BuildingProvider.CONTENT_URI,
                                        String.valueOf(id));
                                destIntent.setData(data);
                                //startActivity(wordIntent);
                                startActivityForResult(destIntent,MapsActivity.DEST_CODE);
                            }
                        });
                        mListView.setVisibility(View.VISIBLE);
                        btn_saveCar.setVisibility(View.INVISIBLE);
                        btn_Locate.setVisibility(View.INVISIBLE);
                        btn_Back.setVisibility(View.VISIBLE);

                        txt_Info.setVisibility(View.VISIBLE);

                        clearRoute();
                        btn_Cancel.setVisibility(View.INVISIBLE);
                        //mListView.setcolor
                    }
                }

            }
        });
        //searchView.seton


        return true;

    }

    public void addUpdateMarker(double x, double y){
        if(this.locationMarker!=null)
            this.locationMarker.remove();
        this.locationMarker=mMap.addMarker(new MarkerOptions().position(new LatLng(y, x)).title("Position").icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));
        //mMap.moveCamera(new Came);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(y, x),16));
        UiSettings sett=mMap.getUiSettings();
        sett.setCompassEnabled(true);
    }

    public static void setSearchViewOnClickListener(View v, View.OnClickListener listener) {
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)v;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = group.getChildAt(i);
                if (child instanceof LinearLayout || child instanceof RelativeLayout) {
                    setSearchViewOnClickListener(child, listener);
                }

                if (child instanceof TextView) {
                    TextView text = (TextView)child;
                    //text.setFocusable(false);
                }
                child.setOnClickListener(listener);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                onSearchRequested();
                return true;
            default:
                return false;
        }
    }
   /* TileProvider tileProvider = new UrlTileProvider(256, 256) {
        @Override
        public URL getTileUrl(int x, int y, int zoom) {

       // Define the URL pattern for the tile images
            String s = String.format("https://dl.dropboxusercontent.com/u/25207350/Tiles/%d_%d_%d.png",
                    x, y,zoom);

            try {
                return new URL(s);
            } catch (MalformedURLException e) {
                throw new AssertionError(e);
            }
        }
    };*/

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
                //just added a comment
            }
        }
    }


    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //this.locationMarker=;
        //this.locationMarker= mMap.addMarker(new MarkerOptions().position(new LatLng( 34.038170,-84.581801 )).title("Marker"));
        //mMap.setMapType(GoogleMap.MAP_TYPE_NONE);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(34.038170, -84.581801), 15));
        //mMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));

        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new CustomTileProvider(getResources().getAssets())));

        //New code below


        //
       // checkNumberOfAdjacent();
        //Popup(points[0].getOriginalValue()+"\n"+points[1].getOriginalValue()+"\n"+points[2].getOriginalValue()+"\n"+points[3].getOriginalValue());
    }

    public void calcAndDisplayRoute(String source, String target){
        List<Point> route=new ArrayList<Point>();

        route.clear();
        double dist = graph.getRoute(source, target,route);
        //Popup("dist is "+dist+"\n"+points[1].getOriginalValue()+"\n"+points[3].getOriginalValue());
        //List<Point> route = graph.getRoute(points[2].toString(), points[points.length - 4].toString());
        if(route.size()==0) {

            Popup("Sorry coudn't find the route");
        }
        else {

            List<LatLng> set = new ArrayList<LatLng>();
            for (Point p : route) {
                set.add(new LatLng(p.getY(), p.getX()));
            }
            //Popup(route.size() + "");
            this.currentRoute = mMap.addPolyline(new PolylineOptions()
                    .addAll(set)
                    .width(5)
                    .color(Color.RED));
            currentRoute.setZIndex(1000);

            this.finishMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(route.get(0).getY(), route.get(0).getX())).title("Destination").icon(BitmapDescriptorFactory.fromResource(R.drawable.finish)));

        }
    }

    public void clearRoute(){
        if(this.currentRoute!=null) {
            this.currentRoute.remove();
        }
        if(this.finishMarker!=null){
            this.finishMarker.remove();
        }
    }



    /*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    public void onStop() {

        //Popup("onstop");
        // If the client is connected
        if (mLocationClient.isConnected()) {
           // stopPeriodicUpdates();
        }

        // After disconnect() is called, the client is considered "dead".
        //mLocationClient.disconnect();

        super.onStop();
    }
    /*
     * Called when the Activity is going into the background.
     * Parts of the UI may be visible, but the Activity is inactive.
     */
    @Override
    public void onPause() {

        // Save the current setting for updates

        if(currentLocation!=null){
            mEditor.putString("X",this.currentLocation.getX()+"");
            mEditor.putString("Y",this.currentLocation.getY()+"");
        }
        //outState.putBoolean("UpdatesRequested",mUpdatesRequested);
        mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, mUpdatesRequested);
        mEditor.commit();

        mListView.setVisibility(View.INVISIBLE);


        btn_Locate.setVisibility(View.VISIBLE);
        btn_saveCar.setVisibility(View.VISIBLE);
        btn_Back.setVisibility(View.INVISIBLE);

        txt_Info.setVisibility(View.INVISIBLE);

        clearRoute();

        super.onPause();

    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {

        super.onStart();

        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        mLocationClient.connect();

    }
    /*
     * Called when the system detects that this Activity is now visible.
     */
    @Override
    public void onResume() {

        super.onResume();

        setUpMapIfNeeded();
    }


    public void cancelRoute(View v){
        clearRoute();
        btn_Cancel.setVisibility(View.INVISIBLE);
    }

    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // Log the result
                        Log.d(LocationUtils.APPTAG, getString(R.string.resolved));

                        // Display the result
                        //mConnectionState.setText(R.string.connected);
                        //mConnectionStatus.setText(R.string.resolved);
                        break;

                    // If any other result was returned by Google Play services
                    default:
                        // Log the result
                        Log.d(LocationUtils.APPTAG, getString(R.string.no_resolution));

                        // Display the result
                        // mConnectionState.setText(R.string.disconnected);
                        //mConnectionStatus.setText(R.string.no_resolution);

                        break;
                }

            case MapsActivity.DEST_CODE :

                if(resultCode == RESULT_OK){

                    if(mPrefs.contains("X")&& mPrefs.contains("Y") && mPrefs.contains(LocationUtils.KEY_UPDATES_REQUESTED)){

                        double x=Double.valueOf( mPrefs.getString("X","0"));
                        double y=Double.valueOf( mPrefs.getString("Y","0"));
                        //Popup("stuff "+x+" : "+y);
                        this.currentLocation=new Point(x,y);
                        //this.mUpdatesRequested=mPrefs.getBoolean(LocationUtils.KEY_UPDATES_REQUESTED,true);
                        //if(mUpdatesRequested){
                        //mLocationClient = new LocationClient(this, this, this);
                        //mLocationClient.connect();
                           // if (servicesConnected()) {
                          //      startPeriodicUpdates();
                                //
                           // }
                       // }
                       addUpdateMarker(currentLocation.getX(),currentLocation.getY());

                    }



                    String result=intent.getStringExtra("result");

                    //Uri myuri=Uri.parse(result);
                    BuildingDatabase db=new BuildingDatabase(getApplicationContext());
                    Building building=db.getBuildingById(result);

                    Point[] points = graph.getVertices();


                    clearRoute();

                    if(this.currentLocation==null){
                        Popup("sorry the current location is null");
                    }
                    else {

                        Point p0 = graph.getClosestVertex(this.currentLocation, points);
                        Point p1 = graph.getClosestVertex(building.getPoints().get(0), points);



                        if (p1 == null || p0 == null) {
                            Popup("sorry one of the points is null");
                        } else {

                            calcAndDisplayRoute(p0.toString(), p1.toString());
                            btn_Cancel.setVisibility(View.VISIBLE);
                        }
                        //
                    }




                }
                // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
                Log.d(LocationUtils.APPTAG,
                        getString(R.string.unknown_activity_request_code, requestCode));

                break;
        }

    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(LocationUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), LocationUtils.APPTAG);
            }
            return false;
        }
    }

    /**
     * Invoked by the "Start Updates" button
     * Sends a request to start location updates
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void startUpdates(View v) {
        mUpdatesRequested = true;

        stopPeriodicUpdates();
        startPeriodicUpdates();
        if (servicesConnected()) {
            startPeriodicUpdates();
        }
    }

    public void backToMap(View v) {
        this.mListView.setVisibility(View.INVISIBLE);
        this.btn_Back.setVisibility(View.INVISIBLE);

        btn_Locate.setVisibility(View.VISIBLE);
        btn_saveCar.setVisibility(View.VISIBLE);
        txt_Info.setVisibility(View.INVISIBLE);
    }

    /**
     * Invoked by the "Stop Updates" button
     * Sends a request to remove location updates
     * request them.
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void stopUpdates(View v) {
        mUpdatesRequested = false;

        if (servicesConnected()) {
            stopPeriodicUpdates();
        }
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        //mConnectionStatus.setText(R.string.connected);

        if (mUpdatesRequested) {
            startPeriodicUpdates();
        }
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        //mConnectionStatus.setText(R.string.disconnected);
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {

            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }


    @Override
    public void onLocationChanged(Location location) {

        // Report to the UI that the location was updated
        //mConnectionStatus.setText(R.string.location_updated);

        // In the UI, set the latitude and longitude to the value received
        //btn_Locate.setText(LocationUtils.getLatLng(this, location));
        //this.locationMarker.



        addUpdateMarker(location.getLongitude(),location.getLatitude());

        this.currentLocation = new Point( location.getLongitude(),location.getLatitude());

        btn_saveCar.setVisibility(View.VISIBLE);
    }

    /**
     * In response to a request to start updates, send a request
     * to Location Services
     */
    private void startPeriodicUpdates() {

        mLocationClient.requestLocationUpdates(mLocationRequest, this);
        // mConnectionState.setText(R.string.location_requested);
    }

    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);
        //mConnectionState.setText(R.string.location_updates_stopped);
    }


    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this,
                LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(getSupportFragmentManager(), LocationUtils.APPTAG);
        }
    }

    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }


}
