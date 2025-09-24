package org.sa;

import java.util.List;

public class Main {
  public static final PointDTO KAUNAS_CENTER = new PointDTO(54.8985, 23.9036);
  public static final PointDTO ROKISKIS_CENTER = new PointDTO(55.9474, 25.5948);
  public static void main(String[] args) {
    PointService service = new PointService();
    //loop center points that i want to try here
      //but now just go with one point:
    List<PointDTO> perfectCircle = service.generatePerfectCirclePoints(ROKISKIS_CENTER, 30, 2);
    List<PointDTO> circlePointsSnappedOnRoad = service.snapPointsOnRoadGrid(perfectCircle);
    List<PointDTO> routed = service.connectSnappedPointsWithRoutes(circlePointsSnappedOnRoad);

    List<PointDTO> noLoops = service.removeLoops(routed, 0.01); //10m away is considered the same point
    List<PointDTO> noLoopsRouted = service.connectSnappedPointsWithRoutes(noLoops); //reroute to fix loop cuts

    //the same process again in case rerouting created loops again.
    List<PointDTO> noLoops1 = service.removeLoops(noLoopsRouted, 0.01);
    List<PointDTO> noLoopsRouted1 = service.connectSnappedPointsWithRoutes(noLoops1);


    //the same process again in case rerouting created loops again.
    List<PointDTO> noLoops2 = service.removeLoops(noLoopsRouted1, 0.02);
    List<PointDTO> noLoopsRouted2 = service.connectSnappedPointsWithRoutes(noLoops2);

    service.outputGPX(noLoopsRouted2);
    service.outputGpxWaypoints(noLoopsRouted2);
  }
}
