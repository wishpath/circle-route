package org.sa.service;

import org.sa.PointDTO;

import java.util.ArrayList;
import java.util.List;

public class RouteImprovingService {
  private GraphHopperService graphHopperService;

  public RouteImprovingService(GraphHopperService graphHopperService) {
    this.graphHopperService = graphHopperService;
  }

  /**
   * Analyze route with a sliding window of specified length in kilometers.
   */
  public List<PointDTO> improveRouteBySlidingWindow(List<PointDTO> routePoints, double windowSizeKm, PointDTO centerPoint, String profile) {
    List<PointDTO> behind = new ArrayList<>();

    //build window and keep track : start, end, points and distance
    int startIndex = 0;
    double accumulatedDistance = 0.0;
    if (routePoints.size() < 2) throw new RuntimeException("short list");
    int endIndex = startIndex + 1; // so window should never be empty


    // Expand window until distance reaches windowSizeKm or end of route
    while (endIndex < routePoints.size() - 1 && accumulatedDistance < windowSizeKm) {
      accumulatedDistance += GeoUtils.getDistanceBetweenLocations(routePoints.get(endIndex), routePoints.get(endIndex + 1));
      endIndex++;
    }
    List<PointDTO> window = routePoints.subList(startIndex, endIndex);

    while (startIndex < routePoints.size() - 1) {
      List<PointDTO> reroutedWindow = rerouteFirstAndLastPoints(routePoints, startIndex, endIndex, profile);

      if (isReroutedWindowBetter(reroutedWindow, window, accumulatedDistance, centerPoint)) {
        behind.addAll(reroutedWindow);
        accumulatedDistance = 0.0;
        startIndex = endIndex + 1;
        // Expand window until distance reaches windowSizeKm or end of route
        while (endIndex < routePoints.size() - 1 && accumulatedDistance < windowSizeKm) {
          accumulatedDistance += GeoUtils.getDistanceBetweenLocations(routePoints.get(endIndex), routePoints.get(endIndex + 1));
          endIndex++;
        }
        window = routePoints.subList(startIndex, endIndex);
      }
      else {
        //leave point '0' behind the window, so between 0-1 distance will be removed and '1' will become new '0'
        behind.add(routePoints.get(startIndex));
        window = new ArrayList<>(window.subList(1, window.size()));
        if (startIndex + 1 < routePoints.size()) {
          accumulatedDistance -= GeoUtils.getDistanceBetweenLocations(routePoints.get(startIndex), routePoints.get(startIndex + 1));
        }
        startIndex++;
        // Expand window until distance reaches windowSizeKm or end of route
        while (endIndex < routePoints.size() - 1 && accumulatedDistance < windowSizeKm) {
          accumulatedDistance += GeoUtils.getDistanceBetweenLocations(routePoints.get(endIndex), routePoints.get(endIndex + 1));
          endIndex++; //after this action endIndex might be at most — 'routePoints.size() - 1'
          window.add(routePoints.get(endIndex));
        }
      }

      //deal the last short distance in the end:
      if (endIndex == routePoints.size() - 1) { //very simple check, should not be heavy to calculate
        for (int i = startIndex; i < routePoints.size(); i++)
          behind.add(routePoints.get(i));
        break;
      }
    }
    return behind;
  }

  private List<PointDTO> rerouteFirstAndLastPoints(List<PointDTO> routePoints, int startIndex, int endIndex, String profile) {
    List<PointDTO> firstAndLastPoints = List.of(routePoints.get(startIndex), routePoints.get(endIndex));
    return graphHopperService.connectSnappedPointsWithRoutes(firstAndLastPoints, profile);
  }

  private boolean isReroutedWindowBetter(List<PointDTO> reroutedWindow, List<PointDTO> originalWindow,
                                         double originalCrustLength, PointDTO centerPoint) {

    double reroutedCrustLength = GeoUtils.getCurveDistanceNoClosing(reroutedWindow);


    double originalPizzaSliceArea = GeoUtils.getPizzaSliceAreaKm(originalWindow, centerPoint);
    double reroutedPizzaSliceArea = GeoUtils.getPizzaSliceAreaKm(reroutedWindow, centerPoint);

    double reroutedEfficiencySqKm = reroutedPizzaSliceArea / reroutedCrustLength;
    double originalEfficiencySqKm = originalPizzaSliceArea / originalCrustLength;
    //if (reroutedCrustLength != originalCrustLength) System.out.println("FOUND ALTERNATIVE REROUTING " + reroutedCrustLength + " " + originalCrustLength + " " + reroutedEfficiencySqKm + " " + originalEfficiencySqKm);

    //if (reroutedEfficiencySqKm > originalEfficiencySqKm) System.out.println("FOUND BETTER REROUTING");
    return reroutedEfficiencySqKm > originalEfficiencySqKm;
  }
}
