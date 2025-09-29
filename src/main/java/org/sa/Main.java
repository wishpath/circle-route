package org.sa;

import org.sa.APPS.GPXRouteEfficiencyEvaluator;
import org.sa.config.Props;
import org.sa.service.GraphHopperService;
import org.sa.service.OutputService;
import org.sa.service.RouteImprovingService;
import org.sa.service.RouteService;

import java.util.List;

public class Main {

  public static void main(String[] args) {
    RouteService routeService = new RouteService();
    GraphHopperService graphHopperService = new GraphHopperService();
    OutputService outputService = new OutputService();
    RouteImprovingService routeImprovingService = new RouteImprovingService(graphHopperService);

    //implement loop of center points and lengths here later

    PointDTO routeCenterPoint = routeService.movePoint(Props.CIRCLE_CENTER, 2.0, -0.7);
    List<PointDTO> perfectCirclePoints = routeService.generatePerfectCirclePoints(routeCenterPoint, 25, 2);
    System.out.println("perfect circle points: " + perfectCirclePoints.size());
    List<PointDTO> circlePointsSnappedOnRoad = graphHopperService.snapPointsOnRoadGrid(perfectCirclePoints);
    System.out.println("snapped on road points: " + circlePointsSnappedOnRoad.size());
    List<PointDTO> routed = graphHopperService.connectSnappedPointsWithRoutes(circlePointsSnappedOnRoad);
    System.out.println("routed points: " + routed.size());

    List<PointDTO> noLoopRoutedPoints = removeLoopsByLoopingTheSameActions(graphHopperService, routeService, routed);

    //output
    outputService.outputGPX(noLoopRoutedPoints);
    outputService.outputGpxWaypoints(noLoopRoutedPoints);

    //printout efficiency parameters
    GPXRouteEfficiencyEvaluator.evaluateGPXRoutesInDirectory(Props.GPX_OUTPUT_DIR);
  }

  private static List<PointDTO> removeLoopsByLoopingTheSameActions(GraphHopperService graphHopperService, RouteService service, List<PointDTO> routePoints) {
    double indicatorOfLoop_maxDistance_loopStart_loopFinish_km = 0.2;
    List<PointDTO> noLoops = service.removeLoops(routePoints, indicatorOfLoop_maxDistance_loopStart_loopFinish_km);
    System.out.println("1 loops cut: " + noLoops.size());
    List<PointDTO> noLoopsRouted = graphHopperService.connectSnappedPointsWithRoutes(noLoops); //reroute to fix loop cuts
    System.out.println("1 rerout: " + noLoopsRouted.size());
    List<PointDTO> shifted = service.shiftABtoBA_andReverse(noLoopsRouted);
    System.out.println("1 shifted: " + shifted.size());

    for (int i = 2; i < 8; i++, indicatorOfLoop_maxDistance_loopStart_loopFinish_km /= 2) {
      System.out.println();
      noLoops = service.removeLoops(shifted, indicatorOfLoop_maxDistance_loopStart_loopFinish_km);
      System.out.println(i + " loops cut: " + noLoops.size());
      noLoopsRouted = graphHopperService.connectSnappedPointsWithRoutes(noLoops);
      System.out.println(i + " rerout: " + noLoopsRouted.size());
      shifted = service.shiftABtoBA_andReverse(noLoopsRouted);
      System.out.println(i + " shifted: " + shifted.size());
    }
    return shifted;
  }
}