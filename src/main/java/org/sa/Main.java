package org.sa;

import org.sa.APPS.LithuaniaTraverser;
import org.sa.DTO.PointDTO;
import org.sa.config.Props;
import org.sa.service.*;

import java.util.ArrayList;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    new LithuaniaTraverser().traverse();
  }

  private static void generateRoute(RouteService routeService, PointDTO routeCenterPoint, GraphHopperService graphHopperService, RouteImprovingService routeImprovingService, OutputService outputService, int circleCounter) {
    List<PointDTO> perfectCirclePoints = routeService.generatePerfectCirclePoints(routeCenterPoint, 25, 2);
    //System.out.println("perfect circle points: " + perfectCirclePoints.size());
    List<PointDTO> circlePointsSnappedOnRoad = graphHopperService.snapPointsOnRoadGrid(perfectCirclePoints, Props.GRASSHOPPER_PROFILE1_FOOT_SHORTEST);
    //System.out.println("snapped on road points: " + circlePointsSnappedOnRoad.size());
    List<PointDTO> routed = graphHopperService.connectSnappedPointsWithRoutes(circlePointsSnappedOnRoad, Props.GRASSHOPPER_PROFILE1_FOOT_SHORTEST);
    //System.out.println("routed points: " + routed.size());

    List<PointDTO> noLoopRoutedPoints = removeLoopsByLoopingTheSameActions(graphHopperService, routeService, routed);

    List<PointDTO> smoothRoutePoints = new ArrayList<>(noLoopRoutedPoints);
    double maxCheckWindowSize = GeoUtils.autoCloseRouteAndGetLengthKm(noLoopRoutedPoints) / 6;
    String profile = Props.GRASSHOPPER_PROFILE1_FOOT_SHORTEST;
    for (double checkWindowSizeKm = 0.3; checkWindowSizeKm < maxCheckWindowSize; checkWindowSizeKm += 0.3) {
      //System.out.println("window size: " + checkWindowSizeKm);
      smoothRoutePoints = routeImprovingService.improveRouteBySlidingWindow(smoothRoutePoints, checkWindowSizeKm, routeCenterPoint, profile);
      smoothRoutePoints = routeService.shiftABtoBA_andReverse(smoothRoutePoints);
      profile = (profile.equals(Props.GRASSHOPPER_PROFILE1_FOOT_SHORTEST)) ? Props.GRASSHOPPER_PROFILE2_BIKE_SHORTEST : Props.GRASSHOPPER_PROFILE1_FOOT_SHORTEST;
    }

    //output
    outputService.outputGPX(smoothRoutePoints, circleCounter);
    //outputService.outputGpxWaypoints(smoothRoutePoints);

    //printout efficiency parameters
    //GPXRouteEfficiencyEvaluator.evaluateGPXRoutesInDirectory(Props.GPX_OUTPUT_DIR);
  }

  private static List<PointDTO> removeLoopsByLoopingTheSameActions(GraphHopperService graphHopperService, RouteService routeService, List<PointDTO> routePoints) {
    double indicatorOfLoop_maxDistance_loopStart_loopFinish_km = 0.2;
    List<PointDTO> noLoops = routeService.removeLoops(routePoints, indicatorOfLoop_maxDistance_loopStart_loopFinish_km);
    //System.out.println("1 loops cut: " + noLoops.size());
    List<PointDTO> noLoopsRouted = graphHopperService.connectSnappedPointsWithRoutes(noLoops, Props.GRASSHOPPER_PROFILE1_FOOT_SHORTEST); //reroute to fix loop cuts
    //System.out.println("1 rerout: " + noLoopsRouted.size());
    List<PointDTO> shifted = routeService.shiftABtoBA_andReverse(noLoopsRouted);
    //System.out.println("1 shifted: " + shifted.size());

    for (int i = 2; i < 8; i++, indicatorOfLoop_maxDistance_loopStart_loopFinish_km /= 2) {
      //System.out.println();
      noLoops = routeService.removeLoops(shifted, indicatorOfLoop_maxDistance_loopStart_loopFinish_km);
      //System.out.println(i + " loops cut: " + noLoops.size());
      noLoopsRouted = graphHopperService.connectSnappedPointsWithRoutes(noLoops, Props.GRASSHOPPER_PROFILE1_FOOT_SHORTEST);
      //System.out.println(i + " rerout: " + noLoopsRouted.size());
      shifted = routeService.shiftABtoBA_andReverse(noLoopsRouted);
      //System.out.println(i + " shifted: " + shifted.size());
    }
    return shifted;
  }
}