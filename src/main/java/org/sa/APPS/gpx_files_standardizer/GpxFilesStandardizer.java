package org.sa.APPS.gpx_files_standardizer;

import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;
import org.sa.service.*;

import java.io.File;
import java.util.List;

/**
 * Standardizes GPX route files by: starting point, route direction and file name
 * */
public class GpxFilesStandardizer {

  public static final String GPX_FILES_TO_STANDARDIZE_DIRECTORY = "src/main/java/org/sa/APPS/gpx_files_standardizer/gpx_files_to_standardize";
  public static final String STANDARDIZED_OUTPUT_DIRECTORY = "src/main/java/org/sa/APPS/gpx_files_standardizer/standardized_output";

  public static void main(String[] args) {
    standardizeGPXRoutes();
  }

  // make route start on north, go clockwise and close.
  public static void standardizeGPXRoutes() {
    //prep
    RouteService routeService = new RouteService();
    EfficiencyService efficiencyService = new EfficiencyService();

    int counter = 1;
    for (File gpxFile : FilesUtil.listGpxFiles(GPX_FILES_TO_STANDARDIZE_DIRECTORY)) {
      //read points
      List<PointDTO> unclosedRoutePoints = GpxFileToPointsParser.parseFromGpxFileToPoints(gpxFile);

      //modify
      List<PointDTO> startingAtNorth_closedRoute = routeService.startAndFinishRouteOnMostNorthernPoint(unclosedRoutePoints);
      List<PointDTO> clockwiseRouteClosed = routeService.makeRouteClockwise(startingAtNorth_closedRoute);

      //use efficiency details to name file
      EfficiencyDTO efficiency = efficiencyService.getRouteEfficiency(clockwiseRouteClosed);
      String circleFileName =
        TownTool.getRouteClosestTownName(clockwiseRouteClosed) + "_" +
        (int) efficiency.routeLength + "km_" +
        efficiency.efficiencyPercent + "eff_" +
        (int) efficiency.routeAreaKm + "sqkm_" +
        counter++ + ".gpx";
      efficiencyService.printRouteEfficiency(efficiency, circleFileName);

      //write
      new PointsWriterToGpxFile().outputPointsAsGPX(clockwiseRouteClosed, circleFileName, STANDARDIZED_OUTPUT_DIRECTORY);
    }
  }
}
