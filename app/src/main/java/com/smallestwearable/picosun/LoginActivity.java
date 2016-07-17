package com.smallestwearable.picosun;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// A login screen that offers login via Google
public class LoginActivity extends AppCompatActivity implements View.OnClickListener,
        //GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback {
        //GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
    private static final long SCAN_PERIOD = 60000; /* 5 seconds */
    protected static final int REQUEST_ENABLE_BT = 1;
    protected static final int REQUEST_CHECK_SETTINGS = 0x2;
    private boolean mScanning = false;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private Handler mHandler = new Handler();

    private static final int RC_SIGN_IN = 0;
    private static final String TAG = "LoginActivity";// Logcat tag

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    private SignInButton btnSignIn;
    private Button btnSignOut, btnBTScan;
    private Button btnRedLED, btnYellowLED, btnGreenLED;
    private ImageView imgProfile;
    private TextView txtName, txtEmail, uvindex, lux;
    private LinearLayout llProfile, llScan, llData;

    public ArrayList<String> mArrayNewDev = new ArrayList<>();
    private ArrayAdapter <String> aNewBTDev;

    int uvValue, uvValueOld, luxValue, luxValueOld;

    GoogleMap googleMap;
    Location mLastLocation;
    /*
    // A flag indicating that a PendingIntent is in progress and prevents us
    // from starting further intents.
    private boolean mIntentInProgress;
    private boolean mSignInClicked;
    private ConnectionResult mConnectionResult;
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnSignIn = (SignInButton) findViewById(R.id.sign_in_button);
        btnSignOut = (Button) findViewById(R.id.sign_out_button);
        btnBTScan = (Button) findViewById(R.id.scan_bt_button);
        btnRedLED = (Button) findViewById(R.id.red_led_bt_button);
        btnYellowLED = (Button) findViewById(R.id.yellow_led_bt_button);
        btnGreenLED = (Button) findViewById(R.id.green_led_bt_button);
        imgProfile = (ImageView) findViewById(R.id.user_ProfilePic);
        txtName = (TextView) findViewById(R.id.user_Name);
        txtEmail = (TextView) findViewById(R.id.user_Email);
        uvindex = (TextView) findViewById(R.id.uv_index_value);
        lux = (TextView) findViewById(R.id.lux_value);
        llProfile = (LinearLayout) findViewById(R.id.llProfile);
        llData = (LinearLayout) findViewById(R.id.llData);
        llScan = (LinearLayout) findViewById(R.id.llScan);

        // Button click listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.scan_bt_button).setOnClickListener(this);
        findViewById(R.id.red_led_bt_button).setOnClickListener(this);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestProfile()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                //.addConnectionCallbacks(this)
                //.addOnConnectionFailedListener(this)
                //.enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addApi(LocationServices.API)
                .addApi(AppIndex.API).build();

        // Create The Adapter with passing ArrayList as 3rd parameter
        aNewBTDev = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, mArrayNewDev);
        ListView lNewBTDev=(ListView)findViewById(R.id.NewBTDev);
        lNewBTDev.setAdapter(aNewBTDev);// Set The Adapter

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mMap);
        mapFragment.getMapAsync(this);

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        final PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {

                final Status status = result.getStatus();
                Log.i("Location Setting Status",String.valueOf(status));
                final LocationSettingsStates LS_state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here. //TODO Initialize location requests here
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(LoginActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
        //Update the UI
        updateUI(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<ScanFilter>();
            //scanLeDevice(true);
        }
        uvValueOld = 0;
        luxValueOld = 0;
    }

/*
    private void GoogleApiClient.ConnectionCallbacks() {
        @Override
        protected void onConnectionSuspended(); {
            super.onConnectionSuspended();
        };
    }
    @Override
    public void onConnected (Bundle connectionHint) {
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            //mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            //mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();
            Log.i("Location Lat:", String.valueOf(latitude));
            Log.i("Location Lon:", String.valueOf(longitude));

            LatLng latLng = new LatLng(latitude, longitude);
            googleMap.addMarker(new MarkerOptions().position(latLng));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }

    }
*/
    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

/*
    public void onConnected(Bundle connectionHint) {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            //mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            //mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
            Log.i("Location Lat:",String.valueOf(mLastLocation.getLatitude()));
            Log.i("Location Lon:",String.valueOf(mLastLocation.getLongitude()));
        }
    }
*/

    @Override
    public void onMapReady(GoogleMap map) {
        double latitude = 29.585219;
        double longitude = -95.129711;
        LatLng latLng = new LatLng(latitude, longitude);
        map.addMarker(new MarkerOptions().position(latLng).title("Home"));
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        map.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

/*
    @Override
    public void onLocationChanged(Location location) {
        TextView locationTv = (TextView) findViewById(R.id.latlongLocation);
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);
        googleMap.addMarker(new MarkerOptions().position(latLng));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        locationTv.setText("Latitude:" + latitude + ", Longitude:" + longitude);
    }
*/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.scan_bt_button:
                scanLeDevice(true);
                break;
            case R.id.red_led_bt_button:
                toggleRedLED();
                break;
        }
    }
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate our menu from the resources by using the menu inflater.
        getMenuInflater().inflate(R.menu.main, menu);

        // It is also possible add items here. Use a generated id from
        // resources (ids.xml) to ensure that all menu ids are distinct.
        MenuItem locationItem = menu.add(0, R.id.menu_location, 0, R.string.menu_location);
        locationItem.setIcon(R.drawable.ic_action_location);

        // Need to use MenuItemCompat methods to call any action item related methods
        MenuItemCompat.setShowAsAction(locationItem, MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }
*/
    private void toggleRedLED(){
        if(mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        BluetoothDevice picoWearUV = mBluetoothAdapter.getRemoteDevice(String.valueOf("EC:95:D4:9A:82:2A"));
        if (picoWearUV.getBondState() == BluetoothDevice.BOND_NONE){
            picoWearUV.createBond();
        }

/*
        if(picoWearUV.getBondState()== BluetoothDevice.BOND_BONDED) {
            ParcelUuid[] uuid = picoWearUV.getUuids();
            picoWearUV.createRfcommSocketToServiceRecord();
        }
*/
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);
            mScanning = true;

            //Set up scan settings and filter for a PicoWear device
            settings = new ScanSettings.Builder()
                    .setReportDelay(0)
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build();
            ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceName("PicoWear_01")
                    .build();
            filters.add(filter);
            mLEScanner.startScan(filters, settings, mScanCallback);

            aNewBTDev.setNotifyOnChange(true);
            aNewBTDev.clear();
            aNewBTDev.notifyDataSetChanged();
        } else {
            mScanning = false;
            mLEScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getDevice().getName() != null) {
                if (result.getDevice().getName().equals("PicoWear_01")) {
                    Log.i("callbackType", String.valueOf(callbackType));
                    Log.i("result", result.toString());
                    Log.i("Device Name", result.getDevice().getName());

                    mArrayNewDev.clear();
                    mArrayNewDev.add(result.getDevice().getName() + "    " + result.getRssi()
                            + "\n" + result.getDevice());
                    ListView lNewBTDev=(ListView)findViewById(R.id.NewBTDev);
                    lNewBTDev.invalidateViews();

                    byte[] data = result.getScanRecord().getManufacturerSpecificData().valueAt(0);
                    uvValue = (int)data[1];
                    //luxValue = data[4];
                    luxValue = (((int)data[3] * 256 + (int)data[4]) * 2);
                    Log.i("Size of Mfg Data", Integer.toString(data.length));
                    Log.i("UV", String.valueOf(uvValue));
                    Log.i("Previous UV", String.valueOf(uvValueOld));
                    Log.i("Lux", String.valueOf(luxValue));

                    uvindex.setText(String.valueOf(uvValue));
                    lux.setText(String.valueOf(luxValue));

                    //Set notification if UV value changed
                    if (uvValueOld != uvValue) {
                        Log.i("Notification triggered", "ID = 1");
                        Notify("UV Update", "Current UV Index is : " + String.valueOf(uvValue), 1);
                        uvValueOld = uvValue;
                    }
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
            switch (errorCode) {//TODO add toast for each fail mode
                case 1:
                    //Toast.makeText(this, "Scan is currently ongoing...", Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
            }
        }
    };

    private void Notify(String notificationTitle, String notificationMessage, int mId){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.transparentlogo)
                        .setContentTitle("PicoSun " + notificationTitle)
                        .setContentText(notificationMessage)
                        .setColor(0x00)
                        .setLights(0xffff0000,100,900);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, Notification.class);

        // The stack builder object will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(Notification.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mBuilder.build());
    }
    /*
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
        updateUI(false);
    }

    @Override
    public void onConnected(Bundle arg0) {
        mSignInClicked = false;
        Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();
        //getProfileInformation(); // Get user's information
        updateUI(true); // Update the UI after signin
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }
        if (!mIntentInProgress) {
            mConnectionResult = result;// Store the ConnectionResult for later usage
            if (mSignInClicked) {
                // The user has already clicked 'sign-in' so we attempt to
                // resolve all errors until the user is signed in, or they cancel.
                resolveSignInError();
            }
        }
    }

    private void resolveSignInError() {
        if (mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    private class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public LoadProfileImage(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
*/
    private void signIn() {
        if (!mGoogleApiClient.isConnecting()) {
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, RC_SIGN_IN);
            //mSignInClicked = true;
            //resolveSignInError();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }

        if (requestCode == REQUEST_CHECK_SETTINGS){
            final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
            switch (requestCode) {
                case REQUEST_CHECK_SETTINGS:
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            // All required changes were successfully made
                            //GetUserLocation();//TODO: FINALLY YOUR OWN METHOD TO GET YOUR USER LOCATION HERE
                            break;
                        case Activity.RESULT_CANCELED:
                            // The user was asked to change settings, but chose not to
                            Toast.makeText(LoginActivity.this, "Need location to work ... quitting App", Toast.LENGTH_LONG).show();
                            finish();
                        default:
                            break;
                    }
                    break;
            }
        }

    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            Toast.makeText(this, "User is signed in", Toast.LENGTH_LONG).show();
            GoogleSignInAccount acct = result.getSignInAccount();
            String sFullName = acct.getDisplayName();
            txtName.setText(sFullName);
            String sEmail = acct.getEmail();
            txtEmail.setText(sEmail);

            Uri uPhoto = acct.getPhotoUrl();
            //System.out.println(uPhoto);
            //imgProfile.setImageURI(null);
            //imgProfile.setImageURI(uPhoto);
            Log.i("Login Info ",sFullName + " " + sEmail);

            InputStream imageStream = null;
            try {
                Log.i("Showing picture","TRUE");
                imageStream = getContentResolver().openInputStream(uPhoto);
                imgProfile.setImageBitmap(BitmapFactory.decodeStream(imageStream));
            } catch (FileNotFoundException e) {
                Log.e("Image Display", "File not FOUND");
            } finally {
                if (imageStream != null) {
                    try {
                        imageStream.close();
                    } catch (IOException e) {
                        // Ignore the exception
                    }
                }
            }
            updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }

    private void signOut() {
        //if (mGoogleApiClient.isConnected()) {
            Log.i("SignOut", "Signing out ...");
            //Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            //Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            updateUI(false);
        //}
    }

    private void updateUI(boolean isSignedIn) {
        ListView lNewBTDev=(ListView)findViewById(R.id.NewBTDev);

        if (isSignedIn) {
            btnSignIn.setVisibility(View.GONE);
            btnSignOut.setVisibility(View.VISIBLE);
            btnBTScan.setVisibility(View.VISIBLE);
            btnRedLED.setVisibility(View.VISIBLE);
            btnYellowLED.setVisibility(View.VISIBLE);
            btnGreenLED.setVisibility(View.VISIBLE);
            llProfile.setVisibility(View.VISIBLE);
            llScan.setVisibility(View.VISIBLE);
            llData.setVisibility(View.VISIBLE);
            lNewBTDev.setVisibility(View.VISIBLE);
        } else {
            btnSignIn.setVisibility(View.VISIBLE);
            btnSignOut.setVisibility(View.GONE);
            btnBTScan.setVisibility(View.GONE);
            btnRedLED.setVisibility(View.GONE);
            btnYellowLED.setVisibility(View.GONE);
            btnGreenLED.setVisibility(View.GONE);
            llProfile.setVisibility(View.GONE);
            llScan.setVisibility(View.GONE);
            llData.setVisibility(View.GONE);
            lNewBTDev.setVisibility(View.GONE);
        }
    }
}

