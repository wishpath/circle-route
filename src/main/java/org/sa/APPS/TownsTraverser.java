package org.sa.APPS;

import org.sa.DTO.EfficiencyDTO;
import org.sa.DTO.PointDTO;
import org.sa.map_data.TownData;
import org.sa.service.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//check limited amount of routes, improve efficiency for the real scan and also check output on the map if there are some problems!
public class TownsTraverser {
  //keeping grass hopper profile here in case it will be needed to rotate them here
  public static String GRAPHHOPPER_PROFILE_FOOT_SHORTEST = "foot_shortest"; // delete cache when changed
  private RouteGeneratorAndModifier routeGenerator = new RouteGeneratorAndModifier();
  private GraphHopper graphHopper = new GraphHopper(GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
  private GpxOutput gpxOutput = new GpxOutput();
  private EfficiencyService efficiencyService = new EfficiencyService();

  private static final double CIRCLE_LENGTH_MIN = 10.0;
  private static final double CIRCLE_LENGTH_MAX = 10.0;
  private static final double CIRCLE_LENGTH_STEP = 5.0;
  private static final int sizeInstances = ((int)((CIRCLE_LENGTH_MAX - CIRCLE_LENGTH_MIN) / CIRCLE_LENGTH_STEP)) + 1;

  private static final int MAX_DISTANCE_BETWEEN_POINTS_KM = 2; // distance between ideal circle points


  public void traverse() {
    LocalDateTime start = LocalDateTime.now();

    //Total instances: 150, duration: 31 seconds,  instances per second: 4
    //Total instances: 150, duration: 60 seconds, instances per second: 2
    //Total instances: 150, duration: 63 seconds, instances per second: 2
    outer:
    for (double perimeter = CIRCLE_LENGTH_MIN; perimeter <= CIRCLE_LENGTH_MAX; perimeter += CIRCLE_LENGTH_STEP) {
      final double finalPerimeter = perimeter;
      System.out.println("Circle length: "  + perimeter);
      TownData.townName_townCenterPoint.forEach((townName, center) -> {
        List<PointDTO> perfectCircle = routeGenerator.generatePerfectCirclePoints(center, finalPerimeter, MAX_DISTANCE_BETWEEN_POINTS_KM); // +0s
        List<PointDTO> snappedCircle = graphHopper.snapPointsOnRoadGrid(perfectCircle);
        List<PointDTO> routedClosedCircle = graphHopper.connectSnappedPointsWithRoutesAndClose(snappedCircle, GRAPHHOPPER_PROFILE_FOOT_SHORTEST);
        //List<PointDTO> noLoopRoutedPoints = removeLoopsByLoopingTheSameActions(routedClosedCircle); // doubled method from LithuaniaTraverse

        EfficiencyDTO efficiencyDTO = efficiencyService.getRouteEfficiency(routedClosedCircle);
        gpxOutput.outputGPX(routedClosedCircle, efficiencyDTO.efficiencyPercent + "_"+ (int) efficiencyDTO.routeLength + "_" + townName + ".gpx");
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
