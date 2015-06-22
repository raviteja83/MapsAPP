package com.example.ravi_pc.mapapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    int PLACE_PICKER_REQUEST = 1;
    private final String PLACES_SEARCH = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
    private final String DIRECTIONS = "https://maps.googleapis.com/maps/api/directions/json?";
    LatLng currentLocation,markerdraglatLng;
    Location mLastLocation;
    // private static String current_address;
    AutoCompleteTextView mAutoCompleteTextView;
    PlaceAutoCompleteAdapter mAdapter;
    private GoogleApiClient mGoogleApiClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API)
                .enableAutoManage(this, 0, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.autocomplete_textview);
        mAutoCompleteTextView.setOnItemClickListener(mAutocompleteClickListener);


        Button places  = (Button) findViewById(R.id.places);
        places.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                Context context = getApplicationContext();
                try {
                    startActivityForResult(builder.build(context), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceAutoCompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            Log.i("MAIN", "Autocomplete item selected: " + item.description);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

            Toast.makeText(getApplicationContext(), "Clicked: " + item.description,
                    Toast.LENGTH_SHORT).show();
            Log.i("MAIN", "Called getPlaceById to get Place details for " + item.placeId);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e("MAIN", "Place query did not complete. Error: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);
            LatLng latLng = place.getLatLng();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng,18);
            mMap.animateCamera(cameraUpdate);
            mMap.addMarker(new MarkerOptions().position(latLng)
                    .snippet(place.getAddress().toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title(place.getName().toString()));
            Log.i("MAIN", "Place details received: " + place.getName());
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mAutoCompleteTextView.getWindowToken(), 0);
            places.release();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //setUpMapIfNeeded();
        /*PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                .getCurrentPlace(mGoogleApiClient, null);

        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                   *//* Log.i("PLACES", String.format("Place '%s' has likelihood: %g",
                            placeLikelihood.getPlace().getName(),
                            placeLikelihood.getLikelihood()));*//*
                    LatLng latlng = placeLikelihood.getPlace().getLatLng();
                    String title = placeLikelihood.getPlace().getAddress().toString();
                    mMap.addMarker(new MarkerOptions().position(latlng)
                            .snippet(title)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(placeLikelihood.getPlace().getName().toString()));
                }
                likelyPlaces.release();
            }
        });*/
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                LatLng latlng = place.getLatLng();
                String title = place.getAddress().toString();
                Toast.makeText(this,place.getName().toString(),Toast.LENGTH_SHORT).show();
                mMap.addMarker(new MarkerOptions().position(latlng)
                        .snippet(title)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        .title(place.getName().toString()));
            }
        }
    }


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
        mMap.setMyLocationEnabled(true);
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                Toast.makeText(getApplicationContext(), "Long clicked", Toast.LENGTH_SHORT).show();
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                try {
                    markerdraglatLng = latLng;
                    List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    String address = addresses.get(0).getAddressLine(0) + "," + addresses.get(0).getAddressLine(1);
                    mMap.addMarker(new MarkerOptions().draggable(true)
                            .position(latLng)
                            .snippet(addresses.get(0).getAddressLine(2))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(address));
                    new DirectionsTask().execute(DIRECTIONS+"origin="+currentLocation.latitude+","+currentLocation.longitude+"&destination="+latLng.latitude+","+latLng.longitude
                            +"&units=metric&mode=walking&geodesic=true&key="+getString(R.string.browser_key));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }
            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                try {
                    LatLng latLng = marker.getPosition();
                    Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    String address = addresses.get(0).getAddressLine(0) + "," + addresses.get(0).getAddressLine(1);
                    marker.setTitle(address);
                    marker.setSnippet(addresses.get(0).getAddressLine(2));
                    new PlacesTask().execute(PLACES_SEARCH +
                            "location=" + latLng.latitude + "," + latLng.longitude +
                            "&radius=500"+"&sensor=false"+"&key=" + getString(R.string.browser_key) );
                }catch (Exception e){e.printStackTrace();}
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                mAutoCompleteTextView.setText(marker.getTitle());
                return false;
            }
        });
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    private void drawMarker(Location location) {
        try {
           // mMap.clear();
            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mAdapter = new PlaceAutoCompleteAdapter(this, android.R.layout.simple_list_item_1,
                    mGoogleApiClient, new LatLngBounds(currentLocation,currentLocation), null);
            mAutoCompleteTextView.setAdapter(mAdapter);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLocation, 18);
            mMap.animateCamera(cameraUpdate);
            Toast.makeText(MapsActivity.this, "draw Marker", Toast.LENGTH_SHORT).show();
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            String current_address = addresses.get(0).getAddressLine(0)+","+ addresses.get(0).getAddressLine(1);
            mMap.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .snippet(addresses.get(0).getAddressLine(2))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title(current_address));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            drawMarker(mLastLocation);
            new PlacesTask().execute(PLACES_SEARCH +
                    "location=" + mLastLocation.getLatitude() + "," + mLastLocation.getLongitude() +
                    "&radius=1000"+"&sensor=false&key=" + getString(R.string.browser_key) );
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this,"Make Sure Google services is correctly installed",Toast.LENGTH_SHORT).show();
    }

    private class PlacesTask extends AsyncTask<String,Void, String> {
        String data = null;
        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try{
                System.out.println("=====Background Task===");
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(url[0]).openConnection();
                // Connecting to url
                urlConnection.connect();
                // Reading data from url
                InputStream iStream = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
                StringBuilder sb  = new StringBuilder();
                String line;
                while( ( line = br.readLine())  != null){
                    sb.append(line);
                }
                data = sb.toString();
                br.close();
            }catch(Exception e){
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result){
            ParserTask parserTask = new ParserTask();
            // Start parsing the Google places in JSON format
            // Invokes the "doInBackground()" method of the class ParseTask
            parserTask.execute(result);
        }
    }
    private class DirectionsTask extends AsyncTask<String,Void,String> {
        String data = null;
        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url) {
            try{
                System.out.println("=====Directions Task===");
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(url[0]).openConnection();
                System.out.println(url[0]);
                // Connecting to url
                urlConnection.connect();
                // Reading data from url
                InputStream iStream = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
                StringBuilder sb  = new StringBuilder();
                String line;
                while( ( line = br.readLine())  != null){
                    sb.append(line);
                }
                data = sb.toString();
                br.close();
            }catch(Exception e){
                Log.d("Directions Task", e.toString());
            }
            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result){
            //System.out.println(result);
            try {
                JSONObject data = new JSONObject(result);
                JSONObject object = data.getJSONArray("routes").getJSONObject(0);
                JSONObject overview = object.getJSONObject("overview_polyline");
                //List<String> points = new DirectionsJSONParser().parse(data);
                List<LatLng> points = PolyUtil.decode(overview.getString("points"));
                PolylineOptions polylineOptions = new PolylineOptions();
                for(LatLng point:points)
                    polylineOptions.add(point);
                mMap.addPolyline(polylineOptions.color(Color.RED).width(10).geodesic(true));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>{
        JSONObject jObject;
        // Invoked by execute() method of this object
        @Override
        protected List<HashMap<String,String>> doInBackground(String... jsonData) {
            List<HashMap<String, String>> places = null;
            PlaceJSONParser placeJsonParser = new PlaceJSONParser();
            try{
                jObject = new JSONObject(jsonData[0]);
                /** Getting the parsed data as a List construct */
                places = placeJsonParser.parse(jObject);
            }catch(Exception e){
                Log.d("Exception",e.toString());
            }
            return places;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(List<HashMap<String,String>> list){
            // Clears all the existing markers
            // mMap.clear();
            for(int i=0;i<list.size();i++){
                // Creating a marker
                MarkerOptions markerOptions = new MarkerOptions();
                // Getting a place from the places list
                HashMap<String, String> hmPlace = list.get(i);
                // Getting latitude of the place
                double lat = Double.parseDouble(hmPlace.get("lat"));
                // Getting longitude of the place
                double lng = Double.parseDouble(hmPlace.get("lng"));
                // Getting name
                String name = hmPlace.get("place_name");
                // Getting vicinity
                String vicinity = hmPlace.get("vicinity");
                LatLng latLng = new LatLng(lat, lng);
                // Setting the position for the marker
                markerOptions.position(latLng);
                // Setting the title for the marker.
                //This will be displayed on taping the marker
                markerOptions.title(name + " : " + vicinity);
                // Placing a marker on the touched position
                mMap.addMarker(markerOptions);
            }
        }
    }
}