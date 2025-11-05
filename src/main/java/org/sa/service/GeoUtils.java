package org.sa.service;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.sa.DTO.PointDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeoUtils {

  /**
   * Calculates the great-circle distance (in kilometers) between two geographic points
   * on the Earth's surface using the Haversine formula.
   *
   * The Earth is modeled as a perfect sphere with radius 6371 km, so this gives
   * the distance along the curve of the Earth ("as the crow flies"), not a flat
   * straight-line projection.
   */
  public static double getDistanceBetweenLocations(PointDTO from, PointDTO to) {
    double earthRadiusKm = 6371.0;

    /** A radian is the way we measure angular distance on Earth’s sphere when converting latitude and longitude differences */
    double deltaLatitudeRadians = Math.toRadians(to.latitude - from.latitude);
    double deltaLongitudeRadians = Math.toRadians(to.longitude - from.longitude);
    double fromLatitudeRadians = Math.toRadians(from.latitude);
    double toLatitudeRadians = Math.toRadians(to.latitude);

    double haversine = Math.sin(deltaLatitudeRadians / 2) * Math.sin(deltaLatitudeRadians / 2)
        + Math.sin(deltaLongitudeRadians / 2) * Math.sin(deltaLongitudeRadians / 2)
        * Math.cos(fromLatitudeRadians) * Math.cos(toLatitudeRadians);

    double angularDistance = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    return earthRadiusKm * angularDistance;
  }

  public static double autoCloseRouteAndGetLengthKm(List<PointDTO> route) {
    if (route.size() < 2) return 0.0;
    double distanceWithoutClosing = getCurveDistanceNoClosing(route);
    double closingDistance = getDistanceBetweenLocations(route.get(route.size() - 1), route.get(0));
    return distanceWithoutClosing + closingDistance;
  }

  public static double getCurveDistanceNoClosing(List<PointDTO> route) {
    return getCurveDistanceNoClosing(route, 0, route.size() - 1);
  }

  public static double getCurveDistanceNoClosing(List<PointDTO> route, int fromIndex, int toIndex) {
    double totalDistance = 0.0;
    for (int i = fromIndex; i < toIndex; i++)
      totalDistance += getDistanceBetweenLocations(route.get(i), route.get(i + 1));
    return totalDistance;
  }

  /**
   * Approximates the area (in square kilometers) of the polygon formed by the given route.
   * The route should form a closed loop (first point ≈ last point).
   *
   * Converts latitude/longitude degrees into approximate kilometers (with longitude
   * scaled by cos(latitude)) and applies the Shoelace formula for polygon area.
   */
  public static double getRouteAreaKm(List<PointDTO> noLoopRoutedPoints) {
    if (noLoopRoutedPoints.size() < 3) return 0.0;

    double earthKmPerDegree = 111.32;
    double accumulatedSum = 0.0;

    for (int i = 0; i < noLoopRoutedPoints.size(); i++) {
      PointDTO currentPoint = noLoopRoutedPoints.get(i);
      PointDTO nextPoint = noLoopRoutedPoints.get((i + 1) % noLoopRoutedPoints.size());

      double currentPointXkm = currentPoint.longitude * earthKmPerDegree * Math.cos(Math.toRadians(currentPoint.latitude));
      double currentPointYkm = currentPoint.latitude * earthKmPerDegree;
      double nextPointXkm = nextPoint.longitude * earthKmPerDegree * Math.cos(Math.toRadians(nextPoint.latitude));
      double nextPointYkm = nextPoint.latitude * earthKmPerDegree;

      accumulatedSum += (currentPointXkm * nextPointYkm - nextPointXkm * currentPointYkm);
    }

    double polygonAreaKm2 = Math.abs(accumulatedSum) / 2.0;
    return polygonAreaKm2;
  }

  public static double getCircleAreaByLength(double circleLength) {
    return (circleLength * circleLength) / (4 * Math.PI);
  }

  public static Polygon offsetPolygonInwards(Polygon jtsPolygon, double offsetValueKm) {
    BufferParameters params = new BufferParameters();
    params.setJoinStyle(BufferParameters.JOIN_BEVEL); // Straight truncation of corners — minimal computation
    Geometry offsetGeom = BufferOp.bufferOp(jtsPolygon, -offsetValueKm / 111.32, params);
    return (Polygon) offsetGeom;
  }
  public static Polygon offsetPolygonOutwards(Polygon jtsPolygon, double offsetValueKm) {
    BufferParameters params = new BufferParameters();
    params.setJoinStyle(BufferParameters.JOIN_BEVEL); // Straight truncation of corners — minimal computation
    Geometry offsetGeom = BufferOp.bufferOp(jtsPolygon, offsetValueKm / 111.32, params);
    return (Polygon) offsetGeom;
  }

  public static Polygon offsetPolygonOutwards(List<PointDTO> mainTownIntvlEdge, int offsetKm) {
    return offsetPolygonOutwards(convertPointsToPolygon(mainTownIntvlEdge), offsetKm);
  }
  public static boolean isWithinPolygon(Polygon polygon, double latitude, double longitude) {
    // Check if point is inside polygon (true if inside or on boundary)
    return polygon.covers(new GeometryFactory().createPoint(new Coordinate(longitude, latitude)));
  }

  public static boolean isWithinPolygon(Polygon polygon, PointDTO point) {
    return isWithinPolygon(polygon, point.latitude, point.longitude);
  }

  /** Calculates the great-circle distance between two geographic coordinates using the Haversine formula.*/
  public static double measureCircleDistanceHaversineMeters(double startLatitudeY, double startLongitudeX, double endLatitudeY, double endLongitudeX) {
    double earthRadiusMeters = 6371000;
    double deltaLatitudeY = Math.toRadians(endLatitudeY - startLatitudeY);
    double deltaLongitudeX = Math.toRadians(endLongitudeX - startLongitudeX);
    double haversineValue = Math.sin(deltaLatitudeY/2)*Math.sin(deltaLatitudeY/2)
        + Math.cos(Math.toRadians(startLatitudeY))*Math.cos(Math.toRadians(endLatitudeY))
        * Math.sin(deltaLongitudeX/2)*Math.sin(deltaLongitudeX/2);
    return 2 * earthRadiusMeters * Math.atan2(Math.sqrt(haversineValue), Math.sqrt(1 - haversineValue));
  }

  public static List<PointDTO> addExtraPointsInBetweenExistingOnes(List<PointDTO> routePoints) {
    List<PointDTO> enhancedRoute = new ArrayList<>();
    if (routePoints.size() < 2) return new ArrayList<>(routePoints);

    int n = routePoints.size();
    for (int i = 0; i < n; i++) {
      PointDTO current = routePoints.get(i);
      PointDTO next = routePoints.get((i + 1) % n); // wrap around for last → first
      enhancedRoute.add(current);

      // calculate midpoint
      double midLat = (current.latitude + next.latitude) / 2;
      double midLon = (current.longitude + next.longitude) / 2;

      enhancedRoute.add(new PointDTO(midLat, midLon));
    }
    return enhancedRoute;
  }

  /** Checks if at least the given percentage of points from 'small' are inside 'big' polygon.*/
  public static boolean areMostPointsWithinPolygon(List<PointDTO> smallPoints, List<PointDTO> bigPolygonPoints, int minOkPercentage) {
    if (smallPoints.isEmpty() || bigPolygonPoints.size() < 3) return false;

    GeometryFactory geoFactory = new GeometryFactory();
    Polygon bigPolygon = geoFactory.createPolygon(closeLoop(bigPolygonPoints));
    Polygon slightBigPolygonOffset = offsetPolygonOutwards(bigPolygon, 0.1); //without offset, matching lines are sometimes counted outside
    long insideCount = smallPoints.stream()
        .filter(p -> slightBigPolygonOffset.covers(geoFactory.createPoint(new Coordinate(p.longitude, p.latitude))))
        .count();

    double percentageOfSmallPointsWithinBig = (insideCount * 100.0) / smallPoints.size();
    return percentageOfSmallPointsWithinBig >= minOkPercentage;
  }

  /** Converts a list of points into a closed coordinate ring:
   *  ensures first and last coordinates are identical so the shape forms a loop. */
  private static Coordinate[] closeLoop(List<PointDTO> points) {
    if (points.size() < 3) return new Coordinate[0];
    Coordinate[] coordinates = points.stream()
        .map(p -> new Coordinate(p.longitude, p.latitude))
        .toArray(Coordinate[]::new);
    if (coordinates[0].equals2D(coordinates[coordinates.length - 1])) return coordinates;
    Coordinate[] closed = Arrays.copyOf(coordinates, coordinates.length + 1);
    closed[closed.length - 1] = new Coordinate(coordinates[0]);
    return closed;
  }

  public static Polygon convertPointsToPolygon(List<PointDTO> points) {
    if (points == null || points.size() < 3) return null;
    GeometryFactory geoFactory = new GeometryFactory();
    return geoFactory.createPolygon(closeLoop(points));
  }
}

