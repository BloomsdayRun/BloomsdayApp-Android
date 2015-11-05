/*
Non-networked, runner tracking MVP
Geolocation samples:
    http://developer.android.com/training/location/retrieve-current.html
    http://developer.android.com/training/location/receive-location-updates.html
 */

package edu.gonzaga.bloomsday;

import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;

    private Location mLastLocation;

    private Location mCurrentLocation;
    private Location mPriorLocation;

    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;
    private GoogleApiClient mGoogleApiClient;

    private TextView mLatitudeTextView;
    private TextView mLongitudeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //Get layout elements
        mLatitudeTextView = (TextView) findViewById(R.id.latitude);
        mLongitudeTextView = (TextView) findViewById(R.id.longitude);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //Create Google API client for getting location
        buildGoogleApiClient();
        //Create location request for polling
        createLocationRequest();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000); //poll new location every 10s
        mLocationRequest.setFastestInterval(5000); //set max update frequency at every 5s
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    //TODO: Won't get location updates unless user has fine-grained GPS enabled; tell user to turn it on
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }

    // TODO: Clean up
    // TODO: Add uncertainty
    // TODO: zoom in/out buttons
    // TODO: interpolate between positions
    private void updateUI() {
        System.out.println("Updating UI");
        System.out.println(String.valueOf(mCurrentLocation.getLatitude()));
        System.out.println(String.valueOf(mCurrentLocation.getLongitude()));
        System.out.println(mLastUpdateTime);

        LatLng here = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        mMap.addMarker(new MarkerOptions().position(here).title("You are here!"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(here));
        mLatitudeTextView.setText(String.valueOf(mCurrentLocation.getLatitude()));
        mLongitudeTextView.setText(String.valueOf(mCurrentLocation.getLongitude()));

        //Draw polylines betwixt here and prior location
        if (mPriorLocation == null) {
            mPriorLocation = mLastLocation;
        } else {
            if (mPriorLocation != mLastLocation) {
                LatLng prevLL = new LatLng(mPriorLocation.getLatitude(), mPriorLocation.getLongitude());
                Polyline line = mMap.addPolyline(new PolylineOptions()
                        .add(prevLL, here)
                        .width(5)
                        .color(Color.RED));
                mPriorLocation = mLastLocation;
            }
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    //TODO: Handle failed connection, refused services
    public void onConnected(Bundle connectionHint) {
        System.out.println("Connected!");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            System.out.println("latitude: " + String.valueOf(mLastLocation.getLatitude()));
            System.out.println("longitude: " + String.valueOf(mLastLocation.getLongitude()));
        } else {
            System.out.println("Location is null!");
        }

        //TODO: requesting location updates should be a user defined choice
        boolean mRequestingLocationUpdates = true;
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        //Testing: Launch a new thread for printing out current loc
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    System.out.println("Printing LOC");
                    if (mCurrentLocation != null) {
                        System.out.println(String.valueOf(mCurrentLocation.getLatitude()));
                        System.out.println(String.valueOf(mCurrentLocation.getLongitude()));
                    }
                    try {
                        //Go to sleep for 5 seconds
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();

    }

    @Override
    public void onConnectionSuspended(int n) {
        //TODO: STUB
        System.out.println("Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult res) {
        //TODO: STUB
        System.out.println("Connection failed");
    }


    /**
     * Asks the user to download Google Play services if not yet installed
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Default marker
        LatLng def = new LatLng(0, 0);
        mMap.addMarker(new MarkerOptions().position(def).title("Default"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(def));

        //Zoom in
        //TODO: Set max/min zooms for course
        //15 is roughly a person in the neighborhood; higher -> closer
        CameraUpdate defaultZoom = CameraUpdateFactory.zoomTo(15);
        mMap.animateCamera(defaultZoom);
    }
}
