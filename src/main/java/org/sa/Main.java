package org.sa;

import org.sa.config.Props;
import org.sa.service.*;

import java.util.ArrayList;
import java.util.List;

public class Main {

  public static void main(String[] args) {
    RouteService routeService = new RouteService();
    GraphHopperService graphHopperService = new GraphHopperService();
    OutputService outputService = new OutputService();
    RouteImprovingService routeImprovingService = new RouteImprovingService(graphHopperService);


    int circleCounter = 0;
    PointDTO areaCenterPoint = routeService.movePoint(Props.CIRCLE_CENTER, 2.0, -0.7);
    PointDTO initialRouteCenterPoint = routeService.movePoint(areaCenterPoint, -2.5, -2.5);
    for (double north = 0; north < 5; north += 0.5) //10 instances
      for (double east = 0; east < 5; east += 0.5, circleCounter++) {//10 instances
        System.out.println(circleCounter);
        PointDTO iterationCenterPoint = routeService.movePoint(initialRouteCenterPoint, north, east);
        generateRoute(routeService, iterationCenterPoint, graphHopperService, routeImprovingService, outputService, circleCounter);
      }



  }

  private static void generateRoute(RouteService routeService, PointDTO routeCenterPoint, GraphHopperService graphHopperService, RouteImprovingService routeImprovingService, OutputService outputService, int circleCounter) {
    List<PointDTO> perfectCirclePoints = routeService.generatePerfectCirclePoints(routeCenterPoint, 25, 2);
    //System.out.println("perfect circle points: " + perfectCirclePoints.size());
    List<PointDTO> circlePointsSnappedOnRoad = graphHopperService.snapPointsOnRoadGrid(perfectCirclePoints);
    //System.out.println("snapped on road points: " + circlePointsSnappedOnRoad.size());
    List<PointDTO> routed = graphHopperService.connectSnappedPointsWithRoutes(circlePointsSnappedOnRoad);
    //System.out.println("routed points: " + routed.size());

    List<PointDTO> noLoopRoutedPoints = removeLoopsByLoopingTheSameActions(graphHopperService, routeService, routed);

    List<PointDTO> smoothRoutePoints = new ArrayList<>(noLoopRoutedPoints);
    double maxCheckWindowSize = GeoUtils.autoCloseRouteAndGetLengthKm(noLoopRoutedPoints) / 6;
    for (double checkWindowSizeKm = 0.3; checkWindowSizeKm < maxCheckWindowSize; checkWindowSizeKm += 0.3) {
      //System.out.println("window size: " + checkWindowSizeKm);
      smoothRoutePoints = routeImprovingService.improveRouteBySlidingWindow(smoothRoutePoints, checkWindowSizeKm, routeCenterPoint);
      smoothRoutePoints = routeService.shiftABtoBA_andReverse(smoothRoutePoints);
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
    List<PointDTO> noLoopsRouted = graphHopperService.connectSnappedPointsWithRoutes(noLoops); //reroute to fix loop cuts
    //System.out.println("1 rerout: " + noLoopsRouted.size());
    List<PointDTO> shifted = routeService.shiftABtoBA_andReverse(noLoopsRouted);
    //System.out.println("1 shifted: " + shifted.size());

    for (int i = 2; i < 8; i++, indicatorOfLoop_maxDistance_loopStart_loopFinish_km /= 2) {
      //System.out.println();
      noLoops = routeService.removeLoops(shifted, indicatorOfLoop_maxDistance_loopStart_loopFinish_km);
      //System.out.println(i + " loops cut: " + noLoops.size());
      noLoopsRouted = graphHopperService.connectSnappedPointsWithRoutes(noLoops);
      //System.out.println(i + " rerout: " + noLoopsRouted.size());
      shifted = routeService.shiftABtoBA_andReverse(noLoopsRouted);
      //System.out.println(i + " shifted: " + shifted.size());
    }
    return shifted;
  }
}