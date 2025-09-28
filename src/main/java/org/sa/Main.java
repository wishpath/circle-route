package org.sa;

import org.sa.config.Props;
import org.sa.service.GeoUtils;
import org.sa.service.OutputService;
import org.sa.service.RouteService;

import java.util.List;

public class Main {

  public static void main(String[] args) {
    RouteService routeService = new RouteService();
    OutputService outputService = new OutputService();
    //loop center points that i want to try here, but for now just go with one point:
    PointDTO routeCenterPoint = routeService.movePoint(Props.CIRCLE_CENTER, 2.0, -0.7);
    List<PointDTO> perfectCirclePoints = routeService.generatePerfectCirclePoints(routeCenterPoint, 25, 2);
    System.out.println("perfect circle points: " + perfectCirclePoints.size());
    List<PointDTO> circlePointsSnappedOnRoad = routeService.snapPointsOnRoadGrid(perfectCirclePoints);
    System.out.println("snapped on road points: " + circlePointsSnappedOnRoad.size());
    List<PointDTO> routed = routeService.connectSnappedPointsWithRoutes(circlePointsSnappedOnRoad);
    System.out.println("routed points: " + routed.size());

    List<PointDTO> noLoopRoutedPoints = removeLoopsByLoopingTheSameActions(routeService, routed);

    //efficiency measurement
    double routeLength = Math.round(GeoUtils.getRouteDistanceKm(noLoopRoutedPoints) * 100.0) / 100.0;
    double routeAreaKm = Math.round(GeoUtils.getRouteAreaKm(noLoopRoutedPoints) * 100.0) / 100.0;
    double idealAreaKm = Math.round(GeoUtils.getCircleAreaByLength(routeLength) * 100.0) / 100.0;
    double perfectSquareAreaKm = Math.round((routeLength * routeLength / 16) * 100.0) / 100.0;
    double routeAreaPerKm = Math.round((routeAreaKm / routeLength) * 100.0) / 100.0;
    double ideaAreaPerKm = Math.round((idealAreaKm / routeLength) * 100.0) / 100.0;
    int efficiencyPercent = (int)(routeAreaKm/idealAreaKm*100);
    int perfectSquareEfficiencyPercent = (int)(perfectSquareAreaKm/idealAreaKm*100);
    System.out.println(
      "\nEFFICIENCY: " +
      "\nLength: " + routeLength +
      " km\nArea: " + routeAreaKm +
      " sq.km\nArea per 1km: " + routeAreaPerKm +
      " sq.km/1km\nFor this length ideally area would be: " + idealAreaKm +
      " sq.km\nFor this length ideally area per 1km would be: " + ideaAreaPerKm +
      " sq.km/1km\nEfficiency for this length: " + efficiencyPercent +
      "%\nEfficiency of perfect square for this length would be: " + perfectSquareEfficiencyPercent + "%\n\n"
      );
    outputService.outputGPX(noLoopRoutedPoints);
    outputService.outputGpxWaypoints(noLoopRoutedPoints);
  }

  private static List<PointDTO> removeLoopsByLoopingTheSameActions(RouteService service, List<PointDTO> routePoints) {
    double indicatorOfLoop_maxDistance_loopStart_loopFinish_km = 0.2;
    List<PointDTO> noLoops = service.removeLoops(routePoints, indicatorOfLoop_maxDistance_loopStart_loopFinish_km);
    System.out.println("1 loops cut: " + noLoops.size());
    List<PointDTO> noLoopsRouted = service.connectSnappedPointsWithRoutes(noLoops); //reroute to fix loop cuts
    System.out.println("1 rerout: " + noLoopsRouted.size());
    List<PointDTO> shifted = service.shiftABtoBA_andReverse(noLoopsRouted);
    System.out.println("1 shifted: " + shifted.size());

    for (int i = 2; i < 8; i++, indicatorOfLoop_maxDistance_loopStart_loopFinish_km /= 2) {
      System.out.println();
      noLoops = service.removeLoops(shifted, indicatorOfLoop_maxDistance_loopStart_loopFinish_km);
      System.out.println(i + " loops cut: " + noLoops.size());
      noLoopsRouted = service.connectSnappedPointsWithRoutes(noLoops);
      System.out.println(i + " rerout: " + noLoopsRouted.size());
      shifted = service.shiftABtoBA_andReverse(noLoopsRouted);
      System.out.println(i + " shifted: " + shifted.size());
    }
    return shifted;
  }
}