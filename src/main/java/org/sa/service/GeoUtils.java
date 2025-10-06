package org.sa.service;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.sa.PointDTO;

import java.util.ArrayList;
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

  public static double getPizzaSliceAreaKm(List<PointDTO> pizzaCrust, PointDTO pizzaCenter) {
    List<PointDTO> pizzaSlice = new ArrayList<>();
    pizzaSlice.add(pizzaCenter);
    pizzaSlice.addAll(pizzaCrust);
    pizzaSlice.add(pizzaCenter);
    return getRouteAreaKm(pizzaSlice);
  }

  public static List<PointDTO> offsetPolygonInwards(List<PointDTO> polygon, double offsetValueKm) {
    GeometryFactory geometryFactory = new GeometryFactory();
    if (polygon.size() < 3) return new ArrayList<>(polygon);

    // Ensure polygon is closed
    List<PointDTO> closedPolygon = new ArrayList<>(polygon);
    if (!closedPolygon.get(0).equals(closedPolygon.get(closedPolygon.size() - 1)))
      closedPolygon.add(closedPolygon.get(0));

    // Convert PointDTO -> JTS Coordinates
    Coordinate[] coords = closedPolygon.stream()
        .map(p -> new Coordinate(p.longitude, p.latitude))
        .toArray(Coordinate[]::new);

    LinearRing shell = geometryFactory.createLinearRing(coords);
    Polygon jtsPolygon = geometryFactory.createPolygon(shell, null);

    // Offset inward (negative distance)
    BufferParameters params = new BufferParameters();
    params.setJoinStyle(BufferParameters.JOIN_ROUND); // rounded corners
    Geometry offsetGeom = BufferOp.bufferOp(jtsPolygon, -offsetValueKm / 111.32, params);

    // Convert back to List<PointDTO> (only use the exterior ring)
    List<PointDTO> offsetPoints = new ArrayList<>();
    if (offsetGeom instanceof Polygon offsetPolygon) {
      Coordinate[] offsetCoords = offsetPolygon.getExteriorRing().getCoordinates();
      for (Coordinate c : offsetCoords) offsetPoints.add(new PointDTO(c.y, c.x));
    }

    return offsetPoints;
  }

  public static boolean isWithinPolygon(List<PointDTO> polygon, double latitude, double longitude) {
    if (polygon.size() < 3) return false;

    GeometryFactory geometryFactory = new GeometryFactory();

    // Ensure polygon is closed
    List<PointDTO> closedPolygon = new ArrayList<>(polygon);
    if (!closedPolygon.get(0).equals(closedPolygon.get(closedPolygon.size() - 1)))
      closedPolygon.add(closedPolygon.get(0));

    // Convert to JTS Coordinates
    Coordinate[] coords = closedPolygon.stream()
        .map(p -> new Coordinate(p.longitude, p.latitude))
        .toArray(Coordinate[]::new);

    LinearRing shell = geometryFactory.createLinearRing(coords);
    Polygon jtsPolygon = geometryFactory.createPolygon(shell, null);

    // Create point
    Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));

    // Check if point is inside polygon (true if inside or on boundary)
    return jtsPolygon.covers(point);
  }

}

