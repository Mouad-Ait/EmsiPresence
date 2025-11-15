package com.emsi.emsipresence;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

public class DirectionsHelper {
    private static final String API_KEY = "AIzaSyBrjU1slQIbG3NChnFr_EXiSAUDmCGIDiM";

    public static DirectionsResult getDirections(LatLng origin, LatLng destination) {
        try {
            GeoApiContext context = new GeoApiContext.Builder()
                    .apiKey(API_KEY)
                    .build();

            return DirectionsApi.newRequest(context)
                    .mode(TravelMode.DRIVING)
                    .origin(new com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                    .destination(new com.google.maps.model.LatLng(destination.latitude, destination.longitude))
                    .await();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}