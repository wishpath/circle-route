package org.sa.APPS;

import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;
import org.sa.service.*;

import java.io.File;
import java.util.List;

public class GpxFilesStandardizer {

  public static final String GPX_FILES_TO_EVALUATE_DIRECTORY = "src/main/java/org/sa/APPS/gpx_files_to_evaluate";
  public static final String PROCESSED_OUTPUT_DIRECTORY = "src/main/java/org/sa/APPS/processed/";

  public static void main(String[] args) {
    standardizeGPXRoutes();
  }

  // make rout start on north, go clockwise and close.
  public static void standardizeGPXRoutes() {
    //prep
    RouteService modifierService = new RouteService();
    EfficiencyService efficiencyService = new EfficiencyService();
    File[] gpxFiles = FilesUtil.listGpxFiles(GPX_FILES_TO_EVALUATE_DIRECTORY);

    int counter = 1;
    for (File gpxFile : gpxFiles) {
      //read points
      List<PointDTO> unclosedRoutePoints = GpxFileToPointsParser.parseFromGpxFileToPoints(gpxFile);

      //modify
      List<PointDTO> startingAtNorth_closedRoute = modifierService.startAndFinishRouteOnMostNorthernPoint(unclosedRoutePoints);
      List<PointDTO> clockwiseRouteClosed = modifierService.makeRouteClockwise(startingAtNorth_closedRoute);

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
      new GpxOutput().outputPointsAsGPX(clockwiseRouteClosed, circleFileName, PROCESSED_OUTPUT_DIRECTORY);
    }
  }


}
