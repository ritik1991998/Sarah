package com.triointeli.sarah;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.triointeli.sarah.DatabaseModels.Reminder;
import com.triointeli.sarah.DatabaseModels.YourPlaces;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private static final int GEOFENCE_RADIUS = 200; //in meter
    private static final String GEOFENCE_REQ_ID = "My Geofence";

    private RecyclerView mRecyclerView;
    public static RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private EditText tmpToStoreAddLocnName;
    private String tmpEditTextLocation;
    private View mAddPlaceView;

    private final static int MY_PERMISSION_FINE_LOCATION = 101;
    private final static int PLACE_PICKER_REQUEST = 1;

    private ArrayList<YourPlaces> yourPlacesArrayList;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private NotificationCompat.Builder builder;

    private View addReminderPopup;
    private View addReminderPopup2;
    private View addReminderPopup3;
    Menu menu_ourPlcaes;
    int subMenuCount;
    TextView currentLocation;
    private int indexSubmenu;

    NotificationManagerCompat notificationManager;

    Realm realm;

    ArrayList<Reminder> reminders;

    private static final int NOTIFICATION_ID_1 = 1;

    private static Location prevLocn = null, newLocn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        indexSubmenu = 0;

        requestPermission();
        realm = Realm.getDefaultInstance();

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        currentLocation = (TextView) findViewById(R.id.current_place);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        buildGoogleApiClent();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        Menu menu = navigationView.getMenu();
        menu.add("Title1");
        menu.add("Title2");
        //menu.getItem(2).getSubMenu().add("home");*/
        menu_ourPlcaes = menu.getItem(3).getSubMenu();
        subMenuCount = menu_ourPlcaes.size();
        reminders = new ArrayList<>();

        addReminderPopup = getLayoutInflater().inflate(R.layout.add_reminder_popup, null);
        addReminderPopup2 = getLayoutInflater().inflate(R.layout.add_reminder_popup_2, null);
        addReminderPopup3 = getLayoutInflater().inflate(R.layout.add_reminder_popup_3, null);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setView(addReminderPopup);
                final AlertDialog dialogAddPlace = builder.create();
                dialogAddPlace.setCustomTitle(getLayoutInflater().inflate(R.layout.add_place_popup_title, null));
//                dialogAddPlace.show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        /*mAdapter = new ReminderAdapter(reminders);

        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.HORIZONTAL));

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        reminders.add(new Reminder("fine", "yeah", true));
        reminders.add(new Reminder("wtf", "fk u", false));*/

        yourPlacesArrayList = new ArrayList<YourPlaces>();
        addCurrentlyStoredPlacesToArrayList();

        builder = new NotificationCompat.Builder(this);
        notificationManager = NotificationManagerCompat.from(getApplicationContext());
    }

    private void addCurrentlyStoredPlacesToArrayList() {

        menu_ourPlcaes.clear();
        indexSubmenu = 0;

        RealmResults<YourPlaces> places = realm.where(YourPlaces.class).findAll();

        // Use an iterator to add all
        realm.beginTransaction();

        for (YourPlaces place : places) {
            yourPlacesArrayList.add(place);
            displaySubmenu(place);
        }

        realm.commitTransaction();
    }


    protected synchronized void buildGoogleApiClent() {
        //this object helps us to connect with Google Api Services
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            //Intent intentProfile=new Intent(this,class);
            //startActivity(intentProfile);
        } else if (id == R.id.nav_talk_sarah) {
            //Intent intentSarah=new Intent(this,class);
            //startActivity(intentSarah);
        } else if (id == R.id.nav_login) {
            //Intent intentLogin=new Intent(this,class);
            //startActivity(intentLogin);
        } else if (id == R.id.addPlace) {
            mAddPlaceView = getLayoutInflater().inflate(R.layout.popup_add_your_place, null);
            tmpToStoreAddLocnName = (EditText) mAddPlaceView.findViewById(R.id.placeName_addPlacePopupEditText);

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setView(mAddPlaceView);
            final AlertDialog dialogAddPlace = builder.create();
            dialogAddPlace.setCustomTitle(getLayoutInflater().inflate(R.layout.add_place_popup_title, null));
            dialogAddPlace.show();

            ImageButton next = (ImageButton) mAddPlaceView.findViewById(R.id.nextImageButton);
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TextUtils.isEmpty(tmpToStoreAddLocnName.getText().toString())) {
                        Snackbar.make(mAddPlaceView, "Please specify name.", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    } else {
                        tmpEditTextLocation = tmpToStoreAddLocnName.getText().toString();

                        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                        try {
                            Intent intent = builder.build(MainActivity.this);
                            startActivityForResult(intent, PLACE_PICKER_REQUEST);
                        } catch (GooglePlayServicesRepairableException e) {
                            e.printStackTrace();
                        } catch (GooglePlayServicesNotAvailableException e) {
                            e.printStackTrace();
                        }
                        dialogAddPlace.dismiss();
                    }
                }
            });

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {

                Place place = PlacePicker.getPlace(MainActivity.this, data);
                LatLng locationLatLng = place.getLatLng();
                addYourPlace(locationLatLng, tmpEditTextLocation);
                tmpToStoreAddLocnName.setText(null);

            }
        }
    }

    private void requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_FINE_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_FINE_LOCATION:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "This app requires location permissions to be granted", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }

    public void addYourPlace(final LatLng latLng, final String name) {

        final String LAT = Double.toString(latLng.latitude);
        final String LNG = Double.toString(latLng.longitude);

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm bgRealm) {
                YourPlaces place = bgRealm.createObject(YourPlaces.class);
                place.setPlaceLAT(LAT);
                place.setPlaceLNG(LNG);
                place.setName(name);

                //add new place to database
                yourPlacesArrayList.add(place);
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {

                //this adds your places to menu in navigation  drawer
                addCurrentlyStoredPlacesToArrayList();

                //start geo fence addition process
                Geofence geofence = createGeofence(latLng, GEOFENCE_RADIUS);
                GeofencingRequest geofenceRequest = createGeofenceRequest(geofence);
                addGeofence(geofenceRequest);

                // Transaction was a success.
                Toast.makeText(MainActivity.this, "Successfully Stored", Toast.LENGTH_SHORT).show();
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                // Transaction failed and was automatically canceled.
                Toast.makeText(MainActivity.this, error.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }


    // Create a Geofence
    private Geofence createGeofence(LatLng latLng, float radius) {
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT |Geofence.GEOFENCE_TRANSITION_DWELL )
                .setLoiteringDelay(5).build();
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_FINE_LOCATION);
            }
        } else {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    request,
                    createGeofencePendingIntent()
            );
        }
    }

    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;

    private PendingIntent createGeofencePendingIntent() {
        if (geoFencePendingIntent != null)
            return geoFencePendingIntent;

        Intent intent = new Intent(this, GeofenceTransitionService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //this method is triggered as the connection is established(See onStart())
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat #requestPermissions
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //make it 20 mins
        mLocationRequest.setInterval(10000);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        mGoogleApiClient.connect();

    }

    //this method is triggered every time t=loacation is changes either by our own app or by the system
    @Override
    public void onLocationChanged(Location location) {

        if (prevLocn == null) {
            prevLocn = location;
            newLocn = location;
        } else {
            prevLocn = newLocn;
            newLocn = location;

            float dist;
            dist = newLocn.distanceTo(prevLocn);

            if (dist < 500) {
                Toast.makeText(this, "sucess", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onStart() {
        super.onStart();

        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        super.onStop();
    }

    private void displaySubmenu(final YourPlaces place) {
        menu_ourPlcaes.add(0, indexSubmenu, Menu.NONE, place.getName()).setIcon(R.drawable.ic_room_black_24dp).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                currentLocation.setText(place.getName());
                reminders.clear();
//                mAdapter.notifyDataSetChanged();
                return false;
            }
        });

        indexSubmenu++;

    }
//    private void displaySubmenu() {
////        Log.i(objects.get(0).getName(),"point ma252");
//        menu_ourPlcaes.add(0, indexSubmenu, Menu.NONE, yourPlacesArrayList.get(indexSubmenu).getName()).setIcon(R.drawable.ic_room_black_24dp).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                currentLocation.setText(yourPlacesArrayList.get(item.getItemId()).getName());
//                reminders.clear();
////                reminders.add(new Reminder(objects.get(item.getItemId()).getReminders().get(0), objects.get(item.getItemId()).getTime(),objects.get(item.getItemId()).getChecked().get(0)));
//                mAdapter.notifyDataSetChanged();
//                return false;
//            }
//        });
//        indexSubmenu++;
//
//    }

    public static Intent makeNotificationIntent(Context context, String msg) {
        Intent intent = new Intent( context, MainActivity.class );
        intent.putExtra( "Hello this is sarah", msg );
        return intent;
    }

}

