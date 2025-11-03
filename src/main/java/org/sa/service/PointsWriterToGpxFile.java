package org.sa.service;

import org.sa.DTO.PointDTO;
import org.sa.config.Props;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class PointsWriterToGpxFile {

  public void outputPointsAsGPX(List<PointDTO> circlePointsSnappedOnRoad, String circleFileName) { //overwrites
    outputPointsAsGPX(circlePointsSnappedOnRoad, circleFileName, Props.GPX_OUTPUT_DIR);
  }

  public void outputPointsAsGPX(List<PointDTO> circlePointsSnappedOnRoad, String circleFileName, String outputDir) { //overwrites
    try {
      File dir = new File(outputDir);
      if (!dir.exists()) dir.mkdirs();

      File gpxFile = new File(dir, circleFileName);
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

      System.out.println("GPX file written to: \u001B[34m" + gpxFile.getPath() + "\u001B[0m");
    } catch (IOException e) {
      throw new RuntimeException("Failed to write GPX file", e);
    }
  }
}
