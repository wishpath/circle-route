package org.sa.APPS;

import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;
import org.sa.map_data.TownData;
import org.sa.service.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * testing and experimentation class
 * checks that a limited number of towns produces a limited number of routes
 * provides a quick test compared to lithuania traverser class
 * used for efficiency improvements
 * checks gpx track output on the map
 * sometimes used for visualizing town locations on the map
 */
public class TownsTraverser {
  //keeping grass hopper profile here in case it will be needed to rotate them here
  public static String GRAPHHOPPER_PROFILE_FOOT_SHORTEST = "foot_shortest"; // delete cache when changed
  private RouteService routeGenerator = new RouteService();
  private GraphHopper graphHopper = new GraphHopper(GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
  private PointsWriterToGpxFile gpxOutput = new PointsWriterToGpxFile();
  private EfficiencyService efficiencyService = new EfficiencyService();

  private static final double CIRCLE_LENGTH_MIN = 10.0;
  private static final double CIRCLE_LENGTH_MAX = 10.0;
  private static final double CIRCLE_LENGTH_STEP = 5.0;
  private static final int sizeInstances = ((int)((CIRCLE_LENGTH_MAX - CIRCLE_LENGTH_MIN) / CIRCLE_LENGTH_STEP)) + 1;

  private static final int MAX_DISTANCE_BETWEEN_POINTS_KM = 2; // distance between ideal circle points


  public void traverse() {
    LocalDateTime start = LocalDateTime.now();
    outer:
    for (double perimeter = CIRCLE_LENGTH_MIN; perimeter <= CIRCLE_LENGTH_MAX; perimeter += CIRCLE_LENGTH_STEP) {
      final double finalPerimeter = perimeter;
      System.out.println("Circle length: "  + perimeter);
      TownData.townName_townCenterPoint.forEach((townName, center) -> {
        List<PointDTO> perfectCircle = routeGenerator.generatePerfectCirclePoints(center, finalPerimeter, MAX_DISTANCE_BETWEEN_POINTS_KM); // +0s
        List<PointDTO> snappedCircle = graphHopper.snapPointsOnRoadGrid(perfectCircle);
        List<PointDTO> routedClosedCircle = graphHopper.connectSnappedPointsWithRoutesAndClose(snappedCircle, GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
        List<PointDTO> noLoopRoutedPoints = removeLoopsByLoopingTheSameActions(routedClosedCircle);

        EfficiencyDTO efficiencyDTO = efficiencyService.getRouteEfficiency(noLoopRoutedPoints);
        gpxOutput.outputPointsAsGPX(noLoopRoutedPoints, efficiencyDTO.efficiencyPercent + "_"+ (int) efficiencyDTO.routeLength + "_" + townName + ".gpx");
      });
    }


    int instances = (TownData.townName_townCenterPoint.size() * sizeInstances);
    int timeSeconds = (int) java.time.Duration.between(start, LocalDateTime.now()).toSeconds();
    System.out.println(
        "Total instances: " + instances +
            ", duration: " + timeSeconds + " seconds, " +
            "instances per second: " + ((timeSeconds != 0) ? (instances / timeSeconds) : "ALL_IN_ZERO_SECONDS")
      );
  }

  //doubled method, care!
  private List<PointDTO> removeLoopsByLoopingTheSameActions(List<PointDTO> routePoints) {
    double indicatorOfLoop = 0.3;
    List<PointDTO> noLoops = new ArrayList<>();
    List<PointDTO> noLoopsRouted;
    List<PointDTO> shifted = GeoUtils.addExtraPointsInBetweenExistingOnes(routePoints);


    for (int i = 0; i < 2; i++) {
      noLoops = routeGenerator.removeLoops(shifted, indicatorOfLoop, graphHopper, GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
      noLoopsRouted = graphHopper.connectSnappedPointsWithRoutesAndClose(noLoops, GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
      shifted = routeGenerator.shiftABtoBA_andReverse(noLoopsRouted);
      shifted.add(shifted.get(0));
    }
    return shifted;
  }
}
