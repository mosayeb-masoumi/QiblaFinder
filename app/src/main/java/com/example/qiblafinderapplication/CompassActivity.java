package com.example.qiblafinderapplication;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.utils.GpsTracker;


public class CompassActivity extends AppCompatActivity {
    public QiblaCompassView compassView;
    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener compassListener;
    private float orientation = 0;
    ImageView ghotb;
    boolean internetState = false;
    boolean gpsState = false;
    int locationRequestCode;
    int internetRequestcode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        ghotb = (ImageView) findViewById(R.id.ghotb);


        if (LocationPermissionGranted()) {

            if (checkGpsOn() ) {
         comass();
            } else {
                showStartMapDialogowghat();
            }

        } else {
            askLocationPermissionowghat();
        }


        compassView = findViewById(R.id.compass_view);
        compassListener = new SensorEventListener() {

            static final float ALPHA = 0.15f;
            float azimuth;

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                // angle between the magnetic north direction
                // 0=North, 90=East, 180=South, 270=West
                float angle = event.values[0] + orientation;
                if (stop) angle = 0;
                ghotb.setRotation(-angle);
                azimuth = lowPass(angle, azimuth);
                compassView.setBearing(azimuth);
                compassView.invalidate();
            }

            /**
             * https://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
             * http://developer.android.com/reference/android/hardware/SensorEvent.html#values
             */
            private float lowPass(float input, float output) {
                if (Math.abs(180 - input) > 170) {
                    return input;
                }
                return output + ALPHA * (input - output);
            }
        };

        setCompassMetrics();

        ImageView help= findViewById(R.id.more);
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
             ViewPermissionDialog alert = new ViewPermissionDialog();
                alert.showDialog(CompassActivity.this);
            }
        });
    }
    public class ViewPermissionDialog {

        public void showDialog(Activity activity) {
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(true);
            dialog.setContentView(R.layout.dialog_android_compass);
            TextView txt = (TextView) dialog.findViewById(R.id.txt);

            txt.setText(getString(R.string.txt_compass));

            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();

        }
    }
    String latitude="0";
    String longitude="0";
    public void comass(){
        GpsTracker gpsTracker = new GpsTracker(getApplicationContext());
        if (gpsTracker.canGetLocation()) {


                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        GpsTracker gpsTracker = new GpsTracker(getApplicationContext());
                       latitude = String.valueOf(gpsTracker.getLatitude());
                        longitude = String.valueOf(gpsTracker.getLongitude());


                        if (latitude.equals("0"))  {
                            new Handler().postDelayed(new Runnable() {
                                public void run() {
                                    GpsTracker gpsTracker = new GpsTracker(getApplicationContext());
                                    latitude = String.valueOf(gpsTracker.getLatitude());
                                    longitude = String.valueOf(gpsTracker.getLongitude());


                                }
                            }, 1000);
                        }
                    }
                }, 500);



        } else {
            //  gpsTracker.showSettingsAlert();
           Toast.makeText(getApplicationContext(),"خطا در گرفتن موقعیت جغرافیایی",Toast.LENGTH_LONG).show();
        }
        new Handler().postDelayed(new Runnable() {
            public void run() {
                compassView.setLongitude(Double.parseDouble(longitude));
                compassView.setLatitude(Double.parseDouble(latitude));
                compassView.initCompassView();
                compassView.invalidate();

                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                if (sensorManager != null) {
                    sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
                    if (sensor != null) {
                        sensorManager.registerListener(compassListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
                    } else {
                        Toast.makeText(CompassActivity.this, getString(R.string.compass_not_found), Toast.LENGTH_SHORT).show();
                    }
                }
            }
            }, 1500);
    }
    private void showStartMapDialogowghat() {

        final Dialog dialog = new Dialog(CompassActivity.this);
        dialog.setContentView(R.layout.dialog_map_gps);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // remove rectangulare back
//        dialog.setTitle("Title...");

        LinearLayout root = dialog.findViewById(R.id.root);
        RelativeLayout rl_btn_yes = dialog.findViewById(R.id.rl_btn_yes);
        RelativeLayout rl_btn_no = dialog.findViewById(R.id.rl_btn_no);
        TextView txt = dialog.findViewById(R.id.txt);
        TextView txt_accept = dialog.findViewById(R.id.accept);
        TextView txt_deny = dialog.findViewById(R.id.deny);


       if (checkGpsOn() ) {
            txt.setText("اتصال به اینترنت را بررسی نمایید");
            internetState = false;
            gpsState = true;
            locationRequestCode = 201;
            internetRequestcode = 202;
        } else if (!checkGpsOn() ) {
            txt.setText("لطفا برای تنظیم موقعیت مکانی (GPS) و اینترنت تلفن همراه خود را فعال کنید");
            internetState = true;
            gpsState = false;
            locationRequestCode = 301;
            internetRequestcode = 302;
        }


        txt_deny.setText("رد کردن");
        txt_accept.setText("فعالسازی");


        rl_btn_yes.setOnClickListener(view -> {


            turnOnInternetGpsowghat(internetState, gpsState);

            dialog.dismiss();
        });


        rl_btn_no.setOnClickListener(view -> {
            dialog.dismiss();


        });

        dialog.show();

    }
    private void turnOnInternetGpsowghat(boolean internetState, boolean gpsState) {

        if (!internetState && !gpsState) {

            if (LocationPermissionGranted()) {
                turnOnGPSowghat();
            } else {
                askLocationPermissionowghat();
            }

        } else if (internetState && !gpsState) {

            if (LocationPermissionGranted()) {
                turnOnGPSowghat();
            } else {
                askLocationPermissionowghat();
            }

        } else if (!internetState && gpsState) {

            turnOnWifi();
        }

    }
    private void turnOnGPSowghat() {

        Intent settingintent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(settingintent, 3500);
    }
    private void askLocationPermissionowghat() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2500);
    }
    private void turnOnWifi() {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        startActivityForResult(intent, internetRequestcode);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setCompassMetrics();
    }
    private boolean LocationPermissionGranted() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }
    private boolean checkGpsOn() {
        final LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    public int getStatusBarHeight() {
      int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
       // return result;
        Rect rectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;
        int contentViewTop =
                window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int titleBarHeight= contentViewTop - statusBarHeight;
        Log.e("FDfs","s"+statusBarHeight+"=="+titleBarHeight+"="+result);
        return titleBarHeight;
    }
    private void setCompassMetrics() {
     getStatusBarHeight();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
       //RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(width, (600 ));
       // ghotb.setLayoutParams(parms);
  /*      RelativeLayout relativeLayout = new RelativeLayout(this);
// ImageView


// Setting layout params to our RelativeLayout
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(50, 60);

// Setting position of our ImageView
        layoutParams.leftMargin = 70;
        layoutParams.topMargin = 80;

// Finally Adding the imageView to RelativeLayout and its position
        relativeLayout.addView(ghotb, layoutParams);*/

   //     ghotb.setX(0);
     //   ghotb.setY(0);
   int     centreX= Math.round( (ghotb.getPivotX() + ghotb.getWidth()  / 2));
   int     centreY=Math.round( (ghotb.getPivotY() + ghotb.getHeight() / 2));

      compassView.setScreenResolution(width/2, (height/2 ));

//compassView.setLayoutParams(parms);
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return;
        }

        switch (wm.getDefaultDisplay().getOrientation()) {
            case Surface.ROTATION_0:
                orientation = 0;
                break;
            case Surface.ROTATION_90:
                orientation = 90;
                break;
            case Surface.ROTATION_180:
                orientation = 180;
                break;
            case Surface.ROTATION_270:
                orientation = 270;
                break;
        }
    }



    public boolean stop = false;


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensor != null) {
            sensorManager.unregisterListener(compassListener);
        }
    }

    private void checkLocationPermissionowghat() {

        if (LocationPermissionGranted()) {

            if (checkGpsOn() ) {
                comass();
            } else {
                showStartMapDialogowghat();
            }

        } else {
            askLocationPermissionowghat();
        }
    }
    private void checkToStartMapowghat() {

        if ( checkGpsOn()) {
            comass();
        } else {}


    }
    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
       if (requestCode == 101) {
           comass();

        } else
       if (requestCode == 2500) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                checkLocationPermissionowghat();
            } else {
                comass();

            }
        } else if (requestCode == 3500) {
           if (!checkGpsOn() ) {
                checkToStartMapowghat();
            } else  {
               comass();
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

         if (requestCode == 101) {
             comass();

        }  else if (requestCode == 2500) {

           if (!checkGpsOn()) {
                checkToStartMapowghat();
            } else  {
               comass();
            }

        } else if (requestCode == 3500) {
            if (checkGpsOn() ) {
                comass();
            } else  {
                checkToStartMapowghat();
            }

        }
    }

}