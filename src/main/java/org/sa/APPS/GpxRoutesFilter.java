package org.sa.APPS;

import org.sa.DTO.PointDTO;
import org.sa.map_data.TownData;
import org.sa.service.EfficiencyService;
import org.sa.service.GeoUtils;
import org.sa.service.GpxOutput;
import org.sa.service.GpxParser;

import java.io.File;
import java.util.*;

public class GpxRoutesFilter {
  private static final String MAIN_TOWN = "kaunas";
  private static final int RADIUS_KM = 66;
  private static final int MIN_EFFICIENCY = 0;

  private static final String SOURCE_DIR = "src/main/java/org/sa/routes";
  private static final String INTVL_EDGES_DIR = "src/main/java/org/sa/INTVL-taken";
  private static final String OUTPUT_DIR = "src/main/java/org/sa/filtered/" + MAIN_TOWN + "_radius" + RADIUS_KM + "_efficiency" + MIN_EFFICIENCY + "/";

  public static void main(String[] args) {
    EfficiencyService efficiencyService = new EfficiencyService();
    File[] allGpxRouteFiles = new File(SOURCE_DIR).listFiles((d, n) -> n.toLowerCase().endsWith(".gpx"));
    if (allGpxRouteFiles == null) return;

    PointDTO centerPoint = TownData.townName_townCenterPoint.get(MAIN_TOWN);
    if (centerPoint == null) {
      System.err.println("Unknown center: " + MAIN_TOWN);
      return;
    }

    int counter = 1;
    List<PointDTO> mainTownIntvlEdge = getMainTownIntvlEdge();
    for (File gpxFile : allGpxRouteFiles) {
      List<PointDTO> points = GpxParser.parseGpxFile(gpxFile);

      if (points.isEmpty()) continue;
      if (points.get(0).equals(points.getLast())) points.add(points.get(0));
      if (mainTownIntvlEdge != null && GeoUtils.areMostPointsWithinPolygon(points, mainTownIntvlEdge, 95)) {
        System.err.println(gpxFile.getName() + " is within " + MAIN_TOWN + " INTV edge and was excluded from filter output");
        continue;
      }

      double distance = GeoUtils.getDistanceBetweenLocations(centerPoint, points.get(0));
      double efficiency = efficiencyService.getRouteEfficiency(points).efficiencyPercent;
      if (distance < RADIUS_KM && efficiency >= MIN_EFFICIENCY)
        new GpxOutput().outputGPXToDir(points, gpxFile.getName().replace(".gpx", "_" + counter++ + ".gpx"), OUTPUT_DIR);
    }

    writeIntvlCityEdges(centerPoint);
  }

  private static void writeIntvlCityEdges(PointDTO centerPoint) {
    Map<String, List<File>> closeCityName_gpxFiles = new HashMap<>();

    for (File cityEdgeFile : getAllIntvlCityEdgesFiles()) {
      String cityName = cityEdgeFile.getName().split(" ")[1];
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

  private static File[] getAllIntvlCityEdgesFiles() {
    File[] allIntvlCityEdges = new File(INTVL_EDGES_DIR)
        .listFiles((dir, fileName) -> fileName.toLowerCase().endsWith(".gpx"));
    if (allIntvlCityEdges == null) {
      System.err.println("No GPX files found in INTVL directory.");
      return null;
    }
    for (File cityEdgeFile : allIntvlCityEdges) {
      if (!cityEdgeFile.getName().matches("^\\d{4}_\\d{2}_\\d{2} [A-Za-z]+ .*\\.gpx$"))
        throw new IllegalArgumentException("Filename: " + cityEdgeFile.getName() + ". Must be 'YYYY_MM_DD CityName description.gpx'");
    }
    return allIntvlCityEdges;
  }

  private static List<PointDTO> getMainTownIntvlEdge() {
    File[] allCityEdgeFiles = getAllIntvlCityEdgesFiles();
    if (allCityEdgeFiles == null || allCityEdgeFiles.length == 0) return List.of();

    List<File> mainCityFiles = Arrays.stream(allCityEdgeFiles)
        .filter(f -> f.getName().toLowerCase().contains(" " + MAIN_TOWN.toLowerCase() + " "))
        .toList();

    if (mainCityFiles.isEmpty()) return List.of();

    File latestMainCityFile = mainCityFiles.stream()
        .max(Comparator.comparing(File::getName))
        .orElse(null);

    return latestMainCityFile == null ? List.of() : GpxParser.parseGpxFile(latestMainCityFile);
  }
}
