package org.sa.service;

import org.sa.PointDTO;

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





  public List<PointDTO> removeLoops(List<PointDTO> routedPoints, double indicatorOfLoop_maxDistance_loopStart_loopFinish) {
    List<PointDTO> cleaned = new ArrayList<>();
    if (routedPoints.isEmpty()) return cleaned;
    double routedPointsTotalDistance = GeoUtils.getRouteDistanceKm(routedPoints);
    double loopSizeThreshold = routedPointsTotalDistance * 0.3;

    int i = 0;

    //if loop is found — cut it out!
    for (; i < routedPoints.size(); i++) {
      Integer loopEndingIndex = getLoopEndingIndex(routedPoints, i, indicatorOfLoop_maxDistance_loopStart_loopFinish);

      //no loop found
      if (loopEndingIndex == null) {
        cleaned.add(routedPoints.get(i));
        i++;
      }
      else {
        boolean isLoopBig = GeoUtils.getRouteDistanceKm(routedPoints, i, loopEndingIndex) > loopSizeThreshold; // computationally expensive!!!
        if (isLoopBig) { //if loop is big, still don't cut it
          cleaned.add(routedPoints.get(i));
          i++;
        }
        else { //cut out the loop
          cleaned.add(routedPoints.get(loopEndingIndex));
          i = loopEndingIndex + 1;
        }
      }
    }
    return cleaned;
  }

  private Integer getLoopEndingIndex(List<PointDTO> routedPoints, int i, double indicatorOfLoop_maxDistance_loopStart_loopFinish) {
    //get first index outside loopThreshold
    PointDTO current = routedPoints.get(i);
    Integer startIndex = i;
    while (GeoUtils.getDistanceBetweenLocations(routedPoints.get(startIndex), current) < indicatorOfLoop_maxDistance_loopStart_loopFinish) {
      startIndex++;
      if (startIndex ==routedPoints.size())
        return null;
    }

    //start looking for the biggest possible loop, so start from the end point
    for (int j = routedPoints.size() - 2; j >= startIndex; j--)
      if (GeoUtils.getDistanceBetweenLocations(routedPoints.get(j), current) < indicatorOfLoop_maxDistance_loopStart_loopFinish)
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
    Collections.reverse(points);
    return result;
  }

  public PointDTO movePoint(PointDTO point, double northKm, double eastKm) {
    double earthKmPerDegree = 111.32;

    double latOffset = northKm / earthKmPerDegree;
    double lonOffset = eastKm / (earthKmPerDegree * Math.cos(Math.toRadians(point.latitude)));

    double newLat = point.latitude + latOffset;
    double newLon = point.longitude + lonOffset; // east is positive

    return new PointDTO(newLat, newLon);
  }
}

