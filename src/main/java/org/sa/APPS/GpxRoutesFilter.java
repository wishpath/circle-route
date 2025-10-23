package org.sa.APPS;

import org.sa.DTO.PointDTO;
import org.sa.service.EfficiencyService;
import org.sa.service.GeoUtils;
import org.sa.service.GpxOutput;
import org.sa.service.GpxParser;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class GpxRoutesFilter {
  private static final String CENTER = "kaunas";
  private static final int RADIUS_KM = 65;
  private static final int MIN_EFFICIENCY = 74;

  private static final String SOURCE_DIR = "src/main/java/org/sa/routes";
  private static final String INTVL_EDGES_DIR = "src/main/java/org/sa/INTVL-taken";
  private static final String OUTPUT_DIR = "src/main/java/org/sa/filtered/" + CENTER + "_radius" + RADIUS_KM + "_efficiency" + MIN_EFFICIENCY + "/";

  public static void main(String[] args) {
    EfficiencyService efficiencyService = new EfficiencyService();
    File[] gpxRouteFiles = new File(SOURCE_DIR).listFiles((d, n) -> n.toLowerCase().endsWith(".gpx"));
    if (gpxRouteFiles == null) return;

    PointDTO centerPoint = CitiesTraverser.city_townCenter.get(CENTER);
    if (centerPoint == null) {
      System.err.println("Unknown center: " + CENTER);
      return;
    }

    int counter = 1;
    for (File gpx : gpxRouteFiles) {
      List<PointDTO> points = GpxParser.parseGpxFile(gpx);
      if (points.isEmpty()) continue;

      double distance = GeoUtils.getDistanceBetweenLocations(centerPoint, points.get(0));
      double efficiency = efficiencyService.getRouteEfficiency(points).efficiencyPercent;
      if (distance < RADIUS_KM && efficiency >= MIN_EFFICIENCY)
        new GpxOutput().outputGPXToDir(points, gpx.getName().replace(".gpx", "_" + counter++), OUTPUT_DIR);
    }

    writeIntvlCityEdges(centerPoint);
  }

  private static void writeIntvlCityEdges(PointDTO centerPoint) {
    File[] allIntvlCityEdges = new File(INTVL_EDGES_DIR)
        .listFiles((dir, fileName) -> fileName.toLowerCase().endsWith(".gpx"));
    if (allIntvlCityEdges == null) {
      System.out.println("No GPX files found in INTVL directory.");
      return;
    }

    Map<String, List<File>> closeCityName_gpxFiles = new HashMap<>();
    Pattern validPattern = Pattern.compile("^\\d{4}_\\d{2}_\\d{2} [A-Za-z]+ .*\\.gpx$");

    for (File cityEdgeFile : allIntvlCityEdges) {
      String fileName = cityEdgeFile.getName();
      if (!validPattern.matcher(fileName).matches())
        throw new IllegalArgumentException("Filename: " + fileName + ". Must be 'YYYY_MM_DD CityName description.gpx'");

      String cityName = fileName.split(" ")[1];
      List<PointDTO> routePoints = GpxParser.parseGpxFile(cityEdgeFile);
      boolean isClose = routePoints.stream()
          .anyMatch(p -> GeoUtils.getDistanceBetweenLocations(centerPoint, p) < RADIUS_KM);
      if (isClose) closeCityName_gpxFiles.computeIfAbsent(cityName, k -> new ArrayList<>()).add(cityEdgeFile);
    }

    closeCityName_gpxFiles.forEach((city, files) -> {
      File latestFile = files.stream().max(Comparator.comparing(File::getName)).orElse(null);
      if (latestFile != null)
        new GpxOutput().outputGPXToDir(GpxParser.parseGpxFile(latestFile), latestFile.getName(), OUTPUT_DIR);
    });
  }

}
