package org.sa.APPS;

import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;
import org.sa.service.*;

import java.io.File;
import java.util.List;

public class GpxFilesProcessor {

  public static final String GPX_FILES_TO_EVALUATE_DIRECTORY = "src/main/java/org/sa/APPS/gpx_files_to_evaluate";
  public static final String PROCESSED_OUTPUT_DIRECTORY = "src/main/java/org/sa/APPS/processed/";

  public static void main(String[] args) {
    processGPXRoutesInDirectory(GPX_FILES_TO_EVALUATE_DIRECTORY);
  }

  public static void processGPXRoutesInDirectory(String folderWithGpxFilesToEvaluate) {
    RouteGeneratorAndModifier modifierService = new RouteGeneratorAndModifier();
    EfficiencyService efficiencyService = new EfficiencyService();
    File[] gpxFiles = listGpxFiles(folderWithGpxFilesToEvaluate);

    int counter = 1;
    for (File gpxFile : gpxFiles) {
      List<PointDTO> unclosedRoutePoints = GpxParser.parseGpxFile(gpxFile);
      if (unclosedRoutePoints.size() < 2) continue;

      List<PointDTO> startingAtNorth_closedRoute = modifierService.startAndFinishRouteOnMostNorthernPoint(unclosedRoutePoints);
      List<PointDTO> clockwiseRouteClosed = modifierService.makeRouteClockwise(startingAtNorth_closedRoute);
      EfficiencyDTO efficiency = efficiencyService.getRouteEfficiency(clockwiseRouteClosed);

      String circleFileName =
        TownTool.getRouteClosestTownName(clockwiseRouteClosed) + "_" +
        (int) efficiency.routeLength + "km_" +
        efficiency.efficiencyPercent + "eff_" +
        (int) efficiency.routeAreaKm + "sqkm_" +
        counter++ + ".gpx";

      efficiencyService.printRouteEfficiency(efficiency, circleFileName);

      new GpxOutput().outputGPXToDir(clockwiseRouteClosed, circleFileName, PROCESSED_OUTPUT_DIRECTORY);
    }
  }

  private static File[] listGpxFiles(String folderWithGpxFilesToEvaluate) {
    File folder = new File(folderWithGpxFilesToEvaluate);
    if (!folder.exists() || !folder.isDirectory()) throw new IllegalStateException("Folder not found: " + folderWithGpxFilesToEvaluate);

    File[] gpxFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".gpx"));
    if (gpxFiles == null || gpxFiles.length == 0) {
      System.err.println("No GPX files found in folder: " + folderWithGpxFilesToEvaluate);
    }
    return gpxFiles;
  }
}
