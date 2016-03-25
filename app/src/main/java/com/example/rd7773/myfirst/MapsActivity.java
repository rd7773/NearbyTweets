package com.example.rd7773.myfirst;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

import static com.google.android.gms.location.LocationSettingsStatusCodes.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback ,GoogleApiClient.ConnectionCallbacks ,GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private static SharedPreferences mSharedPreferences;
    private GoogleApiClient googleApiClient;
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final int REQUEST_PERM_LOCATION = 100;
    RelativeLayout rlMain;
    private boolean permission_granted = false;
    Location mLastLocation;
    SupportMapFragment mapFragment;
    Context ctx;
    List<Status> newTweets;
    List<Marker> markersList;
    Twitter twitter;
    HashMap<String , Status> tweetsList;
    long latestTweetId =0;
    Marker shownMarker;
    ImageView refresh;
    TimerTask mTimerTask;
    Timer mTimer;
    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        setContentView(R.layout.activity_maps);
        rlMain = (RelativeLayout) findViewById(R.id.rlMain);
        refresh = (ImageView) findViewById(R.id.refresh);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.

        handler = new Handler(Looper.getMainLooper()){

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                for(int i=0;i<newTweets.size();i++){
                    twitter4j.Status tweet = newTweets.get(i);
                    GeoLocation geoLoc = tweet.getGeoLocation();
                    if(geoLoc!=null) {
                        LatLng loc = new LatLng(geoLoc.getLatitude(), geoLoc.getLongitude());
                        Marker marker = mMap.addMarker(new MarkerOptions().position(loc));
                        markersList.add(marker);
                        tweetsList.put(marker.getId(),tweet);
                    }

                }


                if(markersList.size()>100){

                    for(int i=0;i<markersList.size()-100;i++){

                        markersList.get(0).remove();
                        markersList.remove(0);
                    }

                }

                newTweets.clear();


            }
        };

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(AppIndex.API).build();

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("q9mgBy4O422gfZAYzorgoTjV0")
                .setOAuthConsumerSecret("Qk0rwJu2h4Hb5fp74pXef27NTm5AbY9EePknTHK01CYEtWES5Q")
                .setOAuthAccessToken("158087401-GBqVtpqBx3Kt8QcDt9zwvEoI9cbKO65omEhGg2m2")
                .setOAuthAccessTokenSecret("FHbWL7R16nkwx2BUrV6eGz6QKPEloQo0615aclWv6OiuM");

        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter = tf.getInstance();

        markersList = new ArrayList<>(100);
        newTweets = new ArrayList<>();
        tweetsList = new HashMap<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            requestLocationPermission(this);

        }else{

            permission_granted = true;
            displayPromptForEnablingGPS();


        }


        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(permission_granted){
            try {
                mMap.setMyLocationEnabled(true);
            }catch (SecurityException e){
                e.printStackTrace();
            }
        }

        //googleMap.clear();
        //new FetchTweets().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        if(mLastLocation!=null) {

            launchTimerTask();
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title("You are here"));
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, 14);
            googleMap.animateCamera(update);
            mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if(permission_granted){
            try {
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                        googleApiClient);
            }catch (SecurityException e){
                e.printStackTrace();
            }

            if(mLastLocation==null)
            {
                new FetchLastLocation().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }else
                mapFragment.getMapAsync(this);

        }else
            mapFragment.getMapAsync(this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    public void displayPromptForEnablingGPS()
    {

        LocationManager lm = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;


        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {

            settingsrequest();

        }else{
            if(googleApiClient.isConnected()){
                googleApiClient.disconnect();
                googleApiClient.connect();

            }else
                googleApiClient.connect();
        }
    }

    public void settingsrequest()
    {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true); //this is the key ingredient

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final com.google.android.gms.common.api.Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case SUCCESS:
                        if(googleApiClient.isConnected()){
                            googleApiClient.disconnect();
                            googleApiClient.connect();

                        }else
                            googleApiClient.connect();
                        break;
                    case RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
// Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        break;
                    case Activity.RESULT_CANCELED:

                        final Snackbar snackbar = Snackbar.make(rlMain,
                                "Permission to fetch location was denied earlier. Allow app to use device location.", Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction("Open Settings", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivityForResult(intent,2);
                                snackbar.dismiss();
                            }
                        });

                        snackbar.show();
                        settingsrequest();//keep asking if imp or do whatever
                        break;
                }
                break;
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERM_LOCATION) {
            if(grantResults.length == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                permission_granted = true;
                displayPromptForEnablingGPS();
            } else {

                permission_granted = false;
                //if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
                //{
                final Snackbar snackbar = Snackbar.make(rlMain,
                        "Permission to fetch location was denied earlier. Allow app to use device location.", Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction("Open Settings", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent,2);
                        snackbar.dismiss();
                    }
                });

                snackbar.show();
                //}

                googleApiClient.connect();
                // Permission was denied or request was cancelled
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        googleApiClient.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.rd7773.myfirst/http/host/path")
        );
        AppIndex.AppIndexApi.start(googleApiClient, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();
        finishTimerTask();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.rd7773.myfirst/http/host/path")
        );
        AppIndex.AppIndexApi.end(googleApiClient, viewAction);
        googleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestLocationPermission(final Activity ctx){

        if (ContextCompat.checkSelfPermission(ctx,Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(ctx,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {


                showDialog("This app searches community area that are"
                        + " defined nearby user's current location which"
                        + " helps user find relevant communities to join."
                        + " To access current location , app needs permission from you."
                        + " It seems you have not provided permission earlier. Please re-consider!","Google Location");

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(ctx,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERM_LOCATION);

                // requestCode is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }else{
            ctx.onRequestPermissionsResult(REQUEST_PERM_LOCATION,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION },
                    new int[]{0,0});
        }
    }

    void showDialog(String msg , String title){

        AlertDialog dialog = new AlertDialog.Builder(this).setMessage(msg).setTitle(title)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ActivityCompat.requestPermissions(MapsActivity.this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_PERM_LOCATION);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        onRequestPermissionsResult(REQUEST_PERM_LOCATION,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION },
                                new int[]{-1,-1});

                        dialog.cancel();
                    }
                }).create();

        dialog.show();

    }


    private class FetchLastLocation extends AsyncTask<Void, Void, Void> {
        ProgressDialog mProgress;
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            mProgress = new ProgressDialog(ctx);
            mProgress.setMessage("Finding current location");
            mProgress.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub

            try {
                while(mLastLocation==null)
                {
                    try {
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                                googleApiClient);
                        Thread.sleep(1000);
                    }catch (SecurityException e){
                        e.printStackTrace();
                    }
                }

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            if(mProgress.isShowing())
                mProgress.cancel();

            mapFragment.getMapAsync(MapsActivity.this);
            super.onPostExecute(result);
        }

    }


    private class FetchTweets extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            try {
                Query query = new Query("India"); //

                GeoLocation location = new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()); //latitude, longitude

                query.setGeoCode(location, 1, Query.MILES); //location, radius, unit
                query.setSinceId(latestTweetId);
                QueryResult result;

                do {
                    result = twitter.search(query);


                    for (twitter4j.Status tweet : result.getTweets()) {

                        if(tweet.getGeoLocation()!=null){

                            newTweets.add(tweet);
                            long id = tweet.getId();

                            if(id>latestTweetId){

                                latestTweetId = id;
                            }

                        }

                        System.out.println("@" + tweet.getUser().getScreenName() + " - " + tweet.getText());
                    }

                } while ((query = result.nextQuery()) != null);

            } catch (TwitterException te) {
                System.out.println("Failed to search tweets: " + te.getMessage());
            }
           return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            for(twitter4j.Status tweet : newTweets){

                GeoLocation geoLoc = tweet.getGeoLocation();
                if(geoLoc!=null) {
                    LatLng loc = new LatLng(geoLoc.getLatitude(), geoLoc.getLongitude());
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .snippet(tweet.getText()).
                            position(loc).title("@" +tweet.getUser().getScreenName()));
                    tweetsList.put(marker.getId(),tweet);
                    markersList.add(marker);
                }

            }

            newTweets.clear();

            super.onPostExecute(result);
        }

    }

    class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View myContentsView;
        ImageLoader imageLoader = AppController.getInstance().getImageLoader();
        MyInfoWindowAdapter(){
            myContentsView = getLayoutInflater().inflate(R.layout.info_window, null);
        }

        @Override
        public View getInfoContents(final Marker marker) {

            shownMarker = marker;

            TextView tvName = ((TextView)myContentsView.findViewById(R.id.name));
            TextView tvTime = ((TextView)myContentsView.findViewById(R.id.timestamp));
            TextView tvTweet = ((TextView)myContentsView.findViewById(R.id.tvDescription));
            TextView tvTitle = ((TextView)myContentsView.findViewById(R.id.tvTitle));
            TextView tvReTweetCount = ((TextView)myContentsView.findViewById(R.id.tvRetweetCount));
            TextView tvLikeCount = ((TextView)myContentsView.findViewById(R.id.tvLikeCount));

            CircleImageView ivPic = (CircleImageView) myContentsView.findViewById(R.id.profilePic);

            Status tweet = tweetsList.get(marker.getId());

            if(tweet!=null) {
                User user = tweet.getUser();
                tvName.setText(user.getName());
                tvTitle.setText("@"+user.getScreenName());
                tvTweet.setText(tweet.getText());

                CharSequence tweetTime = DateUtils.getRelativeTimeSpanString(
                        tweet.getCreatedAt().getTime(),
                        System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS);

                tvTime.setText(tweetTime);
                tvReTweetCount.setText(format(tweet.getRetweetCount()) + " Retweets");
                tvLikeCount.setText(format(tweet.getFavoriteCount()) + " Likes");

                String image = tweet.getUser().getMiniProfileImageURL();

                if (image != null && image.trim().length() > 0) {

                    ivPic.setResponseObserver(new VolleyImageView.ResponseObserver() {
                        @Override
                        public void onError() {

                            if (marker.isInfoWindowShown())
                                marker.hideInfoWindow();
                        }

                        @Override
                        public void onSuccess() {
                            getInfoWindow(marker);
                        }
                    });
                    ivPic.setDefaultImageResId(R.drawable.profile27);
                    ivPic.setImageUrl(tweet.getUser().getMiniProfileImageURL(), imageLoader);
                } else {

                    ivPic.setImageResource(R.drawable.profile27);
                }
                return myContentsView;
            }

            return null;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            // TODO Auto-generated method stub

            if(shownMarker!=null&&shownMarker.isInfoWindowShown()){
                shownMarker.hideInfoWindow();
                shownMarker.showInfoWindow();

            }

            return null;
        }

    }


    private static String[] suffix = new String[]{"","k", "m", "b", "t"};
    private static int MAX_LENGTH = 4;

    public static String format(double number) {
        String r = new DecimalFormat("##0E0").format(number);
        r = r.replaceAll("E[0-9]", suffix[Character.getNumericValue(r.charAt(r.length() - 1)) / 3]);
        while(r.length() > MAX_LENGTH || r.matches("[0-9]+\\.[a-z]")){
            r = r.substring(0, r.length()-2) + r.substring(r.length() - 1);
        }
        return r;
    }

    private void launchTimerTask() {

        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                // Perform your recurring method calls in here.
                try {

                    try {
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                                googleApiClient);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }

                    if (mLastLocation != null) {


                        Query query = new Query("India"); //

                        GeoLocation location = new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()); //latitude, longitude

                        query.setGeoCode(location, 5, Query.MILES); //location, radius, unit
                        query.setSinceId(latestTweetId);
                        QueryResult result;

                        do {
                            result = twitter.search(query);

                            for (twitter4j.Status tweet : result.getTweets()) {

                                if (tweet.getGeoLocation() != null) {

                                    newTweets.add(tweet);
                                    long id = tweet.getId();

                                    if (id > latestTweetId) {

                                        latestTweetId = id;
                                    }

                                }

                                System.out.println("@" + tweet.getUser().getScreenName() + " - " + tweet.getText());
                            }

                        } while ((query = result.nextQuery()) != null);

                        handler.sendEmptyMessage(1);
                    }

                    }catch(TwitterException te){
                        System.out.println("Failed to search tweets: " + te.getMessage());
                    }





            }
        };
        mTimer.schedule(mTimerTask,0,10000);
    }

    private void finishTimerTask() {
        if (mTimerTask != null) {
            mTimerTask.cancel();
        }
        if (mTimer != null) {
            mTimer.purge();
            mTimer.cancel();
        }
    }

}

