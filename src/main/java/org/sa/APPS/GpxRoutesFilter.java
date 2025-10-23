package org.sa.APPS;

import org.sa.DTO.PointDTO;
import org.sa.service.*;

import java.io.File;
import java.util.List;

public class GpxRoutesFilter {
  private static final String CENTER = "kaunas";
  private static final int RADIUS_KM = 65;
  private static final int MIN_EFFICIENCY = 80;

  private static final String SOURCE_DIR = "src/main/java/org/sa/routes";
  private static final String OUTPUT_DIR = "src/main/java/org/sa/filtered/" + CENTER + "_radius" + RADIUS_KM + "_efficiency" + MIN_EFFICIENCY + "/";

  public static void main(String[] args) {
    EfficiencyService efficiencyService = new EfficiencyService();
    File[] gpxFiles = new File(SOURCE_DIR).listFiles((d, n) -> n.toLowerCase().endsWith(".gpx"));
    if (gpxFiles == null) return;

    PointDTO centerPoint = CitiesTraverser.city_townCenter.get(CENTER);
    if (centerPoint == null) {
      System.err.println("Unknown center: " + CENTER);
      return;
    }

    int counter = 1;
    for (File gpx : gpxFiles) {
      List<PointDTO> points = GpxParser.parseGpxFile(gpx);
      if (points.isEmpty()) continue;

      double distance = GeoUtils.getDistanceBetweenLocations(centerPoint, points.get(0));
      double efficiency = efficiencyService.getRouteEfficiency(points).efficiencyPercent;
      if (distance < RADIUS_KM && efficiency >= MIN_EFFICIENCY)
        new GpxOutput().outputGPXToDir(points, gpx.getName().replace(".gpx", "_" + counter++), OUTPUT_DIR);
    }
  }
}
