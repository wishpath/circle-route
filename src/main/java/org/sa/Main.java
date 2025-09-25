package org.sa;

import java.util.List;

public class Main {
  public static final PointDTO KAUNAS_CENTER = new PointDTO(54.8985, 23.9036);
  public static final PointDTO ROKISKIS_CENTER = new PointDTO(55.9474, 25.5948);
  public static void main(String[] args) {
    PointService service = new PointService();
    //loop center points that i want to try here
      //but now just go with one point:
    PointDTO point = service.movePoint(ROKISKIS_CENTER, 2.0, -0.7);
    List<PointDTO> perfectCircle = service.generatePerfectCirclePoints(point, 25, 2);
    System.out.println("perfect circle points: " + perfectCircle.size());
    List<PointDTO> circlePointsSnappedOnRoad = service.snapPointsOnRoadGrid(perfectCircle);
    System.out.println("snapped on road points: " + circlePointsSnappedOnRoad.size());
    List<PointDTO> routed = service.connectSnappedPointsWithRoutes(circlePointsSnappedOnRoad);
    System.out.println("routed points: " + routed.size());






    double loopThresholdKm = 0.035;
    List<PointDTO> noLoops = service.removeLoops(routed, loopThresholdKm); //10m away is considered the same point
    System.out.println("1 loops cut: " + noLoops.size());
    List<PointDTO> noLoopsRouted = service.connectSnappedPointsWithRoutes(noLoops); //reroute to fix loop cuts
    System.out.println("1 rerout: " + noLoopsRouted.size());
    List<PointDTO> shifted = service.shiftABtoBA(noLoopsRouted);
    System.out.println("1 shifted: " + shifted.size());

    for (int i = 2; i < 8; i++, loopThresholdKm /= 1.5) {
      System.out.println();
      noLoops = service.removeLoops(shifted, loopThresholdKm);
      System.out.println(i + " loops cut: " + noLoops.size());
      noLoopsRouted = service.connectSnappedPointsWithRoutes(noLoops);
      System.out.println(i + " rerout: " + noLoopsRouted.size());
      shifted = service.shiftABtoBA(noLoopsRouted);
      System.out.println(i + " shifted: " + shifted.size());
    }


    service.outputGPX(shifted);
    service.outputGpxWaypoints(shifted);
  }
}
