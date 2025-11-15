package com.emsi.emsipresence;


import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.List;

public class LocalisationActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int GPS_REQUEST_CODE = 2;
    private Polyline currentRoute;
    private Button btnStartNavigation;
    private boolean isNavigating = false;
    private LatLng destination;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Marker currentLocationMarker;
    private GeoApiContext geoApiContext;

    // Coordonnées des écoles EMSI à Casablanca
    private final LatLng[] emsiLocations = {
            new LatLng(33.5893598, -7.6052136), // EMSI centre1
            new LatLng(33.5413701, -7.6731040), // EMSI Oranger
            new LatLng(33.5815334, -7.6335085), // EMSI roudani
            new LatLng(33.583655, -7.642290)  // EMSI maarif
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localisation);

        // Initialisation de l'API Directions
        geoApiContext = new GeoApiContext.Builder()
                .apiKey("AIzaSyCAiGKU8aLbF4isjlKZ6s2O5Qgar8HCll4")
                .build();

        checkGPSEnabled();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btnStartNavigation = findViewById(R.id.btn_start_navigation);
        btnStartNavigation.setVisibility(Button.GONE);
        btnStartNavigation.setOnClickListener(v -> {
            if (isNavigating) {
                stopNavigation();
            } else {
                startNavigation();
            }
        });

        Button btnLocate = findViewById(R.id.btn_locate);
        btnLocate.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                getCurrentLocation();
            }
        });

        setupLocationRequest();
    }

    private void setupLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || !isNavigating) return;

                Location location = locationResult.getLastLocation();
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                updateCurrentLocationMarker(currentLatLng);
                updateRoute(currentLatLng, destination);
            }
        };
    }





    private class GetDirectionsTask extends AsyncTask<LatLng, Void, DirectionsResult> {
        @Override
        protected DirectionsResult doInBackground(LatLng... latLngs) {
            try {
                return DirectionsApi.newRequest(geoApiContext)
                        .origin(new com.google.maps.model.LatLng(latLngs[0].latitude, latLngs[0].longitude))
                        .destination(new com.google.maps.model.LatLng(latLngs[1].latitude, latLngs[1].longitude))
                        .mode(TravelMode.DRIVING)
                        .await();
            } catch (Exception e) {
                Log.e("DirectionsError", "Erreur API Directions", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(DirectionsResult result) {
            if (result != null && result.routes != null && result.routes.length > 0) {
                List<LatLng> path = new ArrayList<>();
                for (com.google.maps.model.LatLng point : result.routes[0].overviewPolyline.decodePath()) {
                    path.add(new LatLng(point.lat, point.lng));
                }

                drawRoute(path);
                showNavigationSteps(result);
            } else {
                Toast.makeText(LocalisationActivity.this,
                        "Impossible de calculer l'itinéraire",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void drawRoute(List<LatLng> path) {
        runOnUiThread(() -> {
            if (currentRoute != null) {
                currentRoute.remove();
            }

            currentRoute = mMap.addPolyline(new PolylineOptions()
                    .addAll(path)
                    .width(12)
                    .color(Color.BLUE)
                    .geodesic(true));

            // Ajuster la vue de la carte
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : path) {
                builder.include(point);
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        });
    }

    private void showNavigationSteps(DirectionsResult result) {
        if (result.routes[0].legs[0].steps != null) {
            StringBuilder steps = new StringBuilder("Instructions de navigation :\n\n");
            for (int i = 0; i < result.routes[0].legs[0].steps.length; i++) {
                steps.append(i + 1).append(". ")
                        .append(result.routes[0].legs[0].steps[i].htmlInstructions.replaceAll("<[^>]*>", ""))
                        .append(" (").append(result.routes[0].legs[0].steps[i].distance.humanReadable).append(")\n\n");
            }

            new AlertDialog.Builder(this)
                    .setTitle("Itinéraire vers EMSI")
                    .setMessage(steps.toString())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void updateCurrentLocationMarker(LatLng position) {
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }
        currentLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(position)
                .title("Votre position")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
    }

    private void updateRoute(LatLng origin, LatLng destination) {
        new Thread(() -> {
            DirectionsResult result = DirectionsHelper.getDirections(origin, destination);

            if (result != null && result.routes != null && result.routes.length > 0) {
                List<LatLng> path = new ArrayList<>();

                // Extraire tous les points de l'itinéraire
                for (com.google.maps.model.LatLng point : result.routes[0].overviewPolyline.decodePath()) {
                    path.add(new LatLng(point.lat, point.lng));
                }

                runOnUiThread(() -> {
                    // Supprimer l'ancien itinéraire
                    if (currentRoute != null) {
                        currentRoute.remove();
                    }

                    // Dessiner le nouvel itinéraire
                    currentRoute = mMap.addPolyline(new PolylineOptions()
                            .addAll(path)
                            .width(12)
                            .color(Color.BLUE)
                            .geodesic(true));

                    // Ajuster la caméra
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(origin);
                    builder.include(destination);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                });
            }
        }).start();
    }

    private void checkGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSEnableDialog();
        }
    }

    private void showGPSEnableDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Activation du GPS")
                .setMessage("L'application nécessite l'activation du GPS pour la navigation")
                .setPositiveButton("Activer", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, GPS_REQUEST_CODE);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_REQUEST_CODE) {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(this, "Le GPS n'est pas activé", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.setOnMarkerClickListener(this);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        for (LatLng location : emsiLocations) {
            mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("EMSI Casablanca")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        LatLng position = marker.getPosition();

        // Ouvrir Google Maps avec l’itinéraire vers ce marker
        String uri = "https://www.google.com/maps/dir/?api=1"
                + "&destination=" + position.latitude + "," + position.longitude
                + "&travelmode=driving";

        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        // Vérifie que Google Maps est installé
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Google Maps n'est pas installé", Toast.LENGTH_SHORT).show();
        }

        return true; // Consomme l’événement du clic
    }


    private void showNavigationOptions() {
        new AlertDialog.Builder(this)
                .setTitle("Navigation vers EMSI")
                .setMessage("Voulez-vous démarrer la navigation vers ce campus?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    btnStartNavigation.setVisibility(Button.VISIBLE);
                    getCurrentLocationAndDrawRoute();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private void getCurrentLocationAndDrawRoute() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null && destination != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        updateRoute(currentLatLng, destination);

                        // Afficher les instructions
                        new Thread(() -> {
                            DirectionsResult result = DirectionsHelper.getDirections(currentLatLng, destination);
                            runOnUiThread(() -> showNavigationSteps(result));
                        }).start();
                    }
                });
    }

    private void startNavigation() {
        if (destination == null) return;

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        isNavigating = true;
        btnStartNavigation.setText("Arrêter la navigation");
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopNavigation() {
        isNavigating = false;
        btnStartNavigation.setText("Commencer la navigation");
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    mMap.setMyLocationEnabled(true);
                }
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        updateCurrentLocationMarker(currentLatLng);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isNavigating) {
            stopNavigation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (geoApiContext != null) {
            geoApiContext.shutdown();
        }
    }
}