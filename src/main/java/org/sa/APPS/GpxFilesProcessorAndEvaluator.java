package org.sa.APPS;

import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;
import org.sa.service.*;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public class GpxFilesProcessorAndEvaluator {
  private static final String GPX_FOLDER = "src/main/java/org/sa/APPS/gpx_files_to_evaluate";
  //  private static final String GPX_FOLDER = "src/main/java/org/sa/output-gpx";


  public static void main(String[] args) {
    evaluateGPXRoutesInDirectory(GPX_FOLDER);
  }

  public static void evaluateGPXRoutesInDirectory(String path) {
    RouteGenerator routeGenerator = new RouteGenerator();
    EfficiencyService efficiencyService = new EfficiencyService();

    File folder = new File(path);
    if (!folder.exists() || !folder.isDirectory()) {
      System.err.println("Folder not found: " + path);
      return;
    }

    File[] gpxFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".gpx"));
    if (gpxFiles == null || gpxFiles.length == 0) {
      System.err.println("No GPX files found in folder: " + path);
      return;
    }
    int counter = 1;
    for (File gpxFile : gpxFiles) {
      List<PointDTO> routePoints = GpxParser.parseGpxFile(gpxFile);
      if (routePoints.size() < 2) continue;
      //close route
      //if (!routePoints.get(0).equals(routePoints.get(routePoints.size() - 1))) routePoints.add(routePoints.get(0));

      //make route start and finish at most northern point
      List<PointDTO> startingAtNorthRoute_closed = routeGenerator.startAndFinishRouteOnMostNorthernPoint(routePoints);
      List<PointDTO> clockwiseRoute = routeGenerator.makeRouteClockwise(startingAtNorthRoute_closed);


      String city = CitiesTraverser.city_townCenter.entrySet().stream()
          .min(Comparator.comparingDouble(e -> GeoUtils.getDistanceBetweenLocations(e.getValue(), clockwiseRoute.get(0))))
          .map(java.util.Map.Entry::getKey)
          .orElse("Unknown");

      EfficiencyDTO eff = efficiencyService.getRouteEfficiency(clockwiseRoute);
      efficiencyService.printRouteEfficiency(eff, gpxFile.getName());

      new GpxOutput().outputGPXToDir(
          clockwiseRoute,
          city + "_" + (int)eff.routeLength + "km_" + eff.efficiencyPercent + "eff_" + (int)eff.routeAreaKm + "sqkm_" + counter++,
          "src/main/java/org/sa/APPS/processed/");
    }
  }
}
