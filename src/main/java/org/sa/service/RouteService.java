package org.sa.service;

import org.sa.DTO.PointDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteService {

  public List<PointDTO> generatePerfectCirclePoints(PointDTO center, double circleLengthKm, double maxDistanceBetweenPointsKm) {
    double earthRadiusKm = 6371.0;
    double radiusKm = circleLengthKm / (2 * Math.PI);
    int pointCount = (int) Math.ceil(circleLengthKm / maxDistanceBetweenPointsKm);
    List<PointDTO> points = new ArrayList<>();

    for (int i = 0; i < pointCount; i++) {
      double angle = 2 * Math.PI * i / pointCount;
      double latOffset = (radiusKm / earthRadiusKm) * Math.sin(angle);
      double lonOffset = (radiusKm / (earthRadiusKm * Math.cos(Math.toRadians(center.latitude)))) * Math.cos(angle);
      double lat = center.latitude + Math.toDegrees(latOffset);
      double lon = center.longitude + Math.toDegrees(lonOffset);
      points.add(new PointDTO(lat, lon));
    }
    return points;
  }

  public List<PointDTO> removeLoops(List<PointDTO> initialRoute, double indicatorOfLoop, GraphHopper graphHopper, String graphHopperProfileFootShortest) {
    List<PointDTO> cleanedFromLoops = new ArrayList<>();
    if (initialRoute.isEmpty()) return cleanedFromLoops;
    double loopSizeThreshold = GeoUtils.autoCloseRouteAndGetLengthKm(initialRoute) * 0.3; // loop cannot be bigger than a third of course.

    int loopStartPoint = 0;

    //if loop is found — cut it out!
    for (; loopStartPoint < initialRoute.size(); loopStartPoint++) { //TODO:for efficiency move by some distance, like at least 50 meters
      Integer loopEndingIndex = getLoopEndingIndex(initialRoute, loopStartPoint, indicatorOfLoop, loopSizeThreshold); //if stays null — there is no loop.
      //not found
      if (loopEndingIndex == null) {
        cleanedFromLoops.add(initialRoute.get(loopStartPoint));
        loopStartPoint++;
      }

      //found!!!
      else {

        // case when route end and start formulates a loop
        if (loopEndingIndex < loopStartPoint) {

          //find where to start clean list
          //System.out.println("loopEndingIndex is index in the initial route, but we need this points index in the new route");
          int newStartingIndexOfCleanRoute = 0;
          for (int i = 0; i < cleanedFromLoops.size(); i++) {
            if (cleanedFromLoops.get(i).equals(initialRoute.get(loopEndingIndex))) {
              newStartingIndexOfCleanRoute = i;
              //System.out.println("found start point!!!!!!!!!!!!!!!!!!!!!!"); /** may not find, perhaps because of this original point did not get into 'clean' list*/
            }
            if (newStartingIndexOfCleanRoute == 0) {
              newStartingIndexOfCleanRoute = findClosestPointInRoute(cleanedFromLoops, initialRoute.get(loopEndingIndex));
              //System.out.println("found CLOSEST to start point");
            }
          }

          //simply reroute this loop that covers start and endpoint
          List<PointDTO> rerouted = graphHopper.connectSnappedPointsWithRoutesNoClosing(List.of(initialRoute.get(loopStartPoint), initialRoute.get(newStartingIndexOfCleanRoute)), graphHopperProfileFootShortest);
          cleanedFromLoops.addAll(rerouted);
          return cleanedFromLoops.subList(newStartingIndexOfCleanRoute, cleanedFromLoops.size());
        }
        List<PointDTO> rerouted = graphHopper.connectSnappedPointsWithRoutesNoClosing(List.of(initialRoute.get(loopStartPoint), initialRoute.get(loopEndingIndex)), graphHopperProfileFootShortest);

        //case when there is no other way to get rid of this loop
        if (areTooSimilarCurves(rerouted, initialRoute, loopStartPoint, loopEndingIndex)) {
          cleanedFromLoops.add(initialRoute.get(loopStartPoint));
          loopStartPoint++;
          continue;
        }

        //TODO: are cases when the loop start is first point of initial route
        //TODO: are cases when the loop end is end point of initial route

        cleanedFromLoops.addAll(rerouted);
        loopStartPoint = loopEndingIndex + 1;
      }
    }
    return cleanedFromLoops;
  }

  private boolean areTooSimilarCurves(List<PointDTO> rerouted, List<PointDTO> routedPoints, int loopStartPoint, Integer loopEndingIndex) {
    double reroutedDistance = 0.0;
    double originalDistance = 0.0;
    for (int i = 1; i < rerouted.size(); i++) reroutedDistance += GeoUtils.getDistanceBetweenLocations(rerouted.get(i - 1), rerouted.get(i));
    for (int i = loopStartPoint + 1; i <= loopEndingIndex; i++) originalDistance += GeoUtils.getDistanceBetweenLocations(routedPoints.get(i - 1), routedPoints.get(i));
    if (reroutedDistance / originalDistance > 0.99) return true;
    return false;
  }

  private int findClosestPointInRoute(List<PointDTO> cleanedFromLoops, PointDTO pointDTO) {
    final double TOLERANCE_KM = 0.01; // ≈ 10 meters
    int closestIndex = -1;
    double minDistanceKm = Double.MAX_VALUE;

    for (int i = 0; i < cleanedFromLoops.size(); i++) {
      double distanceKm = GeoUtils.getDistanceBetweenLocations(pointDTO, cleanedFromLoops.get(i));
      if (distanceKm < TOLERANCE_KM) return i;
      if (distanceKm < minDistanceKm) { minDistanceKm = distanceKm; closestIndex = i; }
    }

    return closestIndex;
  }

  private Integer getLoopEndingIndex(List<PointDTO> route, int loopStartPointIndex, double indicatorOfLoopKm, double loopSizeThreshold) {
    //get first index outside loopThreshold (loopEndpointRangeStartIndex)
    //after this point (loopEndpointRangeStartIndex), if any point is too close to loopStartPoint (by indicatorOfLoopKm distance)
    //then this is the loop endpoint that will be returned
    //TODO: sowhere could be checks so that route is not shorter that loop SizeThreshold
    PointDTO loopStartPoint = route.get(loopStartPointIndex);
    Integer loopEndpointRangeStartIndex = loopStartPointIndex;
    double accumulatedDistance = 0;
    while (accumulatedDistance < indicatorOfLoopKm) {
      int prevPointIndex = loopEndpointRangeStartIndex;
      loopEndpointRangeStartIndex++;
      loopEndpointRangeStartIndex %= route.size(); /** in case if goes after the index of the last point*/
      accumulatedDistance += GeoUtils.getDistanceBetweenLocations(
          route.get(prevPointIndex),
          route.get(loopEndpointRangeStartIndex));
    }

    accumulatedDistance = 0; //reuse of the same variable, so set to 0;
    int loopEndpointRangeEndIndex = loopEndpointRangeStartIndex;
    while (accumulatedDistance < loopSizeThreshold) {
      int prevPointIndex = loopEndpointRangeEndIndex;
      loopEndpointRangeEndIndex++;
      loopEndpointRangeEndIndex %= route.size(); /** in case if goes after the index of the last point*/
      accumulatedDistance += GeoUtils.getDistanceBetweenLocations(
          route.get(prevPointIndex),
          route.get(loopEndpointRangeEndIndex));
    }

    //start looking for the biggest possible loop, so start from max possible end point
    for (int j = loopEndpointRangeEndIndex; j >= loopEndpointRangeStartIndex; j--, j %= route.size() /** in case if goes before the index 0*/)
      if (GeoUtils.getDistanceBetweenLocations(route.get(j), loopStartPoint) < indicatorOfLoopKm)
        return (Integer) j;

    return null;
  }

  public List<PointDTO> shiftABtoBA_andReverse(List<PointDTO> points) {
    if (points == null || points.isEmpty()) return List.of();
    int mid = (int)((double) points.size() * 0.28);
    List<PointDTO> firstHalf = points.subList(0, mid);
    List<PointDTO> secondHalf = points.subList(mid, points.size());
    List<PointDTO> result = new ArrayList<>(secondHalf);
    result.addAll(firstHalf);
    Collections.reverse(result);
    return result;
  }

  public List<PointDTO> startAndFinishRouteOnMostNorthernPoint(List<PointDTO> route) {
    if (route == null || route.isEmpty()) return List.of();
    PointDTO northernmost = route.stream().max((a, b) -> Double.compare(a.latitude, b.latitude)).orElse(route.get(0));
    int startIndex = route.indexOf(northernmost);
    if (startIndex == 0) return route; // already starts at northernmost
    List<PointDTO> reordered = new ArrayList<>(route.subList(startIndex, route.size()));
    reordered.addAll(route.subList(0, startIndex));
    reordered.add(reordered.get(0)); // close the loop
    return reordered;
  }

  public List<PointDTO> makeRouteClockwise(List<PointDTO> route) {
    if (route == null || route.size() < 3) return route;
    double area = 0;
    for (int i = 0; i < route.size() - 1; i++) {
      PointDTO p1 = route.get(i), p2 = route.get(i + 1);
      area += (p2.longitude - p1.longitude) * (p2.latitude + p1.latitude);
    }
    return area > 0 ? route : new ArrayList<>(route.reversed());
  }
}

