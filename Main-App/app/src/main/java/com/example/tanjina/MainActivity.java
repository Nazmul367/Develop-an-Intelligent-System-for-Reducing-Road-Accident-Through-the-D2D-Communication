package com.example.tanjina;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {

    public static final int RequestPermissionCode = 1;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private static final int REQUEST_CHECK_SETTINGS = 199;
    TextView latitude, longitude, token_text, v_length;        //TextView longitude;
    String length="";
    // Editing Part
    private TextView speed;

    Button button;
    String server_url = "http://tm.tanjumow.tk/updateLoc.php";
    String delete_url="http://tm.tanjumow.tk/deleteToken.php";
    AlertDialog.Builder builder;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        token=(getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).getString(FirebaseService.KEY_FCM_TOKEN, "No token found"));
        Intent intent=getIntent();
        length=intent.getStringExtra("Length");
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        latitude = (TextView) findViewById(R.id.latitude_text);
        longitude = (TextView) findViewById(R.id.longitude_text);
        token_text = (TextView)findViewById(R.id.token_text);
        v_length = (TextView)findViewById(R.id.vv_length);
        speed = findViewById(R.id.speed);

        Log.d("TOKEN", "TOKEN: " + getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).getString(FirebaseService.KEY_FCM_TOKEN, "No token found"));


        // Editing Part
        button = (Button) findViewById(R.id.btnLogout);
        builder = new AlertDialog.Builder(MainActivity.this);

    }

    private void sendLocationData(final String lat, final String lng, final String speed) {
        token_text.setText(token);
        StringRequest stringRequest = new StringRequest(Method.POST, server_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(MainActivity.this, "Location data successfully send to server!",
                                Toast.LENGTH_SHORT).show();

                    }
                }
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(MainActivity.this, "Error...", Toast.LENGTH_SHORT).show();
                error.printStackTrace();

            }
        }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("latitude", lat);
                params.put("longitude", lng);
                params.put("token_text",token);
                params.put("length",length);
                params.put("speed", speed);
                return params;
            }
        };

        MySingleton.getInstance(MainActivity.this).addTorequestque(stringRequest);
    }
    public void Logout(View view){
        StringRequest stringRequest = new StringRequest(Method.POST, delete_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JSONObject jObj = null;
                        try {
                            jObj = new JSONObject(response);
                            boolean error = jObj.getBoolean("error");
                            if(error){
                                Toast.makeText(MainActivity.this, "Wrong",
                                        Toast.LENGTH_SHORT).show();

                            }else{
                                Toast.makeText(MainActivity.this, "Location data successfully send to server!",
                                        Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }

                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this, e.toString(),
                                    Toast.LENGTH_SHORT).show();
                        }



                    }
                }
                , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(MainActivity.this, "Error...", Toast.LENGTH_SHORT).show();
                error.printStackTrace();

            }
        }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("token_text",token);;
                return params;
            }
        };

        MySingleton.getInstance(MainActivity.this).addTorequestque(stringRequest);
    }


    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        super.onStop();
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Log.e( tag: "MainActivity", msg: "ConnectionFailed : " + connectionResult.getErrorCode() );

    }

    @Override
    public void onConnectionSuspended(int i) {
        //Log.e( tag: "MainActivity", msg: "Connection Suspended");

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();

        } else {

            requestLocationUpdates();
            startLocationUpdates();

            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            setLocation(location);
                        }

                    });
        }
    }

    private void setLocation(Location location) {
        if (location != null) {
            latitude.setText(String.valueOf(location.getLatitude()));
            longitude.setText(String.valueOf(location.getLongitude()));
        }
    }

    /* Remove the location listener updates when Activity is paused */
    @Override
    protected void onPause() {
        stopLocationUpdates();
        super.onPause();
    }

    private void requestLocationUpdates() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (fusedLocationProviderClient != null && mLocationRequest != null && mLocationCallback != null) {
            fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationProviderClient != null && mLocationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        }
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }

            // Current location
            Location currentLocation = locationResult.getLastLocation();
            float speed = locationResult.getLastLocation().getSpeed();
            int speedInKm = (int) ((speed * 3600) / 1000);

            if (currentLocation != null) {
                setLocation(currentLocation);

                sendLocationData(String.valueOf(currentLocation.getLatitude()),
                        String.valueOf(currentLocation.getLongitude()), String.valueOf(speedInKm));

                Toast.makeText(MainActivity.this, "Location changed: " + currentLocation.getLatitude()
                        + " " + currentLocation.getLongitude()+ " Speed: " + speedInKm, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void requestPermissions() {

        ActivityCompat.requestPermissions(MainActivity.this, new
                String[]{ACCESS_FINE_LOCATION}, RequestPermissionCode);
    }
}
