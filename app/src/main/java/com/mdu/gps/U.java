package com.mdu.gps;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.Spanned;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Iterator;

public class U {

  static final int Depth = 2;

  static final String LOCATION_NEW_MESSAGE = "com.mdu.gps.LOCATION_RESULT";
  static final String LOCATION_MESSAGE = "com.mdu.gps.LOCATION_MESSAGE";

  static final String LOCATION_NEW_POINT = "com.mdu.gps.LOCATION_NEW_POINT";
  static final String LOCATION_POINT = "com.mdu.gps.LOCATION_POINT";

  // title, close, message
  static void A(Context ctx, String[] s) {
    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
    builder.setTitle(s[0]);
    builder .setMessage(s[2]);
    builder .setCancelable(false);
    builder .setNegativeButton(s[1], new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });
    AlertDialog alert = builder.create();
    alert.show();
  }

  private static double toRad(double a) {
    return a * Math.PI / 180.;
  }

  // from http://www.movable-type.co.uk/scripts/latlong.html
   static double DistanceDeg(double lng1, double lat1, double lng2, double lat2) {
    double R = 6367445; // metres
    double φ1 = toRad(lat1);
    double φ2 = toRad(lat2);
    double Δφ = toRad(lat2 - lat1);
    double Δλ = toRad(lng2 - lng1);
    double Δφ2 = Δφ/2.;
    double Δλ2 = Δλ/2.;

    double a = Math.sin(Δφ2) * Math.sin(Δφ2) + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ2) * Math.sin(Δλ2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    double d = R * c;
    return d;
  }

   static double DistanceDeg2(double lngA, double latA, double lngB, double latB) {
    double a = toRad(latA);
    double b = toRad(latB);
    double c = toRad(lngA);
    double d = toRad(lngB);
    double R = 6367445;
    double dst = R * Math.acos(Math.sin(a) * Math.sin(b) + Math.cos(a) * Math.cos(b) * Math.cos(c - d));
    return dst;
  }


  static void traceLocation(Context ctx, Location loc) {

    String s = "location changed; " + "\n" +
            "\tLAT " + loc.getLatitude() + "\n" +
            "\tLONG " + loc.getLongitude() + "\n" +
            "\tACCURACY " + loc.getAccuracy() + " m\n" +
            "\tPROVIDER " + loc.getProvider() + "\n" +
            "\tSPEED " + loc.getSpeed() + " m/s\n" +
            "\tALTITUDE " +  loc.getAltitude() + "\n" +
            "\tBEARING " + loc.getBearing() + " degrees east of true north";
    M(ctx, s);
  }

  static void listSatellites(Context ctx, LocationManager lm) {

    String s = "";
    GpsStatus gpsStatus = lm.getGpsStatus(null);
    if(gpsStatus != null) {
      Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
      Iterator<GpsSatellite> sati = satellites.iterator();
      int i=0;
      while (sati.hasNext()) {
        GpsSatellite satellite = sati.next();
        if(satellite.usedInFix())
          s += (i++) + ": \n" +
                  "\tprn " + satellite.getPrn() + "\n" + // pseudo random number
//                  "used in fix " + satellite.usedInFix() + "\n" + // used in fix
                  "\tnoise " + satellite.getSnr() + "\n" + // signal noise
                  "\tazimuth " + satellite.getAzimuth() + "\n" + // azimuth
                  "\televation " + satellite.getElevation()+ "\n"; // elevation
      }
      M(ctx, s);
    }
  }

  static int countSatellites(LocationManager lm) {
    GpsStatus gpsStatus = lm.getGpsStatus(null);
    int count = 0;
    if(gpsStatus != null) {
      Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();

      for (GpsSatellite satellite : satellites)
        if (satellite.usedInFix())
          count++;
    }
    return count;
  }

  static void Message(Context ctx, String message) {
    if (message == null)
      return;

    LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(ctx);
    Intent intent = new Intent(LOCATION_MESSAGE);
    intent.putExtra(LOCATION_NEW_MESSAGE, message);
    if (!broadcaster.sendBroadcast(intent)) {
      Toast(ctx, "Failed to send broadcast " + message);
    }
  }

  static void NewPoint(Context ctx, double[] loc) {
    if(loc == null)
      return;

    LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(ctx);
    Intent intent = new Intent(LOCATION_POINT);
    intent.putExtra(LOCATION_NEW_POINT, loc);
    if (!broadcaster.sendBroadcast(intent)) {
      T(ctx, "Failed to send broadcast " + loc);
    }
  }

  static String makeString(Context ctx, String msg, int depth) {
    StackTraceElement[] trace = new Error().getStackTrace();
    int d = depth < 0 ? Depth : depth;
    StackTraceElement st = trace[d];
    String caller = st.getMethodName();
    String cls = st.getFileName();
    int i = cls.lastIndexOf('.');
    if (i > 0)
      cls = cls.substring(0, i);
    String s = cls + " / " + caller;
    if (msg != null)
      s += " " + msg;
    return s;
  }

  static Toast Toast(Context ctx, String message) {
    Toast t = Toast.makeText(ctx, message, android.widget.Toast.LENGTH_SHORT);
    t.show();
    return t;
  }

  static void T(Context ctx, String msg, int depth) {
    String s = makeString(ctx, msg, depth);
    if(s != null)
      Toast(ctx, s);
  }

  static void T(Context ctx, String msg) {
    String s = makeString(ctx, msg, -1);
    if(s != null)
      Toast(ctx, s);
  }

  static void T(Context ctx) {
    String s = makeString(ctx, null, -1);
    if(s != null)
      Toast(ctx, s);
  }

   static void M(Context ctx, String msg) {
     String s = makeString(ctx, msg, -1);
     if(s != null)
       Message(ctx, s);
  }

   static void M(Context ctx) {
     String s = makeString(ctx, "", -1);
     if(s != null)
       Message(ctx, s);
  }

  private static String makeExceptionString(Context ctx, Exception e) {
    String ret = e.getMessage() + "\n";
    StackTraceElement[] ste = e.getStackTrace();
    int count = 5;
    for(StackTraceElement s:ste) {
      ret += s.toString() + ctx.getString(R.string.NL);
      if(count-- <= 0)
        break;
    }
    return ret;
  }

  // report execption
  static void E(Context ctx, Exception e) {
    String s = makeExceptionString(ctx, e);
    A(ctx, new String[] {e.getMessage(), ctx.getString(R.string.CLOSE), s});
  }

  static void MNonNl(EditText tv, String m) {
    tv.append(m);
  }

  // Broadcast receiver to get messages from LocationService

  static BroadcastReceiver createPointReceiver(final LocationStore points) {
    return new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        double[] p = intent.getDoubleArrayExtra(U.LOCATION_NEW_POINT);
        M(context, context.getString(R.string.LEFT_RIGHT_ARROW) + " " + p[0] + " " + context.getString(R.string.UP_DOWN_ARROW) + " " + p[1]);
      }
    };
  }

    // Broadcast receiver to get messages from LocationService

  static BroadcastReceiver createMessageReceiver(final EditText tv) {
    return new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String s = intent.getStringExtra(U.LOCATION_NEW_MESSAGE);

        if(s == null || "".equals(s))
          return;

        if (!s.endsWith("\n"))
          s += "\n";

        if(s.indexOf('<') >= 0 && s.indexOf('>') >= 0) {
          Spanned sp = Html.fromHtml(s);
          tv.append(sp);
        } else {
          tv.append(s);
        }
      }
    };
  }

  static void Sleep(Context ctx, long time) {
    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
      E(ctx, e);
    }
  }

  public static void Dialog(Context ctx, String[] strings, DIL ok, DIL cancel) {
    AlertDialog.Builder abld = new AlertDialog.Builder(ctx);
    abld.setMessage(strings[0]);
    abld.setCancelable(false);
    abld.setPositiveButton(strings[1], ok);
    abld.setNegativeButton(strings[2], cancel);
    AlertDialog alert = abld.create();
    alert.show();
  }

}

// short name for Dialog Interface Listener
abstract class DIL implements DialogInterface.OnClickListener {
}
