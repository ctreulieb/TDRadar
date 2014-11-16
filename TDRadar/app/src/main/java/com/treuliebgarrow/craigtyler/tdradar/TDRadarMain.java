package com.treuliebgarrow.craigtyler.tdradar;

import android.content.res.XmlResourceParser;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.provider.SyncStateContract;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class TDRadarMain extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private ArrayList<Location> tdLocations;
    private Marker userLoc;
    Location currentLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tdradar_main);
        setUpMapIfNeeded();
        tdLocations = new ArrayList<Location>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        //get location

        //find nearby ATMS/Branches
        XmlResourceParser locations = getApplicationContext().getResources().getXml(R.xml.tdloc);
        try{
            int eventType = locations.getEventType();
            double lat = 0;
            Location bLoc = new Location("TDLoc");
            locations.next();
            while(eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG && locations.getName().equalsIgnoreCase("lat"))
                {
                    bLoc.setLatitude(Double.parseDouble(locations.nextText()));
                }else if(eventType == XmlPullParser.START_TAG && locations.getName().equalsIgnoreCase("long"))
                {
                    bLoc.setLongitude(Double.parseDouble(locations.nextText()));
                    tdLocations.add(new Location(bLoc));
                }
                eventType = locations.next();
            }
        }catch (XmlPullParserException e) {
            System.out.println("XMLPullParserException - " + e.getMessage());
        }catch (IOException e) {
            System.out.println("IOException - " + e.getMessage());
        }
        //sort list by distance
        Collections.sort(tdLocations, new TDLocationComparator(currentLoc));
        //place pins
        for(int i = 0; i < 3; ++i){
            mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(tdLocations.get(i).getLatitude(),tdLocations.get(i).getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            );
        }
    }

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
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
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
            }
        }
    }


    private void setUpMap() {
        GPSService gps = new GPSService(getBaseContext());
        currentLoc = gps.getLocation();
        //put pin at current location
        if(currentLoc != null)
        {
            LatLng llCLoc = new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(llCLoc, 13));
            userLoc = mMap.addMarker(new MarkerOptions()
                            .position(llCLoc)
            );
        }
    }

}
