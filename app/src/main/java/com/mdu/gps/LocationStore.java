package com.mdu.gps;

import android.content.Context;
import android.location.Location;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class LocationStore {
  ArrayList<Location> points;
  Context context;
  String dateFormat = "yyyy-MM-dd-SSS";
  String ext = ".kml";
  String storage = Environment.DIRECTORY_DOCUMENTS;

  LocationStore(Context ctx) {
    points = new ArrayList<>();
    context = ctx;
  }

  boolean store(Location loc) {
    return points.add(loc);
  }

  String getFileName() {
    Locale l = Locale.getDefault();
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, l);
    String name = sdf.format(new Date());
    return name + ext;
  }

  File getFile() {
    String extState = Environment.getExternalStorageState();

    try {
      String fname = getFileName();
      String path = Environment.
              getExternalStoragePublicDirectory(storage).
              getAbsolutePath();
      File dir = new File(path);
      dir.mkdirs();
      File f = new File(dir, fname);
      f.createNewFile();
      return f;
    } catch (Exception e) {
      U.E(context, e);
    }
    return null;
  }

  String saveInFile() {
    File file = getFile();
    String name = file.getAbsolutePath();
    U.M(context, "Save in file " + name);
    byte[] b;
    try {
      FileOutputStream s = new FileOutputStream(file);
      b = kmlStart.getBytes();
      s.write(b);
      for(Location l:points) {
        String str = "        " + l.getLatitude() + "," + l.getLongitude() + "," + l.getAltitude() + "\n";
        b = str.getBytes();
        s.write(b);
      }
      b = kmlEnd.getBytes();
      s.write(b);
      s.flush();
      s.close();
    } catch (IOException e) {
      U.E(context, e);
    }
    return name;
  }

  String kmlStart =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
          "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
          "<Document>\n" +
          "  <name>Paths</name>\n" +
          "  <description>Description</description> \n"+
          "  <Style id=\"yellowLineGreenPoly\">\n" +
          "    <LineStyle>\n" +
          "      <color>000000</color>\n" +
          "      <width>1</width>\n" +
          "    </LineStyle>\n" +
          "    <PolyStyle>\n" +
          "      <color>7f00ff00</color>\n" +
          "    </PolyStyle>\n" +
          "  </Style>\n" +
          "  <Placemark>\n" +
          "    <name>Absolute Extruded</name>\n" +
          "    <description>Transparent green wall with yellow outlines</description>\n" +
          "    <styleUrl>#yellowLineGreenPoly</styleUrl>\n" +
          "    <LineString>\n" +
          "      <extrude>1</extrude>\n" +
          "      <tessellate>1</tessellate>\n" +
          "      <altitudeMode>absolute</altitudeMode>\n" +
          "      <coordinates>\n";
//           -112.2550785337791,36.07954952145647,2357
String kmlEnd =
        "        </coordinates>\n" +
        "      </LineString>\n" +
        "    </Placemark>\n" +
        "  </Document>\n" +
        "</kml>\n";
}
