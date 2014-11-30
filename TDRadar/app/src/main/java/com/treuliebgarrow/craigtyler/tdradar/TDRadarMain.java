package com.treuliebgarrow.craigtyler.tdradar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/*
* TDRadarMain - Class that houses the logic for the TDRadar application
* Authors - Craig Treulieb, Tyler Garrow
* Date - 11/30/2014
* */
public class TDRadarMain extends FragmentActivity implements GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Location userLocation;
    private LatLngBounds.Builder builder;
    private Polyline dirLine;
    private boolean mapReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tdradar_main);
        setUpMapIfNeeded();
        //Positions the camera the first time once the map is loaded and ready
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if(builder != null){
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 85));
                }
                mapReady = true;
                mMap.setOnCameraChangeListener(null);
            }
        });
    }

    @Override
    protected void onResume() {
        ArrayList<Marker> markers = new ArrayList<Marker>();
        ArrayList<Location> tdLocations = new ArrayList<Location>();
        super.onResume();
        setUpMapIfNeeded();
        mMap.clear();
        GPSService gps = new GPSService(getBaseContext());
        userLocation = gps.getLocation();
        gps.closeGPS();
        //If location us unavailable prompt user to turn on their Location/Network services
        if(userLocation == null){

            AlertDialog.Builder mAlertDialog = new AlertDialog.Builder(TDRadarMain.this);
            mAlertDialog.setTitle("Location not available, Open GPS?")
                    .setMessage("Activate GPS to use use location services?")
                    .setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            TDRadarMain.this.startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).show();
        }else{
            LatLng llCLoc = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(llCLoc, 13));

            //Add marker at users current location
            markers.add(mMap.addMarker(new MarkerOptions()
                    .position(llCLoc)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.person))));

            //Parse XML for TD Locations
            XmlResourceParser locations = getApplicationContext().getResources().getXml(R.xml.tdloc);
            try{
                int eventType = locations.getEventType();
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
            Collections.sort(tdLocations, new TDLocationComparator(userLocation));

            //place pins
            for(int i = 0; i < 3; ++i){
                markers.add(mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(tdLocations.get(i).getLatitude(), tdLocations.get(i).getLongitude()))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.atm))
                ));
            }
            builder = new LatLngBounds.Builder();
            for(Marker m : markers) {
                builder.include(m.getPosition());
            }

            //Move/zoom map to show all markers
            if(mapReady)
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 85));
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
                mMap.setOnMarkerClickListener(this);
                setUpMap();
            }
        }
    }


    private void setUpMap() {
        //Disable zoom in/out buttons since app handles zoom/placement of map. Users can still pinch and pan map if they would like.
        mMap.getUiSettings().setZoomControlsEnabled(false);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        if(isNetworkAvailable())
            new getDirections(new LatLng(userLocation.getLatitude(),userLocation.getLongitude()),marker.getPosition()).execute();

        return true;
    }

    //getDirections class. Constructor takes in a star and end LatLng and will draw the directions route on the map
    private class getDirections extends AsyncTask<Void, Void, ArrayList<LatLng>> {
        private LatLng start;
        private LatLng end;
        public getDirections(LatLng start, LatLng end)
        {
            this.start = start;
            this.end = end;
        }
        @Override
        protected ArrayList<LatLng> doInBackground(Void... params){
            String url = "http://maps.googleapis.com/maps/api/directions/xml?"
                    + "origin=" + start.latitude + "," + start.longitude
                    + "&destination=" + end.latitude + "," + end.longitude
                    + "&sensor=false&units=metric&mode=driving";

            Log.d("GoogleMapsDirection", url);
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpContext localContext = new BasicHttpContext();
                HttpPost httpPost = new HttpPost(url);
                HttpResponse response = httpClient.execute(httpPost, localContext);
                InputStream in = response.getEntity().getContent();
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = builder.parse(in);
                NodeList nl1, nl2, nl3;
                ArrayList<LatLng> listGeopoints = new ArrayList<LatLng>();
                nl1 = doc.getElementsByTagName("step");
                if (nl1.getLength() > 0) {
                    for (int i = 0; i < nl1.getLength(); i++) {
                        Node node1 = nl1.item(i);
                        nl2 = node1.getChildNodes();

                        Node locationNode = nl2.item(getNodeIndex(nl2, "start_location"));
                        nl3 = locationNode.getChildNodes();
                        Node latNode = nl3.item(getNodeIndex(nl3, "lat"));
                        double lat = Double.parseDouble(latNode.getTextContent());
                        Node lngNode = nl3.item(getNodeIndex(nl3, "lng"));
                        double lng = Double.parseDouble(lngNode.getTextContent());
                        listGeopoints.add(new LatLng(lat, lng));

                        locationNode = nl2.item(getNodeIndex(nl2, "polyline"));
                        nl3 = locationNode.getChildNodes();
                        latNode = nl3.item(getNodeIndex(nl3, "points"));
                        ArrayList<LatLng> arr = decodePoly(latNode.getTextContent());
                        for (LatLng anArr : arr) {
                            listGeopoints.add(new LatLng(anArr.latitude, anArr.longitude));
                        }

                        locationNode = nl2.item(getNodeIndex(nl2, "end_location"));
                        nl3 = locationNode.getChildNodes();
                        latNode = nl3.item(getNodeIndex(nl3, "lat"));
                        lat = Double.parseDouble(latNode.getTextContent());
                        lngNode = nl3.item(getNodeIndex(nl3, "lng"));
                        lng = Double.parseDouble(lngNode.getTextContent());
                        listGeopoints.add(new LatLng(lat, lng));
                    }
                }

                return listGeopoints;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(ArrayList<LatLng> dir){
            LatLngBounds.Builder dirBuilder = new LatLngBounds.Builder();
            PolylineOptions dirLineOptions = new PolylineOptions().width(5).color(Color.RED);
            for (LatLng aDir : dir) {
                dirLineOptions.add(aDir);
                dirBuilder.include(aDir);
            }
            if(dirLine != null) {
                dirLine.remove();
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(dirBuilder.build(), 85));
            dirLine = mMap.addPolyline(dirLineOptions);
        }

        private int getNodeIndex(NodeList nl, String nodename) {
            for(int i = 0 ; i < nl.getLength() ; i++) {
                if(nl.item(i).getNodeName().equals(nodename))
                    return i;
            }
            return -1;
        }

        private ArrayList<LatLng> decodePoly(String encoded) {
            ArrayList<LatLng> poly = new ArrayList<LatLng>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;
            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;
                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng position = new LatLng((double) lat / 1E5, (double) lng / 1E5);
                poly.add(position);
            }
            return poly;
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
