package org.sa.service;

import org.sa.PointDTO;
import org.sa.config.Props;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class OutputService {

  public void outputGpxWaypoints(List<PointDTO> points) { //for the display of points on the map rather than continuous route
    //overwrites
    try {
      File dir = new File(Props.GPX_OUTPUT_DIR);
      if (!dir.exists()) dir.mkdirs();

      File gpxFile = new File(dir, Props.DATA_NAME + "-circle-waypoints.gpx");
      try (FileWriter writer = new FileWriter(gpxFile)) {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<gpx version=\"1.1\" creator=\"PointService\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n");

        for (int i = 0; i < points.size(); i++) {
          PointDTO p = points.get(i);
          writer.write(String.format("  <wpt lat=\"%f\" lon=\"%f\">\n", p.latitude, p.longitude));
          writer.write("    <name>Point " + (i + 1) + "</name>\n");
          writer.write("  </wpt>\n");
        }

        writer.write("</gpx>\n");
      }

      System.out.println("Waypoints GPX file written to: " + gpxFile.getAbsolutePath());
    } catch (IOException e) {
      throw new RuntimeException("Failed to write GPX waypoints file", e);
    }
  }

  public void outputGPX(List<PointDTO> circlePointsSnappedOnRoad, int circleCounter) { //overwrites
    try {
      File dir = new File(Props.GPX_OUTPUT_DIR);
      if (!dir.exists()) dir.mkdirs();

      File gpxFile = new File(dir, Props.DATA_NAME + "-circle-route" + circleCounter + ".gpx");
      try (FileWriter writer = new FileWriter(gpxFile)) {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<gpx version=\"1.1\" creator=\"PointService\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
        writer.write("  <trk>\n");
        writer.write("    <name>Circle Route</name>\n");
        writer.write("    <trkseg>\n");

        for (PointDTO p : circlePointsSnappedOnRoad) {
          writer.write(String.format("      <trkpt lat=\"%f\" lon=\"%f\"></trkpt>\n", p.latitude, p.longitude));
        }

        writer.write("    </trkseg>\n");
        writer.write("  </trk>\n");
        writer.write("</gpx>\n");
      }

      System.out.println("GPX file written to: " + gpxFile.getAbsolutePath());
    } catch (IOException e) {
      throw new RuntimeException("Failed to write GPX file", e);
    }
  }
}
