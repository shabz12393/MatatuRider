package com.jfloydconsult.mataturider;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.ValueEventListener;
import com.jfloydconsult.mataturider.common.AppConstants;
import com.jfloydconsult.mataturider.common.Common;
import com.jfloydconsult.mataturider.helper.CustomInfoWindow;
import com.jfloydconsult.mataturider.model.Rider;
import com.jfloydconsult.mataturider.retrofit.IGoogleAPI;
import com.jfloydconsult.mataturider.utility.SurveyLab;

public class Home extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 3000, DISPLACEMENT = 10; // = 5 seconds
    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;
    private static final String TAG = "Home";
    public SurveyLab mSurveyLab;
    FusedLocationProviderClient mFusedLocationClient;
    //Pickup Request
    FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    DatabaseReference mPickupRequestDbRef = mDatabase.getReference().child(AppConstants.PICKUP_REQUESTS);
    Marker mUserMarker;
    SupportMapFragment mapFragment;
    private Location mLastLocation;
    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String mUserId;
    private GoogleMap mMap;
    //Bike Animation
    private float v;
    private AutocompleteSupportFragment places;
    private String destination;
    private IGoogleAPI mServices;
    LocationRequest mLocationRequest;
    //BottomSheetFragment
    ImageView imgExpandable;
    BottomSheetRiderFragment mBottomSheet;

    //Pickup Request
    Button btnPickupRequest;
    private boolean isContinue = false;

    boolean isDriverFound = false;
    String mDriverId = "";
    int radius = 1; //1km
    int mDistance = 1; //3km
    private static final int LIMIT =3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        }
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);


        places = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.place_location_autocomplete_fragment);
        places.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

        places.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
                if (mSurveyLab.checkConnectivity()) {
                    destination = place.getName();
                    destination = destination.replace(" ", "+"); // Replace space with + for fetch data
                    Log.d(TAG, destination);
                    //getDirection();

                } else {
                    Toast.makeText(Home.this, "You are offline", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });
        mSurveyLab = SurveyLab.get(getApplicationContext());

        // we add permissions we need to request location of the users
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionsToRequest = permissionsToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }


        mServices = Common.getGoogleAPI();
        mAuth = FirebaseAuth.getInstance();

        //Init View
        imgExpandable = findViewById(R.id.content_main_imgExpandable);
        mBottomSheet = BottomSheetRiderFragment.newInstance("Rider Bottom Sheet");
        imgExpandable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheet.show(getSupportFragmentManager(),mBottomSheet.getTag());
            }
        });

        btnPickupRequest = findViewById(R.id.content_main_pickup_request);

        btnPickupRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isContinue=false;
                requestPickupHere(mAuth.getUid());
            }
        });

        connectToMap();
    }

    private void connectToMap() {
        if (mSurveyLab.checkConnectivity()) {
            isContinue=true;
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            createLocationRequest();
            getLastLocation();
        } else {
            // stop location updates
            stopLocationUpdates();
            //remove Customer
            CustomerRemoved();
            Snackbar.make(mapFragment.getView(), "You are offline", Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
    }

    private void requestPickupHere(String uid) {
        DatabaseReference dbRequest = mDatabase.getReference(AppConstants.PICKUP_REQUESTS);
        GeoFire geoFire = new GeoFire(dbRequest);
        geoFire.setLocation(uid, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

        if(mUserMarker.isVisible())
            mUserMarker.remove();
        if(mLastLocation!=null){
            //Add new marker
            mUserMarker = mMap.addMarker(new MarkerOptions()
                    .title("Pickup Here")
                    .snippet("")
                    .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            mUserMarker.showInfoWindow();

            btnPickupRequest.setText("Getting your Driver...");

            findDriver();
        }
    }

    private void findDriver() {
        DatabaseReference driversRef = mDatabase.getReference(AppConstants.DRIVERS_AVAILABLE);
        GeoFire gfDrivers = new GeoFire(driversRef);

        GeoQuery geoQuery = gfDrivers.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()),
                radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                //if found
                if(!isDriverFound){
                    isDriverFound=true;
                    mDriverId=key;
                    btnPickupRequest.setText("CALL DRIVER");
                    Toast.makeText(Home.this, ""+key, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
            // if driver still not found, increase radius
                if(!isDriverFound){
                    radius++;
                    findDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
    private void LoadAllAvailableDrivers() {

        //Load all available Driver in distance 3km
        DatabaseReference driverLocationRef = mDatabase.getReference(AppConstants.DRIVERS_AVAILABLE);
        GeoFire gfDrivers = new GeoFire(driverLocationRef);

        GeoQuery geoQuery = gfDrivers.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(),
                        mLastLocation.getLongitude()),
                mDistance);

        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //Use key to get email from Drivers
                //Table Drivers is the table where Drivers keep their information
                mDatabase.getReference(AppConstants.DRIVERS)
                        .child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                //Because Customer Model and Rider Model have same properties
                                //We can use the Customer Model to get the data here
                                Rider rider = snapshot.getValue(Rider.class);
                                if(rider!=null)
                                //Add driver to map
                                mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(location.latitude, location.longitude))
                                        .flat(true)
                                        .title("Driver: "+rider.getFullName())
                                        .snippet("Phone: "+rider.getPhone())
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bike)));
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                //3km distance
                if(mDistance<=LIMIT){
                    mDistance++;
                    LoadAllAvailableDrivers();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        //calling the method displayselectedscreen and passing the id of selected menu
        //make this method blank
        return true;
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

            // Permissions ok, we get last location
            mFusedLocationClient.getLastLocation().addOnCompleteListener(
                    new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            mLastLocation = task.getResult();
                                if (mLastLocation == null&&isContinue) {
                                    startLocationUpdates();
                                } else {
                                  LatLng Customer = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                                        //Add Marker
                                    if (mUserMarker != null)
                                            mUserMarker.remove(); //Remove already marker
                                    mUserMarker = mMap.addMarker(new MarkerOptions()
                                                .position(Customer)
                                                .title(getResources().getString(R.string.your_location_string)));
                                    // move camera to this position
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Customer, 15.0f));
                                    LoadAllAvailableDrivers();
                                 }
                        }
                    }
            );
    }


    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );
    }

    private void stopLocationUpdates() {
        // stop location updates
        if(mFusedLocationClient!=null)
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private void signOut() {
        stopLocationUpdates();
        CustomerRemoved();
        mAuth.signOut();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }

    private void CustomerRemoved() {
        if(mUserMarker.isVisible())
            mUserMarker.remove();
        mUserId = mAuth.getCurrentUser().getUid();
        GeoFire geoFire = new GeoFire(mPickupRequestDbRef);
        geoFire.removeLocation(mUserId);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setInfoWindowAdapter(new CustomInfoWindow(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        connectToMap();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // stop location updates
        isContinue=false;
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if(locationResult==null)
                return;
            else{
                if(mUserMarker!=null)
                    mUserMarker.remove();
                mLastLocation = locationResult.getLastLocation();
                LatLng Customer = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                mUserMarker = mMap.addMarker(new MarkerOptions()
                        .position(Customer)
                        .title(getResources().getString(R.string.your_location_string)));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Customer, 15.0f));
                LoadAllAvailableDrivers();
                if (!isContinue)
                    stopLocationUpdates();
            }

            /*
            mUserId = mAuth.getCurrentUser().getUid();
            GeoFire geoFire = new GeoFire(mPickupRequestDbRef);
            geoFire.setLocation(mUserId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
       */ }
    };

//Permissions

    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }
        return result;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }
                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(Home.this).
                                    setMessage("These permissions are mandatory to get your location. You need to allow them.").
                                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.
                                                        toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();

                            return;
                        }
                    }
                } else {
                    if (mSurveyLab.checkConnectivity()) {
                        // Permissions ok, we get last location
                        getLastLocation();
                    }
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sign_out:
                signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
