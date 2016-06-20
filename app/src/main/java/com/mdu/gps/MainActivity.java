package com.mdu.gps;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
  EditText text;
  Switch swtch;
  BroadcastReceiver messageReceiver;

  private static final int LOC_PERM = 0x3141;
  private static final int EXT_PERM = 0x3142;
  private static final int GPS_LOC  = 0x3143;

  LocationService locService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main);

    text = (EditText) findViewById(R.id.text);
    text.setMovementMethod(new ScrollingMovementMethod());
    swtch = (Switch) findViewById(R.id.swtch);
    swtch.setOnClickListener(this);

    LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

    if(messageReceiver == null) {
      messageReceiver = U.createMessageReceiver(text);
      lbm.registerReceiver(messageReceiver, new IntentFilter(U.LOCATION_MESSAGE));
    }

    if (!checkLocationPermissions())
      requestLocationPermissions();

    if (!checkExtStoragePermissions())
      requestExtStoragePermissions();

    if (isGPSEnabled(this)) {
      U.M(this, "GPS <font color='green'> enabled </color>");
    } else {
      U.M(this, "GPS <b> not </b> enabled");
      enableGPSDialog(this, text);
    }

  }

  public void onClick(View v) {
    final Switch s = swtch;
    boolean check = s.isChecked();

    if(check) {
      startLocationService();
      locService.setCollecting(true);
    } else {
      U.Dialog(this, new String[]{
              "Stop collecting points ?",
              getString(R.string.OK),
              getString(R.string.CANCEL)
      }, new DIL() {
        @Override // OK, stop collecting
        public void onClick(DialogInterface dialogInterface, int i) {
          locService.setCollecting(false);
          String fn = locService.store.saveInFile();
          stopLocationService();
          showFile(fn);
        }
      }, new DIL() {
        @Override // Nan, I changed my mind
        public void onClick(DialogInterface dialogInterface, int i) {
          s.setChecked(true);
        }
      });
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

    lbm.unregisterReceiver(messageReceiver);
    messageReceiver = null;
  }

  @Override
  public void onConfigurationChanged(Configuration config) {
    super.onConfigurationChanged(config);
  }

  @Override
  public void onStart() {
    super.onStart();
  }

  public void onRestart() {
    super.onRestart();
  }

  @Override
  public void onStop() {
    super.onStop();
  }

  private ServiceConnection locConnection = new ServiceConnection() {

    public void onServiceConnected(ComponentName className, IBinder binder) {
      LocationService.LocBinder b = (LocationService.LocBinder) binder;
      locService = b.getService();
      U.M(MainActivity.this, locService + " connected");
      swtch.setChecked(locService.isCollecting());
    }

    public void onServiceDisconnected(ComponentName className) {
      locService = null;
    }

  };

  @Override
  public void onPause() {
    super.onPause();

    unbindService(locConnection);
  }

  @Override
  public void onResume() {
    super.onResume();

    Intent intent= new Intent(this, LocationService.class);
    bindService(intent, locConnection, Context.BIND_AUTO_CREATE);
  }

  void stopLocationService() {
    Intent i = new Intent(this, LocationService.class);
    ComponentName name = i.getComponent();
    boolean b = stopService(i);
    if (b)
      U.M(this, "Service " + name + " stopped");
    else
      U.M(this, "Failed to stop service " + name);
  }

  void startLocationService() {
    Intent i = new Intent(this, LocationService.class);
    ComponentName n = startService(i);
    U.M(this, "Service " + n + " started");
  }

  boolean checkLocationPermissions() {
    boolean pf = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    boolean pc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    if (pf && pc) {
      U.M(this, "permission granted");
    } else {
      U.M(this, "permission NOT granted " + pf + " " + pc);
    }
    return pf && pc;
  }

  boolean checkExtStoragePermissions() {
    boolean pf = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED;
    boolean pc = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED;
    if (pf && pc) {
      U.M(this, "permission granted");
    } else {
      U.M(this, "permission NOT granted " + pf + " " + pc);
    }
    return pf && pc;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == LOC_PERM) {
      for (int i = 0; i < permissions.length; i++) {
        String permission = permissions[i];
        int grantResult = grantResults[i];

        if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {

          if (grantResult == PackageManager.PERMISSION_GRANTED) {
            U.M(this, "permission " + permission + " granted");
          } else {
            U.M(this, "permission " + permission + " *NOT* granted");
          }
        }
      }
    }
    if(requestCode == EXT_PERM) {
      for (int i = 0; i < permissions.length; i++) {
        String permission = permissions[i];
        int grantResult = grantResults[i];

        if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {

          if (grantResult == PackageManager.PERMISSION_GRANTED) {
            U.M(this, "permission " + permission + " granted");
          } else {
            U.M(this, "permission " + permission + " *NOT* granted");
          }
        }
      }
    }
  }

  void requestLocationPermissions() {
    ActivityCompat.requestPermissions(this, new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    }, LOC_PERM);
  }

  void requestExtStoragePermissions() {
    ActivityCompat.requestPermissions(this, new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    }, EXT_PERM);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode == GPS_LOC) {
      switch (requestCode) {
        case GPS_LOC:
          super.onResume();
          break;
      }
    }
  }

  public void enableGPSDialog(final Activity ctx, final EditText tv) {
    U.Dialog(ctx, new String[]{"Enable GPS", ctx.getString(R.string.OK), ctx.getString(R.string.CANCEL)}, new DIL() {
      public void onClick(DialogInterface dialogInterface, int i) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        ctx.startActivityForResult(intent, GPS_LOC);
        int k = 0;

        while (!isGPSEnabled(ctx)) {
          try {
            if (k == 0)
              U.MNonNl(tv, "Wait for GPS enabled .");
            else
              U.MNonNl(tv, ".");
            Thread.sleep(200);
          } catch (InterruptedException e) {
            U.E(ctx, e);
          }
          k++;
        }
        U.MNonNl(tv, "\n");
        ctx.finishActivity(GPS_LOC);

        final Switch b = (Switch) findViewById(R.id.swtch);
        b.setChecked(true);

      }
    }, null);
  }

  static boolean isGPSEnabled(Context ctx) {
    return ((LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE))
            .isProviderEnabled(LocationManager.GPS_PROVIDER);
  }

  String fileAsString(File f) {
    String ret = "";
    try {
    BufferedReader r = new BufferedReader(new FileReader(f));
    String l;
    while((l = r.readLine()) != null)
      ret += l + "\n";
  } catch (IOException e){
      U.E(this, e);
    }
    return ret;
  }

  void showFile(String name) {
    File f = new File(name);
    String str = fileAsString(f);
    U.MNonNl(text, str);
    U.A(this, new String[]{name, getString(R.string.CLOSE), str});
  }

  static void T(Context ctx) {
    U.T(ctx, "" + LocationService.IsRunning(ctx), 3);
  }

}
